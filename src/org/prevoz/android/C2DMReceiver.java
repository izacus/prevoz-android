package org.prevoz.android;

import java.io.IOException;
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
import android.util.Log;

import com.google.android.c2dm.C2DMBaseReceiver;

public class C2DMReceiver extends C2DMBaseReceiver  
{
	public C2DMReceiver()
	{
		super("gandalfar@gmail.com");
	}

	
	
	@Override
	public void onRegistered(Context context, String registrationId)
			throws IOException {
		super.onRegistered(context, registrationId);
	}



	@Override
	public void onUnregistered(Context context) 
	{
		super.onUnregistered(context);
	}



	@Override
	public void onError(Context context, String error) 
	{
		Log.e(this.toString(), "C2DM error: " + error);
	}

	@Override
	protected void onMessage(Context context, Intent intent) 
	{
		Log.i(this.toString(), "C2DM Message received.");
		
		String from = intent.getExtras().getString("fromcity");
		String to = intent.getExtras().getString("tocity");
		
		// Create notification message:
		NotificationManager notifyManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		Notification notification = new Notification(R.drawable.status, getString(R.string.notify_statusbar) + " " + from + " - " + to, System.currentTimeMillis());
		
		// Prepare search results launch intent
		Calendar when = Calendar.getInstance();
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		try 
		{
			when.setTime(format.parse(intent.getExtras().getString("date")));
		} 
		catch (ParseException e) 
		{
			Log.e(this.toString(), "Failed to parse passed date.", e);
		}
		
		ArrayList<Integer> rideIds = parseRideIds(intent.getExtras().getString("rides"));
		Intent notificationIntent = new Intent(context, SearchResultsActivity.class);
		notificationIntent.putExtra("from", from);
		notificationIntent.putExtra("to", to);
		notificationIntent.putExtra("when", when.getTimeInMillis());
		notificationIntent.putExtra("highlights", rideIds.toArray());
		
		PendingIntent pIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
		notification.setLatestEventInfo(context, 
										rideIds.size() + " " + LocaleUtil.getStringNumberForm(getResources(), R.array.ride_forms, rideIds.size()) + " v " + LocaleUtil.getDayName(getResources(), when).toLowerCase(),
										from + " - " + to, 
										pIntent);
		
		notification.flags = (Notification.FLAG_AUTO_CANCEL | Notification.DEFAULT_ALL);
		notification.number = rideIds.size();
		notifyManager.notify(1, notification);
	}
	
	private static ArrayList<Integer> parseRideIds(String rideIds)
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
		
		return ids;
	}
}
