package org.orbeon.saxon.expr;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.xpath.XPathException;

/**
* MappingFunction is an interface that must be satisfied by an object passed to a
* MappingIterator. It represents an object which, given an Item, can return a
* SequenceIterator that delivers a sequence of zero or more Items.
*/

public interface MappingFunction {

    /**
    * Map one item to a sequence.
    * @param item The item to be mapped.
    * If context is supplied, this must be the same as context.currentItem().
    * @param context The processing context. This is supplied only for mapping constructs that
    * set the context node, position, and size. Otherwise it is null.
    * @param info Arbitrary information supplied by the creator of the MappingIterator. It must be
    * read-only and immutable for the duration of the iteration.
    * @return either (a) a SequenceIterator over the sequence of items that the supplied input
    * item maps to, or (b) an Item if it maps to a single item, or (c) null if it maps to an empty
    * sequence.
    */

    public Object map(Item item, XPathContext context, Object info) throws XPathException;

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
