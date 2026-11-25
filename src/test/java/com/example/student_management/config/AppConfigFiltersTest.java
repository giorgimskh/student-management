package com.example.student_management.config;

import com.example.student_management.filter.ResponseTimeFilter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

import static org.assertj.core.api.Assertions.assertThat;

class AppConfigFiltersTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(AppConfig.class);

    @Test
    @SuppressWarnings("unchecked")
    void responseTimeFilter_onlyWrapsApiRequests_soTheWebUiIsServedNormally() {
        runner.run(context -> {
            FilterRegistrationBean<ResponseTimeFilter> registration =
                    context.getBean("responseTimeFilter", FilterRegistrationBean.class);
            assertThat(registration.getUrlPatterns()).containsExactly("/api/*");
        });
    }
}
