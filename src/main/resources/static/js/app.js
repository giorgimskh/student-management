"use strict";

/* ===========================================================================
   Student Management UI. Plain JS talking to the REST API under /api.
   =========================================================================== */

const DEFAULT_API_KEY = "my-secret-key";
const PAGE_SIZE = 10;

/** Status rules, mirrored from the server's State pattern for button hints. The server still decides. */
const STATUS_RULES = {
    ACTIVE:    { edit: true,  borrow: true,  enroll: true,  transitions: ["suspend", "graduate", "withdraw"] },
    SUSPENDED: { edit: true,  borrow: false, enroll: false, transitions: ["reactivate", "withdraw"] },
    GRADUATED: { edit: false, borrow: false, enroll: false, transitions: [] },
    WITHDRAWN: { edit: false, borrow: false, enroll: false, transitions: [] },
};
const TRANSITION_LABELS = { suspend: "Suspend", reactivate: "Reactivate", graduate: "Graduate", withdraw: "Withdraw" };
const TRANSITION_DONE = { suspend: "was suspended", reactivate: "was reactivated", graduate: "graduated", withdraw: "withdrew" };

const state = {
    view: "students",
    studentPage: 0,
    studentQuery: "",
    statusFilter: "",
    selectedId: null,
    bookPage: 0,
    coursePage: 0,
    names: new Map(),       // id -> display name for students, books and courses (activity feed, undo history)
    photoUrls: new Map(),   // studentId -> object URL (photos need the API key header, so <img src> can't fetch them)
};

/* ---------- Helpers ---------- */

const $ = (sel, root = document) => root.querySelector(sel);
const $$ = (sel, root = document) => [...root.querySelectorAll(sel)];

