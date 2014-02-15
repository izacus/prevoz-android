package org.prevoz.android.ride;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.Click;
import com.googlecode.androidannotations.annotations.EFragment;
import com.googlecode.androidannotations.annotations.ViewById;

import org.prevoz.android.R;
import org.prevoz.android.api.ApiClient;
import org.prevoz.android.api.rest.RestRide;
import org.prevoz.android.api.rest.RestSearchRide;
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
    @ViewById(R.id.rideinfo_insurance)
    protected TextView txtInsurance;
    @ViewById(R.id.rideinfo_driver)
    protected TextView txtDriver;
    @ViewById(R.id.rideinfo_comment)
    protected TextView txtComment;

    @ViewById(R.id.rideinfo_button_call)
    protected Button callButton;
    @ViewById(R.id.rideinfo_button_sms)
    protected Button smsButton;

    private RestRide ride = null;

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

        if (sourceRide.price == 0)
        {
            txtPrice.setVisibility(View.INVISIBLE);
        }
        else
        {
            txtPrice.setText(String.format(LocaleUtil.getLocale(), "%1.1f €", sourceRide.price));
        }

        Calendar cal = new GregorianCalendar();
        cal.setTime(sourceRide.date);
        txtDate.setText(LocaleUtil.localizeDate(getResources(), cal));

        // Hide call/SMS buttons on devices without telephony support
        PackageManager pm = getActivity().getPackageManager();
        if (!pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY))
        {
            callButton.setVisibility(View.GONE);
            smsButton.setVisibility(View.GONE);
        }


        // Reenable them after contact information is loaded
        callButton.setEnabled(false);
        smsButton.setEnabled(false);

        // Load detail data
        ApiClient.getAdapter().getRide(String.valueOf(sourceRide.id), this);
    }

    private SpannableString getPhoneNumberString(String phoneNumber, boolean confirmed)
    {
        SpannableString phoneNumberString = new SpannableString(phoneNumber + (confirmed ? "" : "\nNi potrjena"));
        if (confirmed)
            return phoneNumberString;

        RelativeSizeSpan sizeSpan = new RelativeSizeSpan(0.55f);
        phoneNumberString.setSpan(sizeSpan, phoneNumber.length(), phoneNumberString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        ForegroundColorSpan colorSpan = new ForegroundColorSpan(getResources().getColor(android.R.color.darker_gray));
        phoneNumberString.setSpan(colorSpan, phoneNumber.length(), phoneNumberString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        TypefaceSpan typefaceSpan = new TypefaceSpan("sans-serif-thin");
        phoneNumberString.setSpan(typefaceSpan, phoneNumber.length(), phoneNumberString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        StyleSpan styleSpan = new StyleSpan(Typeface.BOLD_ITALIC);
        phoneNumberString.setSpan(styleSpan, phoneNumber.length(), phoneNumberString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        return phoneNumberString;
    }

    @Override
    public void success(RestRide restRide, Response response)
    {
        ride = restRide;

        vProgress.setVisibility(View.GONE);
        vDetails.setVisibility(View.VISIBLE);

        txtPhone.setText(getPhoneNumberString(restRide.phoneNumber, restRide.phoneNumberConfirmed));
        txtPeople.setText(String.valueOf(restRide.numPeople) + (restRide.isFull ? " (Ni mest)" : ""));
        txtComment.setText(restRide.comment);

        if (restRide.author == null || restRide.author.length() == 0)
        {
            txtDriver.setVisibility(View.GONE);
        }
        else
        {
            txtDriver.setText(restRide.author);
        }


        txtInsurance.setText(restRide.insured ? "\u2713 Ima zavarovanje." : "\u2717 Nima zavarovanja.");

        callButton.setEnabled(true);
        smsButton.setEnabled(true);
    }

    @Override
    public void failure(RetrofitError retrofitError)
    {
        vProgress.setVisibility(View.GONE);
    }

    @Click(R.id.rideinfo_button_call)
    protected void clickCall()
    {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + ride.phoneNumber));
        startActivity(intent);
    }

    @Click(R.id.rideinfo_button_sms)
    protected void clickSms()
    {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("smsto:" + ride.phoneNumber));
        startActivity(intent);
    }
}
