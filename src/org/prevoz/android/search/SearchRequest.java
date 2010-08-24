package org.prevoz.android.search;

import java.util.HashMap;

import android.app.Activity;

public class SearchRequest
{
    private Activity context = null;
    private HashMap<String, String> parameters = null;
    
    public SearchRequest(Activity context, HashMap<String, String> parameters)
    {
	this.context = context;
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
}
