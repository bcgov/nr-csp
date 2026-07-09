package ca.bc.gov.nrs.csp.backend.util;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ToStringUtilsTest {

    @Test
    void toJson_serializesObjectAsIndentedJson() {
        String json = ToStringUtils.toJson(Map.of("key", "value"));

        assertThat(json)
                .startsWith("{")
                .contains("\"key\"")
                .contains("\"value\"")
                // INDENT_OUTPUT is enabled, so the output is pretty-printed across lines
                .contains(System.lineSeparator());
    }

    @Test
    void toJson_serializesRecordFields() {
        record Sample(String name, int count) {}

        String json = ToStringUtils.toJson(new Sample("widget", 3));

        assertThat(json)
                .contains("\"name\"")
                .contains("\"widget\"")
                .contains("\"count\"")
                .contains("3");
    }

    @Test
    void toJson_returnsNullLiteral_forNullInput() {
        assertThat(ToStringUtils.toJson(null)).isEqualTo("null");
    }

    @Test
    void toJson_fallsBackToToString_whenSerializationFails() {
        // Jackson fails on empty beans by default (FAIL_ON_EMPTY_BEANS),
        // so a plain Object triggers the catch block and toString() fallback.
        Object unserializable = new Object();

        String result = ToStringUtils.toJson(unserializable);

        assertThat(result)
                .isEqualTo(unserializable.toString())
                .startsWith("java.lang.Object@");
    }
}
