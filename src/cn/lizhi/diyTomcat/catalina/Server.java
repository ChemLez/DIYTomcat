package cn.lizhi.diyTomcat.catalina;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.log.LogFactory;
import cn.hutool.system.SystemUtil;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class Server {

    private Service service; // Service作为属性，请求进入到Service进行处理

    public Server() { // 无参构造器
        this.service = new Service(this); // 目的将Server,Service,Engine,Host进行同一套资源的关联绑定
    }

    public void start() {
        TimeInterval timeInterval = DateUtil.timer();
        logJVM(); // 日志打印 JVM信息
        init(); // 初始化
        LogFactory.get().info("Server start up in {} ms", timeInterval.intervalMs()); // 整个服务启动所花费的时间
    }

    private void init() {
        service.start();
    }

    private static void logJVM() {
        /**
         * 用于打印JVM的相关信息
         */
        // 存放JVM相关信息
        Map<String, String> infos = new LinkedHashMap<>();
        // 这里是自定义服务端信息
        infos.put("Server version", "Chemlez Diy Tomcat/1.0.1"); // Server端信息
        infos.put("Server built", "2020-5-12 15:13:30");
        infos.put("Server number", "1.0.1");
        // 以下为正式的JVM信息
        infos.put("OS Name\t", SystemUtil.get("os.name"));
        infos.put("OS Version", SystemUtil.get("os.version"));
        infos.put("Architecture", SystemUtil.get("os.arch"));
        infos.put("Java Home", SystemUtil.get("java.home"));
        infos.put("JVM Version", SystemUtil.get("java.runtime.version"));
        infos.put("JVM Vendor", SystemUtil.get("java.vm.specification.vendor"));
        Set<String> keys = infos.keySet();
        for (String key : keys) {
            LogFactory.get().info(key + ":\t\t" + infos.get(key)); // 这里获取日志对象的方式是 LogFactory.get()，将日志信息切入 将 JVM相关信息作为 info

        }
    }
}
