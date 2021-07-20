package cn.lizhi.diyTomcat.Thread;


import java.util.HashSet;
import java.util.concurrent.TimeUnit;

// 自定义的线程池
public class ThreadPool {

    private BlockingQueue<Runnable> taskQueue;
    private final HashSet<Worker> workers = new HashSet<>();
    private int coreSize; // 核心线程数 - 线程池中含有最多的线程数量
    private long timeout; // 超时时间
    private TimeUnit timeUnit; // 时间单位
    private RejectPolicy<Runnable> rejectPolicy;

    /**
     * @param coreSize:线程池中的线程数
     * @param timeout:超时时间
     * @param timeUnit:时间单位
     * @param queueCapacity:任务队列容量
     * @param rejectPolicy:拒绝策略
     */
    public ThreadPool(int coreSize, long timeout, TimeUnit timeUnit, int queueCapacity, RejectPolicy<Runnable> rejectPolicy) {
        this.coreSize = coreSize;
        this.timeout = timeout;
        this.timeUnit = timeUnit;
        this.rejectPolicy = rejectPolicy;
        this.taskQueue = new BlockingQueue<>(queueCapacity);
    }

    public void execute(Runnable task) { // 主线程方法
        synchronized (workers) {
            if (workers.size() < coreSize) { // 线程池中存在空额，直接创建进程执行任务
                Worker worker = new Worker(task);
                workers.add(worker);
                worker.start(); // start是主线程的方法，执行该方法后，开辟一个线程，去执行它自己的任务，主线程并不会再这里等待子线程执行完了才继续，而是start方法执行完后，就直接向下执行，也就释放了锁，后续线程获得锁接着执行
            } else {
                taskQueue.tryPut(rejectPolicy, task); // 传入任务
            }
        }
    }


    class Worker extends Thread {
        private Runnable task;

        public Worker(Runnable task) {
            this.task = task;
        }

        @Override
        public void run() {
            while (task != null || (task = taskQueue.poll(timeout, timeUnit)) != null) {
                try {
                    task.run();
                } finally {
                    task = null;
                }
            }
            synchronized (workers) {

                workers.remove(this);
            }
        }
    }
}

