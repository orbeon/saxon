package net.sf.saxon.expr;
import net.sf.saxon.om.DocumentInfo;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.trans.StaticError;
import net.sf.saxon.trans.XPathException;

import java.io.PrintStream;


/**
 * An expression whose value is always a set of nodes containing a single node,
 * the document root.
 * Note that the root of a tree is not necessarily a document node.
*/

public class RootExpression extends SingleNodeExpression {


    /**
    * Simplify an expression
    * @return the simplified expression
    */

     public Expression simplify(StaticContext env) throws StaticError {
        return this;
    }

    /**
    * Is this expression the same as another expression?
    */

    public boolean equals(Object other) {
        return (other instanceof RootExpression);
    }

    /**
    * Specify that the expression returns a singleton
    */

    public final int computeCardinality() {
        return StaticProperty.EXACTLY_ONE;
    }

    /**
    * get HashCode for comparing two expressions
    */

    public int hashCode() {
        return "RootExpression".hashCode();
    }

    /**
    * Return the first element selected by this Expression
    * @param context The evaluation context
    * @return the NodeInfo of the first selected element, or null if no element
    * is selected
    */

    public NodeInfo getNode(XPathContext context) throws XPathException {
        Item current = context.getContextItem();
        if (current==null) {
            dynamicError("Finding root of tree: the context item is undefined", "XP0002", context);
        }
        if (current instanceof NodeInfo) {
            DocumentInfo doc = ((NodeInfo)current).getDocumentRoot();
            if (doc==null) {
                dynamicError("The root of the tree containing the context item is not a document node", "XP0050", context);
            }
            return doc;
        }
        typeError("Finding root of tree: the context item is not a node", "XP0020", context);
        // dummy return; we never get here
        return null;
    }

    /**
    * Determine which aspects of the context the expression depends on. The result is
    * a bitwise-or'ed value composed from constants such as StaticProperty.VARIABLES and
    * StaticProperty.CURRENT_NODE
    */

    public int getIntrinsicDependencies() {
        return StaticProperty.DEPENDS_ON_CONTEXT_DOCUMENT |
                StaticProperty.SINGLE_DOCUMENT_NODESET |
                StaticProperty.CONTEXT_DOCUMENT_NODESET;
    }

    /**
    * Diagnostic print of expression structure
    */

    public void display(int level, NamePool pool, PrintStream out) {
        out.println(ExpressionTool.indent(level) + '/');
    }

}

//
// The contents of this file are subject to the Mozilla Public License Version 1.0 (the "License");
// you may not use this file except in compliance with the License. You may obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations under the License.
//
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
