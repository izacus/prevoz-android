package org.prevoz.android.ride;

import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.TextView;

import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.EFragment;
import com.googlecode.androidannotations.annotations.ViewById;

import org.prevoz.android.R;
import org.prevoz.android.api.ApiClient;
import org.prevoz.android.api.rest.RestRide;
import org.prevoz.android.api.rest.RestSearchRide;
import org.prevoz.android.util.Database;
import org.prevoz.android.util.LocaleUtil;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by jernej on 15/02/14.
 */
@EFragment(R.layout.fragment_rideinfo)
public class RideInfoFragment extends DialogFragment implements Callback<RestRide> {
    private static final SimpleDateFormat timeFormatter = LocaleUtil.getSimpleDateFormat("HH:mm");
    private static final String ARG_RIDE = "ride";

    public static RideInfoFragment newInstance(RestSearchRide ride)
    {
        RideInfoFragment fragment = new RideInfoFragment_();
        Bundle args = new Bundle();
        args.putSerializable(ARG_RIDE, ride);
        fragment.setArguments(args);
        return fragment;
    }

    protected RestSearchRide sourceRide;

    @ViewById(R.id.rideinfo_from)
    protected TextView txtFrom;

    @ViewById(R.id.rideinfo_to)
    protected TextView txtTo;

    @ViewById(R.id.rideinfo_time)
    protected TextView txtTime;

    @ViewById(R.id.rideinfo_price)
    protected TextView txtPrice;

    @ViewById(R.id.rideinfo_date)
    protected TextView txtDate;

    @ViewById(R.id.rideinfo_details)
    protected View vDetails;
    @ViewById(R.id.rideinfo_load_progress)
    protected View vProgress;

    @ViewById(R.id.rideinfo_phone)
    protected TextView txtPhone;
    @ViewById(R.id.rideinfo_people)
    protected TextView txtPeople;
    @ViewById(R.id.rideinfo_driver)
    protected TextView txtDriver;
    @ViewById(R.id.rideinfo_comment)
    protected TextView txtComment;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
        {
            setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_Holo_Light_Dialog_NoActionBar_MinWidth);
        }
        else
        {
            setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_Dialog);
        }

        sourceRide = (RestSearchRide) getArguments().getSerializable(ARG_RIDE);
    }

    @AfterViews
    protected void initFragment()
    {
        txtFrom.setText(LocaleUtil.getLocalizedCityName(getActivity(), sourceRide.fromCity, sourceRide.fromCountry));
        txtTo.setText(LocaleUtil.getLocalizedCityName(getActivity(), sourceRide.toCity, sourceRide.toCountry));
        txtTime.setText(timeFormatter.format(sourceRide.date));
        txtPrice.setText(String.format(LocaleUtil.getLocale(), "%1.1f €", sourceRide.price));

        Calendar cal = new GregorianCalendar();
        cal.setTime(sourceRide.date);
        txtDate.setText(LocaleUtil.localizeDate(getResources(), cal));

        // Load detail data
        ApiClient.getAdapter().getRide(String.valueOf(sourceRide.id), this);
    }

    @Override
    public void success(RestRide restRide, Response response)
    {
        vProgress.setVisibility(View.GONE);
        vDetails.setVisibility(View.VISIBLE);

        txtPhone.setText(restRide.phoneNumber);
        txtPeople.setText(String.valueOf(restRide.numPeople));
        txtComment.setText(restRide.comment);

        if (restRide.author == null || restRide.author.length() == 0)
            txtDriver.setVisibility(View.GONE);
        else
            txtDriver.setText(restRide.author);
    }

    @Override
    public void failure(RetrofitError retrofitError)
    {
        vProgress.setVisibility(View.GONE);
    }
}
