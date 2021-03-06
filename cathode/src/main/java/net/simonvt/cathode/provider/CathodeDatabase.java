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
package net.simonvt.cathode.provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import net.simonvt.cathode.provider.CathodeContract.ActorColumns;
import net.simonvt.cathode.provider.CathodeContract.EpisodeColumns;
import net.simonvt.cathode.provider.CathodeContract.MovieActors;
import net.simonvt.cathode.provider.CathodeContract.MovieColumns;
import net.simonvt.cathode.provider.CathodeContract.MovieDirectors;
import net.simonvt.cathode.provider.CathodeContract.MovieGenres;
import net.simonvt.cathode.provider.CathodeContract.MovieProducers;
import net.simonvt.cathode.provider.CathodeContract.MovieTopWatchers;
import net.simonvt.cathode.provider.CathodeContract.MovieWriters;
import net.simonvt.cathode.provider.CathodeContract.SeasonColumns;
import net.simonvt.cathode.provider.CathodeContract.ShowActor;
import net.simonvt.cathode.provider.CathodeContract.ShowColumns;
import net.simonvt.cathode.provider.CathodeContract.ShowGenres;
import net.simonvt.cathode.provider.CathodeContract.ShowTopWatchers;
import net.simonvt.cathode.provider.CathodeContract.TopEpisodeColumns;
import net.simonvt.cathode.provider.CathodeContract.TopWatcherColumns;
import net.simonvt.cathode.util.DateUtils;

public class CathodeDatabase extends SQLiteOpenHelper {

  private static final String DATABASE_NAME = "cathode.db";
  private static final int DATABASE_VERSION = 2;

  public interface Tables {

    String SHOWS = "shows";

    String SHOWS_WITH_UNWATCHED = SHOWS
        + " LEFT OUTER JOIN episodes ON episodes._id=(SELECT episodes._id FROM"
        + " episodes WHERE episodes.watched=0 AND episodes.showId=shows._id AND episodes.season<>0"
        + " AND episodes.episodeFirstAired>"
        + DateUtils.YEAR_IN_MILLIS
        // TODO: Find better solution
        + " ORDER BY episodes.season ASC, episodes.episode ASC LIMIT 1)";

    String SHOWS_WITH_UNCOLLECTED = SHOWS
        + " LEFT OUTER JOIN episodes ON episodes._id=(SELECT episodes._id FROM"
        + " episodes WHERE episodes.inCollection=0 AND episodes.showId=shows._id AND episodes.season<>0"
        + " AND episodes.episodeFirstAired>"
        + DateUtils.YEAR_IN_MILLIS
        // TODO: Find better solution
        + " ORDER BY episodes.season ASC, episodes.episode ASC LIMIT 1)";

    String SHOW_TOP_WATCHERS = "showTopWatchers";
    String SHOW_TOP_EPISODES = "topEpisodes";
    String SHOW_ACTORS = "showActors";
    String SHOW_GENRES = "showGenres";
    String SEASONS = "seasons";
    String EPISODES = "episodes";
    String EPISODES_WITH_SHOW_TITLE = EPISODES + " JOIN " + SHOWS + " AS " + SHOWS + " ON "
        + SHOWS + "." + CathodeContract.Shows._ID
        + "=" + EPISODES + "." + CathodeContract.Episodes.SHOW_ID;
    String MOVIES = "movies";
    String MOVIE_GENRES = "movieGenres";
    String MOVIE_TOP_WATCHERS = "movieTopWatchers";
    String MOVIE_ACTORS = "movieActors";
    String MOVIE_DIRECTORS = "movieDirectors";
    String MOVIE_WRITERS = "movieWriters";
    String MOVIE_PRODUCERS = "movieProducers";

    String SHOW_SEARCH_SUGGESTIONS = "showSearchSuggestions";
    String MOVIE_SEARCH_SUGGESTIONS = "movieSearchSuggestions";
  }

  interface References {

