package org.prevoz.android.myrides;

import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.EFragment;
import com.googlecode.androidannotations.annotations.ViewById;
import org.prevoz.android.R;
import org.prevoz.android.api.ApiClient;
import org.prevoz.android.api.rest.RestRide;
import org.prevoz.android.util.ViewUtils;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

import java.util.List;

@EFragment(R.layout.fragment_myrides)
public class MyRidesFragment extends Fragment implements Callback<List<RestRide>>
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
        ViewUtils.setupEmptyView(myridesList, emptyView, "Nimate objavljenih prevozov.");
        myridesList.setVisibility(View.INVISIBLE);
        emptyView.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        ApiClient.getAdapter().getMyRides(this);
    }

    @Override
    public void success(List<RestRide> restRide, Response response)
    {
        Log.d(LOG_TAG, "Rides loaded: " + response.getStatus());
    }

    @Override
    public void failure(RetrofitError error)
    {
        Log.e(LOG_TAG, "Ride load failed!");
        error.printStackTrace();
    }
}
