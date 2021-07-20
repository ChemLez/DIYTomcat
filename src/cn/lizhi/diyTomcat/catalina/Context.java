package cn.lizhi.diyTomcat.catalina;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.LogFactory;
import cn.lizhi.diyTomcat.watcher.ContextFileChangeWatcher;
import cn.lizhi.diyTomcat.classloader.WebappClassLoader;
import cn.lizhi.diyTomcat.exception.WebConfigDuplicatedException;
import cn.lizhi.diyTomcat.http.ApplicationContext;
import cn.lizhi.diyTomcat.http.StandardServletConfig;
import cn.lizhi.diyTomcat.util.ContextXMLUtil;
import org.apache.jasper.JspC;
import org.apache.jasper.compiler.JspRuntimeContext;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import java.io.File;
import java.util.*;

/**
 * 代表一个应用
 */
public class Context {

    /**
     * path - 访问的路径
     * docBase - 服务器真实路径
     * 目的是将 path和docBase进行映射
     **/
    private String path; // 访问路径,资源路径 -- 文件夹 即 虚拟目录
    private String docBase; // 对应在文件系统中的位置 - 磁盘上的绝对路径 即文件系统目录 多个虚拟目录 可以映射一个 文件系统目录
    private File contextWebXmlFile; // 存放Servlet的映射信息。

    private Host host; // 该context对应的host
    private boolean reloadable; // 是否支持重加载

    // Servlet 映射的方式
    private Map<String, String> url_servletClassName; // 地址对应Servlet的类名
    private Map<String, String> url_servletName; // 地址对应Servlet的名称
    private Map<String, String> servletName_className; // Servlet的名称对应类名
    private Map<String, String> className_servletName; // Servlet类名对应名称
    private Map<String, Map<String, String>> servlet_className_init_params; // 用于存放初始化信息 - 每个servlet的初始化信息.key:servletClassName，value:其对应的init-params

    // Filter 映射方法
    private Map<String, List<String>> url_filterClassName; //地址对应Filter类名
    private Map<String, List<String>> url_FilterNames; //地址对应filter的名称
    private Map<String, String> filterName_className; // filter的名称对应类名
    private Map<String, String> className_filterName; // 类名对应filter名称
    private Map<String, Map<String, String>> filter_className_init_params; // 每个filter对应的初始化参数 filterClassName:value

    private WebappClassLoader webappClassLoader; //  一个web应用对应一个自己独立的WebappClassLoader。
    private ContextFileChangeWatcher contextFileChangeWatcher; // 该context对应的监听器
    private ServletContext servletContext; // 该context对应的servlet容器 代表整个应用 context和servletContext关联
    private Map<Class<?>, HttpServlet> servletPool; // 存放servlet的池子
    private List<String> loadOnStartupServletClassNames; // 用于存放哪些类需要做自启动
    private Map<String, Filter> filterPool; // 存放filterPool的池子

    private List<ServletContextListener> listeners; // 监听器集合 - 监听Context的生命周期


    public Context(String path, String docBase, Host host, boolean reloadable) {

        TimeInterval timeInterval = DateUtil.timer(); // 开始计时
        this.host = host;
        this.reloadable = reloadable;
        this.path = path;
        this.docBase = docBase;
        this.servletContext = new ApplicationContext(this);
        this.servletPool = new HashMap<>(); // 初始化该servletPool
        this.servlet_className_init_params = new HashMap<>();
        this.loadOnStartupServletClassNames = new ArrayList<>();
        this.filterPool = new HashMap<>();

        this.contextWebXmlFile = new File(docBase, ContextXMLUtil.getWatchedResource()); // docBase/WEB-INF/web.xml 获取到该文件对象，该句开始可以获得外部的web应用对应的servlet映射
        this.url_servletClassName = new HashMap<>();
        this.url_servletName = new HashMap<>();
        this.servletName_className = new HashMap<>();
        this.className_servletName = new HashMap<>();

        // Filter的映射集合的初始化
        this.url_filterClassName = new HashMap<>();
        this.url_FilterNames = new HashMap<>();
        this.filterName_className = new HashMap<>();
        this.className_filterName = new HashMap<>();
        this.filter_className_init_params = new HashMap<>();

        ClassLoader commonClassLoader = Thread.currentThread().getContextClassLoader(); // Thread.currentThread().getContextClassLoader() 就可以获取到 Bootstrap 里通过 Thread.currentThread().setContextClassLoader(commonClassLoader); 设置的 commonClassLoader.只有主线程进行了设置
        this.webappClassLoader = new WebappClassLoader(docBase, commonClassLoader); // 该context对应的WebappClassLoader 这样WebappClassLoader 是在 commonClassLoader层级之下的
        this.listeners = new ArrayList<>();

        LogFactory.get().info("Deploying web application directory {}", this.docBase);
        deploy(); // 将上面的容器进行填充
        LogFactory.get().info("Deployment of web application directory {} has finished in {} ms", this.docBase, timeInterval.intervalMs());
    }

