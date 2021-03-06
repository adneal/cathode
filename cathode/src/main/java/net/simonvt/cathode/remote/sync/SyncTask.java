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
package net.simonvt.cathode.remote.sync;

import javax.inject.Inject;
import net.simonvt.cathode.api.service.UserService;
import net.simonvt.cathode.remote.TraktTask;
import net.simonvt.cathode.settings.ActivityWrapper;

public class SyncTask extends TraktTask {

  @Inject transient UserService userService;

  @Override protected void doTask() {
    queueTask(new SyncUpdatedShows());
    queueTask(new SyncUpdatedMovies());

    queueTask(new SyncUserActivityTask());

    if (ActivityWrapper.trendingNeedsUpdate(service)) {
      ActivityWrapper.updateTrending(service);
      queueTask(new SyncTrendingShowsTask());
      queueTask(new SyncTrendingMoviesTask());
    }

    if (ActivityWrapper.recommendationsNeedsUpdate(service)) {
      ActivityWrapper.updateRecommendations(service);
      queueTask(new SyncShowRecommendations());
      queueTask(new SyncMovieRecommendations());
    }

    postOnSuccess();
  }
}
