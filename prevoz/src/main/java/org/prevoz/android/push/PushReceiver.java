package org.prevoz.android.push;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
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

import java.util.ArrayList;
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
        final LocalDate date = LocalDate.parse(extras.getString("date"), DateTimeFormatter.ISO_LOCAL_DATE);
        createNewNotification(context, database, from, to, date, rideIds);
    }

    public static void createNewNotification(@NonNull Context context,
                                             @NonNull PrevozDatabase database,
                                             @NonNull City from,
                                             @NonNull City to,
                                             @NonNull LocalDate date,
                                             @NonNull int[] rideIds) {
        // Create notification message:
        android.app.NotificationManager notifyManager = (android.app.NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        String title = context.getResources().getQuantityString(R.plurals.notify_statusbar, rideIds.length) + " " + from.getLocalizedName(database) + " - " + to.getLocalizedName(database);

        int id = from.hashCode() + to.hashCode();
        // Prepare search results launch intent
        Intent notificationIntent = new Intent(context, MainActivity.class);
        notificationIntent.putExtra("from", from);
        notificationIntent.putExtra("to", to);
        notificationIntent.putExtra("when", date.atStartOfDay(LocaleUtil.getLocalTimezone()).toInstant().toEpochMilli());
        notificationIntent.putExtra("highlights", rideIds);

        PendingIntent pIntent = PendingIntent.getActivity(context, id, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification publicNotification = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.icon_ab)
                .setContentTitle(context.getResources().getQuantityString(R.plurals.notify_statusbar_private, rideIds.length))
                .setContentIntent(pIntent)
                .setCategory(NotificationCompat.CATEGORY_SOCIAL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setNumber(rideIds.length)
                .setColor(context.getResources().getColor(R.color.prevoztheme_color))
                .setWhen(System.currentTimeMillis())
                .setShowWhen(true)
                .setGroup("RIDES")
                .setOngoing(false)
                .setAutoCancel(true)
                .build();

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.icon_ab)
                .setContentTitle(title)
                .setContentIntent(pIntent)
                .setCategory(NotificationCompat.CATEGORY_SOCIAL)
                .setAutoCancel(true)
                .setNumber(rideIds.length)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setPublicVersion(publicNotification)
                .setWhen(System.currentTimeMillis())
                .setShowWhen(true)
                .setGroup("RIDES")
                .setColor(context.getResources().getColor(R.color.prevoztheme_color))
                .setOngoing(false);

        if (rideIds.length > 1) {
            notificationBuilder.setContentText(rideIds.length + " " + LocaleUtil.getStringNumberForm(context.getResources(), R.array.ride_forms, rideIds.length) + " v " + LocaleUtil.getNotificationDayName(context.getResources(), date).toLowerCase());
        }

        Notification notification = notificationBuilder.build();
        notifyManager.notify(id, notification);
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
