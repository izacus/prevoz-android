package org.prevoz.android.ride;

import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.widget.TextView;

import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.EFragment;
import com.googlecode.androidannotations.annotations.ViewById;

import org.prevoz.android.R;
import org.prevoz.android.api.rest.RestSearchRide;
import org.prevoz.android.util.Database;
import org.prevoz.android.util.LocaleUtil;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Created by jernej on 15/02/14.
 */
@EFragment(R.layout.fragment_rideinfo)
public class RideInfoFragment extends DialogFragment
{
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
    }
}
