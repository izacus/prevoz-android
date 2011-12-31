package org.prevoz.android.c2dm;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.prevoz.android.R;
import org.prevoz.android.util.Database;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.google.android.c2dm.C2DMessaging;

public class NotificationManager
{
	public static final int REGISTRATION_SUCCESS = 0;
	public static final int REGISTRATION_FAILURE = 1;
	
	private static NotificationManager instance;
	private static Context applicationContext;
	
	public static synchronized NotificationManager getInstance(Context context)
	{
		applicationContext = context;
		return getInstance();
	}
	
	private static synchronized NotificationManager getInstance()
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
		
		return registrationId != null && registrationId.trim().length() > 0;
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

	public void disableNotification(final Context context, final String from, final String to, final Calendar when, final Handler callback) 
	{
		Log.d(this.toString(), "Disabling " + from + " - " + to  + ", " + when.toString());
		
		final NotifySubscription subscription = Database.getNotificationSubscription(context, from, to, when);
		
		if (subscription == null)
		{
			Log.e(this.toString(), "Requested subscription for disabling not found!");
			return;
		}
		
		NotificationRegistrationRequest request = new NotificationRegistrationRequest(this.registrationId,
																					  from,
																					  to,
																					  when,
																					  false);
		
		final NotificationRegistrationTask task = new NotificationRegistrationTask(request);
		Handler handler = new Handler() {

			@Override
			public void handleMessage(Message msg) 
			{
				try
				{
					if (!task.get())
					{
						Toast.makeText(context, context.getString(R.string.notify_dereg_fail), Toast.LENGTH_SHORT).show();
						
						if (callback != null)
							callback.sendEmptyMessage(REGISTRATION_SUCCESS);
						return;
					}
				}
				catch (ExecutionException e) { return; }
				catch (InterruptedException e) { return; }
				
				Toast.makeText(context, context.getString(R.string.notify_dereg_success), Toast.LENGTH_SHORT).show();
				Database.deleteNotificationSubscription(context, subscription.getId());
				if (callback != null)
					callback.sendEmptyMessage(REGISTRATION_FAILURE);
			}
			
		};
		task.setCallback(handler);
		task.execute((Void)null);
	}

	public void enableNotification(final Context context, final String from, final String to, final Calendar when, final Handler callback) 
	{
		Log.d(this.toString(), "Enabling " + from + " - " + to  + ", " + when.toString());
		
		if (Database.getNotificationSubscription(context, from, to, when) != null)
		{
			// TODO: notify
			return;
		}
		
		
		NotificationRegistrationRequest request = new NotificationRegistrationRequest(this.registrationId,
																					  from,
																					  to,
																					  when,
																					  true);
		
		final NotificationRegistrationTask task = new NotificationRegistrationTask(request);
		Handler handler = new Handler() {

			@Override
			public void handleMessage(Message msg) 
			{
				try
				{
					if (!task.get())
					{
						Toast.makeText(context, context.getString(R.string.notify_reg_fail), Toast.LENGTH_SHORT).show();
						
						if (callback != null)
							callback.sendEmptyMessage(REGISTRATION_SUCCESS);
						return;
					}
				}
				catch (ExecutionException e) { return; }
				catch (InterruptedException e) { return; }
				
				Toast.makeText(context, context.getString(R.string.notify_reg_success), Toast.LENGTH_SHORT).show();
				Database.addNotificationSubscription(context, from, to, when);
				if (callback != null)
					callback.sendEmptyMessage(REGISTRATION_FAILURE);
			}
			
		};
		task.setCallback(handler);
		task.execute((Void)null);
	}
}
