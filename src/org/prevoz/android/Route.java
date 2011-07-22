package org.prevoz.android;

/**
 * Carries basic route information
 * 
 * @author Jernej Virag
 * 
 */
public class Route
{
	private String from;
	private String to;
	private RideType type;

	public Route(String from, String to, RideType type)
	{
		super();
		this.from = from;
		this.to = to;
		this.type = type;
	}

	public String getFrom()
	{
		return from;
	}

	public String getTo()
	{
		return to;
	}

	@Override
	public String toString()
	{
		String fromText;
		String toText;
		
		if (from == null || from.length() == 0)
		{
			// TODO: Add resource
			fromText = "Vsi kraji";
		}
		else
		{
			fromText = from;
		}
		
		if (to == null || to.length() == 0)
		{
			// TODO: Add resource
			toText = "Vsi kraji";
		}
		else
		{
			toText = to;
		}
		
		return fromText + " - " + toText;
	}

	public RideType getType()
	{
		return type;
	}
}
