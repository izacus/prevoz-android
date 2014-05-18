package org.prevoz.android.myrides;

import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.EFragment;
import com.googlecode.androidannotations.annotations.OptionsMenu;
import com.googlecode.androidannotations.annotations.ViewById;
import org.prevoz.android.R;
import org.prevoz.android.api.ApiClient;
import org.prevoz.android.api.rest.RestSearchResults;
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

    @AfterViews
    protected void initFragment()
    {
        setListVisibility(false);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        ApiClient.getAdapter().getMyRides(this);
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
