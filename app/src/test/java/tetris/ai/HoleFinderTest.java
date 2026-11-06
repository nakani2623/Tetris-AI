package tetris.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import tetris.engine.Board;
import tetris.engine.GameEngine;
import tetris.engine.Tetromino;
import tetris.engine.type.TetrominoType;
import tetris.utils.Point;

public class HoleFinderTest {
    @Test 
    void emptyBoardHasOneHole() {
        // Arrange
        GameEngine engine = new GameEngine(10, 20);
        HoleFinder holeFinder = new HoleFinder(engine.getBoard());

        // Act
        int holes = holeFinder.run();

        // Assert
        assertEquals(1, holes);
    }

    @Test 
    void currentTetrominoIgnored() {
        // Arrange
        GameEngine engine = new GameEngine(4, 20);
        Tetromino current = new Tetromino(new Point(1, 10), TetrominoType.I);
        addTetromino(engine.getBoard(), current);
        engine.getBoard().setCurrentTetromino(current);
        HoleFinder holeFinder = new HoleFinder(engine.getBoard());

        // Act
        int holes = holeFinder.run();

        // Assert
        assertEquals(1, holes);
    }
    
    @Test
    void singleTetrominoCreatesHoles() {
        // Arrange
        GameEngine engine = new GameEngine(4, 20);
        addTetromino(engine.getBoard(), new Tetromino(new Point(1, 10), TetrominoType.I));
        HoleFinder holeFinder = new HoleFinder(engine.getBoard());

        // Act
        int holes = holeFinder.run();

        // Assert
        assertEquals(2, holes);
    }

    @Test
    void multipleTetrominosCreateHoles() {
        // Arrange
        GameEngine engine = new GameEngine(4, 20);
        addTetromino(engine.getBoard(), new Tetromino(new Point(1, 6), TetrominoType.I));
        addTetromino(engine.getBoard(), new Tetromino(new Point(1, 12), TetrominoType.I));
        HoleFinder holeFinder = new HoleFinder(engine.getBoard());

        // Act
        int holes = holeFinder.run();

        // Assert
        assertEquals(3, holes);
    }

    private void addTetromino(Board board, Tetromino tetromino) {
        board.getAllTetrominos().add(tetromino);
    }
}