    String SHOW_ID = "REFERENCES " + Tables.SHOWS + "(" + BaseColumns._ID + ")";
    String SEASON_ID = "REFERENCES " + Tables.SEASONS + "(" + BaseColumns._ID + ")";
    String MOVIE_ID = "REFERENCES " + Tables.MOVIES + "(" + BaseColumns._ID + ")";
  }

  interface Trigger {
    String EPISODE_UPDATE_AIRED_NAME = "episodeUpdateAired";
    String EPISODE_UPDATE_WATCHED_NAME = "episodeUpdateWatched";
    String EPISODE_UPDATE_COLLECTED_NAME = "episodeUpdateCollected";
    String EPISODE_INSERT_NAME = "episodeInsert";

    String SEASONS_UPDATE_WATCHED = "UPDATE " + Tables.SEASONS + " SET "
        + SeasonColumns.WATCHED_COUNT + "=(SELECT COUNT(*) FROM " + Tables.EPISODES + " WHERE "
        + Tables.EPISODES + "." + EpisodeColumns.SEASON_ID
        + "=NEW." + EpisodeColumns.SEASON_ID
        + " AND " + Tables.EPISODES + "." + EpisodeColumns.WATCHED
        + "=1 AND " + Tables.EPISODES + "." + EpisodeColumns.SEASON + ">0)"
        + " WHERE " + Tables.SEASONS + "." + BaseColumns._ID + "=NEW." + EpisodeColumns.SEASON_ID
        + ";";

    String SEASONS_UPDATE_COLLECTED = "UPDATE " + Tables.SEASONS + " SET "
        + SeasonColumns.IN_COLLECTION_COUNT + "=(SELECT COUNT(*) FROM " + Tables.EPISODES + " WHERE "
        + Tables.EPISODES + "." + EpisodeColumns.SEASON_ID
        + "=NEW." + EpisodeColumns.SEASON_ID
        + " AND " + Tables.EPISODES + "." + EpisodeColumns.IN_COLLECTION
        + "=1 AND " + Tables.EPISODES + "." + EpisodeColumns.SEASON + ">0)"
        + " WHERE " + Tables.SEASONS + "." + BaseColumns._ID + "=NEW." + EpisodeColumns.SEASON_ID
        + ";";

    String SEASONS_UPDATE_AIRDATE = "UPDATE " + Tables.SEASONS + " SET "
        + SeasonColumns.AIRDATE_COUNT + "=(SELECT COUNT(*) FROM " + Tables.EPISODES + " WHERE "
        + Tables.EPISODES + "." + EpisodeColumns.SEASON_ID
        + "=NEW." + EpisodeColumns.SEASON_ID
        + " AND " + Tables.EPISODES + "." + EpisodeColumns.FIRST_AIRED
        + ">" + DateUtils.YEAR_IN_MILLIS
        + " AND " + Tables.EPISODES + "." + EpisodeColumns.SEASON + ">0)"
        + " WHERE " + Tables.SEASONS + "." + BaseColumns._ID + "=NEW." + EpisodeColumns.SEASON_ID
        + ";";

    String SHOWS_UPDATE_WATCHED = "UPDATE " + Tables.SHOWS + " SET "
        + ShowColumns.WATCHED_COUNT + "=(SELECT COUNT(*) FROM " + Tables.EPISODES + " WHERE "
        + Tables.EPISODES + "." + EpisodeColumns.SHOW_ID
        + "=NEW." + EpisodeColumns.SHOW_ID
        + " AND " + Tables.EPISODES + "." + EpisodeColumns.WATCHED
        + "=1 AND " + Tables.EPISODES + "." + EpisodeColumns.SEASON + ">0)"
        + " WHERE " + Tables.SHOWS + "." + BaseColumns._ID + "=NEW." + EpisodeColumns.SHOW_ID
        + ";";

