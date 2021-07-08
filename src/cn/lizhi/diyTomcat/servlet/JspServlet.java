package cn.lizhi.diyTomcat.servlet;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.util.StrUtil;
import cn.lizhi.diyTomcat.catalina.Context;
import cn.lizhi.diyTomcat.classloader.JspClassLoader;
import cn.lizhi.diyTomcat.http.Request;
import cn.lizhi.diyTomcat.http.Response;
import cn.lizhi.diyTomcat.util.Constant;
import cn.lizhi.diyTomcat.util.JspUtil;
import cn.lizhi.diyTomcat.util.WebXMLUtil;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import java.io.File;
import java.io.IOException;

/**
 * jsp servlet的分发
 */
public class JspServlet extends HttpServlet {

    private static JspServlet instance = new JspServlet();

    private JspServlet() {

    }

    public static synchronized JspServlet getInstance() {
        return instance;
    }

    /**
     * 目前的处理逻辑是将jsp文件当成文本文件进行处理
     */
    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        try {
            Request request = (Request) req;
            Response response = (Response) res;
            String uri = request.getRequestURI(); // 资源路径 - 二级目录/.../具体资源
            if ("/".equals(uri)) { // 根路径
                uri = WebXMLUtil.getWelcomeFile(request.getContext()); // 单独为根路径的话，就获取根路径的首页
            }
            // 获取资源文件对象
            String fileName = StrUtil.removePrefix(uri, "/");
            File file = FileUtil.file(request.getRealPath(fileName)); // 请求资源的资源对象
            File jspFile = file;

            if (jspFile.exists()) { // 存在该jsp文件
                Context context = request.getContext(); // 该request对应的context
                String path = context.getPath(); // 虚拟目录
                // 存放的子文件夹 路径
                String subFolder; // 主要是用来处理root路径，其他的路径都是以虚拟目录作为子文件夹；而root目录对应的是"/"。所以是通过"_"作为映射的子目录
                if ("/".equals(path)) { // 根目录
                    subFolder = "_";
                } else {
                    subFolder = StrUtil.subAfter(path, "/", false); // 取第一个 "/" 后面的内容；如 /java => java
                }
                // 获取该jsp的servlet文件对象
                String servletClassPath = JspUtil.getServletClassPath(uri, subFolder); // jsp对应的class文件路径
                File jspServletClassFile = new File(servletClassPath); // servletClass 文件对象
                if (!jspServletClassFile.exists()) { // 该servlet文件对象不存在，需转译该 jsp文件
                    JspUtil.compileJsp(context, jspFile);
                } else if (jspFile.lastModified() > jspServletClassFile.lastModified()) { // jsp最后一次修改的时间 大于 jspServletClassFile 生成的时间；说明了该jsp文件进行了修改，需要重新转译 和 编译
                    JspUtil.compileJsp(context, jspFile);
                    JspClassLoader.invalidJspClassLoader(uri, context); // 移除原来的classLoader
                }

                String extName = FileUtil.extName(file);
                String mimeType = WebXMLUtil.getMimeType(extName); // 需要响应的类型
                response.setContentType(mimeType);
                // 获取jsp文件对应的classLoader进行类加载
                JspClassLoader jspClassLoader = JspClassLoader.getJspClassLoader(uri, context); // 该jsp文件对应的classLoader; 根据uri 和 context 获取当前jsp 对应的 JspClassLoader
                String jspServletClassName = JspUtil.getJspServletClassName(uri, subFolder); // 获取 jsp 对应的 servlet Class Name;该servlet的类路径
                Class<?> jspServletClass = jspClassLoader.loadClass(jspServletClassName);// 获取加载类的对象
                System.out.println(jspServletClass.getClassLoader());
                System.out.println(jspServletClass);
                HttpServlet servlet = context.getServlet(jspServletClass); // 通过单例的方式 获取该servlet对象
                servlet.service(request, response); // 将请求交给jsp编译后的class文件，就是servlet文件 来执行它的service方法
                if (response.getRedirectPath() != null) {
                    response.setStatus(Constant.CODE_302);
                } else {
                    response.setStatus(Constant.CODE_200);
                }
            } else { // 文件不存在
                response.setStatus(Constant.CODE_404);
            }
        } catch (IORuntimeException | ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
        }
    }
}
