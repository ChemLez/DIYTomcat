package cn.lizhi.diyTomcat.servlet;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.lizhi.diyTomcat.catalina.Context;
import cn.lizhi.diyTomcat.http.Request;
import cn.lizhi.diyTomcat.http.Response;
import cn.lizhi.diyTomcat.util.Constant;
import cn.lizhi.diyTomcat.util.WebXMLUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

/**
 * 用于加载静态资源 servlet的分发
 */
public class DefaultServlet extends HttpServlet {

    private static DefaultServlet instance = new DefaultServlet();

    public static synchronized DefaultServlet getInstance() {
        return instance;
    }

    private DefaultServlet() {

    }

    /**
     * 静态资源的处理逻辑
     * @param httpServletRequest
     * @param httpServletResponse
     * @throws ServletException
     * @throws IOException
     */
    @Override
    public void service(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {

        Request request = (Request) httpServletRequest;
        Response response = (Response) httpServletResponse;
        String uri = request.getUri();

        // 下面的静态资源的处理逻辑
        if ("/500.html".equals(uri)) {
            throw new RuntimeException("this is a deliberately created exception"); // 异常抛出，并抓取异常，进行处理
        }

        if ("/".equals(uri)) { // 直接访问的页面，服务器首页 -- 下面是作为普通文件 进行访问
            uri = WebXMLUtil.getWelcomeFile(request.getContext());
            if (uri.endsWith(".jsp")) { // 首页文件是jsp文件
                JspServlet.getInstance().service(httpServletRequest, httpServletResponse);
                return;
            }
        }

        String fileName = StrUtil.removePrefix(uri, "/"); // 取资源路径 路径中的资源
        File file = FileUtil.file(request.getRealPath(fileName)); // 从request中获取到真实绝对路径 返回文件对象 最终调用的是 return new File(context.getDocBase(), filename).getAbsolutePath(); 返回该资源文件对象

        if (file.exists()) {
            // 获取文件的mime-type类型
            String extName = FileUtil.extName(file); // 获取文件的拓展名
            String mimeType = WebXMLUtil.getMimeType(extName); // 文件应该返回的类型
            response.setContentType(mimeType); // 返回对应的文件类型

            if (fileName.equals("timeConsume.html")) { // 阻塞1s
                ThreadUtil.sleep(1000);
            }
            byte[] body = FileUtil.readBytes(file); // 静态资源 设置为byte[]
            response.setBody(body);
            response.setStatus(Constant.CODE_200);
        } else {
            response.setStatus(Constant.CODE_404);

        }
    }
}
