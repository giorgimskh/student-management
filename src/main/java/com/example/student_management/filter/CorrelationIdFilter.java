package com.example.student_management.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.UUID;

public class CorrelationIdFilter implements Filter {
    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String REQUEST_ID_ATTRIBUTE = "requestId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest=(HttpServletRequest) request;
        HttpServletResponse httpServletResponse=(HttpServletResponse) response;

        String requestId = httpServletRequest.getHeader(REQUEST_ID_HEADER);
        if(requestId==null || requestId.isBlank()){
            requestId= UUID.randomUUID().toString();
        }

        httpServletRequest.setAttribute(REQUEST_ID_ATTRIBUTE,requestId);
        httpServletResponse.setHeader(REQUEST_ID_HEADER,requestId);

        chain.doFilter(request,response);
    }
}
