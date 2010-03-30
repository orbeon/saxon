package org.orbeon.saxon.evpull;

import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.trans.XPathException;

/**
* EventMappingFunction is an interface that must be satisfied by an object passed to an
* EventMappingIterator. It represents an object which, given an Item, can return an
* EventIterator that delivers a sequence of zero or more PullEvents.
*/

public interface EventMappingFunction {

    /**
    * Map one item to a sequence of pull events.
    * @param item The item to be mapped.
    * @return one of the following: (a) an EventIterator over the sequence of items that the supplied input
    * item maps to, or (b) null if it maps to an empty sequence.
    */

    public EventIterator map(Item item) throws XPathException;

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
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//

