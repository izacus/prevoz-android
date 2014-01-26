package org.prevoz.android.search;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.EFragment;
import com.googlecode.androidannotations.annotations.ViewById;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.view.ViewHelper;

import org.prevoz.android.R;
import org.prevoz.android.api.ApiClient;
import org.prevoz.android.api.rest.RestSearchRequest;
import org.prevoz.android.api.rest.RestSearchResults;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

import static com.nineoldandroids.view.ViewPropertyAnimator.animate;

@EFragment(R.layout.fragment_search_results)
public class SearchResultsFragment extends Fragment implements Callback<RestSearchResults>
{
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    public static final String PARAM_SEARCH_FROM = "SearchFrom";
    public static final String PARAM_SEARCH_TO = "SearchTo";
    public static final String PARAM_SEARCH_DATE = "SearchDate";

    @ViewById(R.id.search_results_list)
    protected ListView resultList;
    @ViewById(R.id.search_results_progress)
    protected View progressView;
    @ViewById(R.id.search_results_container)
    protected View resultsContainer;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        String from = args.getString(PARAM_SEARCH_FROM);
        String to = args.getString(PARAM_SEARCH_TO);
        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(args.getLong(PARAM_SEARCH_DATE));

        RestSearchRequest request = new RestSearchRequest(from, "SI", to, "SI", sdf.format(date.getTime()));
        ApiClient.getAdapter().search(request, this);
    }

    @AfterViews
    protected void afterViews()
    {
        ViewHelper.setAlpha(resultsContainer, 0.0f);
    }

    @Override
    public void success(RestSearchResults restSearchResults, Response response)
    {
        if (getActivity() == null) return;
        Log.d("Prevoz", "Response: " + response.getBody().toString());
        resultList.setAdapter(new SearchResultsAdapter(getActivity(), restSearchResults.results));
        setListViewHeightBasedOnChildren(resultList);
        resultList.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw()
            {
                resultList.getViewTreeObserver().removeOnPreDrawListener(this);

                ViewHelper.setTranslationY(resultsContainer, 300.0f);
                ObjectAnimator containerFlyUp = ObjectAnimator.ofFloat(resultsContainer, "translationY", 0.0f);
                ObjectAnimator containerFadeIn = ObjectAnimator.ofFloat(resultsContainer, "alpha", 0.00f, 1.0f, 1);
                ObjectAnimator loadingFadeOut = ObjectAnimator.ofFloat(progressView, "alpha", 1.0f, 0.0f);

                AnimatorSet animator = new AnimatorSet();
                animator.setDuration(3000);
                animator.playTogether(containerFlyUp, containerFadeIn, loadingFadeOut);
                animator.start();

                return true;
            }
        });
    }

    @Override
    public void failure(RetrofitError retrofitError)
    {
        Log.d("Prevoz", "Response: " + retrofitError);
    }

    public static void setListViewHeightBasedOnChildren(ListView listView)
    {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) {
            return;
        }

        int desiredWidth = View.MeasureSpec.makeMeasureSpec(listView.getWidth(), View.MeasureSpec.AT_MOST);
        int totalHeight = 0;
        View view = null;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            view = listAdapter.getView(i, view, listView);
            if (i == 0) {
                view.setLayoutParams(new ViewGroup.LayoutParams(desiredWidth, ViewGroup.LayoutParams.WRAP_CONTENT));
            }
            view.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED);
            totalHeight += view.getMeasuredHeight();
        }
        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
        listView.requestLayout();
    }
}
