package org.prevoz.android.myrides;

import android.app.ProgressDialog;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.app.Fragment;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;
import com.fourmob.datetimepicker.date.DatePickerDialog;
import com.googlecode.androidannotations.annotations.*;
import com.sleepbot.datetimepicker.time.RadialPickerLayout;
import com.sleepbot.datetimepicker.time.TimePickerDialog;
import org.prevoz.android.MainActivity;
import org.prevoz.android.R;
import org.prevoz.android.UiFragment;
import org.prevoz.android.api.ApiClient;
import org.prevoz.android.api.rest.RestRide;
import org.prevoz.android.api.rest.RestStatus;
import org.prevoz.android.model.City;
import org.prevoz.android.model.CityNameTextValidator;
import org.prevoz.android.ride.RideInfoFragment;
import org.prevoz.android.ride.RideInfoListener;
import org.prevoz.android.search.CityAutocompleteAdapter;
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
    protected AutoCompleteTextView textFrom;
    @ViewById(R.id.newride_to)
    protected AutoCompleteTextView textTo;
    @ViewById(R.id.newride_date_edit)
    protected EditText textDate;
    @ViewById(R.id.newride_time_edit)
    protected EditText textTime;
    @ViewById(R.id.newride_price)
    protected EditText textPrice;
    @ViewById(R.id.newride_phone)
    protected EditText textPhone;
    @ViewById(R.id.newride_people)
    protected EditText textPeople;
    @ViewById(R.id.newride_notes)
    protected EditText textNotes;
    @ViewById(R.id.newride_insurance)
    protected CheckBox chkInsurance;

    @InstanceState
    protected Calendar setTime;
    @InstanceState
    protected boolean dateSet;
    @InstanceState
    protected boolean timeSet;

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
        textDate.setText(LocaleUtil.getShortFormattedDate(getResources(), setTime));
    }

    @Override
    public void onTimeSet(RadialPickerLayout radialPickerLayout, int hour, int minute)
    {
        setTime.set(Calendar.HOUR_OF_DAY, hour);
        setTime.set(Calendar.MINUTE, minute);

        timeSet = true;
        textTime.setText(timeFormat.format(setTime.getTime()));
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
                    activity.showFragment(UiFragment.FRAGMENT_MY_RIDES);
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
