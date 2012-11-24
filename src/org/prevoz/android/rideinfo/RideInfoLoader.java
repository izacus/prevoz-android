package org.prevoz.android.rideinfo;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;
import org.prevoz.android.City;
import org.prevoz.android.Globals;
import org.prevoz.android.RideType;
import org.prevoz.android.util.AsyncLoader;
import org.prevoz.android.util.HTTPHelper;

import android.content.Context;
import android.util.Log;

public class RideInfoLoader extends AsyncLoader<Ride> 
{
	private int rideID;
	
	public RideInfoLoader(Context context, int rideID) 
	{
		super(context);
		this.rideID = rideID;
	}

	@Override
	public Ride loadInBackground() 
	{
		String response = null;

		try
		{
			String url = Globals.API_URL + "/carshare/" + rideID + "/";
			response = HTTPHelper.httpGet(url);
		}
		catch (IOException e)
		{
			Log.e(this.toString(), "Error retrieving data for ride ID " + rideID, e);
			return null;
		}

		if (response == null)
		{
			return null;
		}

		try
		{
			JSONObject root = new JSONObject(response);

			int id = root.getInt("id");
			RideType type = root.getString("share_type").equalsIgnoreCase(
					"share") ? RideType.SHARE : RideType.SEEK;

			String from = root.getString("from");
			String to = root.getString("to");

			String iso8601 = root.getString("date_iso8601");
			Date time = HTTPHelper.parseISO8601(iso8601);

			int people = root.getInt("num_people");

			Double price = root.isNull("price") ? null : root
					.getDouble("price");

			String author = root.getString("author");
			String contact = root.getString("contact");
			String comment = root.getString("comment").replace('\r', ' ');

			boolean isAuthor = root.getBoolean("is_author");
			boolean isInsured = root.getBoolean("insured");
			boolean isFull = root.getBoolean("full");
			
			Log.i(this.toString(), "Succesfully parsed response for ride id " + rideID);
			
			return new Ride(id, type, new City(from, "SI"), new City(to, "SI"), time, people, price, author, contact, comment, isAuthor, isInsured, isFull);
		}
		catch (JSONException e)
		{
			Log.e(this.toString(), "Error parsing JSON for ride id " + rideID, e);
			return null;
		}
		catch (ParseException e)
		{
			Log.e(this.toString(),
					"Error parsing date for ride id + " + rideID, e);
			return null;
		}

	}
}
