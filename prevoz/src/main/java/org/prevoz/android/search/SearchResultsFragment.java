package org.prevoz.android.search;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.nineoldandroids.view.ViewHelper;

import org.prevoz.android.PrevozFragment;
import org.prevoz.android.R;
import org.prevoz.android.api.rest.RestSearchResults;
import org.prevoz.android.api.ApiClient;
import org.prevoz.android.auth.AuthenticationUtils;
import org.prevoz.android.events.Events;
import org.prevoz.android.model.City;
import org.prevoz.android.push.PushManager;
import org.prevoz.android.ui.ListDisappearAnimation;
import org.prevoz.android.ui.ListFlyupAnimator;
import org.prevoz.android.util.LocaleUtil;
import org.prevoz.android.util.PrevozActivity;
import org.prevoz.android.util.ViewUtils;

import java.util.Calendar;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.greenrobot.event.EventBus;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import rx.schedulers.Schedulers;
import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

import static com.nineoldandroids.view.ViewPropertyAnimator.animate;

public class SearchResultsFragment extends PrevozFragment implements Callback<RestSearchResults>
{
    @InjectView(R.id.search_results_list)
    protected StickyListHeadersListView resultList;
    @InjectView(R.id.search_results_noresults)
    protected TextView noResultsText;

    protected View searchNofityButtonContainer;
    protected View searchNotifyButton;
    protected ImageView searchNotifyButtonIcon;
    protected ProgressBar searchNotifyButtonProgress;
    protected TextView searchNofityButtonText;

    private View headerFragmentView;
    private StickyListHeadersAdapter adapter;

    // Needed to keep track of last searches
    // TODO: find a better solution
    // TODO: store all to instance state
    protected RestSearchResults results;
    protected boolean shouldShowNotificationButton = false;
    protected City lastFrom;
    protected City lastTo;
    protected Calendar lastDate;
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
        searchNofityButtonContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickNotificationButton();
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View views = inflater.inflate(R.layout.fragment_search_list, container, false);
        ButterKnife.inject(this, views);

        resultList.setDivider(null);
        resultList.setDividerHeight(0);
        resultList.addHeaderView(headerFragmentView, null, true);

        FragmentTransaction ft = getChildFragmentManager().beginTransaction();
        ft.replace(R.id.search_form, new SearchFragment());
        ft.commit();

        if (results == null)
            showHistory(false);
        else
            showResults(results);
        return views;
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

        if (restSearchResults == null || restSearchResults.results == null || restSearchResults.results.size() == 0)
        {
            results = null;
            ViewUtils.showMessage(getActivity(), R.string.search_no_results, true);
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
        Activity activity = getActivity();
        if (retrofitError.getResponse() != null && retrofitError.getResponse().getStatus() == 403) {
            if (activity != null) {
                Toast.makeText(activity, "Prijava ni več veljavna, odjavljam...", Toast.LENGTH_SHORT).show();
            }

            authUtils.logout().subscribeOn(Schedulers.io()).subscribe();
            ApiClient.setBearer(null);
            EventBus.getDefault().post(new Events.NewSearchEvent(lastFrom, lastTo, lastDate));
            return;
        }

        if (activity != null)
            ViewUtils.showMessage(activity, "Napaka med iskanjem, a internet deluje?", true);
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
        pushManager.setSubscriptionStatus(getActivity(), lastFrom, lastTo, lastDate, !pushManager.isSubscribed(lastFrom, lastTo, lastDate));
    }

    // TODO: Move to background
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
