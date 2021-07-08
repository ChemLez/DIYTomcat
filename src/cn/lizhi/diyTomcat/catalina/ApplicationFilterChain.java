package cn.lizhi.diyTomcat.catalina;

import cn.hutool.core.util.ArrayUtil;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public class ApplicationFilterChain implements FilterChain {

    /**
     * 过滤器链 这里写处理过滤器的逻辑
     *
     * @param servletRequest
     * @param servletResponse
     * @throws IOException
     * @throws ServletException
     */

    private Filter[] filters;
    private Servlet servlet;
    private int pos;

    public ApplicationFilterChain(List<Filter> filters, Servlet servlet) {
        this.filters = ArrayUtil.toArray(filters, Filter.class);
        this.servlet = servlet;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        if (pos < filters.length) { // 逐个执行需要的过滤器
            Filter filter = filters[pos++];
            filter.doFilter(servletRequest, servletResponse, this);
        } else { // 过滤器对request和response增强完毕后，开始执行service方法
            servlet.service(request, response);
        }
    }
}
