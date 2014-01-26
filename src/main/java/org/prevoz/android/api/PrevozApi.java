package org.prevoz.android.api;


import org.prevoz.android.api.rest.RestSearchRequest;
import org.prevoz.android.api.rest.RestSearchResults;

import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.POST;

public interface PrevozApi
{
    @POST("/search/shares")
    public void search(@Body RestSearchRequest searchRequest, Callback<RestSearchResults> cb);
}
