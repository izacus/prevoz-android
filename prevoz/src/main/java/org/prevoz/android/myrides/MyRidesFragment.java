package org.prevoz.android.myrides;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.crashlytics.android.Crashlytics;

import org.prevoz.android.MainActivity;
import org.prevoz.android.PrevozFragment;
import org.prevoz.android.R;
import org.prevoz.android.api.ApiClient;
import org.prevoz.android.api.rest.RestRide;
import org.prevoz.android.api.rest.RestSearchResults;
import org.prevoz.android.events.Events;
import org.prevoz.android.model.Bookmark;
import org.prevoz.android.ride.RideInfoFragment;
import org.prevoz.android.util.PrevozActivity;
import org.prevoz.android.util.ViewUtils;

import java.util.List;


import butterknife.ButterKnife;
import butterknife.InjectView;
import de.greenrobot.event.EventBus;
import retrofit.RetrofitError;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

public class MyRidesFragment extends PrevozFragment
{
    private static final String LOG_TAG = "Prevoz.MyRides";

    @InjectView(R.id.myrides_list)
    protected StickyListHeadersListView myridesList;

    @InjectView(R.id.empty_view)
    protected View emptyView;

    @InjectView(R.id.myrides_throbber)
    protected ProgressBar throbber;

    private MyRidesAdapter adapter = null;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ((PrevozActivity)getActivity()).getApplicationComponent().inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View views = inflater.inflate(R.layout.fragment_myrides, container, false);
        ButterKnife.inject(this, views);

        myridesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                RestRide ride = ((MyRidesAdapter)parent.getAdapter()).getItem(position);
                RideInfoFragment fragment = RideInfoFragment.newInstance(ride, RideInfoFragment.PARAM_ACTION_EDIT);
                fragment.show(getActivity().getSupportFragmentManager(), "RideInfoFragment");
            }
        });

        return views;
    }

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
        Observable<RestRide> myRides = ApiClient.getAdapter().getMyRides()
                .flatMap(new Func1<RestSearchResults, Observable<RestRide>>() {
                    @Override
                    public Observable<RestRide> call(RestSearchResults restSearchResults) {
                        if (restSearchResults == null || restSearchResults.results == null)
                            return Observable.empty();
                        return Observable.from(restSearchResults.results);
                    }
                });

        Observable<RestRide> bookmarkedRides = ApiClient.getAdapter().getBookmarkedRides()
                .flatMap(new Func1<RestSearchResults, Observable<RestRide>>() {
                    @Override
                    public Observable<RestRide> call(RestSearchResults restSearchResults) {
                        if (restSearchResults == null || restSearchResults.results == null)
                            return Observable.empty();
                        return Observable.from(restSearchResults.results);
                    }
                })
                .filter(new Func1<RestRide, Boolean>() {
                    @Override
                    public Boolean call(RestRide restRide) {
                        return Bookmark.shouldShow(restRide.bookmark);
                    }
                });

        myRides.mergeWith(bookmarkedRides)
               .toList()
               .observeOn(AndroidSchedulers.mainThread())
               .subscribe(new Subscriber<List<RestRide>>() {
                   @Override
                   public void onCompleted() {

                   }

                   @Override
                   public void onError(Throwable e) {
                       showLoadFailureError(e);
                   }

                   @Override
                   public void onNext(List<RestRide> rides) {
                       ViewUtils.setupEmptyView(myridesList, emptyView, "Nimate objavljenih ali zaznamovanih prevozov.");
                       Activity activity = getActivity();
                       if (activity == null) return;

                       adapter = new MyRidesAdapter((FragmentActivity) activity, rides);
                       myridesList.setAdapter(adapter);
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

    private void setListVisibility(boolean listVisible)
    {

        if (listVisible) {
            emptyView.setVisibility(adapter == null || adapter.getCount() == 0 ? View.VISIBLE : View.INVISIBLE);
            myridesList.setVisibility(adapter == null || adapter.getCount() != 0 ? View.VISIBLE : View.INVISIBLE);
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
