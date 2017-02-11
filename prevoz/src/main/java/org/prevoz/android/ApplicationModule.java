package org.prevoz.android;

import android.content.Context;
import android.net.ConnectivityManager;
import android.support.annotation.NonNull;

import org.prevoz.android.model.PrevozDatabase;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class ApplicationModule {

    @NonNull private final Context application;
    @NonNull private final PrevozDatabase database;

    public ApplicationModule(@NonNull PrevozApplication application) {
        this.application = application;
        this.database = new PrevozDatabase(application);
    }

    @Provides @Singleton
    Context provideApplicationContext() {
        return application;
    }

    @Provides @Singleton
    PrevozDatabase provideDatabase() { return database; }

    @Provides @Singleton
    ConnectivityManager provideConnectivityManager() { return (ConnectivityManager) application.getSystemService(Context.CONNECTIVITY_SERVICE); }

}
