package org.prevoz.android.push;

import android.content.Context;

import org.prevoz.android.ApplicationModule;
import org.prevoz.android.model.PrevozDatabase;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module(includes = ApplicationModule.class)
public class PushModule {

    @Provides
    @Singleton
    public PushManager providePushManager(Context context, PrevozDatabase database) {
        return new PushManager(context, database);
    }

}
