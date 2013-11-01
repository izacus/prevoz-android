package org.prevoz.android.rideinfo;

import org.prevoz.android.Globals;
import org.prevoz.android.R;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
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

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;

public class RideInfoActivity extends SherlockFragmentActivity implements LoaderCallbacks<Ride>
{
	public static final String RIDE_ID = RideInfoActivity.class.toString() + ".ride_id";
    private static final String LOG_TAG = "Prevoz.RideInfo";

    private Ride ride;
	private RideInfoUtil rideInfoUtil;
	public ViewFlipper rideFlipper;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ride_info_activity);
		getSupportActionBar().setHomeButtonEnabled(true);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		prepareUIElements();
		
		// Get ride ID
		if (getIntent().getExtras() != null)
		{
			int rideID = getIntent().getExtras().getInt(RIDE_ID);
			Log.d(LOG_TAG, "Requesting ride info for id " + rideID);
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
		Intent intent = null;
		if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY))
		{
			intent = new Intent(Intent.ACTION_VIEW, Uri.parse("sms:" + ride.getContact()));
			intent.putExtra("address", ride.getContact());
			intent.setType("vnd.android-dir/mms-sms");
		}
		else
		{
			intent = new Intent(Intent.ACTION_INSERT);
			intent.setType(ContactsContract.Contacts.CONTENT_TYPE);
			intent.putExtra(ContactsContract.Intents.Insert.NAME, ride.getAuthor());
			intent.putExtra(ContactsContract.Intents.Insert.PHONE, ride.getContact());
		}
		
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
						fullBox.setOnCheckedChangeListener(null);	// Ugly hack
						if (msg.what == RideFullTask.REQUEST_RIDE_FULL)
						{
							int res = R.string.ride_set_full_success;
							Toast.makeText(RideInfoActivity.this, res, Toast.LENGTH_SHORT).show();
							fullBox.setChecked(true);
						}
						else if (msg.what == RideFullTask.REQUEST_RIDE_NOT_FULL)
						{
							int res = R.string.ride_set_empty_success;
							Toast.makeText(RideInfoActivity.this, res, Toast.LENGTH_SHORT).show();
							fullBox.setChecked(false);
						}
						else
						{
							fullBox.setChecked(!checked);
							Toast.makeText(RideInfoActivity.this, R.string.network_error, Toast.LENGTH_SHORT).show();
						}
						
						fullBox.setOnCheckedChangeListener(_this);
						ride.setFull(parent.isChecked());
						rideInfoUtil.showPeople(getResources(), ride);
						parent.setEnabled(true);
					}
				});
				
				task.execute(checked);
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
			Toast.makeText(this, R.string.ride_deleted, Toast.LENGTH_SHORT).show();
			finish();
		}
		else if (code == Globals.REQUEST_ERROR_NETWORK)
		{
			Toast.makeText(this, R.string.network_error, Toast.LENGTH_SHORT).show();
			rideFlipper.showNext();
		}
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
		if (item.getItemId() == android.R.id.home)
		{
			finish();
			return true;
		}
		
		return super.onOptionsItemSelected(item);
	}
}
