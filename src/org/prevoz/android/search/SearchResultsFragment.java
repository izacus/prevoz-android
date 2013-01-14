package org.prevoz.android.search;

import java.util.Calendar;

import org.prevoz.android.City;
import org.prevoz.android.Globals;
import org.prevoz.android.R;
import org.prevoz.android.c2dm.NotificationManager;
import org.prevoz.android.rideinfo.RideInfoActivity;
import org.prevoz.android.search.SearchResultAdapter.SearchResultViewWrapper;
import org.prevoz.android.util.SectionedAdapter;
import org.prevoz.android.util.SectionedAdapterUtil;

import roboguice.inject.InjectResource;
import roboguice.inject.InjectView;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.flurry.android.FlurryAgent;
import com.github.rtyley.android.sherlock.roboguice.fragment.RoboSherlockFragment;

public class SearchResultsFragment extends RoboSherlockFragment implements LoaderCallbacks<SearchResults>
{
	private enum DisplayScreens
	{
		LOADING_SCREEN,
		RESULTS_SCREEN
	};
	
	private MenuItem notifyButton;
	@InjectResource(R.drawable.bell)
	private Drawable bellImg;
	@InjectResource(R.drawable.bell_cross)
	private Drawable bellCrossImg;
	
	
	// Status
	private City from;
	private City to;
	private Calendar when;
	
	// Views
	@InjectView(R.id.search_results_flipper)
	private ViewFlipper viewFlipper;
	@InjectView(R.id.search_results_list)
	private ListView resultList;
	private boolean notificationEnabled = false;
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
		
		// Get data from activity
		SearchResultsActivity activity = (SearchResultsActivity) getActivity();
		this.from = activity.getFrom();
		this.to = activity.getTo();
		this.when = activity.getWhen();
		showView(DisplayScreens.RESULTS_SCREEN);
		
		notificationEnabled = false;
		
		if (from != null && to != null)
		{
			notificationEnabled = NotificationManager.getInstance(getActivity().getApplicationContext()).isNotified(getActivity(), from, to, when);
		}
		
		getSherlockActivity().supportInvalidateOptionsMenu();
		Log.d(this.toString(), "Activity created, succefully fetched data.");
		FlurryAgent.onPageView();
		
		// Get loader for search results
		getLoaderManager().initLoader(Globals.LOADER_SEARCH_RESULTS, null, this);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) 
	{
		inflater.inflate(R.menu.menu_search_results, menu);
		
		notifyButton = menu.findItem(R.id.menu_results_notify);
		if (NotificationManager.getInstance(getActivity().getApplicationContext()).notificationsAvailable() && from != null && to != null)
		{
			notifyButton.setVisible(true);
		}
		else
		{
			notifyButton.setVisible(false);
		}
		
		updateNotifyGraphic();
	}

	
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId())
		{
			case R.id.menu_results_notify:
				notifyClicked();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	public void updateNotifyGraphic()
	{
		if (notificationEnabled)
		{
			notifyButton.setIcon(bellCrossImg);
		}
		else
		{
			notifyButton.setIcon(bellImg);
		}
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}
	
	

	@Override
	public void onResume() 
	{
		super.onResume();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, 
							 ViewGroup container,
							 Bundle savedInstanceState)
	{
		
		View view = inflater.inflate(R.layout.search_results_frag, container, false);
		return view;
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) 
	{
		super.onViewCreated(view, savedInstanceState);
		resultList.setEmptyView(view.findViewById(R.id.search_no_results));
		
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
	}

	private void showSearchResults(SearchResults results)
	{
		Log.d(this.toString(), "Search results retrieved, drawing...");
		
		if (!results.isSuccessful())
		{
			Toast.makeText(getActivity(), results.getErrors().values().iterator().next(), Toast.LENGTH_LONG).show();
			getActivity().finish();
		}
		
		SectionedAdapter resultsAdapter = SectionedAdapterUtil.buildAdapterWithResults(getActivity(), results, ((SearchResultsActivity)getActivity()).getHighlights());
		// Show results
		resultList.setAdapter(resultsAdapter);
		viewFlipper.showNext();
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
		SearchRequest request = new SearchRequest(getActivity(), from, to, when);
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
	
	private void notifyClicked()
	{
		notifyButton.setEnabled(false);
		
		if (from == null || to == null)
		{
			Toast.makeText(getActivity(), R.string.notify_missing_loc, Toast.LENGTH_SHORT).show();
			notifyButton.setEnabled(true);
			return;
		}
		
		if (notificationEnabled)
		{
			Handler handler = new Handler() 
			{
				@Override
				public void handleMessage(Message msg) 
				{
					super.handleMessage(msg);
					if (msg.what == NotificationManager.REGISTRATION_SUCCESS)
					{
						notificationEnabled = false;
						updateNotifyGraphic();
						getSherlockActivity().supportInvalidateOptionsMenu();
					}
					
					notifyButton.setEnabled(true);
				}
			};
			
			NotificationManager.getInstance(getActivity().getApplicationContext()).disableNotification(getActivity(), from, to, when, handler);
		}
		else
		{
			Handler handler = new Handler() 
			{
				@Override
				public void handleMessage(Message msg) 
				{
					super.handleMessage(msg);
					if (msg.what == NotificationManager.REGISTRATION_SUCCESS)
					{
						notificationEnabled = true;
						updateNotifyGraphic();
					}
					
					notifyButton.setEnabled(true);
				}
			};
			
			NotificationManager.getInstance(getActivity().getApplicationContext()).enableNotification(getActivity(), from, to, when, handler);
		}
	}
}
