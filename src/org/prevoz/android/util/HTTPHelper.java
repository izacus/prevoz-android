package org.prevoz.android.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.HttpsURLConnection;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.prevoz.android.Globals;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

public class HTTPHelper
{
	static
	{
	    // HTTP connection reuse which was buggy pre-froyo
	    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
	        System.setProperty("http.keepAlive", "false");
	    }
	}
	
	private static String sessionCookies = "";

	public static Date parseISO8601(String date) throws ParseException
	{
		// There's a bug in SimpleDateFormatter library so we're parsing dates
		// manually
		SimpleDateFormat formatter = LocaleUtil.getSimpleDateFormat("yyyy-MM-dd'T'hh:mm:ssz");
		Date result = formatter.parse(date);
		
		return result;
	}

	/**
	 * Builds a HTTP parameter string for URL inclusion
	 * 
	 * @param params
	 *            Parameter key/value pairs
	 * @return built parameter string
	 */
	public static String buildGetParams(Map<String, String> params)
	{
		if (params == null)
			return "";

		StringBuilder paramString = new StringBuilder();
		paramString.append("?");

		for (Entry<String, String> entry : params.entrySet())
		{
			try
			{
				paramString.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
				paramString.append("=");
				paramString.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
				paramString.append("&");
			}
			catch (UnsupportedEncodingException e)
			{
				Log.e("HTTPHelper", "Something went really wrong with encodings!", e);
				throw new RuntimeException("Unsupported encoding!");
			}
		}

		// Delete the last amperstand
		paramString.deleteCharAt(paramString.length() - 1);

		Log.d("Utilities", paramString.toString());

		return paramString.toString();
	}

	/**
	 * Reads data from input stream to the end and returns it as single string
	 * 
	 * @param is
	 *            Input stream to read
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
	 * 
	 * @param url
	 *            URL of the resource on server
	 * @param params
	 *            Parameters to be appended to URL
	 * @return Server response or <b>null</b> if the request failed
	 * @throws IOException
	 */
	public static String httpGet(String urlString, String params) throws IOException
	{
		URL url = new URL(getUrlPrefix() + urlString + (params != null ? params : ""));
		
		URLConnection connection;
		if (getUrlPrefix().startsWith("https"))
		{
			connection = (HttpsURLConnection)url.openConnection();
		}
		else
		{
			connection = (HttpURLConnection)url.openConnection();
		}
		
		connection.addRequestProperty("User-Agent", "Prevoz on Android " + Build.VERSION.SDK_INT);

		Log.d("HTTPHelper", "Getting " + url + params);
		
		// Add session cookies to request
		if (sessionCookies != null)
		{
			Log.d("HTTPHelper", "Getting with session cookies " + sessionCookies);
			connection.addRequestProperty("Cookie", sessionCookies);
		}

		try
		{
			InputStream instream = new BufferedInputStream(connection.getInputStream());
			String responseString = HTTPHelper.convertStreamToString(instream);
			instream.close();

			return responseString;
		}
		finally
		{
			if (connection instanceof HttpURLConnection)
			{
				((HttpURLConnection)connection).disconnect();
			}
			else
			{
				((HttpsURLConnection)connection).disconnect();
			}
		}
	}

	public static String httpPost(String url) throws IOException
	{
		return httpPost(url, null, false);
	}
	
	public static String httpPost(String url, Map<String, String> parameters, boolean storeCookies) throws IOException
	{
		DefaultHttpClient client = new DefaultHttpClient();
		
		HttpPost post = new HttpPost(getUrlPrefix() + url);

		// Set user agent for sending
		post.addHeader("User-Agent", "Prevoz on Android "
				+ Build.VERSION.SDK_INT);

		// Prepare session cookies
		if (sessionCookies != null)
		{
			Log.d("HTTPHelper", "Posting with session cookies " + sessionCookies);
			post.addHeader("Cookie", sessionCookies);
		}
		
		// Prepare POST parameters
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
		
		if (parameters != null)
		{
			for (Entry<String, String> param : parameters.entrySet())
			{
				nameValuePairs.add(new BasicNameValuePair(param.getKey(), param.getValue()));
			}
		}
		
		post.setEntity(new UrlEncodedFormEntity(nameValuePairs, HTTP.UTF_8));
		
		// Do call
		HttpResponse response = client.execute(post);
		HttpEntity entity = response.getEntity();

		if (entity != null)
		{
			String responseString = EntityUtils.toString(entity);
			if (storeCookies)
			{
				StringBuilder builder = new StringBuilder(50);
				
				CookieStore store = client.getCookieStore();
				
				for (Cookie cookie : store.getCookies())
				{
					builder.append(cookie.getName() + "=" + cookie.getValue() + ";");
				}
				
				sessionCookies = builder.toString();
				
				Log.d("HTTPHelper", "Storing session cookies " + sessionCookies);
			}
			
			return responseString;
		}

		return null;
	}
	
	public static void updateSessionCookies(Context context)
	{
		// Restore session cookies
		CookieSyncManager.createInstance(context).sync();
		CookieManager cookieManager = CookieManager.getInstance();
		sessionCookies = cookieManager.getCookie(Globals.API_DOMAIN);
		
		Log.d("HTTPHelper", "Storing session cookies " + sessionCookies);
	}
	
	/**
	 * Returns http:// on <FROYO and https:// >=FROYO to workaround CA problem
	 * @return
	 */
	public static String getUrlPrefix() {
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ECLAIR_MR1) {
			return "https://";
		}
		
		return "http://";
	}
}
