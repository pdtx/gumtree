package com.github.gumtreediff.client;

import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.EditScriptGenerator;
import com.github.gumtreediff.actions.SimplifiedChawatheScriptGenerator;
import com.github.gumtreediff.gen.TreeGenerators;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.Tree;

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
            String srcFile = "C:\\Users\\fancy\\Desktop\\AccountServiceApplication.java";
            String dstFile = "C:\\Users\\fancy\\Desktop\\AccountServiceApplication1.java";
            Tree src =  TreeGenerators.getInstance().getTree(srcFile).getRoot();
            Tree dst =  TreeGenerators.getInstance().getTree(dstFile).getRoot();
            Matcher defaultMatcher = Matchers.getInstance().getMatcher();
            MappingStore mappings = defaultMatcher.match(src, dst);
            EditScriptGenerator editScriptGenerator = new SimplifiedChawatheScriptGenerator();
            EditScript actions = editScriptGenerator.computeActions(mappings);
            System.out.printf("done");
        }catch (Exception ignore) {
            ignore.printStackTrace();
        }

    }
}
