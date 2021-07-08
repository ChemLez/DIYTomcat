package cn.lizhi.diyTomcat;


import cn.lizhi.diyTomcat.classloader.CommonClassLoader;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 启动类
 */
public class BootStrap {

    public static void main(String[] args) throws ClassNotFoundException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {

        // 修改成通过CommonClassLoader加载Server类，然后实例化,再通过反射的方式调用其start方法  下面这种方式实现了方法和执行方法对象的解耦
        CommonClassLoader commonClassLoader = new CommonClassLoader();
        Thread.currentThread().setContextClassLoader(commonClassLoader); // 将CommonClassLoader设置到当前线程(主线程中)

        String serverClassName = "cn.lizhi.diyTomcat.catalina.Server"; // 该Server的全类名
        Class<?> serverClass = commonClassLoader.loadClass(serverClassName); // 通过该加载器来加载该类，获取该类的Class对象
        Object serverObject = serverClass.newInstance(); // 获取实例化该对象 - 即后续执行该方法的对象
        Method m = serverClass.getMethod("start"); // 获取该类的方法对象
        m.invoke(serverObject); // 使用该对象执行该方法

    }

}
