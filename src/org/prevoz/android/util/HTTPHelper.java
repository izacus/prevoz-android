package org.prevoz.android.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.prevoz.android.Globals;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

public class HTTPHelper
{
    private static SimpleDateFormat iso8601formatter = new SimpleDateFormat("yyyy-MM-DD'T'hh:mm:ssZ");
    private static String sessionCookies = "";
    
    public static Date parseISO8601(String date) throws ParseException
    {
	return iso8601formatter.parse(date);
    }
    
    /**
     * Builds a HTTP parameter string for URL inclusion
     * @param params Parameter key/value pairs
     * @return built parameter string
     */
    public static String buildGetParams(Map<String, String> params)
    {
	if (params == null)
	    return "";

	StringBuilder paramString = new StringBuilder();
	paramString.append("?");

	for (String key : params.keySet())
	{
	    paramString.append(URLEncoder.encode(key));
	    paramString.append("=");
	    paramString.append(URLEncoder.encode(params.get(key)));
	    paramString.append("&");
	}

	// Delete the last amperstand
	paramString.deleteCharAt(paramString.length() - 1);

	Log.d("Utilities", paramString.toString());

	return paramString.toString();
    }

    /**
     * Reads data from input stream to the end and returns it as single string
     * @param is Input stream to read
     * @return data from stream
     */
    private static String convertStreamToString(InputStream is)
    {
	BufferedReader reader = new BufferedReader(new InputStreamReader(is));
	StringBuilder sb = new StringBuilder();

	String line = null;
	try
	{
	    while ((line = reader.readLine()) != null)
	    {
		sb.append(line + "\n");
	    }
	} 
	catch (IOException e)
	{
	    e.printStackTrace();
	} 
	finally
	{
	    try
	    {
		is.close();
	    } 
	    catch (IOException e)
	    {
		e.printStackTrace();
	    }
	}
	
	return sb.toString();
    }
    
    public static String httpGet(String url) throws IOException
    {
	return httpGet(url, null);
    }
    
    /**
     * Makes a GET request to a server and reads response
     * @param url URL of the resource on server
     * @param params Parameters to be appended to URL
     * @return Server response or <b>null</b> if the request failed
     * @throws IOException
     */
    public static String httpGet(String url, String params) throws IOException
    {	
	DefaultHttpClient client = new DefaultHttpClient();
	HttpGet get = new HttpGet(url + (params != null ? params : ""));
	
	get.addHeader("User-Agent", "Prevoz on Android " + Build.VERSION.SDK_INT);
	
	// Add session cookies to request
	if (sessionCookies != null)
	{
	    get.addHeader("Cookie", sessionCookies);
	}
	
	HttpResponse response = client.execute(get);
	HttpEntity entity = response.getEntity();
	 
	if (entity != null)
	{
	    InputStream instream = entity.getContent();
	    String responseString = HTTPHelper.convertStreamToString(instream);
	    instream.close();
	    
	    return responseString;
	}
	
	return null;
    }
    
    public static String httpPost(String url) throws IOException
    {
	DefaultHttpClient client = new DefaultHttpClient();
	HttpPost post = new HttpPost(url);
	
	post.addHeader("User-Agent", "Prevoz on Android " + Build.VERSION.SDK_INT);
	
	if (sessionCookies != null)
	{
	    post.addHeader("Cookie", sessionCookies);
	}
	
	HttpResponse response = client.execute(post);
	HttpEntity entity = response.getEntity();
	 
	if (entity != null)
	{
	    InputStream instream = entity.getContent();
	    String responseString = HTTPHelper.convertStreamToString(instream);
	    instream.close();
	    
	    return responseString;
	}
	
	return null;
    }
    
    public static void updateSessionCookies(Context context)
    {
	// Restore session cookies
	CookieSyncManager.createInstance(context);
	CookieManager cookieManager = CookieManager.getInstance();
	sessionCookies = cookieManager.getCookie(Globals.API_DOMAIN);
    }
}
