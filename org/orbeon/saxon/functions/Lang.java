package net.sf.saxon.functions;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.style.StandardNames;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.BooleanValue;


public class Lang extends SystemFunction {

    /**
    * preEvaluate: this method suppresses compile-time evaluation by doing nothing
    */

    public Expression preEvaluate(StaticContext env) {
        return this;
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
                DynamicError err = new DynamicError("The context item is undefined");
                err.setErrorCode("FONC0001");
                err.setXPathContext(c);
                throw err;
            }
            if (!(current instanceof NodeInfo)) {
                DynamicError err = new DynamicError("The context item is not a node");
                err.setErrorCode("FOTY0011");
                err.setXPathContext(c);
                throw err;
            }
            target = (NodeInfo)current;
        }
        boolean b = isLang(argument[0].evaluateItem(c).getStringValue(), target);
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

    private boolean isLang(String arglang, NodeInfo target) {


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
