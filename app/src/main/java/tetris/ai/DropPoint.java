package tetris.ai;

import java.util.ArrayList;
import java.util.List;

import tetris.engine.GameEngine;
import tetris.engine.Tetromino;

/**
 * A Tetromino represents a drop point and how to get to this point
 * DropPoint
 */
public class DropPoint {
    GameEngine engine;
    List<AIAction> executionOrder;

    public DropPoint(GameEngine engine) {
        this.engine = new GameEngine(engine);
        executionOrder = new ArrayList<>();
    }

    /**
     * Copy constructor
     * @param dp DropPoint to copy
     */

    public DropPoint(DropPoint dp) {
        this.engine = new GameEngine(dp.engine);
        this.executionOrder = new ArrayList<>(dp.executionOrder);
    }

    public Tetromino getTetromino() {
        return engine.getBoard().getcurrentTetromino();
    }

    public Tetromino getGhost() {
        return engine.getBoard().getShadowTetromino();
    }

    @Override
    public String toString() {
        return getGhost().toString() + ", execution size: " + executionOrder.size();
    }
}
