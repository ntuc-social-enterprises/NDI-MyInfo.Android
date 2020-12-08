package sg.nedigital.myinfo.services

import com.google.gson.JsonElement
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface MyInfoService {
    @GET("person/{sub}/")
    fun getPerson(
        @Path("sub") sub: String,
        @Header("Authorization") auth: String,
        @Query("client_id") clientId: String,
        @Query("attributes") attributes: String
    ): Call<JsonElement>
}
