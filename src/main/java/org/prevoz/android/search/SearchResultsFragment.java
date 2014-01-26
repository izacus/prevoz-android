package org.prevoz.android.search;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.googlecode.androidannotations.annotations.EFragment;

import org.prevoz.android.R;
import org.prevoz.android.api.ApiClient;
import org.prevoz.android.api.rest.RestSearchRequest;
import org.prevoz.android.api.rest.RestSearchResults;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

@EFragment(R.layout.fragment_search_results)
public class SearchResultsFragment extends Fragment implements Callback<RestSearchResults>
{
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    public static final String PARAM_SEARCH_FROM = "SearchFrom";
    public static final String PARAM_SEARCH_TO = "SearchTo";
    public static final String PARAM_SEARCH_DATE = "SearchDate";

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        String from = args.getString(PARAM_SEARCH_FROM);
        String to = args.getString(PARAM_SEARCH_TO);
        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(args.getLong(PARAM_SEARCH_DATE));

        RestSearchRequest request = new RestSearchRequest(from, "SI", to, "SI", sdf.format(date.getTime()));
        ApiClient.getAdapter().search(request, this);
    }

    @Override
    public void success(RestSearchResults restSearchResults, Response response)
    {
        Log.d("Prevoz", "Response: " + response.getBody().toString());
    }

    @Override
    public void failure(RetrofitError retrofitError)
    {
        Log.d("Prevoz", "Response: " + retrofitError);
    }
}
