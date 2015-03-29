package org.prevoz.android.api;

import android.os.Build;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.squareup.okhttp.OkHttpClient;

import org.prevoz.android.PrevozApplication_;
import org.prevoz.android.model.Bookmark;
import org.prevoz.android.util.LocaleUtil;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.client.OkClient;
import retrofit.converter.GsonConverter;

public class ApiClient
{
    public static final String BASE_URL = "https://prevoz.org";

    private static final RestAdapter adapter;
    private static String bearer = null;

    static
    {
        Gson gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .registerTypeAdapter(Date.class, new Iso8601DateAdapter())
                .registerTypeAdapter(Calendar.class, new Iso8601CalendarAdapter())
                .registerTypeAdapter(GregorianCalendar.class, new Iso8601CalendarAdapter())
                .registerTypeAdapter(Bookmark.class, new BookmarkAdapter())
                .create();

        adapter = new RestAdapter.Builder()
                                 .setEndpoint(BASE_URL)
                                 .setConverter(new GsonConverter(gson))
                                 .setRequestInterceptor(new CookieSetterInterceptor())
                                 .setClient(new OkClient(new OkHttpClient()))
                                 .build();
    }

    public static PrevozApi getAdapter()
    {
        return adapter.create(PrevozApi.class);
    }

    public static void setBearer(String bearer)
    {
        ApiClient.bearer = bearer;
    }

    private static class Iso8601CalendarAdapter extends TypeAdapter<Calendar>
    {
        private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz", Locale.US);

        static
        {
            sdf.setTimeZone(LocaleUtil.getLocalTimezone());
        }

        @Override
        public void write(JsonWriter out, Calendar value) throws IOException
        {
            if (value == null) {
                out.nullValue();
            } else {
                out.value(sdf.format(value.getTime()));
            }
        }

        @Override
        public Calendar read(JsonReader in) throws IOException
        {
            try
            {
                Date time = sdf.parse(in.nextString());
                Calendar cal = Calendar.getInstance(LocaleUtil.getLocalTimezone());
                cal.setTime(time);
                return cal;
            }
            catch (ParseException e)
            {
                throw new IOException("Invalid date encountered: " + e.getMessage());
            }
        }
    }

    private static class Iso8601DateAdapter extends TypeAdapter<Date>
    {
        private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz", Locale.US);

        @Override
        public void write(JsonWriter out, Date value) throws IOException
        {
            out.value(sdf.format(value));
        }

        @Override
        public Date read(JsonReader in) throws IOException
        {
            try
            {
                return sdf.parse(in.nextString());
            }
            catch (ParseException e)
            {
                throw new IOException("Invalid date encountered: " + e.getMessage());
            }
        }
    }

    private static class BookmarkAdapter extends TypeAdapter<Bookmark>
    {
        @Override
        public void write(JsonWriter out, Bookmark value) throws IOException {
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
            requestFacade.addHeader("User-Agent", String.format("Prevoz/%d Android/%d", PrevozApplication_.VERSION, Build.VERSION.SDK_INT));

            if (bearer != null)
                requestFacade.addHeader("Authorization", String.format("Bearer %s", bearer));
        }
    }
}
