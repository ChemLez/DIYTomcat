package cn.lizhi.diyTomcat.http;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.db.Session;
import cn.lizhi.diyTomcat.catalina.Connector;
import cn.lizhi.diyTomcat.catalina.Context;
import cn.lizhi.diyTomcat.catalina.Engine;
import cn.lizhi.diyTomcat.catalina.Service;
import cn.lizhi.diyTomcat.util.MiniBrowser;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.sql.Struct;
import java.util.*;


public class Request extends BaseRequest {

    private String requestString;
    private String uri;
    private Socket socket; // 客户端的socket
    private Context context; // 即context对象先与request对象实例化完成。这里是找出该request所对应的context对象
    //    private Engine engine; // 从engine中获取host
//    private Service service; // 其对应的Service
    private Connector connector;
    private String method;
    private String queryString; // 请求参数
    private Map<String, String[]> parameterMap; // 请求参数的集合
    private Map<String, String> headMap; // 存放请求头的集合
    private Cookie[] cookies; // 客户端携带的cookies
    private HttpSession session; // 一个客户端对应着一个session，这里自然一个请求就对应着一个session request中的session来自于SessionManger
    private boolean forwarded;// 用于表明当前的请求是否是 转发请求
    private Map<String, Object> attributesMap; // 用于存放参数 - 后天设定

    public Request(Socket socket, Connector connector) throws IOException {
        this.connector = connector;
        this.socket = socket;
        this.parameterMap = new HashMap<>();
        this.headMap = new HashMap<>();
        this.attributesMap = new HashMap<>();
        parseHttpRequest(); // 对request请求体进行解析成字符串
        parseMethod();
        if (StrUtil.isEmpty(requestString)) {
            return;
        }
        parseUri(); // 解析Uri 这里解析出规整的 uri
        parseContext(); // 通过从上面的uri解析中 得到path，获取context对象。即得到该uri对应的context对象，回到server服务器端后，通过getDocBase和uri定位服务端的资源，进行返回
        if (!"/".equals(context.getPath())) { // 非根路径，则对uri进行修正 例如：/a/a.html -> /a.html，因为此时前一部分的路径设置在了context的path中,即 非ROOT目录，只保留资源 ,例如 修改为 /x.html，即修正为真正要具体访问到的那个具体的页面资源
            uri = StrUtil.removePrefix(uri, context.getPath()); // 进行修正，只获取到 具体的资源
            if (StrUtil.isEmpty(uri)) {
                uri = "/";
            }
        }
        parseParameters(); // 解析请求参数
        parseHeaders(); // 解析出请求头
        parseCookies(); // 解析出请求中的cookie信息
    }

    public boolean isForwarded() {
        return forwarded;
    }

    public void setForwarded(boolean forwarded) {
        this.forwarded = forwarded;
    }

    @Override
    public HttpSession getSession() {
        return session;
    }

