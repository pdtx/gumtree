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
 * Copyright 2011 Jean-Rémy Falleri
 */

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

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;

@Register(id = "js-rhino", accept = "\\.js$", priority = Registry.Priority.MAXIMUM)
public class RhinoTreeGenerator extends TreeGenerator {
    @Override
    public TreeContext generate(Reader r) throws IOException {
        CompilerEnvirons env = new CompilerEnvirons();
        env.setRecordingLocalJsDocComments(true);
        env.setAllowSharpComments(true);
        env.setRecordingComments(true);
        env.setLanguageVersion(Context.VERSION_ES6);
        Parser p = new Parser(env);
        try {
            StringWriter sw = new StringWriter();
            char[] arr =new char[1024 * 4];
            int n;
            while (-1!=(n=r.read(arr))){
                sw.write(arr,0,n);
            }
            AstRoot root = p.parse(sw.toString(), null, 1);
            RhinoTreeVisitor visitor = new RhinoTreeVisitor(root);
            root.visitAll(visitor);
            return visitor.getTreeContext();
        }
        catch (EvaluatorException e) {
            String message = String.format("Syntax error: %s at line %d, column %d",
                    e.getMessage(),  e.lineNumber(), e.columnNumber());
            throw new SyntaxException(message, e);
        }
    }

}
