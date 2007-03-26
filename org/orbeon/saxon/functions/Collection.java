package org.orbeon.saxon.functions;
import org.orbeon.saxon.CollectionURIResolver;
import org.orbeon.saxon.value.AtomicValue;
import org.orbeon.saxon.value.AnyURIValue;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.EmptyIterator;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.trans.DynamicError;

/**
 * Implement the fn:collection() function. This is responsible for calling the
 * registered {@link CollectionURIResolver}. For the effect of the default
 * system-supplied CollectionURIResolver, see {@link StandardCollectionURIResolver}
 */

public class Collection extends SystemFunction {

    private String expressionBaseURI = null;

    public void checkArguments(StaticContext env) throws XPathException {
        if (expressionBaseURI == null) {
            super.checkArguments(env);
            expressionBaseURI = env.getBaseURI();
        }
    }

    /**
    * preEvaluate: this method suppresses compile-time evaluation by doing nothing
    */

    public Expression preEvaluate(StaticContext env) {
        return this;
    }

    /**
     * Iterate over the contents of the collection
     * @param context the dynamic context
     * @return an iterator, whose items will always be nodes (typically but not necessarily document nodes)
     * @throws XPathException
     */

    public SequenceIterator iterate(final XPathContext context) throws XPathException {

        String href;

        if (getNumberOfArguments() == 0) {
            // No arguments supplied: this gets the default collection
            href = null;
        } else {
            href = argument[0].evaluateItem(context).getStringValue();
        }

        CollectionURIResolver resolver = context.getConfiguration().getCollectionURIResolver();
        SequenceIterator iter;
        try {
            iter = resolver.resolve(href, expressionBaseURI, context);
        } catch (XPathException e) {
            e.setLocator(this);
            throw e;
        }

        if (iter == null) {
            return EmptyIterator.getInstance();
        } else {
            ItemMappingFunction imf = new ItemMappingFunction() {
                public Item map(Item item) throws XPathException {
                    if (item instanceof NodeInfo) {
                        return item;
                    } else if (((AtomicValue)item).getPrimitiveValue() instanceof AnyURIValue) {
                        return Document.makeDoc(
                                item.getStringValue(),
                                expressionBaseURI,
                                context,
                                Collection.this);
                    } else {
                        throw new DynamicError("Value returned by CollectionURIResolver must be an anyURI or a NodeInfo");
                    }
                };
            };
            return new ItemMappingIterator(iter, imf);
        }
    }



    // TODO: provide control over error recovery (etc) through options in the catalog file.

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
