package com.example.student_management.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

public class ResponseTimeFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletResponse httpResponse = (HttpServletResponse) response;
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(httpResponse);

        long startTime = System.currentTimeMillis();
        try {
            chain.doFilter(request, wrappedResponse); // controller writes into the wrapper's buffer, not the real response
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            wrappedResponse.setHeader("X-Response-Time-Ms", String.valueOf(duration)); // safe: nothing committed yet
            wrappedResponse.copyBodyToResponse(); // NOW flush buffered body + headers to the real response
        }
    }
}