package tetris.ai;

import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import tetris.controller.dispatch.MainThreadDispatcher;
import tetris.engine.Board;
import tetris.engine.GameEngine;
import tetris.engine.Mino;
import tetris.engine.Tetromino;

public class Main {
    private final GameEngine engine;
    private final MainThreadDispatcher dispatcher;

    public Main(GameEngine engine, MainThreadDispatcher dispatcher) {
        this.engine = engine;
        this.dispatcher = dispatcher;
    }

    public void run() throws InterruptedException, ExecutionException {
        GameEngine explorerEngine = new GameEngine(this.engine);
        Set<DropPoint> dropPoints = new DropPointExporer(explorerEngine).run();
        if (dropPoints.isEmpty()) {
            return;
        }

        DropPoint bestPlacement = chooseBestPlacement(dropPoints);
        System.out.println("Got best placement: " + bestPlacement);
        execute(bestPlacement, engine);
    }

    public void execute(DropPoint dropPoint, GameEngine engine) throws InterruptedException, ExecutionException {
        FutureTask<Void> actionTask = new FutureTask<>(() -> {
            for (AIAction action : dropPoint.executionOrder) {
                action.execute(engine);
            }
            return null;
        });
        dispatcher.dispatch(actionTask);
        actionTask.get();
    }

    private DropPoint chooseBestPlacement(Set<DropPoint> dropPoints) throws InterruptedException, ExecutionException {
        DropPoint bestPlacement = null;
        int lowestStackHeight = Integer.MAX_VALUE;
        int minHoles = Integer.MAX_VALUE;
        for (DropPoint dp : dropPoints) {
            GameEngine simulatedEngine = new GameEngine(this.engine);
            execute(dp, simulatedEngine);
            int holes = new HoleFinder(simulatedEngine.getBoard()).run();
            int stackHeight = stackHeight(simulatedEngine);
            System.out.println("Drop point " + dp + ": holes=" + holes + ", stack height=" + stackHeight);
            if (stackHeight < lowestStackHeight || (stackHeight == lowestStackHeight && holes < minHoles)) {
                lowestStackHeight = stackHeight;
                minHoles = holes;
                bestPlacement = dp;
            }
        }
        return bestPlacement;
    }

    private int stackHeight(GameEngine engine) {
        Board board = engine.getBoard();
        int maxHeight = 0;
        for (Tetromino tetromino : board.getAllTetrominos()) {
            if (tetromino == board.getcurrentTetromino())
                continue;
            for (Mino m : tetromino.children) {
                int height = (int)board.getHeight() - (int)m.getPosition().getY();
                if (height > maxHeight)
                    maxHeight = height;
            }
        }
        System.out.println("Stack height on " + engine + ": " + maxHeight);
        return maxHeight;
    }
}
