package org.prevoz.android;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class CityListAdapter extends ArrayAdapter<City> 
{
	private Activity context;
	
	public CityListAdapter(Context context, int textViewResourceId, List<City> objects) 
	{
		super(context, textViewResourceId, objects);
		this.context = (Activity) context;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) 
	{
		View row = null;
		
		if (convertView == null)
		{
			LayoutInflater inflater = context.getLayoutInflater();
			row = inflater.inflate(R.layout.city_item, parent, false);
		}
		else
		{
			row = convertView;
		}
		
		TextView mainLine = (TextView) row.findViewById(R.id.city_name);
		TextView bottomLine = (TextView) row.findViewById(R.id.city_country);
		
		City city = getItem(position);
		
		mainLine.setText(city.getDisplayName());
		
		if (city.getCountryCode().equals("SI"))
		{
			bottomLine.setVisibility(View.GONE);
		}
		else
		{
			bottomLine.setVisibility(View.VISIBLE);
			bottomLine.setText(city.getCountryCode());
		}
		
		return row;
	}

}
