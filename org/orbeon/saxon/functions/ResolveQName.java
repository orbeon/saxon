package net.sf.saxon.functions;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.*;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.QNameValue;


/**
* This class supports the resolve-QName function in XPath 2.0
*/

public class ResolveQName extends SystemFunction {

    /**
    * Evaluate the expression
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        AtomicValue arg0 = (AtomicValue)argument[0].evaluateItem(context);
        if (arg0 == null) {
            return null;
        }

        CharSequence qname = arg0.getStringValueCS();
        String[] parts;
        try {
            parts = Name.getQNameParts(qname);
        } catch (QNameException err) {
            dynamicError(err.getMessage(), "FOCA0002", context);
            return null;
        }

        NodeInfo element = (NodeInfo)argument[1].evaluateItem(context);
        SequenceIterator nsNodes = element.iterateAxis(Axis.NAMESPACE);

        while (true) {
            NodeInfo namespace = (NodeInfo)nsNodes.next();
            if (namespace==null) {
                break;
            }
            String prefix = namespace.getLocalPart();
            if (prefix.equals(parts[0])) {
                return new QNameValue(prefix, namespace.getStringValue(), parts[1]);
            }
        }

        if (parts[0].equals("")) {
            return new QNameValue("", null, parts[1]);
        }

        dynamicError(
            "Namespace prefix '" + parts[0] + "' is not in scope for the selected element", "FONS0004", context);
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
