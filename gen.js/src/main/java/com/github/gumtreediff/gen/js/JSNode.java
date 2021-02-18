package com.github.gumtreediff.gen.js;

import com.alibaba.fastjson.JSONObject;
import org.mozilla.javascript.Node;
import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.NodeVisitor;

public class JSNode extends AstNode {


    public JSNode(String kind) {
        this.kind = kind;
    }

    public JSNode(int pos) {
        super(pos);
    }

    public String getKind() {
        return kind;
    }

    public int getEndLineno() {
        return endLineno;
    }

    public void setEndLineno(int endLineno) {
        this.endLineno = endLineno;
    }

    private int endLineno;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    private String value;

    public static JSNode generateNode(JSONObject jsonObject){
        if(jsonObject == null){
            return null;
        }
        JSONObject loc = (JSONObject) jsonObject.get("loc");
        if(loc == null){
            return null;
        }

        int positionStart = jsonObject.getInteger("start");
        int positionEnd = jsonObject.getInteger("end");
        String kind = jsonObject.getString("kind");

        JSNode node = new JSNode(positionStart);
        node.setLineno(loc.getJSONObject("start").getInteger("line"));
        node.setEndLineno(loc.getJSONObject("end").getInteger("line"));
        node.setPosition(positionStart);
        node.setLength(positionEnd-positionStart+1);
        if(kind == null){
            kind = jsonObject.getString("type");
        }
        if(kind == null){
            kind = jsonObject.getString("_babelType");
        }
        if(kind == null){
            kind = jsonObject.getString("sourceType");
        }

        String value = jsonObject.getString("value");
        if(value == null){
            value = jsonObject.getString("name");
        }
        if(value == null){
            value = jsonObject.getString("identifierName");
        }
        node.setBounds(positionStart, positionEnd);
        node.setKind(kind);
        node.setValue(value);
        return node;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    private String kind;


    @Override
    public String toSource(int depth) {
        StringBuilder sb = new StringBuilder();
        for (Node node : this) {
            sb.append(((AstNode)node).toSource(depth));
            if(node.getType() == Token.COMMENT) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    public void visitAll(NodeVisitor visitor) {
        visit(visitor);
    }


    @Override
    public void visit(NodeVisitor v) {
        if (v.visit(this)) {
            for (Node kid : this) {
                ((AstNode)kid).visit(v);
            }
        }
    }
}
