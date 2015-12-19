package org.prevoz.android.push;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.prevoz.android.MainActivity;
import org.prevoz.android.PrevozApplication;
import org.prevoz.android.R;
import org.prevoz.android.model.City;
import org.prevoz.android.model.PrevozDatabase;
import org.prevoz.android.util.LocaleUtil;
import org.threeten.bp.LocalDate;
import org.threeten.bp.format.DateTimeFormatter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.StringTokenizer;

import javax.inject.Inject;

public class PushReceiver extends BroadcastReceiver
{
    @Inject
    protected PrevozDatabase database;

    @Override
    public void onReceive(Context context, Intent intent)
    {
        Log.i(this.toString(), "GCM Message received.");
        final Bundle extras = intent.getExtras();
        if (extras == null) return;

        if (database == null) {
            ((PrevozApplication)context.getApplicationContext()).component().inject(this);
        }

        final int[] rideIds = parseRideIds(extras.getString("rides"));
        if (rideIds.length == 0) return;

        if (!(extras.containsKey("fromcity") && extras.containsKey("tocity") && extras.containsKey("from_country") && extras.containsKey("to_country")))
            return;

        final City from = new City(extras.getString("fromcity"), extras.getString("from_country"));
        final City to = new City(extras.getString("tocity"), extras.getString("to_country"));

        // Create notification message:
        android.app.NotificationManager notifyManager = (android.app.NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        String title = context.getString(R.string.notify_statusbar) + " " + from.getLocalizedName(database) + " - " + to.getLocalizedName(database);

        // Prepare search results launch intent
        LocalDate when = LocalDate.parse(extras.getString("date"), DateTimeFormatter.ISO_LOCAL_DATE);

        Intent notificationIntent = new Intent(context, MainActivity.class);
        notificationIntent.putExtra("from", from);
        notificationIntent.putExtra("to", to);
        notificationIntent.putExtra("when", when.atStartOfDay(LocaleUtil.getLocalTimezone()).toInstant());
        notificationIntent.putExtra("highlights", rideIds);

        PendingIntent pIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.icon_ab)
                .setContentTitle(title)
                .setContentText(rideIds.length + " " + LocaleUtil.getStringNumberForm(context.getResources(), R.array.ride_forms, rideIds.length) + " v " + LocaleUtil.getNotificationDayName(context.getResources(), when).toLowerCase())
                .setContentIntent(pIntent)
                .setCategory(NotificationCompat.CATEGORY_SOCIAL)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(false)
                .getNotification();

        notification.number = rideIds.length;
        notifyManager.notify(1, notification);
    }

    public static int[] parseRideIds(String rideIds)
    {
        if (rideIds == null) return new int[0];
        ArrayList<Integer> ids = new ArrayList<>();
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
