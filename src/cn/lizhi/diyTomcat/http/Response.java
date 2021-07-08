package cn.lizhi.diyTomcat.http;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;

import javax.servlet.http.Cookie;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 构造Response对象
 * 用于封装响应消息
 */
public class Response extends BaseResponse {

    private StringWriter stringWriter; // 字符流 - 针对字符串
    private PrintWriter writer; // 文本输出流 该流的主要作用 用来定义流输出的位置
    private String contentType; // 响应类型
    private byte[] body; // 准备body用来存放二进制文件
    private int status; // 返回的状态码属性
    private List<Cookie> cookies;
    private String redirectPath;


    public Response() {
        this.stringWriter = new StringWriter();
        this.writer = new PrintWriter(stringWriter); // 当调用writer.println()时，定义输出流在stringWriter中，而StringWriter用来存放响应消息
        this.contentType = "text/html"; // 默认的返回方式
        this.cookies = new ArrayList<>();
    }


    public void setContentType(String contentType) { // 用于设定content-type
        this.contentType = contentType;
    }



    public void setBody(byte[] body) { // 用于设定静态资源的字节 —— 静态资源的字节数组通过set到body中
        this.body = body;
    }

    @Override
    public PrintWriter getWriter() {
        return writer;
    }



    public String getContentType() {
        return contentType;
    }



    public byte[] getBody() throws UnsupportedEncodingException {
//        String content = stringWriter.toString();
//        byte[] body = content.getBytes(StandardCharsets.UTF_8); // 请求体的字节数组 这里是直接局部响应资源的 字节数组

        if (body == null) { // 当body不为空时，直接返回body , 到这里是全局响应资源的字节数组 当请求资源时是动态资源时，才会进入这个判断
            String content = stringWriter.toString(); // 资源 这里通过service方法中的 response.getWriter().print，将数据流输出到stringWriter
            body = content.getBytes(StandardCharsets.UTF_8);
        }
        return body;
    }

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public void setStatus(int status) {
        this.status = status;
    }

    @Override
    public void addCookie(Cookie cookie) {
        cookies.add(cookie);
    }

    public String getRedirectPath() {
        return redirectPath;
    }


    public void sendRedirect(String redirect) { // 设定重定向的地址
        this.redirectPath = redirect;
    }

    //    public List<Cookie> getCookies() {
//        return cookies;
//    }

    /**
     * 将cookie集合转换成 cookie Header;对cookie的内容进行响应
     */

    public String getCookiesHeader() {
        if (cookies == null) {
            return ""; // 返回先的响应头中，无cookie
        }
        String pattern = "EEE,d MMM yyy HH:mm:ss 'GMT'"; // 设定日期的返回格式
        SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.ENGLISH); // 设定需要格式化的日期
        StringBuffer sb = new StringBuffer();
        for (Cookie cookie : cookies) {
            sb.append("\r\n"); // 换行
            sb.append("Set-Cookie:"); // 返回的字段
//            System.out.println(cookie.getName() + "=" + cookie.getValue() + ";");
            sb.append(cookie.getName()).append("=").append(cookie.getValue()).append(";"); // Set-Cookie:name=value;
            if (cookie.getMaxAge() != -1) { // -1 代表 只要浏览器关闭就不会失效
                sb.append("Expires=");// 失效时间 Set-Cookie:name=value;Expires=
                Date now= new Date(); // 当前时间
                DateTime expire = DateUtil.offset(now, DateField.MINUTE, cookie.getMaxAge()); // 失效日期 转为分钟 失效，即再过多少分钟就会失效
                sb.append(sdf.format(expire)).append(";"); // Set-Cookie:name=value;Expires=date;
            }
            if (cookie.getPath() != null) {
                sb.append("Path=").append(cookie.getPath()); // Set-Cookie:name=value;Expires=date;Path="/"失效时间
            }
        }

        return sb.toString();
    }
}
