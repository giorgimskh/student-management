package com.example.student_management.config;

import com.example.student_management.filter.CorrelationIdFilter;
import com.example.student_management.filter.RequestLoggingFilter;
import com.example.student_management.filter.ResponseTimeFilter;
import com.example.student_management.filter.SimpleApiKeyFilter;
import jakarta.servlet.Filter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class AppConfig {
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilter() {
        return register(new CorrelationIdFilter(), 0);
    }

    @Bean
    public FilterRegistrationBean<RequestLoggingFilter> loggingFilter() {
        return register(new RequestLoggingFilter(), 1);
    }

    @Bean
    public FilterRegistrationBean<SimpleApiKeyFilter> simpleApiKeyFilter() {
        return register(new SimpleApiKeyFilter(), 2);
    }

    @Bean
    public FilterRegistrationBean<ResponseTimeFilter> responseTimeFilter() {
        return register(new ResponseTimeFilter(), 3);
    }

    private static <T extends Filter> FilterRegistrationBean<T> register(T filter, int order) {
        FilterRegistrationBean<T> registrationBean = new FilterRegistrationBean<>(filter);
        registrationBean.setOrder(order);
        return registrationBean;
    }
}
