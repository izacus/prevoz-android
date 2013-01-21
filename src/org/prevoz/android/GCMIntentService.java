package org.prevoz.android;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.StringTokenizer;

import org.prevoz.android.search.SearchResultsActivity;
import org.prevoz.android.util.LocaleUtil;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gcm.GCMBaseIntentService;

public class GCMIntentService extends GCMBaseIntentService 
{
	public GCMIntentService()
	{
		super(org.prevoz.android.c2dm.NotificationManager.GCM_PROJECT_ID);
	}

	@Override
	protected void onError(Context context, String regId) 
	{
		Log.e("GCMIntentService", "Error while registering with GCM!");
	}

	@Override
	protected void onRegistered(Context context, String regId) 
	{
		org.prevoz.android.c2dm.NotificationManager nm = org.prevoz.android.c2dm.NotificationManager.getInstance(context);
		nm.setRegistrationId(regId);
	}

	@Override
	protected void onUnregistered(Context context, String regId) 
	{
		org.prevoz.android.c2dm.NotificationManager nm = org.prevoz.android.c2dm.NotificationManager.getInstance(context);
		nm.setRegistrationId(null);
	}

	@Override
	protected void onMessage(Context context, Intent intent) 
	{
		Log.i(this.toString(), "GCM Message received.");
		
		Bundle extras = intent.getExtras();
		for (String key : extras.keySet())
		{
			Log.d(this.toString(), key + ":" + extras.get(key));
		}

        City from = new City(intent.getExtras().getString("fromcity"), intent.getExtras().getString("fromcountry"));
        City to = new City(intent.getExtras().getString("tocity"), intent.getExtras().getString("tocountry"));
		
		// Create notification message:
		NotificationManager notifyManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		String title = getString(R.string.notify_statusbar) + " " + from.getLocalizedName(this) + " - " + to.getLocalizedName(this);
		
		// Prepare search results launch intent
		Calendar when = Calendar.getInstance(LocaleUtil.getLocalTimezone());
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		format.setTimeZone(LocaleUtil.getLocalTimezone());
		
		try 
		{
			when.setTime(format.parse(intent.getExtras().getString("date")));
		} 
		catch (ParseException e) 
		{
			Log.e(this.toString(), "Failed to parse passed date.", e);
		}
		
		int[] rideIds = parseRideIds(intent.getExtras().getString("rides"));
		Intent notificationIntent = new Intent(context, SearchResultsActivity.class);
		notificationIntent.putExtra("from", from.getDisplayName());
        notificationIntent.putExtra("from_country", from.getCountryCode());
		notificationIntent.putExtra("to", to.getDisplayName());
        notificationIntent.putExtra("to_country", to.getCountryCode());
		notificationIntent.putExtra("when", when.getTimeInMillis());
		notificationIntent.putExtra("highlights", rideIds);
		
		PendingIntent pIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		
		Notification notification = new NotificationCompat.Builder(context)
										 .setSmallIcon(R.drawable.notify_icon)
										 .setContentTitle(title)
										 .setContentText(rideIds.length + " " + LocaleUtil.getStringNumberForm(getResources(), R.array.ride_forms, rideIds.length) + " v " + LocaleUtil.getNotificationDayName(getResources(), when).toLowerCase())
										 .setContentIntent(pIntent)
										 .getNotification();
		
		notification.flags |= (Notification.DEFAULT_LIGHTS | Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE);
		notification.flags &= ~Notification.FLAG_ONGOING_EVENT;	// Clear ongoing flag
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		notification.number = rideIds.length;
		notifyManager.notify(1, notification);
	}
	
	private static int[] parseRideIds(String rideIds)
	{
		ArrayList<Integer> ids = new ArrayList<Integer>();
		String stripped = rideIds.replaceAll("[^0-9,]", "");
		
		StringTokenizer tokenizer = new StringTokenizer(stripped.trim(), ",");
		while (tokenizer.hasMoreTokens())
		{
			String token = tokenizer.nextToken();
			Integer id = Integer.valueOf(token);
			ids.add(id);
		}
		
		int[] iIds = new int[ids.size()];
		for (int i = 0; i < ids.size(); i++)
			iIds[i] = ids.get(i);
		
		return iIds;
	}
}
