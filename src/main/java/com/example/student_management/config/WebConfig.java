package com.example.student_management.config;

import com.example.student_management.interceptor.JsonContentTypeInterceptor;
import com.example.student_management.interceptor.RequestLoggingInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Bean
    public RequestLoggingInterceptor requestLoggingInterceptor() {
        return new RequestLoggingInterceptor();
    }

    @Bean
    public JsonContentTypeInterceptor jsonContentTypeInterceptor(){
        return new JsonContentTypeInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requestLoggingInterceptor())
                .addPathPatterns("/api/**");

        registry.addInterceptor(jsonContentTypeInterceptor()).
                addPathPatterns("/api/**");
    }

}