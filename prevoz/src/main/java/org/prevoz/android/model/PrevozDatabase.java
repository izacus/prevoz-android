package org.prevoz.android.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.AbstractCursor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import com.pushtorefresh.storio.sqlite.SQLiteTypeMapping;
import com.pushtorefresh.storio.sqlite.StorIOSQLite;
import com.pushtorefresh.storio.sqlite.impl.DefaultStorIOSQLite;
import com.pushtorefresh.storio.sqlite.queries.DeleteQuery;
import com.pushtorefresh.storio.sqlite.queries.Query;

import org.prevoz.android.R;
import org.prevoz.android.provider.Country;
import org.prevoz.android.provider.CountryStorIOSQLiteDeleteResolver;
import org.prevoz.android.provider.CountryStorIOSQLiteGetResolver;
import org.prevoz.android.provider.CountryStorIOSQLitePutResolver;
import org.prevoz.android.provider.Location;
import org.prevoz.android.provider.LocationStorIOSQLiteDeleteResolver;
import org.prevoz.android.provider.LocationStorIOSQLiteGetResolver;
import org.prevoz.android.provider.LocationStorIOSQLitePutResolver;
import org.prevoz.android.provider.Notification;
import org.prevoz.android.provider.NotificationStorIOSQLiteDeleteResolver;
import org.prevoz.android.provider.NotificationStorIOSQLiteGetResolver;
import org.prevoz.android.provider.NotificationStorIOSQLitePutResolver;
import org.prevoz.android.provider.SearchHistoryItem;
import org.prevoz.android.provider.SearchHistoryItemStorIOSQLiteDeleteResolver;
import org.prevoz.android.provider.SearchHistoryItemStorIOSQLiteGetResolver;
import org.prevoz.android.provider.SearchHistoryItemStorIOSQLitePutResolver;
import org.prevoz.android.util.FileUtil;
import org.prevoz.android.util.LocaleUtil;
import org.threeten.bp.LocalDate;

import java.util.List;
import java.util.concurrent.Callable;

import rx.Observable;
import rx.Single;
import rx.schedulers.Schedulers;
import rx.subjects.ReplaySubject;

public class PrevozDatabase {
    private static final String LOG_TAG = "Prevoz.Database";
    private static final String PREF_CONTENT_DB_VERSION = "content_db_ver";
    private static final String DATABASE_NAME = "dataprovider"; // For legacy reasons.

    private static final int DATABASE_VERSION = 15;
    private static final int CONTENT_DB_VERSION = 3;    // This is the version of actual data in content database.

    private final Context context;

    // This ensures that the database is ready when other operations work.
    private final ReplaySubject<StorIOSQLite> databasePrepared = ReplaySubject.create(1);

    public PrevozDatabase(Context context) {
        this.context = context;
        // Initialize database

        StorIOSQLite sqlite = DefaultStorIOSQLite.builder()
                 .sqliteOpenHelper(new PrevozDatabaseOpenHelper(context))
                 .addTypeMapping(Country.class, SQLiteTypeMapping.<Country>builder()
                    .putResolver(new CountryStorIOSQLitePutResolver())
                    .getResolver(new CountryStorIOSQLiteGetResolver())
                    .deleteResolver(new CountryStorIOSQLiteDeleteResolver())
                    .build())
                .addTypeMapping(Location.class, SQLiteTypeMapping.<Location>builder()
                        .putResolver(new LocationStorIOSQLitePutResolver())
                        .getResolver(new LocationStorIOSQLiteGetResolver())
                        .deleteResolver(new LocationStorIOSQLiteDeleteResolver())
                        .build())
                .addTypeMapping(Notification.class, SQLiteTypeMapping.<Notification>builder()
                        .putResolver(new NotificationStorIOSQLitePutResolver())
                        .getResolver(new NotificationStorIOSQLiteGetResolver())
                        .deleteResolver(new NotificationStorIOSQLiteDeleteResolver())
                        .build())
                .addTypeMapping(SearchHistoryItem.class, SQLiteTypeMapping.<SearchHistoryItem>builder()
                        .putResolver(new SearchHistoryItemStorIOSQLitePutResolver())
                        .getResolver(new SearchHistoryItemStorIOSQLiteGetResolver())
                        .deleteResolver(new SearchHistoryItemStorIOSQLiteDeleteResolver())
                        .build())
                .build();

        checkUpdateContentDatabase(sqlite);
    }

