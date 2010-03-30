package org.orbeon.saxon.expr;
import org.orbeon.saxon.trace.ExpressionPresenter;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.om.Axis;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.pattern.AnyNodeTest;

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
            dynamicError("The context item is not set", "XPDY0002", context);
        }
        if (item instanceof NodeInfo) {
            return ((NodeInfo)item).getParent();
        } else {
            dynamicError("The context item for the parent axis (..) is not a node", "XPTY0020", context);
            return null;
        }
    }


    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    public Expression copy() {
        return new ParentNodeExpression();
    }


    /**
     * Add a representation of this expression to a PathMap. The PathMap captures a map of the nodes visited
     * by an expression in a source tree.
     *
     * @param pathMap     the PathMap to which the expression should be added
     * @param pathMapNodeSet
     * @return the pathMapNode representing the focus established by this expression, in the case where this
     *         expression is the first operand of a path expression or filter expression
     */

    public PathMap.PathMapNodeSet addToPathMap(PathMap pathMap, PathMap.PathMapNodeSet pathMapNodeSet) {
        AxisExpression parent = new AxisExpression(Axis.PARENT, AnyNodeTest.getInstance());
        parent.setContainer(getContainer());
        return parent.addToPathMap(pathMap, pathMapNodeSet);
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
     * The toString() method for an expression attempts to give a representation of the expression
     * in an XPath-like form, but there is no guarantee that the syntax will actually be true XPath.
     * In the case of XSLT instructions, the toString() method gives an abstracted view of the syntax
     */

    public String toString() {
        return "..";
    }

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void explain(ExpressionPresenter destination) {
        destination.startElement("parent");
        destination.endElement();
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