    public void setSession(HttpSession session) {
        this.session = session;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public Socket getSocket() {
        return socket;
    }

    @Override
    public void removeAttribute(String name) {
        attributesMap.remove(name);
    }

    @Override
    public void setAttribute(String name, Object value) {
        attributesMap.put(name, value);
    }

    @Override
    public Object getAttribute(String name) {
        return attributesMap.get(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() { // 返回枚举类型 - 存储的是Key
        Set<String> keys = attributesMap.keySet();
        return Collections.enumeration(keys);
    }

    /**
     * 从cookie中获取sessionid - cookie中的JsessionId就用来对应着服务端的session
     *
     * @return
     */
    public String getJSessionIdFromCookie() {
        if (cookies == null) {
            return null;
        }
        String res = null;
        for (Cookie cookie : cookies) {
            if ("JSESSIONID".equals(cookie.getName())) { // 获取到存在sessionId的cookie，目的是用这个id去匹配服务端的session
//                return cookie.getValue();
                System.out.println(cookie.getName()+"："+cookie.getValue());
                res = cookie.getValue();
            }
        }
        return res;
    }

    private void parseMethod() {
        method = StrUtil.subBefore(requestString, " ", false); // 遇见的第一个空格进行分隔
    }

    private void parseHttpRequest() throws IOException {
        InputStream is = socket.getInputStream(); // 获取浏览器请求消息的输入流, 这里可能会阻塞，等待客户端的请求
        byte[] bytes = MiniBrowser.readBytes(is, false); // 带参数false是因为，如果读取到的数据不够 bufferSize ,那么就不继续读取了。因为浏览器默认使用长连接，发出的连接不会主动关闭，那么 Request 读取数据的时候 就会卡在那里了
        requestString = new String(bytes, StandardCharsets.UTF_8);
    }

    private void parseUri() {
        /**
         * 从request当中解析出uri
         */
        String temp;
        temp = StrUtil.subBetween(requestString, " ", " "); // 取两者之间的内容。
        if (!StrUtil.contains(temp, '?')) {
            uri = temp;
            return;
        }
        temp = StrUtil.subBefore(temp, "?", false); // 带有参数的取uri，将请求参数去除
        uri = temp;
    }

    private void parseContext() {
        Service service = connector.getService(); // 从connector中获取service
        Engine engine = service.getEngine(); // 获取引擎
        // 下面这两句代码的作用是将 空的目录，进行返回，例如，请求 /b /,这种没有指定具体的参数的路径，返回 /b / 的context对象。
        context = engine.getDefaultHost().getContext(uri); // 存在该uri对应的Context,如果获取到的不为null，则说明请求没有带上具体的资源。
        if (context != null) {
            return;
        }

        String path = StrUtil.subBetween(uri, "/", "/");// 资源路径 将最后的.html 具体资源剔除  即将一级目录取出 /a/b/index.html => a 这个其实就是context中的path，根据这个path，将context取出
        if (path == null) { // root路径,代表前面没有path，直接在root下访问的x.html文件
            path = "/";
        } else {
            path = "/" + path;
        }
//        context = BootStrap.contextMap.get(path); // 获取加载的context对象,path
//        context = host.getContext(path); // 这里通过从host进行加载，即从host中进行获取.
//        context = engine.getDefaultHost().getContext(path); // 从engine中进行获取,该思路是：获取context。通过从engine中获取host，host中获取context
        context = engine.getDefaultHost().getContext(path); // 这个context和path进行对应
        if (context == null) { // 容器中不存在，获取 "/” 对应的 ROOT Context
            context = engine.getDefaultHost().getContext("/");
        }
    }

    public Context getContext() {
        return context;
    }

    public String getUri() {
        return uri;
    }

    public String getRequestString() {
        return requestString;
    }

    @Override
    public String getMethod() {
        return method;
    }

    @Override
    public ServletContext getServletContext() {
        return context.getServletContext(); // 返回context中对应的容器
    }

    @Override
    public String getRealPath(String path) { // 返回该context中包含path的绝对路径
        return context.getServletContext().getRealPath(path);
    }

    @Override
    public String getParameter(String name) { // 获取参数值 - 单个

        String[] values = parameterMap.get(name); // 将其对应的value值输出
        if (values != null && values.length != 0) {
            return values[0];
        }
        return null;
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return parameterMap;
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(parameterMap.keySet());
    }

    @Override
    public String[] getParameterValues(String name) { // 获取参数值 - 多个
        return parameterMap.get(name);
    }

    /**
     * 返回ApplicationDispatcher对象
     * @param uri
     * @return
     */
    @Override
    public RequestDispatcher getRequestDispatcher(String uri) {
        return new ApplicationRequestDispatcher(uri);
    }

    /**
     * 根据 get 或 post 方式 解析参数 - 并将参数设定到parameterMap中
     */
    public void parseParameters() {
        if ("GET".equals(this.getMethod())) { // 请求方式是GET
            String url = StrUtil.subBetween(this.requestString, " ", " "); // 先将url取出，?后的就是请求参数
            if (StrUtil.contains(url, '?')) {
                this.queryString = StrUtil.subAfter(url, '?', false); // 第一个问号开始后的就是请求参数
            }
        }

        if ("POST".equals(this.getMethod())) { // 请求方式是POST请求 - 那么就需要从请求体中，进行参数的获取
            queryString = StrUtil.subAfter(this.requestString, "\r\n\r\n", false); // 取出请求空行后的内容
        }

        if (queryString == null) { // 无请求参数
            return;
        }

        queryString = URLUtil.decode(queryString); // 将请求参数 统一成UTF-8编码的形式

        String[] parameterValues = queryString.split("&"); // 将每一对请求参数进行分割
        if (parameterValues.length != 0) { // 存在请求参数对
            for (String parameterValue : parameterValues) { // 遍历所有的请求参数值 即 parameter=value&...&... 进行参数的设定
                String[] nameValues = parameterValue.split("="); // parameter=value
                String name = nameValues[0]; // parameter
                String value = nameValues[1]; // value
                String[] values = parameterMap.get(name);
                // 重新参数的map
                if (values == null) {
                    values = new String[]{value};// 直接复制给values
                } else {
                    values = ArrayUtil.append(values, value); // 将value的值扩充到values中
                }
                parameterMap.put(name, values);
            }

        }
    }

    @Override
    public String getHeader(String name) {
        if (name == null) {
            return null;
        }
        name = name.toLowerCase(); // 黑盒处理 - 统一成小写进行进行处理
        return headMap.get(name);
    }

    /**
     * 返回所有的head的key
     *
     * @return
     */
    @Override
    public Enumeration<String> getHeaderNames() {
        return Collections.enumeration(headMap.keySet()); // 返回headName
    }

    /**
     * head中的值转换为int类型 String 的Int才行
     *
     * @param name
     * @return
     */
    @Override
    public int getIntHeader(String name) {
        String value = headMap.get(name);
        return Convert.toInt(value, 0);
    }

    /**
     * 从requestString 中解析出这些 header
     */
    public void parseHeaders() {
        StringReader stringReader = new StringReader(requestString); // 将requestString读取到流当中
        List<String> lines = new ArrayList<>();
        IoUtil.readLines(stringReader, lines); // 将字符串的流读入到lines集合中
        for (int i = 1; i < lines.size(); ++i) {
            String line = lines.get(i);
            if (line.length() == 0) { // 读取到了请求空行处，即到这里后面就没有请求头了
                break;
            }
            String[] headers = line.split(":");
            String headerName = headers[0].toLowerCase(); // headerName全转化为小写 全部统一成小写 进行处理
            String headerValue = headers[1];
            headMap.put(headerName, headerValue);
        }

    }

    /**
     * 从http请求协议中解析出Cookie
     */
    public void parseCookies() {
        ArrayList<Cookie> cookieList = new ArrayList<>();
        String cookies = headMap.get("cookie");
        if (cookies != null) {
//            System.out.println(cookies);
            String[] pairs = StrUtil.split(cookies, ";"); // 取出每一对Cookie中的值
            for (String pair : pairs) {
                if (StrUtil.isBlank(pair)) { // 该cookie是的空的
                    continue;
                }
//                System.out.println(pair);
                String[] segs = StrUtil.split(pair, "=");
//                String name = segs[0].split(" ")[1];
                String name = StrUtil.subAfter(segs[0], " ", false);
                String value = segs[1];
                Cookie cookie = new Cookie(name, value);
                cookieList.add(cookie);
            }
        }
        this.cookies = ArrayUtil.toArray(cookieList, Cookie.class);
    }

    @Override
    public Cookie[] getCookies() {
        return cookies;
    }

    public String getLocalAddr() {
        return socket.getLocalAddress().getHostAddress(); // 主机地址
    }

    public String getLocalName() {
        return socket.getLocalAddress().getHostName(); // 主机名
    }

    public int getLocalPort() {
        return socket.getLocalPort(); // 端口号
    }

    public String getProtocol() { // 协议
        return "HTTP:/1.1";
    }

    public String getRemoteAddr() {
        InetSocketAddress isa = (InetSocketAddress) socket.getRemoteSocketAddress();
        String temp = isa.getAddress().toString();
        return StrUtil.subAfter(temp, "/", false);
    }

    public String getRemoteHost() {
        InetSocketAddress isa = (InetSocketAddress) socket.getRemoteSocketAddress();
        return isa.getHostName();
    }

    public int getRemotePort() {
        return socket.getPort();
    }

    public String getScheme() {
        return "http";
    }

    public String getServerName() {
        return getHeader("host").trim();
    }

    public int getServerPort() {
        return getLocalPort();
    }

    public String getContextPath() { // 虚拟目录
        String result = this.context.getPath();
        if ("/".equals(result)) // 代表的是根目录
            return "";
        return result;
    }

    public String getRequestURI() {
        return uri;
    }

    public StringBuffer getRequestURL() { // 获取请求中的URL
        StringBuffer url = new StringBuffer();
        String scheme = getScheme();
        int port = getServerPort();
        if (port < 0) {
            port = 80; // Work around java.net.URL bug
        }
        url.append(scheme);
        url.append("://");
        url.append(getServerName());
        if ((scheme.equals("http") && (port != 80)) || (scheme.equals("https") && (port != 443))) {
            url.append(':');
            url.append(port);
        }
        url.append(getRequestURI());
        return url;
    }

    public String getServletPath() {
        return uri;
    }

    public Connector getConnector() {
        return connector;
    }
}
