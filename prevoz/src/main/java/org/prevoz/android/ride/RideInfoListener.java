package org.prevoz.android.ride;

import org.prevoz.android.api.rest.RestRide;

public interface RideInfoListener
{
    void onLeftButtonClicked(RestRide r);

    void onRightButtonClicked(RestRide r);
}
