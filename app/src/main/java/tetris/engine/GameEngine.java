package tetris.engine;

import java.util.ArrayList;
import java.util.List;

// import javafx.application.Platform;

import tetris.controller.GameOverListener;
import tetris.engine.observers.Observable;
import tetris.engine.observers.Observer;
import tetris.engine.operator.*;
import tetris.engine.rotator.Rotator;
import tetris.engine.rotator.SuperRotator;
import tetris.engine.score.ScoreManager;
import tetris.engine.spinDetecter.AllMiniDetecter;
import tetris.engine.spinDetecter.SpinDetecter;
import tetris.engine.type.Direction;
import tetris.engine.type.Rotation;
import tetris.engine.type.SpinState;
import tetris.engine.type.TetrominoType;

public class GameEngine implements Observable {
    // low level module references
    private ScoreManager scoreManager;
    private Board board;
    private SpinDetecter spinDetecter; 
    private Rotator rotator;
    private NextQueue nextQueue;
    private AutoMoveScheduler autoMoveScheduler;
    // private HoldManager holdManager;

    // hold
    private TetrominoType holdTetrominoType;

    private LockPolicy lockPolicy;
    private boolean started;

    private GameOverListener listener;

    // plan to remove
    private List<Observer> observers; // move to low level modules

    /**
     * Constructs game bord with specific dimension
     * @param width
     * @param height
     */
    public GameEngine(Board board) {
        this.board = board;
        // configure game, todo: create and use initEngine(JSON? configFile) instead below
        observers = new ArrayList<>();

        // default components behaviour
        listener = () -> {};
        spinDetecter = new AllMiniDetecter();
        rotator = new SuperRotator();
        nextQueue = new NextQueue();
        // holdManager = new HoldManager();
    }
    
    public GameEngine(int width, int height) {
        this(new Board(width, height));
    }

    /** Copies board/hold state for simulation, without timers or a shared score manager. */
    public GameEngine(GameEngine source) {
        this(new Board(source.board));
        this.holdTetrominoType = source.holdTetrominoType;
        // Runtime components and mutable scoring state are intentionally not copied.
    }
//  ███████╗████████╗ █████╗ ██████╗ ████████╗
//  ██╔════╝╚══██╔══╝██╔══██╗██╔══██╗╚══██╔══╝
//  ███████╗   ██║   ███████║██████╔╝   ██║
//  ╚════██║   ██║   ██╔══██║██╔══██╗   ██║
//  ███████║   ██║   ██║  ██║██║  ██║   ██║
//  ╚══════╝   ╚═╝   ╚═╝  ╚═╝╚═╝  ╚═╝   ╚═╝
    public void start() {
        started = true;
        spawn();
        if (autoMoveScheduler != null) autoMoveScheduler.start();
    }

    /**
     * Reset the board state into initial
     */
    public void reset() {
        resetLockPolicy();
        if (autoMoveScheduler != null) autoMoveScheduler.reset();
        if (scoreManager != null) scoreManager.reset();
        board.reset();
        nextQueue.reset();
        holdTetrominoType = null;
        notifyObservers();
    }
//  ███╗   ███╗ ██████╗ ██╗   ██╗███████╗
//  ████╗ ████║██╔═══██╗██║   ██║██╔════╝
//  ██╔████╔██║██║   ██║██║   ██║█████╗
//  ██║╚██╔╝██║██║   ██║╚██╗ ██╔╝██╔══╝
//  ██║ ╚═╝ ██║╚██████╔╝ ╚████╔╝ ███████╗
//  ╚═╝     ╚═╝ ╚═════╝   ╚═══╝  ╚══════╝
    public void move(Direction direction){
        Operator op;
        if (direction == Direction.DOWN) {op = new MoveDownOperator();}
        else if (direction == Direction.LEFT) {op = new MoveLeftOperator();}
        else if (direction == Direction.RIGHT) {op = new MoveRightOperator();}
        else {op = null;}
        board.tryOperate(op);

        // check for start/reset of lock timer
        checkLanded();
        
    }

