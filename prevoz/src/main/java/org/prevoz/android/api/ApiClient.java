package org.prevoz.android.api;

import android.os.Build;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import org.prevoz.android.PrevozApplication;
import org.prevoz.android.model.Bookmark;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.format.DateTimeParseException;

import java.io.IOException;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.schedulers.Schedulers;

public class ApiClient
{
    public static final String BASE_URL = "https://prevoz.org";

    private static final Retrofit retrofit;
    private static String bearer = null;

    static
    {
        Gson gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .registerTypeAdapter(ZonedDateTime.class, new Iso8601ZonedCalendarAdapter())
                .registerTypeAdapter(Bookmark.class, new BookmarkAdapter())
                .create();

        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        GsonConverterFactory gsonFactory = GsonConverterFactory.create(gson);
        OkHttpClient client = new OkHttpClient.Builder()
                                              .addInterceptor(new CookieSetterInterceptor())
                                              .addNetworkInterceptor(interceptor)
                                              .build();

        retrofit = new Retrofit.Builder()
                               .baseUrl(BASE_URL)
                               .addConverterFactory(gsonFactory)
                               .addCallAdapterFactory(RxJavaCallAdapterFactory.createWithScheduler(Schedulers.io()))
                               .client(client)
                               .build();
    }

    public static PrevozApi getAdapter()
    {
        return retrofit.create(PrevozApi.class);
    }

    public static void setBearer(String bearer)
    {
        ApiClient.bearer = bearer;
    }

    private static class Iso8601ZonedCalendarAdapter extends TypeAdapter<ZonedDateTime>
    {
        private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
        private static final DateTimeFormatter outFormatter = DateTimeFormatter.ISO_DATE;

        @Override
        public void write(JsonWriter out, ZonedDateTime value) throws IOException
        {
            if (value == null) {
                out.nullValue();
            } else {
                out.value(outFormatter.format(value));
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

    private static class CookieSetterInterceptor implements Interceptor
    {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            Request.Builder builder = request.newBuilder()
                                             .header("User-Agent", String.format("Prevoz/%d Android/%d", PrevozApplication.VERSION, Build.VERSION.SDK_INT));

            if (bearer != null) {
                builder.header("Authorization", String.format("Bearer %s", bearer));
                builder.header("WWW-Authenticate", "Bearer realm=\"api\"");
                // Server caches requests too enthusiasticly so append this to parameter list
                HttpUrl url = request.url().newBuilder().addQueryParameter("nocache", String.valueOf(System.currentTimeMillis())).build();
                builder.url(url);
            }

            return chain.proceed(builder.build());
        }
    }
}
