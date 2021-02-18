package com.github.gumtreediff.gen.js;

import com.github.gumtreediff.tree.DefaultTree;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import org.mozilla.javascript.ast.*;

import java.util.HashMap;
import java.util.Map;

import static com.github.gumtreediff.tree.TypeSet.type;

public class BabelTreeVisitor implements NodeVisitor {
    private Map<AstNode, Tree> trees;
    private TreeContext context;

    public BabelTreeVisitor(AstNode root) {
        trees = new HashMap<>();
        context = new TreeContext();
        Tree tree = buildTree(root);
        context.setRoot(tree);
    }



    public TreeContext getTreeContext() {
        return context;
    }

    @Override
    public boolean visit(AstNode node) {
        if(node == null){
            return true;
        }
        DefaultTree t = (DefaultTree) trees.get(node);
        if(t == null){
            t = new DefaultTree(type(((JSNode)node).getKind()), Tree.NO_LABEL);
            trees.put(node, t);
        }
        if(node.getParent() == null){
            return true;
        }
        t.setBeginLine(node.getLineno());
        t.setEndLine(((JSNode)node).getEndLineno());
        t.setLabel(((JSNode) node).getValue());
        t.setPos(node.getPosition());
        t.setLength(node.getLength());
        t.setParent(trees.get(node.getParent()));
        t.getParent().addChild(t);

        return true;
    }

    DefaultTree buildTree(AstNode node) {
        if(node == null){
            return null;
        }
        DefaultTree t = new DefaultTree(type(((JSNode)node).getKind()), Tree.NO_LABEL);
        trees.put(node, t);
        return t;
    }

}
