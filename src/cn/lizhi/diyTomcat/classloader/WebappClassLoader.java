package cn.lizhi.diyTomcat.classloader;

import cn.hutool.core.io.FileUtil;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

// 用于加载某个web应用下的 class 和 jar
public class WebappClassLoader extends URLClassLoader {

    public WebappClassLoader(String docBase, ClassLoader commonClassLoader) {
        super(new URL[]{}, commonClassLoader); // 通过组合的方式，建立类加载器的层次关系
        try {
            File webInfoFolder = new File(docBase, "WEB-INF"); // WEB-INF 目录
            File classesFolder = new File(webInfoFolder, "classes"); // classes 对象 目录下全是classes文件 WEB-INF/classes
            File libFolder = new File(webInfoFolder, "lib"); // 第三方包 WEB-INF/lib
            URL url;
            url = new URL("file:" + classesFolder.getAbsolutePath() + "/"); // classesFolder的url路径 即：class加载的url;将 classes 目录，通过 addURL 加进去
            this.addURL(url);
            List<File> jarFiles = FileUtil.loopFiles(libFolder); // 扫描Jar包 - 将jar包通过 addURL加载进去 - 文件集合
            for (File jarFile : jarFiles) { // 将每个jarFile进行 URL的添加
                url = new URL("file:" + jarFile.getAbsolutePath());
                this.addURL(url);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        try {
            close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


}
