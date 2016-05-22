package org.prevoz.android;

import android.content.Context;

import org.prevoz.android.auth.AuthenticationModule;
import org.prevoz.android.auth.AuthenticationUtils;
import org.prevoz.android.model.CityNameTextValidator;
import org.prevoz.android.push.PushManager;
import org.prevoz.android.push.PushModule;
import org.prevoz.android.push.PushReceiver;
import org.prevoz.android.ride.RideInfoActivity;
import org.prevoz.android.util.PrevozActivity;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {ApplicationModule.class, AuthenticationModule.class, PushModule.class, MVPModule.class})
public interface ApplicationComponent {
    void inject(PrevozApplication prevozApplication);
    void inject(PrevozActivity activity);
    void inject(PrevozFragment fragment);
    void inject(RideInfoActivity fragment);
    void inject(CityNameTextValidator validator);
    void inject(SearchActivity activity);

    Context context();

    AuthenticationUtils authUtils();
    PushManager pushManager();

    void inject(PushReceiver pushReceiver);
}
