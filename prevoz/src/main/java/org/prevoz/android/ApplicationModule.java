package org.prevoz.android;

import android.content.Context;

import org.prevoz.android.model.PrevozDatabase;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class ApplicationModule {

    private final Context application;
    private final PrevozDatabase database;

    public ApplicationModule(PrevozApplication application) {
        this.application = application;
        this.database = new PrevozDatabase(application);
    }

    @Provides @Singleton
    Context provideApplicationContext() {
        return application;
    }

    @Provides @Singleton
    PrevozDatabase provideDatabase() { return database; }

}