    public void moveDAS(Direction direction) {
        Operator op;
        if (direction == Direction.DOWN) {op = new MoveDownOperator();}
        else if (direction == Direction.LEFT) {op = new MoveLeftOperator();}
        else if (direction == Direction.RIGHT) {op = new MoveRightOperator();}
        else {op = null;}

        Tetromino moving = board.getcurrentTetromino();
        while (board.getcurrentTetromino() == moving) {
            if (! board.tryOperate(op)) {
                break;
            }
            checkLanded();
            if (board.getcurrentTetromino() != moving) break;
            // let "auto move scheduler move piece by DAS/SoftDrop state"
            if (autoMoveScheduler != null) autoMoveScheduler.onOperate();
        }
    }

//  ██╗  ██╗ ██████╗ ██╗     ██████╗
//  ██║  ██║██╔═══██╗██║     ██╔══██╗
//  ███████║██║   ██║██║     ██║  ██║
//  ██╔══██║██║   ██║██║     ██║  ██║
//  ██║  ██║╚██████╔╝███████╗██████╔╝
//  ╚═╝  ╚═╝ ╚═════╝ ╚══════╝╚═════╝
//            ◀───[ T ]───▶
    public void hold() {
        // remove current tetromino
        resetLockPolicy();
        board.getAllTetrominos().remove(board.getcurrentTetromino());

        Tetromino currentTetrominoClone = new Tetromino(board.getcurrentTetromino());
        if (holdTetrominoType == null) {
            spawn();
        }

        else {
            if (!board.spawn(holdTetrominoType) || (scoreManager != null && scoreManager.haveAchievedGoal())) {
                listener.onGameOver();
                return;
            }
            if (autoMoveScheduler != null) autoMoveScheduler.onSpawn();
            checkLanded();
        }

        holdTetrominoType = currentTetrominoClone.type;
        board.updateShadow();
        notifyObservers();
    }

//  ███████╗ ██████╗ ███████╗████████╗    ██████╗ ██████╗  ██████╗ ██████╗
//  ██╔════╝██╔═══██╗██╔════╝╚══██╔══╝    ██╔══██╗██╔══██╗██╔═══██╗██╔══██╗
//  ███████╗██║   ██║█████╗     ██║       ██║  ██║██████╔╝██║   ██║██████╔╝
//  ╚════██║██║   ██║██╔══╝     ██║       ██║  ██║██╔══██╗██║   ██║██╔═══╝
//  ███████║╚██████╔╝██║        ██║       ██████╔╝██║  ██║╚██████╔╝██║
//  ╚══════╝ ╚═════╝ ╚═╝        ╚═╝       ╚═════╝ ╚═╝  ╚═╝ ╚═════╝ ╚═╝
    public void startSoftDrop() {
        if (autoMoveScheduler != null) autoMoveScheduler.startSoftDrop();
    }

    public void endSoftDrop() {
        if (autoMoveScheduler != null) autoMoveScheduler.endSoftDrop();
    }

    public void setSoftDropFactor(double softDropFactor) {
        if (autoMoveScheduler != null) autoMoveScheduler.setSoftDropFactor(softDropFactor);
    }
//  ██████╗  █████╗ ███████╗     ██████╗██╗  ██╗ █████╗ ██████╗  ██████╗ ███████╗
//  ██╔══██╗██╔══██╗██╔════╝    ██╔════╝██║  ██║██╔══██╗██╔══██╗██╔════╝ ██╔════╝
//  ██║  ██║███████║███████╗    ██║     ███████║███████║██████╔╝██║  ███╗█████╗
//  ██║  ██║██╔══██║╚════██║    ██║     ██╔══██║██╔══██║██╔══██╗██║   ██║██╔══╝
//  ██████╔╝██║  ██║███████║    ╚██████╗██║  ██║██║  ██║██║  ██║╚██████╔╝███████╗
//  ╚═════╝ ╚═╝  ╚═╝╚══════╝     ╚═════╝╚═╝  ╚═╝╚═╝  ╚═╝╚═╝  ╚═╝ ╚═════╝ ╚══════╝
    public void chargeDAS(Direction direction) {
        if (autoMoveScheduler != null) autoMoveScheduler.chargeDAS(direction);
    }  
    
