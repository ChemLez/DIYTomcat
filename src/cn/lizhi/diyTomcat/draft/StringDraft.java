package cn.lizhi.diyTomcat.draft;

import cn.hutool.core.util.StrUtil;

import java.io.File;

public class StringDraft {

    public static void main(String[] args) {
        String s = "/a/b/index.html";
        String s1 = StrUtil.subBetween(s, "/", "/"); // a;最先符合规则的文本串
        System.out.println(s1);

        System.out.println(File.separator);
        String uri = "/abc/abc.htl";
        uri = StrUtil.removePrefix(uri, "/");
        System.out.println(uri);
    }
}
