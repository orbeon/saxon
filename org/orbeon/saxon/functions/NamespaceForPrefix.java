package net.sf.saxon.functions;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.InscopeNamespaceResolver;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NamespaceResolver;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.AnyURIValue;


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
        String prefix = argument[0].evaluateItem(context).getStringValue();
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
