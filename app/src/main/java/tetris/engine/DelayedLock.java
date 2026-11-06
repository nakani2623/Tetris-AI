package tetris.engine;

import java.util.Objects;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import tetris.controller.dispatch.MainThreadDispatcher;

/** A per-engine lock timer. Dispatch must return callbacks to the game thread. */
public final class DelayedLock implements LockPolicy {
    private final MainThreadDispatcher dispatcher;
    private final long delayMillis;
    private final int maxResets;
    private final ScheduledThreadPoolExecutor scheduler;
    private ScheduledFuture<?> pending;
    private long generation;
    private int resets;
    private boolean closed;

    public DelayedLock(MainThreadDispatcher dispatcher, long delayMillis, int maxResets) {
        this.dispatcher = Objects.requireNonNull(dispatcher);
        if (delayMillis < 0 || maxResets < 0) throw new IllegalArgumentException();
        this.delayMillis = delayMillis;
        this.maxResets = maxResets;
        scheduler = new ScheduledThreadPoolExecutor(1);
        scheduler.setRemoveOnCancelPolicy(true);
    }

    @Override
    public void onPositionChanged(boolean landed, Runnable lock) {
        if (closed) return;
        cancelPending();
        if (!landed) return;
        if (++resets > maxResets) {
            reset();
            lock.run();
            return;
        }
        long ticket = generation;
        pending = scheduler.schedule(() -> dispatcher.dispatch(() -> {
            // A cancelled timer may already have queued a callback on the game thread.
            if (closed || ticket != generation) return;
            reset();
            lock.run();
        }), delayMillis, TimeUnit.MILLISECONDS);
    }

    private void cancelPending() {
        generation++;
        if (pending != null) {
            pending.cancel(false);
            pending = null;
        }
    }

    @Override
    public void reset() {
        cancelPending();
        resets = 0;
    }

    @Override
    public void close() {
        closed = true;
        reset();
        scheduler.shutdownNow();
    }
}
