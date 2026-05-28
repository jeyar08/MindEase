package com.jeyar.mindease.network;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface GeminiApi {

    @POST("v1beta/models/{model}:generateContent")
    Call<GeminiResponse> generateContent(
            @Path("model") String model,
            @Query("key") String apiKey,
            @Body GeminiRequest request
    );
}
