package org.prevoz.android.myrides;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.crashlytics.android.Crashlytics;
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersDecoration;

import org.prevoz.android.MainActivity;
import org.prevoz.android.PrevozFragment;
import org.prevoz.android.R;
import org.prevoz.android.api.ApiClient;
import org.prevoz.android.api.rest.RestRide;
import org.prevoz.android.events.Events;
import org.prevoz.android.ui.DividerItemDecoration;
import org.prevoz.android.util.PrevozActivity;
import org.prevoz.android.util.ViewUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;
import retrofit.RetrofitError;
import retrofit.client.Response;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class MyRidesFragment extends PrevozFragment
{
    private static final String LOG_TAG = "Prevoz.MyRides";

    @BindView(R.id.myrides_list)
    protected RecyclerView myridesList;

    @BindView(R.id.empty_view)
    protected View emptyView;

    @BindView(R.id.myrides_throbber)
    protected ProgressBar throbber;


    private MyRidesAdapter adapter = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter = new MyRidesAdapter(getActivity());
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ((PrevozActivity)getActivity()).getApplicationComponent().inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View views = inflater.inflate(R.layout.fragment_myrides, container, false);
        ButterKnife.bind(this, views);
        myridesList.setHasFixedSize(true);
        myridesList.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));
        LinearLayoutManager llm = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
        myridesList.setLayoutManager(llm);
        myridesList.addItemDecoration(new StickyRecyclerHeadersDecoration(adapter));
        myridesList.setAdapter(adapter);
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
        ViewUtils.setupEmptyView(myridesList, emptyView, "Nimate objavljenih ali zaznamovanih prevozov.");
        setListVisibility(false);
        emptyView.setVisibility(View.INVISIBLE);
        adapter.clear();

        Observable<RestRide> myRides = ApiClient.getAdapter().getMyRides()
                .flatMap(restSearchResults -> {
                    if (restSearchResults == null || restSearchResults.results == null)
                        return Observable.empty();
                    return Observable.from(restSearchResults.results);
                });

        Observable<RestRide> bookmarks =  ApiClient.getAdapter().getBookmarkedRides()
                .flatMap(restSearchResults -> {
                    if (restSearchResults == null || restSearchResults.results == null)
                        return Observable.empty();
                    return Observable.from(restSearchResults.results);
                });

        myRides.mergeWith(bookmarks)
               .toList()
               .observeOn(AndroidSchedulers.mainThread())
                .subscribe((rides) -> {
                            adapter.addRides(rides);
                            setListVisibility(true);
                        },
                        this::showLoadFailureError);
    }

    private void showLoadFailureError(Throwable error) {
        Crashlytics.logException(error.getCause());
        if (error instanceof RetrofitError) {
            // Check for unauthorzied
            RetrofitError rerror = (RetrofitError)error;
            if (rerror.getResponse() != null) {
                Response response = rerror.getResponse();
                Crashlytics.log(Log.ERROR, LOG_TAG, response.getBody().toString());
                if (response.getStatus() == 403 || response.getStatus() == 401) {
                    authUtils.logout().subscribeOn(Schedulers.io()).toBlocking().firstOrDefault(null);
                    authUtils.requestAuthentication(getActivity(), MainActivity.REQUEST_CODE_AUTHORIZE_MYRIDES);
                    return;
                }
            }
        }

        Log.e(LOG_TAG, "Ride load failed!");
        ViewUtils.setupEmptyView(myridesList, emptyView, "Pri nalaganju vaših prevozov je prišlo do napake.");
        setListVisibility(true);
    }

    private void setListVisibility(boolean listVisible)
    {

        if (listVisible) {
            emptyView.setVisibility(adapter == null || adapter.getItemCount() == 0 ? View.VISIBLE : View.INVISIBLE);
            myridesList.setVisibility(adapter == null || adapter.getItemCount() != 0 ? View.VISIBLE : View.INVISIBLE);
        } else {
            emptyView.setVisibility(View.INVISIBLE);
            myridesList.setVisibility(View.INVISIBLE);
        }


        throbber.setVisibility(listVisible ? View.INVISIBLE : View.VISIBLE);
    }

    public void onEventMainThread(Events.MyRideStatusUpdated e)
    {
        loadRides();
    }
}
