package org.prevoz.android.rideinfo;

import java.text.SimpleDateFormat;

import org.prevoz.android.R;
import org.prevoz.android.util.LocaleUtil;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

public class RideInfoActivity extends FragmentActivity implements LoaderCallbacks<Ride>
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
		rideFlipper.setDisplayedChild(0);
		
		// Get ride ID
		if (getIntent().getExtras() != null)
		{
			rideID = getIntent().getExtras().getInt(RIDE_ID);
			Log.d(this.toString(), "Requesting ride info for id " + rideID);
		}
		
		getSupportLoaderManager().initLoader(rideID, null, this);
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

	private void showRide(Ride ride)
	{
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
		
		rideFlipper.showNext();
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

	public Loader<Ride> onCreateLoader(int loaderID, Bundle args) 
	{
		return new RideInfoLoader(this, rideID); 
	}

	public void onLoadFinished(Loader<Ride> loader, Ride result) 
	{
		if (result == null)
		{
			Toast.makeText(this, getString(R.string.network_error), Toast.LENGTH_LONG).show();
			return;
		}
		
		this.ride = result;
		showRide(ride);
	}

	public void onLoaderReset(Loader<Ride> loader) 
	{
		// Nothing TBD
		
	}
	
	
}
