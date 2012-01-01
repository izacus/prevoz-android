package org.prevoz.android.util;
import org.prevoz.android.R;

import android.content.Context;
import android.util.Log;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

public class GAUtils {

	private static GAUtils utils;
	
	private static synchronized GoogleAnalyticsTracker getTracker(Context context)
	{
		if (utils == null)
		{
			utils = new GAUtils(context);
		}
		
		return utils.getTracker();
	}

	public static void trackPageView(Context context, String page)
	{
		try
		{
			getTracker(context).trackPageView(page);
		}
		catch (Exception e)
		{
			Log.e("GAUtils", "Tracker error: " + e.getMessage(), e);
		}
	}
	
	public static void trackEvent(Context context, String category, String action, String label, int value)
	{
		try
		{
			getTracker(context).trackEvent(category, action, label, value);
		}
		catch (Exception e)
		{
			Log.e("GAUtils", "Tracker error: " + e.getMessage(), e);
		}
	}

	public static void dispatch(Context applicationContext) 
	{
		try
		{
			getTracker(applicationContext).dispatch();
		}
		catch (Exception e)
		{
			Log.e("GAUtils", "Tracker error: " + e.getMessage(), e);
		}
	}
	
	private GoogleAnalyticsTracker tracker;
	
	public GAUtils(Context context) 
	{
		tracker = GoogleAnalyticsTracker.getInstance();
		tracker.startNewSession(context.getString(R.string.ga_identity), context);
	}

	private GoogleAnalyticsTracker getTracker() 
	{
		return tracker;
	}
	
	@Override
	protected void finalize() throws Throwable 
	{
		super.finalize();
		try
		{
			if (tracker != null)
			{
				tracker.stopSession();
				Log.d("GAUtils - finalize", "Stopping tracking.");
			}
		}
		catch (Exception e) {
			Log.e("GAUtils", "Failed to finalize!", e);
		};
	}
}
