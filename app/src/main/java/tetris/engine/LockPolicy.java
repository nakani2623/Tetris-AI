package tetris.engine;

/** Optional automatic locking. Methods are called on the game thread. */
public interface LockPolicy extends AutoCloseable {
    void onPositionChanged(boolean landed, Runnable lock);
    void reset();
    @Override
    void close();
}
