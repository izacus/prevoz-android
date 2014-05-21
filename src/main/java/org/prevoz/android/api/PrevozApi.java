package org.prevoz.android.api;

import org.prevoz.android.api.rest.*;
import retrofit.Callback;
import retrofit.client.Response;
import retrofit.http.*;

import java.util.List;

public interface PrevozApi
{
    @GET("/search/shares/")
    public void search(@Query("f")  String from,
                       @Query("fc") String fromCountry,
                       @Query("t")  String to,
                       @Query("tc") String toCountry,
                       @Query("d")  String date,
                       Callback<RestSearchResults> cb);

    @GET("/carshare/{id}/")
    public void getRide(@Path("id") String id, Callback<RestRide> cb);

    @GET("/accounts/status/")
    public void getAccountStatus(Callback<RestAccountStatus> cb);

    @POST("/accounts/login/apikey/")
    public void loginWithApiKey(@Body RestApiKey apiKey, Callback<RestAccountStatus> cb);

    @FormUrlEncoded
    @POST("/c2dm/register/")
    public void setSubscriptionState(@Field("registration_id") String registrationId,
                                     @Field("from") String form,
                                     @Field("fromcountry") String fromCountry,
                                     @Field("to") String to,
                                     @Field("tocountry") String toCountry,
                                     @Field("date") String date,
                                     @Field("action") String action, Callback<RestPushStatus> cb);

    @POST("/carshare/create/")
    public void postRide(@Body RestRide ride, Callback<Response> cb);

    @GET("/carshare/list/")
    public void getMyRides(Callback<RestSearchResults> cb);
}
