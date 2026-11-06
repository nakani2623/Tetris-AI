package tetris.ai;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import tetris.engine.Board;
import tetris.engine.GameEngine;

/**
 * PointExporer
 * find all possible drop points for a tetromino given a board state.
 */
public class DropPointExporer {
    GameEngine engine;
    private Board board;
    private Set<AIAction> actions = new HashSet<>();
    private Set<DropPoint> dropPoints = new HashSet<>();

    public DropPointExporer(GameEngine engine) {
        this.engine = engine;
        this.board = engine.getBoard();
        actions.add(AIAction.MOVE_LEFT);
        actions.add(AIAction.MOVE_RIGHT);
        actions.add(AIAction.ROTATE_CLOCKWISE);
        actions.add(AIAction.ROTATE_COUNTER_CLOCKWISE);
    }

    private void breathFirstExploreFrom(DropPoint baseline) {
        Queue<DropPoint> queue = new ArrayDeque<>();
        queue.add(baseline);

        while (!queue.isEmpty()) {
            DropPoint current = queue.remove();
            if (isKnown(current)) {
                continue;
            }

            dropPoints.add(current);

            for (AIAction action : actions) {
                DropPoint nextDropPoint = new DropPoint(current);
                nextDropPoint.executionOrder.add(action);

                action.execute(nextDropPoint.engine);
                queue.add(nextDropPoint);
            }
        }

        for (DropPoint dp : dropPoints) {
            dp.executionOrder.add(AIAction.HARD_DROP);
        }
    }

    private boolean isKnown(DropPoint newPoint) {
        for (DropPoint known : dropPoints) {
            if (newPoint.getGhost().isSamePositionAndRotation(known.getGhost()))
                return true;
        }
        return false;
    }

    // Removes positions occupy the same block but have different rotation states
    private void removeDuplicates() {
        List<DropPoint> unique = new ArrayList<>();

        for (DropPoint candidate : dropPoints) {
            int duplicateIndex = -1;

            for (int i = 0; i < unique.size(); i++) {
                if (candidate.getGhost()
                        .isSamePosition(unique.get(i).getGhost())) {
                    duplicateIndex = i;
                    break;
                }
            }

            if (duplicateIndex == -1) {
                unique.add(candidate);
            } else if (candidate.executionOrder.size()
                    < unique.get(duplicateIndex).executionOrder.size()) {
                unique.set(duplicateIndex, candidate);
            }
        }

        dropPoints.clear();
        dropPoints.addAll(unique);
    }

    public Set<DropPoint> run() {
        DropPoint origin = new DropPoint(engine);
        breathFirstExploreFrom(origin);

        removeDuplicates();

        return dropPoints;
    }
}