package org.prevoz.android.c2dm;


public class NotificationManager 
{
	private static NotificationManager instance;
	
	public static synchronized NotificationManager getInstance()
	{
		if (instance == null)
		{
			instance = new NotificationManager();
		}
		
		return instance;
	}
	
	private NotificationManager()
	{
	}
}
