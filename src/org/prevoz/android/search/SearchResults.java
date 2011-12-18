package org.prevoz.android.search;

import java.util.ArrayList;
import java.util.HashMap;

public class SearchResults
{
	private HashMap<String, String> errors;
	private ArrayList<SearchRide> rides;

	public SearchResults(ArrayList<SearchRide> rides)
	{
		this.rides = rides;
		this.errors = null;
	}

	public SearchResults(HashMap<String, String> errors)
	{
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

	public HashMap<String, String> getErrors()
	{
		return errors;
	}

	public ArrayList<SearchRide> getRides()
	{
		return rides;
	}
}
