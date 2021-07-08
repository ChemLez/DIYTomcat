package cn.lizhi.diyTomcat.util;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.SecureUtil;
import cn.lizhi.diyTomcat.http.Request;
import cn.lizhi.diyTomcat.http.Response;
import cn.lizhi.diyTomcat.http.StandardSession;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;
import java.util.*;

/**
 * 用于管理 session
 */
public class SessionManager {

    private static Map<String, StandardSession> sessionMap = new HashMap<>(); // 用于存放所有的session
    private static int defaultTimeout = getTimeout(); // 获取session的存活时间

    static {
        startSessionOutDateCheckThread(); // 默认启动检测Session是否失效的线程
    }


    private static int getTimeout() {
        int defaultResult = 30;// 默认时间为30 分钟
        try {
            String xml = FileUtil.readUtf8String(Constant.webXmlFile); // 从配置文件中 获取给服务器设定的session有效时间
            Document d = Jsoup.parse(xml);
            Elements es = d.select("session-config session-timeout"); // 定位到 session-timeout 标签
            if (es.isEmpty()) { // 标签不存在，直接返回默认时间
                return defaultResult;
            }
            return Convert.toInt(es.get(0).text());
        } catch (IORuntimeException e) {
            return defaultResult; // 传回默认的有效时间
        }
    }


    // 用于创建 sessionid
    public static synchronized String generateSessionId() {
        String result = null;
        byte[] bytes = RandomUtil.randomBytes(16); // 16个字节长度的随机字节数组
        result = new String(bytes);
        result = SecureUtil.md5(result); // 采用md5加密，生成16进制的MD5字符串
        result = result.toUpperCase(); // 将其中的字母 全部转化为大写字母
        return result;
    }

    /**
     * 获取session的逻辑
     * 如果浏览器没有传递 jsessionid 过来，那么就创建一个新的session
     * 如果浏览器传递过来的 jsessionid 无效，那么也创建一个新的 sessionid
     * 否则就使用现成的session, 并且修改它的lastAccessedTime， 以及创建对应的 cookie
     */
    public static HttpSession getSession(String jsessionid, Request request, Response response) {
        if (jsessionid == null) { // 没有传递session过来
            return newSession(request, response);
        } else {
            StandardSession currentSession = sessionMap.get(jsessionid);
            if (currentSession == null) { // // 该session已经失效，现在重新创建session
                return newSession(request, response);
            }
            // 存在，将时间进行重置
            currentSession.setLastAccessedTime(System.currentTimeMillis()); // request.getSession().getAttribute("name_in_session"); 从这一步获取到session，这个session保存了之前存入的attribute
            // 创建对应的cookie
            createCookieBySession(currentSession, request, response);
            return currentSession;
        }
    }

    /**
     * 由session创建对应的cookie
     *
     * @param session
     * @param request
     * @param response
     */
    private static void createCookieBySession(StandardSession session, Request request, Response response) {
        Cookie cookie = new Cookie("JSESSIONID", session.getId()); // 创建该session对应的cookie;该目的是将 服务器端用来的sessionId保存在cookie中，用来和服务器端的session进行匹配
        // cookie进行初始化
        cookie.setMaxAge(session.getMaxInactiveInterval()); // 设置cookie持久化的时间
        cookie.setPath(request.getContext().getPath()); // path - 访问的资源路径目录 ，虚拟目录

        response.addCookie(cookie); // 将cookie进行客户端响应，以后客户端上的这个cookie可以用来对应到服务端的cookie
    }

    /**
     * 创建session
     *
     * @param request
     * @param response
     * @return
     */
    private static HttpSession newSession(Request request, Response response) {
        ServletContext servletContext = request.getServletContext(); // 获取到servletContext
        String sId = generateSessionId(); // 随机生成sessionId
        StandardSession session = new StandardSession(sId, servletContext); // servletContext代表是整个应用 - 创建session
        // session初始化
        session.setMaxInactiveInterval(defaultTimeout); // 设定最大的间隔时间
        session.setLastAccessedTime(System.currentTimeMillis()); // 当创建的时候，最后一次使用时间，就是创建时间
        sessionMap.put(sId, session); // 将新创建的session存放咋map中

        createCookieBySession(session, request, response); // 创建对应的cookie
        return session;
    }


    /**
     * 从这里开启一个新的线程，每隔30秒调用一次 checkOutDataSession方法 随着类加载进行一次执行，所以只会有一个线程来执行该方法，不会产生线程同步的问题
     */
    private static void startSessionOutDateCheckThread() {
        new Thread() {
            @Override
            public void run() {
                while (true) {
//                    synchronized (SessionManager.class) { // 线程同步
                    checkOutDateSession();
                    ThreadUtil.sleep(30 * 1000); // 每隔30s 进行一次检查
                }
            }
//            }
        }.start();
    }

    /**
     * 从sessionMap里根据 lastAccessedTime 筛选出过期的 jsessionids ,然后把他们从 sessionMap 里去掉
     */
    private static void checkOutDateSession() {
        Set<String> jsessionids = sessionMap.keySet(); // 集合中所有的session
        List<String> outdateJessions = new ArrayList<>(); // 用于存放过时的session，以它们的id进行标识过时的session
        for (String jsessionid : jsessionids) {
            StandardSession standardSession = sessionMap.get(jsessionid);
            long interval = System.currentTimeMillis() - standardSession.getLastAccessedTime(); // 单位 ms 当前时间 - 最后一次使用的时间
            if (interval > standardSession.getMaxInactiveInterval() * 60 * 1000) { // 表明session已经超时
                outdateJessions.add(jsessionid);
            }
        }
        for (String jsessionid : outdateJessions) { // 删除过时的session
            sessionMap.remove(jsessionid);
        }
    }

}
