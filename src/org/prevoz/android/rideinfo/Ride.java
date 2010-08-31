package org.prevoz.android.rideinfo;

import java.util.Date;

import org.prevoz.android.RideType;

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

    public Ride(int id, 
	    	RideType type, 
	    	String from, 
	    	String to, 
	    	Date time,
	    	int people, 
	    	Double price, 
	    	String author, 
	    	String contact,
	    	String comment, 
	    	boolean isAuthor)
    {
	super();
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
}
