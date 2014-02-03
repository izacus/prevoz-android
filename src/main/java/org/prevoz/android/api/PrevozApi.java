package org.prevoz.android.api;


import org.prevoz.android.api.rest.RestSearchResults;

import retrofit.Callback;
import retrofit.http.GET;
import retrofit.http.Query;

public interface PrevozApi
{
    @GET("/search/shares")
    public void search(@Query("f") String from, @Query("fc") String fromCountry, @Query("t") String to, @Query("tc") String toCountry, Callback<RestSearchResults> cb);
}
