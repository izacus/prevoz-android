package org.prevoz.android.rideinfo;

import java.util.Date;

import org.prevoz.android.RideType;

import android.os.Bundle;

public class Ride
{
	private int id;
	private RideType type;

	private String from;
	private String to;

	private Date time;

	private int people;
	private Double price;

	private String author;
	private String contact;
	private String comment;

	private boolean isAuthor;

	public Ride(int id, RideType type, String from, String to, Date time,
			int people, Double price, String author, String contact,
			String comment, boolean isAuthor)
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
		from = bundle.getString("rideinfo_from");
		to = bundle.getString("rideinfo_to");
		time = new Date(bundle.getLong("rideinfo_time"));
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

	public String getFrom()
	{
		return from;
	}

	public String getTo()
	{
		return to;
	}

	public Date getTime()
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

	public void storeToBundle(Bundle bundle)
	{
		bundle.putInt("rideinfo_id", id);
		bundle.putInt("rideinfo_type", type.ordinal());
		bundle.putString("rideinfo_from", from);
		bundle.putString("rideinfo_to", to);
		bundle.putLong("rideinfo_time", time.getTime());
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
}
