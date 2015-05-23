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

import com.crashlytics.android.Crashlytics;

import org.prevoz.android.R;
import org.prevoz.android.UiFragment;
import org.prevoz.android.api.ApiClient;
import org.prevoz.android.api.PrevozApi;
import org.prevoz.android.api.rest.RestRide;
import org.prevoz.android.api.rest.RestStatus;
import org.prevoz.android.auth.AuthenticationUtils;
import org.prevoz.android.events.Events;
import org.prevoz.android.model.Bookmark;
import org.prevoz.android.myrides.NewRideActivity;
import org.prevoz.android.util.LocaleUtil;
import org.prevoz.android.util.PrevozActivity;
import org.prevoz.android.util.ViewUtils;

import java.text.SimpleDateFormat;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import de.greenrobot.event.EventBus;
import icepick.Icepick;
import icepick.Icicle;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import rx.schedulers.Schedulers;
import si.virag.fuzzydateformatter.FuzzyDateTimeFormatter;

public class RideInfoActivity extends PrevozActivity
{
    private static final SimpleDateFormat timeFormatter = LocaleUtil.getSimpleDateFormat("HH:mm");
    private static final String ARG_RIDE = "ride";
    private static final String ARG_ACTION = "action";

    public static final String PARAM_ACTION_SHOW = "show";
    public static final String PARAM_ACTION_EDIT = "edit";
    public static final String PARAM_ACTION_SUBMIT = "submit";

	public static void show(Activity parent, RestRide ride) {
		Intent i = new Intent(parent, RideInfoActivity.class);
		i.putExtra(ARG_RIDE, (Parcelable)ride);
		parent.startActivity(i);
	}

	public static void show(Activity parent, RestRide ride, String action) {
		Intent i = new Intent(parent, RideInfoActivity.class);
		i.putExtra(ARG_RIDE, (Parcelable)ride);
		i.putExtra(ARG_ACTION, action);
		parent.startActivity(i);
	}


	public static void show(Activity parent, RestRide ride, Bundle options) {
		Intent i = new Intent(parent, RideInfoActivity.class);
		i.putExtra(ARG_RIDE, (Parcelable)ride);
		ActivityCompat.startActivity(parent, i, options);
	}

	@InjectView(R.id.rideinfo_container)
	protected View container;

    @InjectView(R.id.rideinfo_favorite)
    protected ImageView imgFavorite;

    @InjectView(R.id.rideinfo_from)
    protected TextView txtFrom;

    @InjectView(R.id.rideinfo_to)
    protected TextView txtTo;

    @InjectView(R.id.rideinfo_time)
    protected TextView txtTime;

    @InjectView(R.id.rideinfo_price)
    protected TextView txtPrice;

    @InjectView(R.id.rideinfo_date)
    protected TextView txtDate;

    @InjectView(R.id.rideinfo_details)
    protected View vDetails;

    @InjectView(R.id.rideinfo_phone)
    protected TextView txtPhone;
    @InjectView(R.id.rideinfo_people)
    protected TextView txtPeople;
    @InjectView(R.id.rideinfo_insurance)
    protected TextView txtInsurance;
    @InjectView(R.id.rideinfo_driver)
    protected TextView txtDriver;
    @InjectView(R.id.rideinfo_comment)
    protected TextView txtComment;

    @InjectView(R.id.rideinfo_full_box)
    protected View vFull;
    @InjectView(R.id.rideinfo_ridefull)
    protected CheckBox chkFull;

    @InjectView(R.id.rideinfo_button_call)
    protected Button leftButton;
    @InjectView(R.id.rideinfo_button_sms)
    protected Button rightButton;

    @Inject
    protected AuthenticationUtils authUtils;

    @Icicle protected RestRide ride = null;
    @Icicle protected String action = null;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Icepick.restoreInstanceState(this, savedInstanceState);
		setContentView(R.layout.activity_rideinfo);
		ButterKnife.inject(this);
		getApplicationComponent().inject(this);

		ride = getIntent().getParcelableExtra(ARG_RIDE);
		action = getIntent().getStringExtra(ARG_ACTION);

        if (action == null)
            action = PARAM_ACTION_SHOW;

        if (PARAM_ACTION_SHOW.equals(action) && ride.isAuthor)
        {
            action = PARAM_ACTION_EDIT;
        }


		imgFavorite.setVisibility(authUtils.isAuthenticated() && !ride.isAuthor ? View.VISIBLE : View.INVISIBLE);
		updateFavoriteIcon();

