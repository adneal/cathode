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

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.api.entity.Season;
import net.simonvt.cathode.api.entity.TvShow;
import net.simonvt.cathode.api.enumeration.DetailLevel;
import net.simonvt.cathode.api.service.UserService;
import net.simonvt.cathode.provider.CathodeContract;
import net.simonvt.cathode.provider.CathodeProvider;
import net.simonvt.cathode.provider.EpisodeWrapper;
import net.simonvt.cathode.provider.ShowWrapper;
import net.simonvt.cathode.remote.TraktTask;
import timber.log.Timber;

public class SyncShowsCollectionTask extends TraktTask {

  @Inject transient UserService userService;

  private void addOp(ArrayList<ContentProviderOperation> ops, ContentProviderOperation op)
      throws RemoteException, OperationApplicationException {
    ops.add(op);
    if (ops.size() >= 50) {
      service.getContentResolver().applyBatch(CathodeProvider.AUTHORITY, ops);
      ops.clear();
    }
  }

  @Override protected void doTask() {
    try {
      ContentResolver resolver = service.getContentResolver();
      List<TvShow> shows = userService.libraryShowsCollection(DetailLevel.MIN);

      Cursor c = resolver.query(CathodeContract.Episodes.CONTENT_URI, new String[] {
          CathodeContract.Episodes._ID, CathodeContract.Episodes.SHOW_ID,
          CathodeContract.Episodes.SEASON_ID,
      }, CathodeContract.Episodes.IN_COLLECTION + "=1", null, null);

      List<Long> episodeIds = new ArrayList<Long>(c.getCount());
      while (c.moveToNext()) {
        episodeIds.add(c.getLong(0));
      }
      c.close();

      ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

      for (TvShow show : shows) {
        if (show.getTvdbId() == null) {
          continue;
        }
        final int tvdbId = show.getTvdbId();
        final long showId = ShowWrapper.getShowId(resolver, tvdbId);

        if (showId == -1) {
          queueTask(new SyncShowTask(tvdbId));
        } else {
          List<Season> seasons = show.getSeasons();
          for (Season season : seasons) {
            final int number = season.getSeason();
            Season.Episodes episodes = season.getEpisodes();
            List<Integer> inCollection = episodes.getNumbers();
            for (int episodeNumber : inCollection) {
              final long episodeId =
                  EpisodeWrapper.getEpisodeId(resolver, showId, number, episodeNumber);
              if (episodeId != -1) {
                if (!episodeIds.remove(episodeId)) {
                  ContentProviderOperation.Builder builder = ContentProviderOperation.newUpdate(
                      CathodeContract.Episodes.buildFromId(episodeId));
                  builder.withValue(CathodeContract.Episodes.IN_COLLECTION, true);
                  addOp(ops, builder.build());
                }
              } else {
                queueTask(new SyncEpisodeTask(tvdbId, number, episodeNumber));
              }
            }
          }
        }
      }

      for (long episodeId : episodeIds) {
        ContentProviderOperation.Builder builder =
            ContentProviderOperation.newUpdate(CathodeContract.Episodes.buildFromId(episodeId));
        builder.withValue(CathodeContract.Episodes.IN_COLLECTION, false);
        addOp(ops, builder.build());
      }

      resolver.applyBatch(CathodeProvider.AUTHORITY, ops);

      postOnSuccess();
    } catch (RemoteException e) {
      Timber.e(e, null);
      postOnFailure();
    } catch (OperationApplicationException e) {
      Timber.e(e, null);
      postOnFailure();
    }
  }
}
