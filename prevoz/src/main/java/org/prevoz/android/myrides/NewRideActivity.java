package org.prevoz.android.myrides;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;

import com.rengwuxian.materialedittext.MaterialAutoCompleteTextView;
import com.rengwuxian.materialedittext.MaterialEditText;
import com.rengwuxian.materialedittext.validation.METValidator;
import com.rengwuxian.materialedittext.validation.RegexpValidator;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;
import com.wdullaer.materialdatetimepicker.time.RadialPickerLayout;
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;

import org.prevoz.android.R;
import org.prevoz.android.api.rest.RestRide;
import org.prevoz.android.events.Events;
import org.prevoz.android.model.City;
import org.prevoz.android.model.CityNameTextValidator;
import org.prevoz.android.ride.RideInfoActivity;
import org.prevoz.android.search.CityAutocompleteAdapter;
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

public class NewRideActivity extends PrevozActivity implements DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener {

    private static final String PREF_PHONE_NO = "org.prevoz.phoneno";
    private static final String PREF_HAS_INSURANCE = "org.prevoz.hasinsurance";

    public static final String PARAM_EDIT_RIDE = "EditRide";

    @InjectView(R.id.toolbar)
    protected Toolbar toolbar;
    @InjectView(R.id.newride_from)
    protected MaterialAutoCompleteTextView textFrom;
    @InjectView(R.id.newride_to)
    protected MaterialAutoCompleteTextView textTo;
    @InjectView(R.id.newride_date_edit)
    protected MaterialEditText textDate;
    @InjectView(R.id.newride_time_edit)
    protected MaterialEditText textTime;
    @InjectView(R.id.newride_price)
    protected MaterialEditText textPrice;
    @InjectView(R.id.newride_phone)
    protected MaterialEditText textPhone;
    @InjectView(R.id.newride_people)
    protected MaterialEditText textPeople;
    @InjectView(R.id.newride_notes)
    protected MaterialEditText textNotes;
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
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                shouldGoFromDateToTime = true;
                onDateClicked();
                textTo.clearFocus();
                textPrice.requestFocus();
                return true;
            }

            return false;
        });

        textNotes.setOnEditorActionListener((textView, actionId, keyEvent) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
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
        textTo.setEnabled(false);
        textDate.setEnabled(false);
        chkInsurance.setEnabled(false);
        textPhone.setEnabled(false);
        textPrice.setEnabled(false);
        textPeople.requestFocus();

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

		RideInfoActivity.show(this, ride, RideInfoActivity.PARAM_ACTION_SUBMIT);
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
            textDate.setText(LocaleUtil.localizeDate(getResources(), setTime));

        if (time)
            textTime.setText(LocaleUtil.getFormattedTime(setTime));
    }

    private boolean validateForm()
    {

        // & as a logical operator doesn't short circuit
        return textPhone.validateWith(new RegexpValidator(getString(R.string.newride_error_phone), "[0-9\\+ ]{9,}")) &
               textPeople.validateWith(new RegexpValidator(getString(R.string.newride_error_people_num), "[1-6]")) &
               textPrice.validateWith(new RegexpValidator(getString(R.string.newride_error_price_invalid), "[0-9.,]{1,6}")) &
               textTime.validateWith(new RegexpValidator(getString(R.string.newride_error_time_missing), ".+")) &
               textDate.validateWith(new RegexpValidator(getString(R.string.newride_error_date_missing), ".+")) &
               textFrom.validateWith(new RegexpValidator(getString(R.string.newride_error_from_missing), ".+")) &
               textTo.validateWith(new RegexpValidator(getString(R.string.newride_error_to_missing), ".+")) &
               textTime.validateWith(new METValidator(getString(R.string.newride_error_date_passed)) {
                    @Override
                    public boolean isValid(CharSequence charSequence, boolean b) {
                        return Calendar.getInstance(LocaleUtil.getLocalTimezone()).before(setTime);
                    }
               });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Icepick.saveInstanceState(this, outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        EventBus.getDefault().registerSticky(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.slide_down);
    }

    public void onEventMainThread(Events.ShowMessage message) {
        ViewUtils.showMessage(this, message.getMessage(this), true);
        EventBus.getDefault().removeStickyEvent(message);
    }
}
