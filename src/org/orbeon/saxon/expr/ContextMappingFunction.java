package org.orbeon.saxon.expr;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.trans.XPathException;

/**
 * ContextMappingFunction is an interface that must be satisfied by an object passed to a
 * ContextMappingIterator. It represents an object which, given an Item, can return a
 * SequenceIterator that delivers a sequence of zero or more Items.
 * <p>
 * This is a specialization of the more general MappingFunction class: it differs in that
 * each item being processed becomes the context item while it is being processed.
*/

public interface ContextMappingFunction {

    /**
    * Map one item to a sequence.
    * @param context The processing context. The item to be mapped is the context item identified
    * from this context: the values of position() and last() also relate to the set of items being mapped
    * @return a SequenceIterator over the sequence of items that the supplied input
    * item maps to
    */

    public SequenceIterator map(XPathContext context) throws XPathException;

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
