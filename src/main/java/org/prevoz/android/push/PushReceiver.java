package org.prevoz.android.push;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.prevoz.android.MainActivity_;
import org.prevoz.android.R;
import org.prevoz.android.model.City;
import org.prevoz.android.util.LocaleUtil;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.StringTokenizer;

public class PushReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {
        Log.i(this.toString(), "GCM Message received.");

        Bundle extras = intent.getExtras();
        for (String key : extras.keySet())
        {
            Log.d(this.toString(), key + ":" + extras.get(key));
        }

        int[] rideIds = parseRideIds(intent.getExtras().getString("rides"));
        if (rideIds.length == 0)
            return;

        City from = new City(intent.getExtras().getString("fromcity"), intent.getExtras().getString("from_country"));
        City to = new City(intent.getExtras().getString("tocity"), intent.getExtras().getString("to_country"));

        // Create notification message:
        android.app.NotificationManager notifyManager = (android.app.NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        String title = context.getString(R.string.notify_statusbar) + " " + from.getLocalizedName(context) + " - " + to.getLocalizedName(context);

        // Prepare search results launch intent
        Calendar when = Calendar.getInstance();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        try
        {
            when.setTime(format.parse(intent.getExtras().getString("date")));
        }
        catch (ParseException e)
        {
            Log.e(this.toString(), "Failed to parse passed date.", e);
            return;
        }

        Intent notificationIntent = new Intent(context, MainActivity_.class);
        notificationIntent.putExtra("from", from);
        notificationIntent.putExtra("to", to);
        notificationIntent.putExtra("when", when.getTimeInMillis());
        notificationIntent.putExtra("highlights", rideIds);

        PendingIntent pIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.icon_ab)
                .setContentTitle(title)
                .setContentText(rideIds.length + " " + LocaleUtil.getStringNumberForm(context.getResources(), R.array.ride_forms, rideIds.length) + " v " + LocaleUtil.getNotificationDayName(context.getResources(), when).toLowerCase())
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
