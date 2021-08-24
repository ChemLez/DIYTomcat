```
tomcat/
├── b
│   └── index.html
├── conf
│   ├── context.xml 									# tomcat中关于加载context的配置
│   ├── server.xml 										# server服务器的配置
│   └── web.xml 										# web配置
├── javaweb0.war 										# 用于测试加载多个web项目以及web的动态与静态两种加载方式
├── lib  												# 引入的第三方jar包
├── logs 												# 日志文件							
├── src
│   ├── cn
│   │   └── lizhi
│   │       └── diyTomcat
│   │           ├── BootStrap.java						# tomcat启动类
│   │           ├── Thread								# 自定义线程池 - 简略版
│   │           │   ├── BlockingQueue.java
│   │           │   ├── RejectPolicy.java
│   │           │   └── ThreadPool.java
│   │           ├── catalina							# Tomcat相关内置对象的实现
│   │           │   ├── ApplicationFilterChain.java		# 过滤器链
│   │           │   ├── Connector.java					
│   │           │   ├── Context.java
│   │           │   ├── Engine.java
│   │           │   ├── Host.java
│   │           │   ├── HttpProcessor.java
│   │           │   ├── Server.java
│   │           │   ├── Service.java
│   │           │   └── StandardFilterConfig.java
│   │           ├── classloader							# 自定义的类加载器 - 用于加载不同的web项目
│   │           │   ├── CommonClassLoader.java
│   │           │   ├── JspClassLoader.java
│   │           │   └── WebappClassLoader.java
│   │           ├── draft			
│   │           │   └── StringDraft.java
│   │           ├── exception							# 自定义异常处理
│   │           │   └── WebConfigDuplicatedException.java
│   │           ├── http								# http相关的对象设计
│   │           │   ├── ApplicationContext.java			# Context容器 用于和Context类的组合
│   │           │   ├── ApplicationRequestDispatcher.java # 请求转发
│   │           │   ├── BaseRequest.java
│   │           │   ├── BaseResponse.java
│   │           │   ├── BaseServlet.java
│   │           │   ├── Request.java
│   │           │   ├── Response.java
│   │           │   ├── StandardServletConfig.java		# 用于Servlet初始化
│   │           │   └── StandardSession.java			# session对象
│   │           ├── servlet								# 分别对应三种资源的动态转发
│   │           │   ├── DefaultServlet.java
│   │           │   ├── InvokerServlet.java
│   │           │   └── JspServlet.java
│   │           ├── test								# 相关测试文件
│   │           │   ├── TestClassLoader.java
│   │           │   └── TestTomcat.java
│   │           ├── util								# 相关工具类
│   │           │   ├── Constant.java
│   │           │   ├── ContextXMLUtil.java
│   │           │   ├── JspUtil.java
│   │           │   ├── MiniBrowser.java
│   │           │   ├── ServerXMLUtil.java
│   │           │   ├── SessionManager.java
│   │           │   ├── ThreadPoolUtil.java
│   │           │   └── WebXMLUtil.java
│   │           ├── watcher								# 监听器
│   │           │   ├── ContextFileChangeWatcher.java	# 对Context容器的监听
│   │           │   └── WarFileWatcher.java
│   │           └── webappservlet						# 测试Servlet的转发
│   │               └── HelloServlet.java
│   └── log4j.properties
├── webapps												# 相关web资源
│   ├── ROOT
│   │   ├── a.JPG
│   │   ├── a.html
│   │   ├── a.jsp
│   │   ├── a.pdf
│   │   ├── a.txt
│   │   ├── index.html
│   │   └── timeConsume.html
│   ├── a
│   │   ├── a.html
│   │   └── index.html
│   ├── b
│   │   └── index.html
│   ├── c
│   │   └── d
│   │       ├── a.html
│   │       └── index.html
│   └── j2ee
│       └── WEB-INF
│           └── web.xml
└── work											   # 转译和编译的jsp资源
    ├── _
    │   └── org
    │       └── apache
    │           └── jsp
    │               ├── a_jsp.class
    │               └── a_jsp.java
    └── java								
        └── org
            └── apache
                └── jsp
                    ├── hello_jsp.class
                    ├── hello_jsp.java
                    ├── index_jsp.class
                    ├── index_jsp.java
                    ├── jump1_jsp.class
                    └── jump1_jsp.java
```