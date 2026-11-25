package com.example.student_management.filter;

import com.example.student_management.security.ApiKeyValidator;
import com.example.student_management.security.StaticApiKeyValidator;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class SimpleApiKeyFilter implements Filter {
    private static final String API_KEY_HEADER = "X-Api-Key";
    public static final String DEFAULT_API_KEY = "my-secret-key";

    private final ApiKeyValidator apiKeyValidator;

    /** Accepts only {@link #DEFAULT_API_KEY}. */
    public SimpleApiKeyFilter() {
        this(new StaticApiKeyValidator(DEFAULT_API_KEY));
    }

    public SimpleApiKeyFilter(ApiKeyValidator apiKeyValidator) {
        this.apiKeyValidator = apiKeyValidator;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest=(HttpServletRequest) request;
        HttpServletResponse httpServletResponse=(HttpServletResponse) response;

        String requestURI=httpServletRequest.getRequestURI();
        if(requestURI!=null && !requestURI.startsWith("/api")){
            chain.doFilter(request,response);
            return;
        }

        String apiKey=httpServletRequest.getHeader(API_KEY_HEADER);

        if(apiKeyValidator.isValid(apiKey)){
            chain.doFilter(request,response);
        }else{
            httpServletResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpServletResponse.setContentType("application/json");
            httpServletResponse.getWriter().write("{\"error\": \"Unauthorized: Missing or invalid API key\"}");
        }
    }
}
