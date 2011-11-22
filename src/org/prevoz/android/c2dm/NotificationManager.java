package org.prevoz.android.c2dm;

import java.util.Calendar;
import java.util.List;

import org.prevoz.android.util.Database;

import android.content.Context;
import android.util.Log;


public class NotificationManager 
{
	private static NotificationManager instance;
	
	public static synchronized NotificationManager getInstance()
	{
		if (instance == null)
		{
			instance = new NotificationManager();
		}
		
		return instance;
	}
	
	private NotificationManager()
	{
	}

	public boolean notificationsAvailable()
	{
		// TODO implement
		return true;
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
