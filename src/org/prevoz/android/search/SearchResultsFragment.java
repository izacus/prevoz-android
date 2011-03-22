package org.prevoz.android.search;

import java.util.Calendar;

import org.prevoz.android.R;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class SearchResultsFragment extends Fragment
{
	public SearchResultsFragment(String from, String to, Calendar when)
	{
		
	}
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, 
							 ViewGroup container,
							 Bundle savedInstanceState)
	{
		View view = inflater.inflate(R.layout.search_results_frag, container, false); 
		return view;
	}

}
