package org.prevoz.android.ride;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.crashlytics.android.Crashlytics;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
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
import org.prevoz.android.auth.AuthenticationUtils;
import org.prevoz.android.events.Events;
import org.prevoz.android.model.Bookmark;
import org.prevoz.android.myrides.NewRideFragment;
import org.prevoz.android.util.LocaleUtil;
import org.prevoz.android.util.ViewUtils;

import java.text.SimpleDateFormat;

import de.greenrobot.event.EventBus;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import si.virag.fuzzydateformatter.FuzzyDateTimeFormatter;

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

    @ViewById(R.id.rideinfo_favorite)
    protected ImageView imgFavorite;

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

    @Bean
    protected AuthenticationUtils authUtils;

    private RideInfoListener listener;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.Prevoz_RideInfo);

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
        imgFavorite.setVisibility(authUtils.isAuthenticated() ? View.VISIBLE : View.INVISIBLE);
        updateFavoriteIcon();

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
        setPeopleText();
        txtComment.setText(ride.comment);

        if ((ride.author == null || ride.author.length() == 0) && ride.published == null) {
            txtDriver.setVisibility(View.GONE);
        } else if (ride.published == null) {
            txtDriver.setText(ride.author + "\u00A0");
        } else {
            if (ride.author == null || ride.author.length() == 0) {
                txtDriver.setText(FuzzyDateTimeFormatter.getTimeAgo(getActivity(), ride.published.getTime()) + "\u00A0");   // Add non-breaking space at the end to prevent italic letter clipping
            } else {
                SpannableStringBuilder ssb = new SpannableStringBuilder();
                ssb.append(ride.author);
                ssb.append(", ");
                ssb.append(FuzzyDateTimeFormatter.getTimeAgo(getActivity(), ride.published.getTime()));
                ssb.setSpan(new StyleSpan(Typeface.BOLD), 0, ride.author.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                ssb.append("\u00A0");
                txtDriver.setText(ssb);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
            txtInsurance.setText(ride.insured ? "\u2713 Ima zavarovanje." : "\u2717 Nima zavarovanja.");
        else
            txtInsurance.setText(ride.insured ? "Ima zavarovanje." : "Nima zavarovanja.");


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
            chkFull.setChecked(ride.isFull);
        }

        setupActionButtons(action);
    }

    private void setPeopleText()
    {
        txtPeople.setText(String.valueOf(ride.numPeople) + (ride.isFull ? " (Polno)" : ""));
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
        switch (currentAction) {
            case PARAM_ACTION_SUBMIT:
                leftButton.setText("Prekliči");
                rightButton.setText("Oddaj");
                break;
            case PARAM_ACTION_EDIT:
                leftButton.setText("Uredi");
                rightButton.setText("Izbriši");
                break;
            default:
                leftButton.setText(R.string.rideinfo_call);
                rightButton.setText(R.string.rideinfo_send_sms);
                break;
        }
    }

    protected void updateFavoriteIcon() {
        Drawable drawable = getResources().getDrawable(Bookmark.shouldShow(ride.bookmark) ? R.drawable.ic_favorite : R.drawable.ic_favorite_outline);
        drawable.setColorFilter(new PorterDuffColorFilter(getResources().getColor(R.color.prevoztheme_color), PorterDuff.Mode.SRC_ATOP));
        imgFavorite.setImageDrawable(drawable);
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
            EventBus.getDefault().post(new Events.ShowFragment(UiFragment.FRAGMENT_NEW_RIDE, false, params));
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

            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(getActivity(), "Nimate nameščene nobene aplikacije za pošiljanje SMS sporočil.", Toast.LENGTH_SHORT).show();
            }

        }
        else if (PARAM_ACTION_EDIT.equals(action))
        {
            final Activity activity = getActivity();
            dismiss();

            new MaterialDialog.Builder(activity)
                              .title(ride.fromCity + " - " + ride.toCity)
                              .titleColorRes(R.color.prevoztheme_color_dark)
                              .content(getString(R.string.ride_delete_message, LocaleUtil.getDayName(getResources(), ride.date).toLowerCase(LocaleUtil.getLocale()), LocaleUtil.getFormattedTime(ride.date)))
                              .positiveText(R.string.ride_delete_ok)
                              .negativeText(R.string.ride_delete_cancel)
                              .callback(new MaterialDialog.SimpleCallback() {
                                  @Override
                                  public void onPositive(MaterialDialog materialDialog) {
                                      final ProgressDialog deleteDialog = new ProgressDialog(activity);
                                      deleteDialog.setMessage(activity.getString(R.string.ride_delete_progress));
                                      deleteDialog.show();

                                      ApiClient.getAdapter().deleteRide(String.valueOf(ride.id), new Callback<Response>() {
                                          @Override
                                          public void success(Response response, Response response2) {
                                              EventBus.getDefault().post(new Events.RideDeleted(ride.id));
                                              deleteDialog.dismiss();
                                              ViewUtils.showMessage(activity, R.string.ride_delete_success, false);
                                          }

                                          @Override
                                          public void failure(RetrofitError error) {
                                              deleteDialog.dismiss();
                                              ViewUtils.showMessage(activity, R.string.ride_delete_failure, true);
                                          }
                                      });
                                  }
                              })
                              .show();
        }
        else
        {
            dismiss();

            if (listener != null)
                listener.onRightButtonClicked(ride);
        }
    }

    @Click(R.id.rideinfo_favorite)
    protected void clickFavorite() {
        if (Bookmark.shouldShow(ride.bookmark)) {
            ride.bookmark = null;
        } else {
            ride.bookmark = Bookmark.BOOKMARK;
        }

        updateFavoriteIcon();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            imgFavorite.animate().scaleX(2.0f).scaleY(2.0f).alpha(0.2f).setDuration(200).setListener(new AnimatorListenerAdapter() {
                @Override
                @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    imgFavorite.setAlpha(1.0f);
                    imgFavorite.setScaleX(1);
                    imgFavorite.setScaleY(1);
                }
            });
        }

        ApiClient.getAdapter().setRideBookmark(String.valueOf(ride.id), Bookmark.shouldShow(ride.bookmark) ? "bookmark" : "erase", new Callback<Response>() {
            @Override
            public void success(Response response, Response response2) {
                Log.i("Prevoz", "Bookmark set OK.");
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e("Prevoz", "Failed to set bookmark status.", error);
                Crashlytics.logException(error);
            }
        });
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
                setPeopleText();
            }

            @Override
            public void failure(RetrofitError retrofitError)
            {
                chkFull.setChecked(!rideFull);
                ViewUtils.showMessage(getActivity(), "Stanja prevoza ni bilo mogoče spremeniti :(", true);
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
