package org.prevoz.android.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.content.Context;
import android.util.Log;

public class FileUtil
{
    public static String readTxtFile(Context context, int rawResId)
    {
	InputStream is = context.getResources().openRawResource(rawResId);
	BufferedReader reader = new BufferedReader(new InputStreamReader(is));
	
	StringBuilder fileContent = new StringBuilder(8192);
	
	String line = null;
	
	try
	{
	    while ((line = reader.readLine()) != null)
	    {
	        fileContent.append(line);
	    }
	} 
	catch (IOException e)
	{
	    Log.e("FileUtil", "Failed to read resource with ID " + rawResId, e);
	    return "";
	}
	
	return fileContent.toString();
    }
}
