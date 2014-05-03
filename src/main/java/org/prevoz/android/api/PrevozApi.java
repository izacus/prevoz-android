package org.prevoz.android.api;


import org.prevoz.android.api.rest.RestAccountStatus;
import org.prevoz.android.api.rest.RestRide;
import org.prevoz.android.api.rest.RestSearchResults;

import retrofit.Callback;
import retrofit.http.GET;
import retrofit.http.Path;
import retrofit.http.Query;

public interface PrevozApi
{
    @GET("/search/shares")
    public void search(@Query("f") String from, @Query("fc") String fromCountry, @Query("t") String to, @Query("tc") String toCountry, @Query("d") String date, Callback<RestSearchResults> cb);

    @GET("/carshare/{id}/")
    public void getRide(@Path("id") String id, Callback<RestRide> cb);

    @GET("/accounts/status/")
    public void getAccountStatus(Callback<RestAccountStatus> cb);
}
