package cn.lizhi.diyTomcat.Thread;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
// 自定义的阻塞队列
public class BlockingQueue<T> {

    private Deque<T> queue = new ArrayDeque<>();
    private ReentrantLock lock = new ReentrantLock();
    private Condition fullWaitSet = lock.newCondition(); // 生产者等待条件
    private Condition emptyWaitSet = lock.newCondition(); // 消费者等待条件
    private int capacity; // 任务数

    public BlockingQueue(int capacity) {
        this.capacity = capacity;
    }

    // 阻塞获取任务
    public T take() {
        try {
            lock.lock();
            while (queue.isEmpty()) {
                try {
                    emptyWaitSet.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            T t = queue.removeFirst();
            fullWaitSet.signal();
            return t;
        } finally {
            lock.unlock();
        }
    }

    // 阻塞添加任务
    public void put(T task) {
        lock.lock();
        try {
            while (queue.size() == capacity) { // 任务队列已经满了
                try {
                    fullWaitSet.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            queue.addLast(task);
            emptyWaitSet.signal();
        } finally {
            lock.unlock();
        }
    }

    // 带超时的等待 - 任务获取
    public T poll(long timeout, TimeUnit unit) {
        lock.lock();
        try {
            timeout = unit.toNanos(timeout); // 等待超时时间
            while (queue.isEmpty()) { // 队列为空
                try {
                    if (timeout <= 0) {
                        return null;
                    }
                    long usedTime = emptyWaitSet.awaitNanos(timeout); // 消耗的时间
                    timeout -= usedTime;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            T t = queue.getFirst(); // 获取到任务
            fullWaitSet.signal(); // 唤醒生产者线程
            return t;
        } finally {
            lock.unlock();
        }
    }

    // 带超时的获取
    public boolean offer(T task, long timeout, TimeUnit unit) {
        lock.lock();
        try {
            timeout = unit.toNanos(timeout);
            while (queue.size() == capacity) { // 队列已经满
                if (timeout <= 0) {
                    return false;
                }
                try {
                    long usedTime = fullWaitSet.awaitNanos(timeout);
                    timeout -= usedTime;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            queue.addLast(task);
            emptyWaitSet.signal();
            return true;
        } finally {
            lock.unlock();
        }
    }

    public int size() {
        lock.lock();
        try {
            return queue.size();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 待拒绝的策略
     * @param rejectPolicy
     * @param task
     */
    public void tryPut(RejectPolicy<T> rejectPolicy, T task) {
        lock.lock();
        try {
            if (queue.size() == capacity) { // 队列已经满了，无法添加，选用策略模式
                rejectPolicy.reject(this, task); // 将该任务及对了交给具体的拒绝模式去处理
            } else {
                queue.addLast(task);
                emptyWaitSet.signal();
            }
        } finally {
            lock.unlock();
        }
    }
}