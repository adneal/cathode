package net.simonvt.cathode;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.CalendarContract;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import com.squareup.picasso.Picasso;
import com.squareup.tape.ObjectQueue;
import dagger.Module;
import dagger.ObjectGraph;
import dagger.Provides;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.simonvt.cathode.api.ApiKey;
import net.simonvt.cathode.api.ResponseParser;
import net.simonvt.cathode.api.TraktModule;
import net.simonvt.cathode.api.UserCredentials;
import net.simonvt.cathode.event.AuthFailedEvent;
import net.simonvt.cathode.remote.PriorityTraktTaskQueue;
import net.simonvt.cathode.remote.TraktTask;
import net.simonvt.cathode.remote.TraktTaskQueue;
import net.simonvt.cathode.remote.TraktTaskSerializer;
import net.simonvt.cathode.remote.TraktTaskService;
import net.simonvt.cathode.remote.action.EpisodeCollectionTask;
import net.simonvt.cathode.remote.action.EpisodeRateTask;
import net.simonvt.cathode.remote.action.EpisodeWatchedTask;
import net.simonvt.cathode.remote.action.EpisodeWatchlistTask;
import net.simonvt.cathode.remote.action.MovieCollectionTask;
import net.simonvt.cathode.remote.action.MovieRateTask;
import net.simonvt.cathode.remote.action.MovieWatchedTask;
import net.simonvt.cathode.remote.action.MovieWatchlistTask;
import net.simonvt.cathode.remote.action.ShowCollectionTask;
import net.simonvt.cathode.remote.action.ShowRateTask;
import net.simonvt.cathode.remote.action.ShowWatchedTask;
import net.simonvt.cathode.remote.action.ShowWatchlistTask;
import net.simonvt.cathode.remote.sync.SyncEpisodeTask;
import net.simonvt.cathode.remote.sync.SyncEpisodeWatchlistTask;
import net.simonvt.cathode.remote.sync.SyncMovieTask;
import net.simonvt.cathode.remote.sync.SyncMoviesCollectionTask;
import net.simonvt.cathode.remote.sync.SyncMoviesTask;
import net.simonvt.cathode.remote.sync.SyncMoviesWatchedTask;
import net.simonvt.cathode.remote.sync.SyncMoviesWatchlistTask;
import net.simonvt.cathode.remote.sync.SyncSeasonTask;
import net.simonvt.cathode.remote.sync.SyncShowTask;
import net.simonvt.cathode.remote.sync.SyncShowsCollectionTask;
import net.simonvt.cathode.remote.sync.SyncShowsTask;
import net.simonvt.cathode.remote.sync.SyncShowsWatchedTask;
import net.simonvt.cathode.remote.sync.SyncShowsWatchlistTask;
import net.simonvt.cathode.remote.sync.SyncTask;
import net.simonvt.cathode.remote.sync.SyncTrendingMoviesTask;
import net.simonvt.cathode.remote.sync.SyncTrendingShowsTask;
import net.simonvt.cathode.remote.sync.SyncUpdatedMovies;
import net.simonvt.cathode.remote.sync.SyncUpdatedShows;
import net.simonvt.cathode.scheduler.EpisodeTaskScheduler;
import net.simonvt.cathode.scheduler.MovieTaskScheduler;
import net.simonvt.cathode.scheduler.SeasonTaskScheduler;
import net.simonvt.cathode.scheduler.ShowTaskScheduler;
import net.simonvt.cathode.settings.Settings;
import net.simonvt.cathode.ui.HomeActivity;
import net.simonvt.cathode.ui.PhoneController;
import net.simonvt.cathode.ui.adapter.EpisodeWatchlistAdapter;
import net.simonvt.cathode.ui.adapter.MovieSearchAdapter;
import net.simonvt.cathode.ui.adapter.MoviesAdapter;
import net.simonvt.cathode.ui.adapter.SeasonAdapter;
import net.simonvt.cathode.ui.adapter.SeasonsAdapter;
import net.simonvt.cathode.ui.adapter.ShowDescriptionAdapter;
import net.simonvt.cathode.ui.adapter.ShowsAdapter;
import net.simonvt.cathode.ui.dialog.LogoutDialog;
import net.simonvt.cathode.ui.dialog.RatingDialog;
import net.simonvt.cathode.ui.fragment.EpisodeFragment;
import net.simonvt.cathode.ui.fragment.EpisodesWatchlistFragment;
import net.simonvt.cathode.ui.fragment.LoginFragment;
import net.simonvt.cathode.ui.fragment.MovieCollectionFragment;
import net.simonvt.cathode.ui.fragment.MovieFragment;
import net.simonvt.cathode.ui.fragment.MovieWatchlistFragment;
import net.simonvt.cathode.ui.fragment.SearchMovieFragment;
import net.simonvt.cathode.ui.fragment.SearchShowFragment;
import net.simonvt.cathode.ui.fragment.SeasonFragment;
import net.simonvt.cathode.ui.fragment.ShowInfoFragment;
import net.simonvt.cathode.ui.fragment.ShowsCollectionFragment;
import net.simonvt.cathode.ui.fragment.ShowsWatchlistFragment;
import net.simonvt.cathode.ui.fragment.TrendingMoviesFragment;
import net.simonvt.cathode.ui.fragment.TrendingShowsFragment;
import net.simonvt.cathode.ui.fragment.UpcomingShowsFragment;
import net.simonvt.cathode.ui.fragment.WatchedMoviesFragment;
import net.simonvt.cathode.ui.fragment.WatchedShowsFragment;
import net.simonvt.cathode.util.DateUtils;
import net.simonvt.cathode.util.LogWrapper;
import net.simonvt.cathode.util.MovieSearchHandler;
import net.simonvt.cathode.util.ShowSearchHandler;
import net.simonvt.cathode.widget.PhoneEpisodeView;
import net.simonvt.cathode.widget.RemoteImageView;
import retrofit.ErrorHandler;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class CathodeApp extends Application {

  private static final String TAG = "CathodeApp";

  public static final boolean DEBUG = BuildConfig.DEBUG;

  private static final int AUTH_NOTIFICATION = 2;

  private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

  private ObjectGraph objectGraph;

  @Inject Bus bus;

  @Override
  public void onCreate() {
    super.onCreate();
    objectGraph = ObjectGraph.create(new AppModule(this));
    objectGraph.plus(new TraktModule());

    objectGraph.inject(this);

    bus.register(this);
  }

  @Subscribe public void onAuthFailure(AuthFailedEvent event) {
    LogWrapper.v(TAG, "[onAuthFailure]");
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
    final String username = settings.getString(Settings.USERNAME, null);
    final String password = settings.getString(Settings.PASSWORD, null);
    if (password == null) return; // User has logged out, ignore.

    Intent intent = new Intent(this, HomeActivity.class);
    intent.setAction(HomeActivity.ACTION_LOGIN);
    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

    PendingIntent pi = PendingIntent.getActivity(this, 0, intent, 0);

    Notification.Builder builder = new Notification.Builder(this) //
        .setSmallIcon(R.drawable.ic_noti_error)
        .setTicker(this.getString(R.string.auth_failed))
        .setContentTitle(this.getString(R.string.auth_failed))
        .setContentText(this.getString(R.string.auth_failed_desc, username))
        .setContentIntent(pi)
        .setPriority(Notification.PRIORITY_HIGH);

    NotificationManager nm = (NotificationManager) this.getSystemService(NOTIFICATION_SERVICE);
    nm.notify(AUTH_NOTIFICATION, builder.build());
  }

  public static void inject(Context context) {
    ((CathodeApp) context.getApplicationContext()).objectGraph.inject(context);
  }

  public static void inject(Context context, Object object) {
    ((CathodeApp) context.getApplicationContext()).objectGraph.inject(object);
  }

  public static Account getAccount(Context context) {
    return new Account(context.getString(R.string.accountName),
        context.getString(R.string.accountType));
  }

  public static void setupAccount(Context context) {
    AccountManager manager = AccountManager.get(context);
    Account account = getAccount(context);
    manager.addAccountExplicitly(account, null, null);

    ContentResolver.setIsSyncable(account, CalendarContract.AUTHORITY, 1);
    ContentResolver.setSyncAutomatically(account, CalendarContract.AUTHORITY, true);
    ContentResolver.addPeriodicSync(account, CalendarContract.AUTHORITY, null,
        12 * DateUtils.HOUR_IN_MILLIS);
  }

  public static void removeAccount(Context context) {
    AccountManager am = AccountManager.get(context);
    Account[] accounts = am.getAccountsByType(context.getString(R.string.accountType));
    for (Account account : accounts) {
      am.removeAccount(account, null, null);
    }
  }

  @Module(
      includes = {
          TraktModule.class
      },

      injects = {
          CathodeApp.class,

          // Task queues
          PriorityTraktTaskQueue.class, TraktTaskQueue.class,

          // Task schedulers
          EpisodeTaskScheduler.class, MovieTaskScheduler.class, SeasonTaskScheduler.class,
          ShowTaskScheduler.class,

          // Activities
          HomeActivity.class,

          // Fragments
          SearchShowFragment.class, EpisodeFragment.class, EpisodesWatchlistFragment.class,
          LoginFragment.class, LogoutDialog.class, MovieCollectionFragment.class,
          MovieFragment.class, MovieWatchlistFragment.class, SearchMovieFragment.class,
          SeasonFragment.class, ShowInfoFragment.class, ShowsCollectionFragment.class,
          ShowsWatchlistFragment.class, TrendingShowsFragment.class, TrendingMoviesFragment.class,
          UpcomingShowsFragment.class, WatchedMoviesFragment.class, WatchedShowsFragment.class,

          // Dialogs
          RatingDialog.class,

          // ListAdapters
          EpisodeWatchlistAdapter.class, SeasonAdapter.class, SeasonsAdapter.class,
          ShowsAdapter.class, ShowDescriptionAdapter.class, MoviesAdapter.class,
          MovieSearchAdapter.class,

          // Views
          PhoneEpisodeView.class, RemoteImageView.class,

          // Services
          TraktTaskService.class,

          // Tasks
          EpisodeCollectionTask.class, EpisodeRateTask.class, EpisodeWatchedTask.class,
          EpisodeWatchlistTask.class, MovieCollectionTask.class, MovieRateTask.class,
          MovieWatchedTask.class, MovieWatchlistTask.class, ShowRateTask.class,
          ShowCollectionTask.class, ShowWatchlistTask.class, SyncShowsCollectionTask.class,
          SyncEpisodeTask.class, SyncEpisodeWatchlistTask.class, SyncMovieTask.class,
          SyncMoviesTask.class, SyncMoviesCollectionTask.class, SyncMoviesWatchedTask.class,
          SyncMoviesWatchlistTask.class, SyncSeasonTask.class, SyncShowTask.class,
          SyncShowsTask.class, SyncTrendingMoviesTask.class, SyncTrendingShowsTask.class,
          SyncShowsWatchlistTask.class, SyncShowsWatchedTask.class, ShowWatchedTask.class,
          SyncUpdatedMovies.class, SyncUpdatedShows.class, SyncTask.class,

          // Misc
          PhoneController.class, ResponseParser.class, ShowSearchHandler.class,
          ShowSearchHandler.SearchThread.class, MovieSearchHandler.class,
          MovieSearchHandler.SearchThread.class
      })
  static class AppModule {

    private final Context appContext;

    AppModule(Context appContext) {
      this.appContext = appContext;
    }

    @Provides @Singleton TraktTaskQueue provideTaskQueue(Gson gson) {
      TraktTaskQueue queue = TraktTaskQueue.create(appContext, gson);
      if (DEBUG) {
        queue.setListener(new ObjectQueue.Listener<TraktTask>() {
          @Override
          public void onAdd(ObjectQueue<TraktTask> queue, TraktTask entry) {
            LogWrapper.i("TraktTaskQueue", "Queue size: " + queue.size());
          }

          @Override
          public void onRemove(ObjectQueue<TraktTask> queue) {
            LogWrapper.i("TraktTaskQueue", "Queue size: " + queue.size());
          }
        });
      }
      return queue;
    }

    @Provides @Singleton PriorityTraktTaskQueue providePriorityTaskQueue(Gson gson) {
      PriorityTraktTaskQueue queue = PriorityTraktTaskQueue.create(appContext, gson);
      if (DEBUG) {
        queue.setListener(new ObjectQueue.Listener<TraktTask>() {
          @Override
          public void onAdd(ObjectQueue<TraktTask> queue, TraktTask entry) {
            LogWrapper.i("PriorityTraktTaskQueue", "Queue size: " + queue.size());
          }

          @Override
          public void onRemove(ObjectQueue<TraktTask> queue) {
            LogWrapper.i("PriorityTraktTaskQueue", "Queue size: " + queue.size());
          }
        });
      }
      return queue;
    }

    @Provides @Singleton ShowSearchHandler provideShowSearchHandler(Bus bus) {
      return new ShowSearchHandler(appContext, bus);
    }

    @Provides @Singleton MovieSearchHandler provideMovieSearchHandler(Bus bus) {
      return new MovieSearchHandler(appContext, bus);
    }

    @Provides @ApiKey String provideApiKey() {
      return appContext.getString(R.string.apikey);
    }

    @Provides @Singleton ErrorHandler provideErrorHandler(final Bus bus) {
      return new ErrorHandler() {
        @Override public Throwable handleError(RetrofitError error) {
          Response response = error.getResponse();
          if (response != null) {
            final int statusCode = response.getStatus();
            if (statusCode == 401) {
              MAIN_HANDLER.post(new Runnable() {
                @Override public void run() {
                  bus.post(new AuthFailedEvent());
                }
              });
            }
          }

          return error;
        }
      };
    }

    @Provides @Singleton Picasso providePicasso() {
      return new Picasso.Builder(appContext).build();
    }

    @Provides @Singleton Bus provideBus() {
      return new Bus();
    }

    @Provides @Singleton Gson provideGson() {
      GsonBuilder builder = new GsonBuilder();
      builder.registerTypeAdapter(TraktTask.class, new TraktTaskSerializer());
      return builder.create();
    }

    @Provides @Singleton EpisodeTaskScheduler provideEpisodeScheduler() {
      return new EpisodeTaskScheduler(appContext);
    }

    @Provides @Singleton SeasonTaskScheduler provideSeasonScheduler() {
      return new SeasonTaskScheduler(appContext);
    }

    @Provides @Singleton ShowTaskScheduler provideShowScheduler() {
      return new ShowTaskScheduler(appContext);
    }

    @Provides @Singleton MovieTaskScheduler provideMovieScheduler() {
      return new MovieTaskScheduler(appContext);
    }

    @Provides @Singleton UserCredentials provideCredentials() {
      SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(appContext);
      final String username = settings.getString(Settings.USERNAME, null);
      final String password = settings.getString(Settings.PASSWORD, null);
      return new UserCredentials(username, password);
    }
  }
}