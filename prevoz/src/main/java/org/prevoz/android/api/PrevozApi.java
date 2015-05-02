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
import rx.Observable;

public interface PrevozApi
{
    String FULL_STATE_AVAILABLE = "available";
    String FULL_STATE_FULL = "full";

    @GET("/api/search/shares/")
    Observable<RestSearchResults> search(@Query("f") String from,
                @Query("fc") String fromCountry,
                @Query("t") String to,
                @Query("tc") String toCountry,
                @Query("d") String date);

    @GET("/api/accounts/status/")
    Observable<RestAccountStatus> getAccountStatus();

    @FormUrlEncoded
    @POST("/api/c2dm/register/")
    Observable<RestPushStatus> setSubscriptionState(@Field("registration_id") String registrationId,
                              @Field("from") String form,
                              @Field("fromcountry") String fromCountry,
                              @Field("to") String to,
                              @Field("tocountry") String toCountry,
                              @Field("date") String date,
                              @Field("action") String action);

    @POST("/api/carshare/create/")
    void postRide(@Body RestRide ride, Callback<RestStatus> cb);

    @GET("/api/carshare/list/")
    Observable<RestSearchResults> getMyRides();

    @GET("/api/search/bookmarks/")
    Observable<RestSearchResults> getBookmarkedRides();

    @DELETE("/api/carshare/delete/{id}/")
    void deleteRide(@Path("id") String id, Callback<Response> cb);

    @FormUrlEncoded
    @POST("/api/carshare/full/{id}/")
    void setFull(@Path("id") String id, @Field("state") String state, Callback<Response> cb);

    @FormUrlEncoded
    @POST("/oauth2/access_token/")
    Observable<RestAuthTokenResponse> getAccessToken(@Field("grant_type") String grantType,
                                                     @Field("client_id") String clientId,
                                                     @Field("client_secret") String clientSecret,
                                                     @Field("code") String code);
    @FormUrlEncoded
    @POST("/api/carshare/bookmark/{id}/")
    void setRideBookmark(@Path("id") String id, @Field("state") String state, Callback<Response> cb);
}
