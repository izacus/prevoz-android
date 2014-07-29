package org.prevoz.android.search;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.nineoldandroids.view.ViewHelper;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.InstanceState;
import org.androidannotations.annotations.ViewById;
import org.prevoz.android.PrevozApplication;
import org.prevoz.android.R;
import org.prevoz.android.api.ApiClient;
import org.prevoz.android.api.rest.RestRide;
import org.prevoz.android.api.rest.RestSearchResults;
import org.prevoz.android.events.Events;
import org.prevoz.android.model.City;
import org.prevoz.android.model.Route;
import org.prevoz.android.push.PushManager;
import org.prevoz.android.ride.RideInfoFragment;
import org.prevoz.android.ui.ListDisappearAnimation;
import org.prevoz.android.ui.ListFlyupAnimator;
import org.prevoz.android.util.LocaleUtil;

import java.util.Calendar;

import de.greenrobot.event.EventBus;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

import static com.nineoldandroids.view.ViewPropertyAnimator.animate;


@EFragment(R.layout.fragment_search_list)
public class SearchResultsFragment extends Fragment implements Callback<RestSearchResults>
{
    @ViewById(R.id.search_results_list)
    protected StickyListHeadersListView resultList;
    @ViewById(R.id.search_results_noresults)
    protected TextView noResultsText;

    protected View searchNofityButtonContainer;
    protected View searchNotifyButton;
    protected ImageView searchNotifyButtonIcon;
    protected ProgressBar searchNotifyButtonProgress;
    protected TextView searchNofityButtonText;

    @InstanceState
    protected RestSearchResults results;

    @InstanceState
    protected boolean shouldShowNotificationButton = false;

    private View headerFragmentView;
    private StickyListHeadersAdapter adapter;

    @Bean
    protected PushManager pushManager;

    // Needed to keep track of last searches
    // TODO: find a better solution
    @InstanceState
    protected City lastFrom;
    @InstanceState
    protected City lastTo;
    @InstanceState
    protected Calendar lastDate;
    @InstanceState
    protected int[] highlightRides;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        headerFragmentView = getLayoutInflater(savedInstanceState).inflate(R.layout.header_search_form, null, false);
        searchNofityButtonContainer = headerFragmentView.findViewById(R.id.search_notify_button_container);
        searchNofityButtonText = (TextView) headerFragmentView.findViewById(R.id.search_notify_button_text);
        searchNotifyButtonIcon = (ImageView) headerFragmentView.findViewById(R.id.search_notify_button_icon);
        searchNotifyButtonProgress = (ProgressBar) headerFragmentView.findViewById(R.id.search_notify_button_progress);
        searchNotifyButton = headerFragmentView.findViewById(R.id.search_notify_button);

        searchNotifyButton.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                if (event.getAction() == MotionEvent.ACTION_UP)
                {
                    clickNotificationButton();
                }

