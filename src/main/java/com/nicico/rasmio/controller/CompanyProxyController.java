package com.nicico.rasmio.controller;

import com.nicico.rasmio.service.RasmioProxyService;
import com.nicico.rasmio.service.RasmioProxyService.ProxyResult;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
public class CompanyProxyController {

    private final RasmioProxyService proxyService;

    public CompanyProxyController(RasmioProxyService proxyService) {
        this.proxyService = proxyService;
    }

    @GetMapping("/api/company/{companyId}/info")
    ResponseEntity<?> getCompanyInfo(@PathVariable String companyId,
                                     @RequestParam(required = false) String fields,
                                     @RequestParam(defaultValue = "false") boolean refresh) {
        return toResponse(proxyService.getCompanyInfo(companyId, fields, refresh));
    }

    @GetMapping("/api/company/{companyId}/people")
    ResponseEntity<?> getCompanyPeople(@PathVariable String companyId,
                                       @RequestParam(required = false) String fields,
                                       @RequestParam(defaultValue = "false") boolean refresh) {
        return toResponse(proxyService.getCompanyPeople(companyId, fields, refresh));
    }

    private ResponseEntity<?> toResponse(ProxyResult result) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Cache", result.fromCache() ? "HIT" : "MISS");
        for (Map.Entry<String, String> entry : result.rateLimitHeaders().entrySet()) {
            headers.set(entry.getKey(), entry.getValue());
        }
        return ResponseEntity.status(HttpStatusCode.valueOf(result.httpStatus())).headers(headers).body(result.payload());
    }
}
