package org.prevoz.android.api;

import android.os.Build;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.prevoz.android.PrevozApplication_;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.converter.GsonConverter;

public class ApiClient
{
    private static RestAdapter adapter = null;
    private static String bearer = null;

    static
    {
        Gson gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .registerTypeAdapter(Date.class, new Iso8601DateAdapter())
                .create();

        adapter = new RestAdapter.Builder()
                                 .setServer("https://prevoz.org/api")
                                 .setConverter(new GsonConverter(gson))
                                 .setRequestInterceptor(new CookieSetterInterceptor())
                                 .setLogLevel(RestAdapter.LogLevel.FULL)
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

    private static class Iso8601DateAdapter extends TypeAdapter<Date>
    {
        private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz");

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
