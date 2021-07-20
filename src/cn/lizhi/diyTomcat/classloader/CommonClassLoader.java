package cn.lizhi.diyTomcat.classloader;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * URLClassLoader: 实现了从硬盘上以绝对路径的方式加载类
 */
public class CommonClassLoader extends URLClassLoader {

    // java默认的在调用子类构造方法前先调用父类的构造方法，如果你没有指定调用父类的哪个构造方法，那么java默认调用父类无参数的构造方法  在这里定义出需要加载的哪些类
    public CommonClassLoader() { //
        super(new URL[]{}); // 调用父类的构造器 来完成对象的实例化
        try {
            File workingFolder = new File(System.getProperty("user.dir")); // 当前项目目录
            File libFolder = new File(workingFolder, "lib");// 定位到lib文件夹
            File[] jarFiles = libFolder.listFiles();
            assert jarFiles != null;
            for (File file : jarFiles) {
                if (file.getName().endsWith("jar")) { //  扫描 lib 目录下的jar, 然后通过 addURL 加到当前的库里面去
                    URL url = new URL("file:" + file.getAbsolutePath());
                    this.addURL(url);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
