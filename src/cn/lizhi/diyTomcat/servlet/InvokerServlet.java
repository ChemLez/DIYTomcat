package cn.lizhi.diyTomcat.servlet;

import cn.hutool.core.util.ReflectUtil;
import cn.lizhi.diyTomcat.catalina.Context;
import cn.lizhi.diyTomcat.http.Request;
import cn.lizhi.diyTomcat.http.Response;
import cn.lizhi.diyTomcat.util.Constant;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 用于加载动态资源 对动态资源的servlet的分发
 */
public class InvokerServlet extends HttpServlet {

    private static InvokerServlet instance = new InvokerServlet();

    private InvokerServlet() { // 不能通过new关键字进行对象的实例化

    }

    public static synchronized InvokerServlet getInstance() { // 获取该单例对象
        return instance;
    }

    public void service(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException, ServletException {
        Request request = (Request) httpServletRequest; // 向下转型
        Response response = (Response) httpServletResponse;

        String uri = request.getUri();
        Context context = request.getContext(); // 获取到request下该context对象
        String servletClassName = context.getServletClassName(uri);// 该uri下对应的servletClassName 全限定类名

        try {
            Class<?> servletClass = context.getWebappClassLoader().loadClass(servletClassName); // 获取该class对象；后续根据这个类对象，实例化出servlet对象出来 context对应的classLoader进行加载该类 再由Service交给对应的servlet进行处理;同一个类加载器
            System.out.println(servletClass.getClassLoader());
            System.out.println(servletClass);
            Object servletObject = context.getServlet(servletClass); // 负责从context中获取其对应的servlet对象
            ReflectUtil.invoke(servletObject, "service", request, response); // 执行方法 Reflect 反射执行
            if (response.getRedirectPath() != null) { // 是否存在跳转地址
                response.setStatus(Constant.CODE_302);
            } else {
                response.setStatus(Constant.CODE_200);
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

}
