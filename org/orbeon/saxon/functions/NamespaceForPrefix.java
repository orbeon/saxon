package org.orbeon.saxon.functions;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.Axis;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.value.StringValue;
import org.orbeon.saxon.xpath.XPathException;


/**
* This class supports the function namespace-uri-for-prefix()
*/

public class NamespaceForPrefix extends SystemFunction {

    public Item evaluateItem(XPathContext context) throws XPathException {
        NodeInfo element = (NodeInfo)argument[1].evaluateItem(context);
        String prefix = argument[0].evaluateItem(context).getStringValue();
        SequenceIterator nsNodes = element.iterateAxis(Axis.NAMESPACE);
        while (true) {
            NodeInfo ns = (NodeInfo)nsNodes.next();
            if (ns == null) {
                break;
            }
            if (ns.getLocalPart().equals(prefix)) {
                return new StringValue(ns.getStringValue());
            }
        }
        return null;
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
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
