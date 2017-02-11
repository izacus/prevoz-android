package org.prevoz.android.push

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.support.v4.app.NotificationCompat
import android.util.Log
import com.crashlytics.android.Crashlytics
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.prevoz.android.MainActivity
import org.prevoz.android.PrevozApplication
import org.prevoz.android.R
import org.prevoz.android.model.City
import org.prevoz.android.model.PrevozDatabase
import org.prevoz.android.util.LocaleUtil
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import java.util.*
import javax.inject.Inject

class PushReceiverService : FirebaseMessagingService() {

    init {
        (application as PrevozApplication?)?.component()?.inject(this)
    }

    @Inject lateinit var database : PrevozDatabase

    override fun onMessageReceived(msg: RemoteMessage?) {
        if (msg == null) return
        val data = msg.data
        val rideIds = parseRideIds(data["rides"])
        if (rideIds.isEmpty()) return

        if (!data.containsKey("fromcity") ||
            !data.containsKey("tocity") ||
            !data.containsKey("from_country") ||
            !data.containsKey("to_country")) {
            Crashlytics.log("Invalid GCM message received: " + data.toString())
        }

        Log.d("Prevoz", "Message: " + data.toString())

        val from = City(data["fromcity"]!!, data["from_country"]!!)
        val to = City(data["tocity"]!!, data["to_country"]!!)
        val date = LocalDate.parse(data["date"]!!, DateTimeFormatter.ISO_LOCAL_DATE)
        createNewNotification(database, from, to, date, rideIds)

    }

    fun createNewNotification(database: PrevozDatabase,
                              from: City,
                              to: City,
                              date: LocalDate,
                              rideIds: IntArray) {
        // Create notification message:
        val notifyManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val title = resources.getQuantityString(R.plurals.notify_statusbar, rideIds.size) + " " + from.getLocalizedName(database) + " - " + to.getLocalizedName(database)

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
                .setContentTitle(resources.getQuantityString(R.plurals.notify_statusbar_private, rideIds.size))
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
                .setColor(resources.getColor(R.color.prevoztheme_color))
                .setOngoing(false)

        if (rideIds.size > 1) {
            notificationBuilder.setContentText(rideIds.size.toString() + " " + LocaleUtil.getStringNumberForm(resources, R.array.ride_forms, rideIds.size) + " v " + LocaleUtil.getNotificationDayName(resources, date).toLowerCase())
        }

        val notification = notificationBuilder.build()
        notifyManager.notify(id, notification)
    }


    fun parseRideIds(rideIds: String?): IntArray {
        if (rideIds == null) return IntArray(0)
        val ids = ArrayList<Int>()
        val stripped = rideIds.replace("[^0-9,]".toRegex(), "")

        val tokenizer = StringTokenizer(stripped.trim { it <= ' ' }, ",")
        while (tokenizer.hasMoreTokens()) {
            val token = tokenizer.nextToken()
            val id = Integer.valueOf(token)
            ids.add(id)
        }

        val iIds = IntArray(ids.size)
        for (i in ids.indices)
            iIds[i] = ids[i]

        return iIds
    }
}