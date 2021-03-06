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
package net.simonvt.cathode.remote;

import android.os.Handler;
import android.os.Looper;
import com.squareup.tape.Task;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.util.HttpUtils;
import retrofit.RetrofitError;
import retrofit.client.Response;
import timber.log.Timber;

public abstract class TraktTask implements Task<TraktTaskService> {

  private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

  @Inject protected transient TraktTaskQueue queue;

  @Inject @PriorityQueue protected transient TraktTaskQueue priorityQueue;

  protected transient TraktTaskService service;

  private transient boolean canceled;

  @Override public final void execute(final TraktTaskService service) {
    CathodeApp.inject(service, this);
    this.service = service;

    new Thread(new Runnable() {
      @Override public void run() {
        try {
          Timber.tag(TraktTask.this.getClass().getSimpleName()).d("doTask");
          doTask();
        } catch (RetrofitError e) {
          e.printStackTrace();
          logError(e);
          postOnFailure();
        }
      }
    }).start();
  }

  protected abstract void doTask();

  protected void logError(RetrofitError e) {
    try {
      String url = e.getUrl();
      Response response = e.getResponse();
      if (response != null) {
        int statusCode = response.getStatus();
        String body = HttpUtils.streamToString(response.getBody().in());
        Timber.i("URL: %s", url);
        Timber.i("Status code: %d", statusCode);
        Timber.i("Body:");
        Timber.i(body);
        Timber.e(e, null);
      }
    } catch (Throwable t) {
      // Ignore
    }
  }

  public void cancel() {
    synchronized (this) {
      canceled = true;
    }
  }

  protected final void queueTask(final TraktTask task) {
    Timber.d("Queueing task: " + task.getClass().getSimpleName());
    synchronized (this) {
      if (!canceled) queue.add(task);
    }
  }

  protected final void queuePriorityTask(final TraktTask task) {
    Timber.d("Queueing priority task: " + task.getClass().getSimpleName());
    synchronized (this) {
      if (!canceled) priorityQueue.add(task);
    }
  }

  protected final void postOnSuccess() {
    MAIN_HANDLER.post(new Runnable() {
      @Override public void run() {
        service.onSuccess();
      }
    });
  }

  protected void postOnFailure() {
    MAIN_HANDLER.post(new Runnable() {
      @Override public void run() {
        service.onFailure();
      }
    });
  }

  public interface TaskCallback {

    void onSuccess();

    void onFailure();
  }
}