function esc(value) {
    return String(value ?? "").replace(/[&<>"']/g, c =>
        ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
}

function initials(name) {
    return (name || "?").trim().split(/\s+/).slice(0, 2).map(p => p[0]).join("").toUpperCase();
}

function label(status) {
    return status ? status.charAt(0) + status.slice(1).toLowerCase() : "";
}

function badge(status) {
    return `<span class="badge badge-${esc(status)}">${esc(label(status))}</span>`;
}

function timeAgo(iso) {
    const date = new Date(iso);
    if (isNaN(date)) return "";
    const s = Math.round((Date.now() - date) / 1000);
    if (s < 45) return "just now";
    if (s < 3600) return `${Math.round(s / 60)} min ago`;
    if (s < 86400) return `${Math.round(s / 3600)} h ago`;
    return date.toLocaleDateString();
}

const DAY_MS = 86400000;

/** "Due in 3 days" / "Due today" / "Overdue by 2 days" for an open loan. */
function dueText(loan) {
    const days = Math.ceil((new Date(loan.dueAt) - Date.now()) / DAY_MS);
    if (loan.overdue) {
        const late = Math.max(1, -days);
        return `Overdue by ${late} day${late === 1 ? "" : "s"}`;
    }
    if (days <= 0) return "Due today";
    return `Due in ${days} day${days === 1 ? "" : "s"}`;
}

/** A coloured badge for an open loan: red when overdue, amber when due within 3 days. */
function dueBadge(loan) {
    const tone = loan.overdue ? "overdue" : (new Date(loan.dueAt) - Date.now() < 3 * DAY_MS ? "due-soon" : "due");
    const date = new Date(loan.dueAt).toLocaleDateString();
    return `<span class="badge badge-${tone}" title="Due ${esc(date)}">${esc(dueText(loan))}</span>`;
}

/** Remembers display names so ids from the server can be shown as names. */
function rememberNames({ students = [], books = [], courses = [] }) {
    students.forEach(s => state.names.set(s.id, s.name));
    books.forEach(b => state.names.set(b.id, `"${b.title}"`));
    courses.forEach(c => state.names.set(c.id, c.courseName));
}

const UUID_PATTERN = /[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/gi;

/** "Enroll student 43bb… in course dcfa…" -> "Enroll student Ana in course Databases". Unknown ids are shortened. */
function withNames(text) {
    return String(text).replace(UUID_PATTERN, id => state.names.get(id) || `#${id.slice(0, 8)}`);
}

function apiKey() {
    try { return localStorage.getItem("apiKey") || DEFAULT_API_KEY; } catch { return DEFAULT_API_KEY; }
}

function toast(message, isError = false) {
    const el = document.createElement("div");
    el.className = "toast" + (isError ? " error" : "");
    el.textContent = message;
    $("#toasts").appendChild(el);
    setTimeout(() => el.remove(), isError ? 5200 : 3200);
}

/** Turns an API error body ({error: "..."} or {field: "message", ...}) into one line. */
function errorText(body, status) {
    if (body && typeof body === "object") {
        if (body.error) return body.error;
        const parts = Object.values(body).filter(v => typeof v === "string");
        if (parts.length) return parts.join(" · ");
    }
    return `Request failed (${status})`;
}

class ApiError extends Error {
    constructor(message, status) { super(message); this.status = status; }
}

async function api(path, { method = "GET", body, raw = false } = {}) {
    const headers = { "X-Api-Key": apiKey() };
    const init = { method, headers };
    if (body instanceof FormData) {
        init.body = body;
    } else if (body !== undefined) {
        headers["Content-Type"] = "application/json";
        init.body = JSON.stringify(body);
    }
    const res = await fetch(path, init);
    if (!res.ok) {
        let data = null;
        try { data = await res.json(); } catch { /* no JSON body */ }
        if (res.status === 401) openSettings();
        throw new ApiError(errorText(data, res.status), res.status);
    }
    if (raw) return res;
    if (res.status === 204) return null;
    const text = await res.text();
    return text ? JSON.parse(text) : null;
}

/** Runs an action, shows a toast, and reports failures without throwing. */
async function attempt(action, successMessage) {
    try {
        const result = await action();
        if (successMessage) toast(successMessage);
        return result;
    } catch (e) {
        toast(e.message, true);
        return undefined;
    }
}

function pager(el, page, onMove) {
    if (!page || page.totalPages <= 1) { el.innerHTML = ""; return; }
    el.innerHTML = `
        <span>Page ${page.page + 1} of ${page.totalPages} · ${page.totalElements} total</span>
        <span class="btns">
            <button class="btn btn-ghost btn-small" data-dir="-1" ${page.page === 0 ? "disabled" : ""}>Previous</button>
            <button class="btn btn-ghost btn-small" data-dir="1" ${page.last ? "disabled" : ""}>Next</button>
        </span>`;
    $$("button", el).forEach(b => b.addEventListener("click", () => onMove(Number(b.dataset.dir))));
}

/* ---------- Navigation ---------- */

function showView(view) {
    state.view = view;
    $$(".tab").forEach(t => t.classList.toggle("active", t.dataset.view === view));
    $$(".view").forEach(v => v.classList.toggle("active", v.id === `view-${view}`));
    ({ students: loadStudents, books: loadBooks, courses: loadCourses, activity: loadActivity })[view]();
}

/* ---------- Students ---------- */

async function loadStudents() {
    const list = $("#studentList");
    if (!list.children.length) list.innerHTML = '<li class="skeleton"></li>'.repeat(4);
    const q = state.studentQuery.trim();
    const status = state.statusFilter ? `&status=${encodeURIComponent(state.statusFilter)}` : "";
    const path = q
        ? `/api/students/search?name=${encodeURIComponent(q)}&page=${state.studentPage}&size=${PAGE_SIZE}&sort=name${status}`
        : `/api/students?page=${state.studentPage}&size=${PAGE_SIZE}&sort=name${status}`;
    const page = await attempt(() => api(path));
    if (!page) { list.innerHTML = '<li class="none">Could not load students.</li>'; return; }

    rememberNames({ students: page.content });
    const shown = page.content; // the server already applied the name and status filters
    list.innerHTML = shown.length ? shown.map(s => `
        <li class="row ${s.id === state.selectedId ? "selected" : ""}" data-id="${esc(s.id)}">
            <span class="avatar" data-photo="${esc(s.id)}">${esc(initials(s.name))}</span>
            <span class="row-main">
                <span class="row-title">${esc(s.name)}</span>
                <span class="row-sub">${esc(s.email)}</span>
            </span>
            ${badge(s.status)}
        </li>`).join("")
        : `<li class="none">${q || state.statusFilter ? "No students match." : "No students yet. Create the first one."}</li>`;

    $$(".row", list).forEach(row => row.addEventListener("click", () => selectStudent(row.dataset.id)));
    pager($("#studentPager"), page, dir => { state.studentPage += dir; loadStudents(); });
    shown.forEach(s => loadAvatar(s.id));
}

async function loadAvatar(id, { force = false } = {}) {
    if (force) {
        const old = state.photoUrls.get(id);
        if (old) URL.revokeObjectURL(old);
        state.photoUrls.delete(id);
    }
    let url = state.photoUrls.get(id);
    if (url === undefined) {
        try {
            const res = await api(`/api/students/${id}/photo`, { raw: true });
            url = URL.createObjectURL(await res.blob());
        } catch {
            url = null; // no photo: keep initials
        }
        state.photoUrls.set(id, url);
    }
    $$(`[data-photo="${CSS.escape(id)}"]`).forEach(el => {
        const name = state.names.get(id) || "";
        el.innerHTML = url ? `<img src="${url}" alt="">` : esc(initials(name));
    });
}

async function selectStudent(id) {
    state.selectedId = id;
    $$("#studentList .row").forEach(r => r.classList.toggle("selected", r.dataset.id === id));
    await renderStudentDetail();
}

async function renderStudentDetail() {
    const panel = $("#studentDetail");
    const id = state.selectedId;
    if (!id) return;

    const [student, books, allBooks, allCourses, activity, loans] = await Promise.all([
        attempt(() => api(`/api/students/${id}`)),
        attempt(() => api(`/api/students/${id}/books?size=100`)),
        attempt(() => api(`/api/books?size=100`)),
        attempt(() => api(`/api/courses?size=100`)),
        attempt(() => api(`/api/students/${id}/activity`)),
        attempt(() => api(`/api/students/${id}/loans`)),
    ]);
    const openLoanByBook = new Map((loans ?? []).filter(l => !l.returnedAt).map(l => [l.bookId, l]));
    if (!student) { panel.innerHTML = '<div class="empty-state"><p>Student not found.</p></div>'; return; }
    rememberNames({ students: [student], books: allBooks?.content, courses: allCourses?.content });

    const rules = STATUS_RULES[student.status] || STATUS_RULES.ACTIVE;
    const myBooks = books?.content ?? student.books ?? [];
    const myBookIds = new Set(myBooks.map(b => b.id));
    const myCourseIds = new Set(student.courses.map(c => c.id));
    const bookOptions = (allBooks?.content ?? []).filter(b => !myBookIds.has(b.id));
    const courseOptions = (allCourses?.content ?? []).filter(c => !myCourseIds.has(c.id));

    panel.innerHTML = `
        <div class="detail-hero">
            <span class="avatar large" data-photo="${esc(student.id)}">${esc(initials(student.name))}</span>
            <div>
                <h2>${esc(student.name)}</h2>
                <div class="row-sub">${esc(student.email)}</div>
                <div class="hero-meta">${badge(student.status)}
                    <span class="time">Joined ${student.createdAt ? esc(new Date(student.createdAt).toLocaleDateString()) : "—"}</span>
                </div>
            </div>
            <div class="photo-actions">
                <label class="btn btn-ghost btn-small" style="margin:0">Upload photo
                    <input type="file" accept="image/*" id="photoInput" hidden>
                </label>
                <button class="btn btn-ghost btn-small" id="removePhoto">Remove photo</button>
                <button class="btn btn-danger btn-small" id="deleteStudent">Delete</button>
            </div>
        </div>

        <div class="detail-body">
            <section>
                <h4 class="section-title">Status</h4>
                <div class="status-actions">
                    ${rules.transitions.length
                        ? rules.transitions.map(t => `<button class="btn ${t === "withdraw" ? "btn-danger" : "btn-ghost"}" data-transition="${t}">${TRANSITION_LABELS[t]}</button>`).join("")
                        : '<span class="none">No further status changes. This is a final status.</span>'}
                </div>
                <p class="rule-note">${esc(ruleSummary(student.status, rules))}</p>
            </section>

            <section>
                <h4 class="section-title">Profile</h4>
                <form class="profile-form" id="profileForm">
                    <label>Name <input name="name" value="${esc(student.name)}" required ${rules.edit ? "" : "disabled"}></label>
                    <label>Email <input name="email" type="email" value="${esc(student.email)}" required ${rules.edit ? "" : "disabled"}></label>
                    <button class="btn btn-primary" ${rules.edit ? "" : "disabled"}>Save</button>
                </form>
            </section>

            <section>
                <h4 class="section-title">Books <span>${myBooks.length}</span></h4>
                ${myBooks.length ? `<ul class="chips">${myBooks.map(b => {
                    const loan = openLoanByBook.get(b.id);
                    return `
                    <li class="chip">${esc(b.title)} ${loan ? dueBadge(loan) : `<small>${esc(b.isbn)}</small>`}
                        <button title="Give back" data-return-book="${esc(b.id)}">×</button></li>`;
                }).join("")}</ul>`
                    : '<p class="none">No books borrowed.</p>'}
                <form class="inline-form" id="assignBookForm">
                    <select name="bookId" ${rules.borrow ? "" : "disabled"}>
                        <option value="">${bookOptions.length ? "Choose a book to lend…" : "No other books"}</option>
                        ${bookOptions.map(b => `<option value="${esc(b.id)}">${esc(b.title)}</option>`).join("")}
                    </select>
                    <button class="btn btn-ghost" ${rules.borrow ? "" : "disabled"}>Lend</button>
                </form>
            </section>

            <section>
                <h4 class="section-title">Courses <span>${student.courses.length}</span></h4>
                ${student.courses.length ? `<ul class="chips">${student.courses.map(c => `
                    <li class="chip">${esc(c.courseName)} <small>${esc(c.code)}</small>
                        <button title="Drop course" data-drop-course="${esc(c.id)}">×</button></li>`).join("")}</ul>`
                    : '<p class="none">Not enrolled in any course.</p>'}
                <form class="inline-form" id="enrollForm">
                    <select name="courseId" ${rules.enroll ? "" : "disabled"}>
                        <option value="">${courseOptions.length ? "Choose a course…" : "No other courses"}</option>
                        ${courseOptions.map(c => `<option value="${esc(c.id)}">${esc(c.code)} · ${esc(c.courseName)}</option>`).join("")}
                    </select>
                    <button class="btn btn-ghost" ${rules.enroll ? "" : "disabled"}>Enroll</button>
                </form>
            </section>

            <section>
                <h4 class="section-title">Loan history <span>${(loans ?? []).length}</span></h4>
                ${loanHistory(loans ?? [])}
            </section>

            <section>
                <h4 class="section-title">Activity</h4>
                ${activityList(activity ?? [], 6)}
            </section>
        </div>`;

    loadAvatar(student.id);
    bindDetail(student);
}

function loanHistory(loans) {
    if (!loans.length) return '<p class="none">Has never borrowed a book.</p>';
    const day = iso => new Date(iso).toLocaleDateString();
    return `<table class="table loans">
        <thead><tr><th>Book</th><th>Borrowed</th><th>Returned</th></tr></thead>
        <tbody>${loans.slice(0, 10).map(l => `
            <tr>
                <td>${esc(l.bookTitle)}</td>
                <td class="time">${esc(day(l.borrowedAt))}</td>
                <td>${l.returnedAt
                    ? `<span class="time">${esc(day(l.returnedAt))}</span>${new Date(l.returnedAt) > new Date(l.dueAt) ? ' <span class="badge badge-due-soon">Late</span>' : ""}`
                    : dueBadge(l)}</td>
            </tr>`).join("")}</tbody>
    </table>`;
}

function ruleSummary(status, rules) {
    const can = [], cannot = [];
    (rules.edit ? can : cannot).push("edit their profile");
    (rules.borrow ? can : cannot).push("borrow books");
    (rules.enroll ? can : cannot).push("enroll in courses");
    const who = `${label(status)} students`;
    if (!cannot.length) return `${who} can ${can.join(", ")}.`;
    if (!can.length) return `${who} can't ${cannot.join(", ")}. They can still give back books and drop courses.`;
    return `${who} can ${can.join(", ")}, but can't ${cannot.join(" or ")}.`;
}

function bindDetail(student) {
    const id = student.id;
    const refresh = async () => { await renderStudentDetail(); loadStudents(); };

    $$("[data-transition]").forEach(btn => btn.addEventListener("click", async () => {
        const t = btn.dataset.transition;
        if (t === "withdraw" || t === "graduate") {
            if (!confirm(`${TRANSITION_LABELS[t]} ${student.name}? This is a final status (you can still undo it).`)) return;
        }
        if (await attempt(() => api(`/api/students/${id}/${t}`, { method: "POST" }), `${student.name} ${TRANSITION_DONE[t]}`)) await refresh();
    }));

    $("#profileForm").addEventListener("submit", async e => {
        e.preventDefault();
        const f = new FormData(e.target);
        if (await attempt(() => api(`/api/students/${id}`, { method: "PUT", body: { name: f.get("name"), email: f.get("email") } }), "Profile saved")) await refresh();
    });

    $("#assignBookForm").addEventListener("submit", async e => {
        e.preventDefault();
        const bookId = new FormData(e.target).get("bookId");
        if (!bookId) return;
        if (await attempt(() => api(`/api/students/${id}/books/${bookId}`, { method: "POST" }), "Book lent")) await refresh();
    });

    $("#enrollForm").addEventListener("submit", async e => {
        e.preventDefault();
        const courseId = new FormData(e.target).get("courseId");
        if (!courseId) return;
        if (await attempt(() => api(`/api/students/${id}/courses/${courseId}`, { method: "POST" }), "Enrolled")) await refresh();
    });

    $$("[data-return-book]").forEach(b => b.addEventListener("click", async () => {
        if (await attempt(() => api(`/api/students/${id}/books/${b.dataset.returnBook}`, { method: "DELETE" }), "Book given back") !== undefined) await refresh();
    }));

    $$("[data-drop-course]").forEach(b => b.addEventListener("click", async () => {
        if (await attempt(() => api(`/api/students/${id}/courses/${b.dataset.dropCourse}`, { method: "DELETE" }), "Course dropped") !== undefined) await refresh();
    }));

    $("#photoInput").addEventListener("change", async e => {
        const file = e.target.files[0];
        if (!file) return;
        const form = new FormData();
        form.append("file", file);
        if (await attempt(() => api(`/api/students/${id}/photo`, { method: "POST", body: form }), "Photo uploaded") !== undefined) {
            await loadAvatar(id, { force: true });
        }
    });

    $("#removePhoto").addEventListener("click", async () => {
        if (await attempt(() => api(`/api/students/${id}/photo`, { method: "DELETE" }), "Photo removed") !== undefined) {
            await loadAvatar(id, { force: true });
        }
    });

    $("#deleteStudent").addEventListener("click", async () => {
        if (!confirm(`Delete ${student.name}? This can't be undone.`)) return;
        if (await attempt(() => api(`/api/students/${id}`, { method: "DELETE" }), "Student deleted") !== undefined) {
            state.selectedId = null;
            $("#studentDetail").innerHTML = '<div class="empty-state"><div class="empty-illustration"></div><p>Select a student to see their profile, books and courses.</p></div>';
            loadStudents();
        }
    });
}

/* ---------- Books ---------- */

async function loadBooks() {
    const tbody = $("#bookTable");
    const page = await attempt(() => api(`/api/books?page=${state.bookPage}&size=${PAGE_SIZE}&sort=title`));
    if (!page) return;
    rememberNames({ books: page.content });
    if (!page.content.length) {
        tbody.innerHTML = '<tr><td colspan="5" class="none">No books yet.</td></tr>';
        pager($("#bookPager"), page, () => {});
        return;
    }
    // One request for every open loan instead of one "owner" request per book.
    const loans = await attempt(() => api("/api/loans")) ?? [];
    const loanByBook = new Map(loans.map(l => [l.bookId, l]));
    const overdue = loans.filter(l => l.overdue).length;
    $("#overdueSummary").innerHTML = overdue
        ? `<span class="badge badge-overdue">${overdue} overdue</span>`
        : loans.length ? `<span class="badge badge-due">${loans.length} on loan, none overdue</span>` : "";

    tbody.innerHTML = page.content.map(b => {
        const loan = loanByBook.get(b.id);
        return `
        <tr>
            <td><strong>${esc(b.title)}</strong></td>
            <td class="mono">${esc(b.isbn)}</td>
            <td>${loan
                ? `<a href="#" data-goto="${esc(loan.studentId)}">${esc(loan.studentName)}</a>`
                : '<span class="badge badge-free">Available</span>'}</td>
            <td>${loan ? dueBadge(loan) : ""}</td>
            <td><button class="btn btn-danger btn-small" data-delete-book="${esc(b.id)}" data-title="${esc(b.title)}">Delete</button></td>
        </tr>`;
    }).join("");

    $$("[data-goto]", tbody).forEach(a => a.addEventListener("click", e => { e.preventDefault(); gotoStudent(a.dataset.goto); }));
    $$("[data-delete-book]", tbody).forEach(b => b.addEventListener("click", async () => {
        if (!confirm(`Delete "${b.dataset.title}"?`)) return;
        if (await attempt(() => api(`/api/books/${b.dataset.deleteBook}`, { method: "DELETE" }), "Book deleted") !== undefined) loadBooks();
    }));
    pager($("#bookPager"), page, dir => { state.bookPage += dir; loadBooks(); });
}

/* ---------- Courses ---------- */

async function loadCourses() {
    const grid = $("#courseGrid");
    const page = await attempt(() => api(`/api/courses?page=${state.coursePage}&size=12&sort=courseName`));
    if (!page) return;
    rememberNames({ courses: page.content });
    grid.innerHTML = page.content.length ? page.content.map(c => `
        <div class="course-card">
            <div class="course-code">${esc(c.code)}</div>
            <div class="course-name">${esc(c.courseName)}</div>
            <div class="actions">
                <button class="btn btn-ghost btn-small" data-roster="${esc(c.id)}" data-name="${esc(c.courseName)}">Students</button>
                <button class="btn btn-danger btn-small" data-delete-course="${esc(c.id)}" data-name="${esc(c.courseName)}">Delete</button>
            </div>
        </div>`).join("") : '<p class="none">No courses yet.</p>';

    $$("[data-roster]", grid).forEach(b => b.addEventListener("click", () => showRoster(b.dataset.roster, b.dataset.name)));
    $$("[data-delete-course]", grid).forEach(b => b.addEventListener("click", async () => {
        if (!confirm(`Delete course "${b.dataset.name}"?`)) return;
        if (await attempt(() => api(`/api/courses/${b.dataset.deleteCourse}`, { method: "DELETE" }), "Course deleted") !== undefined) loadCourses();
    }));
    pager($("#coursePager"), page, dir => { state.coursePage += dir; loadCourses(); });
}

async function showRoster(courseId, name) {
    const students = await attempt(() => api(`/api/courses/${courseId}/students`));
    if (!students) return;
    $("#courseStudentsTitle").textContent = `Students in ${name}`;
    $("#courseStudentsList").innerHTML = students.length ? students.map(s => `
        <li class="row" data-goto="${esc(s.id)}" style="cursor:pointer">
            <span class="avatar">${esc(initials(s.name))}</span>
            <span class="row-main"><span class="row-title">${esc(s.name)}</span><span class="row-sub">${esc(s.email)}</span></span>
            ${badge(s.status)}
        </li>`).join("") : '<li class="none">Nobody is enrolled yet.</li>';
    $$("[data-goto]", $("#courseStudentsList")).forEach(r => r.addEventListener("click", () => {
        $("#courseStudentsDialog").close();
        gotoStudent(r.dataset.goto);
    }));
    $("#courseStudentsDialog").showModal();
}

function gotoStudent(id) {
    showView("students");
    selectStudent(id);
}

/* ---------- Activity & undo ---------- */

/** [text, tone]; "{item}" is replaced with the related book or course name. */
const EVENT_TEXT = {
    STUDENT_CREATED: ["was added", ""],
    STUDENT_UPDATED: ["updated their profile", ""],
    STUDENT_DELETED: ["was deleted", "danger"],
    BOOK_ASSIGNED: ["borrowed {item}", ""],
    BOOK_REMOVED: ["gave back {item}", ""],
    COURSE_ENROLLED: ["enrolled in {item}", ""],
    COURSE_DROPPED: ["dropped {item}", "warn"],
    STUDENT_SUSPENDED: ["was suspended", "warn"],
    STUDENT_REACTIVATED: ["was reactivated", ""],
    STUDENT_GRADUATED: ["graduated", "grad"],
    STUDENT_WITHDRAWN: ["withdrew", "danger"],
    STUDENT_STATUS_RESTORED: ["had a status change undone", "warn"],
    BOOK_OVERDUE: ["is late returning {item}", "danger"],
};

function activityList(events, limit = 50) {
    if (!events.length) return '<p class="none">Nothing yet.</p>';
    return `<ol class="timeline">${events.slice(0, limit).map(e => {
        const [template, tone] = EVENT_TEXT[e.type] || [e.type.toLowerCase().replaceAll("_", " "), ""];
        const fallback = e.type.startsWith("BOOK") ? "a book" : "a course";
        const text = template.replace("{item}", (e.relatedId && state.names.get(e.relatedId)) || fallback);
        const who = state.names.get(e.studentId) || `Student #${String(e.studentId).slice(0, 8)}`;
        return `<li><span class="dot ${tone}"></span><span><strong>${esc(who)}</strong> ${esc(text)}</span>
                <span class="time" title="${esc(e.occurredAt)}">${esc(timeAgo(e.occurredAt))}</span></li>`;
    }).join("")}</ol>`;
}

async function loadActivity() {
    const [events, history, students, books, courses] = await Promise.all([
        attempt(() => api("/api/activity")),
        attempt(() => api("/api/commands/history")),
        attempt(() => api("/api/students?size=100")),
        attempt(() => api("/api/books?size=100")),
        attempt(() => api("/api/courses?size=100")),
    ]);
    rememberNames({ students: students?.content, books: books?.content, courses: courses?.content });
    $("#activityFeed").outerHTML = `<div id="activityFeed">${activityList(events ?? [])}</div>`;
    $("#historyList").innerHTML = (history ?? []).length
        ? history.map(h => `<li>${esc(withNames(h))}</li>`).join("")
        : '<li class="none" style="background:none">Nothing to undo.</li>';
}

async function undoLast() {
    const result = await attempt(() => api("/api/commands/undo", { method: "POST" }));
    if (!result) return;
    toast(`Undone: ${withNames(result.undone)}`);
    if (state.view === "students") { loadStudents(); if (state.selectedId) renderStudentDetail(); }
    else showView(state.view);
}

/* ---------- Dialogs & forms ---------- */

function openSettings() {
    const dialog = $("#settingsDialog");
    if (dialog.open) return;
    $("#settingsForm").apiKey.value = apiKey();
    dialog.showModal();
}

function bindCreateForm(formId, path, toBody, onDone) {
    const form = $(formId);
    const error = $(".form-error", form);
    form.addEventListener("submit", async e => {
        e.preventDefault();
        error.hidden = true;
        try {
            const created = await api(path, { method: "POST", body: toBody(new FormData(form)) });
            form.closest("dialog").close();
            form.reset();
            onDone(created);
        } catch (err) {
            error.textContent = err.message;
            error.hidden = false;
        }
    });
}

function init() {
    $$(".tab").forEach(t => t.addEventListener("click", () => showView(t.dataset.view)));
    $$("[data-open]").forEach(b => b.addEventListener("click", () => {
        const dialog = $(`#${b.dataset.open}`);
        $(".form-error", dialog)?.setAttribute("hidden", "");
        dialog.showModal();
        $("input", dialog)?.focus();
    }));
    $$("[data-close]").forEach(b => b.addEventListener("click", () => b.closest("dialog").close()));

    $("#undoBtn").addEventListener("click", undoLast);
    $("#settingsBtn").addEventListener("click", openSettings);
    $("#refreshActivity").addEventListener("click", loadActivity);
    $("#settingsForm").addEventListener("submit", e => {
        e.preventDefault();
        try { localStorage.setItem("apiKey", e.target.apiKey.value.trim() || DEFAULT_API_KEY); } catch { /* private mode */ }
        $("#settingsDialog").close();
        toast("API key saved");
        showView(state.view);
    });

    let searchTimer;
    $("#studentSearch").addEventListener("input", e => {
        clearTimeout(searchTimer);
        searchTimer = setTimeout(() => { state.studentQuery = e.target.value; state.studentPage = 0; loadStudents(); }, 250);
    });
    $("#statusFilter").addEventListener("change", e => { state.statusFilter = e.target.value; state.studentPage = 0; loadStudents(); });

    bindCreateForm("#studentForm", "/api/students",
        f => ({ name: f.get("name"), email: f.get("email") }),
        s => { toast(`${s.name} added`); loadStudents(); selectStudent(s.id); });
    bindCreateForm("#bookForm", "/api/books",
        f => ({ title: f.get("title"), isbn: f.get("isbn") }),
        b => { toast(`"${b.title}" added`); loadBooks(); });
    bindCreateForm("#courseForm", "/api/courses",
        f => ({ courseName: f.get("courseName"), code: f.get("code") }),
        c => { toast(`${c.code} added`); loadCourses(); });

    showView("students");
}

document.addEventListener("DOMContentLoaded", init);
