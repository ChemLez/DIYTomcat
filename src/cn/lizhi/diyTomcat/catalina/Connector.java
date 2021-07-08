package cn.lizhi.diyTomcat.catalina;

import cn.hutool.log.LogFactory;
import cn.lizhi.diyTomcat.http.Request;
import cn.lizhi.diyTomcat.http.Response;
import cn.lizhi.diyTomcat.util.ThreadPoolUtil;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Connector implements Runnable {

    int port; // 端口号
    private Service service; // 该Connector对应的Service
    // 压缩相关参数
    private String compression; // 该端口是否开启压缩
    private int compressionMinSize; // 最小进行压缩的字节数
    private String noCompressionUserAgents; // 表示不进行压缩的浏览器
    private String compressableMimeType; // 需要进行压缩的类型

    public String getCompression() {
        return compression;
    }

    public void setCompression(String compression) {
        this.compression = compression;
    }

    public int getCompressionMinSize() {
        return compressionMinSize;
    }

    public void setCompressionMinSize(int compressionMinSize) {
        this.compressionMinSize = compressionMinSize;
    }

    public String getNoCompressionUserAgents() {
        return noCompressionUserAgents;
    }

    public void setNoCompressionUserAgents(String noCompressionUserAgents) {
        this.noCompressionUserAgents = noCompressionUserAgents;
    }

    public String getCompressableMimeType() {
        return compressableMimeType;
    }

    public void setCompressableMimeType(String compressableMimeType) {
        this.compressableMimeType = compressableMimeType;
    }

    public Connector(Service service) {
        this.service = service;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public Service getService() {
        return service;
    }

    public void init() {
        LogFactory.get().info("Initializing ProtocolHandler [http-bio-{}]", port); // 日志记录
    }

    // 作用是创建一个线程，以当前类为任务，启动运行，并打印tomcat风格的日志
    public void start() {
        LogFactory.get().info("Starting ProtocolHandler [http-bio-{}]", port);
        new Thread(this).start(); // 以该类实例化对象作为任务进行启动 传入这个对象，该对象在堆上，线程共享。这里是采用多线程的目的，是同时启动三个线程，用来监听18081,18082,18083三个端口
    }

    @Override
    public void run() { // 只用来进行接收请求
        try {
            ServerSocket ss = new ServerSocket(port); // 服务端socket 用来接收客户端请求 用于监听该端口的请求
            while (true) {
                Socket s = ss.accept(); // 这里是服务器端对该套接字端口进行监听，当有连接请求时，服务器端会创建一个新的套接字用于和当前这个特定的客户端进行连接，数据的传输通过这个新的套接字；而原来的ss套接字用来继续监听其他的客户端请求(TCP连接中的第一次请求)
                // 内置写需要执行的任务
                Runnable r = () -> { // 以下是每个执行任务 线程私有的 多个请求访问同一个端口。这里的多线程是，每个端口进行并发请求。
                    try {
                        HttpProcessor httpProcessor = new HttpProcessor(); // 用于处理请求
                        Request request = new Request(s, Connector.this); // 将客户端信息进行封装
                        Response response = new Response();
                        httpProcessor.execute(s, request, response); // 处理请求
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }; // 定义线程任务
                ThreadPoolUtil.run(r);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
