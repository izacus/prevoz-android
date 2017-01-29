package org.prevoz.android.api;

import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.squareup.okhttp.OkHttpClient;

import org.prevoz.android.BuildConfig;
import org.prevoz.android.PrevozApplication;
import org.prevoz.android.model.Bookmark;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.format.DateTimeParseException;

import java.io.IOException;
import java.util.GregorianCalendar;
import java.util.Locale;

import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.client.OkClient;
import retrofit.converter.GsonConverter;

public class ApiClient
{
    public static final String BASE_URL = "https://prevoz.org";

    @NonNull private static final RestAdapter adapter;
    @Nullable private static String bearer = null;

    static
    {
        Gson gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .registerTypeAdapter(ZonedDateTime.class, new Iso8601CalendarAdapter())
                .registerTypeAdapter(GregorianCalendar.class, new Iso8601CalendarAdapter())
                .registerTypeAdapter(Bookmark.class, new BookmarkAdapter())
                .create();


        OkHttpClient client = new OkHttpClient();
        adapter = new RestAdapter.Builder()
                                 .setEndpoint(BASE_URL)
                                 .setConverter(new GsonConverter(gson))
                                 .setLogLevel(BuildConfig.DEBUG ? RestAdapter.LogLevel.FULL : RestAdapter.LogLevel.NONE)
                                 .setRequestInterceptor(new CookieSetterInterceptor())
                                 .setClient(new OkClient(client))
                                 .build();
    }

    public static PrevozApi getAdapter()
    {
        return adapter.create(PrevozApi.class);
    }

    public static void setBearer(@Nullable String bearer)
    {
        ApiClient.bearer = bearer;
    }

    @Nullable public static String getBearer() {
        return bearer;
    }

    private static class Iso8601CalendarAdapter extends TypeAdapter<ZonedDateTime>
    {
        private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

        @Override
        public void write(JsonWriter out, ZonedDateTime value) throws IOException
        {
            if (value == null) {
                out.nullValue();
            } else {
                out.value(formatter.format(value));
            }
        }

        @Override
        public ZonedDateTime read(JsonReader in) throws IOException
        {
            try
            {
                return ZonedDateTime.parse(in.nextString(), formatter);
            }
            catch (DateTimeParseException e)
            {
                throw new IOException("Invalid date encountered: " + e.getMessage());
            }
        }
    }

    private static class BookmarkAdapter extends TypeAdapter<Bookmark>
    {
        @Override
        public void write(JsonWriter out, Bookmark value) throws IOException {
            if (value == null) {
                out.nullValue();
                return;
            }

            switch (value) {
                case GOING_WITH:
                    out.value("going_with");
                    break;
                case OUT_OF_SEATS:
                    out.value("out_of_seats");
                    break;
                case NOT_GOING_WITH:
                    out.value("not_going_with");
                    break;
                case BOOKMARK:
                    out.value("bookmark");
                default:
                    out.nullValue();
            }
        }

        @Override
        public Bookmark read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }

            String info = in.nextString();
            switch (info) {
                case "going_with":
                    return Bookmark.GOING_WITH;
                case "out_of_seats":
                    return Bookmark.OUT_OF_SEATS;
                case "not_going_with":
                    return Bookmark.NOT_GOING_WITH;
                case "bookmark":
                    return Bookmark.BOOKMARK;
            }

            return null;
        }
    }

    private static class CookieSetterInterceptor implements RequestInterceptor
    {
        @Override
        public void intercept(RequestFacade requestFacade)
        {
            requestFacade.addHeader("User-Agent", String.format(Locale.US, "Prevoz/%d Android/%d", PrevozApplication.VERSION, Build.VERSION.SDK_INT));

            if (bearer != null) {
                requestFacade.addHeader("Authorization", String.format("Bearer %s", bearer));
                requestFacade.addHeader("WWW-Authenticate", "Bearer realm=\"api\"");
            }
        }
    }
}
