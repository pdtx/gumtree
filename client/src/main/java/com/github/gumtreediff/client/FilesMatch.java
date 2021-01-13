package com.github.gumtreediff.client;

import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.EditScriptGenerator;
import com.github.gumtreediff.actions.SimplifiedChawatheScriptGenerator;
import com.github.gumtreediff.gen.jdt.JdtTreeGenerator;
import com.github.gumtreediff.io.ActionsIoUtils;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.TreeContext;

public class FilesMatch {

    public static String charsetName = "UTF-8";

    public static void main(String[] args) throws Exception {
        Run.initGenerators();
        String preFile = "D:\\gumtree\\javaDiff\\AnnotationMapping.java";
        String curFile = "D:\\gumtree\\javaDiff\\AnnotationMapping2.java";
//        String leftInput = "class Main {\n"
//                + "    public static void foo() {\n"
//                + "        a(b);\n"
//                + "    }\n"
//                + "}\n";
//        String rightInput = "class Main {\n"
//                + "    public static void foo() {\n"
//                + "        a.b();\n"
//                + "    }\n"
//                + "}";
//        Tree src = new JdtTreeGenerator().generateFrom().string(leftInput).getRoot();
//        Tree dst = new JdtTreeGenerator().generateFrom().string(rightInput).getRoot();
        TreeContext src = new JdtTreeGenerator().generateFrom().file(preFile);
        TreeContext dst = new JdtTreeGenerator().generateFrom().file(curFile);

        Matcher defaultMatcher = Matchers.getInstance().getMatcher();
        MappingStore mappings = defaultMatcher.match(src.getRoot(), dst.getRoot());
        EditScriptGenerator editScriptGenerator = new SimplifiedChawatheScriptGenerator();
        EditScript actions = editScriptGenerator.computeActions(mappings);

        ActionsIoUtils.ActionSerializer serializer = ActionsIoUtils.toJson(src, actions, mappings);
        serializer.writeTo(System.out);
    }
}
