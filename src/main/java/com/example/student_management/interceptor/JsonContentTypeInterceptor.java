package com.example.student_management.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;


public class JsonContentTypeInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws java.io.IOException {
        String method = request.getMethod();

        if(!"POST".equalsIgnoreCase(method) && !"PUT".equalsIgnoreCase(method ))
            return true;

        // no body sent at all — nothing to validate, let it through
        if (request.getContentLength() <= 0) return true;

        String contentType=request.getContentType();

        if(contentType==null || !contentType.startsWith("application/json")){
            response.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Unsupported Media Type. Content-Type must be application/json\"}");
            return false;
        }



        return true;
    }
}