package net.sf.saxon.functions;
import net.sf.saxon.expr.MappingFunction;
import net.sf.saxon.expr.MappingIterator;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Axis;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.value.StringValue;
import net.sf.saxon.xpath.XPathException;

/**
* This class supports fuctions get-in-scope-prefixes()
*/

public class InScopeNamespaces extends SystemFunction implements MappingFunction {

    /**
    * Iterator over the results of the expression
    */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        NodeInfo element = (NodeInfo)argument[0].evaluateItem(context);
        SequenceIterator nsNodes = element.iterateAxis(Axis.NAMESPACE);
        return new MappingIterator(nsNodes, this, null, null);
    }

    /**
    * Map a namespace node to its name (the namespace prefix)
    */

    public Object map(Item item, XPathContext context, Object info) throws XPathException {
        NodeInfo nsNode = (NodeInfo)item;
        return new StringValue(nsNode.getLocalPart());
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
