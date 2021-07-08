package cn.lizhi.diyTomcat.http;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * ServletConfig是Servlet初始化的时候需要进行传递的参数 - 从下面定义的参数就能够看出，是对servlet进行初始化操作
 */
public class StandardServletConfig implements ServletConfig {

    private ServletContext servletContext; //  servletContext 容器
    private Map<String, String> initParameters; // 对应的servlet 需要 设定的参数
    private String servletName; // servletName

    /**
     *
     * @param servletContext servlet容器
     * @param servletName 需要初始化的servletName
     * @param initParameters 对应servlet的初始化参数
     */
    public StandardServletConfig(ServletContext servletContext, String servletName, Map<String, String> initParameters) {
        this.servletContext = servletContext;
        this.servletName = servletName;
        this.initParameters = initParameters;
        if (this.initParameters == null) {
            this.initParameters = new HashMap<>();
        }
    }

    @Override
    public String getServletName() {
        return servletName;
    }

    @Override
    public ServletContext getServletContext() {
        return servletContext;
    }

    @Override
    public String getInitParameter(String name) {
        return initParameters.get(name);
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return Collections.enumeration(initParameters.keySet());
    }
}
