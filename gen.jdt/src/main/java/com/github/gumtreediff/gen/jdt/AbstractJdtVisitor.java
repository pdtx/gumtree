/*
 * This file is part of GumTree.
 *
 * GumTree is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GumTree is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GumTree.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2011-2015 Jean-Rémy Falleri <jr.falleri@gmail.com>
 * Copyright 2011-2015 Floréal Morandat <florealm@gmail.com>
 */

package com.github.gumtreediff.gen.jdt;

import com.github.gumtreediff.gen.jdt.cd.EntityType;
import com.github.gumtreediff.tree.DefaultTree;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.tree.Type;
import org.eclipse.jdt.core.dom.*;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import static com.github.gumtreediff.tree.TypeSet.type;

public abstract class AbstractJdtVisitor extends ASTVisitor {

    protected TreeContext context = new TreeContext();

    protected Deque<Tree> trees = new ArrayDeque<>();

    public int[] lineEndTable;
    int length = 0;

    public AbstractJdtVisitor() {
        super(true);
    }

    public TreeContext getTreeContext() {
        return context;
    }

    protected void pushNode(ASTNode n, String label) {
        push(n, nodeAsSymbol(n), label, n.getStartPosition(), n.getLength());
    }

    protected void pushNode(ASTNode n, Type type, int pos, int length) {
        push(n, type, "", pos, length);
    }

    protected void pushFakeNode(EntityType n, int startPosition, int length) {
        Type type = type(n.name()); // FIXME is that consistent with AbstractJDTVisitor.type
        push(null, type,"", startPosition, length);
    }

    protected void push(ASTNode n, Type type, String label, int startPosition, int length) {
        Tree t = context.createTree(type, label);
        t.setPos(startPosition);
        t.setLength(length);
        try{
            if(n instanceof CompilationUnit) {
                Field f = n.getClass().getDeclaredField("lineEndTable");
                if(f != null){
                    f.setAccessible(true);
                    lineEndTable =(int[]) f.get(n);
                    this.length = lineEndTable.length;
                }
            }
        } catch (NoSuchFieldException | IllegalAccessException e){
            e.printStackTrace();
        }
        if(lineEndTable != null){
            ((DefaultTree)t).setBeginLine(getLine(t.getPos()));
            ((DefaultTree)t).setEndLine(getLine(t.getPos()+t.getLength()));
        }
        if (trees.isEmpty())
            context.setRoot(t);
        else {
            Tree parent = trees.peek();
            t.setParentAndUpdateChildren(parent);
        }

        if (n instanceof TypeDeclaration)
            t.setMetadata("id", getId((TypeDeclaration) n));
        else if (n instanceof MethodDeclaration)
            t.setMetadata("id", getId((MethodDeclaration) n));
        else if (n instanceof FieldDeclaration)
            t.setMetadata("id", getId((FieldDeclaration) n));
        else if (n instanceof EnumDeclaration)
            t.setMetadata("id", getId((EnumDeclaration) n));

        trees.push(t);
    }

    private int getLine(int pos) {
        if(lineEndTable != null){
            int i = 0;
            while(i < length && pos >= lineEndTable[i]){
                if(pos == lineEndTable[i]){
                    break;
                }
                i++;
            }
            return i+1;
        }
        return -1;
    }

    private String getId(TypeDeclaration d) {
        return "Type " + d.getName();
    }

    private String getId(EnumDeclaration d) {
        return "Enum " + d.getName();
    }

    private String getId(MethodDeclaration d) {
        StringBuilder b = new StringBuilder();
        b.append("Method ");
        b.append(d.getName() + "(");
        for (SingleVariableDeclaration v : (List<SingleVariableDeclaration>) d.parameters())
            b.append(" " + v.getType().toString());
        b.append(")");
        return b.toString();
    }

    private String getId(FieldDeclaration d) {
        return "Field " + ((VariableDeclarationFragment) d.fragments().get(0)).getName();
    }

    protected Tree getCurrentParent() {
        return trees.peek();
    }

    protected void popNode() {
        trees.pop();
    }

    protected static Type nodeAsSymbol(ASTNode node) {
        return nodeAsSymbol(node.getNodeType());
    }

    protected static Type nodeAsSymbol(int id) {
        return type(ASTNode.nodeClassForType(id).getSimpleName());
    }
}