    String SHOWS_UPDATE_COLLECTED = "UPDATE " + Tables.SHOWS + " SET "
        + ShowColumns.IN_COLLECTION_COUNT + "=(SELECT COUNT(*) FROM " + Tables.EPISODES + " WHERE "
        + Tables.EPISODES + "." + EpisodeColumns.SHOW_ID
        + "=NEW." + EpisodeColumns.SHOW_ID
        + " AND " + Tables.EPISODES + "." + EpisodeColumns.IN_COLLECTION
        + "=1 AND " + Tables.EPISODES + "." + EpisodeColumns.SEASON + ">0)"
        + " WHERE " + Tables.SHOWS + "." + BaseColumns._ID + "=NEW." + EpisodeColumns.SHOW_ID
        + ";";

    String SHOWS_UPDATE_AIRDATE = "UPDATE " + Tables.SHOWS + " SET "
        + ShowColumns.AIRDATE_COUNT + "=(SELECT COUNT(*) FROM " + Tables.EPISODES + " WHERE "
        + Tables.EPISODES + "." + EpisodeColumns.SHOW_ID
        + "=NEW." + EpisodeColumns.SHOW_ID
        + " AND " + Tables.EPISODES + "." + EpisodeColumns.FIRST_AIRED
        + ">" + DateUtils.YEAR_IN_MILLIS
        + " AND " + Tables.EPISODES + "." + EpisodeColumns.SEASON + ">0)"
        + " WHERE " + Tables.SHOWS + "." + BaseColumns._ID + "=NEW." + EpisodeColumns.SHOW_ID
        + ";";

    String EPISODE_UPDATE_AIRED = "CREATE TRIGGER " + EPISODE_UPDATE_AIRED_NAME
        + " AFTER UPDATE OF "
        + EpisodeColumns.FIRST_AIRED
        + " ON " + Tables.EPISODES + " BEGIN "
        + SEASONS_UPDATE_AIRDATE
        + SHOWS_UPDATE_AIRDATE
        + " END;";

    String EPISODE_UPDATE_WATCHED = "CREATE TRIGGER " + EPISODE_UPDATE_WATCHED_NAME
        + " AFTER UPDATE OF "
        + EpisodeColumns.WATCHED
        + " ON " + Tables.EPISODES + " BEGIN "
        + SEASONS_UPDATE_WATCHED
        + SHOWS_UPDATE_WATCHED
        + " END;";

    String EPISODE_UPDATE_COLLECTED = "CREATE TRIGGER " + EPISODE_UPDATE_COLLECTED_NAME
        + " AFTER UPDATE OF "
        + EpisodeColumns.IN_COLLECTION
        + " ON " + Tables.EPISODES + " BEGIN "
        + SEASONS_UPDATE_COLLECTED
        + SHOWS_UPDATE_COLLECTED
        + " END;";

    String EPISODE_INSERT = "CREATE TRIGGER " + EPISODE_INSERT_NAME
        + " AFTER INSERT ON " + Tables.EPISODES
        + " BEGIN "
        + SEASONS_UPDATE_WATCHED
        + SEASONS_UPDATE_AIRDATE
        + SEASONS_UPDATE_COLLECTED
        + SHOWS_UPDATE_WATCHED
        + SHOWS_UPDATE_AIRDATE
        + SHOWS_UPDATE_COLLECTED
        + " END;";
  }

  private static volatile CathodeDatabase instance;

  public static CathodeDatabase getInstance(Context context) {
    if (instance == null) {
      synchronized (CathodeDatabase.class) {
        if (instance == null) {
          instance = new CathodeDatabase(context);
        }
      }
    }

    return instance;
  }

