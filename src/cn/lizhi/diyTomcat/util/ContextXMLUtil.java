package cn.lizhi.diyTomcat.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class ContextXMLUtil {

    // 获取WatchedResource标签内容，即：web.xml - servlet的配置在此
    public static String getWatchedResource() {
        try {
            String xml = FileUtil.readUtf8String(Constant.contextXmlFile);
            Document d = Jsoup.parse(xml);
            Element e = d.select("WatchedResource").first();
            return e.text();
        } catch (IORuntimeException ex) {
            ex.printStackTrace();
            return "WEB-INF/web.xml"; // 直接返回这个web.xml配置文件
        }

    }
}
