package org.prevoz.android.search;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ListView;

import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.EFragment;
import com.googlecode.androidannotations.annotations.InstanceState;
import com.googlecode.androidannotations.annotations.ViewById;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.view.ViewHelper;

import org.prevoz.android.R;
import org.prevoz.android.api.ApiClient;
import org.prevoz.android.api.rest.RestSearchRequest;
import org.prevoz.android.api.rest.RestSearchResults;
import org.prevoz.android.api.rest.RestSearchRide;
import org.prevoz.android.events.Events;
import org.prevoz.android.ui.ListDisappearAnimation;
import org.prevoz.android.ui.ListFlyupAnimator;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import de.greenrobot.event.EventBus;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;


@EFragment(R.layout.fragment_search_list)
public class SearchResultsFragment extends Fragment implements Callback<RestSearchResults>
{
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    public static final String PARAM_SEARCH_FROM = "SearchFrom";
    public static final String PARAM_SEARCH_TO = "SearchTo";
    public static final String PARAM_SEARCH_DATE = "SearchDate";

    @ViewById(R.id.search_results_list)
    protected ListView resultList;

    @InstanceState
    protected RestSearchResults results;

    @InstanceState
    protected boolean searching;

    private View headerFragmentView;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        headerFragmentView = getLayoutInflater(savedInstanceState).inflate(R.layout.header_search_form, null, false);
    }

    @AfterViews
    protected void afterViews()
    {
        searching = false;
        resultList.addHeaderView(headerFragmentView, null, true);

        if (results == null)
        {
            resultList.setAdapter(new SearchResultsAdapter(getActivity(), new ArrayList<RestSearchRide>()));
        }
        else
        {
            showResults(results, false);
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void success(RestSearchResults restSearchResults, Response response)
    {
        searching = false;
        if (getActivity() == null) return;
        Log.d("Prevoz", "Response: " + response.getBody().toString());
        results = restSearchResults;
        showResults(results, true);
    }

    @Override
    public void failure(RetrofitError retrofitError)
    {
        searching = false;
        Log.d("Prevoz", "Response: " + retrofitError);
    }

    private void showResults(RestSearchResults results, boolean animate)
    {
        resultList.setAdapter(new SearchResultsAdapter(getActivity(), results.results));
        new ListFlyupAnimator(resultList).animate();
    }

    public void onEventMainThread(Events.NewSearchEvent e)
    {
        if (searching)
            return;

        if (resultList.getAdapter() != null)
        {
            new ListDisappearAnimation(resultList).animate();
        }

        Log.d("Prevoz", "Starting search for " + e.from + "-" + e.to + " [" + e.date.toString() + "]");
        ApiClient.getAdapter().search(e.from, "SI", e.to, "SI", this);
        searching = true;
    }
}
