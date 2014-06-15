package org.prevoz.android.myrides;

import android.app.ProgressDialog;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.support.v4.app.Fragment;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;
import com.fourmob.datetimepicker.date.DatePickerDialog;
import com.sleepbot.datetimepicker.time.RadialPickerLayout;
import com.sleepbot.datetimepicker.time.TimePickerDialog;
import org.androidannotations.annotations.*;
import org.prevoz.android.MainActivity;
import org.prevoz.android.R;
import org.prevoz.android.UiFragment;
import org.prevoz.android.api.ApiClient;
import org.prevoz.android.api.rest.RestRide;
import org.prevoz.android.model.City;
import org.prevoz.android.model.CityNameTextValidator;
import org.prevoz.android.ride.RideInfoFragment;
import org.prevoz.android.ride.RideInfoListener;
import org.prevoz.android.search.CityAutocompleteAdapter;
import org.prevoz.android.ui.FloatingHintAutocompleteEditText;
import org.prevoz.android.ui.FloatingHintEditText;
import org.prevoz.android.util.Database;
import org.prevoz.android.util.LocaleUtil;
import org.prevoz.android.util.StringUtil;
import org.prevoz.android.util.ViewUtils;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

import java.text.SimpleDateFormat;
import java.util.Calendar;

@EFragment(R.layout.fragment_newride)
public class NewRideFragment extends Fragment implements DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener, RideInfoListener
{
    public static String PARAM_EDIT_RIDE = "EditRide";

    private static SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", LocaleUtil.getLocale());

    @ViewById(R.id.newride_from)
    protected FloatingHintAutocompleteEditText textFrom;
    @ViewById(R.id.newride_to)
    protected FloatingHintAutocompleteEditText textTo;
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

    @InstanceState
    protected Calendar setTime;
    @InstanceState
    protected boolean dateSet;
    @InstanceState
    protected boolean timeSet;

    @InstanceState
    protected Long editRideId;

    @AfterViews
    protected void initFragment()
    {
        setTime = Calendar.getInstance(LocaleUtil.getLocale());

        // Round time to nearest 30 mins
        if (setTime.get(Calendar.MINUTE) >= 45 || setTime.get(Calendar.MINUTE) <= 15)
            setTime.set(Calendar.MINUTE, 0);
        else
            setTime.set(Calendar.MINUTE, 30);

        SQLiteDatabase db = Database.getSettingsDatabase(getActivity());
        textFrom.setAdapter(new CityAutocompleteAdapter(getActivity(), db));
        textTo.setAdapter(new CityAutocompleteAdapter(getActivity(), db));
        textFrom.setValidator(new CityNameTextValidator(getActivity()));
        textTo.setValidator(new CityNameTextValidator(getActivity()));

        if (getArguments() != null)
        {
            RestRide editRide = getArguments().getParcelable(PARAM_EDIT_RIDE);
            fillInEditRide(editRide);
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
        setTime = Calendar.getInstance(LocaleUtil.getLocale());
        setTime.setTime(editRide.date);
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
    }

    @Click(R.id.newride_date_edit)
    protected void clickDate()
    {
        ViewUtils.hideKeyboard(getActivity());
        DatePickerDialog dialog = DatePickerDialog.newInstance(this,
                                                                setTime.get(Calendar.YEAR),
                                                                setTime.get(Calendar.MONTH),
                                                                setTime.get(Calendar.DAY_OF_MONTH),
                                                                false);
        dialog.show(getActivity().getSupportFragmentManager(), "NewDate");
    }

    @Click(R.id.newride_time_edit)
    protected void clickTime()
    {
        ViewUtils.hideKeyboard(getActivity());
        TimePickerDialog dialog = TimePickerDialog.newInstance(this,
                                                                setTime.get(Calendar.HOUR_OF_DAY),
                                                                setTime.get(Calendar.MINUTE),
                                                                true,
                                                                false);
        dialog.show(getActivity().getSupportFragmentManager(), "NewTime");
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
                                      setTime.getTime(),
                                      textPhone.getText().toString(),
                                      chkInsurance.isChecked(),
                                      textNotes.getText().toString());

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
    }

    @Override
    public void onTimeSet(RadialPickerLayout radialPickerLayout, int hour, int minute)
    {
        setTime.set(Calendar.HOUR_OF_DAY, hour);
        setTime.set(Calendar.MINUTE, minute);

        timeSet = true;
        updateDateTimeDisplay(false, true);
    }

    protected void updateDateTimeDisplay(boolean date, boolean time)
    {
        if (date)
            textDate.setText(LocaleUtil.getShortFormattedDate(getResources(), setTime));

        if (time)
            textTime.setText(timeFormat.format(setTime.getTime()));
    }

    private boolean validateForm()
    {
        textPhone.setFloatingHintColor(null);
        textPeople.setFloatingHintColor(null);
        textPrice.setFloatingHintColor(null);
        textTime.setFloatingHintColor(null);
        textDate.setFloatingHintColor(null);
        textTo.setFloatingHintColor(null);
        textFrom.setFloatingHintColor(null);

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
            textTo.setFloatingHintColor(Color.RED);
        }

        if (textFrom.getText().length() == 0)
        {
            error = getActivity().getString(R.string.newride_error_from_missing);
            textFrom.setFloatingHintColor(Color.RED);
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
        Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLeftButtonClicked(RestRide r)
    {
        // Canceled, nothing to be done.
    }

    @Override
    public void onRightButtonClicked(RestRide r)
    {
        final ProgressDialog dialog = new ProgressDialog(getActivity());
        dialog.setMessage("Oddajam prevoz...");
        dialog.show();

        ApiClient.getAdapter().postRide(r, new Callback<Response>()
        {
            @Override
            public void success(Response response, Response response2)
            {
                dialog.dismiss();
                Toast.makeText(getActivity(), "Prevoz je bil oddan.", Toast.LENGTH_SHORT).show();

                MainActivity activity = (MainActivity) getActivity();
                if (activity != null)
                    activity.showFragment(UiFragment.FRAGMENT_MY_RIDES, false);
            }

            @Override
            public void failure(RetrofitError error)
            {
                dialog.dismiss();
                // TODO: Error message.
            }
        });
    }
}
