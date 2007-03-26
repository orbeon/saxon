package org.orbeon.saxon.functions;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.InscopeNamespaceResolver;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.NamespaceResolver;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.AnyURIValue;
import org.orbeon.saxon.value.StringValue;


/**
* This class supports the function namespace-uri-for-prefix()
*/

public class NamespaceForPrefix extends SystemFunction {

    /**
     * Evaluate the function
     * @param context the XPath dynamic context
     * @return the URI corresponding to the prefix supplied in the first argument, or null
     * if the prefix is not in scope
     * @throws XPathException if a failure occurs evaluating the arguments
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        NodeInfo element = (NodeInfo)argument[1].evaluateItem(context);
        StringValue p = (StringValue)argument[0].evaluateItem(context);
        String prefix;
        if (p == null) {
            prefix = "";
        } else {
            prefix = p.getStringValue();
        }
        NamespaceResolver resolver = new InscopeNamespaceResolver(element);
        String uri = resolver.getURIForPrefix(prefix, true);
        if (uri == null) {
            return null;
        }
        return new AnyURIValue(uri);
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
