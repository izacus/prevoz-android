package org.prevoz.android.rideinfo;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;
import org.prevoz.android.Globals;
import org.prevoz.android.RideType;
import org.prevoz.android.util.HTTPUtils;

import android.os.Handler;
import android.util.Log;

public class LoadInfoTask implements Runnable
{
    private int rideID;
    private Handler callback;
    
    private Ride result = null;
    
    public LoadInfoTask()
    {
	
    }

    public void loadInfo(int id, Handler callback)
    {
	this.callback = callback;
	this.rideID = id;
	
	Thread thread = new Thread(this);
	thread.setDaemon(true);
	thread.start();
    }
    
    public void run()
    {
	String response = null;
	
	try
	{
	    String url = Globals.API_URL + "/carshare/" + rideID + "/";
	    response = HTTPUtils.httpGet(url);
	}
	catch(IOException e)
	{
	    Log.e(this.toString(), "Error retrieving data for ride ID " + rideID, e);
	    callback.sendEmptyMessage(Globals.REQUEST_ERROR_NETWORK);
	    return;
	}
	
	if (response == null)
	{
	    callback.sendEmptyMessage(Globals.REQUEST_ERROR_SERVER);
	    return;
	}
	
	try
	{
	    JSONObject root = new JSONObject(response);
	    
	    int id = root.getInt("id");
	    RideType type = root.getString("share_type") == "share" ? RideType.SHARE : RideType.SEEK;
	    
	    String from = root.getString("from");
	    String to = root.getString("to");
	    
	    Date time = HTTPUtils.parseISO8601(root.getString("date_iso8601"));
	    
	    int people = root.getInt("num_people");
	    
	    Double price = root.isNull("price") ? null : root.getDouble("price");
	    
	    String author = root.getString("author");
	    String contact = root.getString("contact");
	    String comment = root.getString("comment");
	    
	    boolean isAuthor = root.getBoolean("is_author");
	    
	    result = new Ride(id, type, from, to, time, people, price, author, contact, comment, isAuthor);
	}
	catch (JSONException e)
	{
	    Log.e(this.toString(), "Error parsing JSON for ride id " + rideID, e);
	    callback.sendEmptyMessage(Globals.REQUEST_ERROR_SERVER);
	    return;
	}
	catch (ParseException e)
	{
	    Log.e(this.toString(), "Error parsing date for ride id + " + rideID, e);
	    callback.sendEmptyMessage(Globals.REQUEST_ERROR_SERVER);
	    return;
	}
	
	Log.i(this.toString(), "Succesfully parsed response for ride id " + rideID);
	callback.sendEmptyMessage(Globals.REQUEST_SUCCESS);
    }

    public Ride getResult()
    {
        return result;
    }

}
