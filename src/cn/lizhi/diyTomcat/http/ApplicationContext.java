package cn.lizhi.diyTomcat.http;

import cn.lizhi.diyTomcat.catalina.Context;

import java.io.File;
import java.util.*;

public class ApplicationContext extends BaseServlet {

    private Map<String, Object> attributesMap; // 用于存放属性
    private Context context; // 内置Context

    public ApplicationContext(Context context) {
        this.attributesMap = new HashMap<>();
        this.context = context;
    }

    @Override
    public void removeAttribute(String name) {
        attributesMap.remove(name);
    }

    @Override
    public void setAttribute(String name,Object object) {
        attributesMap.put(name, object);
    }

    @Override
    public Object getAttribute(String name) {
        return attributesMap.getOrDefault(name, null);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        Set<String> keys = attributesMap.keySet();
        return Collections.enumeration(keys);
    }

    @Override
    public String getRealPath(String path) {
        return new File(context.getDocBase(), path).getAbsolutePath(); // 资源在服务器端的真实路径
    }

}