		txtFrom.setText(LocaleUtil.getLocalizedCityName(this, ride.fromCity, ride.fromCountry));
		txtTo.setText(LocaleUtil.getLocalizedCityName(this, ride.toCity, ride.toCountry));
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
				txtDriver.setText(FuzzyDateTimeFormatter.getTimeAgo(this, ride.published.getTime()) + "\u00A0");   // Add non-breaking space at the end to prevent italic letter clipping
			} else {
				SpannableStringBuilder ssb = new SpannableStringBuilder();
				ssb.append(ride.author);
				ssb.append(", ");
				ssb.append(FuzzyDateTimeFormatter.getTimeAgo(this, ride.published.getTime()));
				ssb.setSpan(new StyleSpan(Typeface.BOLD), 0, ride.author.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
				ssb.append("\u00A0");
				txtDriver.setText(ssb);
			}
		}

		txtInsurance.setText(ride.insured ? "\u2713 Ima zavarovanje." : "\u2717 Nima zavarovanja.");

		// Hide call/SMS buttons on devices without telephony support
		PackageManager pm = getPackageManager();
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
        drawable.setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP));
        imgFavorite.setImageDrawable(drawable);
    }

    @OnClick(R.id.rideinfo_button_call)
    protected void onCallClicked()
    {
        if (PARAM_ACTION_SHOW.equals(action))
        {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + ride.phoneNumber));
            startActivity(intent);
        }
        else if (PARAM_ACTION_EDIT.equals(action))
        {
            Intent intent = new Intent(this, NewRideActivity.class);
            intent.putExtra(NewRideActivity.PARAM_EDIT_RIDE, (Parcelable)ride);
            ActivityCompat.startActivity(this, intent, ActivityOptionsCompat.makeCustomAnimation(this, R.anim.slide_up, 0).toBundle());
        }

        finish();
    }

    @OnClick(R.id.rideinfo_button_sms)
    protected void onSmsClicked()
    {
        if (PARAM_ACTION_SHOW.equals(action))
        {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("smsto:" + ride.phoneNumber));

            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, "Nimate nameščene nobene aplikacije za pošiljanje SMS sporočil.", Toast.LENGTH_SHORT).show();
            }

        }
        else if (PARAM_ACTION_EDIT.equals(action))
        {
            new AlertDialog.Builder(this, R.style.Prevoz_Theme_Dialog)
                            .setTitle(String.format("%s - %s", ride.fromCity, ride.toCity))
                            .setMessage(getString(R.string.ride_delete_message, LocaleUtil.getDayName(getResources(), ride.date).toLowerCase(LocaleUtil.getLocale()), LocaleUtil.getFormattedTime(ride.date)))
                            .setNegativeButton(R.string.ride_delete_cancel, null)
                            .setPositiveButton(R.string.ride_delete_ok, (dialog, which) -> {
                                final ProgressDialog deleteDialog = new ProgressDialog(this);
                                deleteDialog.setMessage(getString(R.string.ride_delete_progress));
                                deleteDialog.show();

                                ApiClient.getAdapter().deleteRide(String.valueOf(ride.id), new Callback<Response>() {
                                    @Override
                                    public void success(Response response, Response response2) {
                                        finish();
                                        EventBus.getDefault().post(new Events.MyRideStatusUpdated(ride, true));
                                        deleteDialog.dismiss();
                                        EventBus.getDefault().postSticky(new Events.ShowMessage(R.string.ride_delete_success));
                                    }

                                    @Override
                                    public void failure(RetrofitError error) {
                                        finish();
                                        deleteDialog.dismiss();
                                        EventBus.getDefault().postSticky(new Events.ShowMessage(R.string.ride_delete_failure));
                                    }
                                });
                            }).show();
        }
        else if (PARAM_ACTION_SUBMIT.equals(action)) {
            submitRide();
        }
        else
        {
            finish();
        }
    }

    @OnClick(R.id.rideinfo_favorite)
    protected void onFavoriteClicked() {
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
                EventBus.getDefault().postSticky(new Events.MyRideStatusUpdated(ride, false));
            }

            @Override
            public void failure(RetrofitError error) {
                Log.e("Prevoz", "Failed to set bookmark status.", error);
                Crashlytics.logException(error);
            }
        });
    }

    @OnCheckedChanged(R.id.rideinfo_ridefull)
    protected void onFullClicked()
    {
        chkFull.setEnabled(false);
        final boolean rideFull = chkFull.isChecked();

        ApiClient.getAdapter().setFull(String.valueOf(ride.id), rideFull ? PrevozApi.FULL_STATE_FULL : PrevozApi.FULL_STATE_AVAILABLE, new Callback<Response>() {
            @Override
            public void success(Response response, Response response2) {
                ride.isFull = rideFull;
                chkFull.setEnabled(true);
                setPeopleText();
            }

            @Override
            public void failure(RetrofitError retrofitError) {
                chkFull.setChecked(!rideFull);
                ViewUtils.showMessage(RideInfoActivity.this, "Stanja prevoza ni bilo mogoče spremeniti :(", true);
                chkFull.setEnabled(true);
            }
        });
    }

    protected void submitRide() {
        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage("Oddajam prevoz...");
        dialog.show();

        // TODO: remove when server timezone parsing is fixed
        ride.date.setTimeZone(LocaleUtil.getLocalTimezone());
        ApiClient.getAdapter().postRide(ride, new Callback<RestStatus>() {
            @Override
            public void success(RestStatus status, Response response) {
                try {
                    dialog.dismiss();
                } catch (IllegalArgumentException e) {
                    // Why does this happen?
                    return;
                }

                if (!("created".equals(status.status) || "updated".equals(status.status))) {
                    if (status.error != null && status.error.size() > 0) {
                        String firstKey = status.error.keySet().iterator().next();
                        EventBus.getDefault().postSticky(new Events.ShowMessage(status.error.get(firstKey).get(0)));
                    }
                } else {
                    EventBus.getDefault().postSticky(new Events.ShowMessage(R.string.newride_publish_success));
                    EventBus.getDefault().postSticky(new Events.ShowFragment(UiFragment.FRAGMENT_MY_RIDES, false));
                }

                finish();
            }

            @Override
            public void failure(RetrofitError error) {
                if (dialog.isShowing()) dialog.dismiss();
                if (error.getResponse() != null && error.getResponse().getStatus() == 403) {
                    EventBus.getDefault().postSticky(new Events.ShowMessage("Vaša prijava ni več veljavna, prosimo ponovno se prijavite."));
                    authUtils.logout().subscribeOn(Schedulers.io()).subscribe();
                } else {
                    EventBus.getDefault().postSticky(new Events.ShowMessage(R.string.newride_publish_failure));
                }

                finish();
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        Icepick.saveInstanceState(this, outState);
    }
}
