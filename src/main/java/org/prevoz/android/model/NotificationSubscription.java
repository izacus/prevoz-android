package org.prevoz.android.model;

import java.util.Calendar;

public class NotificationSubscription
{
    private Integer id;
    private City from;
    private City to;
    private Calendar date;

    public NotificationSubscription(City from, City to, Calendar date)
    {
        this.from = from;
        this.to = to;
        this.date = date;
        this.id = null;
    }

    public NotificationSubscription(int id, City from, City to, Calendar date)
    {
        this(from, to, date);
        this.id = id;
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
