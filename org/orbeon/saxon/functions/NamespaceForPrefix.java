package net.sf.saxon.functions;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Axis;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.pattern.AnyNodeTest;
import net.sf.saxon.value.StringValue;
import net.sf.saxon.xpath.XPathException;


/**
* This class supports fuction get-namespace-uri-for-prefix()
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
