package org.orbeon.saxon.functions;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.expr.ExpressionVisitor;
import org.orbeon.saxon.expr.StaticProperty;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.om.StandardNames;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.AtomicType;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.value.BooleanValue;


public class Lang extends SystemFunction {

    /**
    * preEvaluate: this method suppresses compile-time evaluation by doing nothing
     * @param visitor an expression visitor
     */

    public Expression preEvaluate(ExpressionVisitor visitor) {
        return this;
    }

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        if (argument.length==1) {
            if (contextItemType == null) {
                XPathException err = new XPathException("The context item for lang() is undefined");
                err.setErrorCode("XPDY0002");
                err.setIsTypeError(true);
                err.setLocator(this);
                throw err;
            } else if (contextItemType instanceof AtomicType) {
                XPathException err = new XPathException("The context item for lang() is not a node");
                err.setErrorCode("XPTY0004");
                err.setIsTypeError(true);
                err.setLocator(this);
                throw err;
            }
        }
        return super.typeCheck(visitor, contextItemType);
    }

    /**
    * Evaluate in a general context
    */

    public Item evaluateItem(XPathContext c) throws XPathException {
        NodeInfo target;
        if (argument.length > 1) {
            target = (NodeInfo)argument[1].evaluateItem(c);
        } else {
            Item current = c.getContextItem();
            if (current==null) {
                XPathException err = new XPathException("The context item is undefined");
                err.setErrorCode("XPDY0002");
                err.setXPathContext(c);
                throw err;
            }
            if (!(current instanceof NodeInfo)) {
                XPathException err = new XPathException("The context item is not a node");
                err.setErrorCode("XPTY0004");
                err.setXPathContext(c);
                throw err;
            }
            target = (NodeInfo)current;
        }
        final Item arg0Val = argument[0].evaluateItem(c);
        final String testLang = (arg0Val==null ? "" : arg0Val.getStringValue());
        boolean b = isLang(testLang, target);
        return BooleanValue.get(b);
    }

    /**
    * Determine the dependencies
    */

    public int getIntrinsicDependencies() {
        return StaticProperty.DEPENDS_ON_CONTEXT_ITEM;
    }

    /**
    * Test whether the context node has the given language attribute
    * @param arglang the language being tested
    * @param target the target node
    */

    public static boolean isLang(String arglang, NodeInfo target) {


        String doclang = null;
        NodeInfo node = target;

        while(node!=null) {
            doclang = node.getAttributeValue(StandardNames.XML_LANG);
            if (doclang!=null) break;
            node = node.getParent();
            if (node==null) return false;
        }

        if (doclang==null) return false;

        if (arglang.equalsIgnoreCase(doclang)) return true;
        int hyphen = doclang.indexOf("-");
        if (hyphen<0) return false;
        doclang = doclang.substring(0, hyphen);
        if (arglang.equalsIgnoreCase(doclang)) return true;
        return false;
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
