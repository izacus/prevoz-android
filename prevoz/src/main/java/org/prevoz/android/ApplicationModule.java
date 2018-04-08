package org.prevoz.android;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.support.annotation.NonNull;

import org.prevoz.android.model.CityNameTextValidator;
import org.prevoz.android.model.PrevozDatabase;
import org.prevoz.android.util.LocaleUtil;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class ApplicationModule {

    @NonNull private final Context application;
    @NonNull private final PrevozDatabase database;
    @NonNull private final LocaleUtil localeUtil;

    public ApplicationModule(@NonNull PrevozApplication application) {
        this.application = application;
        this.database = new PrevozDatabase(application);
        this.localeUtil = new LocaleUtil(application.getResources(), database);
    }

    @Provides @Singleton
    Context provideApplicationContext() {
        return application;
    }

    @Provides @Singleton
    PrevozDatabase provideDatabase() { return database; }

    @Provides @Singleton
    LocaleUtil provideLocaleUtil() { return localeUtil; }

    @Provides @Singleton
    CityNameTextValidator provideCityNameTextValidator() { return new CityNameTextValidator(localeUtil, database); }

    @Provides @Singleton
    ConnectivityManager provideConnectivityManager() { return (ConnectivityManager) application.getSystemService(Context.CONNECTIVITY_SERVICE); }

    @Provides @Singleton
    PackageManager providePackageManager() { return application.getPackageManager(); }
}
