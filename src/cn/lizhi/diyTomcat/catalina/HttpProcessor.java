package cn.lizhi.diyTomcat.catalina;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import cn.hutool.log.LogFactory;
import cn.lizhi.diyTomcat.http.Request;
import cn.lizhi.diyTomcat.http.Response;
import cn.lizhi.diyTomcat.servlet.DefaultServlet;
import cn.lizhi.diyTomcat.servlet.InvokerServlet;
import cn.lizhi.diyTomcat.servlet.JspServlet;
import cn.lizhi.diyTomcat.util.Constant;
import cn.lizhi.diyTomcat.util.SessionManager;

import javax.servlet.Filter;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class HttpProcessor {

    public void execute(Socket s, Request request, Response response) {

        try {
            String uri = request.getUri();
            if (uri == null) { // 不存在资源标识，即一开始请求不符合http请求规范
                return; // request 为空
            }
            prepareSession(request, response); // 该步骤将cookie存入到response中，session存入到request中，这里的session和cookie是对应关系
            Context context = request.getContext(); // 获取该context对象
            // 通过context获取servletClassName，如果是空就表示访问的不是动态资源
            // 通过类名servletClassName 实例化 servlet对象，然后调用其doGet方法。 -- 通过反射完成
            String servletClassName = context.getServletClassName(uri); // 交给相应的servlet进行处理 - 动态资源
            // 采用责任链分发请求
            HttpServlet workingServlet;
            if (servletClassName != null) {
                workingServlet = InvokerServlet.getInstance();
            } else if (uri.endsWith(".jsp")) {
                workingServlet = JspServlet.getInstance();
            }else {
                workingServlet = DefaultServlet.getInstance();
            }
            List<Filter> filters = request.getContext().getMatchedFilters(uri);
            ApplicationFilterChain filterChain = new ApplicationFilterChain(filters, workingServlet);
            filterChain.doFilter(request, response);

            if (request.isForwarded()) { // 如果后续是重定向就不再进行处理，第一次中 socket已经关闭了
                return;
            }

            if (response.getStatus() == Constant.CODE_302) { // 重定向
                handle302(s, request, response);
                return;
            }
            if (response.getStatus() == Constant.CODE_200) {
                handle200(s, response,request);
            } else if (response.getStatus() == Constant.CODE_404) {
                handle404(s, uri);
            }

        } catch (Exception e) { // 服务端出现问题，这里处理异常，并将500信息进行返回
            LogFactory.get().error(e); // 日志记录异常信息，输出到控制台
            handle500(s, e);
        } finally {
            try {
                if (!s.isClosed()) { // 为关闭的情况下
                    s.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * 先通过cookie拿到jsessionid，然后通过SessionManager创建session，并且设置在request上
     *
     * @param request
     * @param response
     */
    private void prepareSession(Request request, Response response) {
        String jSessionId = request.getJSessionIdFromCookie(); // 从客户端的cookie中获取sessionId
        HttpSession session = null;
        session = SessionManager.getSession(jSessionId, request, response); // 这里jSessionId可能为null，表明cookie未存入session;通过该方法，session对应的cookie存入到了response中了
        request.setSession(session); // 在这里对request的session进行初始化赋值,这个session已经已经被添加进 sessionMap 进行了管理
    }

    /**
     * 对200请求进行客户端的响应
     *
     * 判断是否支持压缩，根据能否进行压缩，要响应两种不同的响应头(包含gzip和不含gzip的响应头)
     * @param s
     * @param response
     * @throws IOException
     */
    private void handle200(Socket s, Response response,Request request) throws IOException {

        OutputStream os = s.getOutputStream(); // 输出流，用来输出response 作为响应消息
        String contentType = response.getContentType(); // 响应内容类型
//        String headText = Constant.RESPONSE_HEAD_202; // 响应头
        String headText = null;
        String cookiesHeader = response.getCookiesHeader(); // cookie的响应头,这里其实也用来响应sessionId
//        headText = StrUtil.format(headText, contentType, cookiesHeader); // 头文本的构造

        byte[] body = response.getBody(); // 响应体
        boolean gzip = isGzip(request, body, contentType);
        if (gzip) { // 判断是否支持压缩
            headText = Constant.RESPONSE_HEAD_200_GZIP; // 支持压缩的响应头
        } else {
            headText = Constant.RESPONSE_HEAD_200; // 不支持压缩的响应头
        }
        headText = StrUtil.format(headText, contentType, cookiesHeader); // 响应头
        if (gzip) { // 对body进行压缩
            body = ZipUtil.gzip(body); //
        }
        byte[] head = headText.getBytes(); // 响应头字节数组
        byte[] responseBytes = new byte[head.length + body.length];// response的字节数组，由上面两个部分构造,构造出 response

        ArrayUtil.copy(head, 0, responseBytes, 0, head.length);
        ArrayUtil.copy(body, 0, responseBytes, head.length, body.length);


        os.write(responseBytes); // 响应消息
        os.flush();
    }

    private void handle404(Socket s, String uri) throws IOException {
        /**
         * 增加个 handle 404 ，根据 uri， Constant.textFormat_404 和 Constant.response_head_404 组成 404 返回响应。
         */
        String responseText = StrUtil.format(Constant.RESPONSE_TEXTFORM_404, uri, uri);
        responseText = Constant.RESPONSE_HEAD_404 + responseText;
        byte[] responseBytes = responseText.getBytes(StandardCharsets.UTF_8);
        OutputStream os = s.getOutputStream();
        os.write(responseBytes);
        os.flush();
    }

    private void handle500(Socket s, Exception e) {
        try {
            StackTraceElement[] stes = e.getStackTrace(); // 获取到异常堆栈信息
            StringBuffer sb = new StringBuffer();
            sb.append(e.toString()); // 异常信息，定义的Message
            sb.append("\r\n"); // 换行
            for (StackTraceElement ste : stes) { // 挨个将堆栈信息方进去
                sb.append("\t"); // 缩进
                sb.append(ste.toString());
                sb.append("\r\n");
            }

            String msg = e.getMessage(); // 将堆栈信息返回字符串
            if (msg != null && msg.length() > 20) { // 消息过长进行(这里设定的超过20) 截断,这里的Message用来显示在最上方，太长了的话就进行截断
                msg = msg.substring(0, 19);
            }
            String text = StrUtil.format(Constant.RESPONSE_TEXTFORM_500, msg, e.toString(), sb.toString());
            text = Constant.RESPONSE_HEAD_500 + text;
            byte[] responseBytes = text.getBytes();
            OutputStream os = s.getOutputStream();
            os.write(responseBytes);
            os.flush();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void handle302(Socket s, Request request, Response response) throws IOException {
        OutputStream os = s.getOutputStream();
        String redirectPath = response.getRedirectPath();
        String head_text = Constant.RESPONSE_HEAD_302; // 302的请求头
        String header = StrUtil.format(head_text, redirectPath);
        byte[] responseBytes = header.getBytes(StandardCharsets.UTF_8);
        os.write(responseBytes);
    }

    /**
     * 判断是否进行zip
     * @param request
     * @param body
     * @param mimeType
     * @return
     */
    private boolean isGzip(Request request, byte[] body, String mimeType) {
        String acceptEncoding = request.getHeader("Accept-Encoding"); // 请求的编码 其中 gzip如果存在 就包含在这里面
        if (!StrUtil.containsAny(acceptEncoding, "gzip")) { // 无该请求头，不进行压缩
            return false;
        }

        Connector connector = request.getConnector();
        // 对属性进行判断，看是否达到压缩的条件
        if (mimeType.contains(";")) { // 请求的文本类型
            mimeType = StrUtil.subBefore(mimeType, ";", false);
        }
        if (!"on".equals(connector.getCompression())) { // 服务器端未开启压缩设置
            return false;
        }
        if (body.length < connector.getCompressionMinSize()) { // 未达到压缩的条件
            return false;
        }
        String userAgents = connector.getNoCompressionUserAgents();
        String[] eachUserAgents = userAgents.split(","); // 不需要压缩的浏览器
        for (String eachUserAgent : eachUserAgents) { // 请求的浏览器是否是不要压缩的设定
            eachUserAgent = eachUserAgent.trim();
            String userAgent = request.getHeader("User-Agent");
            if (StrUtil.containsAny(userAgent, eachUserAgent)) {
                return false;
            }
        }

        String mimeTypes = connector.getCompressableMimeType();
        String[] eachMimeTypes = mimeTypes.split(",");
        for (String eachMimeType : eachMimeTypes) { // 是需要压缩的类型，进行返回
            if (eachMimeType.equals(mimeType)) {
                return true;
            }
        }
        return false;
    }
}
