package org.prevoz.android.myrides;

import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import de.greenrobot.event.EventBus;
import org.androidannotations.annotations.*;
import org.prevoz.android.MainActivity;
import org.prevoz.android.R;
import org.prevoz.android.api.ApiClient;
import org.prevoz.android.api.rest.RestRide;
import org.prevoz.android.api.rest.RestSearchResults;
import org.prevoz.android.auth.AuthenticationUtils;
import org.prevoz.android.events.Events;
import org.prevoz.android.ride.RideInfoFragment;
import org.prevoz.android.search.SearchResultsAdapter;
import org.prevoz.android.util.ViewUtils;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

@EFragment(R.layout.fragment_myrides)
public class MyRidesFragment extends Fragment implements Callback<RestSearchResults>
{
    private static final String LOG_TAG = "Prevoz.MyRides";

    @ViewById(R.id.myrides_list)
    protected ListView myridesList;

    @ViewById(R.id.empty_view)
    protected View emptyView;

    @ViewById(R.id.myrides_throbber)
    protected ProgressBar throbber;

    @Bean
    protected AuthenticationUtils authUtils;

    @Override
    public void onResume()
    {
        super.onResume();
        EventBus.getDefault().register(this);


        if (!authUtils.isAuthenticated())
        {
            requestAuthentication();
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
        ApiClient.getAdapter().getMyRides(this);
    }

    @Background
    protected void requestAuthentication()
    {
        AccountManager am = AccountManager.get(getActivity());
        AccountManagerFuture<Bundle> result = am.addAccount(getString(R.string.account_type), null, null, null, null, null, null);

        try
        {
            Intent i = (Intent) result.getResult().get(AccountManager.KEY_INTENT);
            getActivity().startActivityForResult(i, MainActivity.REQUEST_CODE_AUTHORIZE_MYRIDES);
        }
        catch (Exception e)
        {
            Log.e(LOG_TAG, "Error!", e);
        }
    }

    @Override
    public void success(RestSearchResults restRide, Response response)
    {
        Log.d(LOG_TAG, "Rides loaded: " + response.getStatus());
        ViewUtils.setupEmptyView(myridesList, emptyView, "Nimate objavljenih prevozov.");
        setListVisibility(true);


        SearchResultsAdapter adapter = new SearchResultsAdapter(getActivity(), restRide.results);
        myridesList.setAdapter(adapter);
    }

    @Override
    public void failure(RetrofitError error)
    {
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
        myridesList.setVisibility(listVisible ? View.VISIBLE : View.INVISIBLE);
        emptyView.setVisibility(listVisible ? View.VISIBLE : View.INVISIBLE);
        throbber.setVisibility(listVisible ? View.INVISIBLE : View.VISIBLE);
    }

    public void onEventMainThread(Events.RideDeleted e)
    {
        loadRides();
    }
}
