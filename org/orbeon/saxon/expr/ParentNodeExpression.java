package net.sf.saxon.expr;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.trans.XPathException;

import java.io.PrintStream;

/**
* Class ParentNodeExpression represents the XPath expression ".." or "parent::node()"
*/

public class ParentNodeExpression extends SingleNodeExpression {

    /**
    * Return the node selected by this SingleNodeExpression
    * @param context The context for the evaluation
    * @return the parent of the current node defined by the context
    */

    public NodeInfo getNode(XPathContext context) throws XPathException {
        Item item = context.getContextItem();
        if (item==null) {
            dynamicError("The context item is not set", context);
        }
        if (item instanceof NodeInfo) {
            return ((NodeInfo)item).getParent();
        } else {
            return null;
        }
    }

    /**
    * Determine which aspects of the context the expression depends on. The result is
    * a bitwise-or'ed value composed from constants such as XPathContext.VARIABLES and
    * XPathContext.CURRENT_NODE
    */
/*
    public int getDependencies() {
        return StaticProperty.CONTEXT_ITEM;
    }
*/
    /**
    * Is this expression the same as another expression?
    */

    public boolean equals(Object other) {
        return (other instanceof ParentNodeExpression);
    }

    /**
    * get HashCode for comparing two expressions
    */

    public int hashCode() {
        return "ParentNodeExpression".hashCode();
    }


    /**
    * Diagnostic print of expression structure
    */

    public void display(int level, NamePool pool, PrintStream out) {
        out.println(ExpressionTool.indent(level) + "..");
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
