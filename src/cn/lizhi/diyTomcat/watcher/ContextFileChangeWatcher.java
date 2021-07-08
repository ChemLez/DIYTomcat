package cn.lizhi.diyTomcat.watcher;

import cn.hutool.core.io.watch.WatchMonitor;
import cn.hutool.core.io.watch.WatchUtil;
import cn.hutool.core.io.watch.Watcher;
import cn.hutool.log.LogFactory;
import cn.lizhi.diyTomcat.catalina.Context;

import java.nio.file.Path;
import java.nio.file.WatchEvent;

/**
 * 文件 改变的监听器,用于监听特定的context
 */
public class ContextFileChangeWatcher {

    private WatchMonitor monitor; // monitor是真正起作用的监听器
    private boolean stop = false; // 表示监听是否继续


    public ContextFileChangeWatcher(Context context) { // 被监听的context
        /**
         * context.getDocBase() 代表监听的文件夹 其实就是监听 该对应的context对象 docBase目录下的文件变化
         * Integer.MAX_VALUE 代表监听的深入；0或者1代表只监听当前目录，而不是监听子目录。
         * watcher 声明的方法，就是当文件发生创建，修改，删除 和 出错的时候。 所谓的出错比如文件不能删除，磁盘错误等等。 对这些方法进行监听
         */
        this.monitor = WatchUtil.createAll(context.getDocBase(), Integer.MAX_VALUE, new Watcher() { // 创建监听器

            private void dealWith(WatchEvent<?> event) {
                synchronized (ContextFileChangeWatcher.class) { // 该类自身作为锁对象 该处理是一个异步处理 当文件发生变化，会发过来很多次事件，因此需要对这些事件一个一个的进行处理
                    String fileName = event.context().toString(); // 取得当前发生变化的文件或者文件夹名称
                    if (stop) { // 当stop的时候，表示已经重载过了，后面再来的消息就闲置 不再处理; 当重新加载context后，即重新创建了context对象后，该stop又会置为false，供发生的变化，能继续被监听到(其实就是一个新的ContextFileChangeWatcher监听对象)
                        return;
                    }
                    if (fileName.endsWith(".jar") || fileName.endsWith(".class") || fileName.endsWith(".xml")) { // 这些文件发生了变化
                        stop = true; // 暂停监听
                        LogFactory.get().info(ContextFileChangeWatcher.this + "检测到了Web应用下的重要变化{}", fileName);
                        context.reload(); // 重新加载该context
                    }
                }
            }

            @Override
            public void onCreate(WatchEvent<?> watchEvent, Path path) {  // 创建
                dealWith(watchEvent);

            }

            @Override
            public void onModify(WatchEvent<?> watchEvent, Path path) { // 修改
                dealWith(watchEvent);

            }

            @Override
            public void onDelete(WatchEvent<?> watchEvent, Path path) { // 删除
                dealWith(watchEvent);

            }

            @Override
            public void onOverflow(WatchEvent<?> watchEvent, Path path) { // 出错(删除权限、磁盘错误)
                dealWith(watchEvent);

            }
        });
        this.monitor.setDaemon(true); // 将该线程设定为守护线程，当所有非守护线程关闭时，该守护线程也会自动退出
    }

    public void start() {
        monitor.start();
    }

    public void stop() {
        monitor.close();
    }


}
