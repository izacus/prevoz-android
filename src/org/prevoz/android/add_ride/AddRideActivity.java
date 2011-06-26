package org.prevoz.android.add_ride;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.prevoz.android.CitySelectorActivity;
import org.prevoz.android.R;
import org.prevoz.android.RideType;
import org.prevoz.android.add_ride.AddStateManager.Views;
import org.prevoz.android.auth.AuthenticationManager;
import org.prevoz.android.auth.AuthenticationStatus;
import org.prevoz.android.rideinfo.Ride;
import org.prevoz.android.rideinfo.RideInfoUtil;
import org.prevoz.android.util.LocaleUtil;
import org.prevoz.android.util.StringUtil;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ViewFlipper;

public class AddRideActivity extends FragmentActivity implements OnTimeSetListener, OnDateSetListener
{
	private static final int FROM_CITY_REQUEST = 1;
	private static final int TO_CITY_REQUEST = 2;
	
	private static final int DIALOG_DATE = 1;
	private static final int DIALOG_TIME = 2;
	
	private final SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm");
	
	private AddStateManager stateManager;
	
	// Form buttons
	private Button fromButton;
	private Button toButton;
	private Button dateButton;
	private Button timeButton;
	private Button nextButton;
	private Spinner peopleSpinner;
	// Form fields
	private EditText priceText;
	private EditText phoneText;
	private EditText commentText;
	
	// Field data
	private String fromCity = null;
	private String toCity = null;
	
	private Calendar dateTime;
	
	// Dialogs
	private DatePickerDialog datePickerDialog;
	private TimePickerDialog timePickerDialog;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.add_ride_activity);
		
		// Initialize values
		dateTime = Calendar.getInstance();		
		
		// Round minutes to the nearest hour
		dateTime.set(Calendar.MINUTE, 0);
		dateTime.set(Calendar.SECOND, 0);
		dateTime.roll(Calendar.HOUR, 1);
		
		prepareFormFields();
		updateDateTime();
		
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
	
	private void prepareFormFields()
	{
		// Prepare UI injection
		ViewFlipper addFlipper = (ViewFlipper) findViewById(R.id.add_flipper);
		stateManager = new AddStateManager(addFlipper);
		stateManager.showView(Views.LOADING);
		
		// From city selector
		fromButton = (Button) findViewById(R.id.from_button);
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
		toButton = (Button) findViewById(R.id.to_button);
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
		dateButton = (Button) findViewById(R.id.date_button);
		dateButton.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				showDialog(DIALOG_DATE);
			}
		});
		
		timeButton = (Button) findViewById(R.id.time_button);
		timeButton.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				showDialog(DIALOG_TIME);
			}
		});
		
		// Initialize dialogs for selection
		timePickerDialog = new TimePickerDialog(this, this, 0, 0, true);
		datePickerDialog = new DatePickerDialog(this, this, 2010, 1, 1);
		
		// Prepare number of people spinner
		peopleSpinner = (Spinner) findViewById(R.id.add_ppl);
		PeopleSpinnerObject[] peopleSpinnerObject = new PeopleSpinnerObject[6];
		for (int i = 0; i < 6; i++)
			peopleSpinnerObject[i] = new PeopleSpinnerObject(this, i + 1);
		
		ArrayAdapter<PeopleSpinnerObject> peopleAdapter = new ArrayAdapter<PeopleSpinnerObject>(this, android.R.layout.simple_spinner_item, peopleSpinnerObject);
		peopleSpinner.setAdapter(peopleAdapter);
		peopleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		
		// Edit text fields
		priceText = (EditText) findViewById(R.id.add_price);
		phoneText = (EditText) findViewById(R.id.add_phone);
		commentText = (EditText) findViewById(R.id.add_comment);
		
		// Next button
		nextButton = (Button)findViewById(R.id.add_button);
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
		Log.i(this.toString(), "Received authentication status: " + status);
		
		
		switch(status)
		{
			case UNKNOWN:
				Toast.makeText(this, R.string.network_error, Toast.LENGTH_LONG);
				break;
				
			case NOT_AUTHENTICATED:				
				Log.i(this.toString(), "Opening user login request...");
				
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
				fromCity = successful ? data.getStringExtra("city") : null;
				StringUtil.setLocationButtonText(fromButton, fromCity, getString(R.string.add_select_city));
				break;
				
			case TO_CITY_REQUEST:
				toCity = successful ? data.getStringExtra("city") : null;
				StringUtil.setLocationButtonText(toButton, toCity, getString(R.string.add_select_city));
				break;
		}
		
	}

	private boolean validateForm()
	{
		// Check from city
		if (fromCity == null)
		{
			showFormError(getString(R.string.add_error_enterfrom));
			return false;
		}
		
		if (toCity == null)
		{
			showFormError(getString(R.string.add_error_enterto));
			return false;
		}
		
		// Check date validity
		if (dateTime.before(Calendar.getInstance()))
		{
			showFormError(getString(R.string.add_error_timepast));
			return false;
		}
		
		// Check price
		double price;
		
		try
		{
			 price = Double.parseDouble(priceText.getText().toString());
		}
		catch (NumberFormatException e)
		{
			showFormError(getString(R.string.add_error_enterprice));
			return false;
		}
		
		if (price < 0.5 && price > 500)
		{
			// TODO: check limits
			showFormError(getString(R.string.add_error_enterprice));
			return false;
		}
		
		if (phoneText.getText().toString().trim().length() == 0)
		{
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
			// Create a new ride object
			final Ride ride = new Ride(0,							// Ride ID
								 RideType.SHARE,					// Ride type
								 fromCity,							// From
								 toCity,							// To
								 dateTime.getTime(),				// Date and time 
								 ((PeopleSpinnerObject)peopleSpinner.getSelectedItem()).getNumber(),		// Number of people
								 Double.parseDouble(priceText.getText().toString().trim()),					// Ride price
								 null,																		// Ride author string
								 StringUtil.numberOnly(phoneText.getText().toString(), false),				// Phone number
								 commentText.getText().toString().trim(),									// Ride comment
								 true);																		// isAuthor flag
			
			
			OnClickListener sendListener = new OnClickListener()
			{
				public void onClick(View v)
				{
					postRide(ride);
				}
			};
			
			RideInfoUtil util = new RideInfoUtil(this, getString(R.string.add_send), sendListener);
			util.showRide(ride, false);
			
			stateManager.showView(Views.PREVIEW);
		}
	}
	
	private void postRide(Ride ride)
	{
		
	}
	
	@Override
	protected Dialog onCreateDialog(int id)
	{
		switch(id)
		{
			case DIALOG_TIME:
				timePickerDialog.updateTime(dateTime.get(Calendar.HOUR_OF_DAY), dateTime.get(Calendar.MINUTE));
				return timePickerDialog;
			case DIALOG_DATE:
				datePickerDialog.updateDate(dateTime.get(Calendar.YEAR), dateTime.get(Calendar.MONTH), dateTime.get(Calendar.DAY_OF_MONTH));
				return datePickerDialog;
			default:
				return super.onCreateDialog(id);
		}
	}

	public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth)
	{
		dateTime.set(Calendar.YEAR, year);
		dateTime.set(Calendar.MONTH, monthOfYear);
		dateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);
		updateDateTime();
	}

	public void onTimeSet(TimePicker view, int hourOfDay, int minute)
	{
		dateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
		dateTime.set(Calendar.MINUTE, minute);
		updateDateTime();
	}

	@Override
	public void onBackPressed()
	{
		// State manager will correctly switch views or return false if the activity must finish
		if (!stateManager.handleBackKey())
			finish();
	}
}
