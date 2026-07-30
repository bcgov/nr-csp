package ca.bc.gov.nrs.csp.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.time.Month;

import static org.assertj.core.api.Assertions.assertThat;

class JacksonConfigTest {

    @Test
    void jacksonCustomizer_disablesWriteDatesAsTimestamps() {
        JsonMapperBuilderCustomizer customizer = new JacksonConfig().jacksonCustomizer();
        JsonMapper.Builder builder = JsonMapper.builder();

        customizer.customize(builder);

        assertThat(builder.isEnabled(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)).isFalse();
    }

    @Test
    void customizedMapper_serializesDatesAsIsoStrings() {
        JsonMapperBuilderCustomizer customizer = new JacksonConfig().jacksonCustomizer();
        JsonMapper.Builder builder = JsonMapper.builder();
        customizer.customize(builder);
        JsonMapper mapper = builder.build();

        String json = mapper.writeValueAsString(LocalDate.of(2024, Month.JANUARY, 15));

        assertThat(json).isEqualTo("\"2024-01-15\"");
    }
}