    // 返回WebClassLoader
    public WebappClassLoader getWebappClassLoader() {
        return webappClassLoader;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getDocBase() {
        return docBase;
    }

    public boolean isReloadable() {
        return reloadable;
    }

    public void setReloadable(boolean reloadable) {
        this.reloadable = reloadable;
    }

    public ServletContext getServletContext() {
        return servletContext;
    }

    // 通过url获得类名
    public String getServletClassName(String url) {
        return url_servletClassName.get(url);
    }

    public void setDocBase(String docBase) {
        this.docBase = docBase;
    }

    /**
     * 停止方法 —— 把 webappClassLoader 和 contextFileChangeWatcher停止了
     */
    public void stop() {
        fireEvent("destroy");
        webappClassLoader.stop(); // 关闭该context的类加载器
        contextFileChangeWatcher.stop(); // 暂停监听
        destroyServlets(); // 销毁全部的servlet
    }

    /**
     * 重新加载host -- 达到对context新建的目的
     */
    public void reload() {
        host.reload(this);
    }

    /**
     * 根据类对象来获取servlet对象,继而将请求request,response交给该servlet的service方法进行处理
     *
     * @param clazz: servletClass 需要返回的servlet 类对象
     * @return
     */
    public synchronized HttpServlet getServlet(Class<?> clazz) throws IllegalAccessException, InstantiationException, ServletException {
        HttpServlet servlet = servletPool.get(clazz); // 通过servletPool池来获取servlet对象
        if (servlet == null) { // 如果该servlet对象不存在，就通过clazz创建该servlet实例,并设置到servletPool中，作为后续使用
            servlet = (HttpServlet) clazz.newInstance(); // 创建servlet对象
            String className = clazz.getName(); // 获取class类名
            String servletName = className_servletName.get(className); // 返回的servletName
            Map<String, String> initParams = servlet_className_init_params.get(className); // 该servlet对应的initParams 此时map为：initName - initValue
            ServletConfig servletConfig = new StandardServletConfig(servletContext, servletName, initParams); // 用于对新建的servlet进行参数的配置
            servlet.init(servletConfig); // 执行servlet的init过程 - 这里是因为第一次加载(创建)servlet。即通过servletConfig对servlet进行初始化操作 对该servlet进行初始化操作；例如servletName
            servletPool.put(clazz, servlet);
        }
        return servlet;
    }

    /**
     * 用于解析需要做自启动的类
     *
     * @param d
     */
    public void parseLoadOnStartup(Document d) {
        Elements es = d.select("load-on-startup");
        for (Element e : es) {
            String loadOnStartupServletClassName = e.parent().select("servlet-class").text();
            loadOnStartupServletClassNames.add(loadOnStartupServletClassName);
        }
    }

    public void handleLoadOnStartup() {
        try {
            for (String loadOnStartupServletClassName : loadOnStartupServletClassNames) {
                Class<?> clazz = webappClassLoader.loadClass(loadOnStartupServletClassName); // 使用自己的webappClassLoader 对该类进行加载
                getServlet(clazz); // servlet类的加载
            }
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | ServletException e) {
            e.printStackTrace();
        }
    }

    /**
     * 用于销毁该context中全部的servlet
     */
    private void destroyServlets() {
        Collection<HttpServlet> servlets = servletPool.values();
        for (HttpServlet servlet : servlets) {
            servlet.destroy();
        }
    }

    /**
     * 从web.xml中解析出初始化参数
     *
     * @param d
     */
    private void parseServletInitParams(Document d) {
        Elements servletClassNameElements = d.select("servlet-class"); // 取出该servlet-class 中的text
        for (Element servletClassNameElement : servletClassNameElements) { // 遍历每个servletClassName
            String servletClassName = servletClassNameElement.text();
            Elements initElements = servletClassNameElement.parent().select("init-param");
            if (initElements.isEmpty()) {
                continue;
            }
            Map<String, String> initParams = new HashMap<>(); // 用于存储每个对应的servletClassName中的初始值
            for (Element initElement : initElements) { // 取出每个init-params中的值
                String name = initElement.select("param-name").get(0).text();
                String value = initElement.select("param-value").get(0).text();
                initParams.put(name, value);
            }
            this.servlet_className_init_params.put(servletClassName, initParams);
        }
        System.out.println("class_name_init_params:" + servlet_className_init_params);
    }

    /**
     * 用于解析web.xml 填入上面的属性集合中 - 当前web项目的web.xml
     * 解析servlet
     *
     * @param d web.xml dom文件
     */
    private void parseServletMapping(Document d) {

        // url_ServletName
        Elements mappingUrlElements = d.select("servlet-mapping url-pattern"); // 所有的url 虚拟路径
        for (Element mappingUrlElement : mappingUrlElements) {
            String urlPattern = mappingUrlElement.text();
            String urlServletName = mappingUrlElement.parent().select("servlet-name").text();
            url_servletName.put(urlPattern, urlServletName); // 将url和servletName做映射
        }

        // servletName_className / className_servletName
        Elements servletNameElements = d.select("servlet servlet-name"); // 目的是将 servlet-name 和servlet-class进行映射
        for (Element servletNameElement : servletNameElements) {
            String servletName = servletNameElement.text();
            String servletClass = servletNameElement.parent().select("servlet-class").text();
            servletName_className.put(servletName, servletClass); // 将servletName和servletClass做映射
            className_servletName.put(servletClass, servletName); // 将servletName和url做映射
        }

        // url_servletClassName
        Set<String> urls = url_servletName.keySet();
        for (String url : urls) { // 取出每个ServletName.这个循环的目的是将，url_pattern和servletClassName 做映射关系
            String servletName = url_servletName.get(url);
            String servletClassName = servletName_className.get(servletName);
            url_servletClassName.put(url, servletClassName); // 这里直接将url和servletName做映射
        }
    }

    /**
     * 用于解析web.xml中的filter信息
     *
     * @param d
     */
    private void parseFilterMapping(Document d) {
        // filter_url_name
        Elements mappingURLElements = d.select("filter-mapping url-pattern");
        for (Element mappingURLElement : mappingURLElements) {
            String urlPattern = mappingURLElement.text();
            String filterName = mappingURLElement.parent().select("filter-name").first().text();
            List<String> filterNamesList = url_FilterNames.computeIfAbsent(urlPattern, k -> new ArrayList<>());
            filterNamesList.add(filterName);
        }
        // class_name_filter_name
        Elements filterClassElements = d.select("filter filter-class");
        for (Element filterClassElement : filterClassElements) {
            String filterClassName = filterClassElement.text();
            String filterName = filterClassElement.parent().select("filter-name").first().text();
            className_filterName.put(filterClassName, filterName);
            filterName_className.put(filterName, filterClassName);
        }
        // url_filterClassname  通过上面的三个集合来进行当前这个集合的初始化 url -> filterClassName 对应
        Set<String> urls = url_FilterNames.keySet(); // 取出所有的url
        for (String url : urls) {
            List<String> filterNames = url_FilterNames.get(url); // 通过url获取所有的filterName
            List<String> filterClassNameList = url_filterClassName.computeIfAbsent(url, k -> new ArrayList<>());
            for (String filterName : filterNames) {
                String filterClassName = filterName_className.get(filterName); // filterName -> className做对应
                filterClassNameList.add(filterClassName);
            }
        }
    }

    /**
     * 解析filter对应的参数
     *
     * @param d
     */
    private void parseFilterInitParams(Document d) {
        Elements initParams = d.select("filter init-param");
        if (initParams.isEmpty()) { // 配置文件中，不存在filter的初始化参数
            return;
        }
        for (Element initParam : initParams) {
            String filterClassName = initParam.parent().select("filter-class").first().text();
            Map<String, String> initParamList = filter_className_init_params.get(filterClassName);
            if (initParamList == null) {
                initParamList = new HashMap<>();
                filter_className_init_params.put(filterClassName, initParamList);
            }
            Elements initParamElements = initParam.parent().select("init-param");
            for (Element initParamElement : initParamElements) {
                String param_name = initParamElement.select("param-name").text();
                String param_value = initParamElement.select("param-value").text();
                initParamList.put(param_name, param_value);
            }
        }
    }

    /**
     * Filter的初始化部分 - 即创建filter对象并存放在filterPool中
     */
    private void initFilter() {
        try {
            Set<String> classNames = className_filterName.keySet();
            for (String className : classNames) {
                Class<?> clazz = this.getWebappClassLoader().loadClass(className); // 该filter对应的类对象
                Map<String, String> initParameters = filter_className_init_params.get(className); // 当前filter对应的初始化参数
                String filterName = className_filterName.get(className);
                FilterConfig filterConfig = new StandardFilterConfig(servletContext, initParameters, filterName);
                Filter filter = filterPool.get(className);
                if (filter == null) {
                    filter = (Filter) ReflectUtil.newInstance(clazz); // 通过反射创建filter对象
                    filter.init(filterConfig); // 调用该过滤器的初始化方法
                    filterPool.put(className, filter);
                }
            }
        } catch (ClassNotFoundException | ServletException e) {
            e.printStackTrace();
        }
    }

    /**
     * 过滤器的匹配模式 - 三种匹配模式
     * @param pattern 模式匹配
     * @param uri 资源路径
     * @return
     */
    private boolean match(String pattern, String uri) {

        // 完全匹配 - 只针对指定的资源 进行过滤
        if (StrUtil.equals(pattern, uri)) {
            return true;
        }
        // /* 拦截所有请求
        if (StrUtil.equals(pattern, "/*")) {
            return true;
        }
        // 后缀名为 /*.jsp文件进行过滤
        if (StrUtil.startWith(pattern, "/*.")) {
            String patternExtName = StrUtil.subAfter(pattern, ".", false); // 扩展名
            String uriExName = StrUtil.subAfter(uri, ".", false); // 具体资源的扩展名
            return StrUtil.equals(patternExtName, uriExName); // 判断是否是指针扩展名资源的过滤器
        }
        return false;
    }

    /**
     * 获取匹配成功的过滤器集合
     * @param uri 待匹配
     * @return
     */
    public List<Filter> getMatchedFilters(String uri) {
        List<Filter> filters = new ArrayList<>();
        Set<String> patters = url_filterClassName.keySet(); // patterns
        Set<String> matchedFilterClassNames = new HashSet<>(); // 匹配成功的filterClassName
        for (String patter : patters) {
            if (match(patter, uri)) {
                List<String> filterClassNames = url_filterClassName.get(patter);
                matchedFilterClassNames.addAll(filterClassNames);
            }
        }
        // 返回filters集合
        for (String matchedFilterClassName : matchedFilterClassNames) {
            Filter filter = filterPool.get(matchedFilterClassName);
            filters.add(filter);
        }
        return filters;
    }

    public void addListener(ServletContextListener listener) {
        this.listeners.add(listener);
    }

    /**
     * 从web.xml重新扫描监听的类 加载listener到list集合中
     */
    private void loadListeners() {
        try {
            if (!contextWebXmlFile.exists()) { // 未配置web.xml文件
                return;
            }
            String xml = FileUtil.readUtf8String(contextWebXmlFile);
            Document d = Jsoup.parse(xml);
            Elements e = d.select("listener listener-class");
            for (Element element : e) {
                String listenerClassName = element.text();
                Class<?> clazz = webappClassLoader.loadClass(listenerClassName);
                ServletContextListener listener = (ServletContextListener) clazz.newInstance();
                addListener(listener);
            }
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * 触发事件 采取的动作
     * @param type：事件类型
     */
    private void fireEvent(String type) {
        ServletContextEvent event = new ServletContextEvent(servletContext); // ServletContextEvent 事件
        for (ServletContextListener listener : listeners) { // 执行所有的监听器
            if ("init".equals(type)) {
                listener.contextInitialized(event);
            }
            if ("destroy".equals(type)) {
                listener.contextDestroyed(event);
            }
        }
    }

    /**
     * 判断是否有重复,存在重复就抛出异常
     *
     * @throws WebConfigDuplicatedException
     */
    private void checkDuplicated() throws WebConfigDuplicatedException {
        String xml = FileUtil.readUtf8String(contextWebXmlFile);
        Document d = Jsoup.parse(xml);
        checkDuplicated(d, "servlet-mapping url-pattern", "servlet url 重复,请保持其唯一性:{} ");
        checkDuplicated(d, "servlet servlet-name", "servlet 名称重复,请保持其唯一性:{} ");
        checkDuplicated(d, "servlet servlet-class", "servlet 类名重复,请保持其唯一性:{} ");

    }

    /**
     * 判断是否存在重复，如果存在重复，就抛出异常
     *
     * @param d
     * @param mapping
     * @param desc
     */
    private void checkDuplicated(Document d, String mapping, String desc) throws WebConfigDuplicatedException {

        Elements elements = d.select(mapping);
        // 这里判断重复的逻辑是放入一个集合中，然后将集合排序之后看两相邻的元素是否相同
        List<String> contents = new ArrayList<>();
        for (Element element : elements) {
            contents.add(element.text());
        }
        Collections.sort(contents);
        for (int i = 0; i < contents.size() - 1; i++) {
            String contentPre = contents.get(i);
            String contentNext = contents.get(i + 1);
            if (contentPre.equals(contentNext)) {
                throw new WebConfigDuplicatedException(StrUtil.format(desc, contentPre)); // 抛出相同的异常
            }
        }
    }

    /**
     * 先判断是否存在web.xml，没有就直接返回
     * 然后判断是否重复
     * 无重复，接着进行web.xml的解析
     */
    private void init() {
        if (!contextWebXmlFile.exists()) { // 该文件不存在 - web文件不存在
            return;
        }
        try {
            checkDuplicated(); // 检查是否存在重复
        } catch (WebConfigDuplicatedException e) {
            e.printStackTrace();
            return;
        }
        String xml = FileUtil.readUtf8String(contextWebXmlFile);
        Document d = Jsoup.parse(xml); // 解析 填充容器
        parseServletMapping(d); // servlet的映射关系解析
        parseServletInitParams(d); // 该servlet对应的初始化参数解析
        parseLoadOnStartup(d); // 获取自启动的servlet
        handleLoadOnStartup(); // 加载自启动的servlet
        parseFilterMapping(d); // 解析filter
        parseFilterInitParams(d); // filter参数解析
        initFilter(); // 初始化Filter
        loadListeners(); // 加载监听器 - Listener
        fireEvent("init");
    }

    /**
     * 加载初始化方法 并打印日志
     */
    private void deploy() {
        init();
        if (reloadable) { // 支持重加载 - 对该context进行监听
            this.contextFileChangeWatcher = new ContextFileChangeWatcher(this);
            contextFileChangeWatcher.start();
        }
        // 进行了JspRuntimeContext的初始化;就是为了能够在jsp所转换的 java 文件里的 javax.servlet.jsp.JspFactory.getDefaultFactory() 这行能够有返回值
        JspC c = new JspC();
        new JspRuntimeContext(servletContext, c);
    }
}