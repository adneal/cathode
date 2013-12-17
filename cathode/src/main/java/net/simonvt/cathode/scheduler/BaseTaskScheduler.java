package net.simonvt.cathode.scheduler;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Executor;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.remote.PriorityQueue;
import net.simonvt.cathode.remote.TraktTask;
import net.simonvt.cathode.remote.TraktTaskQueue;

public class BaseTaskScheduler {

  private static final Executor EXECUTOR = new Executor() {
    @Override public void execute(Runnable r) {
      new Thread(r).start();
    }
  };

  private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

  @Inject TraktTaskQueue queue;

  @Inject @PriorityQueue TraktTaskQueue priorityQueue;

  protected Context context;

  public BaseTaskScheduler(Context context) {
    CathodeApp.inject(context, this);
    this.context = context;
  }

  protected final void postTask(final TraktTask task) {
    MAIN_HANDLER.post(new Runnable() {
      @Override public void run() {
        queue.add(task);
      }
    });
  }

  protected final void postPriorityTask(final TraktTask task) {
    MAIN_HANDLER.post(new Runnable() {
      @Override public void run() {
        priorityQueue.add(task);
      }
    });
  }

  protected void execute(Runnable r) {
    SERIAL_EXECUTOR.execute(r);
  }

  private static final SerialExecutor SERIAL_EXECUTOR = new SerialExecutor();

  private static class SerialExecutor implements Executor {

    final Queue<Runnable> tasks = new ArrayDeque<Runnable>();
    Runnable active;

    public synchronized void execute(final Runnable r) {
      tasks.offer(new Runnable() {
        @Override public void run() {
          try {
            r.run();
          } finally {
            scheduleNext();
          }
        }
      });
      if (active == null) {
        scheduleNext();
      }
    }

    protected synchronized void scheduleNext() {
      if ((active = tasks.poll()) != null) {
        EXECUTOR.execute(active);
      }
    }
  }
}
