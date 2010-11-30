package org.prevoz.android.search;

import java.util.HashMap;

import org.prevoz.android.RideType;

import android.app.Activity;

public class SearchRequest
{
	private Activity context = null;
	private RideType searchType = null;
	private HashMap<String, String> parameters = null;

	public SearchRequest(Activity context, RideType searchType,
			HashMap<String, String> parameters)
	{
		this.context = context;
		this.searchType = searchType;
		this.parameters = parameters;
	}

	public Activity getContext()
	{
		return context;
	}

	public HashMap<String, String> getParameters()
	{
		return parameters;
	}

	public RideType getSearchType()
	{
		return searchType;
	}
}
