package ca.bc.gov.nrs.csp.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.SortHandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WebConfigTest {

    @Test
    void addArgumentResolvers_registersSortAndPageableResolvers() {
        List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>();

        new WebConfig().addArgumentResolvers(resolvers);

        assertThat(resolvers).hasSize(2);
        assertThat(resolvers.get(0)).isInstanceOf(SortHandlerMethodArgumentResolver.class);
        assertThat(resolvers.get(1)).isInstanceOf(PageableHandlerMethodArgumentResolver.class);
    }

    @Test
    void addArgumentResolvers_appendsToExistingResolvers() {
        List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>();
        HandlerMethodArgumentResolver existing = new SortHandlerMethodArgumentResolver();
        resolvers.add(existing);

        new WebConfig().addArgumentResolvers(resolvers);

        assertThat(resolvers).hasSize(3);
        assertThat(resolvers.get(0)).isSameAs(existing);
    }
}
