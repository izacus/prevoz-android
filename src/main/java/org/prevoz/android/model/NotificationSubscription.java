package org.prevoz.android.model;

import java.util.Calendar;

public class NotificationSubscription
{
    private final Integer id;
    private final City from;
    private final City to;
    private final Calendar date;

    public NotificationSubscription(Integer id, City from, City to, Calendar date)
    {
        this.from = from;
        this.to = to;
        this.date = date;
        this.id = id;
    }

    public NotificationSubscription(City from, City to, Calendar date)
    {
        this(null, from, to, date);
    }

    public Integer getId() {
        return id;
    }

    public City getFrom() {
        return from;
    }

    public City getTo() {
        return to;
    }

    public Calendar getDate() {
        return date;
    }
}
