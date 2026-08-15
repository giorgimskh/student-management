package com.example.student_management.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

public class RequestLoggingInterceptor implements HandlerInterceptor {
    private static final Logger log= LoggerFactory.getLogger(RequestLoggingInterceptor.class);
    private static final String START_TIME_ATTR="startTime";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        request.setAttribute(START_TIME_ATTR,System.currentTimeMillis());

        if(handler instanceof HandlerMethod handlerMethod){
            log.info("[INTERCEPTOR] {} {} -> handled by {}#{}",
                    request.getMethod(),request.getRequestURI(),
                    handlerMethod.getBeanType().getSimpleName(),
                    handlerMethod.getMethod().getName());
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception ex) throws Exception {
        long startTime = (long) request.getAttribute(START_TIME_ATTR);
        long duration=System.currentTimeMillis()-startTime;

        if(ex!=null){
            log.warn("[INTERCEPTOR] {} {} -> status={} threw {} ({} ms)",
                    request.getMethod(),request.getRequestURI(),
                    response.getStatus(),ex.getClass().getSimpleName(),duration);
        }else {
            log.info("[INTERCEPTOR] {} {} -> status={} ({} ms)",
                    request.getMethod(),request.getRequestURI(),
                    response.getStatus(),duration);
        }
    }
}
