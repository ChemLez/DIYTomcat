package cn.lizhi.diyTomcat.exception;

// 自定义异常，在配置 web.xml 里面发生 servlet 重复配置的时候会抛出。
public class WebConfigDuplicatedException extends Exception {

    public WebConfigDuplicatedException(String msg) {
        super(msg);
    }
}
