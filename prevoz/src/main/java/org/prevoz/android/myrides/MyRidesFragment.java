package org.prevoz.android.myrides;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.crashlytics.android.Crashlytics;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.ItemClick;
import org.androidannotations.annotations.ViewById;
import org.prevoz.android.MainActivity;
import org.prevoz.android.R;
import org.prevoz.android.api.ApiClient;
import org.prevoz.android.api.rest.RestRide;
import org.prevoz.android.api.rest.RestSearchResults;
import org.prevoz.android.auth.AuthenticationUtils;
import org.prevoz.android.events.Events;
import org.prevoz.android.ride.RideInfoFragment;
import org.prevoz.android.util.ViewUtils;

import de.greenrobot.event.EventBus;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

@EFragment(R.layout.fragment_myrides)
public class MyRidesFragment extends Fragment
{
    private static final String LOG_TAG = "Prevoz.MyRides";

    @ViewById(R.id.myrides_list)
    protected StickyListHeadersListView myridesList;

    @ViewById(R.id.empty_view)
    protected View emptyView;

    @ViewById(R.id.myrides_throbber)
    protected ProgressBar throbber;

    private MyRidesAdapter adapter = null;

    @Bean
    protected AuthenticationUtils authUtils;

    @Override
    public void onResume()
    {
        super.onResume();
        EventBus.getDefault().register(this);


        if (!authUtils.isAuthenticated())
        {
            authUtils.requestAuthentication(getActivity(), MainActivity.REQUEST_CODE_AUTHORIZE_MYRIDES);
        }
        else
        {
            loadRides();
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    private void loadRides()
    {
        setListVisibility(false);

        ApiClient.getAdapter().getMyRides()
                              .observeOn(AndroidSchedulers.mainThread())
                              .subscribe(new Subscriber<RestSearchResults>() {
                                  @Override
                                  public void onCompleted() {
                                  }

                                  @Override
                                  public void onError(Throwable e) {
                                      showLoadFailureError(e);
                                  }

                                  @Override
                                  public void onNext(RestSearchResults restSearchResults) {
                                      ViewUtils.setupEmptyView(myridesList, emptyView, "Nimate objavljenih ali zaznamovanih prevozov.");
                                      Activity activity = getActivity();
                                      if (activity == null) return;

                                      if (adapter == null) {
                                          adapter = new MyRidesAdapter((FragmentActivity) activity);
                                          myridesList.setAdapter(adapter);
                                      }

                                      adapter.setPublishedRides(restSearchResults.results);
                                      setListVisibility(true);
                                  }
                              });

        ApiClient.getAdapter().getBookmarkedRides()
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new Subscriber<RestSearchResults>() {
                                @Override
                                public void onCompleted() {
                                }

                                @Override
                                public void onError(Throwable e) {
                                    showLoadFailureError(e);
                                }

                                @Override
                                public void onNext(RestSearchResults restSearchResults) {
                                    ViewUtils.setupEmptyView(myridesList, emptyView, "Nimate objavljenih ali zaznamovanih prevozov.");
                                    Activity activity = getActivity();
                                    if (activity == null) return;

                                    if (adapter == null) {
                                        adapter = new MyRidesAdapter((FragmentActivity) activity);
                                        myridesList.setAdapter(adapter);
                                    }

                                    adapter.setBookmarkedRides(restSearchResults.results);
                                    setListVisibility(true);
                                }
                            });
    }

    private void showLoadFailureError(Throwable error) {
        Crashlytics.logException(error.getCause());
        if (error instanceof RetrofitError) {
            if (((RetrofitError)error).getResponse() != null && ((RetrofitError)error).getResponse().getStatus() == 403) {
                authUtils.logout();
                ApiClient.setBearer(null);
            }
        }

        Log.e(LOG_TAG, "Ride load failed!");
        ViewUtils.setupEmptyView(myridesList, emptyView, "Pri nalaganju vaših prevozov je prišlo do napake.");
        setListVisibility(true);
    }

    @ItemClick(R.id.myrides_list)
    protected void itemClick(RestRide ride)
    {
        RideInfoFragment fragment = RideInfoFragment.newInstance(ride, RideInfoFragment.PARAM_ACTION_EDIT);
        fragment.show(getActivity().getSupportFragmentManager(), "RideInfoFragment");
    }

    private void setListVisibility(boolean listVisible)
    {

        if (listVisible) {
            emptyView.setVisibility(adapter.getCount() == 0 ? View.VISIBLE : View.INVISIBLE);
            myridesList.setVisibility(adapter.getCount() != 0 ? View.VISIBLE : View.INVISIBLE);
        } else {
            emptyView.setVisibility(View.INVISIBLE);
            myridesList.setVisibility(View.INVISIBLE);
        }

        throbber.setVisibility(listVisible ? View.INVISIBLE : View.VISIBLE);
    }

    public void onEventMainThread(Events.RideDeleted e)
    {
        loadRides();
    }
}
