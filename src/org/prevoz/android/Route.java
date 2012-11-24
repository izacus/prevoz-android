package org.prevoz.android;

/**
 * Carries basic route information
 * 
 * @author Jernej Virag
 * 
 */
public class Route
{
	private City from;
	private City to;
	private RideType type;

	public Route(City from, City to, RideType type)
	{
		super();
		this.from = from == null || from.getDisplayName().length() == 0 ? null : from;
		this.to = to == null || to.getDisplayName().length() == 0 ? null : to;
		this.type = type;
	}

	public City getFrom()
	{
		return from;
	}

	public City getTo()
	{
		return to;
	}

	@Override
	public String toString()
	{
		String fromText;
		String toText;
		
		if (from == null)
		{
			// TODO: Add resource
			fromText = "Vsi kraji";
		}
		else
		{
			fromText = from.toString();
		}
		
		if (to == null)
		{
			// TODO: Add resource
			toText = "Vsi kraji";
		}
		else
		{
			toText = to.toString();
		}
		
		return fromText + " - " + toText;
	}

	public RideType getType()
	{
		return type;
	}
}