                return false;
            }
        });
    }

    @AfterViews
    protected void afterViews()
    {
        resultList.setDivider(null);
        resultList.setDividerHeight(0);
        resultList.addHeaderView(headerFragmentView, null, true);

        FragmentTransaction ft = getChildFragmentManager().beginTransaction();
        ft.replace(R.id.search_form, new SearchFragment_());
        ft.commit();

        if (results == null)
        {
            showHistory(false);
        }
        else
        {
            showResults(results);
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        EventBus.getDefault().registerSticky(this);
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
            showResults(results);
        }

        EventBus.getDefault().post(new Events.SearchComplete());
    }

    @Override
    public void failure(RetrofitError retrofitError)
    {
        Log.d("Prevoz", "Response: " + retrofitError);

        Activity activity = getActivity();
        if (activity != null)
            Toast.makeText(activity, "Napaka pri iskanju - ali je na voljo internetna povezava?", Toast.LENGTH_SHORT).show();
        EventBus.getDefault().post(new Events.SearchComplete());
    }

    private void showResults(RestSearchResults results)
    {
        if (resultList.getAdapter() == null || !(resultList.getAdapter() instanceof SearchResultsAdapter))
        {
            adapter = new SearchResultsAdapter(getActivity(), results.results, highlightRides);
            resultList.setAdapter(adapter);
        }
        else
        {
            final SearchResultsAdapter adapter = (SearchResultsAdapter) resultList.getAdapter();
            adapter.setResults(results.results, highlightRides);

            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
                resultList.smoothScrollToPosition(1);

        }

        resultList.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                RestRide ride = (RestRide) adapter.getItem(position - 1);
                RideInfoFragment rideInfo = RideInfoFragment.newInstance(ride);
                FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
                ft.add(rideInfo, null);
                ft.commitAllowingStateLoss();
            }
        });

        if (results.results != null && results.results.size() > 0)
        {
            showNotificationsButton();
            new ListFlyupAnimator(resultList).animate();
        }
        else
        {
            noResultsText.setVisibility(View.VISIBLE);
            ViewHelper.setAlpha(noResultsText, 0.0f);
            animate(noResultsText).alpha(1.0f).setDuration(200).start();
        }
    }

    private void showNotificationsButton()
    {
        searchNofityButtonContainer.clearAnimation();

        if (!shouldShowNotificationButton ||
             searchNofityButtonContainer.getVisibility() == View.VISIBLE ||
            !pushManager.isPushAvailable())
        {
            ViewHelper.setAlpha(searchNofityButtonContainer, 1.0f);
            return;
        }

        // Show notifications button

        ViewHelper.setAlpha(searchNofityButtonContainer, 0.0f);
        updateNotificationButtonText();
        searchNofityButtonContainer.setVisibility(View.VISIBLE);

        animate(searchNofityButtonContainer).alpha(1.0f).setDuration(200).setListener(null);
    }

    private void updateNotificationButtonText()
    {
        if (pushManager.isSubscribed(lastFrom, lastTo, lastDate))
        {
            searchNotifyButtonIcon.setImageResource(R.drawable.ic_action_cancel);
            searchNofityButtonText.setText("Prenehaj z obveščanjem");
        }
        else
        {
            searchNotifyButtonIcon.setImageResource(R.drawable.ic_action_bell);
            searchNofityButtonText.setText("Obveščaj me o novih prevozih");
        }
    }

    private void hideNotificationsButton()
    {
        if (searchNofityButtonContainer.getVisibility() == View.GONE)
            return;

        searchNofityButtonContainer.clearAnimation();
        animate(searchNofityButtonContainer).alpha(0.0f).setDuration(200).setListener(new com.nineoldandroids.animation.AnimatorListenerAdapter()
        {
            @Override
            public void onAnimationEnd(com.nineoldandroids.animation.Animator animation)
            {
                searchNofityButtonContainer.setVisibility(View.GONE);
            }
        });
    }

    private void clickNotificationButton()
    {
        searchNotifyButton.setEnabled(false);
        searchNotifyButtonIcon.setVisibility(View.INVISIBLE);
        searchNotifyButtonProgress.setVisibility(View.VISIBLE);
        pushManager.setSubscriptionStatus(lastFrom, lastTo, lastDate, !pushManager.isSubscribed(lastFrom, lastTo, lastDate));
    }

    @Background
    protected void showHistory(final boolean animate)
    {
        final Activity activity = getActivity();
        if (activity == null) return;

        adapter = new SearchHistoryAdapter(activity);
        activity.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                if (animate && resultList.getAdapter() != null)
                {
                    hideNotificationsButton();
                    new ListDisappearAnimation(resultList).animate();
                }

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

                if (animate)
                    new ListFlyupAnimator(resultList).animate();
            }
        });
    }

    public void onEventMainThread(Events.NewSearchEvent e)
    {
        EventBus.getDefault().removeStickyEvent(e);
        noResultsText.setVisibility(View.INVISIBLE);

        if (resultList.getAdapter() != null)
        {
            hideNotificationsButton();
            new ListDisappearAnimation(resultList).animate();
        }

        shouldShowNotificationButton = !(e.from == null || e.to == null);
        ApiClient.getAdapter().search(e.from == null ? null : e.from.getDisplayName(),
                                      e.from == null ? null : e.from.getCountryCode(),
                                      e.to == null ? null : e.to.getDisplayName(),
                                      e.to == null ? null : e.to.getCountryCode(),
                                      LocaleUtil.getSimpleDateFormat("yyyy-MM-dd").format(e.date.getTime()), this);

        lastFrom = e.from;
        lastTo = e.to;
        lastDate = e.date;
        highlightRides = e.rideIds;
    }

    public void onEventMainThread(Events.NotificationSubscriptionStatusChanged e)
    {
        updateNotificationButtonText();
        searchNotifyButton.setEnabled(true);
        searchNotifyButtonIcon.setVisibility(View.VISIBLE);
        searchNotifyButtonProgress.setVisibility(View.INVISIBLE);
    }

    public void onEventMainThread(Events.RideDeleted e)
    {
        if (adapter != null && adapter instanceof SearchResultsAdapter)
            ((SearchResultsAdapter) adapter).removeRide(e.id);
    }

    public void onEventMainThread(Events.ClearSearchEvent e)
    {
        showHistory(true);
    }

    public boolean showingResults()
    {
        return (adapter instanceof SearchResultsAdapter);
    }
}
