package com.nicico.rasmio.model;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.bson.Document;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
@org.springframework.data.mongodb.core.mapping.Document(collection = "rasmio_responses")
@CompoundIndex(name = "endpoint_company_fields_idx", def = "{'endpoint': 1, 'companyId': 1, 'requestedFieldsKey': 1}", unique = true)
public class CachedRasmioResponse {

    @Id
    private String id;
    private String endpoint;
    private String companyId;
    private String requestedFieldsKey;
    private String upstreamPath;
    private int httpStatus;
    private Instant fetchedAt;
    private Instant updatedAt;

    @Field("payload")
    private Document payload;

    private Map<String, String> rateLimitHeaders = new LinkedHashMap<>();
}
