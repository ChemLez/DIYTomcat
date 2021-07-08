package cn.lizhi.diyTomcat.util;

import cn.hutool.system.SystemUtil;

import java.io.File;

/**
 * 用于存放响应信息的头信息模板
 */
public class Constant {

    // 202响应的头信息  response的 响应头设定
    public final static String RESPONSE_HEAD_200 =
            "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: {}{}"
                    + "\r\n\r\n"; // 200的响应头

    // 表示进行了压缩的响应头
    public final static String RESPONSE_HEAD_200_GZIP =
            "HTTP/1.1 200 OK\r\n" + "Content-Type: {}{}\r\n" +
            "Content-Encoding: gzip" +
            "\r\n\r\n";

    // 302的响应头
    public static final String RESPONSE_HEAD_302 =
            "HTTP/1.1 302 Found\r\nLocation: {}\r\n\r\n";

    // 404 处理
    public static final String RESPONSE_HEAD_404 = "HTTP/1.1 404 Not Found\r\n" + "Content-Type: text/html\r\n\r\n"; // 404响应头
    public static final String RESPONSE_TEXTFORM_404 =
            "<html><head><title>DIY Tomcat/1.0.1 - Error report</title><style>" +
                    "<!--H1 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:22px;} " +
                    "H2 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:16px;} " +
                    "H3 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:14px;} " +
                    "BODY {font-family:Tahoma,Arial,sans-serif;color:black;background-color:white;} " +
                    "B {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;} " +
                    "P {font-family:Tahoma,Arial,sans-serif;background:white;color:black;font-size:12px;}" +
                    "A {color : black;}A.name {color : black;}HR {color : #525D76;}--></style> " +
                    "</head><body><h1>HTTP Status 404 - {}</h1>" +
                    "<HR size='1' noshade='noshade'><p><b>type</b> Status report</p><p><b>message</b> <u>{}</u></p><p><b>description</b> " +
                    "<u>The requested resource is not available.</u></p><HR size='1' noshade='noshade'><h3>DiyTocmat 1.0.1</h3>" +
                    "</body></html>"; // 响应的文本内容 格式化显示 uri

    // 500 处理
    public static final String RESPONSE_HEAD_500 = "HTTP/1.1 500 Internal Server Error\r\n"
            + "Content-Type: text/html\r\n\r\n";
    public static final String RESPONSE_TEXTFORM_500 = "<html><head><title>DIY Tomcat/1.0.1 - Error report</title><style>"
            + "<!--H1 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:22px;} "
            + "H2 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:16px;} "
            + "H3 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:14px;} "
            + "BODY {font-family:Tahoma,Arial,sans-serif;color:black;background-color:white;} "
            + "B {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;} "
            + "P {font-family:Tahoma,Arial,sans-serif;background:white;color:black;font-size:12px;}"
            + "A {color : black;}A.name {color : black;}HR {color : #525D76;}--></style> "
            + "</head><body><h1>HTTP Status 500 - An exception occurred processing {}</h1>"
            + "<HR size='1' noshade='noshade'><p><b>type</b> Exception report</p><p><b>message</b> <u>An exception occurred processing {}</u></p><p><b>description</b> "
            + "<u>The server encountered an internal error that prevented it from fulfilling this request.</u></p>"
            + "<p>Stacktrace:</p>" + "<pre>{}</pre>" + "<HR size='1' noshade='noshade'><h3>DiyTocmat 1.0.1</h3>"
            + "</body></html>";

    // 以下是分别获取两个文件对象 webapps / rootFolder
    public final static File WEBAPPS_FOLDER = new File(SystemUtil.get("user.dir"), "webapps"); // user.dir -> /Users/liz/Code/diyTomcat5_12 当前项目的绝对路径
    public final static File ROOT_FOLDER = new File(WEBAPPS_FOLDER, "ROOT"); // 定位到ROOT目录

    // 以下两个常量，用于定位server.xml
    public static final File CONF_FOLDER = new File(SystemUtil.get("user.dir"), "conf"); // 定位到conf文件夹 - 返回file对象 以conf 文件夹作为定位
    public static final File SERVER_XML_FILE = new File(CONF_FOLDER, "server.xml"); // 定位到server.xml文件 - 返回file对象

    // 指向 conf/web.xml文件中
    public static final File webXmlFile = new File(CONF_FOLDER, "web.xml"); // 该对象，定位到web.xml文件

    public static final File contextXmlFile = new File(CONF_FOLDER, "context.xml"); // 定位到context.xml文件

    // Jsp进行转译为.java后的存放位置
    public static final String workFolder = SystemUtil.get("user.dir") + File.separator + "work"; // ../work

    // HTTP常用状态码定义
    public static final int CODE_200 = 200;
    public static final int CODE_302 = 302;
    public static final int CODE_404 = 404;
    public static final int CODE_500 = 500;


}
