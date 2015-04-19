package org.prevoz.android.auth;

import android.content.Context;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class AuthenticationModule {

    @Provides @Singleton
    public AuthenticationUtils provideAuthenticationUtils(Context context) {
        return new AuthenticationUtils(context);
    }

}