    public void unchargeDAS(Direction direction) {
        if (autoMoveScheduler != null) autoMoveScheduler.unchargeDAS(direction);
    }   
    

//  ██████╗  ██████╗ ████████╗ █████╗ ████████╗███████╗
//  ██╔══██╗██╔═══██╗╚══██╔══╝██╔══██╗╚══██╔══╝██╔════╝
//  ██████╔╝██║   ██║   ██║   ███████║   ██║   █████╗
//  ██╔══██╗██║   ██║   ██║   ██╔══██║   ██║   ██╔══╝
//  ██║  ██║╚██████╔╝   ██║   ██║  ██║   ██║   ███████╗
//  ╚═╝  ╚═╝ ╚═════╝    ╚═╝   ╚═╝  ╚═╝   ╚═╝   ╚══════╝
    public void rotate(Rotation rotation) {
        if (!rotator.rotate(rotation, board)) {
            return;
        }
        if (autoMoveScheduler != null) autoMoveScheduler.onOperate();
        // detect spin
        SpinState currSpinState = spinDetecter.detectSpin(board);

        // score manager handle spin event
        if (scoreManager != null) {
            scoreManager.onSpin(currSpinState, board.getcurrentTetromino().type);
        }

        // check lock timer
        checkLanded();
    }  
    
//  ███████╗██████╗  █████╗ ██╗    ██╗███╗   ██╗
//  ██╔════╝██╔══██╗██╔══██╗██║    ██║████╗  ██║
//  ███████╗██████╔╝███████║██║ █╗ ██║██╔██╗ ██║
//  ╚════██║██╔═══╝ ██╔══██║██║███╗██║██║╚██╗██║
//  ███████║██║     ██║  ██║╚███╔███╔╝██║ ╚████║
//  ╚══════╝╚═╝     ╚═╝  ╚═╝ ╚══╝╚══╝ ╚═╝  ╚═══╝
//                        ▼
    void spawn() {
        resetLockPolicy();
        // get tetr type from queue
        TetrominoType currentTetrominoType = nextQueue.remove();

        // try spawn, if gameover, notify controller
        if (!board.spawn(currentTetrominoType) || (scoreManager != null && scoreManager.haveAchievedGoal())) {
            listener.onGameOver();
            return;
        }
        if (autoMoveScheduler != null) autoMoveScheduler.onSpawn();


        checkLanded();
        board.updateShadow();
    }

//  ██╗      ██████╗  ██████╗██╗  ██╗
//  ██║     ██╔═══██╗██╔════╝██║ ██╔╝
//  ██║     ██║   ██║██║     █████╔╝
//  ██║     ██║   ██║██║     ██╔═██╗
//  ███████╗╚██████╔╝╚██████╗██║  ██╗
//  ╚══════╝ ╚═════╝  ╚═════╝╚═╝  ╚═╝
//          ▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄
    void lock() {
        resetLockPolicy();
        int nLinesCleared = board.clearCompleted();
        if (scoreManager != null) scoreManager.onLock(nLinesCleared);
        spawn();
    }

    public void hardDrop() {
        Tetromino dropping = board.getcurrentTetromino();
        moveDAS(Direction.DOWN);
        if (board.getcurrentTetromino() == dropping) lock();
    }
    /**
     * check if currentTetromino is landed, 
     * then if landed, start lockTimer
     * otherwise, end lock Timer
     */
    void checkLanded() {
        if (lockPolicy != null && board.getcurrentTetromino() != null) {
            lockPolicy.onPositionChanged(board.landed(board.getcurrentTetromino()), this::lock);
        }
    }

    private void resetLockPolicy() {
        if (lockPolicy != null) lockPolicy.reset();
    }

    /** Install on the game thread. The engine owns and closes the component. */
    public void setLockPolicy(LockPolicy policy) {
        if (lockPolicy == policy) return;
        if (lockPolicy != null) lockPolicy.close();
        lockPolicy = policy;
        checkLanded();
    }

    /** Install on the game thread; null disables automatic movement. */
    public void setAutoMoveScheduler(AutoMoveScheduler scheduler) {
        if (autoMoveScheduler == scheduler) return;
        if (autoMoveScheduler != null) autoMoveScheduler.dispose();
        autoMoveScheduler = scheduler;
        if (started && scheduler != null) scheduler.start();
    }

//   ██████╗ ████████╗██╗  ██╗███████╗██████╗ ███████╗
//  ██╔═══██╗╚══██╔══╝██║  ██║██╔════╝██╔══██╗██╔════╝
//  ██║   ██║   ██║   ███████║█████╗  ██████╔╝███████╗
//  ██║   ██║   ██║   ██╔══██║██╔══╝  ██╔══██╗╚════██║
//  ╚██████╔╝   ██║   ██║  ██║███████╗██║  ██║███████║
//   ╚═════╝    ╚═╝   ╚═╝  ╚═╝╚══════╝╚═╝  ╚═╝╚══════╝
    public void dispose() {
        started = false;
        setLockPolicy(null);
        setAutoMoveScheduler(null);
        if (scoreManager != null) {
            scoreManager.dispose();
            scoreManager = null;
        }
    }

    public ScoreManager getScoreManager() {
        return scoreManager;
    }

    public double getGravity() {
        return autoMoveScheduler == null ? 0 : autoMoveScheduler.getGravity();
     }
    
    public TetrominoType getHoldTetrominoType() {
        return this.holdTetrominoType;
    }

    public void setGravity(double gravity){
        if (autoMoveScheduler != null) autoMoveScheduler.setGravity(gravity);
    }

    public void setGameOverListener(GameOverListener listener) {
        this.listener = listener;
    }

    @Override
    public void addObserver(Observer observer) {
        observers.add(observer);
    }

    @Override
    public void removeObserver(Observer observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers() {
        for (Observer observer : observers) {
            observer.update();
        }
    }

    public void setScoreManager(ScoreManager scoreManager) {
        this.scoreManager = scoreManager;
    }

    public NextQueue getNextQueueManager() {
        return this.nextQueue;
    }

    public Board getBoard() {
        return board;
    }

}
