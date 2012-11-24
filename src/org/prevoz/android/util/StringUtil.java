package org.prevoz.android.util;

import org.prevoz.android.City;

import android.graphics.Color;
import android.widget.Button;

public class StringUtil
{
	/**
	 * Removes all non-digit characters from string
	 * 
	 * @param string
	 * @return
	 */
	public static String numberOnly(String string, boolean decimal)
	{
		StringBuilder output = new StringBuilder();

		for (int i = 0; i < string.length(); i++)
		{
			if (Character.isDigit(string.charAt(i))
					|| (decimal && string.charAt(i) == '.'))
			{
				output.append(string.charAt(i));
			}
		}

		return output.toString();
	}
	
	public static void setLocationButtonText(Button button, City location, String defaultValue)
	{
		if (location == null)
		{
			button.setTextColor(Color.LTGRAY);
			button.setText(defaultValue);
		}
		else
		{
			button.setTextColor(Color.BLACK);
			button.setText(location.toString());
		}
	}
}
