package org.orbeon.saxon.om;


/**
 * A SequenceIterator is used to iterate over a sequence. A LookaheadIterator
 * is one that supports a hasNext() method to determine if there are more nodes
 * after the current node.
 */

public interface LookaheadIterator extends SequenceIterator {

    /**
     * Determine whether there are more items to come. Note that this operation
     * is stateless and it is not necessary (or usual) to call it before calling
     * next(). It is used only when there is an explicit need to tell if we
     * are at the last element.
     * <p/>
     * This method must not be called unless the result of getProperties() on the iterator
     * includes the bit setting {@link SequenceIterator#LOOKAHEAD}
     *
     * @return true if there are more items in the sequence
     */

    public boolean hasNext();


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
// The Initial Developer of the Original Code is Michael Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
