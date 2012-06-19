package org.orbeon.saxon.om;

import org.orbeon.saxon.pattern.NodeTest;
import org.orbeon.saxon.value.Value;
import org.orbeon.saxon.trans.XPathException;


/**
 * A SequenceIterator is used to iterate over a sequence. An AxisIterator
 * is a SequenceIterator that throws no exceptions, and that always returns
 * nodes. The nodes should all be in the same document (though there are
 * some cases, such as PrependIterator, where this is the responsibility of the
 * user of the class and is not enforced.)
 */

public interface AxisIterator extends UnfailingIterator {

    /**
     * Move to the next node, without returning it. Returns true if there is
     * a next node, false if the end of the sequence has been reached. After
     * calling this method, the current node may be retrieved using the
     * current() function.
     */

    public boolean moveNext();

    /**
     * Return an iterator over an axis, starting at the current node.
     * @param axis the axis to iterate over, using a constant such as
     * {@link org.orbeon.saxon.om.Axis#CHILD}
     * @param test a predicate to apply to the nodes before returning them.
     * @throws NullPointerException if there is no current node
     */

    public AxisIterator iterateAxis(byte axis, NodeTest test);

    /**
     * Return the atomized value of the current node.
     * @return the atomized value.
     * @throws NullPointerException if there is no current node
     */

    public Value atomize() throws XPathException;

    /**
     * Return the string value of the current node.
     * @return the string value, as an instance of CharSequence.
     * @throws NullPointerException if there is no current node
     */

    public CharSequence getStringValue();


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
