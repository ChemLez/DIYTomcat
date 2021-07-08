package cn.lizhi.diyTomcat.catalina;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.LogFactory;
import cn.lizhi.diyTomcat.util.Constant;
import cn.lizhi.diyTomcat.util.ServerXMLUtil;
import cn.lizhi.diyTomcat.watcher.WarFileWatcher;

import javax.servlet.ServletException;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Host {

    private String name; // 名称。虚拟主机名 ，request请求中包含 Host主机名
    private Map<String, Context> contextMap; // 用于存放Context对象
    private Engine engine; // 该Host对应的引擎，一个引擎下包含多个Host，而一个Host包含对个Context应用

    public Host(String name, Engine engine)  {
        this.contextMap = new HashMap<>();
        this.name = name;
        this.engine = engine;

        // context对象进行加载
        scanContextsOnWebAppsFolder(); // 从webapps目录下加载context
        scanContextsInServerXML(); // 从xml配置文件下 加载context应用
        scanWarOnWebAppsFolder();
        new WarFileWatcher(this).start();
    }

    private void scanContextsInServerXML()  { // 从server.xml配置文件中加载context对象
        List<Context> contexts = ServerXMLUtil.getContexts(this);
        for (Context context : contexts) {
            contextMap.put(context.getPath(), context);
        }
    }

    private void scanContextsOnWebAppsFolder()  {

        // 当前文件夹

        File[] files = Constant.WEBAPPS_FOLDER.listFiles(); // 从wepapps文件夹下加载所有的context对象  只能获取到一级目录的对象
        assert files != null;
        for (File folder : files) {
            if (folder.isDirectory()) {
                loadContext(folder);
            }
        }
    }

    /**
     * 通过 folder 创建 context，并加载进容器中 -- contextMap
     *
     * @param folder
     */
    private void loadContext(File folder)  {
        String path = folder.getName();
        if ("ROOT".equals(path)) { // ROOT路径单独处理，不在这里进行处理
            path = "/";
        } else {
            path = "/" + path;
        }
        String docBase = folder.getAbsolutePath(); // 获取绝对路径 docBase的最后一级目录就是 path
        Context context = new Context(path, docBase, this, true);
        contextMap.put(context.getPath(), context);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Context getContext(String path) {
        return contextMap.get(path);
    }

    /**
     * 重新创建、加载context对象
     * @param context
     */
    public void reload(Context context)  {
        LogFactory.get().info("Reloading Context with name [{}] has started", context.getPath()); // 新建该context
        // 原context的基本信息
        String path = context.getPath();
        String docBase = context.getDocBase();
        boolean reloadable = context.isReloadable();

        // stop
        context.stop(); // 暂停
        // remove
        contextMap.remove(path); // 移除old context
        // allocate new context
        Context newContext = new Context(path, docBase, this, reloadable);
        // assign it to map
        contextMap.put(path, newContext);
        LogFactory.get().info("Reloading Context with name [{}] has completed", context.getPath());
    }


    /**
     * 把 war 文件解压为目录，并把文件夹加载为 Context
     */
    public void loadWar(File warFile) {
        String fileName = warFile.getName(); // xx.war
        String folderName = StrUtil.subBefore(fileName, ".", true); // 文件夹名称 war
        // 查看是否存在已经有的context
        Context context = getContext("/" + folderName);
        if (context != null) { // 存在直接返回
            return;
        }
        // 查看是否存在对应的文件夹 (文件名和server中配置的名字不一致)
        File folder = new File(Constant.WEBAPPS_FOLDER, folderName);
        if (folder.exists()) { // 该文件存在
            return;
        }
        // 移动war文件，因为jar 命令只支持解压到当前目录下 - war文件还未解压
        File tempWarFile = FileUtil.file(Constant.WEBAPPS_FOLDER, folderName, fileName); // 临时文件夹 ../wepapp/war/xx.war 代表着临时的war文件
        File contextFolder = tempWarFile.getParentFile(); // war/ 文件夹
        contextFolder.mkdir(); // 创建文件夹 ../wepapp/war/ 文件夹
        FileUtil.copyFile(warFile, tempWarFile); // 将该war文件移动到创建的文件夹下面 形成tempFile文件 war文件
        // 解压
        String command = "jar xvf " + fileName; // 解压命令
        Process p = RuntimeUtil.exec(null, contextFolder, command);// 将文件解压至contextFolder ../wepapp/war/解压的文件
        try {
            p.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // 解压之后删除临时的war文件
        tempWarFile.delete();
        // 创建新的Context
        load(contextFolder);

    }

    /**
     * 将一个文件夹 加载成context
     * @param folder
     */
    public void load(File folder) {
        String path = folder.getName(); // 文件名 ../wepapp/war/ 文件夹
        if ("ROOT".equals(path)) { // 根目录
            path = "/";
        } else {
            path = "/" + path;
        }
        String docBase = folder.getAbsolutePath();
        Context context = new Context(path, docBase, this, false);// war包文件不能再修改
        contextMap.put(context.getPath(), context);

    }

    private void scanWarOnWebAppsFolder() {
        File folder = FileUtil.file(Constant.WEBAPPS_FOLDER); // webapp文件夹对象
        File[] files = folder.listFiles();
        assert files != null;
        for (File file : files) {
            if (file.getName().toLowerCase().endsWith(".war")) { // 找寻war文件
                loadWar(file); // 对war文件进行加载
            }
        }
    }

}
