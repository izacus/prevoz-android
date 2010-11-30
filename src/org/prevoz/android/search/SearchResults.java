package org.prevoz.android.search;

import java.util.ArrayList;
import java.util.HashMap;

import org.prevoz.android.RideType;

public class SearchResults
{
	private RideType rideType;
	private HashMap<String, String> errors;
	private ArrayList<SearchRide> rides;

	public SearchResults(RideType rideType, ArrayList<SearchRide> rides)
	{
		this.rideType = rideType;
		this.rides = rides;
		this.errors = null;
	}

	public SearchResults(RideType rideType, HashMap<String, String> errors)
	{
		this.rideType = rideType;
		this.rides = null;
		this.errors = errors;
	}

	/**
	 * Returns true if the search was successful
	 */
	public boolean isSuccessful()
	{
		return errors == null;
	}

	public RideType getRideType()
	{
		return rideType;
	}

	public HashMap<String, String> getErrors()
	{
		return errors;
	}

	public ArrayList<SearchRide> getRides()
	{
		return rides;
	}
}
