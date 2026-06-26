package com.nicico.rasmio.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nicico.rasmio.model.CachedRasmioResponse;
import com.nicico.rasmio.repository.CachedRasmioResponseRepository;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import retrofit2.Call;
import retrofit2.Response;

@Slf4j
@Service
public class RasmioProxyService {

    public static final String DEFAULT_FIELDS_KEY = "__all__";
    private static final Set<String> RATE_LIMIT_HEADERS = Set.of("X-TodayLimit", "X-UsedToday", "X-TotalLimit", "X-UsedTotal");

    private final CachedRasmioResponseRepository repository;
    private final RasmioRetrofitClient rasmioClient;
    private final ObjectMapper objectMapper;

    public RasmioProxyService(CachedRasmioResponseRepository repository, RasmioRetrofitClient rasmioClient,
                              ObjectMapper objectMapper) {
        this.repository = repository;
        this.rasmioClient = rasmioClient;
        this.objectMapper = objectMapper;
    }

    public ProxyResult getCompanyInfo(String companyId, String fields, boolean refresh) {
        return getCachedOrFetch("company-info", companyId, fields, refresh, "/Company/%s/Info",
                rasmioClient.getCompanyInfo(companyId, normalizedFieldsQuery(fields)));
    }

    public ProxyResult getCompanyPeople(String companyId, String fields, boolean refresh) {
        return getCachedOrFetch("company-people", companyId, fields, refresh, "/Company/%s/People",
                rasmioClient.getCompanyPeople(companyId, normalizedFieldsQuery(fields)));
    }

    private ProxyResult getCachedOrFetch(String endpoint, String companyId, String fields, boolean refresh,
                                         String pathTemplate, Call<String> upstreamCall) {
        String fieldsKey = normalizeFields(fields);
        Optional<CachedRasmioResponse> cached = repository.findByEndpointAndCompanyIdAndRequestedFieldsKey(endpoint, companyId, fieldsKey);
        if (!refresh && cached.isPresent()) {
            return ProxyResult.cached(cached.get().getPayload(), cached.get().getHttpStatus(), cached.get().getRateLimitHeaders());
        }

        UpstreamResult upstream = fetchFromRasmio(upstreamCall);
        Document payload = parsePayload(upstream.body());
        CachedRasmioResponse response = cached.orElseGet(CachedRasmioResponse::new);
        Instant now = Instant.now();
        response.setEndpoint(endpoint);
        response.setCompanyId(companyId);
        response.setRequestedFieldsKey(fieldsKey);
        response.setUpstreamPath(pathTemplate.formatted(companyId));
        response.setPayload(payload);
        response.setHttpStatus(upstream.httpStatus());
        response.setRateLimitHeaders(upstream.rateLimitHeaders());
        if (response.getFetchedAt() == null) {
            response.setFetchedAt(now);
        }
        response.setUpdatedAt(now);
        repository.save(response);
        return ProxyResult.fetched(payload, response.getHttpStatus(), response.getRateLimitHeaders());
    }

    private UpstreamResult fetchFromRasmio(Call<String> upstreamCall) {
        try {
            Response<String> response = upstreamCall.execute();
            String body = response.isSuccessful() ? response.body() : errorBody(response);
            return new UpstreamResult(response.code(), body, extractRateLimitHeaders(response.headers()));
        } catch (IOException exception) {
            log.warn("Failed to fetch data from Rasmio", exception);
            return new UpstreamResult(HttpStatus.BAD_GATEWAY.value(),
                    "{\"status\":502,\"message\":\"Failed to fetch data from Rasmio\"}", Map.of());
        }
    }

    private String errorBody(Response<String> response) throws IOException {
        return response.errorBody() == null ? null : response.errorBody().string();
    }

    private Document parsePayload(String body) {
        if (!StringUtils.hasText(body)) {
            return new Document("data", null);
        }
        try {
            Object parsed = objectMapper.readValue(body, Object.class);
            if (parsed instanceof Map<?, ?> map) {
                return new Document((Map<String, Object>) map);
            }
            return new Document("data", parsed);
        } catch (JsonProcessingException exception) {
            return new Document("raw", body);
        }
    }

    static String normalizeFields(String fields) {
        if (!StringUtils.hasText(fields)) {
            return DEFAULT_FIELDS_KEY;
        }
        TreeSet<String> normalized = new TreeSet<>();
        Arrays.stream(fields.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .forEach(normalized::add);
        return normalized.isEmpty() ? DEFAULT_FIELDS_KEY : String.join(",", normalized);
    }

    private String normalizedFieldsQuery(String fields) {
        String fieldsKey = normalizeFields(fields);
        return DEFAULT_FIELDS_KEY.equals(fieldsKey) ? null : fieldsKey;
    }

    private Map<String, String> extractRateLimitHeaders(okhttp3.Headers headers) {
        Map<String, String> values = new LinkedHashMap<>();
        RATE_LIMIT_HEADERS.forEach(header -> {
            String value = headers.get(header);
            if (value != null) {
                values.put(header, value);
            }
        });
        return values;
    }

    private record UpstreamResult(int httpStatus, String body, Map<String, String> rateLimitHeaders) {
    }

    public record ProxyResult(Document payload, boolean fromCache, int httpStatus, Map<String, String> rateLimitHeaders) {
        static ProxyResult cached(Document payload, int httpStatus, Map<String, String> headers) {
            return new ProxyResult(payload, true, httpStatus, headers);
        }

        static ProxyResult fetched(Document payload, int httpStatus, Map<String, String> headers) {
            return new ProxyResult(payload, false, httpStatus, headers);
        }
    }
}
