package org.prevoz.android.add_ride;

import org.prevoz.android.R;
import org.prevoz.android.auth.AuthenticationManager;
import org.prevoz.android.auth.AuthenticationStatus;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;
import android.widget.ViewFlipper;

public class AddRideActivity extends FragmentActivity 
{
	private ViewFlipper addFlipper;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.add_ride_activity);
		
		// Prepare UI injection
		addFlipper = (ViewFlipper) findViewById(R.id.add_flipper);
		addFlipper.setDisplayedChild(0);
		
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
					}
				};
				
				AuthenticationManager.getInstance().requestLogin(this, loginHandler);
				break;
			
			case AUTHENTICATED:
				addFlipper.setDisplayedChild(1);
				break;
		}
	}
}
