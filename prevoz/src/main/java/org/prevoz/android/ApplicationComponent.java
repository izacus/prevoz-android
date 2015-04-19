package org.prevoz.android;

import android.content.Context;

import org.prevoz.android.auth.AuthenticationModule;
import org.prevoz.android.auth.AuthenticationUtils;
import org.prevoz.android.push.PushManager;
import org.prevoz.android.push.PushModule;
import org.prevoz.android.ride.RideInfoFragment;
import org.prevoz.android.util.PrevozActivity;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {ApplicationModule.class, AuthenticationModule.class, PushModule.class})
public interface ApplicationComponent {
    void inject(PrevozApplication prevozApplication);
    void inject(PrevozActivity activity);
    void inject(PrevozFragment fragment);
    void inject(RideInfoFragment fragment);

    Context context();

    AuthenticationUtils authUtils();
    PushManager pushManager();

}