  private CathodeDatabase(Context context) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
  }

  @Override public void onCreate(SQLiteDatabase db) {
    db.execSQL("CREATE TABLE "
        + Tables.SHOWS
        + " ("
        + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
        + ShowColumns.TITLE + " TEXT NOT NULL,"
        + ShowColumns.YEAR + " INTEGER,"
        + ShowColumns.URL + " TEXT,"
        + ShowColumns.FIRST_AIRED + " INTEGER,"
        + ShowColumns.COUNTRY + " TEXT,"
        + ShowColumns.OVERVIEW + " TEXT,"
        + ShowColumns.RUNTIME + " INTEGER DEFAULT 0,"
        + ShowColumns.NETWORK + " TEXT,"
        + ShowColumns.STATUS + " TEXT,"
        + ShowColumns.AIR_DAY + " TEXT,"
        + ShowColumns.AIR_TIME + " TEXT,"
        + ShowColumns.CERTIFICATION + " TEXT,"
        + ShowColumns.IMDB_ID + " TEXT,"
        + ShowColumns.TVDB_ID + " TEXT,"
        + ShowColumns.TVRAGE_ID + " TEXT,"
        + ShowColumns.LAST_UPDATED + " INTEGER DEFAULT 0,"
        + ShowColumns.POSTER + " TEXT,"
        + ShowColumns.FANART + " TEXT,"
        + ShowColumns.BANNER + " TEXT,"
        + ShowColumns.RATING_PERCENTAGE + " INTEGER DEFAULT 0,"
        + ShowColumns.RATING_VOTES + " INTEGER DEFAULT 0,"
        + ShowColumns.RATING_LOVED + " INTEGER DEFAULT 0,"
        + ShowColumns.RATING_HATED + " INTEGER DEFAULT 0,"
        + ShowColumns.WATCHERS + " INTEGER DEFAULT 0,"
        + ShowColumns.PLAYS + " INTEGER DEFAULT 0,"
        + ShowColumns.SCROBBLES + " INTEGER DEFAULT 0,"
        + ShowColumns.CHECKINS + " INTEGER DEFAULT 0,"
        + ShowColumns.RATING + " INTEGER DEFAULT 0,"
        + ShowColumns.IN_WATCHLIST + " INTEGER DEFAULT 0,"
        + ShowColumns.WATCHED_COUNT + " INTEGER DEFAULT 0,"
        + ShowColumns.AIRDATE_COUNT + " INTEGER DEFAULT 0,"
        + ShowColumns.IN_WATCHLIST_COUNT + " INTEGER DEFAULT 0,"
        + ShowColumns.IN_COLLECTION_COUNT + " INTEGER DEFAULT 0,"
        + ShowColumns.TRENDING_INDEX + " INTEGER DEFAULT -1,"
        + ShowColumns.RECOMMENDATION_INDEX + " INTEGER DEFAULT -1,"
        + ShowColumns.HIDDEN + " INTEGER DEFAULT 0)");

    db.execSQL("CREATE TABLE " + Tables.SHOW_TOP_WATCHERS + " ("
        + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
        + ShowTopWatchers.SHOW_ID + " INTEGER " + References.SHOW_ID + ","
        + TopWatcherColumns.PLAYS + " INTEGER DEFAULT 0,"
        + TopWatcherColumns.USERNAME + " TEXT NOT NULL,"
        + TopWatcherColumns.PROTECTED + " INTEGER DEFAULT 0,"
        + TopWatcherColumns.FULL_NAME + " TEXT,"
        + TopWatcherColumns.GENDER + " TEXT,"
        + TopWatcherColumns.AGE + " INTEGER,"
        + TopWatcherColumns.LOCATION + "TEXT,"
        + TopWatcherColumns.ABOUT + " TEXT,"
        + TopWatcherColumns.JOINED + " INTEGER DEFAULT 0,"
        + TopWatcherColumns.AVATAR + " TEXT,"
        + TopWatcherColumns.URL + " TEXT NOT NULL)");

    db.execSQL("CREATE TABLE " + Tables.SHOW_TOP_EPISODES + " ("
        + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
        + TopEpisodeColumns.SHOW_ID + " INTEGER " + References.SHOW_ID + ","
        + TopEpisodeColumns.SEASON_ID + " INTEGER " + References.SEASON_ID + ","
        + TopEpisodeColumns.NUMBER + " INTEGER NOT NULL,"
        + TopEpisodeColumns.PLAYS + " INTEGER DEFAULT 0,"
        + TopEpisodeColumns.TITLE + " TEXT NOT NULL,"
        + TopEpisodeColumns.URL + " TEXT,"
        + TopEpisodeColumns.FIRST_AIRED + " INTEGER DEFAULT 0)");

    db.execSQL("CREATE TABLE " + Tables.SHOW_ACTORS + " ("
        + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
        + ShowActor.SHOW_ID + " INTEGER " + References.SHOW_ID + ","
        + ActorColumns.NAME + " TEXT NOT NULL,"
        + ActorColumns.CHARACTER + " TEXT)");

    db.execSQL("CREATE TABLE " + Tables.SHOW_GENRES + " ("
        + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
        + ShowGenres.SHOW_ID + " INTEGER " + References.SHOW_ID + ","
        + ShowGenres.GENRE + " TEXT)");

    db.execSQL("CREATE TABLE " + Tables.SEASONS + " ("
        + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
        + SeasonColumns.SHOW_ID + " INTEGER " + References.SHOW_ID + ","
        + SeasonColumns.SEASON + " INTEGER NOT NULL,"
        + SeasonColumns.EPISODES + " INTEGER DEFAULT 0,"
        + SeasonColumns.URL + " TEXT,"
        + SeasonColumns.WATCHED_COUNT + " INTEGER DEFAULT 0,"
        + SeasonColumns.AIRDATE_COUNT + " INTEGER DEFAULT 0,"
        + SeasonColumns.IN_COLLECTION_COUNT + " INTEGER DEFAULT 0,"
        + SeasonColumns.IN_WATCHLIST_COUNT + " INTEGER DEFAULT 0,"
        + SeasonColumns.POSTER + " TEXT)");

    db.execSQL("CREATE TABLE " + Tables.EPISODES + " ("
        + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
        + EpisodeColumns.SHOW_ID + " INTEGER " + References.SHOW_ID + ","
        + EpisodeColumns.SEASON_ID + " INTEGER " + References.SEASON_ID + ","
        + EpisodeColumns.SEASON + " INTEGER NOT NULL,"
        + EpisodeColumns.EPISODE + " INTEGER NOT NULL,"
        + EpisodeColumns.TITLE + " TEXT NOT NULL,"
        + EpisodeColumns.OVERVIEW + " TEXT,"
        + EpisodeColumns.URL + " TEXT,"
        + EpisodeColumns.TVDB_ID + " INTEGER,"
        + EpisodeColumns.IMDB_ID + " STRING,"
        + EpisodeColumns.FIRST_AIRED + " INTEGER,"
        + EpisodeColumns.SCREEN + " TEXT,"
        + EpisodeColumns.RATING_PERCENTAGE + " INTEGER DEFAULT 0,"
        + EpisodeColumns.RATING_VOTES + " INTEGER DEFAULT 0,"
        + EpisodeColumns.RATING_LOVED + " INTEGER DEFAULT 0,"
        + EpisodeColumns.RATING_HATED + " INTEGER DEFAULT 0,"
        + EpisodeColumns.WATCHED + " INTEGER DEFAULT 0,"
        + EpisodeColumns.PLAYS + " INTEGER DEFAULT 0,"
        + EpisodeColumns.RATING + " TEXT,"
        + EpisodeColumns.IN_WATCHLIST + " INTEGER DEFAULT 0,"
        + EpisodeColumns.IN_COLLECTION + " INTEGER DEFAULT 0)");

    db.execSQL("CREATE TABLE " + Tables.MOVIES + " ("
        + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
        + MovieColumns.TITLE + " TEXT NOT NULL,"
        + MovieColumns.YEAR + " INTEGER,"
        + MovieColumns.RELEASED + " INTEGER,"
        + MovieColumns.URL + " TEXT,"
        + MovieColumns.TRAILER + " TEXT,"
        + MovieColumns.RUNTIME + " INTEGER,"
        + MovieColumns.TAGLINE + " TEXT,"
        + MovieColumns.OVERVIEW + " TEXT,"
        + MovieColumns.CERTIFICATION + " TEXT,"
        + MovieColumns.IMDB_ID + " TEXT,"
        + MovieColumns.TMDB_ID + " INTEGER,"
        + MovieColumns.RT_ID + " INTEGER DEFAULT 0,"
        + MovieColumns.POSTER + " TEXT,"
        + MovieColumns.FANART + " TEXT,"
        + MovieColumns.RATING_PERCENTAGE + " INTEGER,"
        + MovieColumns.RATING_VOTES + " INTEGER DEFAULT 0,"
        + MovieColumns.RATING_LOVED + " INTEGER DEFAULT 0,"
        + MovieColumns.RATING_HATED + " INTEGER DEFAULT 0,"
        + MovieColumns.RATING + " INTEGER DEFAULT 0,"
        + MovieColumns.WATCHERS + " INTEGER DEFAULT 0,"
        + MovieColumns.PLAYS + " INTEGER DEFAULT 0,"
        + MovieColumns.SCROBBLES + " INTEGER DEFAULT 0,"
        + MovieColumns.CHECKINS + " INTEGER DEFAULT 0,"
        + MovieColumns.LAST_UPDATED + " INTEGER DEFAULT 0,"
        + MovieColumns.WATCHED + " INTEGER DEFAULT 0,"
        + MovieColumns.IN_WATCHLIST + " INTEGER DEFAULT 0,"
        + MovieColumns.IN_COLLECTION + " INTEGER DEFAULT 0,"
        + MovieColumns.TRENDING_INDEX + " INTEGER DEFAULT -1,"
        + MovieColumns.RECOMMENDATION_INDEX + " INTEGER DEFAULT -1)");

    db.execSQL("CREATE TABLE " + Tables.MOVIE_GENRES + " ("
        + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
        + MovieGenres.MOVIE_ID + " INTEGER " + References.MOVIE_ID + ","
        + MovieGenres.GENRE + " TEXT)");

    db.execSQL("CREATE TABLE " + Tables.MOVIE_TOP_WATCHERS + " ("
        + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
        + MovieTopWatchers.MOVIE_ID + " INTEGER " + References.MOVIE_ID + ","
        + TopWatcherColumns.PLAYS + " INTEGER DEFAULT 0,"
        + TopWatcherColumns.USERNAME + " TEXT NOT NULL,"
        + TopWatcherColumns.PROTECTED + " INTEGER DEFAULT 0,"
        + TopWatcherColumns.FULL_NAME + " TEXT,"
        + TopWatcherColumns.GENDER + " TEXT,"
        + TopWatcherColumns.AGE + " INTEGER,"
        + TopWatcherColumns.LOCATION + " TEXT,"
        + TopWatcherColumns.ABOUT + " TEXT,"
        + TopWatcherColumns.JOINED + " INTEGER DEFAULT 0,"
        + TopWatcherColumns.AVATAR + " TEXT,"
        + TopWatcherColumns.URL + " TEXT NOT NULL)");

    db.execSQL("CREATE TABLE " + Tables.MOVIE_ACTORS + " ("
        + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
        + MovieActors.MOVIE_ID + " INTEGER " + References.MOVIE_ID + ","
        + ActorColumns.NAME + " TEXT NOT NULL,"
        + ActorColumns.CHARACTER + " TEXT,"
        + ActorColumns.HEADSHOT + " TEXT)");

    db.execSQL("CREATE TABLE " + Tables.MOVIE_DIRECTORS + " ("
        + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
        + MovieDirectors.MOVIE_ID + " INTEGER " + References.MOVIE_ID + ","
        + MovieDirectors.NAME + " TEXT NOT NULL,"
        + MovieDirectors.HEADSHOT + " TEXT)");

    db.execSQL("CREATE TABLE " + Tables.MOVIE_WRITERS + " ("
        + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
        + MovieWriters.MOVIE_ID + " INTEGER " + References.MOVIE_ID + ","
        + MovieWriters.NAME + " TEXT NOT NULL,"
        + MovieWriters.JOB + " TEXT,"
        + MovieWriters.HEADSHOT + " TEXT)");

    db.execSQL("CREATE TABLE " + Tables.MOVIE_PRODUCERS + " ("
        + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
        + MovieProducers.MOVIE_ID + " INTEGER " + References.MOVIE_ID + ","
        + MovieProducers.NAME + " TEXT NOT NULL,"
        + MovieProducers.EXECUTIVE + " INTEGER NOT NULL,"
        + MovieProducers.HEADSHOT + " TEXT)");

    db.execSQL("CREATE TABLE " + Tables.SHOW_SEARCH_SUGGESTIONS + " ("
        + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
        + CathodeContract.SearchSuggestionsColumns.QUERY + " TEXT NOT NULL,"
        + CathodeContract.SearchSuggestionsColumns.COUNT + " INTEGER DEFAULT 0)");

    db.execSQL("CREATE TABLE " + Tables.MOVIE_SEARCH_SUGGESTIONS + " ("
        + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
        + CathodeContract.SearchSuggestionsColumns.QUERY + " TEXT NOT NULL,"
        + CathodeContract.SearchSuggestionsColumns.COUNT + " INTEGER DEFAULT 0)");

    createTriggers(db);
  }

  @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    switch (oldVersion) {
      case 1:
        db.execSQL("ALTER TABLE " + Tables.SHOWS
        + " ADD COLUMN " + ShowColumns.HIDDEN + " INTEGER DEFAULT 0");
    }
  }

  private void createTriggers(SQLiteDatabase db) {
    db.execSQL(Trigger.EPISODE_INSERT);
    db.execSQL(Trigger.EPISODE_UPDATE_AIRED);
    db.execSQL(Trigger.EPISODE_UPDATE_WATCHED);
    db.execSQL(Trigger.EPISODE_UPDATE_COLLECTED);
  }

  private void dropTriggers(SQLiteDatabase db) {
    db.execSQL("DROP TRIGGER " + Trigger.EPISODE_UPDATE_AIRED_NAME);
    db.execSQL("DROP TRIGGER " + Trigger.EPISODE_UPDATE_WATCHED_NAME);
    db.execSQL("DROP TRIGGER " + Trigger.EPISODE_UPDATE_COLLECTED_NAME);
    db.execSQL("DROP TRIGGER " + Trigger.EPISODE_INSERT_NAME);
  }

  public void clearUserData() {
    SQLiteDatabase db = getWritableDatabase();
    db.beginTransaction();
    dropTriggers(db);

    ContentValues cv;

    cv = new ContentValues();
    cv.put(CathodeContract.Shows.WATCHED_COUNT, 0);
    cv.put(CathodeContract.Shows.IN_COLLECTION_COUNT, 0);
    cv.put(CathodeContract.Shows.IN_WATCHLIST_COUNT, 0);
    cv.put(CathodeContract.Shows.IN_WATCHLIST, false);
    cv.put(CathodeContract.Shows.HIDDEN, false);
    db.update(Tables.SHOWS, cv, null, null);

    cv = new ContentValues();
    cv.put(CathodeContract.Seasons.WATCHED_COUNT, 0);
    cv.put(CathodeContract.Seasons.IN_COLLECTION_COUNT, 0);
    cv.put(CathodeContract.Seasons.IN_WATCHLIST_COUNT, 0);
    db.update(Tables.SEASONS, cv, null, null);

    cv = new ContentValues();
    cv.put(CathodeContract.Episodes.WATCHED, 0);
    cv.put(CathodeContract.Episodes.PLAYS, 0);
    cv.put(CathodeContract.Episodes.IN_WATCHLIST, 0);
    cv.put(CathodeContract.Episodes.IN_COLLECTION, 0);
    db.update(Tables.EPISODES, cv, null, null);

    cv = new ContentValues();
    cv.put(CathodeContract.Movies.WATCHED, 0);
    cv.put(CathodeContract.Movies.IN_COLLECTION, 0);
    cv.put(CathodeContract.Movies.IN_WATCHLIST, 0);
    db.update(Tables.MOVIES, cv, null, null);

    createTriggers(db);

    db.setTransactionSuccessful();
    db.endTransaction();
  }
}
