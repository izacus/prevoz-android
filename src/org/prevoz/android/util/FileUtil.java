package org.prevoz.android.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

import android.content.Context;
import android.util.Log;

public class FileUtil
{
	/**
	 * Reads text SQL file and breaks it up on ";" signs to be used separately
	 * by execSQL
	 * 
	 * @param context
	 * @param rawResId
	 *            resource id of .sql file
	 * @return string array of SQL statements in file
	 */
	public static String[] readSQLStatements(Context context, int rawResId)
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
			return new String[0];
		}
		finally
		{
			try
			{
				reader.close();
			} catch (IOException e)
			{
				// Nothing TBD
			}
		}

		StringTokenizer tokenizeSQL = new StringTokenizer(
				fileContent.toString(), ";");
		String[] SQLStatements = new String[tokenizeSQL.countTokens()];

		int i = 0;

		while (tokenizeSQL.hasMoreTokens())
		{
			SQLStatements[i++] = tokenizeSQL.nextToken();
		}

		return SQLStatements;
	}
}
