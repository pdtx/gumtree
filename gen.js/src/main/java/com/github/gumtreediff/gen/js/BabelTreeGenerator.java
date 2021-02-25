package com.github.gumtreediff.gen.js;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.gumtreediff.gen.Register;
import com.github.gumtreediff.gen.Registry;
import com.github.gumtreediff.gen.SyntaxException;
import com.github.gumtreediff.gen.TreeGenerator;
import com.github.gumtreediff.tree.TreeContext;
import org.mozilla.javascript.EvaluatorException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.gumtreediff.gen.js.JSNode.generateNode;

@Register(id = "js-babel", accept = "\\.js$", priority = Registry.Priority.MAXIMUM)
public class BabelTreeGenerator extends TreeGenerator {

    static List<String> ignoreList = Arrays.asList("loc", "range","start", "end", "type", "kind", "_babelType", "value","tokens","token");

    @Override
    public TreeContext generate(Reader r) throws IOException {
        try {
            String line;
            StringBuilder result = new StringBuilder();
            while ((line = ((BufferedReader)r).readLine()) != null) {
                result.append(line);
            }
            JSONObject jsonObject = JSONObject.parseObject(result.toString());
            JSNode root = generateNode(jsonObject);
            if(root == null){
                return null;
            }
            buildTree(getChildren(jsonObject), root);

            BabelTreeVisitor visitor = new BabelTreeVisitor(root);
            root.visitAll(visitor);

            return visitor.getTreeContext();
        } catch (EvaluatorException e) {
            String message = String.format("Syntax error: %s at line %d, column %d",
                    e.getMessage(), e.lineNumber(), e.columnNumber());
            throw new SyntaxException(message, e);
        }
    }

    public void buildTree(List<Object> list, JSNode parent){
        if(list == null || list.isEmpty()){
            return;
        }
        list.forEach(t -> {
            if(t instanceof JSONArray){
                buildTree(Arrays.asList(((JSONArray) t).toArray()), parent);
                return;
            }
            if(! (t instanceof JSONObject)){
                return;
            }
            JSONObject jsonObject = (JSONObject) t;
            JSNode jsNode = generateNode(jsonObject);
            if(jsNode == null){
                return;
            }
            parent.addChild(jsNode);
            jsNode.setParent(parent);

            buildTree(getChildren(jsonObject), jsNode);
        });
    }

    /**
     * 获取JSON对象可以转化为JSNode的子元素
     * 这里暂定拥有位置信息loc的JSON对象才可以转换为JSNode
     * @param jsonObject JSON
     * @return 子元素列表
     */
    List<Object> getChildren(JSONObject jsonObject){
        List<Object> children = new ArrayList<>();
        for(Map.Entry entry : jsonObject.entrySet()){
            if(ignoreList.contains(entry.getKey().toString())){
                continue;
            }
            Object value = entry.getValue();
            // 排除value为null，Integer, Boolean, Double, String 等情况
            if(! (value instanceof JSONObject) && ! (value instanceof JSONArray)){
                continue;
            }
            if(value instanceof JSONArray){
                children.add(generateObjectWithListAsChildren((JSONArray) value));
            }else {
                children.add(value);
            }
        }
        return children;
    }

    /**
     * 为一组元素生成一个无标签的父结点，用于补充建立层级关系
     * @param array 子元素列表
     * @return 父节点
     */
    private JSONObject generateObjectWithListAsChildren(JSONArray array){
        if(array == null || array.isEmpty()){
            return new JSONObject();
        }
        JSONObject jsonObject = new JSONObject();

        for(int i = 0;i< array.size();i++){
            jsonObject.put("children"+ i, array.get(i));
        }

        JSONObject loc = new JSONObject();
        JSONObject start = new JSONObject();
        JSONObject end = new JSONObject();
        AtomicInteger startLine = new AtomicInteger(Integer.MAX_VALUE);
        AtomicInteger endLine = new AtomicInteger(-1);
        array.forEach(a -> {
            JSONObject o = ((JSONObject)a).getJSONObject("loc");
            if(o.getJSONObject("start").getInteger("line") < startLine.get()){
                startLine.set(o.getJSONObject("start").getInteger("line"));
            }
            if(o.getJSONObject("end").getInteger("line") > endLine.get()){
                endLine.set(o.getJSONObject("end").getInteger("line"));
            }
        });
        JSONObject startO = array.getJSONObject(0);
        JSONObject endO = array.getJSONObject(array.size()-1);

        start.put("line", startLine);
        end.put("line", endLine);
        loc.putIfAbsent("start", start);
        loc.putIfAbsent("end", end);
        jsonObject.put("loc", loc);
        jsonObject.putIfAbsent("start", startO.getInteger("start"));
        jsonObject.putIfAbsent("end", endO.getInteger("end"));
        jsonObject.put("type", "list");
        jsonObject.put("value", null);
        return jsonObject;
    }

    public static String string2json(String s) {
        if (s == null)
            return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '/':
                    sb.append("\\/");
                    break;
                default:
                    if (ch <= '\u001F') {
                        String ss = Integer.toHexString(ch);
                        sb.append("\\u");
                        sb.append("0".repeat(4 - ss.length()));
                        sb.append(ss.toUpperCase());
                    } else {
                        sb.append(ch);
                    }
            }
        }
        return sb.toString();
    }
}

