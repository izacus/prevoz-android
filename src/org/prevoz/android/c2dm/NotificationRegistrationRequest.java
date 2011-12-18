package org.prevoz.android.c2dm;

import java.util.Calendar;

public class NotificationRegistrationRequest 
{
	private String registrationId;
	private String from;
	private String to;
	private Calendar when;
	private boolean register;
	
	
	public NotificationRegistrationRequest(String registrationId, String from, String to,
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


	public String getFrom() {
		return from;
	}


	public String getTo() {
		return to;
	}


	public Calendar getWhen() {
		return when;
	}


	public boolean isRegister() {
		return register;
	}
}
