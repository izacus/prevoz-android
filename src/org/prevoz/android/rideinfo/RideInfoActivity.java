package org.prevoz.android.rideinfo;

import org.prevoz.android.R;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AnimationUtils;
import android.widget.Toast;
import android.widget.ViewFlipper;

public class RideInfoActivity extends FragmentActivity implements LoaderCallbacks<Ride>
{
	public static final String RIDE_ID = RideInfoActivity.class.toString() + ".ride_id";
	
	private Ride ride;
	private RideInfoUtil rideInfoUtil;
	
	public ViewFlipper rideFlipper;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ride_info_activity);
		prepareUIElements();
		
		// Get ride ID
		if (getIntent().getExtras() != null)
		{
			int rideID = getIntent().getExtras().getInt(RIDE_ID);
			Log.d(this.toString(), "Requesting ride info for id " + rideID);
			getSupportLoaderManager().initLoader(rideID, null, this);
		}
	}
	
	private void prepareUIElements()
	{
		rideFlipper = (ViewFlipper) findViewById(R.id.rideinfo_flipper);
		rideFlipper.setInAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));
		rideFlipper.setOutAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_out));
		rideFlipper.setDisplayedChild(0);
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
		return new RideInfoLoader(this, loaderID); 
	}

	public void onLoadFinished(Loader<Ride> loader, Ride result) 
	{
		if (result == null)
		{
			Toast.makeText(this, getString(R.string.network_error), Toast.LENGTH_LONG).show();
			return;
		}
		
		this.ride = result;
		
		OnClickListener callAuthor = new OnClickListener()
		{
			
			public void onClick(View v)
			{
				callAuthor();
			}
		};
		
		OnClickListener sendSMS = new OnClickListener()
		{
			public void onClick(View v)
			{
				sendSMS();
			}
		};
		
		rideInfoUtil = new RideInfoUtil(this, callAuthor, sendSMS);
		rideInfoUtil.showRide(ride, true);
		rideFlipper.showNext();
	}

	public void onLoaderReset(Loader<Ride> loader) 
	{
		// Nothing TBD
		
	}
	
	
}
