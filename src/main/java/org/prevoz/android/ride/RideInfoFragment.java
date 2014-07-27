package org.prevoz.android.ride;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
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
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.CheckedChange;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.InstanceState;
import org.androidannotations.annotations.ViewById;
import org.prevoz.android.MainActivity;
import org.prevoz.android.R;
import org.prevoz.android.UiFragment;
import org.prevoz.android.api.ApiClient;
import org.prevoz.android.api.PrevozApi;
import org.prevoz.android.api.rest.RestRide;
import org.prevoz.android.myrides.DeleteRideDialog;
import org.prevoz.android.myrides.NewRideFragment;
import org.prevoz.android.util.LocaleUtil;

import java.text.SimpleDateFormat;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

@EFragment(R.layout.fragment_rideinfo)
public class RideInfoFragment extends DialogFragment
{
    private static final SimpleDateFormat timeFormatter = LocaleUtil.getSimpleDateFormat("HH:mm");
    private static final String ARG_RIDE = "ride";
    private static final String ARG_ACTION = "action";

    public static final String PARAM_ACTION_SHOW = "show";
    public static final String PARAM_ACTION_EDIT = "edit";
    public static final String PARAM_ACTION_SUBMIT = "submit";

    public static RideInfoFragment newInstance(RestRide ride)
    {
        RideInfoFragment fragment = new RideInfoFragment_();
        Bundle args = new Bundle();
        args.putParcelable(ARG_RIDE, ride);
        fragment.setArguments(args);
        return fragment;
    }

    public static RideInfoFragment newInstance(RestRide ride, String action)
    {
        RideInfoFragment fragment = new RideInfoFragment_();
        Bundle args = new Bundle();
        args.putParcelable(ARG_RIDE, ride);
        args.putString(ARG_ACTION, action);
        fragment.setArguments(args);
        return fragment;
    }

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

    @ViewById(R.id.rideinfo_full_box)
    protected View vFull;
    @ViewById(R.id.rideinfo_ridefull)
    protected CheckBox chkFull;

    @ViewById(R.id.rideinfo_button_call)
    protected Button leftButton;
    @ViewById(R.id.rideinfo_button_sms)
    protected Button rightButton;

    @InstanceState
    protected RestRide ride = null;

    @InstanceState
    protected String action = null;

    private RideInfoListener listener;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.Prevoz_Dialog_RideInfo);

        ride = getArguments().getParcelable(ARG_RIDE);
        action = getArguments().getString(ARG_ACTION);
        if (action == null)
            action = PARAM_ACTION_SHOW;

        if (PARAM_ACTION_SHOW.equals(action) && ride.isAuthor)
        {
            action = PARAM_ACTION_EDIT;
        }
    }

    @AfterViews
    protected void initFragment()
    {
        txtFrom.setText(LocaleUtil.getLocalizedCityName(getActivity(), ride.fromCity, ride.fromCountry));
        txtTo.setText(LocaleUtil.getLocalizedCityName(getActivity(), ride.toCity, ride.toCountry));
        txtTime.setText(timeFormatter.format(ride.date.getTime()));

        if (ride.price == null || ride.price == 0)
        {
            txtPrice.setVisibility(View.INVISIBLE);
        }
        else
        {
            txtPrice.setText(String.format(LocaleUtil.getLocale(), "%1.1f €", ride.price));
        }

        txtDate.setText(LocaleUtil.localizeDate(getResources(), ride.date));

        vProgress.setVisibility(View.GONE);
        vDetails.setVisibility(View.VISIBLE);

        txtPhone.setText(getPhoneNumberString(ride.phoneNumber, ride.phoneNumberConfirmed));
        txtPeople.setText(String.valueOf(ride.numPeople) + (ride.isFull ? " (Ni mest)" : ""));
        txtComment.setText(ride.comment);

        if (ride.author == null || ride.author.length() == 0)
        {
            txtDriver.setVisibility(View.GONE);
        }
        else
        {
            txtDriver.setText(ride.author);
        }


        txtInsurance.setText(ride.insured ? "\u2713 Ima zavarovanje." : "\u2717 Nima zavarovanja.");

        // Hide call/SMS buttons on devices without telephony support
        PackageManager pm = getActivity().getPackageManager();
        if (PARAM_ACTION_SHOW.equals(action) && !pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY))
        {
            leftButton.setVisibility(View.GONE);
            rightButton.setVisibility(View.GONE);
        }
        else if (PARAM_ACTION_EDIT.equals(action))
        {
            vFull.setVisibility(View.VISIBLE);
        }

        setupActionButtons(action);
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

    private void setupActionButtons(String currentAction)
    {
        if (PARAM_ACTION_SUBMIT.equals(currentAction))
        {
            leftButton.setText("Prekliči");
            rightButton.setText("Oddaj");
        }
        else if (PARAM_ACTION_EDIT.equals(currentAction))
        {
            leftButton.setText("Uredi");
            rightButton.setText("Izbriši");
        }
        else
        {
            leftButton.setText(R.string.rideinfo_call);
            rightButton.setText(R.string.rideinfo_send_sms);
        }
    }

    @Click(R.id.rideinfo_button_call)
    protected void clickCall()
    {
        if (PARAM_ACTION_SHOW.equals(action))
        {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + ride.phoneNumber));
            startActivity(intent);
        }
        else if (PARAM_ACTION_EDIT.equals(action))
        {
            MainActivity activity = (MainActivity) getActivity();
            if (activity == null) return;

            Bundle params = new Bundle();
            params.putParcelable(NewRideFragment.PARAM_EDIT_RIDE, ride);
            activity.showFragment(UiFragment.FRAGMENT_NEW_RIDE, false, params);
        }
        else
        {
            if (listener != null)
                listener.onLeftButtonClicked(ride);
        }

        dismiss();
    }

    @Click(R.id.rideinfo_button_sms)
    protected void clickSms()
    {
        if (PARAM_ACTION_SHOW.equals(action))
        {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("smsto:" + ride.phoneNumber));
            startActivity(intent);
        }
        else if (PARAM_ACTION_EDIT.equals(action))
        {
            dismiss();
            DeleteRideDialog dialog = DeleteRideDialog.newInstance(ride);
            dialog.show(getActivity().getSupportFragmentManager(), "DeleteRideDialog");
        }
        else
        {
            dismiss();

            if (listener != null)
                listener.onRightButtonClicked(ride);
        }
    }

    @CheckedChange(R.id.rideinfo_ridefull)
    protected void clickFull()
    {
        chkFull.setEnabled(false);
        final boolean rideFull = chkFull.isChecked();

        final Activity activity = getActivity();
        if (activity == null) return;

        ApiClient.getAdapter().setFull(String.valueOf(ride.id), rideFull ? PrevozApi.FULL_STATE_FULL : PrevozApi.FULL_STATE_AVAILABLE, new Callback<Response>()
        {
            @Override
            public void success(Response response, Response response2)
            {
                ride.isFull = rideFull;
                chkFull.setEnabled(true);
            }

            @Override
            public void failure(RetrofitError retrofitError)
            {
                chkFull.setChecked(!rideFull);
                Toast.makeText(activity, "Stanja prevoza ni bilo mogoče spremeniti ... ali internet deluje?", Toast.LENGTH_SHORT).show();
                chkFull.setEnabled(true);
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {}

    public void setRideInfoListener(RideInfoListener listener)
    {
        this.listener = listener;
    }
}
