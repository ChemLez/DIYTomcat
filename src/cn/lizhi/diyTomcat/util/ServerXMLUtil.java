package cn.lizhi.diyTomcat.util;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.lizhi.diyTomcat.catalina.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.servlet.ServletException;
import java.util.ArrayList;
import java.util.List;

/**
 * 该类用于解析xml
 */
public class ServerXMLUtil {

    /**
     * 返回 xml文件 中的context对象
     * @param host
     * @return
     */
    public static List<Context> getContexts(Host host)  {
        List<Context> result = new ArrayList<>();
        String xml = FileUtil.readUtf8String(Constant.SERVER_XML_FILE);
        Document d = Jsoup.parse(xml);
        Elements es = d.select("context"); // 获取到所有的context节点
        for (Element e : es) {
            String path = e.attr("path");
            String docBase = e.attr("docBase");
            boolean reloadable = Convert.toBool(e.attr("reloadable"), true); // 是否支持重加载，默认设置为true
            Context context = new Context(path, docBase, host, reloadable);
            result.add(context);
        }
        return result;
    }

    private void parseXML(String xml) {

    }

    /**
     * 获取Host的name
     * @return
     */
    public static String getServiceName() {
        String xml = FileUtil.readUtf8String(Constant.SERVER_XML_FILE); // 读取xml
        Document d = Jsoup.parse(xml); // 解析xml
        Element service = d.select("Service").first(); // 获取该Service节点
        return service.attr("name"); // 获取Service的name
    }


    /**
     * 从server.xml中解析出 默认的 host
     * 获取Host的默认name
     * @return
     */
    public static String getEngineDefaultHost() {
        String xml = FileUtil.readUtf8String(Constant.SERVER_XML_FILE);
        Document d = Jsoup.parse(xml);
        Element host = d.select("Engine").first(); //  返回第一个元素
        return host.attr("defaultHost"); // 默认host name
    }

    /**
     * 获取所有的 Host. server.xml 的 host 理论上可以有多个，但是常常是只有一个。
     * @param engine: 包含全部的hosts
     * @return
     */
    public static List<Host> getHosts(Engine engine) {
        List<Host> result = new ArrayList<>();
        String xml = FileUtil.readUtf8String(Constant.SERVER_XML_FILE); // 读取xml
        Document d = Jsoup.parse(xml); // 解析xml

        Elements hosts = d.select("Host"); // 获取全部的Host节点
        for (Element e : hosts) {
            String name = e.attr("name");
            Host host = new Host(name, engine); //
            result.add(host); // 添加进结果集中
        }
        return result;
    }

    /**
     * 获取所有的Connectors集合 - 并解析该标签下的所有属性
     */

    public static List<Connector> getConnectors(Service service) {
        List<Connector> result = new ArrayList<>();
        String xml = FileUtil.readUtf8String(Constant.SERVER_XML_FILE);
        Document d = Jsoup.parse(xml);
        Elements es = d.select("Connector");
        for (Element e : es) {
            int port = Convert.toInt(e.attr("port"));
            String compression = e.attr("compression");
            int compressionMinSize = Convert.toInt(e.attr("compressionMinSize"), 0);
            String noCompressionUserAgents = e.attr("noCompressionUserAgents");
            String compressableMimeType = e.attr("compressableMimeType");
            Connector c = new Connector(service);
            c.setPort(port);
            c.setCompression(compression);
            c.setCompressableMimeType(compressableMimeType);
            c.setNoCompressionUserAgents(noCompressionUserAgents);
            c.setCompressableMimeType(compressableMimeType);
            c.setCompressionMinSize(compressionMinSize);
            result.add(c);
        }
        return result;
    }



}