    private void checkUpdateContentDatabase(final StorIOSQLite sqlite) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (prefs.getInt(PREF_CONTENT_DB_VERSION, -1) == CONTENT_DB_VERSION) {
            databasePrepared.onNext(sqlite);
            databasePrepared.onCompleted();
            return;
        }

        Log.d(LOG_TAG, "Loading initial database...");
        Observable.fromCallable((Callable<Void>) () -> {

            sqlite.delete().byQuery(
                    DeleteQuery.builder().table(Country.TABLE).build()
            ).prepare().executeAsBlocking();
            sqlite.delete().byQuery(
                    DeleteQuery.builder().table(Location.TABLE).build()
            ).prepare().executeAsBlocking();

            // Cities
            {
                sqlite.internal().beginTransaction();
                String[] lines = FileUtil.readLines(context, R.raw.locations);
                for (String line : lines)
                {
                    String[] tokens = line.split(",");
                    Location location = new Location(tokens[1], tokens[2], tokens[3], Float.parseFloat(tokens[4]), Float.parseFloat(tokens[5]),  Integer.parseInt(tokens[0]));
                    sqlite.put().object(location).prepare().executeAsBlocking();
                }
                sqlite.internal().setTransactionSuccessful();
                sqlite.internal().endTransaction();
            }

            // Countries
            {
                String[] lines = FileUtil.readLines(context, R.raw.countries);
                sqlite.internal().beginTransaction();
                for (String line : lines) {
                    String[] tokens = line.split(",");
                    Country country = new Country(tokens[0], tokens[2], tokens[1]);
                    sqlite.put().object(country).prepare().executeAsBlocking();
                }
                sqlite.internal().setTransactionSuccessful();
                sqlite.internal().endTransaction();
            }

            prefs.edit().putInt(PREF_CONTENT_DB_VERSION, CONTENT_DB_VERSION).apply();
            databasePrepared.onNext(sqlite);
            databasePrepared.onCompleted();
            return null;
        })
        .subscribeOn(Schedulers.io())
        .subscribe(aVoid -> {},
                   throwable -> {
                       Log.e(LOG_TAG, "Failed to load fixture database!", throwable);
                       databasePrepared.onError(throwable);
                   },
                   () -> Log.d(LOG_TAG, "Database loaded."));
    }

    public Single<String> getLocalCityName(@NonNull final String city) {
        return databasePrepared.flatMap(storIOSQLite -> storIOSQLite.get()
                    .cursor()
                    .withQuery(Query.builder()
                                .table(Location.TABLE)
                                .columns(Location.NAME)
                                .where(Location.NAME_ASCII + " = ? OR " + Location.NAME + " = ?")
                                .whereArgs(city, city)
                                .build())
                    .prepare()
                    .createObservable()
                    .first())
                    .flatMap(cursor -> {
                        int idx = cursor.getColumnIndex(Location.NAME);
                        if (cursor.moveToNext()) {
                            return Observable.just(cursor.getString(idx));
                        } else {
                            return Observable.just(city);
                        }
                    })
                    .subscribeOn(Schedulers.io())
                    .singleOrDefault("")
                    .toSingle();
    }

    public Single<String> getLocalCountryName(@NonNull String locale, @NonNull String countryCode) {
        final String actualLocale = locale.equalsIgnoreCase("sl") ? "sl-si" : locale;

        return databasePrepared.flatMap(storIOSQLite -> storIOSQLite.get()
                .listOfObjects(Country.class)
                .withQuery(Query.builder()
                        .table(Country.TABLE)
                        .where(Country.LANGUAGE + " = ? AND " + Country.COUNTRY_CODE + " = ?")
                        .whereArgs(actualLocale, countryCode)
                        .build())
                .prepare()
                .createObservable()
                .first())
                .flatMap(Observable::from)
                .map(Country::getName)
                .subscribeOn(Schedulers.io())
                .singleOrDefault("")
                .toSingle();
    }

    public Single<Boolean> cityExists(@NonNull final String city) {
        return databasePrepared.flatMap(storIOSQLite -> storIOSQLite.get()
                .numberOfResults()
                .withQuery(Query.builder()
                            .table(Location.TABLE)
                            .where(Location.NAME + " = ?")
                            .whereArgs(city)
                            .build())
                .prepare()
                .createObservable()
                .first())
                .map(num -> num > 0)
                .subscribeOn(Schedulers.io())
                .toSingle();
    }

    public Cursor cityCursor(@Nullable String constraint, @Nullable String country) {
        // If database isn't prepared, return empty cursor.
        if (!databasePrepared.hasAnyValue()) return new EmptyCursor();
        StorIOSQLite sqlite = databasePrepared.getValue();

        if (TextUtils.isEmpty(constraint)) {
            return sqlite.get().cursor()
                                .withQuery(
                                     Query.builder().table(Location.TABLE).build())
                                .prepare()
                                .executeAsBlocking();
        }

        // Unforunately arguments do not work with LIKE filter
        String queryString = Location.NAME + " LIKE '" + constraint + "%' OR " + Location.NAME_ASCII + " LIKE '" + constraint + "%'";
        Query.CompleteBuilder builder = Query.builder().table(Location.TABLE);
        builder.table(Location.TABLE);

        if (country != null)
        {
            queryString = Location.COUNTRY + " = ? AND " + queryString;
            builder.whereArgs(country);
        }
        builder.where(queryString);

        return sqlite.get()
                     .cursor()
                     .withQuery(builder
                                .orderBy(Location.SORT_NUMBER + " DESC")
                                .build())
                     .prepare()
                     .executeAsBlocking();
    }

    public void addSearchToHistory(@Nullable City from, @Nullable City to, @NonNull LocalDate date) {
        String fcity = from == null ? "" : from.getDisplayName();
        String fcountry = from == null ? "" : from.getCountryCode();
        String tcity = to == null ? "" : to.getDisplayName();
        String tcountry = to == null ? "" : to.getCountryCode();
        long epoch = localDateToEpoch(date);

        // First look for an existing item
        databasePrepared.flatMap(storIOSQLite -> storIOSQLite.get()
                            .listOfObjects(SearchHistoryItem.class)
                            .withQuery(Query.builder()
                                        .table(SearchHistoryItem.TABLE)
                                        .where(SearchHistoryItem.FROM_CITY + " = ? AND " + SearchHistoryItem.FROM_COUNTRY + " = ? AND " +
                                               SearchHistoryItem.TO_CITY + " = ? AND " + SearchHistoryItem.TO_COUNTRY + " = ? ")
                                        .whereArgs(fcity, fcountry, tcity, tcountry)
                                        .build())
                            .prepare()
                            .createObservable()
                            .first())
                            .subscribeOn(Schedulers.io())
                            .flatMap(Observable::from)
        // Grab the existing item or create a new one and insert it back in with new date.
                            .firstOrDefault(new SearchHistoryItem(fcity, fcountry, tcity, tcountry, epoch))
                            .map(searchHistoryItem -> {
                                searchHistoryItem.setDate(epoch);
                                return searchHistoryItem;
                             })
                            .zipWith(databasePrepared, Pair::new)
                            .flatMap(pair -> pair.second.put().object(pair.first).prepare().createObservable().first())
                            .subscribeOn(Schedulers.io())
                            .subscribe(r -> {},
                                       e -> Log.e(LOG_TAG, "Failed to instert search history item!", e));
    }

    public Single<List<Route>> getLastSearches(int count) {
        return databasePrepared.flatMap(storIOSQLite -> storIOSQLite.get()
                                    .listOfObjects(SearchHistoryItem.class)
                                    .withQuery(Query.builder()
                                        .table(SearchHistoryItem.TABLE)
                                        .orderBy(SearchHistoryItem.SEARCH_DATE + " ASC")
                                        .limit(count)
                                        .build())
                .prepare()
                .createObservable()
                .first())
                .flatMap(Observable::from)
                .map(item -> new Route(item.getFrom(), item.getTo()))
                .toList()
                .subscribeOn(Schedulers.io())
                .toSingle();
    }

    public void deleteOldHistoryEntries(int min) {
        // Get count of items from table, then delete the difference.
        Observable<SearchHistoryItem> historyItems =
                databasePrepared.flatMap(storIOSQLite -> storIOSQLite.get()
                            .listOfObjects(SearchHistoryItem.class)
                            .withQuery(Query.builder()
                                        .table(SearchHistoryItem.TABLE)
                                        .orderBy(SearchHistoryItem.SEARCH_DATE + " DESC")
                                        .build())
                            .prepare()
                            .createObservable()
                            .first())
                            .flatMap(Observable::from);

        historyItems.count()
                    .flatMap(totalCount -> historyItems.take(Math.max(totalCount - min, 0)))
                    .toList()
                    .zipWith(databasePrepared, Pair::new)
                    .flatMap(pair -> pair.second.delete()
                                    .objects(pair.first)
                                    .useTransaction(true)
                                    .prepare()
                                    .createObservable()
                                    .first())
                    .subscribeOn(Schedulers.io())
                    .subscribe(a -> {},
                               e -> Log.e(LOG_TAG, "Failed to truncate search table!", e));
    }

    public Single<Boolean> isSubscribedForNotification(@NonNull City from, @NonNull City to, LocalDate date) {
        long epoch = localDateToEpoch(date);

        return databasePrepared.flatMap(storIOSQLite -> storIOSQLite.get()
                        .numberOfResults()
                        .withQuery(Query.builder()
                                    .table(Notification.TABLE)
                                    .where( Notification.FROM_CITY + " = ? AND " + Notification.FROM_COUNTRY + " = ? AND " +
                                            Notification.TO_CITY + " = ? AND " + Notification.TO_COUNTRY + " = ? AND " +
                                            Notification.DATE + " = ?")
                                    .whereArgs(from.getDisplayName(), from.getCountryCode(),
                                               to.getDisplayName(), to.getCountryCode(),
                                               epoch)
                                    .build())
                        .prepare()
                        .createObservable()
                        .first())
                        .map(count -> count > 0)
                        .subscribeOn(Schedulers.io())
                        .toSingle();
    }

    public Observable<List<Notification>> getNotificationSubscriptions() {
        return databasePrepared.flatMap(storIOSQLite -> storIOSQLite.get()
                        .listOfObjects(Notification.class)
                        .withQuery(Query.builder().table(Notification.TABLE).build())
                        .prepare().createObservable().first())
                        .subscribeOn(Schedulers.io());
    }

    public Observable<Boolean> addNotificationSubscription(@NonNull City from, @NonNull City to, @NonNull LocalDate date) {
        long epoch = localDateToEpoch(date);
        Notification notification = new Notification(from.getDisplayName(),
                                                     from.getCountryCode(),
                                                     to.getDisplayName(),
                                                     to.getCountryCode(),
                                                     epoch);
        return databasePrepared.flatMap(storIOSQLite -> storIOSQLite.put()
                                                        .object(notification)
                                                        .prepare()
                                                        .createObservable()
                                                        .first())
                                .subscribeOn(Schedulers.io())
                                .map(v -> true);
    }

    public Observable<Boolean> deleteNotificationSubscription(@NonNull City from, @NonNull City to, @NonNull LocalDate date) {
        long epoch = localDateToEpoch(date);
        return databasePrepared.flatMap(storIOSQLite -> storIOSQLite.delete()
                                        .byQuery(DeleteQuery.builder()
                                                    .table(Notification.TABLE)
                                                    .where(Notification.FROM_CITY + " = ? AND " + Notification.FROM_COUNTRY + " = ? AND " +
                                                           Notification.TO_CITY + " = ? AND " + Notification.TO_COUNTRY + " = ? AND " +
                                                           Notification.DATE + " = ?")
                                                    .whereArgs(from.getDisplayName(), from.getCountryCode(),
                                                               to.getDisplayName(), to.getCountryCode(), epoch)
                                                    .build())
                                        .prepare()
                                        .createObservable()
                                        .first())
                                .subscribeOn(Schedulers.io())
                                .map(v -> true);
    }

    public void pruneOldNotifications() {
        long today = localDateToEpoch(LocalDate.now());
        databasePrepared.flatMap(storIOSQLite -> storIOSQLite.delete()
                                    .byQuery(DeleteQuery.builder()
                                                .table(Notification.TABLE)
                                                .where(Notification.DATE + " < ?")
                                                .whereArgs(today)
                                    .build())
                                    .prepare().createObservable())
                        .subscribeOn(Schedulers.io())
                        .subscribe(v -> {},
                                   e -> Log.e(LOG_TAG, "Failed to prune notifications.", e));
    }

    private static final class EmptyCursor extends AbstractCursor {

        @Override
        public int getCount() {
            return 0;
        }

        @Override
        public String[] getColumnNames() {
            return new String[] { "_id", Location.NAME };
        }

        @Override
        public String getString(int column) {
            return null;
        }

        @Override
        public short getShort(int column) {
            return 0;
        }

        @Override
        public int getInt(int column) {
            return 0;
        }

        @Override
        public long getLong(int column) {
            return 0;
        }

        @Override
        public float getFloat(int column) {
            return 0;
        }

        @Override
        public double getDouble(int column) {
            return 0;
        }

        @Override
        public boolean isNull(int column) {
            return false;
        }
    }


    private long localDateToEpoch(@NonNull LocalDate date) {
        return date.atStartOfDay(LocaleUtil.getLocalTimezone()).toEpochSecond() * 1000;
    }

    private static final class PrevozDatabaseOpenHelper extends SQLiteOpenHelper {

        public PrevozDatabaseOpenHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE country (\n" +
                    "  country_code text,\n" +
                    "  lang text,\n" +
                    "  name text,\n" +
                    "  _id integer PRIMARY KEY AUTOINCREMENT\n" +
                    ");");

            db.execSQL("CREATE TABLE location (\n" +
                    "  country text,\n" +
                    "  lat float,\n" +
                    "  lng float,\n" +
                    "  name text,\n" +
                    "  name_ascii text,\n" +
                    "  sort integer,\n" +
                    "  _id integer PRIMARY KEY AUTOINCREMENT\n" +
                    ");");

            db.execSQL("CREATE TABLE notification (\n" +
                    "  date integer NOT NULL,\n" +
                    "  l_from text NOT NULL,\n" +
                    "  c_from text NOT NULL,\n" +
                    "  reg_date integer NOT NULL DEFAULT((julianday('now') - 2440587.5)*86400000),\n" +
                    "  l_to text NOT NULL,\n" +
                    "  c_to text NOT NULL,\n" +
                    "  _id integer PRIMARY KEY AUTOINCREMENT\n" +
                    ");");

            db.execSQL("CREATE TABLE searchhistoryitem (\n" +
                    "  date integer NOT NULL,\n" +
                    "  l_from text,\n" +
                    "  c_from text,\n" +
                    "  l_to text,\n" +
                    "  c_to text,\n" +
                    "  _id integer PRIMARY KEY AUTOINCREMENT\n" +
                    ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS country");
            db.execSQL("DROP TABLE IF EXISTS location");
            db.execSQL("DROP TABLE IF EXISTS notification");
            db.execSQL("DROP TABLE IF EXISTS searchhistoryitem");
            onCreate(db);
        }
    }
}
