package org.orbeon.saxon.functions;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.expr.StaticContext;
import org.orbeon.saxon.expr.StaticProperty;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.style.StandardNames;
import org.orbeon.saxon.value.BooleanValue;
import org.orbeon.saxon.xpath.XPathException;


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
        boolean b = isLang(argument[0].evaluateItem(c).getStringValue(), c);
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
    * @param context the context, to identify the context node
    */

    private boolean isLang(String arglang, XPathContext context) {

        Item current = context.getContextItem();
        if (current==null) {
            return false;
        }
        if (!(current instanceof NodeInfo)) {
            return false;
        }

        NodeInfo node = (NodeInfo)current;

        String doclang = null;

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
