package org.prevoz.android.rideinfo;

import java.util.Calendar;
import java.util.Date;

import org.prevoz.android.City;
import org.prevoz.android.RideType;

import android.os.Bundle;
import org.prevoz.android.util.LocaleUtil;

public class Ride
{
	private int id;
	private RideType type;

	private City from;
	private City to;

	private Calendar time;

	private int people;
	private Double price;

	private String author;
	private String contact;
	private String comment;

	private boolean isAuthor;
	private boolean isInsured;
	private boolean isFull;

	public Ride(int id, RideType type, City from, City to, Calendar time,
			int people, Double price, String author, String contact,
			String comment, boolean isAuthor, boolean isInsured, boolean isFull)
	{
		this.id = id;
		this.type = type;
		this.from = from;
		this.to = to;
		this.time = time;
		this.people = people;
		this.price = price;
		this.author = author;
		this.contact = contact;
		this.comment = comment;
		this.isAuthor = isAuthor;
		this.isInsured = isInsured;
		this.isFull = isFull;
	}

	/**
	 * Used to restore ride from a bundle
	 * 
	 * @param bundle
	 */
	public Ride(Bundle bundle)
	{
		id = bundle.getInt("rideinfo_id");
		type = RideType.values()[bundle.getInt("rideinfo_type")];
		from = new City(bundle.getString("rideinfo_from"), bundle.getString("rideinfo_from_country"));
		to = new City(bundle.getString("rideinfo_to"), bundle.getString("rideinfo_to_country"));
        time = Calendar.getInstance(LocaleUtil.getLocalTimezone());
        time.setTimeInMillis(bundle.getLong("rideinfo_time"));
		people = bundle.getInt("rideinfo_people");

		if (bundle.containsKey("rideinfo_price"))
		{
			price = bundle.getDouble("rideinfo_price");
		}
		else
		{
			price = null;
		}

		author = bundle.getString("rideinfo_author");
		contact = bundle.getString("rideinfo_contact");
		comment = bundle.getString("rideinfo_comment");
		isAuthor = bundle.getBoolean("rideinfo_isauthor");
	}

	public int getId()
	{
		return id;
	}

	public RideType getType()
	{
		return type;
	}

	public City getFrom()
	{
		return from;
	}

	public City getTo()
	{
		return to;
	}

	public Calendar getTime()
	{
		return time;
	}

	public int getPeople()
	{
		return people;
	}

	public Double getPrice()
	{
		return price;
	}

	public String getAuthor()
	{
		return author;
	}

	public String getContact()
	{
		return contact;
	}

	public String getComment()
	{
		return comment;
	}

	public boolean isAuthor()
	{
		return isAuthor;
	}

	public boolean isInsured()
	{
		return isInsured;
	}

	public void storeToBundle(Bundle bundle)
	{
		bundle.putInt("rideinfo_id", id);
		bundle.putInt("rideinfo_type", type.ordinal());
		bundle.putString("rideinfo_from", from.getDisplayName());
		bundle.putString("rideinfo_from_country", from.getCountryCode());
		bundle.putString("rideinfo_to", to.getDisplayName());
		bundle.putString("rideinfo_to_country", to.getCountryCode());
		bundle.putLong("rideinfo_time", time.getTimeInMillis());
		bundle.putInt("rideinfo_people", people);

		if (price != null)
		{
			bundle.putDouble("rideinfo_price", price);
		}

		bundle.putString("rideinfo_author", author);
		bundle.putString("rideinfo_contact", contact);
		bundle.putString("rideinfo_comment", comment);
		bundle.putBoolean("rideinfo_isauthor", isAuthor);
	}
	
	public boolean isFull() {
		return isFull;
	}

	public void setFull(boolean isFull) {
		this.isFull = isFull;
	}
}
