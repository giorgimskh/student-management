package com.example.student_management.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

public class ResponseTimeInterceptor implements HandlerInterceptor {
    private static final String START_TIME_ATTR = "responseTimeStart";

    @Override
    public boolean preHandle(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {
        request.setAttribute(START_TIME_ATTR,System.currentTimeMillis());
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler, @Nullable ModelAndView modelAndView) throws Exception {
        Long startTime= (Long) request.getAttribute(START_TIME_ATTR);

        if(startTime!=null){
            long duration=System.currentTimeMillis()-startTime;
            response.setHeader("X-Response-Time-Ms",String.valueOf(duration));
        }
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler, @Nullable Exception ex) throws Exception {
        Long startTime= (Long) request.getAttribute(START_TIME_ATTR);

        if(startTime!=null){
            long duration=System.currentTimeMillis()-startTime;
            response.setHeader("X-Response-Time-Ms",String.valueOf(duration));
        }
    }
}
