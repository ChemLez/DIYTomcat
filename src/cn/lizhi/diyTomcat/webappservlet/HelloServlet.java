package cn.lizhi.diyTomcat.webappservlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

// 我们编写的处理逻辑，类似于JavaWeb中编写的Servlet 重写doGet和doPost的方法请求
public class HelloServlet extends HttpServlet {

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            response.getWriter().println("Hello DIY Tomcat from HelloServlet"); // 这里目前是写死的  目前的 j2ee就是虚拟路径
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
