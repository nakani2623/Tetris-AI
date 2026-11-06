package tetris.engine;

import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class DelayedLockTest {
    private Runnable take(LinkedBlockingQueue<Runnable> queue) throws Exception {
        Runnable callback = queue.poll(2, TimeUnit.SECONDS);
        assertNotNull(callback, "Timer must dispatch to the game thread");
        return callback;
    }

    @Test
    void dispatchesLockAndExecutesItOnlyOnce() throws Exception {
        var queue = new LinkedBlockingQueue<Runnable>();
        var count = new AtomicInteger();
        try (var policy = new DelayedLock(queue::add, 0, 15)) {
            policy.onPositionChanged(true, count::incrementAndGet);
            Runnable callback = take(queue);
            assertEquals(0, count.get());
            callback.run();
            callback.run();
            assertEquals(1, count.get());
        }
    }

    @Test
    void cancellationInvalidatesAlreadyDispatchedCallbacks() throws Exception {
        var queue = new LinkedBlockingQueue<Runnable>();
        var count = new AtomicInteger();
        try (var policy = new DelayedLock(queue::add, 0, 15)) {
            policy.onPositionChanged(true, count::incrementAndGet);
            Runnable beforeMovement = take(queue);
            policy.onPositionChanged(true, count::incrementAndGet);
            Runnable beforeLeavingGround = take(queue);
            beforeMovement.run();
            policy.onPositionChanged(false, count::incrementAndGet);
            beforeLeavingGround.run();
            policy.onPositionChanged(true, count::incrementAndGet);
            Runnable beforeReset = take(queue);
            policy.reset();
            beforeReset.run();
            policy.onPositionChanged(true, count::incrementAndGet);
            Runnable beforeClose = take(queue);
            policy.close();
            beforeClose.run();
            assertEquals(0, count.get());
        }
    }

    @Test
    void resetRestoresLockResetBudget() {
        var count = new AtomicInteger();
        try (var policy = new DelayedLock(task -> fail("Timer must not expire"), 60000, 1)) {
            policy.onPositionChanged(true, count::incrementAndGet);
            policy.reset();
            policy.onPositionChanged(true, count::incrementAndGet);
            assertEquals(0, count.get());
            policy.onPositionChanged(true, count::incrementAndGet);
            assertEquals(1, count.get());
        }
    }
}
