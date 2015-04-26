package org.prevoz.android.myrides;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;

import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;
import com.wdullaer.materialdatetimepicker.time.RadialPickerLayout;
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;

import org.prevoz.android.R;
import org.prevoz.android.UiFragment;
import org.prevoz.android.api.ApiClient;
import org.prevoz.android.api.rest.RestRide;
import org.prevoz.android.api.rest.RestStatus;
import org.prevoz.android.events.Events;
import org.prevoz.android.model.City;
import org.prevoz.android.model.CityNameTextValidator;
import org.prevoz.android.ride.RideInfoFragment;
import org.prevoz.android.ride.RideInfoListener;
import org.prevoz.android.search.CityAutocompleteAdapter;
import org.prevoz.android.ui.FloatingHintEditText;
import org.prevoz.android.util.LocaleUtil;
import org.prevoz.android.util.PrevozActivity;
import org.prevoz.android.util.StringUtil;
import org.prevoz.android.util.ViewUtils;

import java.util.Calendar;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import de.greenrobot.event.EventBus;
import icepick.Icepick;
import icepick.Icicle;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import rx.schedulers.Schedulers;

public class NewRideActivity extends PrevozActivity implements DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener, RideInfoListener {

    private static final String PREF_PHONE_NO = "org.prevoz.phoneno";
    private static final String PREF_HAS_INSURANCE = "org.prevoz.hasinsurance";

    public static final String PARAM_EDIT_RIDE = "EditRide";

    @InjectView(R.id.toolbar)
    protected Toolbar toolbar;
    @InjectView(R.id.newride_from)
    protected AutoCompleteTextView textFrom;
    @InjectView(R.id.newride_to)
    protected AutoCompleteTextView textTo;
    @InjectView(R.id.newride_date_edit)
    protected FloatingHintEditText textDate;
    @InjectView(R.id.newride_time_edit)
    protected FloatingHintEditText textTime;
    @InjectView(R.id.newride_price)
    protected FloatingHintEditText textPrice;
    @InjectView(R.id.newride_phone)
    protected FloatingHintEditText textPhone;
    @InjectView(R.id.newride_people)
    protected FloatingHintEditText textPeople;
    @InjectView(R.id.newride_notes)
    protected FloatingHintEditText textNotes;
    @InjectView(R.id.newride_insurance)
    protected CheckBox chkInsurance;

    @Icicle
    protected Calendar setTime;
    @Icicle protected boolean dateSet;
    @Icicle protected boolean timeSet;
    @Icicle protected Long editRideId;

