package org.prevoz.android.add_ride;

import org.prevoz.android.R;
import org.prevoz.android.util.LocaleUtil;

import android.content.Context;

public class PeopleSpinnerObject
{
	private Context context;
	private int number;

	public PeopleSpinnerObject(Context context, int number)
	{
		this.context = context;
		this.number = number;
	}

	public int getNumber()
	{
		return number;
	}

	public String toString()
	{
		return number
				+ " "
				+ LocaleUtil.getStringNumberForm(context.getResources(), R.array.people_tags, number);
	}
}
