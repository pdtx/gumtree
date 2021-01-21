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
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;

import java.io.*;
import java.util.*;

import static com.github.gumtreediff.client.GumtreeMatch.Language.JAVA;
import static com.github.gumtreediff.client.GumtreeMatch.Language.JS;

/**
 * description:
 *
 * @author fancying
 * create: 2021-01-07 21:07
 **/
public class GumtreeMatch {

    static Map<String, Set<String>> emptyResult = new HashMap<>(0);
    static String charsetName = "UTF-8";
    static String separator = "#";
    static String KEY_INSERT = "INSERT";
    static String KEY_MOVE = "MOVE";
    static String KEY_DELETE = "DELETE";
    static String KEY_UPDATE = "UPDATE";

    /**
     * 对两个文件进行匹
     * @param srcFile 改动后文件
     * @param dstFile 改动前文件
     * @return 匹配结果
     */
    public static Map<String, Set<String>> matchFile(String srcFile, String dstFile){
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
    public static Map<String, Set<String>> matchString(String srcString, String dstString, Language type){
        try {
            TreeContext src = null;
            TreeContext dst = null;
            if (type.equals(Language.JS)) {
                src = new RhinoTreeGenerator().generateFrom().string(srcString);
                dst = new RhinoTreeGenerator().generateFrom().string(dstString);
            }
            if (type.equals(Language.JAVA)) {
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
    private static Map<String, Set<String>> match(TreeContext src, TreeContext dst){
        if(src == null || dst == null){
            return emptyResult;
        }
        Map<String, Set<String>> result = new HashMap<>(4);
        Matcher defaultMatcher = Matchers.getInstance().getMatcher();
        MappingStore mappings = defaultMatcher.match(src.getRoot(), dst.getRoot());
        EditScriptGenerator editScriptGenerator = new SimplifiedChawatheScriptGenerator();
        EditScript actions = editScriptGenerator.computeActions(mappings);
        if(actions == null){
            return emptyResult;
        }
        Set<String> insert = new HashSet<>();
        Set<String> update = new HashSet<>();
        Set<String> delete = new HashSet<>();
        Set<String> move = new HashSet<>();
        for(Action a : actions){
            DefaultTree node = ((DefaultTree)a.getNode());
            Tree dstNode;
            int begin = node.getBeginLine();
            int end = node.getEndLine();
            if(begin == -1 && end == -1){
                continue;
            }
            if(a instanceof Move){
                dstNode = ((Move)a).getValue();
                int dstBegin = ((DefaultTree)dstNode).getBeginLine();
                int dstEnd = ((DefaultTree)dstNode).getEndLine();
                move.add(begin+"-"+end+ separator+ dstBegin+ "-"+dstEnd);
            }else if(a instanceof Insert || a instanceof TreeInsert){
                insert.add(begin+"-"+end);
            }else if(a instanceof Update){
                dstNode = ((Update)a).getValue();
                int dstBegin = ((DefaultTree)dstNode).getBeginLine();
                int dstEnd = ((DefaultTree)dstNode).getEndLine();
                update.add(begin+"-"+end+ separator + dstBegin+ "-"+dstEnd);
            }else if (a instanceof Delete || a instanceof TreeDelete){
                delete.add(begin+"-"+end);
            }
        }
        result.put(KEY_INSERT, insert);
        result.put(KEY_UPDATE, update);
        result.put(KEY_DELETE, delete);
        result.put(KEY_MOVE, move);
        fixConflict(result);
        return result;
    }

    private static void fixConflict(Map<String, Set<String>> map){
        Set<String> update = map.get(KEY_UPDATE);
        Set<String> insert = map.get(KEY_INSERT);
        Set<String> delete = map.get(KEY_DELETE);

        // 1. 若MOVE中存在多个src行号对应一个dst行号的情况，且若src相邻，则考虑合并为一个UPDATE
        // 否则记作多个DELETE和1个INSERT
        Set<String> fixMove = mergeLines(insert, delete, update, new ArrayList<>(map.get(KEY_MOVE)));

        // 2. 若MOVE所在节点行号范围与DELETE, INSERT相同，则只保留前者
        for(String s : fixMove){
            insert.removeIf(i -> i.equals(s.split(separator)[1]));
            delete.removeIf(d -> d.equals(s.split(separator)[0]));
        }

       // 3. 若UPDATE所在节点行号存在多个src对应1个dst的情况，若src相连，则合并，否则记作多个DELETE和INSERT
       Set<String> fixUpdate = mergeLines(insert, delete, update, new ArrayList<>(update));

       // 4. 若UPDATE所在节点行号范围与MOVE,INSERT,DELETE相同，则只保留前者
        for(String s : fixUpdate){
            fixMove.removeIf(m -> m.equals(s));
            insert.removeIf(i -> i.equals(s.split(separator)[1]));
            delete.removeIf(d -> d.equals(s.split(separator)[0]));
        }

        map.replace(KEY_MOVE, fixMove);
        map.replace(KEY_UPDATE, fixUpdate);
    }

    private static Set<String> mergeLines(Set<String> insert, Set<String> delete, Set<String> update, java.util.List<String> list) {
        Set<String> fixList = new HashSet<>();
        Map<String, String> tempMap = new TreeMap<>();
        list.sort(Comparator.naturalOrder());
        for (String s : list) {
            String[] temp = s.split(separator);
            tempMap.putIfAbsent(temp[1], temp[0]);
            if(temp[0].equals(tempMap.get(temp[1]))){
                continue;
            }
            int lastEndLine = Integer.parseInt(tempMap.get(temp[1]).split("-")[1]);
            int newBeginLine = Integer.parseInt(temp[0].split("-")[0]);
            if(newBeginLine - lastEndLine == 1){
                update.add(tempMap.get(temp[1]).split("-")[0]+ "-" + temp[0].split("-")[1]
                        + separator + temp[1]);
            }else {
                insert.add(temp[1]);
                delete.add(temp[0]);
                delete.add(tempMap.get(temp[1]));
            }
            tempMap.remove(temp[1]);
        }
        for(String s : tempMap.keySet()) {
            fixList.add(tempMap.get(s) + separator + s);
        }
        return fixList;
    }

    public static void main(String[] args) {
        // test java of file
        String src = "D:\\gumtree\\javaDiff\\";
        String commitId = "5cb37305f09fd5fbe128f4d3b7319fb5fd7008fc" +"\\";
        String srcFile = src + commitId + "src_main_java_com_test_packageTest3_test13.java";
        String dstFile = src + commitId + "src_main_java_com_test_packageTest3_test13-dst.java";
        String output= src + commitId+ "mapping.json";
        Map<String, Set<String>> result = matchFile(srcFile, dstFile);
        System.out.println(result);
        try {
            outputToFile(output, result.toString());
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    /**
     * 将匹配结果输出到指定文件内
     * @param file 指定文件
     */
    private static void outputToFile(String file, String result) throws IOException {
        File outputFile = new File(file);
        try(FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
            Writer writer = new OutputStreamWriter(fileOutputStream, charsetName)){
            if (!outputFile.getParentFile().exists()) {
                outputFile.getParentFile().mkdirs();
            }
            if(!outputFile.exists()){
                outputFile.createNewFile();
            }
            String jsonString = JSONFormatUtil.formatJson(result);
            writer.write(jsonString);
            writer.flush();
        } catch (Exception e){
            e.printStackTrace();
        }
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
