package org.prevoz.android.ride;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.app.AlertDialog;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;
import com.crashlytics.android.answers.CustomEvent;

import org.prevoz.android.PrevozApplication;
import org.prevoz.android.R;
import org.prevoz.android.UiFragment;
import org.prevoz.android.api.ApiClient;
import org.prevoz.android.api.PrevozApi;
import org.prevoz.android.api.rest.RestRide;
import org.prevoz.android.api.rest.RestStatus;
import org.prevoz.android.events.Events;
import org.prevoz.android.model.Bookmark;
import org.prevoz.android.myrides.NewRideActivity;
import org.prevoz.android.util.LocaleUtil;
import org.prevoz.android.util.PrevozActivity;
import org.prevoz.android.util.ViewUtils;
import org.threeten.bp.format.DateTimeFormatter;

import java.io.IOException;
import java.util.Date;
import java.util.Locale;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import de.greenrobot.event.EventBus;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import rx.schedulers.Schedulers;

public class RideInfoActivity extends PrevozActivity {
    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    private static final String ARG_RIDE = "ride";
    private static final String ARG_ACTION = "action";

    public static final String PARAM_ACTION_SHOW = "show";
    public static final String PARAM_ACTION_EDIT = "edit";
    public static final String PARAM_ACTION_SUBMIT = "submit";

    public static void show(Activity parent, RestRide ride) {
        Intent i = new Intent(parent, RideInfoActivity.class);
        i.putExtra(ARG_RIDE, (Parcelable) ride);
        parent.startActivity(i);
    }

    public static void show(Activity parent, RestRide ride, String action, int requestCode) {
        Intent i = new Intent(parent, RideInfoActivity.class);
        i.putExtra(ARG_RIDE, (Parcelable) ride);
        i.putExtra(ARG_ACTION, action);
        parent.startActivityForResult(i, requestCode);
    }


    public static void show(Activity parent, RestRide ride, Bundle options) {
        Intent i = new Intent(parent, RideInfoActivity.class);
        i.putExtra(ARG_RIDE, (Parcelable) ride);
        ActivityCompat.startActivity(parent, i, options);
    }

    @BindView(R.id.rideinfo_container)
    protected View container;

    @BindView(R.id.rideinfo_favorite)
    protected ImageView imgFavorite;

    @BindView(R.id.rideinfo_from)
    protected TextView txtFrom;

    @BindView(R.id.rideinfo_to)
    protected TextView txtTo;

    @BindView(R.id.rideinfo_time)
    protected TextView txtTime;

    @BindView(R.id.rideinfo_price)
    protected TextView txtPrice;

    @BindView(R.id.rideinfo_date)
    protected TextView txtDate;

    @BindView(R.id.rideinfo_details)
    protected View vDetails;

    @BindView(R.id.rideinfo_phone)
    protected TextView txtPhone;
    @BindView(R.id.rideinfo_people)
    protected TextView txtPeople;
    @BindView(R.id.rideinfo_insurance)
    protected TextView txtInsurance;
    @BindView(R.id.rideinfo_driver)
    protected TextView txtDriver;
    @BindView(R.id.rideinfo_car)
    protected TextView txtCar;
    @BindView(R.id.rideinfo_comment)
    protected TextView txtComment;

    @BindView(R.id.rideinfo_full_box)
    protected View vFull;
    @BindView(R.id.rideinfo_ridefull)
    protected CheckBox chkFull;

    @BindView(R.id.rideinfo_button_call)
    protected Button leftButton;
    @BindView(R.id.rideinfo_button_sms)
    protected Button rightButton;

    protected RestRide ride = null;
    protected String action = null;

    GestureDetector detector;

