package cn.lizhi.diyTomcat.http;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * 创建会话 - session
 */
public class StandardSession implements HttpSession {

    private Map<String, Object> attributesMap; // 用于在session中存放数据
    private String id; // 当前session的唯一id
    private long creationTime; // 创建时间 单位 ms
    private long lastAccessedTime; // 最后一次访问时间 单位 ms
    private ServletContext servletContext; // servletContext；ServletContext对象可以被web应用程序中的所有Servlet访问，可以解决不同用户之间的数据共享 - context
    private int maxInactiveInterval; // 最大持续时间分钟数  lastAccessedTime 用于对 session 自动失效。 一般默认是30分钟，如果不登录， session 就会自动失效了。

    public StandardSession(String jsessionid, ServletContext servletContext) {
        this.attributesMap = new HashMap<>();
        this.id = jsessionid;
        this.servletContext = servletContext;
        this.creationTime = System.currentTimeMillis();
    }

    @Override
    public long getCreationTime() {
        return creationTime;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public long getLastAccessedTime() {
        return lastAccessedTime;
    }

    @Override
    public ServletContext getServletContext() {
        return servletContext;
    }

    @Override
    public void setMaxInactiveInterval(int maxInactiveInterval) {
        this.maxInactiveInterval = maxInactiveInterval;
    }

    @Override
    public int getMaxInactiveInterval() {
        return maxInactiveInterval;
    }

    @Override
    public HttpSessionContext getSessionContext() {
        return null;
    }

    @Override
    public Object getAttribute(String name) {
        return attributesMap.get(name);
    }

    @Override
    public Object getValue(String name) {
        return attributesMap.get(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(attributesMap.keySet());
    }

    @Override
    public String[] getValueNames() {
        return new String[0];
    }

    @Override
    public void setAttribute(String name, Object object) {
        attributesMap.put(name, object);
    }

    @Override
    public void putValue(String s, Object o) {

    }

    @Override
    public void removeAttribute(String name) {
        attributesMap.remove(name);
    }

    @Override
    public void removeValue(String s) {

    }

    @Override
    public void invalidate() {
        attributesMap.clear();
    }

    @Override
    public boolean isNew() { // 是否创建新的session
        return creationTime == lastAccessedTime;
    }

    public void setLastAccessedTime(long lastAccessedTime) {
        this.lastAccessedTime = lastAccessedTime;
    }
}
