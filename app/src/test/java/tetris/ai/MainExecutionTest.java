package tetris.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import tetris.controller.dispatch.MainThreadDispatcher;
import tetris.engine.Board;
import tetris.engine.GameEngine;
import tetris.engine.Tetromino;
import tetris.engine.type.TetrominoType;
import tetris.utils.Point;

class MainExecutionTest {
    @Test
    void executeWaitsUntilDispatchedActionSequenceCompletes() throws Exception {
        ArrayBlockingQueue<Runnable> dispatchedTasks = new ArrayBlockingQueue<>(1);
        MainThreadDispatcher queuedDispatcher = task -> {
            if (!dispatchedTasks.offer(task)) {
                throw new IllegalStateException("Unexpected additional dispatched task");
            }
        };
        GameEngine engine = new GameEngine(10, 20, null);
        Tetromino current = new Tetromino(new Point(5, 5), TetrominoType.T);
        Board board = engine.getBoard();
        board.setCurrentTetromino(current);
        double originalX = current.centre.getX();
        DropPoint dropPoint = new DropPoint(engine);
        dropPoint.executionOrder = new ArrayList<>();
        dropPoint.executionOrder.add(AIAction.MOVE_LEFT);
        Main ai = new Main(engine, queuedDispatcher);

        ExecutorService worker = Executors.newSingleThreadExecutor();
        try {
            Future<?> execution = worker.submit(() -> {
                try {
                    ai.execute(dropPoint, engine);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(e);
                } catch (java.util.concurrent.ExecutionException e) {
                    throw new AssertionError(e.getCause());
                }
            });

            Runnable dispatchedTask = dispatchedTasks.poll(1, TimeUnit.SECONDS);
            assertNotNull(dispatchedTask, "Expected the action sequence to be dispatched");
            assertFalse(execution.isDone(), "execute must wait for the dispatched action sequence");
            assertEquals(originalX, board.getcurrentTetromino().centre.getX());

            dispatchedTask.run();
            execution.get(1, TimeUnit.SECONDS);

            assertEquals(originalX - 1, board.getcurrentTetromino().centre.getX());
        } finally {
            worker.shutdownNow();
        }
    }
}