    @Inject
    ConnectivityManager connectivityService;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        LocaleUtil.checkSetLocale(this, getResources().getConfiguration());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rideinfo);
        ((PrevozApplication)getApplication()).component().inject(this);
        ButterKnife.bind(this);

        if (savedInstanceState != null) {
            ride = savedInstanceState.getParcelable("ride");
            action = savedInstanceState.getString("action");
        } else {
            ride = getIntent().getParcelableExtra(ARG_RIDE);
            action = getIntent().getStringExtra(ARG_ACTION);
        }

        if (action == null) {
            action = PARAM_ACTION_SHOW;
        }

        if (PARAM_ACTION_SHOW.equals(action) && ride.isAuthor) {
            action = PARAM_ACTION_EDIT;
        }


        imgFavorite.setVisibility(authUtils.isAuthenticated() && !ride.isAuthor ? View.VISIBLE : View.INVISIBLE);
        updateFavoriteIcon();

        txtFrom.setText(LocaleUtil.getLocalizedCityName(database, ride.fromCity, ride.fromCountry));
        txtTo.setText(LocaleUtil.getLocalizedCityName(database, ride.toCity, ride.toCountry));
        txtTime.setText(ride.date.format(timeFormatter));

        if (ride.price == null || ride.price == 0) {
            txtPrice.setVisibility(View.INVISIBLE);
        } else {
            txtPrice.setText(String.format(LocaleUtil.getLocale(), "%1.1f €", ride.price));
        }

        txtDate.setText(LocaleUtil.localizeDate(getResources(), ride.date));
        vDetails.setVisibility(View.VISIBLE);

        txtPhone.setText(getPhoneNumberString(ride.phoneNumber, ride.phoneNumberConfirmed));
        setPeopleText();
        txtComment.setText(ride.comment);

        if (TextUtils.isEmpty(ride.carInfo)) {
            txtCar.setVisibility(View.GONE);
        } else {
            txtCar.setVisibility(View.VISIBLE);
            txtCar.setText(ride.carInfo);
        }

        if ((ride.author == null || ride.author.length() == 0) && ride.published == null) {
            txtDriver.setVisibility(View.GONE);
        } else if (ride.published == null) {
            txtDriver.setText(ride.author + "\u00A0");
        } else {
            String timeAgo = DateUtils.getRelativeTimeSpanString(ride.published.toInstant().toEpochMilli(),
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_MONTH | DateUtils.FORMAT_ABBREV_WEEKDAY).toString().toLowerCase(Locale.getDefault());

            if (ride.author == null || ride.author.length() == 0) {
                txtDriver.setText(timeAgo + "\u00A0");   // Add non-breaking space at the end to prevent italic letter clipping
            } else {
                SpannableStringBuilder ssb = new SpannableStringBuilder();
                ssb.append(ride.author);
                ssb.append(", ");
                ssb.append(timeAgo);
                ssb.setSpan(new StyleSpan(Typeface.BOLD), 0, ride.author.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                ssb.append("\u00A0");
                txtDriver.setText(ssb);
            }
        }

        txtInsurance.setText(ride.insured ? "\u2713 Ima zavarovanje." : "\u2717 Nima zavarovanja.");

        // Hide call/SMS buttons on devices without telephony support
        PackageManager pm = getPackageManager();
        if (PARAM_ACTION_SHOW.equals(action) && !pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
            leftButton.setVisibility(View.GONE);
            rightButton.setVisibility(View.GONE);
        } else if (PARAM_ACTION_EDIT.equals(action)) {
            vFull.setVisibility(View.VISIBLE);
            chkFull.setChecked(ride.isFull);
        }

        setupActionButtons(action);

        detector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                finish();
                return true;
            }
        });

        ContentViewEvent event = new ContentViewEvent();
        event.putContentName(ride.getRoute().toString());
        if (ride.id != null) {
            event.putContentId(ride.id.toString());
        }

        switch (action) {
            case PARAM_ACTION_SHOW:
                event.putContentType("Ride");
                break;
            case PARAM_ACTION_EDIT:
                event.putContentType("Ride - edit");
                break;
            case PARAM_ACTION_SUBMIT:
                event.putContentType("New ride");
                break;
        }

        Answers.getInstance().logContentView(event);
    }

    private void setPeopleText() {
        txtPeople.setText(String.valueOf(ride.numPeople) + (ride.isFull ? " (Polno)" : ""));
    }

    private SpannableString getPhoneNumberString(String phoneNumber, boolean confirmed) {
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

    private void setupActionButtons(String currentAction) {
        switch (currentAction) {
            case PARAM_ACTION_SUBMIT:
                leftButton.setText(R.string.rideinfo_cancel);
                rightButton.setText(R.string.rideinfo_submit);
                break;
            case PARAM_ACTION_EDIT:
                leftButton.setText(R.string.rideinfo_edit);
                rightButton.setText(R.string.rideinfo_delete);
                break;
            default:
                leftButton.setText(R.string.rideinfo_call);
                rightButton.setText(R.string.rideinfo_send_sms);
                break;
        }
    }

    protected void updateFavoriteIcon() {
        Drawable drawable = getResources().getDrawable(Bookmark.shouldShow(ride.bookmark) ? R.drawable.ic_favorite : R.drawable.ic_favorite_outline);
        drawable.setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP));
        imgFavorite.setImageDrawable(drawable);
    }

    @OnClick(R.id.rideinfo_button_call)
    protected void onCallClicked() {
        if (PARAM_ACTION_SHOW.equals(action)) {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + ride.phoneNumber));
            startActivity(intent);
        } else if (PARAM_ACTION_EDIT.equals(action)) {
            Intent intent = new Intent(this, NewRideActivity.class);
            intent.putExtra(NewRideActivity.PARAM_EDIT_RIDE, (Parcelable) ride);
            ActivityCompat.startActivity(this, intent, ActivityOptionsCompat.makeCustomAnimation(this, R.anim.slide_up, 0).toBundle());
        }

        finish();
    }

    @OnClick(R.id.rideinfo_button_sms)
    protected void onSmsClicked() {
        if (PARAM_ACTION_SHOW.equals(action)) {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("smsto:" + ride.phoneNumber));

            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, "Nimate nameščene nobene aplikacije za pošiljanje SMS sporočil.", Toast.LENGTH_SHORT).show();
            }

        } else if (PARAM_ACTION_EDIT.equals(action)) {
            new AlertDialog.Builder(this, R.style.Prevoz_Theme_Dialog)
                    .setTitle(String.format("%s - %s", ride.fromCity, ride.toCity))
                    .setMessage(getString(R.string.ride_delete_message, LocaleUtil.getDayName(getResources(), ride.date).toLowerCase(LocaleUtil.getLocale()), LocaleUtil.getFormattedTime(ride.date)))
                    .setNegativeButton(R.string.ride_delete_cancel, null)
                    .setPositiveButton(R.string.ride_delete_ok, (dialog, which) -> {
                        final ProgressDialog deleteDialog = new ProgressDialog(this);
                        deleteDialog.setMessage(getString(R.string.ride_delete_progress));
                        deleteDialog.show();

                        Call<ResponseBody> call = ApiClient.getAdapter().deleteRide(String.valueOf(ride.id));
                        call.enqueue(new Callback<ResponseBody>() {
                            @Override
                            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                                finish();
                                EventBus.getDefault().post(new Events.MyRideStatusUpdated(ride, true));
                                deleteDialog.dismiss();
                                EventBus.getDefault().postSticky(new Events.ShowMessage(R.string.ride_delete_success, false));
                            }

                            @Override
                            public void onFailure(Call<ResponseBody> call, Throwable throwable) {
                                finish();
                                deleteDialog.dismiss();
                                EventBus.getDefault().postSticky(new Events.ShowMessage(R.string.ride_delete_failure, true));
                            }
                        });
                    }).show();
        } else if (PARAM_ACTION_SUBMIT.equals(action)) {
            submitRide();
        } else {
            finish();
        }
    }

    @OnClick(R.id.rideinfo_favorite)
    protected void onFavoriteClicked() {
        if (Bookmark.shouldShow(ride.bookmark)) {
            Answers.getInstance().logCustom(new CustomEvent("Bookmark - clear"));
            ride.bookmark = null;
        } else {
            ride.bookmark = Bookmark.BOOKMARK;
            Answers.getInstance().logCustom(new CustomEvent("Bookmark - set"));
        }

        updateFavoriteIcon();
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

        Call<ResponseBody> response = ApiClient.getAdapter().setRideBookmark(String.valueOf(ride.id), Bookmark.shouldShow(ride.bookmark) ? "bookmark" : "erase");
        response.enqueue(new Callback<ResponseBody>() {

            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                Log.i("Prevoz", "Bookmark set OK.");
                EventBus.getDefault().postSticky(new Events.MyRideStatusUpdated(ride, false));
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable throwable) {
                Log.e("Prevoz", "Failed to set bookmark status.", throwable);
                Crashlytics.logException(throwable);
            }
        });
    }

    @OnCheckedChanged(R.id.rideinfo_ridefull)
    protected void onFullClicked() {
        chkFull.setEnabled(false);
        final boolean rideFull = chkFull.isChecked();

        Call<ResponseBody> call = ApiClient.getAdapter().setFull(String.valueOf(ride.id), rideFull ? PrevozApi.FULL_STATE_FULL : PrevozApi.FULL_STATE_AVAILABLE);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                ride.isFull = rideFull;
                chkFull.setEnabled(true);
                setPeopleText();
                EventBus.getDefault().post(new Events.MyRideStatusUpdated(ride, false));
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable throwable) {
                chkFull.setChecked(!rideFull);
                ViewUtils.showMessage(RideInfoActivity.this, "Stanja prevoza ni bilo mogoče spremeniti :(", true);
                chkFull.setEnabled(true);
            }
        });
    }

    protected void submitRide() {
        if (connectivityService.getActiveNetworkInfo() == null || !connectivityService.getActiveNetworkInfo().isConnected()) {
            EventBus.getDefault().postSticky(new Events.ShowMessage("Ni mogoče oddati prevoza, internet ni na voljo!", true));
            finish();
            return;
        }

        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage("Oddajam prevoz...");
        dialog.show();

        Call<RestStatus> call = ApiClient.getAdapter().postRide(ride);
        call.enqueue(new Callback<RestStatus>() {
            @Override
            public void onResponse(Call<RestStatus> call, Response<RestStatus> response) {
                try {
                    dialog.dismiss();
                } catch (IllegalArgumentException e) {
                    // Why does this happen?
                    return;
                }

                if (!response.isSuccessful() && response.code() == 403) {
                    EventBus.getDefault().postSticky(new Events.ShowMessage("Vaša prijava ni več veljavna, prosimo ponovno se prijavite.", true));
                    authUtils.logout().subscribeOn(Schedulers.io()).subscribe();
                } else {
                    if (response.body() == null || !("created".equals(response.body().status) || "updated".equals(response.body().status))) {
                        if (response.body().error != null && response.body().error.size() > 0) {
                            String firstKey = response.body().error.keySet().iterator().next();
                            EventBus.getDefault().postSticky(new Events.ShowMessage(response.body().error.get(firstKey).get(0), true));
                        }
                    } else {
                        EventBus.getDefault().postSticky(new Events.ShowMessage(R.string.newride_publish_success, false));
                        EventBus.getDefault().post(new Events.MyRideStatusUpdated(ride, false));
                        setResult(Activity.RESULT_OK);
                        Answers.getInstance().logCustom(new CustomEvent("New ride submitted"));
                    }
                }

                finish();
            }

            @Override
            public void onFailure(Call<RestStatus> call, Throwable throwable) {
                if (dialog.isShowing()) dialog.dismiss();
                Crashlytics.logException(throwable);
                EventBus.getDefault().postSticky(new Events.ShowMessage(R.string.newride_publish_failure, true));
                setResult(Activity.RESULT_CANCELED);
                finish();
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (detector == null) return false;
        return detector.onTouchEvent(event);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelable("ride", ride);
        outState.putString("action", action);
    }
}
