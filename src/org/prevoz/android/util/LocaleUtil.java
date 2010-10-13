package org.prevoz.android.util;

import java.util.Calendar;
import java.util.Date;

import org.prevoz.android.R;

import android.content.res.Resources;

public class LocaleUtil
{
    public static String getDayName(Resources res, Date date)
    {
	Calendar cal = Calendar.getInstance();
	cal.setTime(date);
	return getDayName(res, cal);
    }
    
    public static String getDayName(Resources res, Calendar date)
    {
	String[] dayNames = res.getStringArray(R.array.day_names);
	return dayNames[date.get(Calendar.DAY_OF_WEEK) - 1];
    }
    
    public static String getFormattedDate(Resources res, Date date)
    {
	Calendar cal = Calendar.getInstance();
	cal.setTime(date);
	
	return getFormattedDate(res, cal);
    }
    
    public static String getFormattedDate(Resources res, Calendar date)
    {
	String[] monthNames =  res.getStringArray(R.array.month_names);
	return date.get(Calendar.DATE) + ". " + monthNames[date.get(Calendar.MONTH)] + " " + date.get(Calendar.YEAR);
    }
    
    public static String getStringNumberForm(Resources res, int resourceArray, int number)
    {
	String[] wordArray = res.getStringArray(resourceArray);
	
	int mod = number % 100;
	
	switch(mod)
	{
	   case 1:
	       return wordArray[0];
	   case 2:
	       return wordArray[1];
	   case 3:
	   case 4:
	       return wordArray[2];
	   default:
	       return wordArray[3];
	}
    }
}
