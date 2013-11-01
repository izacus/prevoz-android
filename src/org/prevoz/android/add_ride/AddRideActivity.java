package org.prevoz.android.add_ride;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

import org.prevoz.android.City;
import org.prevoz.android.CitySelectorActivity;
import org.prevoz.android.R;
import org.prevoz.android.RideType;
import org.prevoz.android.add_ride.AddStateManager.Views;
import org.prevoz.android.auth.AuthenticationManager;
import org.prevoz.android.auth.AuthenticationStatus;
import org.prevoz.android.my_rides.MyRidesActivity;
import org.prevoz.android.rideinfo.Ride;
import org.prevoz.android.rideinfo.RideInfoActivity;
import org.prevoz.android.rideinfo.RideInfoUtil;
import org.prevoz.android.util.LocaleUtil;
import org.prevoz.android.util.StringUtil;

import roboguice.inject.InjectView;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.actionbarsherlock.view.MenuItem;
import com.github.rtyley.android.sherlock.roboguice.activity.RoboSherlockFragmentActivity;

public class AddRideActivity extends RoboSherlockFragmentActivity
{
	private static final int FROM_CITY_REQUEST = 1;
	private static final int TO_CITY_REQUEST = 2;
	
	private static final String PREF_PHONE_NO = "org.prevoz.phoneno";
    private static final String LOG_TAG = "Prevoz.AddRide";

    private final SimpleDateFormat timeFormatter = LocaleUtil.getSimpleDateFormat("HH:mm");
	
	private AddStateManager stateManager;
	
	// Form buttons
	@InjectView(R.id.from_button)
	private Button fromButton;
	@InjectView(R.id.to_button)
	private Button toButton;
	@InjectView(R.id.date_button)
	private Button dateButton;
	@InjectView(R.id.time_button)
	private Button timeButton;
	@InjectView(R.id.add_button)
	private Button nextButton;
	@InjectView(R.id.add_ppl)
	private Spinner peopleSpinner;
	// Form fields
	@InjectView(R.id.add_price)
	private EditText priceText;
	@InjectView(R.id.add_phone)
	private EditText phoneText;
	@InjectView(R.id.add_comment)
	private EditText commentText;
	@InjectView(R.id.check_insurance)
	private CheckBox insuranceCheck;
	
	// Field data
	private City fromCity = null;
	private City toCity = null;
	
