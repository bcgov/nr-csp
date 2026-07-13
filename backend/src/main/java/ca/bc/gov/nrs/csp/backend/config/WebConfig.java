package ca.bc.gov.nrs.csp.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.SortHandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Registers Spring Data's web argument resolvers so controllers can accept Pageable / Sort
 * parameters bound from query string (?page=&size=&sort=). The Spring Boot auto-configuration
 * that normally wires these in is gated on a spring-boot-starter-data-* dependency, which this
 * project does not use (it uses raw JDBC), so we register them here explicitly.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    // Hard ceiling on client-supplied page size: without this a caller can request
    // ?size=1000000 and force an unbounded Oracle fetch. @PageableDefault only sets
    // the default, not the maximum, so the cap must live on the resolver.
    private static final int MAX_PAGE_SIZE = 200;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new SortHandlerMethodArgumentResolver());
        PageableHandlerMethodArgumentResolver pageableResolver =
                new PageableHandlerMethodArgumentResolver(new SortHandlerMethodArgumentResolver());
        pageableResolver.setMaxPageSize(MAX_PAGE_SIZE);
        resolvers.add(pageableResolver);
    }
}
