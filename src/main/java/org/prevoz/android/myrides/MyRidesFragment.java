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
import org.androidannotations.annotations.*;
import org.prevoz.android.MainActivity;
import org.prevoz.android.R;
import org.prevoz.android.api.ApiClient;
import org.prevoz.android.api.rest.RestSearchResults;
import org.prevoz.android.auth.AuthenticationUtils;
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

    @AfterViews
    protected void initFragment()
    {
        setListVisibility(false);
    }

    @Override
    public void onResume()
    {
        super.onResume();

        if (!authUtils.isAuthenticated())
        {
            requestAuthentication();
        }
        else
        {
            ApiClient.getAdapter().getMyRides(this);
        }
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

    }

    @Override
    public void failure(RetrofitError error)
    {
        Log.e(LOG_TAG, "Ride load failed!");
        ViewUtils.setupEmptyView(myridesList, emptyView, "Pri nalaganju vaših prevozov je prišlo do napake.");
        setListVisibility(true);
    }

    private void setListVisibility(boolean listVisible)
    {
        myridesList.setVisibility(listVisible ? View.VISIBLE : View.INVISIBLE);
        emptyView.setVisibility(listVisible ? View.VISIBLE : View.INVISIBLE);
        throbber.setVisibility(listVisible ? View.INVISIBLE : View.VISIBLE);
    }
}
