package org.prevoz.android.api;

import org.prevoz.android.api.rest.RestAccountStatus;
import org.prevoz.android.api.rest.RestApiKey;
import org.prevoz.android.api.rest.RestAuthTokenResponse;
import org.prevoz.android.api.rest.RestPushStatus;
import org.prevoz.android.api.rest.RestRide;
import org.prevoz.android.api.rest.RestSearchResults;
import org.prevoz.android.api.rest.RestStatus;

import retrofit.Callback;
import retrofit.client.Response;
import retrofit.http.Body;
import retrofit.http.DELETE;
import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Path;
import retrofit.http.Query;

public interface PrevozApi
{
    @GET("/api/search/shares/")
    public void search(@Query("f")  String from,
                       @Query("fc") String fromCountry,
                       @Query("t")  String to,
                       @Query("tc") String toCountry,
                       @Query("d")  String date,
                       Callback<RestSearchResults> cb);

    @GET("/api/carshare/{id}/")
    public void getRide(@Path("id") String id, Callback<RestRide> cb);

    @GET("/api/accounts/status/")
    public RestAccountStatus getAccountStatus();

    @POST("/api/accounts/login/apikey/")
    public void loginWithApiKey(@Body RestApiKey apiKey, Callback<RestAccountStatus> cb);

    @FormUrlEncoded
    @POST("/api/c2dm/register/")
    public void setSubscriptionState(@Field("registration_id") String registrationId,
                                     @Field("from") String form,
                                     @Field("fromcountry") String fromCountry,
                                     @Field("to") String to,
                                     @Field("tocountry") String toCountry,
                                     @Field("date") String date,
                                     @Field("action") String action, Callback<RestPushStatus> cb);

    @POST("/api/carshare/create/")
    public void postRide(@Body RestRide ride, Callback<RestStatus> cb);

    @GET("/api/carshare/list/")
    public void getMyRides(Callback<RestSearchResults> cb);

    @DELETE("/api/carshare/delete/{id}/")
    public void deleteRide(@Path("id") String id, Callback<Response> cb);

    @FormUrlEncoded
    @POST("/oauth2/access_token/")
    public RestAuthTokenResponse getAccessToken(@Field("grant_type") String grantType,
                                                @Field("client_id") String clientId,
                                                @Field("client_secret") String clientSecret,
                                                @Field("code") String code);
}
