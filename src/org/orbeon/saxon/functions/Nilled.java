package org.orbeon.saxon.functions;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.BooleanValue;

/**
* This class supports the nilled() function
*/

public class Nilled extends SystemFunction {

    /**
    * Evaluate the function
    */

    public Item evaluateItem(XPathContext c) throws XPathException {
        NodeInfo node = (NodeInfo)argument[0].evaluateItem(c);
        return getNilledProperty(node);
    }

    /**
     * Determine whether a node has the nilled property
     * @param node the node in question (if null, the function returns null)
     * @return the value of the nilled accessor. Returns null for any node other than an
     * element node. For an element node, returns true if the element has been validated and
     * has an xsi:nil attribute whose value is true.
     */

    public static BooleanValue getNilledProperty(NodeInfo node) {
        // TODO: if the type annotation is ANYTYPE, we need to keep an extra bit to represent the nilled
        // property: it will be set only if validation has been performed. A newly-constructed element using
        // validation="preserve" has nilled=false even if xsi:nil = true 
        if (node==null || node.getNodeKind() != Type.ELEMENT) {
            return null;
        }
        return BooleanValue.get(node.isNilled());
//        int typeAnnotation = node.getTypeAnnotation();
//        if (typeAnnotation == -1 || typeAnnotation == StandardNames.XDT_UNTYPED) {
//            return BooleanValue.FALSE;
//        }
//        String val = node.getAttributeValue(StandardNames.XSI_NIL);
//        if (val == null) {
//            return BooleanValue.FALSE;
//        }
//        if (val.trim().equals("1") || val.trim().equals("true")) {
//            return BooleanValue.TRUE;
//        }
//        return BooleanValue.FALSE;
    }

    /**
     * Determine whether a node is nilled. Returns true if the value
     * of the nilled property is true; false if the value is false or absent
     */

    public static boolean isNilled(NodeInfo node) {
        BooleanValue b = getNilledProperty(node);
        return b != null && b.getBooleanValue();
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
