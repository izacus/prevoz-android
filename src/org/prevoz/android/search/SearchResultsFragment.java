package org.prevoz.android.search;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;

import org.prevoz.android.Globals;
import org.prevoz.android.R;
import org.prevoz.android.RideType;
import org.prevoz.android.SectionedAdapter;
import org.prevoz.android.rideinfo.RideInfoActivity;
import org.prevoz.android.search.SearchResultAdapter.SearchResultViewWrapper;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

public class SearchResultsFragment extends Fragment implements LoaderCallbacks<SearchResults>
{
	private enum DisplayScreens
	{
		LOADING_SCREEN,
		RESULTS_SCREEN
	};
	
	// Status
	private String from;
	private String to;
	private Calendar when;
	
	// Views
	private ViewFlipper viewFlipper;
	private ListView resultList;
	
	private GoogleAnalyticsTracker tracker;
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
		
		SearchResultsActivity activity = (SearchResultsActivity) getActivity();
		this.from = activity.getFrom();
		this.to = activity.getTo();
		this.when = activity.getWhen();
		
		showView(DisplayScreens.RESULTS_SCREEN);
		
		Log.d(this.toString(), "Activity created, succefully fetched data.");
		
		tracker = GoogleAnalyticsTracker.getInstance();
		tracker.trackPageView("/SearchResults");
		
		// Get loader for search results
		getLoaderManager().initLoader(Globals.LOADER_SEARCH_RESULTS, null, this);
		
	}

	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, 
							 ViewGroup container,
							 Bundle savedInstanceState)
	{
		View view = inflater.inflate(R.layout.search_results_frag, container, false);
		
		// Populate fields
		viewFlipper = (ViewFlipper) view.findViewById(R.id.search_results_flipper);
		resultList = (ListView) view.findViewById(R.id.search_results_list);
		
		// Prepare click callback for resultlist
		resultList.setOnItemClickListener(new OnItemClickListener() 
		{
			public void onItemClick(AdapterView<?> parent, 
									View view,
									int position, 
									long id) 
			{
				SearchResultViewWrapper viewWrapper = (SearchResultViewWrapper)view.getTag();
				Intent intent = new Intent(getActivity(), RideInfoActivity.class);
				intent.putExtra(RideInfoActivity.RIDE_ID, viewWrapper.getRideId());
				startActivityForResult(intent, 1);
			};
		});
		
		// Set animations
		viewFlipper.setInAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in));
		viewFlipper.setOutAnimation(AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_out));
		
		return view;
	}

	
	private void showSearchResults(SearchResults results)
	{
		Log.d(this.toString(), "Search results retrieved, drawing...");
		
		if (!results.isSuccessful())
		{
			Toast.makeText(getActivity(), results.getErrors().values().iterator().next(), Toast.LENGTH_LONG).show();
			getActivity().finish();
		}
		
		SectionedAdapter resultsAdapter = getSectionedAdapter();
		
		// Build categories of results
		if (results.getRides() != null && results.getRides().size() > 0)
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
				resultsAdapter.addSection(path, new SearchResultAdapter(getActivity(), ridesByPath.get(path)));
			}
			
			// Show results
			resultList.setAdapter(resultsAdapter);
			
			
		}
		else
		{
			// There are no search results, create a simple list with no results text
			String[] noResults = new String[1];
			noResults[0] = getString(R.string.search_no_results);
			ArrayAdapter<String> noResultsAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, noResults);
			resultList.setAdapter(noResultsAdapter);
			resultList.setOnItemClickListener(null);
		}
		
		viewFlipper.showNext();
	}
	
	private SectionedAdapter getSectionedAdapter()
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
					result = (TextView) getActivity().getLayoutInflater().inflate(R.layout.list_header, null);
				}

				result.setText(caption);

				return result;
			}
		};
		
		return adapter;
	}
	
	private void showView(DisplayScreens screen)
	{
		switch(screen)
		{
			case LOADING_SCREEN:
				viewFlipper.setDisplayedChild(1);
				break;
			case RESULTS_SCREEN:
				viewFlipper.setDisplayedChild(0);
				break;
		}
	}

	public Loader<SearchResults> onCreateLoader(int id, Bundle args) 
	{
		tracker.dispatch();
		SearchRequest request = new SearchRequest(getActivity(), RideType.SHARE, from, to, when);
		return new SearchResultsLoader(getActivity(), request);
	}

	public void onLoadFinished(Loader<SearchResults> loader, SearchResults results) 
	{
		showSearchResults(results);
	}

	public void onLoaderReset(Loader<SearchResults> arg0) 
	{
		// Nothing TBD
	}
}
