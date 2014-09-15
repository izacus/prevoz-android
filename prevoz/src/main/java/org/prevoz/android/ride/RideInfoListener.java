package org.prevoz.android.ride;

import org.prevoz.android.api.rest.RestRide;

public interface RideInfoListener
{
    public void onLeftButtonClicked(RestRide r);

    public void onRightButtonClicked(RestRide r);
}
