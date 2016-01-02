package org.prevoz.android.search;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;

import org.prevoz.android.PrevozFragment;
import org.prevoz.android.R;
import org.prevoz.android.api.ApiClient;
import org.prevoz.android.api.rest.RestRide;
import org.prevoz.android.api.rest.RestSearchResults;
import org.prevoz.android.events.Events;
import org.prevoz.android.model.City;
import org.prevoz.android.ui.ListDisappearAnimation;
import org.prevoz.android.ui.ListFlyupAnimator;
import org.prevoz.android.util.LocaleUtil;
import org.prevoz.android.util.ViewUtils;
import org.threeten.bp.LocalDate;
import org.threeten.bp.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.greenrobot.event.EventBus;
import icepick.Icepick;
import icepick.Icicle;
import retrofit.RetrofitError;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;


public class SearchResultsFragment extends PrevozFragment
{
    @InjectView(R.id.search_results_list)
    protected StickyListHeadersListView resultList;

    protected View searchNotifyButtonContainer;
    protected View searchNotifyButton;
    protected ImageView searchNotifyButtonIcon;
    protected ProgressBar searchNotifyButtonProgress;
    protected TextView searchNofityButtonText;

    private View headerFragmentView;
    private StickyListHeadersAdapter adapter;

    // Needed to keep track of last searches
    // TODO: find a better solution
    @Icicle protected RestSearchResults results;
    @Icicle protected boolean shouldShowNotificationButton = false;
    @Icicle protected City lastFrom;
    @Icicle protected City lastTo;
    @Icicle protected LocalDate lastDate;
    @Icicle protected int[] highlightRides;

    @SuppressLint("InflateParams")
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Icepick.restoreInstanceState(this, savedInstanceState);

        headerFragmentView = getLayoutInflater(savedInstanceState).inflate(R.layout.header_search_form, null, false);
        searchNotifyButtonContainer = headerFragmentView.findViewById(R.id.search_notify_button_container);
        searchNofityButtonText = (TextView) headerFragmentView.findViewById(R.id.search_notify_button_text);
        searchNotifyButtonIcon = (ImageView) headerFragmentView.findViewById(R.id.search_notify_button_icon);
        searchNotifyButtonProgress = (ProgressBar) headerFragmentView.findViewById(R.id.search_notify_button_progress);
        searchNotifyButton = headerFragmentView.findViewById(R.id.search_notify_button);
        searchNotifyButtonContainer.setOnClickListener(v -> clickNotificationButton());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View views = inflater.inflate(R.layout.fragment_search_list, container, false);
        ButterKnife.inject(this, views);
        resultList.addHeaderView(headerFragmentView, null, true);

        if (getChildFragmentManager().findFragmentByTag("SearchFragment") == null) {
            FragmentTransaction ft = getChildFragmentManager().beginTransaction();
            ft.replace(R.id.search_form, new SearchFragment(), "SearchFragment");
            ft.commit();
        }

        if (results == null)
            showHistory(false);
        else
            showResults(results);
        return views;
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().registerSticky(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    public void success(RestSearchResults restSearchResults)
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
        }

