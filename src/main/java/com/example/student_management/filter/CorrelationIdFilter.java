package com.example.student_management.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.UUID;

public class CorrelationIdFilter implements Filter {
    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String REQUEST_ID_ATTRIBUTE = "requestId";
    private static  final Logger log= LoggerFactory.getLogger(CorrelationIdFilter.class);
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest=(HttpServletRequest) request;
        HttpServletResponse httpServletResponse=(HttpServletResponse) response;

        log.info("[FILTER] Incoming Request: {} {}",httpServletRequest.getMethod(),httpServletRequest.getRequestURI());

        String requestId = httpServletRequest.getHeader(REQUEST_ID_HEADER);
        if(requestId==null || requestId.trim().isBlank()){
            requestId= UUID.randomUUID().toString();
        }

        httpServletRequest.setAttribute(REQUEST_ID_ATTRIBUTE,requestId);
        httpServletResponse.setHeader(REQUEST_ID_HEADER,requestId);

        try {
            chain.doFilter(request,response);
        }finally {
            log.info("[FILTER] Completed Request: {} {} status={}",
                    httpServletRequest.getMethod(),
                    httpServletRequest.getRequestURI(),
                    httpServletResponse.getStatus());
        }
    }
}
