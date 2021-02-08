package com.github.gumtreediff.gen.js;

import com.github.gumtreediff.gen.Register;
import com.github.gumtreediff.gen.Registry;
import com.github.gumtreediff.gen.SyntaxException;
import com.github.gumtreediff.gen.TreeGenerator;
import com.github.gumtreediff.tree.TreeContext;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.ast.AstRoot;

import java.io.*;

@Register(id = "js-babel", accept = "\\.js$", priority = Registry.Priority.MAXIMUM)
public class BabelTreeGenerator extends TreeGenerator {
    @Override
    public TreeContext generate(Reader r) throws IOException {
        CompilerEnvirons env = new CompilerEnvirons();
        env.setRecordingLocalJsDocComments(true);
        env.setAllowSharpComments(true);
        env.setRecordingComments(true);
        env.setReservedKeywordAsIdentifier(false);
        env.setLanguageVersion(Context.VERSION_ES6);
        Parser p = new Parser(env);
        try {
            String babel = "gen.js/src/main/java/com/github/gumtreediff/gen/js/babelEsLint.js";
            String file = "C:\\Users\\50306\\Desktop\\test\\a.js";
            String command = "node " + babel + " " + file;
            Process pr = Runtime.getRuntime().exec(command);
            InputStream in = pr.getInputStream();
            BufferedReader read = new BufferedReader(new InputStreamReader(in));
//            String line = null;
//            StringBuilder result = new StringBuilder();
//            while ((line = read.readLine()) != null) {
//                result.append(line);
//            }
            AstRoot root = (AstRoot) (Object) read;
            BabelTreeVisitor visitor = new BabelTreeVisitor(root);
            root.visitAll(visitor);
            return visitor.getTreeContext();
        } catch (EvaluatorException e) {
            String message = String.format("Syntax error: %s at line %d, column %d",
                    e.getMessage(), e.lineNumber(), e.columnNumber());
            throw new SyntaxException(message, e);
        }
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

