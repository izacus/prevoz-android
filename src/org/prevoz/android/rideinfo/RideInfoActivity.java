package org.prevoz.android.rideinfo;

import org.prevoz.android.Globals;
import org.prevoz.android.R;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AnimationUtils;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

public class RideInfoActivity extends FragmentActivity implements LoaderCallbacks<Ride>
{
	public static final String RIDE_ID = RideInfoActivity.class.toString() + ".ride_id";
	
	private Ride ride;
	private RideInfoUtil rideInfoUtil;
	
	public ViewFlipper rideFlipper;
	private GoogleAnalyticsTracker tracker;
	
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
		
		tracker = GoogleAnalyticsTracker.getInstance();
		tracker.trackPageView("/RideInfo/");
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
		tracker.trackEvent("RideInfo", "SMSSend", "", 0);
		
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
		tracker.trackEvent("RideInfo", "CallDriver", "", 0);
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
		
		View separator = findViewById(R.id.ride_full_separator);
		final CheckBox fullBox = (CheckBox) findViewById(R.id.ride_full);
		fullBox.setChecked(ride.isFull());
		
		// Setup callbacks for ride full checkbox
		fullBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			public void onCheckedChanged(final CompoundButton parent, final boolean checked) 
			{
				parent.setEnabled(false);
				final OnCheckedChangeListener _this = this;
				RideFullTask task = new RideFullTask(ride.getId(), new Handler() 
				{
					@Override
					public void handleMessage(Message msg) 
					{
						if (msg.what == Globals.REQUEST_SUCCESS)
						{
							int res = checked ? R.string.ride_set_full_success : R.string.ride_set_empty_success;
							Toast.makeText(RideInfoActivity.this, res, Toast.LENGTH_SHORT).show();
						}
						else
						{
							fullBox.setOnCheckedChangeListener(null);	// Ugly hack
							fullBox.setChecked(!checked);
							fullBox.setOnCheckedChangeListener(_this);
							Toast.makeText(RideInfoActivity.this, R.string.network_error, Toast.LENGTH_SHORT).show();
						}
						
						ride.setFull(parent.isChecked());
						parent.setEnabled(true);
					}
				});
				
				task.doInBackground(checked);
			}
		});
		
		if (result.isAuthor())
		{
			separator.setVisibility(View.VISIBLE);
			fullBox.setVisibility(View.VISIBLE);
			
			OnClickListener deleteListener = new OnClickListener()
			{
				public void onClick(View v)
				{
					deleteRide(ride);
				}
			};
			
			rideInfoUtil = new RideInfoUtil(this, callAuthor, sendSMS, getString(R.string.delete), deleteListener);
		}
		else
		{
			separator.setVisibility(View.GONE);
			fullBox.setVisibility(View.GONE);
			rideInfoUtil = new RideInfoUtil(this, callAuthor, sendSMS, null, null);
		}
		
		rideInfoUtil.showRide(ride, true);
		rideFlipper.showNext();
	}

	public void onLoaderReset(Loader<Ride> loader) 
	{
		// Nothing TBD
		
	}
	
	private void deleteRide(Ride ride)
	{
		// Show loading view
		rideFlipper.setDisplayedChild(0);
		
		Handler callback = new Handler()
		{
			@Override
			public void handleMessage(Message msg)
			{
				rideDeleted(msg.what);
			}
		};
		
		DeleteRideTask task = new DeleteRideTask();
		task.startTask(ride.getId(), callback);
	}
	
	
	private void rideDeleted(int code)
	{
		if (code == Globals.REQUEST_SUCCESS)
		{
			tracker.trackEvent("RideInfo", "DeleteRide", "OK", 0);
			Toast.makeText(this, R.string.ride_deleted, Toast.LENGTH_SHORT).show();
			finish();
		}
		else if (code == Globals.REQUEST_ERROR_NETWORK)
		{
			tracker.trackEvent("RideInfo", "DeleteRide", "Fail", 0);
			Toast.makeText(this, R.string.network_error, Toast.LENGTH_SHORT).show();
			rideFlipper.showNext();
		}
	}
}
