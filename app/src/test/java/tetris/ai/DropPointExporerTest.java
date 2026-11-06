package tetris.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import tetris.engine.Board;
import tetris.engine.GameEngine;
import tetris.engine.Tetromino;
import tetris.engine.type.Direction;
import tetris.engine.type.RotationState;
import tetris.engine.type.Rotation;
import tetris.engine.type.TetrominoType;
import tetris.utils.Point;

class DropPointExporerTest {
    @Test
    void findsEveryTPlacementOnAnEmptyTenWideBoard() {
        assertFindsAllTPlacements(10, 5);
    }

    @Test
    void findsEveryTPlacementOnAnEmptySixWideBoard() {
        assertFindsAllTPlacements(6, 3);
    }

    @Test
    void deduplicatesIPlacementsWithDifferentRotationCentres() {
        assertSymmetricPlacements(TetrominoType.I, new int[][][] {
            {{0, 0}, {1, 0}, {2, 0}, {3, 0}},
            {{0, 0}, {0, 1}, {0, 2}, {0, 3}}
        }, 17, 9);
    }

    @Test
    void deduplicatesSPlacementsWithDifferentRotationCentres() {
        assertSymmetricPlacements(TetrominoType.S, new int[][][] {
            {{1, 0}, {2, 0}, {0, 1}, {1, 1}},
            {{0, 0}, {0, 1}, {1, 1}, {1, 2}}
        }, 17, 9);
    }

    @Test
    void deduplicatesZPlacementsWithDifferentRotationCentres() {
        assertSymmetricPlacements(TetrominoType.Z, new int[][][] {
            {{0, 0}, {1, 0}, {1, 1}, {2, 1}},
            {{1, 0}, {0, 1}, {1, 1}, {0, 2}}
        }, 17, 9);
    }

    @Test
    void deduplicatesORotationsWithoutRemovingDistinctPlacements() {
        assertSymmetricPlacements(TetrominoType.O, new int[][][] {
            {{0, 0}, {1, 0}, {0, 1}, {1, 1}}
        }, 9, 5);
    }

    private void assertSymmetricPlacements(
            TetrominoType type, int[][][] shapes, int tenWideCount, int sixWideCount) {
        for (int width : new int[] {10, 6}) {
            GameEngine engine = engineWithCurrentTetromino(
                width, new Tetromino(new Point(width / 2 - 1, 5), type)
            );
            Tetromino originalGhost = new Tetromino(engine.getBoard().getShadowTetromino());
            GameEngine rotated = new GameEngine(engine);
            rotated.rotate(Rotation.CLOCKWISE);
            rotated.rotate(Rotation.CLOCKWISE);
            Tetromino rotatedGhost = rotated.getBoard().getShadowTetromino();

            // Same occupied cells can have different rotation metadata and centres.
            assertEquals(occupiedCells(originalGhost), occupiedCells(rotatedGhost));
            assertNotEquals(originalGhost.rotationState, rotatedGhost.rotationState);
            if (type != TetrominoType.O) {
                assertNotEquals(originalGhost.centre, rotatedGhost.centre);
            }

            Set<DropPoint> results = new DropPointExporer(engine).run();
            Set<Set<Cell>> actual = new HashSet<>();
            for (DropPoint result : results) {
                actual.add(occupiedCells(result.getGhost()));
            }
            Set<Set<Cell>> expected = expectedFloorPlacements(width, shapes);
            assertEquals(width == 10 ? tenWideCount : sixWideCount, expected.size());
            assertEquals(expected, actual, type + " must retain every distinct footprint");
            assertEquals(actual.size(), results.size(), type + " must not repeat footprints");

            // The original landing is also reachable after two rotations; keep only
            // its shortest route (the final HARD_DROP alone).
            var matching = results.stream()
                .filter(result -> occupiedCells(result.getGhost()).equals(occupiedCells(originalGhost)))
                .toList();
            assertEquals(1, matching.size());
            assertEquals(java.util.List.of(AIAction.HARD_DROP), matching.get(0).executionOrder);
        }
    }

    // Independent geometric expectations, without the production equality or rotator.
    private Set<Set<Cell>> expectedFloorPlacements(int width, int[][][] shapes) {
        Set<Set<Cell>> placements = new HashSet<>();
        for (int[][] shape : shapes) {
            int maxX = 0;
            int maxY = 0;
            for (int[] cell : shape) {
                maxX = Math.max(maxX, cell[0]);
                maxY = Math.max(maxY, cell[1]);
            }
            for (int left = 0; left + maxX < width; left++) {
                Set<Cell> cells = new HashSet<>();
                for (int[] cell : shape) {
                    cells.add(new Cell(left + cell[0], 19 - maxY + cell[1]));
                }
                placements.add(cells);
            }
        }
        return placements;
    }

    private Set<Cell> occupiedCells(Tetromino tetromino) {
        Set<Cell> cells = new HashSet<>();
        tetromino.children.forEach(mino -> cells.add(
            new Cell(mino.getPosition().getX(), mino.getPosition().getY())
        ));
        return cells;
    }

    private record Cell(double x, double y) {}

    private void assertFindsAllTPlacements(int width, int startX) {
        GameEngine engine = engineWithCurrentTetromino(
            width,
            new Tetromino(new Point(startX, 5), TetrominoType.T)
        );
        Set<DropPoint> dropPoints = new DropPointExporer(engine).run();
        Set<Position> actualPositions = new HashSet<>();
        dropPoints.forEach(dropPoint -> actualPositions.add(positionOf(dropPoint.getGhost())));
        Set<Position> expectedPositions = new HashSet<>();
        for (int x = 1; x < width - 1; x++) {
            for (RotationState rotationState : RotationState.values()) {
                expectedPositions.add(new Position(x, landingY(rotationState), rotationState));
            }
        }
        expectedPositions.add(new Position(0, landingY(RotationState.RIGHT), RotationState.RIGHT));
        expectedPositions.add(new Position(
            width - 1,
            landingY(RotationState.LEFT),
            RotationState.LEFT
        ));

        assertEquals(expectedPositions, actualPositions);
        assertEquals(4 * (width - 2) + 2, dropPoints.size());
    }

    private int landingY(RotationState rotationState) {
        return switch (rotationState) {
            case ZERO -> 19;
            case RIGHT, TWO, LEFT -> 18;
        };
    }

    private GameEngine engineWithCurrentTetromino(int width, Tetromino current) {
        GameEngine engine = new GameEngine(width, 20);
        Board board = engine.getBoard();
        board.getAllTetrominos().add(current);
        board.setCurrentTetromino(current);
        engine.move(Direction.LEFT);
        engine.move(Direction.RIGHT);
        return engine;
    }

    private Position positionOf(Tetromino tetromino) {
        return new Position(
            tetromino.centre.getX(),
            tetromino.centre.getY(),
            tetromino.rotationState
        );
    }

    private record Position(double x, double y, RotationState rotationState) {}
}
