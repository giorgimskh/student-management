package com.example.student_management.filter;


import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class SimpleApiKeyFilter implements Filter {
    private static final String API_KEY_HEADER = "X-Api-Key";
    private static final String EXPECTED_API_KEY = "my-secret-key";
    private static final Logger log= LoggerFactory.getLogger(SimpleApiKeyFilter.class);
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest=(HttpServletRequest) request;
        HttpServletResponse httpServletResponse=(HttpServletResponse) response;

        log.info("[FILTER] Incoming Request: {} {}",httpServletRequest.getMethod(),httpServletRequest.getRequestURI());

        String requestURI=httpServletRequest.getRequestURI();
        if(requestURI!=null && !requestURI.startsWith("/api")){
            chain.doFilter(request,response);
            log.info("[FILTER] Completed Request: {} {} status={}",httpServletRequest.getMethod(),httpServletRequest.getRequestURI(),httpServletResponse.getStatus());
            return;
        }

        String apiKey=httpServletRequest.getHeader(API_KEY_HEADER);

        if(EXPECTED_API_KEY.equals(apiKey)){
            chain.doFilter(request,response);
        }else{
            httpServletResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpServletResponse.setContentType("application/json");
            httpServletResponse.getWriter().write("{\"error\": \"Unauthorized: Missing or invalid API key\"}");
        }

        log.info("[FILTER] Completed Request: {} {} status={}",httpServletRequest.getMethod(),httpServletRequest.getRequestURI(),httpServletResponse.getStatus());
    }
}
