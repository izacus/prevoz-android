package org.prevoz.android.model;

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

    public Route(City from, City to)
    {
        this.from = from == null || from.getDisplayName().length() == 0 ? null : from;
        this.to = to == null || to.getDisplayName().length() == 0 ? null : to;
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
}
