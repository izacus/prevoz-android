package org.prevoz.android.c2dm;

import java.util.Calendar;
import java.util.List;

import org.prevoz.android.R;
import org.prevoz.android.util.Database;

import com.google.android.c2dm.C2DMessaging;

import android.content.Context;
import android.util.Log;


public class NotificationManager 
{
	private static NotificationManager instance;
	private static Context applicationContext;
	
	public static synchronized NotificationManager getInstance(Context context)
	{
		applicationContext = context;
		return getInstance();
	}
	
	public static synchronized NotificationManager getInstance()
	{
		if (instance == null)
		{
			instance = new NotificationManager();
		}
		
		return instance;
	}
	
	private String registrationId = null;
	
	private NotificationManager()
	{
		if (applicationContext != null)
		{
			registrationId = C2DMessaging.getRegistrationId(applicationContext);
			Log.i(this.toString(), "Device C2DM registration string is " + registrationId);
			
			if (registrationId == null || registrationId.trim().length() == 0)
				C2DMessaging.register(applicationContext, applicationContext.getResources().getString(R.string.c2dm_sender));
		}
	}

	public boolean notificationsAvailable()
	{
		// Attempt to get registration ID again
		if (registrationId == null || registrationId.trim().length() == 0)
		{
			if (applicationContext != null)
			{
				registrationId = C2DMessaging.getRegistrationId(applicationContext);
			}
		}
		
		return registrationId != null && !registrationId.trim().isEmpty();
	}
	
	public boolean isNotified(Context context, String from, String to, Calendar when) 
	{
		return Database.getNotificationSubscription(context, from, to, when) != null;
	}
	
	public List<NotifySubscription> getNotificationSubscriptions(Context context)
	{
		List<NotifySubscription> subscriptons = Database.getNotificationSubscriptions(context);
		return subscriptons;
	}

	public void disableNotification(Context context, String from, String to, Calendar when) 
	{
		Log.d(this.toString(), "Disabling " + from + " - " + to  + ", " + when.toString());
		
		NotifySubscription subscription = Database.getNotificationSubscription(context, from, to, when);
		
		if (subscription == null)
		{
			Log.e(this.toString(), "Requested subscription for disabling not found!");
		}
		
		Database.deleteNotificationSubscription(context, subscription.getId());
		Log.d(this.toString(), "OK");
	}

	public void enableNotification(Context context, String from, String to, Calendar when) 
	{
		Log.d(this.toString(), "Enabling " + from + " - " + to  + ", " + when.toString());
		Database.addNotificationSubscription(context, from, to, when);
	}
}
