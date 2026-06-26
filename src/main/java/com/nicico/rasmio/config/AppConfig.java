package com.nicico.rasmio.config;

import com.nicico.rasmio.service.RasmioRetrofitClient;
import okhttp3.OkHttpClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

@Configuration
@EnableConfigurationProperties(RasmioProperties.class)
public class AppConfig {

    @Bean
    OkHttpClient rasmioOkHttpClient(RasmioProperties properties) {
        return new OkHttpClient.Builder()
                .connectTimeout(properties.getConnectTimeout())
                .readTimeout(properties.getReadTimeout())
                .addInterceptor(chain -> {
                    var requestBuilder = chain.request().newBuilder();
                    if (StringUtils.hasText(properties.getApiKey())) {
                        requestBuilder.header("X-Key", properties.getApiKey());
                    }
                    return chain.proceed(requestBuilder.build());
                })
                .build();
    }

    @Bean
    RasmioRetrofitClient rasmioRetrofitClient(OkHttpClient rasmioOkHttpClient, RasmioProperties properties) {
        return new Retrofit.Builder()
                .baseUrl(normalizedBaseUrl(properties.getBaseUrl()))
                .client(rasmioOkHttpClient)
                .addConverterFactory(ScalarsConverterFactory.create())
                .build()
                .create(RasmioRetrofitClient.class);
    }

    private String normalizedBaseUrl(String baseUrl) {
        return baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
    }
}
