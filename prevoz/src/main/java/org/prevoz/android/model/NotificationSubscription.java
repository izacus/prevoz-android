package org.prevoz.android.model;

import android.os.Parcel;
import android.os.Parcelable;

import org.threeten.bp.LocalDate;

import java.util.Calendar;

public class NotificationSubscription implements Parcelable
{
    private final Long id;
    private final City from;
    private final City to;
    private final LocalDate date;

    public NotificationSubscription(Long id, City from, City to, LocalDate date)
    {
        this.from = from;
        this.to = to;
        this.date = date;
        this.id = id;
    }

    public NotificationSubscription(City from, City to, LocalDate date)
    {
        this(null, from, to, date);
    }

    public Long getId() {
        return id;
    }

    public City getFrom() {
        return from;
    }

    public City getTo() {
        return to;
    }

    public LocalDate getDate() {
        return date;
    }


    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeValue(this.id);
        dest.writeParcelable(this.from, 0);
        dest.writeParcelable(this.to, 0);
        dest.writeSerializable(this.date);
    }

    private NotificationSubscription(Parcel in)
    {
        this.id = (Long) in.readValue(Long.class.getClassLoader());
        this.from = in.readParcelable(City.class.getClassLoader());
        this.to = in.readParcelable((City.class.getClassLoader()));
        this.date = (LocalDate) in.readSerializable();
    }

    public static Parcelable.Creator<NotificationSubscription> CREATOR = new Parcelable.Creator<NotificationSubscription>()
    {
        public NotificationSubscription createFromParcel(Parcel source)
        {
            return new NotificationSubscription(source);
        }

        public NotificationSubscription[] newArray(int size)
        {
            return new NotificationSubscription[size];
        }
    };
}
