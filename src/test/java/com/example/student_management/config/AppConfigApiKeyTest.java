package com.example.student_management.config;

import com.example.student_management.filter.SimpleApiKeyFilter;
import com.example.student_management.security.ApiKeyValidator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/** Loads only {@link AppConfig}, so no database or web server is needed. */
class AppConfigApiKeyTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(AppConfig.class);

    @SuppressWarnings("unchecked")
    private static int statusFor(ApplicationContext context, String apiKey) throws Exception {
        FilterRegistrationBean<SimpleApiKeyFilter> registration =
                context.getBean("simpleApiKeyFilter", FilterRegistrationBean.class);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/students");
        request.addHeader("X-Api-Key", apiKey);
        MockHttpServletResponse response = new MockHttpServletResponse();
        registration.getFilter().doFilter(request, response, new MockFilterChain());
        return response.getStatus();
    }

    @Test
    void withoutProperty_usesTheDefaultKey() {
        runner.run(context -> {
            ApiKeyValidator validator = context.getBean(ApiKeyValidator.class);
            assertThat(validator.isValid(SimpleApiKeyFilter.DEFAULT_API_KEY)).isTrue();
            assertThat(statusFor(context, SimpleApiKeyFilter.DEFAULT_API_KEY)).isEqualTo(200);
            assertThat(statusFor(context, "wrong")).isEqualTo(401);
        });
    }

    @Test
    void propertyOverridesTheKeyUsedByTheFilter() {
        runner.withPropertyValues("app.security.api-key=test-key").run(context -> {
            assertThat(statusFor(context, "test-key")).isEqualTo(200);
            assertThat(statusFor(context, SimpleApiKeyFilter.DEFAULT_API_KEY)).isEqualTo(401);
        });
    }
}
