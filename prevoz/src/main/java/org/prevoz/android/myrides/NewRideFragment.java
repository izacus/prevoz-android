package org.prevoz.android.myrides;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.TimePicker;

import com.crashlytics.android.Crashlytics;
import com.fourmob.datetimepicker.date.DatePickerDialog;
import com.sleepbot.datetimepicker.time.RadialPickerLayout;
import com.sleepbot.datetimepicker.time.TimePickerDialog;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.InstanceState;
import org.androidannotations.annotations.ViewById;
import org.prevoz.android.MainActivity;
import org.prevoz.android.R;
import org.prevoz.android.UiFragment;
import org.prevoz.android.api.ApiClient;
import org.prevoz.android.api.rest.RestRide;
import org.prevoz.android.api.rest.RestStatus;
import org.prevoz.android.auth.AuthenticationUtils;
import org.prevoz.android.events.Events;
import org.prevoz.android.model.City;
import org.prevoz.android.model.CityNameTextValidator;
import org.prevoz.android.ride.RideInfoFragment;
import org.prevoz.android.ride.RideInfoListener;
import org.prevoz.android.search.CityAutocompleteAdapter;
import org.prevoz.android.ui.FloatingHintEditText;
import org.prevoz.android.util.LocaleUtil;
import org.prevoz.android.util.StringUtil;
import org.prevoz.android.util.ViewUtils;

import java.util.Calendar;

import de.greenrobot.event.EventBus;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

@EFragment(R.layout.fragment_newride)
public class NewRideFragment extends Fragment implements DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener, RideInfoListener, android.app.DatePickerDialog.OnDateSetListener, android.app.TimePickerDialog.OnTimeSetListener {
    private static final String PREF_PHONE_NO = "org.prevoz.phoneno";
    private static final String PREF_HAS_INSURANCE = "org.prevoz.hasinsurance";

    public static final String PARAM_EDIT_RIDE = "EditRide";

    @ViewById(R.id.newride_from)
    protected AutoCompleteTextView textFrom;
    @ViewById(R.id.newride_to)
    protected AutoCompleteTextView textTo;
    @ViewById(R.id.newride_date_edit)
    protected FloatingHintEditText textDate;
    @ViewById(R.id.newride_time_edit)
    protected FloatingHintEditText textTime;
    @ViewById(R.id.newride_price)
    protected FloatingHintEditText textPrice;
    @ViewById(R.id.newride_phone)
    protected FloatingHintEditText textPhone;
    @ViewById(R.id.newride_people)
    protected FloatingHintEditText textPeople;
    @ViewById(R.id.newride_notes)
    protected FloatingHintEditText textNotes;
    @ViewById(R.id.newride_insurance)
    protected CheckBox chkInsurance;

    @Bean
    protected AuthenticationUtils authUtils;

    @InstanceState
    protected Calendar setTime;
    @InstanceState
    protected boolean dateSet;
    @InstanceState
    protected boolean timeSet;

    @InstanceState
    protected Long editRideId;

    private boolean shouldGoFromDateToTime = false;

    @AfterViews
    protected void initFragment()
    {
        setTime = Calendar.getInstance(LocaleUtil.getLocalTimezone());

        // Round time to nearest 30 mins
        if (setTime.get(Calendar.MINUTE) >= 45 || setTime.get(Calendar.MINUTE) <= 15)
            setTime.set(Calendar.MINUTE, 0);
        else
            setTime.set(Calendar.MINUTE, 30);

        textFrom.setAdapter(new CityAutocompleteAdapter(getActivity()));
        textTo.setAdapter(new CityAutocompleteAdapter(getActivity()));
        textFrom.setValidator(new CityNameTextValidator(getActivity()));
        textTo.setValidator(new CityNameTextValidator(getActivity()));

        // Setup IME actions
        textTo.setOnEditorActionListener(new TextView.OnEditorActionListener()
        {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
            {
                if (actionId == EditorInfo.IME_ACTION_NEXT)
                {
                    shouldGoFromDateToTime = true;
                    clickDate();
                    textTo.clearFocus();
                    textPrice.requestFocus();
                    return true;
                }

                return false;
            }
        });

        textNotes.setOnEditorActionListener(new TextView.OnEditorActionListener()
        {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent)
            {
                if (actionId == EditorInfo.IME_ACTION_SEND)
                {
                    clickSubmit();
                    return true;
                }

                return false;
            }
        });

        if (getArguments() != null)
        {
            RestRide editRide = getArguments().getParcelable(PARAM_EDIT_RIDE);
            fillInEditRide(editRide);
        }
        else
        {
            // Load defaults from preferences
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
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

    @Click(R.id.newride_date_edit)
    protected void clickDate()
    {
        ViewUtils.hideKeyboard(getActivity());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            DatePickerDialog dialog = DatePickerDialog.newInstance(this,
                    setTime.get(Calendar.YEAR),
                    setTime.get(Calendar.MONTH),
                    setTime.get(Calendar.DAY_OF_MONTH),
                    false);
            dialog.show(getActivity().getSupportFragmentManager(), "NewDate");
        } else {
            android.app.DatePickerDialog dialog = new android.app.DatePickerDialog(getActivity(), this, setTime.get(Calendar.YEAR), setTime.get(Calendar.MONTH), setTime.get(Calendar.DAY_OF_MONTH));
            dialog.show();
        }
    }

