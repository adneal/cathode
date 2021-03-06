/*
 * Copyright (C) 2013 Simon Vig Therkildsen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.simonvt.cathode.ui.fragment;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import butterknife.InjectView;
import com.squareup.otto.Bus;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.event.OnTitleChangedEvent;
import net.simonvt.cathode.provider.CathodeContract;
import net.simonvt.cathode.scheduler.MovieTaskScheduler;
import net.simonvt.cathode.ui.BaseActivity;
import net.simonvt.cathode.ui.dialog.RatingDialog;
import net.simonvt.cathode.widget.CircularProgressIndicator;
import net.simonvt.cathode.widget.ObservableScrollView;
import net.simonvt.cathode.widget.RemoteImageView;

public class MovieFragment extends ProgressFragment
    implements LoaderManager.LoaderCallbacks<Cursor> {

  private static final String TAG = "MovieFragment";

  private static final String ARG_ID = "net.simonvt.cathode.ui.fragment.MovieFragment.id";
  private static final String ARG_TITLE = "net.simonvt.cathode.ui.fragment.MovieFragment.title";

  private static final String DIALOG_RATING =
      "net.simonvt.cathode.ui.fragment.MovieFragment.ratingDialog";

  @Inject MovieTaskScheduler movieScheduler;
  @Inject Bus bus;

  @InjectView(R.id.scrollView) ObservableScrollView scrollView;

  @InjectView(R.id.year) TextView year;
  @InjectView(R.id.certification) TextView certification;
  @InjectView(R.id.fanart) RemoteImageView fanart;
  @InjectView(R.id.poster) RemoteImageView poster;
  @InjectView(R.id.overview) TextView overview;
  @InjectView(R.id.isWatched) TextView isWatched;
  @InjectView(R.id.inCollection) TextView collection;
  @InjectView(R.id.inWatchlist) TextView watchlist;
  @InjectView(R.id.rating) CircularProgressIndicator rating;

  @InjectView(R.id.actorsParent) LinearLayout actorsParent;
  @InjectView(R.id.actors) LinearLayout actors;

  private long movieId;

  private String movieTitle;

  private int currentRating;

  private boolean loaded;

  private boolean watched;

  private boolean collected;

  private boolean inWatchlist;

  public static Bundle getArgs(long movieId, String movieTitle) {
    Bundle args = new Bundle();
    args.putLong(ARG_ID, movieId);
    args.putString(ARG_TITLE, movieTitle);
    return args;
  }

  @Override public void onCreate(Bundle inState) {
    super.onCreate(inState);
    CathodeApp.inject(getActivity(), this);

    setHasOptionsMenu(true);

    Bundle args = getArguments();
    movieId = args.getLong(ARG_ID);
    movieTitle = args.getString(ARG_TITLE);
  }

  @Override public String getTitle() {
    return movieTitle == null ? "" : movieTitle;
  }

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle inState) {
    return inflater.inflate(R.layout.fragment_movie, container, false);
  }

  @Override public void onViewCreated(View view, Bundle inState) {
    super.onViewCreated(view, inState);
    rating.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view) {
        RatingDialog.newInstance(RatingDialog.Type.MOVIE, movieId, currentRating)
            .show(getFragmentManager(), DIALOG_RATING);
      }
    });

    scrollView.setListener(new ObservableScrollView.ScrollListener() {
      @Override public void onScrollChanged(int l, int t) {
        final int offset = (int) (t / 2.0f);
        fanart.setTranslationY(offset);
      }
    });

    getLoaderManager().initLoader(BaseActivity.LOADER_MOVIE, null, this);
    getLoaderManager().initLoader(BaseActivity.LOADER_MOVIE_ACTORS, null, actorsLoader);
  }

  @Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    if (loaded) {
      if (watched) {
        menu.add(0, R.id.action_unwatched, 1, R.string.action_unwatched);
      } else {
        menu.add(0, R.id.action_watched, 2, R.string.action_watched);
        if (inWatchlist) {
          menu.add(0, R.id.action_watchlist_remove, 5, R.string.action_watchlist_remove);
        } else {
          menu.add(0, R.id.action_watchlist_add, 6, R.string.action_watchlist_add);
        }
      }

      if (collected) {
        menu.add(0, R.id.action_collection_remove, 3, R.string.action_collection_remove);
      } else {
        menu.add(0, R.id.action_collection_add, 4, R.string.action_collection_add);
      }
    }
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.action_watched:
        movieScheduler.setWatched(movieId, true);
        return true;

      case R.id.action_unwatched:
        movieScheduler.setWatched(movieId, false);
        return true;

      case R.id.action_watchlist_add:
        movieScheduler.setIsInWatchlist(movieId, true);
        return true;

      case R.id.action_watchlist_remove:
        movieScheduler.setIsInWatchlist(movieId, false);
        return true;

      case R.id.action_collection_add:
        movieScheduler.setIsInCollection(movieId, true);
        return true;

      case R.id.action_collection_remove:
        movieScheduler.setIsInCollection(movieId, false);
        return true;
    }

    return false;
  }

  private void updateView(final Cursor cursor) {
    if (cursor == null || !cursor.moveToFirst()) return;
    loaded = true;

    final String title = cursor.getString(cursor.getColumnIndex(CathodeContract.Movies.TITLE));
    if (!title.equals(movieTitle)) {
      movieTitle = title;
      bus.post(new OnTitleChangedEvent());
    }
    final int year = cursor.getInt(cursor.getColumnIndex(CathodeContract.Movies.YEAR));
    final String certification =
        cursor.getString(cursor.getColumnIndex(CathodeContract.Movies.CERTIFICATION));

    final String fanartUrl = cursor.getString(cursor.getColumnIndex(CathodeContract.Movies.FANART));
    fanart.setImage(fanartUrl);
    final String posterUrl = cursor.getString(cursor.getColumnIndex(CathodeContract.Movies.POSTER));
    poster.setImage(posterUrl);

    currentRating = cursor.getInt(cursor.getColumnIndex(CathodeContract.Movies.RATING));
    final int ratingAll =
        cursor.getInt(cursor.getColumnIndex(CathodeContract.Movies.RATING_PERCENTAGE));
    rating.setValue(ratingAll);

    final String overview =
        cursor.getString(cursor.getColumnIndex(CathodeContract.Movies.OVERVIEW));
    watched = cursor.getInt(cursor.getColumnIndex(CathodeContract.Movies.WATCHED)) == 1;
    collected = cursor.getInt(cursor.getColumnIndex(CathodeContract.Movies.IN_COLLECTION)) == 1;
    inWatchlist = cursor.getInt(cursor.getColumnIndex(CathodeContract.Movies.IN_WATCHLIST)) == 1;

    isWatched.setVisibility(watched ? View.VISIBLE : View.GONE);
    collection.setVisibility(collected ? View.VISIBLE : View.GONE);
    watchlist.setVisibility(inWatchlist ? View.VISIBLE : View.GONE);

    this.year.setText(String.valueOf(year));
    this.certification.setText(certification);
    this.overview.setText(overview);

    setContentVisible(true);
    getActivity().invalidateOptionsMenu();
  }

  @Override public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
    CursorLoader loader =
        new CursorLoader(getActivity(), CathodeContract.Movies.buildFromId(movieId), null, null,
            null, null);
    loader.setUpdateThrottle(2 * DateUtils.SECOND_IN_MILLIS);
    return loader;
  }

  @Override public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
    updateView(cursor);
  }

  @Override public void onLoaderReset(Loader<Cursor> cursorLoader) {
  }

  private void updateActors(Cursor c) {
    actorsParent.setVisibility(c.getCount() > 0 ? View.VISIBLE : View.GONE);
    actors.removeAllViews();

    c.moveToPosition(-1);

    while (c.moveToNext()) {
      View v = LayoutInflater.from(getActivity()).inflate(R.layout.person, actors, false);

      RemoteImageView headshot = (RemoteImageView) v.findViewById(R.id.headshot);
      headshot.setImage(c.getString(c.getColumnIndex(CathodeContract.MovieActors.HEADSHOT)));
      TextView name = (TextView) v.findViewById(R.id.name);
      name.setText(c.getString(c.getColumnIndex(CathodeContract.MovieActors.NAME)));
      TextView character = (TextView) v.findViewById(R.id.job);
      character.setText(c.getString(c.getColumnIndex(CathodeContract.MovieActors.CHARACTER)));

      actors.addView(v);
    }
  }

  private LoaderManager.LoaderCallbacks<Cursor> actorsLoader =
      new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
          CursorLoader loader =
              new CursorLoader(getActivity(), CathodeContract.MovieActors.buildFromMovieId(movieId),
                  null, null, null, null);
          loader.setUpdateThrottle(2 * DateUtils.SECOND_IN_MILLIS);
          return loader;
        }

        @Override public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
          updateActors(cursor);
        }

        @Override public void onLoaderReset(Loader<Cursor> cursorLoader) {
        }
      };
}