        showResults(results);
        EventBus.getDefault().post(new Events.SearchComplete());
    }

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

    private void showResults(@Nullable RestSearchResults results)
    {
        List<RestRide> restResults = results == null ? new ArrayList<>() : results.results;

        if (resultList.getAdapter() == null || !(resultList.getAdapter() instanceof SearchResultsAdapter))
        {
            adapter = new SearchResultsAdapter(getActivity(), database, restResults, highlightRides);
            resultList.setAdapter(adapter);
        }
        else
        {
            final SearchResultsAdapter adapter = (SearchResultsAdapter) resultList.getAdapter();
            adapter.setResults(restResults, highlightRides);

            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
                resultList.smoothScrollToPosition(1);

        }

        showNotificationsButton();
        if (restResults.size() > 0)
        {
            new ListFlyupAnimator(resultList).animate();
        }
    }

    private void showNotificationsButton()
    {
        searchNotifyButtonContainer.clearAnimation();

        if (!shouldShowNotificationButton ||
             searchNotifyButtonContainer.getVisibility() == View.VISIBLE ||
            !pushManager.isPushAvailable())
        {
            return;
        }

        // Show notifications button

        updateNotificationButtonText();
        searchNotifyButtonContainer.setVisibility(View.VISIBLE);
        searchNotifyButtonContainer.animate().alpha(1.0f).setDuration(200).setListener(null);
    }

    private void updateNotificationButtonText()
    {
        pushManager.isSubscribed(lastFrom, lastTo, lastDate)
                   .observeOn(AndroidSchedulers.mainThread())
                   .subscribe(subscribed -> {
                       if (subscribed) {
                           searchNotifyButtonIcon.setImageResource(R.drawable.ic_action_cancel);
                           searchNofityButtonText.setText("Prenehaj z obveščanjem");
                       } else {
                           searchNotifyButtonIcon.setImageResource(R.drawable.ic_action_bell);
                           searchNofityButtonText.setText("Obveščaj me o novih prevozih");
                       }
                   });
    }

    private void hideNotificationsButton()
    {
        if (searchNotifyButtonContainer.getVisibility() == View.GONE)
            return;

        searchNotifyButtonContainer.clearAnimation();
        ViewCompat.animate(searchNotifyButtonContainer).alpha(0.0f).setDuration(200).withEndAction(() -> searchNotifyButtonContainer.setVisibility(View.GONE));
    }

    private void clickNotificationButton()
    {
        searchNotifyButton.setEnabled(false);
        searchNotifyButtonIcon.setVisibility(View.INVISIBLE);
        searchNotifyButtonProgress.setVisibility(View.VISIBLE);

        pushManager.isSubscribed(lastFrom, lastTo, lastDate)
                   .subscribe(subscribed -> {
                       pushManager.setSubscriptionStatus(getActivity(), lastFrom, lastTo, lastDate, !subscribed);
                   });
    }

    protected void showHistory(final boolean animate)
    {
        final Activity activity = getActivity();
        if (activity == null) return;

        database.getLastSearches(5)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(s -> {
                   adapter = new SearchHistoryAdapter(activity, s);
                    if (animate && resultList.getAdapter() != null) {
                        hideNotificationsButton();
                        new ListDisappearAnimation(resultList).animate();
                    }

                    resultList.setAdapter(adapter);
                    if (animate)
                        new ListFlyupAnimator(resultList).animate();
                },
                throwable -> {
                    Log.e("Prevoz", "Error while loading history!", throwable);
                    Crashlytics.logException(throwable);
                });
    }

    private void startSearch(@Nullable String fromCity, @Nullable String fromCountry, @Nullable String toCity, @Nullable String toCountry, @NonNull String dateString) {
        if (resultList.getAdapter() != null)
        {
            hideNotificationsButton();
            new ListDisappearAnimation(resultList).animate();
        }

        final Activity activity = getActivity();
        shouldShowNotificationButton = !(fromCity == null || toCity == null);

        ApiClient.getAdapter().search(fromCity, fromCountry, toCity, toCountry, dateString)
                              .map(restSearchResults -> {

                                  // This is localization cache warmup on backgorund thread
                                  LocaleUtil.getFormattedCurrency(1.0);

                                  if (restSearchResults.results != null) {
                                      for (RestRide ride : restSearchResults.results) {
                                          ride.getLocalizedFrom(database);
                                          ride.getLocalizedTo(database);
                                      }
                                  }

                                  return restSearchResults;
                              })
                              .subscribeOn(Schedulers.io())
                              .observeOn(AndroidSchedulers.mainThread())
                              .subscribe(new Subscriber<RestSearchResults>() {
                                  @Override
                                  public void onCompleted() {

                                  }

                                  @Override
                                  public void onError(Throwable e) {
                                      failure((RetrofitError) e);
                                  }

                                  @Override
                                  public void onNext(RestSearchResults restSearchResults) {
                                      success(restSearchResults);
                                  }
                              });
    }


    public void onEventMainThread(Events.NewSearchEvent e)
    {
        EventBus.getDefault().removeStickyEvent(e);
        startSearch(e.from == null ? null : e.from.getDisplayName(),
                    e.from == null ? null : e.from.getCountryCode(),
                    e.to == null ? null : e.to.getDisplayName(),
                    e.to == null ? null : e.to.getCountryCode(),
                    e.date.format(DateTimeFormatter.ISO_LOCAL_DATE));

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

    public void onEventMainThread(Events.MyRideStatusUpdated e)
    {
        if (adapter != null && adapter instanceof SearchResultsAdapter) {
            final SearchResultsAdapter srAdapter = (SearchResultsAdapter) adapter;
            if (e.deleted)
                srAdapter.removeRide(e.ride.id);
            srAdapter.updateRide(e.ride);
            srAdapter.notifyDataSetChanged();
        }
    }

    public void onEventMainThread(Events.ClearSearchEvent e)
    {
        showHistory(true);
    }

    public boolean showingResults()
    {
        return (adapter instanceof SearchResultsAdapter);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Icepick.saveInstanceState(this, outState);
    }
}
