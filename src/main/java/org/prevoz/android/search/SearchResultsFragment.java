package org.prevoz.android.search;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.*;

import com.googlecode.androidannotations.annotations.*;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.view.ViewHelper;

import org.prevoz.android.R;
import org.prevoz.android.api.ApiClient;
import org.prevoz.android.api.rest.RestSearchRequest;
import org.prevoz.android.api.rest.RestSearchResults;
import org.prevoz.android.api.rest.RestSearchRide;
import org.prevoz.android.events.Events;
import org.prevoz.android.model.Route;
import org.prevoz.android.push.PushManager;
import org.prevoz.android.ride.RideInfoFragment;
import org.prevoz.android.ui.ListDisappearAnimation;
import org.prevoz.android.ui.ListFlyupAnimator;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import de.greenrobot.event.EventBus;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;


@EFragment(R.layout.fragment_search_list)
public class SearchResultsFragment extends Fragment implements Callback<RestSearchResults>
{
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    @ViewById(R.id.search_results_list)
    protected StickyListHeadersListView resultList;

    protected View searchNofityButton;

    @InstanceState
    protected RestSearchResults results;

    @InstanceState
    protected boolean shouldShowNotificationButton = false;

    private View headerFragmentView;
    private StickyListHeadersAdapter adapter;

    @Bean
    protected PushManager pushManager;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        headerFragmentView = getLayoutInflater(savedInstanceState).inflate(R.layout.header_search_form, null, false);
        searchNofityButton = headerFragmentView.findViewById(R.id.search_notify_button_container);
    }

    @AfterViews
    protected void afterViews()
    {
        resultList.setDivider(null);
        resultList.setDividerHeight(0);
        resultList.addHeaderView(headerFragmentView, null, true);

        if (results == null)
        {
            adapter = new SearchHistoryAdapter(getActivity());
            resultList.setAdapter(adapter);
            resultList.setOnItemClickListener(new AdapterView.OnItemClickListener()
            {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id)
                {
                    Route route = (Route) adapter.getItem(position - 1);
                    EventBus.getDefault().post(new Events.SearchFillWithRoute(route));
                }
            });
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
        if (getActivity() == null) return;
        Log.d("Prevoz", "Response: " + response.getBody().toString());

        if (restSearchResults == null || restSearchResults.results == null)
        {
            results = null;
            Toast.makeText(getActivity(), R.string.search_no_results, Toast.LENGTH_SHORT).show();
        }
        else
        {
            results = restSearchResults;
            showResults(results, true);
        }

        EventBus.getDefault().post(new Events.SearchComplete());
    }

    @Override
    public void failure(RetrofitError retrofitError)
    {
        Log.d("Prevoz", "Response: " + retrofitError);
        EventBus.getDefault().post(new Events.SearchComplete());
    }

    private void showResults(RestSearchResults results, boolean animate)
    {
        if (resultList.getAdapter() == null || !(resultList.getAdapter() instanceof SearchResultsAdapter))
        {
            adapter = new SearchResultsAdapter(getActivity(), results.results);
            resultList.setAdapter(adapter);
        }
        else
        {
            final SearchResultsAdapter adapter = (SearchResultsAdapter) resultList.getAdapter();
            adapter.setResults(results.results);

            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
                resultList.smoothScrollToPosition(1);

        }

        resultList.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                RestSearchRide ride = (RestSearchRide) adapter.getItem(position - 1);
                RideInfoFragment rideInfo = RideInfoFragment.newInstance(ride);
                rideInfo.show(getActivity().getSupportFragmentManager(), "RideInfo");
            }
        });

        showNotificationsButton();
        new ListFlyupAnimator(resultList).animate();
    }

    private void showNotificationsButton()
    {
        if (!shouldShowNotificationButton ||
             searchNofityButton.getVisibility() == View.VISIBLE ||
            !pushManager.isPushAvailable())
        {
            return;
        }

        // Show notifications button
        searchNofityButton.clearAnimation();
        searchNofityButton.setAlpha(0.0f);
        searchNofityButton.setVisibility(View.VISIBLE);
        searchNofityButton.animate().alpha(1.0f).setDuration(200).setListener(null);
    }

    private void hideNotificationsButton()
    {
        if (searchNofityButton.getVisibility() == View.GONE)
            return;

        searchNofityButton.clearAnimation();
        searchNofityButton.animate().alpha(0.0f).setListener(new AnimatorListenerAdapter()
        {
            @Override
            public void onAnimationEnd(Animator animation)
            {
                searchNofityButton.setVisibility(View.GONE);
            }
        }).setDuration(200).start();
    }

    public void onEventMainThread(Events.NewSearchEvent e)
    {
        if (resultList.getAdapter() != null)
        {
            hideNotificationsButton();
            new ListDisappearAnimation(resultList).animate();
        }

        Log.d("Prevoz", "Starting search for " + e.from + "-" + e.to + " [" + e.date.toString() + "]");

        shouldShowNotificationButton = !(e.from == null || e.to == null);
        ApiClient.getAdapter().search(e.from == null ? null : e.from.getDisplayName(),
                                      e.from == null ? null : e.from.getCountryCode(),
                                      e.to == null ? null : e.to.getDisplayName(),
                                      e.to == null ? null : e.to.getCountryCode(),
                                      sdf.format(e.date.getTime()), this);
    }
}
