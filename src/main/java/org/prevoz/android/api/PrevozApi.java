package org.prevoz.android.api;


import org.prevoz.android.api.rest.*;

import retrofit.Callback;
import retrofit.http.*;

public interface PrevozApi
{
    @GET("/search/shares")
    public void search(@Query("f") String from, @Query("fc") String fromCountry, @Query("t") String to, @Query("tc") String toCountry, @Query("d") String date, Callback<RestSearchResults> cb);

    @GET("/carshare/{id}/")
    public void getRide(@Path("id") String id, Callback<RestRide> cb);

    @GET("/accounts/status/")
    public void getAccountStatus(Callback<RestAccountStatus> cb);

    @POST("/accounts/login/apikey")
    public void loginWithApiKey(@Body RestApiKey apiKey, Callback<RestAccountStatus> cb);

    @POST("/c2dm/register")
    public void setSubscriptionState(@Body RestPushSubscription subscription, Callback<RestPushStatus> cb);
}
