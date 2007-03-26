package org.orbeon.saxon.om;

import org.orbeon.saxon.trans.XPathException;

/**
 * A ClosingAction is an action that can be performed by a {@link ClosingIterator} when the end of a
 * sequence is reached
 */
public interface ClosingAction {

    /**
     * Notify the end of the sequence reached by the base iterator
     * @param base the iteration that has come to the end of its natural life
     * @param count the number of items that have been read
     */

    public void close(SequenceIterator base, int count) throws XPathException;
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

