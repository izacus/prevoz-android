package org.prevoz.android.my_rides;

import java.io.IOException;

import org.prevoz.android.Globals;
import org.prevoz.android.search.SearchResultsLoader;
import org.prevoz.android.util.HTTPHelper;

import android.content.Context;

public class MyRidesLoader extends SearchResultsLoader 
{
	private static final String RIDES_ENDPOINT = Globals.API_URL + "/carshare/list/";
	
	public MyRidesLoader(Context context) 
	{
		super(context, null);
	}

	@Override
	protected String getResponse() throws IOException 
	{
		return HTTPHelper.httpGet(RIDES_ENDPOINT);
	}
}
