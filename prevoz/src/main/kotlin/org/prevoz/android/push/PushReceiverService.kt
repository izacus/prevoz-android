package org.prevoz.android.push

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import com.crashlytics.android.Crashlytics
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.json.JSONArray
import org.prevoz.android.MainActivity
import org.prevoz.android.PrevozApplication
import org.prevoz.android.R
import org.prevoz.android.model.City
import org.prevoz.android.util.LocaleUtil
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime
import org.threeten.bp.format.DateTimeFormatter
import javax.inject.Inject

class PushReceiverService : FirebaseMessagingService() {

    @Inject lateinit var localeUtil: LocaleUtil

    override fun onCreate() {
        super.onCreate()
        (application as PrevozApplication?)?.component()?.inject(this)
    }

    override fun onMessageReceived(msg: RemoteMessage?) {
        if (msg == null) return
        val data = msg.data

        Log.d("Prevoz", "Message: " + data.toString())

        if (!data.containsKey("fromcity") ||
            !data.containsKey("tocity") ||
            !data.containsKey("from_country") ||
            !data.containsKey("to_country")) {
            Crashlytics.log("Invalid GCM message received: " + data.toString())
        }

        val from = City(data["fromcity"]!!, data["from_country"]!!)
        val to = City(data["tocity"]!!, data["to_country"]!!)
        val date = LocalDate.parse(data["date"]!!, DateTimeFormatter.ISO_LOCAL_DATE)
        val rideIds = parseRideIds(data["rides"])
        if (rideIds.isEmpty()) return
        val rideTimes = parseRideTimes(data["times"])

        createNewNotification(from, to, date, rideIds, if (rideTimes.size == rideIds.size) rideTimes else null)
    }

    private fun createNewNotification(from: City,
                                      to: City,
                                      date: LocalDate,
                                      rideIds: IntArray,
                                      rideTimes: List<LocalTime>?) {
        // Create notification message:
        val notifyManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        var title = resources.getQuantityString(R.plurals.notify_statusbar, rideIds.size, rideIds.size)
        if (rideTimes != null && rideTimes.size == 1) {
            title = title + " ob " + rideTimes[0].format(DateTimeFormatter.ofPattern("HH:mm"))
        }

        val id = from.hashCode() + to.hashCode()
        // Prepare search results launch intent
        val notificationIntent = Intent(this, MainActivity::class.java)
        notificationIntent.putExtra("from", from)
        notificationIntent.putExtra("to", to)
        notificationIntent.putExtra("when", date.atStartOfDay(LocaleUtil.getLocalTimezone()).toInstant().toEpochMilli())
        notificationIntent.putExtra("highlights", rideIds)

        val pIntent = PendingIntent.getActivity(this, id, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val publicNotification = NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.icon_ab)
                .setContentTitle(resources.getQuantityString(R.plurals.notify_statusbar, rideIds.size))
                .setContentIntent(pIntent)
                .setCategory(NotificationCompat.CATEGORY_SOCIAL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setNumber(rideIds.size)
                .setColor(resources.getColor(R.color.prevoztheme_color))
                .setWhen(System.currentTimeMillis())
                .setShowWhen(true)
                .setGroup("RIDES")
                .setOngoing(false)
                .setAutoCancel(true)
                .build()

        val notificationBuilder = NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.icon_ab)
                .setContentTitle(title)
                .setSubText(from.getLocalizedName(localeUtil) + " - " + to.getLocalizedName(localeUtil) + " v " + LocaleUtil.getNotificationDayName(resources, date))
                .setContentIntent(pIntent)
                .setCategory(NotificationCompat.CATEGORY_SOCIAL)
                .setAutoCancel(true)
                .setNumber(rideIds.size)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setPublicVersion(publicNotification)
                .setWhen(System.currentTimeMillis())
                .setShowWhen(true)
                .setGroup("RIDES")
                .setColor(ContextCompat.getColor(this, R.color.prevoztheme_color))
                .setOngoing(false)

        if (rideIds.size > 1) {
            if (rideTimes != null) {
                val times = rideTimes.joinToString { it.format(DateTimeFormatter.ofPattern("HH:mm")) }
                notificationBuilder.setContentText(times)
            }
        }

        val notification = notificationBuilder.build()
        notifyManager.notify(id, notification)
    }


    private fun parseRideIds(rideIds: String?): IntArray {
        if (rideIds == null) return IntArray(0)
        val jsonArray = JSONArray(rideIds)

        return (0 until jsonArray.length())
                .map { jsonArray.getInt(it) }.toIntArray()
    }

    private fun parseRideTimes(rideTimes: String?): List<LocalTime> {
        if (rideTimes == null) return listOf()
        val jsonArray = JSONArray(rideTimes)

        return (0 until jsonArray.length())
            .map { LocalTime.parse(jsonArray.getString(it), DateTimeFormatter.ISO_DATE_TIME) }
    }
}