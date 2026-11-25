package com.example.student_management.filter;

import com.example.student_management.security.StaticApiKeyValidator;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class SimpleApiKeyFilterTest {

    private MockHttpServletResponse run(SimpleApiKeyFilter filter, String uri, String apiKey, MockFilterChain chain) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        if (apiKey != null) {
            request.addHeader("X-Api-Key", apiKey);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        return response;
    }

    @Test
    void delegatesTheDecisionToTheValidator() throws Exception {
        SimpleApiKeyFilter filter = new SimpleApiKeyFilter(key -> "custom".equals(key));

        MockFilterChain allowed = new MockFilterChain();
        run(filter, "/api/students", "custom", allowed);
        assertThat(allowed.getRequest()).isNotNull();

        MockFilterChain rejected = new MockFilterChain();
        MockHttpServletResponse response = run(filter, "/api/students", SimpleApiKeyFilter.DEFAULT_API_KEY, rejected);
        assertThat(rejected.getRequest()).isNull();
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void defaultConstructorAcceptsOnlyTheDefaultKey() throws Exception {
        SimpleApiKeyFilter filter = new SimpleApiKeyFilter();

        assertThat(run(filter, "/api/books", SimpleApiKeyFilter.DEFAULT_API_KEY, new MockFilterChain()).getStatus()).isEqualTo(200);
        assertThat(run(filter, "/api/books", null, new MockFilterChain()).getStatus()).isEqualTo(401);
    }

    @Test
    void nonApiPathsSkipTheCheck() throws Exception {
        SimpleApiKeyFilter filter = new SimpleApiKeyFilter(new StaticApiKeyValidator("secret"));
        MockFilterChain chain = new MockFilterChain();

        run(filter, "/actuator/health", null, chain);

        assertThat(chain.getRequest()).isNotNull();
    }
}
