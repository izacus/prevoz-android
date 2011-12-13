package org.prevoz.android.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

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
		String[] monthNames = res.getStringArray(R.array.month_names);
		return date.get(Calendar.DATE) + ". "
				+ monthNames[date.get(Calendar.MONTH)] + " "
				+ date.get(Calendar.YEAR);
	}

	/**
	 * Builds a localized date string with day name
	 */
	public static String localizeDate(Resources resources, Calendar date)
	{	
		Calendar now = Calendar.getInstance();
		// Check for today and tomorrow
		if (date.get(Calendar.ERA) == now.get(Calendar.ERA) && 
			date.get(Calendar.YEAR) == now.get(Calendar.YEAR))
		{
			// Today
			if (date.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR))
			{
				return resources.getString(R.string.today);
			}
			
			// Add one day to now to get tomorrows date
			now.roll(Calendar.DAY_OF_YEAR, 1);
			
			// Tomorrow, because we added one day to now
			if (date.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR))
			{
				return resources.getString(R.string.tomorrow);
			}
		}

		StringBuilder dateString = new StringBuilder();

		dateString.append(LocaleUtil.getDayName(resources, date) + ", ");
		dateString.append(LocaleUtil.getFormattedDate(resources, date));

		return dateString.toString();
	}
	
	
	public static String getStringNumberForm(Resources res, int resourceArray,
			int number)
	{
		String[] wordArray = res.getStringArray(resourceArray);

		int mod = number % 100;

		switch (mod)
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
	
	public static SimpleDateFormat getSimpleDateFormat(String format)
	{
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		sdf.setTimeZone(getLocalTimezone());
		return sdf;
	}
	
	public static TimeZone getLocalTimezone()
	{
		TimeZone tz = TimeZone.getTimeZone("Europe/Ljubljana");
		
		if (tz.getID().equals(TimeZone.getTimeZone("GMT")))
		{
			return TimeZone.getDefault();
		}
		
		return tz;
	}
}
