package org.prevoz.android;

import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

import org.prevoz.android.util.Database;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

public class GPSManager implements LocationListener
{
	public static final int GPS_PROVIDER_UNAVALABLE = 0;
	public static final int GPS_LOCATION_OK = 1;
	public static final int GPS_CANCELED = 2;

	private static final int NEW_LOCATION_FIX_MILLIS = 180000;
	private static final int LOCATION_LOCK_TIMEOUT_MILLIS = 30000;

	private Context context;
	private String currentCity;
	private Handler callback;
	private Timer timeoutTimer;
	private LocationManager locationManager;

	private class UpdateTimeout extends TimerTask
	{
		private GPSManager listener;

		public UpdateTimeout(GPSManager listener)
		{
			this.listener = listener;
		}

		@Override
		public void run()
		{
			// Location aquisition has failed, send error message
			locationManager.removeUpdates(listener);
			callback.sendEmptyMessage(GPS_PROVIDER_UNAVALABLE);
		}
	}

	/**
	 * Finds city name of current location
	 * 
	 * @return UTF-8 city name from database
	 */
	public void findCurrentCity(Context context, Handler callback)
	{
		this.callback = callback;
		this.context = context;

		// Get location services
		locationManager = (LocationManager) context
				.getSystemService(Context.LOCATION_SERVICE);

		boolean gpsOn = false;
		boolean networkOn = false;

		// Check available providers
		try
		{
			gpsOn = locationManager
					.isProviderEnabled(LocationManager.GPS_PROVIDER);
		}
		catch (Exception e)
		{
		}
		;

		try
		{
			networkOn = locationManager
					.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
		}
		catch (Exception e)
		{
		}
		;

		// Check if either network or GPS provider have new enough location data
		if (gpsOn)
		{
			Log.d(this.toString(), "Checking for cached GPS location.");

			if (checkCachedLocation(locationManager,
					LocationManager.GPS_PROVIDER))
			{
				callback.sendEmptyMessage(GPS_LOCATION_OK);
				return;
			}
		}

		if (networkOn)
		{
			Log.d(this.toString(), "Checking for cached network location.");
			if (checkCachedLocation(locationManager,
					LocationManager.NETWORK_PROVIDER))
			{
				callback.sendEmptyMessage(GPS_LOCATION_OK);
				return;
			}
		}

		// Both data is out of date, start attempting to get a fix
		if (!gpsOn && !networkOn)
		{
			callback.sendEmptyMessage(GPS_PROVIDER_UNAVALABLE);
			return;
		}

		if (gpsOn)
		{
			locationManager.requestLocationUpdates(
					LocationManager.GPS_PROVIDER, 0, 0, this);
		}

		if (networkOn)
		{
			locationManager.requestLocationUpdates(
					LocationManager.NETWORK_PROVIDER, 0, 0, this);
		}

		timeoutTimer = new Timer();
		timeoutTimer.schedule(new UpdateTimeout(this),
				LOCATION_LOCK_TIMEOUT_MILLIS);
	}

	private boolean checkCachedLocation(LocationManager manager, String provider)
	{
		long currentTime = Calendar.getInstance().getTimeInMillis();
		Location loc = manager.getLastKnownLocation(provider);

		if (loc == null)
			return false;

		if (currentTime - loc.getTime() < NEW_LOCATION_FIX_MILLIS)
		{
			Log.d(this.toString(), "Cached location time " + loc.getTime()
					+ " current " + currentTime);

			findCity(loc);
			return true;
		}

		return false;
	}

	private void findCity(Location location)
	{
		double latitude, longtitude;
		Log.i(this.toString(), "Location found!!" + location.getLatitude()
				+ ", " + location.getLongitude());
		latitude = location.getLatitude();
		longtitude = location.getLongitude();

		currentCity = Database.getClosestCity(context, latitude, longtitude);
		Log.i(this.toString(), "Closest city determined to be " + currentCity);
	}

	public void cancelSearch()
	{
		timeoutTimer.cancel();
		locationManager.removeUpdates(this);
		callback.sendEmptyMessage(GPS_CANCELED);
	}

	public String getCurrentCity()
	{
		return currentCity;
	}

	public void onLocationChanged(Location location)
	{
		Log.i(this.toString(), "Location changed: " + location.getLatitude()
				+ ", " + location.getLongitude());

		// Cancel timeouts
		timeoutTimer.cancel();
		locationManager.removeUpdates(this);

		findCity(location);

		callback.sendEmptyMessage(GPS_LOCATION_OK);
	}

	public void onProviderDisabled(String provider)
	{
		// Nothing TBD
	}

	public void onProviderEnabled(String provider)
	{
		// Nothing TBD
	}

	public void onStatusChanged(String provider, int status, Bundle extras)
	{
		Log.i(this.toString(), provider + " status " + status);
	}
}
