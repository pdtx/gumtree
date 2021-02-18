package com.github.gumtreediff.client;

import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.EditScriptGenerator;
import com.github.gumtreediff.actions.SimplifiedChawatheScriptGenerator;
import com.github.gumtreediff.actions.model.*;
import com.github.gumtreediff.gen.jdt.JdtTreeGenerator;
import com.github.gumtreediff.gen.js.BabelTreeGenerator;
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
                    String babel = "gen.js/src/main/java/com/github/gumtreediff/gen/js/babelEsLint.js".replace("/","\\");
                    String srcCommand = "node " + babel + " " + srcFile;
                    String dstCommand = "node " + babel + " " + dstFile;
                    Process pr = Runtime.getRuntime().exec(srcCommand);
                    InputStream in = pr.getInputStream();
                    BufferedReader r = new BufferedReader(new InputStreamReader(in, charsetName));
                    src = new BabelTreeGenerator().generate(r);
                    r.close();
                    in.close();
                    pr.destroy();

                    Process pr2 = Runtime.getRuntime().exec(dstCommand);
                    InputStream in2 = pr2.getInputStream();
                    BufferedReader r2 = new BufferedReader(new InputStreamReader(in2, charsetName));
                    dst = new BabelTreeGenerator().generate(r2);
                    r2.close();
                    in2.close();
                    pr2.destroy();
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
     * @return 匹配结果 fixme 结果按照行号排序？
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

    /**
     * 处理四种类型之间的行号冲突
     * @param map 匹配结果集 key: insert, delete, update, move
     */
    private static void fixConflict(Map<String, Set<String>> map){
        Set<String> update = map.get(KEY_UPDATE);
        Set<String> insert = map.get(KEY_INSERT);
        Set<String> delete = map.get(KEY_DELETE);

        // 1. 若MOVE中存在多个src行号对应1个dst行号的情况，且若src相邻，则考虑合并为一个UPDATE，否则记作多个DELETE和1个INSERT
        // 若UPDATE存在多个src对应1个dst的情况，若src相连，则合并为一个UPDATE，否则记作多个DELETE和1个INSERT
        Set<String> tempMove = mergeLines(insert, delete, update, new ArrayList<>(map.get(KEY_MOVE)), true);
        Set<String> tempUpdate = mergeLines(insert, delete, update, new ArrayList<>(update), true);

       // 2. 若MOVE存在一个src对应多个dst行号的情况，若dst相邻，则考虑合并为一个UPDATE，否则记作1个DELETE和多个INSERT
        // 若UPDATE存在一个src对应多个dst行号的情况，若dst相邻，则考虑合并为一个UPDATE，否则记作1个DELETE和多个INSERT
        Set<String> fixMove = mergeLines(insert, delete, tempUpdate, new ArrayList<>(tempMove), false);
        Set<String> fixUpdate = mergeLines(insert, delete, tempUpdate, new ArrayList<>(tempUpdate), false);

        // 3. 若UPDATE所在节点行号范围与INSERT,DELETE相同，则移除前者并拆解为DELETE和INSERT
        // 若update只与其中一个同，则移除后者，保留前者
        // MOVE同上
        divideLines(insert, delete, fixUpdate);
        divideLines(insert, delete, fixMove);

        for(String s : insert){
            fixUpdate.removeIf(u -> u.split(separator)[1].equals(s));
            fixMove.removeIf(m -> m.split(separator)[1].equals(s));
        }
        for(String s : delete){
            fixUpdate.removeIf(u -> u.split(separator)[0].equals(s));
            fixMove.removeIf(m -> m.split(separator)[0].equals(s));
        }

        map.replace(KEY_MOVE, fixMove);
        map.replace(KEY_UPDATE, fixUpdate);
    }

    /**
     * 将update和move与insert，delete重复的行号拆解为delete，insert
     * 若update和move只与其中一个同，则移除后者，保留前者
     * @param insert 新增集
     * @param delete 删除集
     * @param fixSet 待拆解集，这里为update和move
     */
    private static void divideLines(Set<String> insert, Set<String> delete, Set<String> fixSet) {
        fixSet.forEach(s -> {
            String[] temp = s.split(separator);
            if(insert.contains(temp[1]) && delete.contains(temp[0])){
                delete.add(temp[0]);
                insert.add(temp[1]);
            }else if (insert.contains(temp[1])){
                insert.remove(temp[1]);
            } else if (delete.contains(temp[0])){
                delete.remove(temp[0]);
            }
        });
    }

    /**
     * 对update和move中多源或多映射的情况进行处理
     * @param insert 新增集
     * @param delete 删除集
     * @param update 修改集
     * @param list 需要被处理的原数据
     * @param srcsToOneDst 是否为多对一，true表示多对一，false表示一对多
     * @return 由原数据处理后得到的新集，一般为处理后的update或move
     */
    private static Set<String> mergeLines(Set<String> insert, Set<String> delete, Set<String> update, java.util.List<String> list, boolean srcsToOneDst) {
        if(list.isEmpty() || list.size() == 1){
            return new HashSet<>(list);
        }
        Set<String> fixSet = new HashSet<>();
        Map<String, String> tempMap = new HashMap<>(list.size());
        list.sort(Comparator.naturalOrder());
        for (String s : list) {
            String[] temp = s.split(separator);
            String key = srcsToOneDst ? temp[1] : temp[0];
            String value = srcsToOneDst ? temp[0] : temp[1];
            tempMap.putIfAbsent(key, value);
            if(value.equals(tempMap.get(key))){
                continue;
            }
            int lastEndLine = Integer.parseInt(tempMap.get(key).split("-")[1]);
            int newBeginLine = Integer.parseInt(value.split("-")[0]);
            if(newBeginLine - lastEndLine == 1){
                update.add(srcsToOneDst ? tempMap.get(key).split("-")[0]+ "-" + value.split("-")[1]
                        + separator + key
                        : key + separator
                        + tempMap.get(key).split("-")[0]+ "-" + value.split("-")[1]);
            }else if(newBeginLine - lastEndLine > 1){
                insert.add(temp[1]);
                delete.add(temp[0]);
                if(srcsToOneDst){
                    delete.add(tempMap.get(key));
                }else {
                    insert.add(tempMap.get(key));
                }
            }
            tempMap.remove(key);
        }
        tempMap.keySet().forEach(t -> fixSet.add(
                srcsToOneDst ? tempMap.get(t) + separator + t
                        : t + separator + tempMap.get(t)
        ));
        return fixSet;
    }

    public static void main(String[] args) {
        // test java of file
        String src = "D:\\gumtree\\javaDiff\\";
        String commitId = "9b91a2506e638bc95711432967a4853726fc1aeb" +"\\";
        String srcFile = src + commitId + "src_main_java_com_test_packageTest1_testRename_testRename4.java";
        String dstFile = src + commitId + "src_main_java_com_test_packageTest1_testRename_testRename4-dst.java";
        String jsSrc = "D:\\gumtree\\jsDiff\\";
        String jsSrcFile = jsSrc + "Measure.js";
        String jsDstFile = jsSrc + "Measure2.js";
        String output= src + commitId+ "mapping.json";
        Map<String, Set<String>> result = matchFile(jsSrcFile, jsDstFile);
        System.out.println(result);
//        try {
//            outputToFile(output, result.toString());
//        } catch (IOException e){
//            e.printStackTrace();
//        }
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

    /**
     * 可以处理的文件类型
     */
    public enum Language{

        /**
         * js
         */
        JS(".js"),
        /**
         * java
         */
        JAVA(".java");

        public String getSuffix() {
            return suffix;
        }

        private String suffix;

        Language(String suffix){
            this.suffix = suffix;
        }
    }

}
