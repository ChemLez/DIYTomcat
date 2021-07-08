package cn.lizhi.diyTomcat.classloader;

import cn.hutool.core.util.StrUtil;
import cn.lizhi.diyTomcat.catalina.Context;
import cn.lizhi.diyTomcat.util.Constant;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

/**
 * 一个jsp文件对应一个JspClassLoader
 * 如果这个jsp文件进行了修改，那么就要换一个新的JspClassLoader
 * JspClassLoader 基于 由 jsp 文件转移并编译出来的 class 文件，进行类的加载
 */
public class JspClassLoader extends URLClassLoader {

    private static Map<String, JspClassLoader> map = new HashMap<>(); // 做jsp文件和JspClassLoader映射的

    /**
     * 基于WebClassLoader进行创建
     * 类加载器的设定：
     * 1.找出需要加载的 文件夹或文件 都是全路径
     * 2.构造URL对象 -> new URL("file" + fileName/dictionaryName)
     * 3.通过this.addURL() 将其添加进行
     * @param context
     */
    private JspClassLoader(Context context) {
        super(new URL[]{},context.getWebappClassLoader()); // this.webappClassLoader = new WebappClassLoader(docBase, commonClassLoader)  这里奠定了JspClassLoader是WebappClassLoader层级以下
        String subFolder; // work下jsp的对应的class目录
        String path = context.getPath();
        if ("/".equals(path)) {
            subFolder = "_";
        } else {
            subFolder = StrUtil.subAfter(path, "/", false);
        }
        File classLoaderFolder = new File(Constant.workFolder, subFolder);
        try {
            URL url = new URL("file:" + classLoaderFolder.getAbsolutePath() + "/"); // 需要加载的class
            this.addURL(url);

        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 让jsp和class文件取消关联
     * @param uri
     * @param context
     */
    public static void invalidJspClassLoader(String uri, Context context) {
        String key = context.getPath() + "/" + uri; // jsp文件路径
        map.remove(key);
    }

    /**
     * 让Jsp和JspClassLoader进行对应，如果没有该Jsp对应的ClassLoader就创建一个和它对应
     * @param uri
     * @param context
     * @return
     */
    public static JspClassLoader getJspClassLoader(String uri, Context context) {
        String key = context.getPath() + "/" + uri;
        JspClassLoader loader = map.get(key);
        if (loader == null) {
            loader = new JspClassLoader(context);
            map.put(key, loader);
        }
        return loader;
    }
}
