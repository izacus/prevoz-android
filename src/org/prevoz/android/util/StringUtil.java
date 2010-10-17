package org.prevoz.android.util;


public class StringUtil
{
    /**
     * Removes all non-digit characters from string
     * @param string
     * @return
     */
    public static String numberOnly(String string, boolean decimal)
    {
	StringBuilder output = new StringBuilder();
	
	for (int i = 0; i < string.length(); i++)
	{
	    if (Character.isDigit(string.charAt(i)) || (decimal && string.charAt(i) == '.'))
	    {
		output.append(string.charAt(i));
	    }
	}
	
	return output.toString();
    }
}
