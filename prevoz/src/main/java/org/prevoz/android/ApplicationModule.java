package org.prevoz.android;

import android.content.Context;

import org.prevoz.android.api.ApiClient;
import org.prevoz.android.api.PrevozApi;
import org.prevoz.android.model.PrevozDatabase;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class ApplicationModule {

    private final Context application;
    private final PrevozDatabase database;
    private final PrevozApi api;

    public ApplicationModule(PrevozApplication application) {
        this.application = application;
        this.database = new PrevozDatabase(application);
        this.api = ApiClient.getAdapter();
    }

    @Provides @Singleton
    Context provideApplicationContext() {
        return application;
    }

    @Provides @Singleton
    PrevozDatabase provideDatabase() { return database; }

    @Provides @Singleton
    PrevozApi provideApi() { return api; }
}
