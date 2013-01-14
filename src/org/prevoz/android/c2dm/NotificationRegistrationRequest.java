package org.prevoz.android.c2dm;

import org.prevoz.android.City;

import java.util.Calendar;

public class NotificationRegistrationRequest 
{
	private String registrationId;
	private City from;
	private City to;
	private Calendar when;
	private boolean register;
	
	
	public NotificationRegistrationRequest(String registrationId, City from, City to,
			Calendar when, boolean register) 
	{
		super();
		this.registrationId = registrationId;
		this.from = from;
		this.to = to;
		this.when = when;
		this.register = register;
	}


	public String getRegistrationId() {
		return registrationId;
	}


	public City getFrom() {
		return from;
	}


	public City getTo() {
		return to;
	}


	public Calendar getWhen() {
		return when;
	}


	public boolean isRegister() {
		return register;
	}
}
