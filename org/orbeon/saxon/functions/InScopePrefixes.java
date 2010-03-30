package org.orbeon.saxon.functions;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.sort.IntIterator;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.BuiltInAtomicType;
import org.orbeon.saxon.value.StringValue;

/**
* This class supports fuctions get-in-scope-prefixes()
*/

public class InScopePrefixes extends SystemFunction {

    /**
    * Iterator over the results of the expression
    */

    public SequenceIterator iterate(final XPathContext context) throws XPathException {
        final NodeInfo element = (NodeInfo)argument[0].evaluateItem(context);
        final IntIterator iter = NamespaceCodeIterator.iterateNamespaces(element);
        final NamePool pool = context.getNamePool();

        return new SequenceIterator() {
            private Item current = null;
            private int position = 0;

            public Item current() {
                return current;
            }

            public SequenceIterator getAnother() throws XPathException {
                return iterate(context);
            }

            public int getProperties() {
                return 0;
            }

            public Item next() throws XPathException {
                if (position == 0) {
                    current = new StringValue("xml");
                    position++;
                    return current;
                } else if (iter.hasNext()) {
                    String prefix = pool.getPrefixFromNamespaceCode(iter.next());
                    if (prefix.length() == 0) {
                        current = StringValue.EMPTY_STRING;
                    } else {
                        current = StringValue.makeRestrictedString(
                                prefix, BuiltInAtomicType.NCNAME, null).asAtomic();
                    }
                    position++;
                    return current;
                } else {
                    current = null;
                    position = -1;
                    return null;
                }
            }

            public int position() {
                return position;
            }

            public void close() {
            }
        };

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
