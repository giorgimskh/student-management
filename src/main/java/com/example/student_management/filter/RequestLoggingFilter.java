package com.example.student_management.filter;


import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import java.io.IOException;

public class RequestLoggingFilter  implements Filter  {
    //For writing Log Messages in console
    //When messages are printed, they will be tagged with this class name, making it easy to see where the logs are coming from.
    private static final Logger log= LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest=(HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;

        long startTime=System.currentTimeMillis();

        log.info("[FILTER] Incoming Request: {} {}",httpServletRequest.getMethod(),httpServletRequest.getRequestURI());

        try {
            chain.doFilter(request,response);
        } finally {
            long duration = System.currentTimeMillis()-startTime;

            log.info("[FILTER] Completed Request: {} {} status={}, duration={} ",httpServletRequest.getMethod(),httpServletRequest.getRequestURI(),httpServletResponse.getStatus(),duration);
        }
    }
}
