package com.github.gumtreediff.client;

import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.EditScriptGenerator;
import com.github.gumtreediff.actions.SimplifiedChawatheScriptGenerator;
import com.github.gumtreediff.actions.model.*;
import com.github.gumtreediff.gen.jdt.JdtTreeGenerator;
import com.github.gumtreediff.gen.js.RhinoTreeGenerator;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.DefaultTree;
import com.github.gumtreediff.tree.TreeContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.gumtreediff.client.GumtreeMatch.Language.*;

/**
 * description:
 *
 * @author fancying
 * create: 2021-01-07 21:07
 **/
public class GumtreeMatch {

    static Map<String, List<String>> emptyResult = new HashMap<>(0);

    /**
     * 对两个文件进行匹配
     * @param srcFile 改动后文件
     * @param dstFile 改动前文件
     * @return 匹配结果
     */
    public static Map<String, List<String>> matchFile(String srcFile, String dstFile){
        try{
            Run.initGenerators();
            Language language = getFileType(dstFile);
            TreeContext src = null;
            TreeContext dst = null;
            switch (language){
                case JAVA:
                    src =  new JdtTreeGenerator().generateFrom().file(srcFile);
                    dst =  new JdtTreeGenerator().generateFrom().file(dstFile);
                    break;
                case JS:
                    src = new RhinoTreeGenerator().generateFrom().file(srcFile);
                    dst = new RhinoTreeGenerator().generateFrom().file(dstFile);
                    break;
                default:
                    break;
            }

            return match(src, dst);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return emptyResult;
    }

    /**
     * 判断文件类型，如java或者js
     * @param dstFile 文件
     * @return Language 语言类型
     */
    private static Language getFileType(String dstFile) {
        if(dstFile.endsWith(Language.JAVA.getSuffix())){
            return JAVA;
        }else if(dstFile.endsWith(Language.JS.getSuffix())){
            return JS;
        }
        return JAVA;
    }

    /**
     * 对两个字符串进行匹配
     * @param srcString 改动前字符串
     * @param dstString 改动后字符串
     * @param type 语言类型
     * @return 匹配结果
     */
    public static Map<String, List<String>> matchString(String srcString, String dstString, Language type){
        try {
            TreeContext src = null;
            TreeContext dst = null;
            if (type.equals(JS)) {
                src = new RhinoTreeGenerator().generateFrom().string(srcString);
                dst = new RhinoTreeGenerator().generateFrom().string(dstString);
            }
            if (type.equals(JAVA)) {
                src = new JdtTreeGenerator().generateFrom().string(srcString);
                dst =  new JdtTreeGenerator().generateFrom().string(dstString);
            }

            return match(src, dst);

        } catch (Exception e){
            e.printStackTrace();
        }
        return emptyResult;
    }

    /**
     * 根据两棵语法树进行匹配
     * @param src 改动后
     * @param dst 改动前
     * @return 匹配结果
     */
    private static Map<String, List<String>> match(TreeContext src, TreeContext dst){
        if(src == null || dst == null){
            return emptyResult;
        }
        Map<String, List<String>> result = new HashMap<>(4);
        Matcher defaultMatcher = Matchers.getInstance().getMatcher();
        MappingStore mappings = defaultMatcher.match(src.getRoot(), dst.getRoot());
        EditScriptGenerator editScriptGenerator = new SimplifiedChawatheScriptGenerator();
        EditScript actions = editScriptGenerator.computeActions(mappings);
        if(actions == null){
            return emptyResult;
        }
        List<String> insert = new ArrayList<>();
        List<String> update = new ArrayList<>();
        List<String> delete = new ArrayList<>();
        List<String> move = new ArrayList<>();
        for(Action a : actions){
            DefaultTree node = ((DefaultTree)a.getNode());
            int begin = node.getBeginLine();
            int end = node.getEndLine();
            if(a instanceof Move){
                move.add(begin+"-"+end);
            }else if(a instanceof Insert || a instanceof TreeInsert){
                insert.add(begin+"-"+end);
            }else if(a instanceof Update){
                update.add(begin+"-"+end);
            }else if (a instanceof Delete || a instanceof TreeDelete){
                delete.add(begin+"-"+end);
            }
        }
        result.put("INSERT", insert);
        result.put("UPDATE", update);
        result.put("DELETE", delete);
        result.put("MOVE", move);
        return result;
    }


    public static void main(String[] args) {
        // test java of file
//        String srcFile = "D:\\gumtree\\javaDiff\\AnnotationMapping.java";
//        String dstFile = "D:\\gumtree\\javaDiff\\AnnotationMapping2.java";
//        System.out.println(matchFile(srcFile, dstFile));

        // test js of file
        String srcJsFile = "D:\\gumtree\\jsDiff\\test1.js";
        String dstJsFile = "D:\\gumtree\\jsDiff\\test2.js";
        System.out.println(matchFile(srcJsFile, dstJsFile));

        // test js of string
        String srcString = "function getCurrentDate(){\n" +
                "    var currentTime= new Date();\n" +
                "    let currentDate= currentTime.getFullYear();\n" +
                "    currentDate+= \"-\"+(currentTime.getMonth()+1)+\"-\"+currentTime.getDate();\n" +
                "    return currentDate;\n" +
                "}";
        String dstString = "function getCurrentDate(){\n" +
                "    var currentTime= new Date();\n" +
                "    let currentDate= currentTime.getFullYear();\n" +
                "    currentDate+= \"-\"+(currentTime.getMonth()+1)+\"-\"+currentTime.getDate();\n" +
                "    return currentDate;\n" +
                "}const option = 2;";
        System.out.println(matchString(srcString, dstString, JS));

    }

    public enum Language{

        /**
         * js
         */
        JS("js"),
        /**
         * java
         */
        JAVA("java");

        public String getSuffix() {
            return suffix;
        }

        private String suffix;

        Language(String suffix){
            this.suffix = suffix;
        }
    }

}
