package net.sf.saxon.expr;
import net.sf.saxon.om.SequenceIterator;


/**
* A ReversibleIterator is an interface implemented by any SequenceIterator that is
 * able to deliver items in reverse order (or to supply another iterator that can
 * do so).
*/

public interface ReversibleIterator extends SequenceIterator {

    /**
    * Get a new SequenceIterator that returns the same items in reverse order.
     * If this SequenceIterator is an AxisIterator, then the returned SequenceIterator
     * must also be an AxisIterator.
    */

    public SequenceIterator getReverseIterator();

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