	private Calendar dateTime;

	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.add_ride_activity);
		
		// Update application title
		getSupportActionBar().setTitle(R.string.add_title);
		getSupportActionBar().setHomeButtonEnabled(true);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		dateTime = Calendar.getInstance(LocaleUtil.getLocalTimezone());
		
		// Initialize values
		if (savedInstanceState != null)
		{
			if (savedInstanceState.containsKey("fromCity"))
			{
				fromCity = new City(savedInstanceState.getString("fromCity"), savedInstanceState.getString("fromCityCountry"));
			}
			
			if (savedInstanceState.containsKey("toCity"))
			{
				toCity = new City(savedInstanceState.getString("toCity"), savedInstanceState.getString("toCityCountry"));
			}
			
			dateTime.setTimeInMillis(savedInstanceState.getLong("date"));
			prepareFormFields(savedInstanceState.getInt("numPpl"));
		}
		else
		{
			// Round minutes to the nearest hour
			dateTime.set(Calendar.MINUTE, 0);
			dateTime.set(Calendar.SECOND, 0);
			dateTime.roll(Calendar.HOUR_OF_DAY, 1);
			prepareFormFields(3);
		}
		
		updateDateTime();
		
		SharedPreferences preferences = this.getSharedPreferences(PREF_PHONE_NO, 0);
		phoneText.setText(preferences.getString(PREF_PHONE_NO, ""));
		
		Handler authHandler = new Handler()
		{
			@Override
			public void handleMessage(Message msg) 
			{
				authenticationStatusReceived(AuthenticationStatus.values()[msg.what]);
			}
		};
		
		AuthenticationManager.getInstance().getAuthenticationStatus(this, authHandler);
	}
	
	private void prepareFormFields(int peopleSelected)
	{
		// Prepare UI injection
		ViewFlipper addFlipper = (ViewFlipper) findViewById(R.id.add_flipper);
		stateManager = new AddStateManager(addFlipper);
		stateManager.showView(Views.LOADING);
		
		// From city selector
		fromButton.setOnClickListener(new OnClickListener() 
		{
			public void onClick(View v) 
			{
				Intent cityChooser = new Intent(AddRideActivity.this, CitySelectorActivity.class);
				startActivityForResult(cityChooser, FROM_CITY_REQUEST);
			}
		});
		StringUtil.setLocationButtonText(fromButton, fromCity, getString(R.string.add_select_city));
		
		// To city selector
		toButton.setOnClickListener(new OnClickListener() 
		{
			public void onClick(View v) 
			{
				Intent cityChooser = new Intent(AddRideActivity.this, CitySelectorActivity.class);
				startActivityForResult(cityChooser, TO_CITY_REQUEST);
			}
		});
		StringUtil.setLocationButtonText(toButton, toCity, getString(R.string.add_select_city));
		
		// Date selector
		dateButton.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				DatePickerFragment datePicker = new DatePickerFragment();
				datePicker.show(getSupportFragmentManager(), "datePicker");
			}
		});
		
		timeButton.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				TimePickerFragment timePicker = new TimePickerFragment();
				timePicker.show(getSupportFragmentManager(), "timePicker");
			}
		});
		
		// Prepare number of people spinner
		PeopleSpinnerObject[] peopleSpinnerObject = new PeopleSpinnerObject[6];
		for (int i = 0; i < 6; i++)
			peopleSpinnerObject[i] = new PeopleSpinnerObject(this, i + 1);
		
		ArrayAdapter<PeopleSpinnerObject> peopleAdapter = new ArrayAdapter<PeopleSpinnerObject>(this, android.R.layout.simple_spinner_item, peopleSpinnerObject);
		peopleSpinner.setAdapter(peopleAdapter);
		peopleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		peopleSpinner.setSelection(peopleSelected - 1);
		
		// Next button
		nextButton.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				showPreview();
			}
		});
	}
	
	private void updateDateTime()
	{
		String dateString = LocaleUtil.getDayName(getResources(), dateTime) + ", " + LocaleUtil.getFormattedDate(getResources(), dateTime);
		dateButton.setText(dateString);
		
		String timeString = timeFormatter.format(dateTime.getTime());
		timeButton.setText(timeString);
	}
	
	private void authenticationStatusReceived(AuthenticationStatus status)
	{
		Log.i(LOG_TAG, "Received authentication status: " + status);
		
		
		switch(status)
		{
			case UNKNOWN:
				Toast.makeText(this, R.string.network_error, Toast.LENGTH_LONG).show();
				finish();
				break;
				
			case NOT_AUTHENTICATED:				
				Log.i(LOG_TAG, "Opening user login request...");
				
				Handler loginHandler = new Handler()
				{
					@Override
					public void handleMessage(Message msg) 
					{
						AuthenticationStatus status = AuthenticationStatus.values()[msg.what];
						if (status != AuthenticationStatus.AUTHENTICATED)
						{
							finish();
						}
						else
						{
							stateManager.showView(Views.FORM);
						}
					}
				};
				
				AuthenticationManager.getInstance().requestLogin(this, loginHandler);
				break;
			
			case AUTHENTICATED:
				stateManager.showView(Views.FORM);
				break;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) 
	{
		super.onActivityResult(requestCode, resultCode, data);
		
		boolean successful = (resultCode == Activity.RESULT_OK);

		switch(requestCode)
		{
			case FROM_CITY_REQUEST:
				fromCity = successful ? new City(data.getStringExtra("city"), data.getStringExtra("country")) : null;
				StringUtil.setLocationButtonText(fromButton, fromCity, getString(R.string.add_select_city));
				break;
				
			case TO_CITY_REQUEST:
				toCity = successful ? new City(data.getStringExtra("city"), data.getStringExtra("country")) : null;
				StringUtil.setLocationButtonText(toButton, toCity, getString(R.string.add_select_city));
				break;
		}
		
	}

	private boolean validateForm()
	{
		
		// Check from city
		if (fromCity == null)
		{
			HashMap<String, String> params = new HashMap<String, String>();
			params.put("error", "No From");
			showFormError(getString(R.string.add_error_enterfrom));
			return false;
		}
		
		if (toCity == null)
		{
			HashMap<String, String> params = new HashMap<String, String>();
			params.put("error", "No To");
			showFormError(getString(R.string.add_error_enterto));
			return false;
		}
		
		// Check date validity
		if (dateTime.before(Calendar.getInstance(LocaleUtil.getLocalTimezone())))
		{
			HashMap<String, String> params = new HashMap<String, String>();
			params.put("error", "Date too early");
			showFormError(getString(R.string.add_error_timepast));
			return false;
		}
		
		// Check price
		
		if (priceText.getText().toString().trim().length() > 0)
		{
			double price;
			
			try
			{
				 price = Double.parseDouble(priceText.getText().toString());
			}
			catch (NumberFormatException e)
			{
				HashMap<String, String> params = new HashMap<String, String>();
				params.put("error", "Missing / Invalid price");
				showFormError(getString(R.string.add_error_enterprice));
				return false;
			}
			
			if (price < 0 && price > 500)
			{
				HashMap<String, String> params = new HashMap<String, String>();
				params.put("error", "Price outside limits");
				showFormError(getString(R.string.add_error_enterprice));
				return false;
			}
		}
		
		if (phoneText.getText().toString().trim().length() == 0)
		{
			HashMap<String, String> params = new HashMap<String, String>();
			params.put("error", "Missing phone no.");
			showFormError(getString(R.string.add_error_enterphone));
			return false;
		}
		
		return true;
	}
	
	private void showFormError(String error)
	{
		Toast.makeText(this, error, Toast.LENGTH_LONG).show();
	}
	
	private void showPreview()
	{
		if (validateForm())
		{
			Double price;
			
			if (priceText.getText().toString().trim().length() == 0)
			{
				price = null;
			}
			else
			{
				price = Double.parseDouble(priceText.getText().toString().trim());
			}
			
			// Create a new ride object
			final Ride ride = new Ride(0,							// Ride ID
								 RideType.SHARE,					// Ride type
								 fromCity,							// From
								 toCity,							// To
								 dateTime,				// Date and time
								 ((PeopleSpinnerObject)peopleSpinner.getSelectedItem()).getNumber(),		// Number of people
								 price,																		// Ride price
								 null,																		// Ride author string
								 StringUtil.numberOnly(phoneText.getText().toString(), false),				// Phone number
								 commentText.getText().toString().trim(),									// Ride comment
								 true,																		// isAuthor flag
								 insuranceCheck.isChecked(),												// isInsured flag
								 false);																	// isFull flag
			
			storePhoneNo();
			OnClickListener sendListener = new OnClickListener()
			{
				public void onClick(View v)
				{
					postRide(ride);
				}
			};
			
			// Hide the "ride full" checkbox
			View separator = findViewById(R.id.ride_full_separator);
			final CheckBox fullBox = (CheckBox) findViewById(R.id.ride_full);
			separator.setVisibility(View.GONE);
			fullBox.setVisibility(View.GONE);
			
			RideInfoUtil util = new RideInfoUtil(this, getString(R.string.add_send), sendListener);
			util.showRide(ride, false);
			
			stateManager.showView(Views.PREVIEW);
		}
	}
	
	private void storePhoneNo() {
		SharedPreferences preferences = this.getSharedPreferences(PREF_PHONE_NO, 0);
		SharedPreferences.Editor editor = preferences.edit();
		editor.remove(PREF_PHONE_NO).putString(PREF_PHONE_NO, phoneText.getText().toString());
		editor.commit();
	}
	
	private void postRide(Ride ride)
	{
		stateManager.showView(Views.LOADING);
		
		final SendRideTask task = new SendRideTask(ride);
		
		Handler sendHandler = new Handler()
		{

			@Override
			public void handleMessage(Message msg)
			{
				super.handleMessage(msg);
				
				HashMap<String, String> params = new HashMap<String, String>();
				switch(msg.what)
				{
					case SendRideTask.AUTHENTICATION_ERROR:
						
						params.put("error", "Authentication error");
						authenticationStatusReceived(AuthenticationStatus.NOT_AUTHENTICATED);
						break;
					case SendRideTask.SERVER_ERROR:
						params.put("error", "Server error");
						Toast.makeText(AddRideActivity.this, R.string.server_error, Toast.LENGTH_SHORT).show();
						stateManager.showView(Views.FORM);
						break;
					case SendRideTask.SEND_ERROR:
						params.put("error", "Send error");
						Toast.makeText(AddRideActivity.this, task.getErrorMessage(), Toast.LENGTH_SHORT).show();
						stateManager.showView(Views.FORM);
						break;
					case SendRideTask.SEND_SUCCESS:
						Intent rideInfo = new Intent(AddRideActivity.this, RideInfoActivity.class);
						rideInfo.putExtra(RideInfoActivity.RIDE_ID, task.getRideId());
						startActivity(rideInfo);
						finish();
						break;
				}
			}
		};
		
		stateManager.showView(Views.LOADING);
		task.startTask(sendHandler);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if (item.getItemId() == android.R.id.home)
		{
			Intent intent = new Intent(this, MyRidesActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			return true;
		}
		
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onBackPressed()
	{
		// State manager will correctly switch views or return false if the activity must finish
		if (!stateManager.handleBackKey())
			finish();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		
		if (fromCity != null)
		{
			outState.putString("fromCity", fromCity.getDisplayName());
			outState.putString("fromCityCountry", fromCity.getCountryCode());
		}
		
		if (toCity != null)
		{
			outState.putString("toCity", toCity.getDisplayName());
			outState.putString("toCityCountry", toCity.getCountryCode());
		}
		
		outState.putInt("numPpl", ((PeopleSpinnerObject)peopleSpinner.getSelectedItem()).getNumber());
		outState.putLong("date", dateTime.getTimeInMillis());
	}
	
	public class DatePickerFragment extends DialogFragment implements OnDateSetListener
	{

		@Override
		public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) 
		{
			dateTime.set(Calendar.YEAR, year);
			dateTime.set(Calendar.MONTH, monthOfYear);
			dateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);
			updateDateTime();
		}

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) 
		{
			DatePickerDialog dialog = new DatePickerDialog(AddRideActivity.this, this, 2010, 1, 1);
			dialog.updateDate(dateTime.get(Calendar.YEAR), dateTime.get(Calendar.MONTH), dateTime.get(Calendar.DAY_OF_MONTH));
			return dialog;
		}	
	}
	
	public class TimePickerFragment extends DialogFragment implements OnTimeSetListener
	{
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) 
		{
			TimePickerDialog dialog = new TimePickerDialog(AddRideActivity.this, this, 0, 0, true);
			dialog.updateTime(dateTime.get(Calendar.HOUR_OF_DAY), dateTime.get(Calendar.MINUTE));
			return dialog; 
		}

		@Override
		public void onTimeSet(TimePicker view, int hourOfDay, int minute) 
		{
			dateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
			dateTime.set(Calendar.MINUTE, minute);
			updateDateTime();
		}	
	}
}
