package org.prevoz.android.api.rest;

import com.google.gson.annotations.SerializedName;
import org.prevoz.android.model.City;

import java.util.Date;

/**
 * Created by jernej on 10/05/14.
 */
public class RestPushSubscription
{
    @SerializedName("registration_id")
    public String registrationId;
    @SerializedName("from")
    public String from;
    @SerializedName("fromcountry")
    public String fromCountry;
    @SerializedName("to")
    public String to;
    @SerializedName("tocountry")
    public String toCountry;

    @SerializedName("date")
    public Date date;
    @SerializedName("action")
    public String action;

    public RestPushSubscription(String registrationId, City from, City to, Date date, boolean isSubscribed)
    {
        this.registrationId = registrationId;
        this.from = from.getDisplayName();
        this.fromCountry = from.getCountryCode();
        this.to = to.getDisplayName();
        this.toCountry = to.getCountryCode();
        this.date = date;
        this.action = isSubscribed ? "subscribe" : "unsubscribe";
    }
}