    private boolean shouldGoFromDateToTime = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_newride);
        ButterKnife.inject(this);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setTime = Calendar.getInstance(LocaleUtil.getLocalTimezone());

        // Round time to nearest 30 mins
        if (setTime.get(Calendar.MINUTE) >= 45 || setTime.get(Calendar.MINUTE) <= 15)
            setTime.set(Calendar.MINUTE, 0);
        else
            setTime.set(Calendar.MINUTE, 30);

        textFrom.setAdapter(new CityAutocompleteAdapter(this));
        textTo.setAdapter(new CityAutocompleteAdapter(this));
        textFrom.setValidator(new CityNameTextValidator(this));
        textTo.setValidator(new CityNameTextValidator(this));

        // Setup IME actions
        textTo.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT)
            {
                shouldGoFromDateToTime = true;
                onDateClicked();
                textTo.clearFocus();
                textPrice.requestFocus();
                return true;
            }

            return false;
        });

        textNotes.setOnEditorActionListener((textView, actionId, keyEvent) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND)
            {
                onSubmitClicked();
                return true;
            }

            return false;
        });

        RestRide editRide = getIntent().getParcelableExtra(PARAM_EDIT_RIDE);
        if (editRide != null)
        {
            fillInEditRide(editRide);
        }
        else
        {
            // Load defaults from preferences
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            textPhone.setText(prefs.getString(PREF_PHONE_NO, ""));
            chkInsurance.setChecked(prefs.getBoolean(PREF_HAS_INSURANCE, false));
        }
    }

    protected void fillInEditRide(RestRide editRide)
    {
        if (editRide == null)
            return;

        City from = new City(editRide.fromCity, editRide.fromCountry);
        City to = new City(editRide.toCity, editRide.toCountry);

        textFrom.setText(from.toString());
        textTo.setText(to.toString());
        textPrice.setText(String.valueOf(editRide.price));
        textPeople.setText(String.valueOf(editRide.numPeople));
        textNotes.setText(editRide.comment);
        textPhone.setText(editRide.phoneNumber);
        chkInsurance.setChecked(editRide.insured);
        setTime = editRide.date;
        updateDateTimeDisplay(true, true);
        editRideId = editRide.id;

        // Now disable fields user should not edit
        textFrom.setEnabled(false);
        textFrom.setBackgroundDrawable(null);
        textTo.setEnabled(false);
        textTo.setBackgroundDrawable(null);
        textDate.setEnabled(false);
        textDate.setBackgroundDrawable(null);
        chkInsurance.setEnabled(false);
        textPhone.setEnabled(false);
        textPhone.setBackgroundDrawable(null);
        textPrice.setEnabled(false);
        textPrice.setBackgroundDrawable(null);

        dateSet = true;
        timeSet = true;
    }


    @OnClick(R.id.newride_date_edit)
    protected void onDateClicked()
    {
        ViewUtils.hideKeyboard(this);
        DatePickerDialog dialog = DatePickerDialog.newInstance(this,
                                                                setTime.get(Calendar.YEAR),
                                                                setTime.get(Calendar.MONTH),
                                                                setTime.get(Calendar.DAY_OF_MONTH));
        dialog.show(getFragmentManager(), "NewDate");
    }

    @OnClick(R.id.newride_time_edit)
    protected void onTimeClicked()
    {
        ViewUtils.hideKeyboard(this);
        TimePickerDialog dialog = TimePickerDialog.newInstance(this,
                setTime.get(Calendar.HOUR_OF_DAY),
                setTime.get(Calendar.MINUTE),
                true);
        dialog.show(getFragmentManager(), "NewTime");
    }

    @OnClick(R.id.newride_button)
    protected void onSubmitClicked()
    {
        if (!validateForm())
            return;

        City from = StringUtil.splitStringToCity(textFrom.getText().toString());
        City to = StringUtil.splitStringToCity(textTo.getText().toString());

        RestRide ride = new RestRide(from.getDisplayName(),
                from.getCountryCode(),
                to.getDisplayName(),
                to.getCountryCode(),
                Float.parseFloat(textPrice.getText().toString()),
                Integer.parseInt(textPeople.getText().toString()),
                setTime,
                textPhone.getText().toString(),
                chkInsurance.isChecked(),
                textNotes.getText().toString());
        ride.published = Calendar.getInstance(LocaleUtil.getLocale());

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putString(PREF_PHONE_NO, textPhone.getText().toString()).putBoolean(PREF_HAS_INSURANCE, chkInsurance.isChecked()).apply();

        if (editRideId != null)
            ride.id = editRideId;

        RideInfoFragment rideInfo = RideInfoFragment.newInstance(ride, RideInfoFragment.PARAM_ACTION_SUBMIT);
        rideInfo.setRideInfoListener(this);
        rideInfo.show(getSupportFragmentManager(), "RideInfo");
    }

    @Override
    public void onDateSet(DatePickerDialog datePickerDialog, int year, int month, int day)
    {
        setTime.set(Calendar.YEAR, year);
        setTime.set(Calendar.MONTH, month);
        setTime.set(Calendar.DAY_OF_MONTH, day);

        dateSet = true;
        updateDateTimeDisplay(true, false);

        if (shouldGoFromDateToTime)
        {
            shouldGoFromDateToTime = false;
            onTimeClicked();
        }
    }

    @Override
    public void onTimeSet(RadialPickerLayout radialPickerLayout, int hour, int minute)
    {
        setTime.set(Calendar.HOUR_OF_DAY, hour);
        setTime.set(Calendar.MINUTE, minute);

        timeSet = true;
        updateDateTimeDisplay(false, true);
        textPrice.requestFocus();
    }

    protected void updateDateTimeDisplay(boolean date, boolean time)
    {
        if (date)
            textDate.setText(LocaleUtil.getShortFormattedDate(getResources(), setTime));

        if (time)
            textTime.setText(LocaleUtil.getFormattedTime(setTime));
    }

    private boolean validateForm()
    {
        textPhone.setFloatingHintColor(null);
        textPeople.setFloatingHintColor(null);
        textPrice.setFloatingHintColor(null);
        textTime.setFloatingHintColor(null);
        textDate.setFloatingHintColor(null);

        String error = null;

        if (textPhone.getText().toString().length() == 0)
        {
            error = getString(R.string.newride_error_phone);
            textPhone.setFloatingHintColor(Color.RED);
        }

        try
        {
            int people = Integer.parseInt(textPeople.getText().toString());
            if (people < 1 || people > 6)
            {
                error = getString(R.string.newride_error_people_num);
                textPeople.setFloatingHintColor(Color.RED);
            }
        }
        catch (NumberFormatException e)
        {
            textPeople.setFloatingHintColor(Color.RED);
            error = getString(R.string.newride_error_people_missing);
        }

        try
        {
            double price = Double.parseDouble(textPrice.getText().toString());
            if (price < 0 || price > 100)
            {
                error = getString(R.string.newride_error_price_invalid);
                textPrice.setFloatingHintColor(Color.RED);
            }
        }
        catch (NumberFormatException e)
        {
            error = getString(R.string.newride_error_price_invalid);
            textPrice.setFloatingHintColor(Color.RED);
        }

        if (!timeSet)
        {
            error = getString(R.string.newride_error_time_missing);
        }
        else if (!dateSet)
        {
            error = getString(R.string.newride_error_date_missing);
        }
        else if (setTime.getTimeInMillis() < System.currentTimeMillis())
        {
            error = getString(R.string.newride_error_date_passed);
            textDate.setFloatingHintColor(Color.RED);
            textTime.setFloatingHintColor(Color.RED);
        }


        if (textTo.getText().length() == 0)
        {
            error = getString(R.string.newride_error_to_missing);
        }

        if (textFrom.getText().length() == 0)
        {
            error = getString(R.string.newride_error_from_missing);
        }

        if (error != null)
        {
            showError(error);
            return false;
        }

        return true;
    }

    private void showError(String message)
    {
        ViewUtils.showMessage(this, message, true);
    }

    @Override
    public void onLeftButtonClicked(RestRide r)
    {
        // Canceled, nothing to be done.
    }

    @Override
    public void onRightButtonClicked(RestRide r)
    {
        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage("Oddajam prevoz...");
        dialog.show();

        // TODO: remove when server timezone parsing is fixed
        r.date.setTimeZone(LocaleUtil.getLocalTimezone());
        ApiClient.getAdapter().postRide(r, new Callback<RestStatus>() {
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
                        ViewUtils.showMessage(NewRideActivity.this, status.error.get(firstKey).get(0), true);
                    }
                } else {
                    finish();
                    EventBus.getDefault().postSticky(new Events.ShowMessage(R.string.newride_publish_success));
                    EventBus.getDefault().postSticky(new Events.ShowFragment(UiFragment.FRAGMENT_MY_RIDES, false));
                }
            }

            @Override
            public void failure(RetrofitError error) {
                if (dialog.isShowing()) dialog.dismiss();
                if (error.getResponse() != null && error.getResponse().getStatus() == 403) {
                    ViewUtils.showMessage(NewRideActivity.this, "Vaša prijava ni več veljavna, prosimo ponovno se prijavite.", true);
                    authUtils.logout().subscribeOn(Schedulers.io()).subscribe();
                    finish();
                    return;
                } else {
                    ViewUtils.showMessage(NewRideActivity.this, R.string.newride_publish_failure, true);
                }
            }
        });
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Icepick.saveInstanceState(this, outState);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.slide_down);
    }
}