package org.orbeon.saxon.expr;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.xpath.XPathException;


/**
* A LastPositionFinder is an interface implemented by any SequenceIterator that is
 * able to return the position of the last item in the sequence.
*/

public interface LastPositionFinder extends SequenceIterator {

    /**
    * Get the last position (that is, the number of items in the sequence)
    */

    public int getLastPosition() throws XPathException;

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
