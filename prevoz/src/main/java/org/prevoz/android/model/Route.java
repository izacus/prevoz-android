package org.prevoz.android.model;

import android.util.Log;

/**
 * Carries basic route information
 *
 * @author Jernej Virag
 *
 */
public class Route
{
    private final City from;
    private final City to;

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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Route route = (Route) o;
        boolean fromEqual = (from == null && route.from == null) || (from != null && from.equals(route.from));
        boolean toEqual = (to == null && route.to == null) || (to != null && to.equals(route.to));
        return fromEqual && toEqual;

    }

    @Override
    public int hashCode() {
        int result = from != null ? from.hashCode() : 0;
        result = 31 * result + (to != null ? to.hashCode() : 0);
        return result;
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
