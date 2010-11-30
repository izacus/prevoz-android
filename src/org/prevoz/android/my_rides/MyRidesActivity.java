package org.prevoz.android.my_rides;

import java.util.ArrayList;
import java.util.Calendar;

import org.prevoz.android.MainActivity;
import org.prevoz.android.R;
import org.prevoz.android.Route;
import org.prevoz.android.SectionedAdapter;
import org.prevoz.android.search.SearchActivity;
import org.prevoz.android.util.Database;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

public class MyRidesActivity extends ListActivity implements
		OnItemClickListener, OnCreateContextMenuListener, OnDateSetListener
{
	private static final int CONTEXT_DELETE = Menu.FIRST;
	private static final int DIALOG_DATEPICKER = 0;

	private SectionedAdapter listAdapter = null;

	private ArrayList<Route> favorites = null;
	private Route contextSelected = null;
	private Route selected = null;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		buildList();
	}

	private void buildList()
	{
		favorites = Database.getFavorites(this);

		listAdapter = getSectionedAdapter();

		if (favorites.size() > 0)
		{
			ArrayAdapter<Route> favsAdapter = new ArrayAdapter<Route>(this,
					R.layout.favorite_item, favorites)
			{

				@Override
				public View getView(int position, View convertView,
						ViewGroup parent)
				{
					Route route = this.getItem(position);

					View view = convertView;

					if (convertView == null)
					{
						view = (LinearLayout) getLayoutInflater().inflate(
								R.layout.favorite_item, null);
					}

					TextView routeText = (TextView) view
							.findViewById(R.id.favorite_route);
					routeText.setText(route.toString());

					TextView routeType = (TextView) view
							.findViewById(R.id.favorite_type);
					routeType.setText(route.getType().toString());

					return view;
				}
			};

			listAdapter.addSection(getString(R.string.favorites), favsAdapter);

			// Click callback
			getListView().setOnItemClickListener(this);

			// Context menu callback
			getListView().setOnCreateContextMenuListener(this);
		}
		else
		{
			String[] message = new String[] { getString(R.string.no_favorites) };
			listAdapter.addSection(getString(R.string.favorites),
					new ArrayAdapter<String>(this,
							android.R.layout.simple_list_item_1, message));
		}

		getListView().setAdapter(listAdapter);

	}

	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id)
	{
		ListView listView = (ListView) parent;
		Route selectedObj = (Route) listView.getAdapter().getItem(position);
		selected = selectedObj;

		showDialog(DIALOG_DATEPICKER);
	}

	@Override
	protected Dialog onCreateDialog(int id)
	{
		switch (id)
		{
		case DIALOG_DATEPICKER:
			Calendar cal = Calendar.getInstance();
			return new DatePickerDialog(this, this, cal.get(Calendar.YEAR),
					cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));

		default:
			break;
		}

		return super.onCreateDialog(id);
	}

	// Context menu for list view creation
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo)
	{
		super.onCreateContextMenu(menu, v, menuInfo);

		ListView listView = (ListView) v;
		AdapterContextMenuInfo adapterMenuInfo = (AdapterContextMenuInfo) menuInfo;
		Route selectedObj = (Route) listView.getAdapter().getItem(
				adapterMenuInfo.position);

		this.contextSelected = selectedObj;

		menu.setHeaderTitle(selectedObj.toString());
		menu.add(0, CONTEXT_DELETE, 0, getString(R.string.delete));
	}

	@Override
	public boolean onContextItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		case CONTEXT_DELETE:
			Database.deleteFavorite(this, contextSelected.getFrom(),
					contextSelected.getTo(), contextSelected.getType());
			buildList();
			break;
		default:
			Log.e(this.toString(), "Errorenous item selected in context menu.");
			break;
		}

		return false;
	}

	/**
	 * Used for favorites search dialog
	 */
	public void onDateSet(DatePicker view, int year, int monthOfYear,
			int dayOfMonth)
	{
		Calendar cal = Calendar.getInstance();
		cal.set(year, monthOfYear, dayOfMonth);

		// Switch to search tab
		MainActivity.getInstance().switchToSearch();

		// Send search request
		Intent intent = new Intent(SearchActivity.SEARCH_REQUEST);
		intent.putExtra(SearchActivity.SEARCH_TO, selected.getTo());
		intent.putExtra(SearchActivity.SEARCH_FROM, selected.getFrom());
		intent.putExtra(SearchActivity.SEARCH_DATE, cal.getTimeInMillis());
		sendBroadcast(intent);
	}

	private SectionedAdapter getSectionedAdapter()
	{
		return new SectionedAdapter()
		{

			@Override
			protected View getHeaderView(String caption, int index,
					View convertView, ViewGroup parent)
			{
				TextView result = (TextView) convertView;
				if (convertView == null)
				{
					result = (TextView) getLayoutInflater().inflate(
							R.layout.list_header, null);
				}
				result.setText(caption);
				return result;
			}

			@Override
			public View getView(int position, View convertView, ViewGroup parent)
			{
				return super.getView(position, convertView, parent);
			}

		};
	}
}
