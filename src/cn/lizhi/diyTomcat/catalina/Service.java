package cn.lizhi.diyTomcat.catalina;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.log.LogFactory;
import cn.lizhi.diyTomcat.util.ServerXMLUtil;

import java.util.List;

public class Service {
    /**
     * Service下包含 name,engine
     */
    private String name;
    private Engine engine;
    private Server server; // 该Service对应的Server
    private List<Connector> connectors; // 该服务下所有的Connection


    public void start() {
        init();
    }

    private void init() {
        TimeInterval timeInterval = DateUtil.timer();
        for (Connector connector : connectors) {
            connector.init(); // 初始化,进行日志记录connector加载的时间 每次遍历到一个connector对其进行初始化

        }
        LogFactory.get().info("Initialization processed in {} ms", timeInterval.intervalMs()); // 日志记录 connector初始化完成的时间
        for (Connector connector : connectors) { // 将connector全部开启 端口全部开启
            connector.start();
        }
    }

    public Service(Server server) {
        this.server = server; // 对应的服务项目
        this.name = ServerXMLUtil.getServiceName(); // service Name
        this.engine = new Engine(this);
        this.connectors = ServerXMLUtil.getConnectors(this); // 获取connector对象的集合
    }

    public String getName() {
        return name;
    }

    public Engine getEngine() {
        return engine;
    }

}
