package org.prevoz.android.c2dm;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.prevoz.android.City;
import org.prevoz.android.R;
import org.prevoz.android.util.Database;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gcm.GCMRegistrar;

public class NotificationManager
{
	public static final String GCM_PROJECT_ID = "121500391433";
	
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
			try
			{
				GCMRegistrar.checkDevice(applicationContext);
				GCMRegistrar.checkManifest(applicationContext);
			}
			catch(Exception e)
			{
				this.registrationId = null;
				return;
			}
			
			registrationId = GCMRegistrar.getRegistrationId(applicationContext);
			Log.i(this.toString(), "Device C2DM registration string is " + registrationId);
			
			if (registrationId == null || registrationId.trim().length() == 0)
			{
				GCMRegistrar.register(applicationContext, GCM_PROJECT_ID);
			}
		}
	}

	public boolean notificationsAvailable()
	{
		// Attempt to get registration ID again
		if (registrationId == null || registrationId.trim().length() == 0)
		{
			if (applicationContext != null)
			{
				registrationId = GCMRegistrar.getRegistrationId(applicationContext);
			}
		}
		
		return registrationId != null && registrationId.trim().length() > 0;
	}
	
	public boolean isNotified(Context context, City from, City to, Calendar when)
	{
		return Database.getNotificationSubscription(context, from, to, when) != null;
	}
	
	public List<NotifySubscription> getNotificationSubscriptions(Context context)
	{
		List<NotifySubscription> subscriptons = Database.getNotificationSubscriptions(context);
		return subscriptons;
	}

	public void disableNotification(final Context context, final City from, final City to, final Calendar when, final Handler callback)
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
							callback.sendEmptyMessage(REGISTRATION_FAILURE);
						return;
					}
				}
				catch (ExecutionException e) { return; }
				catch (InterruptedException e) { return; }
				
				Toast.makeText(context, context.getString(R.string.notify_dereg_success), Toast.LENGTH_SHORT).show();
				Database.deleteNotificationSubscription(context, subscription.getId());
				if (callback != null)
					callback.sendEmptyMessage(REGISTRATION_SUCCESS);
			}
			
		};
		task.setCallback(handler);
		task.execute((Void)null);
	}

	public void enableNotification(final Context context, final City from, final City to, final Calendar when, final Handler callback)
	{
		Log.d(this.toString(), "Enabling " + from + " - " + to  + ", " + when.toString());
		
		if (Database.getNotificationSubscription(context, from, to, when) != null)
		{
			callback.sendEmptyMessage(REGISTRATION_FAILURE);
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
							callback.sendEmptyMessage(REGISTRATION_FAILURE);
						return;
					}
				}
				catch (ExecutionException e) { return; }
				catch (InterruptedException e) { return; }
				
				Toast.makeText(context, context.getString(R.string.notify_reg_success), Toast.LENGTH_SHORT).show();
				Database.addNotificationSubscription(context, from, to, when);
				if (callback != null)
					callback.sendEmptyMessage(REGISTRATION_SUCCESS);
			}
			
		};
		task.setCallback(handler);
		task.execute((Void)null);
	}
	
	public void setRegistrationId(String regId)
	{
		registrationId = regId;
	}
	
}
