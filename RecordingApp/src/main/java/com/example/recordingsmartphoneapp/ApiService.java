package com.example.recordingsmartphoneapp;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

interface ApiService {
    @Multipart
    @POST("/upload")
    Call<ResponseBody> postVideo(@Part MultipartBody.Part video, @Part("upload") RequestBody name);
}