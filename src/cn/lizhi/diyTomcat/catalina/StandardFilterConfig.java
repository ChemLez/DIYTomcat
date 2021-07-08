package cn.lizhi.diyTomcat.catalina;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import java.util.*;

/**
 * 主要作用就是用来存放Filter的初始化参数 对Filter进行初始化操作
 */
public class StandardFilterConfig implements FilterConfig {

    private ServletContext servletContext;
    private Map<String, String> initParameters;
    private String filterName;

    public StandardFilterConfig(ServletContext servletContext, Map<String, String> initParameters, String filterName) {
        this.servletContext = servletContext;
        this.initParameters = initParameters;
        this.filterName = filterName;
        if (this.initParameters == null) {
            this.initParameters = new HashMap<>();
        }

    }

    @Override
    public String getFilterName() {
        return filterName;
    }

    @Override
    public ServletContext getServletContext() {
        return servletContext;
    }

    @Override
    public String getInitParameter(String s) {
        return initParameters.get(s);
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        Set<String> keySet = initParameters.keySet();
        return Collections.enumeration(keySet);
    }
}
