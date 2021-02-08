package com.github.gumtreediff.gen.js;

import com.github.gumtreediff.tree.DefaultTree;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.*;

import java.util.HashMap;
import java.util.Map;

import static com.github.gumtreediff.tree.TypeSet.type;

public class BabelTreeVisitor implements NodeVisitor {
    private Map<AstNode, Tree> trees;
    private TreeContext context;

    public BabelTreeVisitor(AstRoot root) {
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
        if (node instanceof AstRoot)
            return true;
        else {
            DefaultTree t = (DefaultTree) buildTree(node);
            Tree p = trees.get(node.getParent());
            p.addChild(t);

            t.setBeginLine(node.getLineno());
            t.setEndLine(node.getLineno());
            if (node instanceof FunctionNode) {
                t.setEndLine(((FunctionNode) node).getEndLineno());
            }

            if (node instanceof Name) {
                Name name = (Name) node;
                t.setLabel(name.getIdentifier());
            } else if (node instanceof StringLiteral) {
                StringLiteral literal = (StringLiteral) node;
                t.setLabel(literal.getValue());
            } else if (node instanceof NumberLiteral) {
                NumberLiteral l = (NumberLiteral) node;
                t.setLabel(l.getValue());
            } else if (node instanceof Comment) {
                Comment c = (Comment) node;
                t.setLabel(c.getValue());
            }

            return true;
        }
    }

    private Tree buildTree(AstNode node) {
        AstNode n = node;
        Tree t = context.createTree(type(Token.typeToName(node.getType())), Tree.NO_LABEL);
        t.setPos(node.getAbsolutePosition());
        t.setLength(node.getLength());
        trees.put(node, t);
        return t;
    }

}
