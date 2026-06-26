package com.nicico.rasmio.repository;

import com.nicico.rasmio.model.CachedRasmioResponse;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CachedRasmioResponseRepository extends MongoRepository<CachedRasmioResponse, String> {

    Optional<CachedRasmioResponse> findByEndpointAndCompanyIdAndRequestedFieldsKey(String endpoint, String companyId, String requestedFieldsKey);
}
