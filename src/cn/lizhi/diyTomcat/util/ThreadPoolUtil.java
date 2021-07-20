package cn.lizhi.diyTomcat.util;

import cn.lizhi.diyTomcat.Thread.BlockingQueue;
import cn.lizhi.diyTomcat.Thread.RejectPolicy;
import cn.lizhi.diyTomcat.Thread.ThreadPool;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadPoolUtil {

    //        private static ThreadPoolExecutor threadPool = new ThreadPoolExecutor(20, 100, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(10));
    private static ThreadPool threadPool = new ThreadPool(100, 60, TimeUnit.SECONDS, 10, (blockingQueue, task) -> {
        // 自己去执行这个任务
        new Thread(task).start();
    });

    public synchronized static void run(Runnable r) { // 可以执行不同的任务
        threadPool.execute(r);
    }
}
