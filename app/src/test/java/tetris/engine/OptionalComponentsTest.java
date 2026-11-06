package tetris.engine;

import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import tetris.engine.type.Direction;
import tetris.engine.type.Rotation;
import tetris.engine.type.TetrominoType;
import tetris.utils.Point;

class OptionalComponentsTest {
    private GameEngine engine() {
        GameEngine engine = new GameEngine(10, 20);
        Tetromino piece = new Tetromino(new Point(4, 5), TetrominoType.T);
        engine.getBoard().getAllTetrominos().add(piece);
        engine.getBoard().setCurrentTetromino(piece);
        return engine;
    }

    @Test
    void copiesDoNotUseInstalledRuntimeComponents() {
        GameEngine source = engine();
        var policy = new RecordingPolicy();
        source.setLockPolicy(policy);
        source.setAutoMoveScheduler(new AutoMoveScheduler(1, 0, task -> fail(), source));
        int notifications = policy.notifications;
        try {
            for (int i = 0; i < 1000; i++) {
                GameEngine copy = new GameEngine(source);
                Tetromino piece = copy.getBoard().getcurrentTetromino();
                copy.moveDAS(Direction.DOWN);
                for (int turn = 0; turn < 20; turn++) copy.rotate(Rotation.CLOCKWISE);
                assertSame(piece, copy.getBoard().getcurrentTetromino());
                assertEquals(0, copy.getGravity());
                assertNull(copy.getScoreManager());
                copy.hardDrop(); // Explicit lock is still supported without components.
                assertNotSame(piece, copy.getBoard().getcurrentTetromino());
                copy.dispose();
            }
            assertEquals(notifications, policy.notifications);
            assertEquals(5, source.getBoard().getcurrentTetromino().centre.getY());
            assertEquals(0, policy.closes);
        } finally {
            source.dispose();
        }
    }

    @Test
    void replacementRemovalAndDisposalReleasePolicies() {
        GameEngine engine = engine();
        var first = new RecordingPolicy();
        var second = new RecordingPolicy();
        engine.setLockPolicy(first);
        engine.setLockPolicy(first);
        assertEquals(0, first.closes);
        engine.setLockPolicy(second);
        assertEquals(1, first.closes);
        engine.moveDAS(Direction.DOWN);
        assertTrue(second.landed);
        engine.hold();
        assertTrue(second.resets > 0);
        int resets = second.resets;
        engine.reset();
        assertTrue(second.resets > resets);
        engine.setLockPolicy(null);
        assertEquals(1, second.closes);
        engine.dispose();
        engine.dispose();
        assertEquals(1, second.closes);
    }

    @Test
    void appCanInstallTimerAndRemovingItCancelsQueuedLock() throws Exception {
        var queue = new LinkedBlockingQueue<Runnable>();
        GameEngine engine = engine();
        try {
            engine.moveDAS(Direction.DOWN);
            Tetromino piece = engine.getBoard().getcurrentTetromino();
            engine.setLockPolicy(new DelayedLock(queue::add, 0, 15));
            Runnable callback = queue.poll(2, TimeUnit.SECONDS);
            assertNotNull(callback);
            engine.setLockPolicy(null);
            callback.run();
            assertSame(piece, engine.getBoard().getcurrentTetromino());
            engine.setLockPolicy(new DelayedLock(queue::add, 0, 15));
            callback = queue.poll(2, TimeUnit.SECONDS);
            assertNotNull(callback);
            callback.run();
            assertNotSame(piece, engine.getBoard().getcurrentTetromino());
        } finally {
            engine.dispose();
        }
    }

    @Test
    void removingAutoMovementInvalidatesQueuedGravity() throws Exception {
        var queue = new LinkedBlockingQueue<Runnable>();
        GameEngine engine = new GameEngine(10, 20);
        try {
            engine.start();
            engine.setAutoMoveScheduler(new AutoMoveScheduler(1, 0, queue::add, engine));
            Runnable callback = queue.poll(2, TimeUnit.SECONDS);
            assertNotNull(callback);
            double y = engine.getBoard().getcurrentTetromino().centre.getY();
            engine.setAutoMoveScheduler(null);
            callback.run();
            assertEquals(y, engine.getBoard().getcurrentTetromino().centre.getY());
        } finally {
            engine.dispose();
        }
    }

    private static class RecordingPolicy implements LockPolicy {
        int notifications, resets, closes;
        boolean landed;
        public void onPositionChanged(boolean landed, Runnable lock) {
            notifications++;
            this.landed = landed;
        }
        public void reset() { resets++; }
        public void close() { closes++; }
    }
}
