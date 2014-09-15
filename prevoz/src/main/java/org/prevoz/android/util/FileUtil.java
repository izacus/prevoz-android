package org.prevoz.android.util;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

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
    public static String[] readLines(Context context, int rawResId)
    {
        InputStream is = context.getResources().openRawResource(rawResId);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));

        List<String> lines = new ArrayList<String>();
        String line;
        try
        {
            while ((line = reader.readLine()) != null)
            {
                lines.add(line);
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

        return lines.toArray(new String[lines.size()]);
    }
}