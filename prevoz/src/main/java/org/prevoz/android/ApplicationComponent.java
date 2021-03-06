package org.prevoz.android;

import android.content.Context;
import android.support.annotation.NonNull;

import org.jetbrains.annotations.NotNull;
import org.prevoz.android.auth.AuthenticationModule;
import org.prevoz.android.auth.AuthenticationUtils;
import org.prevoz.android.model.CityNameTextValidator;
import org.prevoz.android.myrides.MyRidesFragment;
import org.prevoz.android.myrides.MyRidesPresenter;
import org.prevoz.android.push.PushFragment;
import org.prevoz.android.push.PushManager;
import org.prevoz.android.push.PushModule;
import org.prevoz.android.push.PushPresenter;
import org.prevoz.android.push.PushReceiverService;
import org.prevoz.android.ride.RideInfoActivity;
import org.prevoz.android.ride.RideInfoPresenter;
import org.prevoz.android.search.SearchFormPresenter;
import org.prevoz.android.search.SearchFragment;
import org.prevoz.android.search.SearchResultsFragment;
import org.prevoz.android.search.SearchResultsPresenter;
import org.prevoz.android.util.PrevozActivity;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {ApplicationModule.class, AuthenticationModule.class, PushModule.class})
public interface ApplicationComponent {
    void inject(@NonNull PrevozApplication prevozApplication);
    void inject(@NonNull PrevozActivity activity);
    void inject(@NonNull CityNameTextValidator validator);
    void inject(@NonNull SearchFormPresenter searchFormPresenter);
    void inject(@NonNull SearchFragment searchFragment);
    void inject(@NonNull SearchResultsPresenter searchResultsPresenter);
    void inject(@NotNull SearchResultsFragment searchResultsFragment);
    void inject(@NonNull MyRidesPresenter myRidesFragment);
    void inject(@NonNull MyRidesFragment myRidesFragment);
    void inject(@NonNull PushPresenter pushPresenter);
    void inject(@NotNull PushFragment pushFragment);
    void inject(@NonNull PushReceiverService receiverService);
    void inject(@NonNull RideInfoActivity activity);
    void inject(@NonNull RideInfoPresenter rideInfoPresenter);

    Context context();

    AuthenticationUtils authUtils();
    PushManager pushManager();
}
