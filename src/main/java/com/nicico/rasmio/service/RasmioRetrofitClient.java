package com.nicico.rasmio.service;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface RasmioRetrofitClient {

    @GET("Company/{companyId}/Info")
    Call<String> getCompanyInfo(@Path("companyId") String companyId, @Query("fields") String fields);

    @GET("Company/{companyId}/People")
    Call<String> getCompanyPeople(@Path("companyId") String companyId, @Query("fields") String fields);
}
