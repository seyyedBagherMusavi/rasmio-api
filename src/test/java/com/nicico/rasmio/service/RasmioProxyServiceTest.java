package com.nicico.rasmio.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RasmioProxyServiceTest {

    @Test
    void normalizeFieldsSortsAndDeduplicatesRequestedFields() {
        assertThat(RasmioProxyService.normalizeFields(" title, id,title , registrationNo "))
                .isEqualTo("id,registrationNo,title");
    }

    @Test
    void normalizeFieldsUsesDefaultKeyForBlankRequests() {
        assertThat(RasmioProxyService.normalizeFields("  "))
                .isEqualTo(RasmioProxyService.DEFAULT_FIELDS_KEY);
    }
}
