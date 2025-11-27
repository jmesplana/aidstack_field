package com.example.myapplication.gdacs

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface GdacsApiService {
    @GET("gdacsapi/api/events/geteventlist/SEARCH")
    suspend fun getDisasters(
        @Query("fromdate") fromDate: String? = null,
        @Query("todate") toDate: String? = null,
        @Query("alertlevel") alertLevel: String? = null,
        @Query("eventlist") eventList: String? = null
    ): GdacsResponse
}

object GdacsApi {
    private const val BASE_URL = "https://www.gdacs.org/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi).asLenient())
        .build()

    val service: GdacsApiService = retrofit.create(GdacsApiService::class.java)
}
