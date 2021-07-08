package cn.lizhi.diyTomcat.test;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.NetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import cn.lizhi.diyTomcat.util.MiniBrowser;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TestTomcat {

    // 以DIYTomcat做测试
    private static int port = 18080;
    private static String ip = "127.0.0.1";

    /**
     * 只执行一次，在所有测试之前
     */
    @BeforeClass
    public static void beforeClass() {
        // 所有测试之前，先查看diy tomcat 是否已经启动，即端口必须先开启
        if (NetUtil.isUsableLocalPort(port)) { // true -> 代表端口未启动
            System.err.println("请先启动 位于端口：" + port + "的diy tomcat，否则无法进行单元测试");
            System.exit(1);
        } else {
            System.out.println("检测到 diy tomcat 已经启动，开始进行单元测试");
        }
    }

    @Test
    public void testHelloTomcat() {
        /**
         * 服务器访问
         */
        String html = getContentString("/");
        Assert.assertEquals(html, "<h1>Hello DIY Tomcat from chemlez.cn</h1>");
    }

    @Test
    public void testAHtml() {
        /**
         * 测试本地文件访问
         */
        String html = getContentString("/a.html");
        Assert.assertEquals(html, "<h1>Hello DIY Tomcat from a.html</h1>");

    }

    @Test
    public void testTimeConsumeHtml() throws InterruptedException {

        /**
         * 客户端线程池 - 用于模仿多个用户同时请求。即：多个客户端同时访问 timeConsume.html
         */
        // 线程池的定义 - 设定线程池,模仿3个同时访问 timeConsume.html 核心线程池大小20，最大线程池大小20 线程最大空闲时间 60，时间单位-Second,线程等待队列
        ThreadPoolExecutor threadPool = new ThreadPoolExecutor(20, 20, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(10));

        TimeInterval timeInterval = DateUtil.timer(); // 开始计时

        for (int i = 0; i < 3; i++) {
            threadPool.execute(() -> getContentString("/timeConsume.html")); // 从线程池中获取3个线程，执行任务 这里的线程池，是用来模拟客户端的多个请求同时发出
        }
        threadPool.shutdown(); // shutdown 尝试关闭线程池，但是如果 线程池里有任务在运行，就不会强制关闭，直到任务都结束了，才关闭.
        threadPool.awaitTermination(1, TimeUnit.HOURS); // 会给线程池1个小时的时间去执行，如果超过1个小时了也会返回，如果在一个小时内任务结束了，就会马上返回。

        long duration = timeInterval.intervalMs(); // 结束计时

        Assert.assertTrue(duration < 3000); // 断言是否超过3s
    }

//    @Test
//    public void testAIndex() {
//        String html = getContentString("/a/a.html");
//        Assert.assertEquals(html, "Hello DIY Tomcat from a.html@a");
//    }
//
//    @Test
//    public void testBIndex() {
//        String html = getContentString("/b/a.html"); // 模拟客户端发送请求
//        Assert.assertEquals(html, "<h1>Hello DIY Tomcat from a.html@b</h1>");
//    }

    @Test
    public void testAIndex() {
        String html = getContentString("/a");
        Assert.assertEquals(html, "<h1>Hello DIY Tomcat from a.html@a</h1>");
    }

    @Test
    public void testBIndex() {
        String html = getContentString("/b"); // 模拟客户端发送请求
        Assert.assertEquals(html, "<h1>Hello DIY Tomcat from index.html@b</h1>");
    }

    @Test
    public void test404() {
        /**
         * 增加一个单元测试，访问某个不存在的 html , 然后断言 返回的 http 响应里包含 HTTP/1.1 404 Not Found, 毕竟返回的整个 http 响应那么长，不好用 equals 来比较，只要包含关键的头信息，就算测试通过
         */
        String httpString = getHttpString("/not.html");
        containAssert(httpString, "HTTP/1.1 404 Not Found");
    }

    @Test
    public void test500() {
        /**
         * 测试500
         */
        String response = getHttpString("/500.html");
        containAssert(response, "HTTP/1.1 500 Internal Server Error");
    }

    @Test
    public void testTxt() {
        String responseString = getContentString("/a.txt");
        containAssert(responseString, "Hello DIY Tomcat from a.txt");
    }

//    @Test
//    public void testHello() {
//        String html = getContentString("/hello");
//        Assert.assertEquals(html,"Hello DIY Tomcat from HelloServlet");
//    }

    @Test
    public void testHelloWithServlet() {
        String html = getContentString("/j2ee/hello"); // 动态资源
        Assert.assertEquals(html, "Hello DIY Tomcat from HelloServlet");
    }

    /**
     * 测试post方法 - 参数
     */
    @Test
    public void testPostParam() {
        String uri = "/java/param";
        String url = StrUtil.format("http://{}:{}{}", ip, port, uri);
        Map<String, Object> params = new HashMap<>(); // 模拟参数
        params.put("name", "chemLez");
        String html = MiniBrowser.getContentString(url, false, params, false);
        Assert.assertEquals(html, "post name: chemLez");
    }

    @Test
    public void testHeaderParam() {
        String uri = "/java/header";
        String url = StrUtil.format("http://{}:{}{}", ip, port, uri);
        String html = MiniBrowser.getContentString(url);
        Assert.assertEquals(html, "chemlez mini brower / java1.8");
    }

    @Test
    public void testCookie() {
        String uri = "/java/setCookie";
        String html = getHttpString(uri);
        containAssert(html, "Set-Cookie:name=Gareen(cookie);Expires=");
    }

    @Test
    public void testGetCookie() throws IOException {
        String url = StrUtil.format("http://{}:{}{}", ip, port, "/java/getCookie");
        URL u = new URL(url);
        URLConnection conn = u.openConnection();
        conn.setRequestProperty("Cookie", "name=Gareen(cookie)"); // 设定带cookie的请求头
        conn.connect();
        InputStream is = conn.getInputStream();
        String html = IoUtil.read(is, StandardCharsets.UTF_8);
        containAssert(html, "name:Gareen(cookie)");
    }

    @Test
    public void testSession() throws IOException {
        String jSessionId = getContentString("/java/setSession");// 用来设置session；并将其id返回给客户端，用来后面构造该session对应的cookie
        if (jSessionId != null) {
            jSessionId = jSessionId.trim();
        }
        String url = StrUtil.format("http://{}:{}{}", ip, port, "/java/getSession");
        URL u = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) u.openConnection(); // 代表和远方的地址建立连接
        conn.setRequestProperty("Cookie", "JSESSIONID=" + jSessionId); // 构造的请求头部分 这个JSESSIONID和服务端的session的id对应，即用来标识session
        conn.connect();
        InputStream is = conn.getInputStream(); // 响应消息
        String html = IoUtil.read(is, "utf-8");
        containAssert(html, "Gareen(session)");
    }

    @Test
    public void testGzip() {

        byte[] contentByte = getContentByte("/", true); // 压缩过的
//        System.out.println(new String(contentByte));
        byte[] unGzipContentByte = ZipUtil.unGzip(contentByte); // 进行解压
//        System.out.println(new String(unGzipContentByte));
        String content = new String(unGzipContentByte).trim();
//        System.out.println(content);
//        System.out.println(1);
        Assert.assertEquals(content, "<h1>Hello DIY Tomcat from chemlez.cn</h1>");
    }

    @Test
    public void testJsp() {
        String html = getContentString("/java/");
        containAssert(html, "hello jsp@javaweb");
    }

    @Test
    public void testClientJump(){
        String http_servlet = getHttpString("/java/jump1"); // 测试invokerServlet
        System.out.println(http_servlet);
        System.out.println("=====");
        containAssert(http_servlet,"HTTP/1.1 302 Found");
        String http_jsp = getHttpString("/java/jump1.jsp"); // 测试JspServlet
        System.out.println(http_jsp);
        containAssert(http_jsp,"HTTP/1.1 302 Found");
    }

    /**
     * 服务器内部资源跳转
     */
    @Test
    public void testServerJump(){
        String http_servlet = getHttpString("/java/jump2");
        System.out.println(http_servlet);
        containAssert(http_servlet,"Hello DIY Tomcat from HelloServlet");
    }

    /**
     * 带参数的服务器内部跳转
     */
    @Test
    public void testServerJumpWithAttributes(){
        String http_servlet = getHttpString("/java/jump2");
        containAssert(http_servlet,"Hello DIY Tomcat from HelloServlet@javaweb, the name is gareen");
    }

    /**
     * jar文件测试
     */
    @Test
    public void testJavaweb0Hello() {
        String html = getContentString("/javaweb0/hello");
        containAssert(html,"Hello DIY Tomcat from HelloServlet@javaweb");
    }

    private byte[] getContentByte(String uri, boolean gzip) {
        String url = StrUtil.format("http://{}:{}{}", ip, port, uri);
        return MiniBrowser.getContentBytes(url, gzip);
    }


    private String getContentString(String uri) {
        String url = StrUtil.format("http://{}:{}{}", ip, port, uri);
        String content = MiniBrowser.getContentString(url);
        return content;
    }

    private String getHttpString(String uri) { //
        /**
         * 获取 http 响应 - 字符串 目的是看是不是包含404响应信息
         */
        String url = StrUtil.format("http://{}:{}{}", ip, port, uri); // 构造url
        String http = MiniBrowser.getHttpString(url);
        return http;
    }

    private void containAssert(String html, String string) {
        /**
         * 增加一个 containAssert 断言，来判断html 里是否包含某段字符串的断言
         * html 是 http响应
         * string 是设定字符串，查看html中是否包含string
         */
        boolean flag = StrUtil.containsAny(html, string); // 两者不同，返回true;相同 返回false
        Assert.assertTrue(flag);
    }
}