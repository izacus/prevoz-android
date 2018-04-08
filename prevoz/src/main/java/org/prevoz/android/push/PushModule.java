package org.prevoz.android.push;

import android.content.Context;
import android.support.annotation.NonNull;

import org.prevoz.android.ApplicationModule;
import org.prevoz.android.model.PrevozDatabase;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module(includes = ApplicationModule.class)
public class PushModule {

    @Provides
    @Singleton
    public PushManager providePushManager(@NonNull Context application, @NonNull PrevozDatabase database) {
        return new PushManager(application, database);
    }

}
