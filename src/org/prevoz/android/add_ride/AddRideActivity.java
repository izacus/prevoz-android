package org.prevoz.android.add_ride;

import java.util.Calendar;

import org.prevoz.android.CitySelectorActivity;
import org.prevoz.android.R;
import org.prevoz.android.auth.AuthenticationManager;
import org.prevoz.android.auth.AuthenticationStatus;
import org.prevoz.android.util.StringUtil;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;
import android.widget.ViewFlipper;

public class AddRideActivity extends FragmentActivity 
{
	private static final int FROM_CITY_REQUEST = 1;
	private static final int TO_CITY_REQUEST = 2;
	
	private ViewFlipper addFlipper;
	// Form buttons
	private Button fromButton;
	private Button toButton;
	private Button dateButton;
	private Button timeButton;
	
	
	// Field data
	private String fromCity = null;
	private String toCity = null;
	
	private Calendar dateTime;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.add_ride_activity);
		
		// Initialize values
		dateTime = Calendar.getInstance();
		
		prepareFormFields();
		
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
		addFlipper = (ViewFlipper) findViewById(R.id.add_flipper);
		addFlipper.setDisplayedChild(0);
		
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
							addFlipper.setDisplayedChild(1);
						}
					}
				};
				
				AuthenticationManager.getInstance().requestLogin(this, loginHandler);
				break;
			
			case AUTHENTICATED:
				addFlipper.setDisplayedChild(1);
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
}
