package org.prevoz.android.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.prevoz.android.R;
import org.prevoz.android.SectionedAdapter;
import org.prevoz.android.search.SearchResultAdapter;
import org.prevoz.android.search.SearchResults;
import org.prevoz.android.search.SearchRide;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class SectionedAdapterUtil {

	public static SectionedAdapter getSectionedAdapter(final Activity activity)
	{
		SectionedAdapter adapter = new SectionedAdapter()
		{

			@Override
			protected View getHeaderView(String caption, 
										 int index,
										 View convertView, 
										 ViewGroup parent)
			{
				TextView result = (TextView) convertView;

				if (convertView == null)
				{
					result = (TextView) activity.getLayoutInflater().inflate(R.layout.list_header, null);
				}

				result.setText(caption);

				return result;
			}
		};
		
		return adapter;
	}

	public static SectionedAdapter buildAdapterWithResults(Activity activity, SearchResults results) 
	{
		SectionedAdapter resultsAdapter = getSectionedAdapter(activity);
		
		// Build categories of results
		if (results.getRides() != null)
		{
			// Put rides into buckets by paths
			HashMap<String, ArrayList<SearchRide>> ridesByPath = new HashMap<String, ArrayList<SearchRide>>();
			for (SearchRide ride : results.getRides())
			{
				String path = ride.getFrom() + " - " + ride.getTo();

				if (ridesByPath.get(path) == null)
					ridesByPath.put(path, new ArrayList<SearchRide>());

				ridesByPath.get(path).add(ride);
			}
			
			ArrayList<String> ridePaths = new ArrayList<String>(ridesByPath.keySet());
			Collections.sort(ridePaths);

			for (String path : ridePaths)
			{
				resultsAdapter.addSection(path, new SearchResultAdapter(activity, ridesByPath.get(path)));
			}
		}
		
		return resultsAdapter;
	}
}
