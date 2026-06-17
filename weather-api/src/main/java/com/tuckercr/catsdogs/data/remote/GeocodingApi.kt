package com.tuckercr.catsdogs.data.remote

import com.tuckercr.catsdogs.data.remote.dto.GeocodingDirectDto
import retrofit2.http.GET
import retrofit2.http.Query

interface GeocodingApi {

    @GET("geo/1.0/direct")
    suspend fun directSearch(
        @Query("q") query: String,
        @Query("limit") limit: Int = 8,
        @Query("appid") apiKey: String,
    ): List<GeocodingDirectDto>
}
