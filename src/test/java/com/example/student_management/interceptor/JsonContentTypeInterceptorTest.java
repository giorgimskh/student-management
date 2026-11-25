package com.example.student_management.interceptor;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class JsonContentTypeInterceptorTest {
    private final JsonContentTypeInterceptor interceptor = new JsonContentTypeInterceptor();

    private boolean allows(String method, String contentType, byte[] body) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, "/api/students");
        if (contentType != null) request.setContentType(contentType);
        request.setContent(body); // sets Content-Length like a real client would
        return interceptor.preHandle(request, new MockHttpServletResponse(), new Object());
    }

    @Test
    void jsonBody_isAllowed() throws Exception {
        assertThat(allows("POST", "application/json", "{}".getBytes())).isTrue();
        assertThat(allows("PUT", "application/json;charset=UTF-8", "{}".getBytes())).isTrue();
    }

    @Test
    void multipartUpload_isAllowed() throws Exception {
        assertThat(allows("POST", "multipart/form-data; boundary=abc", "--abc--".getBytes())).isTrue();
    }

    @Test
    void otherBodyTypes_areRejectedWith415() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/students");
        request.setContentType("text/plain");
        request.setContent("hello".getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(request, response, new Object())).isFalse();
        assertThat(response.getStatus()).isEqualTo(415);
    }

    @Test
    void requestsWithoutBody_orNonWriteMethods_areAllowed() throws Exception {
        assertThat(allows("POST", null, new byte[0])).isTrue();
        assertThat(allows("GET", "text/plain", "x".getBytes())).isTrue();
    }
}
