package net.simonvt.cathode.ui.fragment;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import net.simonvt.cathode.R;
import net.simonvt.cathode.provider.CathodeContract;
import net.simonvt.cathode.ui.BaseActivity;

public class WatchedMoviesFragment extends MoviesFragment {

  private static final String TAG = "WatchedMoviesFragment";

  @Override public String getTitle() {
    return getResources().getString(R.string.title_movies_watched);
  }

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle inState) {
    return inflater.inflate(R.layout.fragment_movies_watched, container, false);
  }

  @Override protected int getLoaderId() {
    return BaseActivity.LOADER_MOVIES_WATCHED;
  }

  @Override public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
    CursorLoader loader = new CursorLoader(getActivity(), CathodeContract.Movies.CONTENT_URI, null,
        CathodeContract.Movies.WATCHED, null, CathodeContract.Movies.DEFAULT_SORT);
    loader.setUpdateThrottle(2 * DateUtils.SECOND_IN_MILLIS);
    return loader;
  }
}
