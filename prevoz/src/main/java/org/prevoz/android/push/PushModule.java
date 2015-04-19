package org.prevoz.android.push;

import android.content.Context;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class PushModule {

    @Provides
    @Singleton
    public PushManager providePushManager(Context context) {
        return new PushManager(context);
    }

}
