package cn.lizhi.diyTomcat.Thread;

public interface RejectPolicy<T> {
    void reject(BlockingQueue<T> blockingQueue, T task);
}