    @Click(R.id.newride_time_edit)
    protected void clickTime()
    {
        if (getActivity() == null | !isAdded()) return;
        ViewUtils.hideKeyboard(getActivity());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            TimePickerDialog dialog = TimePickerDialog.newInstance(this,
                    setTime.get(Calendar.HOUR_OF_DAY),
                    setTime.get(Calendar.MINUTE),
                    true,
                    false);
            dialog.show(getActivity().getSupportFragmentManager(), "NewTime");
        } else {
            android.app.TimePickerDialog dialog = new android.app.TimePickerDialog(getActivity(), this, setTime.get(Calendar.HOUR_OF_DAY), setTime.get(Calendar.MINUTE), true);
            dialog.show();
        }
    }

    @Click(R.id.newride_button)
    protected void clickSubmit()
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

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.edit().putString(PREF_PHONE_NO, textPhone.getText().toString()).putBoolean(PREF_HAS_INSURANCE, chkInsurance.isChecked()).apply();

        if (editRideId != null)
            ride.id = editRideId;

        RideInfoFragment rideInfo = RideInfoFragment.newInstance(ride, RideInfoFragment.PARAM_ACTION_SUBMIT);
        rideInfo.setRideInfoListener(this);
        rideInfo.show(getActivity().getSupportFragmentManager(), "RideInfo");
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
            clickTime();
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
            error = getActivity().getString(R.string.newride_error_phone);
            textPhone.setFloatingHintColor(Color.RED);
        }

        try
        {
            int people = Integer.parseInt(textPeople.getText().toString());
            if (people < 1 || people > 6)
            {
                error = getActivity().getString(R.string.newride_error_people_num);
                textPeople.setFloatingHintColor(Color.RED);
            }
        }
        catch (NumberFormatException e)
        {
            textPeople.setFloatingHintColor(Color.RED);
            error = getActivity().getString(R.string.newride_error_people_missing);
        }

        try
        {
            double price = Double.parseDouble(textPrice.getText().toString());
            if (price < 0 || price > 100)
            {
                error = getActivity().getString(R.string.newride_error_price_invalid);
                textPrice.setFloatingHintColor(Color.RED);
            }
        }
        catch (NumberFormatException e)
        {
            error = getActivity().getString(R.string.newride_error_price_invalid);
            textPrice.setFloatingHintColor(Color.RED);
        }

        if (!timeSet)
        {
            error = getActivity().getString(R.string.newride_error_time_missing);
        }
        else if (!dateSet)
        {
            error = getActivity().getString(R.string.newride_error_date_missing);
        }
        else if (setTime.getTimeInMillis() < System.currentTimeMillis())
        {
            error = getActivity().getString(R.string.newride_error_date_passed);
            textDate.setFloatingHintColor(Color.RED);
            textTime.setFloatingHintColor(Color.RED);
        }


        if (textTo.getText().length() == 0)
        {
            error = getActivity().getString(R.string.newride_error_to_missing);
        }

        if (textFrom.getText().length() == 0)
        {
            error = getActivity().getString(R.string.newride_error_from_missing);
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
        ViewUtils.showMessage(getActivity(), message, true);
    }

    @Override
    public void onLeftButtonClicked(RestRide r)
    {
        // Canceled, nothing to be done.
    }

    @Override
    public void onRightButtonClicked(RestRide r)
    {
        final Activity activity = getActivity();
        if (activity == null) return;

        final ProgressDialog dialog = new ProgressDialog(activity);
        dialog.setMessage("Oddajam prevoz...");
        dialog.show();

        // TODO: remove when server timezone parsing is fixed
        r.date.setTimeZone(LocaleUtil.getLocalTimezone());
        ApiClient.getAdapter().postRide(r, new Callback<RestStatus>()
        {
            @Override
            public void success(RestStatus status, Response response)
            {
                try
                {
                    dialog.dismiss();
                }
                catch (IllegalArgumentException e)
                {
                    // Why does this happen?
                    return;
                }

                if (!("created".equals(status.status) || "updated".equals(status.status)))
                {
                    if (status.error != null && status.error.size() > 0)
                    {
                        String firstKey = status.error.keySet().iterator().next();
                        final MainActivity activity = (MainActivity) getActivity();
                        if (activity == null) return;
                        ViewUtils.showMessage(activity, status.error.get(firstKey).get(0), true);
                    }
                }
                else
                {
                    final MainActivity activity = (MainActivity) getActivity();
                    if (activity == null) return;
                    ViewUtils.showMessage(activity, R.string.newride_publish_success, false);
                    EventBus.getDefault().post(new Events.ShowFragment(UiFragment.FRAGMENT_MY_RIDES, false));
                }
            }

            @Override
            public void failure(RetrofitError error)
            {
                Crashlytics.logException(error.getCause());
                error.printStackTrace();

                final MainActivity activity = (MainActivity) getActivity();
                if (activity == null || !isAdded()) return;

                if (dialog.isShowing()) dialog.dismiss();
                ViewUtils.showMessage(activity, R.string.newride_publish_failure, true);
            }
        });
    }

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        onDateSet((DatePickerDialog)null, year, monthOfYear, dayOfMonth);
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        onTimeSet((RadialPickerLayout)null, hourOfDay, minute);
    }
}
