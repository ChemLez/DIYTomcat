package cn.lizhi.diyTomcat.watcher;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.watch.WatchMonitor;
import cn.hutool.core.io.watch.WatchUtil;
import cn.hutool.core.io.watch.Watcher;
import cn.lizhi.diyTomcat.catalina.Host;
import cn.lizhi.diyTomcat.util.Constant;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.WatchEvent;

import static cn.hutool.core.io.watch.WatchMonitor.ENTRY_CREATE;

public class WarFileWatcher {

    private WatchMonitor monitor;

    public WarFileWatcher(Host host) {
        this.monitor = WatchUtil.createAll(Constant.WEBAPPS_FOLDER, 1, new Watcher() {
            private void dealWith(WatchEvent<?> event, Path currentPath) {

                synchronized (WarFileWatcher.class) {
                    String fileName = event.context().toString(); // 文件夹名
                    if (fileName.toLowerCase().endsWith(".war") && ENTRY_CREATE.equals(event.kind())) { // .war包 还是 onCreate事件触发
                        File warFile = FileUtil.file(Constant.WEBAPPS_FOLDER, fileName); // 需要加载的 war包 对象
                        host.loadWar(warFile);
                    }
                }
            }

            @Override
            public void onCreate(WatchEvent<?> watchEvent, Path path) {
                dealWith(watchEvent, path);
            }

            @Override
            public void onModify(WatchEvent<?> watchEvent, Path path) {
                dealWith(watchEvent, path);
            }

            @Override
            public void onDelete(WatchEvent<?> watchEvent, Path path) {
                dealWith(watchEvent, path);
            }

            @Override
            public void onOverflow(WatchEvent<?> watchEvent, Path path) {
                dealWith(watchEvent, path);
            }
        });
    }

    public void start() {
        monitor.start();
    }

}
