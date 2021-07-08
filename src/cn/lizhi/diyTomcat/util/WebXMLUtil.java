package cn.lizhi.diyTomcat.util;

import cn.hutool.core.io.FileUtil;
import cn.lizhi.diyTomcat.catalina.Context;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class WebXMLUtil {

    private static Map<String, String> mineTypeMapping = new HashMap<>(); // 用于做mimetype的映射，将后缀名和mimetype一一对应

    public static String getWelcomeFile(Context context) { // context 当前request请求中对应的context对象。
        /**
         * 用于获取某个Context下的欢迎文件名称
         * 如果访问: localhost:18080/ -> 就直接访问到 localhost:18080/index.html
         *          localhost:18080/b/ -> 就直接访问 localhost:18080/b/index.html
         */
        String xml = FileUtil.readUtf8String(Constant.webXmlFile); // 获取xml文件字符串
        Document d = Jsoup.parse(xml); // 解析
        Elements es = d.select("welcome-file");// 获取 welcome-file 列表
        for (Element e : es) {
            String welComeFileName = e.text(); // 提取标签中的内容
            File f = new File(context.getDocBase(), welComeFileName); // index.html、index.htm、index.jsp进行目录路径的定位
            if (f.exists()) { // 该文件存在，直接返回文件名称，其实就是这里的welComeFileName
                return f.getName();
            }
        }
        return "index.html"; // 返回默认的index.html
    }

    // 初始化这个Map 因为这里对线程共享的数据，存在写入操作，因此需要进行线程间的同步
    public static synchronized String getMimeType(String extensionName) {

        if (mineTypeMapping.isEmpty()) {
            initMimeType(); // 因为这里 需要进行初始化，所以要保证线程安全，只进行一次集合的初始化
        }
        String mineType = mineTypeMapping.get(extensionName);
        if (mineType == null) {
            return "text/html"; // 默认的返回格式
        }
        return mineType;
    }

    private static void initMimeType() {
        String xml = FileUtil.readUtf8String(Constant.webXmlFile);
        Document d = Jsoup.parse(xml);
        Elements es = d.select("mime-mapping");
        for (Element e : es) {
            String extensionName = e.select("extension").first().text();
            String mineType = e.select("mime-type").text();
            mineTypeMapping.put(extensionName, mineType);
        }
    }


}
