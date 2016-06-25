package org.prevoz.android;

import org.prevoz.android.api.PrevozApi;
import org.prevoz.android.model.PrevozDatabase;
import org.prevoz.android.search.SearchPresenter;

import dagger.Module;
import dagger.Provides;

@Module
public class MVPModule {

    @Provides
    public SearchPresenter providePresenter(PrevozDatabase database, PrevozApi api) {
        return new SearchPresenter(database, api);
    }

}
