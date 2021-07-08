package cn.lizhi.diyTomcat.catalina;

import cn.lizhi.diyTomcat.util.ServerXMLUtil;

import java.util.List;

/**
 * 该类 用于处理Servlet请求
 */
public class Engine {

    private String defaultHost; // 默认的Host
    private List<Host> hosts; // host的容器，存储着全部的host
    private Service service; // 该Engine对应的Service对象 上层对象

    public Engine(Service service) {
        this.service = service;
        this.defaultHost = ServerXMLUtil.getEngineDefaultHost();
        this.hosts = ServerXMLUtil.getHosts(this);
        checkDefault();
    }


    /**
     * 检查 host中是否存在defaultHost
     */
    private void checkDefault() {
        if (getDefaultHost() == null) {
            throw new RuntimeException("the defaultHost" + defaultHost + "does not exist");
        }
    }

    public Host getDefaultHost() {
        for (Host host : hosts) {
            if (host.getName().equals(defaultHost)) {
                return host;
            }
        }
        return null;
    }
}
