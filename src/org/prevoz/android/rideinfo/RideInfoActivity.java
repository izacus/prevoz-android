package org.prevoz.android.rideinfo;

import java.text.SimpleDateFormat;

import org.prevoz.android.Globals;
import org.prevoz.android.R;
import org.prevoz.android.util.HTTPHelper;
import org.prevoz.android.util.LocaleUtil;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

public class RideInfoActivity extends Activity
{
	// UI fields
	private ViewFlipper rideFlipper;
	
	private TextView fromText;
	private TextView toText;
	private TextView timeText;
	private TextView dayText;
	private TextView dateText;
	private TextView priceText;
	private TextView pplText;
	private TextView pplTagText;
	private TextView driverText;
	private TextView contactText;
	private TextView commentText;
	
	private Button callButton;
	private Button smsButton;
	private Button delButton;
	
	public static final String RIDE_ID = RideInfoActivity.class.toString() + ".ride_id";
	
	private int rideID;
	private Ride ride = null;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ride_info_activity);
		prepareUIElements();
		
		rideFlipper.setInAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));
		rideFlipper.setOutAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_out));
		
		// Get ride ID
		if (getIntent().getExtras() != null)
		{
			rideID = getIntent().getExtras().getInt(RIDE_ID);
			Log.d(this.toString(), "Requesting ride info for id " + rideID);
		}

		if (savedInstanceState != null
				&& savedInstanceState.containsKey("suspended")
				&& savedInstanceState.getBoolean("suspended"))
		{
			ride = new Ride(savedInstanceState);
			rideID = ride.getId();
			showRide(ride);
		}
		else
		{
			loadRideData();
		}
	}
	
	private void prepareUIElements()
	{
		// UI fields
		rideFlipper = (ViewFlipper)findViewById(R.id.rideinfo_flipper);
		
		fromText = (TextView) findViewById(R.id.rideinfo_from);
		toText = (TextView) findViewById(R.id.rideinfo_to);
		timeText = (TextView) findViewById(R.id.rideinfo_time);
		dayText = (TextView) findViewById(R.id.rideinfo_day);
		dateText = (TextView) findViewById(R.id.rideinfo_date);
		priceText = (TextView) findViewById(R.id.rideinfo_price);
		pplText = (TextView) findViewById(R.id.rideinfo_people);
		pplTagText = (TextView) findViewById(R.id.rideinfo_peopletag);
		driverText = (TextView) findViewById(R.id.rideinfo_author);
		contactText = (TextView) findViewById(R.id.rideinfo_phone);
		commentText = (TextView) findViewById(R.id.rideinfo_comment);
		
		callButton = (Button)findViewById(R.id.rideinfo_call);
		smsButton = (Button)findViewById(R.id.rideinfo_sms);
		delButton = (Button) findViewById(R.id.rideinfo_delsend);
	}

	@Override
	/**
	 * Called when activity is about to be killed
	 */
	protected void onSaveInstanceState(Bundle outState)
	{
		if (ride != null)
		{
			outState.putBoolean("suspended", true);
			ride.storeToBundle(outState);
		}

		super.onSaveInstanceState(outState);
	}

	private void loadRideData()
	{
		rideFlipper.setDisplayedChild(0);
		
		HTTPHelper.updateSessionCookies(this);
		final LoadInfoTask loadInfo = new LoadInfoTask();

		Handler handler = new Handler()
		{

			@Override
			public void handleMessage(Message msg)
			{
				switch (msg.what)
				{
					case Globals.REQUEST_SUCCESS:
						showRide(loadInfo.getResult());
						rideFlipper.showNext();
						break;
	
					// TODO: error handling
					case Globals.REQUEST_ERROR_SERVER:
						Toast.makeText(RideInfoActivity.this, R.string.server_error, Toast.LENGTH_LONG).show();
						break;
	
					case Globals.REQUEST_ERROR_NETWORK:
						Toast.makeText(RideInfoActivity.this, R.string.network_error, Toast.LENGTH_LONG).show();
						break;
				}
			}
		};

		loadInfo.loadInfo(rideID, handler);
	}

	private void showRide(Ride ride)
	{
		this.ride = ride;

		Resources res = getResources();
		
		// From and to
		fromText.setText(ride.getFrom());
		toText.setText(ride.getTo());

		// Time and date
		SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm");		
		timeText.setText(timeFormatter.format(ride.getTime()));
		dayText.setText(LocaleUtil.getDayName(res, ride.getTime()) + ",");
		dateText.setText(LocaleUtil.getFormattedDate(res, ride.getTime()));

		// Price and number of people
		if (ride.getPrice() != null)
		{
			priceText.setText(String.format("%1.1f €", ride.getPrice()));
		}
		else
		{
			priceText.setText("?");
		}

		
		pplText.setText(String.valueOf(ride.getPeople()));
		pplTagText.setText(LocaleUtil.getStringNumberForm(res,
				R.array.people_tags, ride.getPeople()));

		// Driver and contact		
		if (ride.getAuthor() == null || ride.getAuthor().trim().length() == 0)
		{
			driverText.setVisibility(View.GONE);
		}
		else
		{
			driverText.setVisibility(View.VISIBLE);
			driverText.setText(ride.getAuthor());
		}

		// Contact info
		contactText.setText(ride.getContact());

		// Comment
		commentText.setText(ride.getComment());

		// Setup button callbacks
		callButton.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				callAuthor();
			}
		});

		smsButton.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				sendSMS();
			}
		});
	}

	/**
	 * Opens SMS messaging application with authors phone number entered
	 */
	private void sendSMS()
	{
		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("sms:" + ride.getContact()));
		intent.putExtra("address", ride.getContact());
		intent.setType("vnd.android-dir/mms-sms");
		this.startActivity(intent);
	}

	/**
	 * Opens the dialer with authors phone number entered
	 */
	private void callAuthor()
	{
		Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + ride.getContact()));
		this.startActivity(intent);
	}
	
	
}
