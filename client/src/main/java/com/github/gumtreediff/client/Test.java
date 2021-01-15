package com.github.gumtreediff.client;

import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.EditScriptGenerator;
import com.github.gumtreediff.actions.SimplifiedChawatheScriptGenerator;
import com.github.gumtreediff.actions.model.*;
import com.github.gumtreediff.gen.TreeGenerators;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.DefaultTree;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * description:
 *
 * @author fancying
 * create: 2021-01-07 21:07
 **/
public class Test {

    public static void main(String[] args) {
        try {
            Run.initGenerators();
            String srcFile = "D:\\gumtree\\javaDiff\\AnnotationMapping.java";
            String dstFile = "D:\\gumtree\\javaDiff\\AnnotationMapping2.java";
            TreeContext src =  TreeGenerators.getInstance().getTree(srcFile);
            TreeContext dst =  TreeGenerators.getInstance().getTree(dstFile);
            Tree srcTree = src.getRoot();
//            TreeContext src =  new JdtTreeGenerator().generateFrom().file(srcFile);
//            TreeContext dst =  new JdtTreeGenerator().generateFrom().file(dstFile);

            // test js of file
//            String srcJsFile = "D:\\gumtree\\jsDiff\\test1.js";
//            String dstJsFile = "D:\\gumtree\\jsDiff\\test2.js";
//            TreeContext src = new RhinoTreeGenerator().generateFrom().file(srcFile);
//            TreeContext dst = new RhinoTreeGenerator().generateFrom().file(dstFile);

            // test js of string
//            String srcJsFile = "function getCurrentDate(){\n" +
//                    "    var currentTime= new Date();\n" +
//                    "    let currentDate= currentTime.getFullYear();\n" +
//                    "    currentDate+= \"-\"+(currentTime.getMonth()+1)+\"-\"+currentTime.getDate();\n" +
//                    "    return currentDate;\n" +
//                    "}";
//            String dstJsFile = "function getCurrentDate(){\n" +
//                    "    var currentTime= new Date();\n" +
//                    "    let currentDate= currentTime.getFullYear();\n" +
//                    "    currentDate+= \"-\"+(currentTime.getMonth()+1)+\"-\"+currentTime.getDate();\n" +
//                    "    return currentDate;\n" +
//                    "}const option = 2;";
//            TreeContext src = new RhinoTreeGenerator().generateFrom().string(srcJsFile);
//            TreeContext dst = new RhinoTreeGenerator().generateFrom().string(dstJsFile);

            Matcher defaultMatcher = Matchers.getInstance().getMatcher();
            MappingStore mappings = defaultMatcher.match(src.getRoot(), dst.getRoot());
            EditScriptGenerator editScriptGenerator = new SimplifiedChawatheScriptGenerator();
            EditScript actions = editScriptGenerator.computeActions(mappings);
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
            Map<String, List<String>> result = new HashMap<>(4);
            result.put("INSERT", insert);
            result.put("UPDATE", update);
            result.put("DELETE", delete);
            result.put("MOVE", move);

            System.out.println(result.toString());

//            ANTLRStringStream fs = new ANTLRFileStream(srcFile);
//            fs.name = srcFile;

//            Lexer lexer = new ActionAnalysis(fs);
//            BufferedTokenStream tokenStream = new BufferedTokenStream(lexer);
//            List<? extends Token> tokens = tokenStream.getTokens();

//            System.out.println(tokens.size());

//            ActionsIoUtils.ActionSerializer actionSerializer = ActionsIoUtils.toJson(src, actions, mappings);
//            actionSerializer.writeTo(System.out);

        }catch (Exception ignore) {
            ignore.printStackTrace();
        }

    }
}
