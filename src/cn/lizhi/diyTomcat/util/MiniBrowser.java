package cn.lizhi.diyTomcat.util;

import cn.hutool.http.HttpUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MiniBrowser {
    /**
     * 客户端 - 用于模拟浏览器
     *
     * @param args
     */

    public static void main(String[] args) {

        String url = "http://localhost:18080/java/header";

        String httpString = getHttpString(url); // 请求体的字符串形式
        System.out.println(httpString);

        System.out.println(getContentString(url));

    }

    public static String getContentString(String url) {
        return getContentString(url, false, null, true);
    }

    public static String getContentString(String url, boolean gzip, Map<String, Object> params, boolean isGet) {

        /**
         * 返回字符串的http响应内容,注意只获取其 响应内容而非全部响应
         */
        byte[] result = getContentBytes(url, gzip, params, isGet);
        if (result == null) {
            return null;
        }
        try {
            return new String(result, StandardCharsets.UTF_8).trim();
        } catch (Exception e) {
            return null;
        }

    }

    public static byte[] getContentBytes(String url, boolean gzip) { // 重载该形式  采用默认的情况
        return getContentBytes(url, gzip, null, true);
    }

    public static byte[] getContentBytes(String url, boolean gzip, Map<String, Object> params, boolean isGet) {
        /**
         * 返回二进制的http响应内容
         */
        byte[] response = getHttpBytes(url, gzip, params, isGet);
        byte[] doubleReturn = "\r\n\r\n".getBytes(); // 响应空行开始的字节处
        int pos = -1; // 用来记录响应空行的开始处
        for (int i = 0; i < response.length - doubleReturn.length; ++i) {
            byte[] temp = Arrays.copyOfRange(response, i, i + doubleReturn.length);
            if (Arrays.equals(temp, doubleReturn)) {
                pos = i;
                break;
            }
        }
        if (pos == -1) {
            return null; // 无响应内容
        }

        pos += doubleReturn.length;
        byte[] result = Arrays.copyOfRange(response, pos, response.length); // http二进制形式的响应内容
        return result;
    }

    public static String getHttpString(String url) {
        return getHttpString(url, false, null, true);
    }

    public static String getHttpString(String url, boolean gzip, Map<String, Object> params, boolean isGet) {
        /**
         * 返回Http响应的字符串形式
         */
        byte[] bytes = getHttpBytes(url, gzip, params, isGet);
        return new String(bytes).trim();
    }

    public static String getHttpString(String url, Map<String, Object> params, boolean isGet) {
        return getHttpString(url, false, params, isGet);
    }

    public static byte[] getHttpBytes(String url, boolean gzip, Map<String, Object> params, boolean isGet) {
        /**
         * 获取整个的http二进制响应
         */
        byte[] result = null;
        String method = isGet ? "GET" : "POST";
        try {
            URL u = new URL(url);
            Socket client = new Socket();
            int port = u.getPort();
            if (port == -1) { // 代表url中port为80
                port = 80;
            }

            // 用于与服务器端建立socket连接
            InetSocketAddress inetSocketAddress = new InetSocketAddress(u.getHost(), port);
            client.connect(inetSocketAddress, 1000); //  build TCP Connect

            // 构造浏览器的请求头
            Map<String, String> requestHeaders = new HashMap<>();
            requestHeaders.put("Host", u.getHost() + ":" + port); // host:port 主机加端口号
            requestHeaders.put("Accept", "text/html"); // 接收数据类型 - 只能够接受text/html的文本类型
            requestHeaders.put("Connection", "close"); // 当前连接状态
            requestHeaders.put("User-Agent", "chemlez mini brower / java1.8");
            if (gzip) {
                requestHeaders.put("Accept-Encoding", "gzip"); // 设置请求头 文件是可压缩类型
            }

            String path = u.getPath(); // 获取路径，只获取到端口号的路径，截止到请求参数前，例如"http://www.ibi.org:8080/java/javap/index.html?isbn=123123&a=c#toc";结果getPath：/java/javap/a.html
            if (path.length() == 0) { // 使用的root路径
                path = "/";

            }

            // 构造请求体
            String firstLine = method + " " + path + " HTTP/1.1\r\n"; // 请求
            StringBuilder httpRequestString = new StringBuilder();
            httpRequestString.append(firstLine);
            Set<String> headers = requestHeaders.keySet();
            for (String header : headers) {
                String headerLine = header + ":" + requestHeaders.get(header) + "\r\n";
                httpRequestString.append(headerLine);
            }

            if (null != params && !isGet) { // post request
                String paramString = HttpUtil.toParams(params); // build request String
                httpRequestString.append("\r\n"); // 添加换行
                httpRequestString.append(paramString); // 请求体
            }

            // 以上用来构造request请求头

            // 通过输出流,将这么一串字符串输出给连接的地址,后面的true是autoFlush,表示开始自动刷新 OutputStream 即，通过浏览器将该request输出到连接的地址处
            PrintWriter pWriter = new PrintWriter(client.getOutputStream(), true); // 将输入流 映射到 该套接字的输出流上
            pWriter.println(httpRequestString); // 将httpRequestString 作为client的输出流 -> 浏览器
            // 这时候你已经将需要的请求所需的字符串发给上面那个url了(通过该输出流 pWriter.println(httpRequestString) ),其实所谓的http协议就是这样,你发给他这么一串符合规范的字符串,他就给你响应,接着他那边就给你返回数据
            // 所以这时候我们开启一个输入流 —— 接收服务端的响应信息
            InputStream is = client.getInputStream(); // 返回 该 套接字的输入流;即 响应消息
            result = readBytes(is, true); // 获取http响应的二进制字节流
            client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static byte[] readBytes(InputStream is, boolean fully) throws IOException {
        /**
         * fully 参数表示是否完全读取
         * 主要因为用来传输a.pdf这个文件。这个文件比较大  那么在传输过程中，可能就不会一次传输 1024个字节，有时候会小于这个字节数，如果读取到的数据小于这个字节就结束的话，那么读取到的文件就是不完整的。
         */

        int buffer_size = 1024;
        // 这里初始化一个输出流,待会存取url返回给我们的数据用
        ByteArrayOutputStream baos = new ByteArrayOutputStream();


        byte[] buffer = new byte[buffer_size];

        int len = -1;
        while ((len = is.read(buffer)) != -1) { // 从输入流中获取数据，即服务器响应的数据
            baos.write(buffer, 0, len); // 将buffer数据输出到baos中
            if (!fully && len != buffer_size) {
                break;
            }
        }
        return baos.toByteArray();

    }
}
