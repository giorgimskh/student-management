package com.example.student_management.config;

import com.example.student_management.filter.CorrelationIdFilter;
import com.example.student_management.filter.RequestLoggingFilter;
import com.example.student_management.filter.SimpleApiKeyFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class AppConfig {
    @Bean
    public Clock clock(){
        return Clock.systemUTC();
    }

    @Bean
    public FilterRegistrationBean<RequestLoggingFilter> loggingFilter(){
        FilterRegistrationBean<RequestLoggingFilter> registrationBean = new FilterRegistrationBean<>();

        registrationBean.setFilter(new RequestLoggingFilter());

        registrationBean.setOrder(1);

        return registrationBean;
    }

    @Bean
    public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilterFilterRegistrationBean(){
        FilterRegistrationBean<CorrelationIdFilter> registrationBean=new FilterRegistrationBean<CorrelationIdFilter>();
        registrationBean.setFilter(new CorrelationIdFilter());

        registrationBean.setOrder(0);
        return registrationBean;
    }

    @Bean
    public FilterRegistrationBean<SimpleApiKeyFilter> simpleApiKeyFilterFilterRegistrationBean(){
        FilterRegistrationBean<SimpleApiKeyFilter> registrationBean = new FilterRegistrationBean<SimpleApiKeyFilter>();
        registrationBean.setFilter(new SimpleApiKeyFilter());

        registrationBean.setOrder(2);
        return registrationBean;
    }
}
