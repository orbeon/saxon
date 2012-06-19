package org.orbeon.saxon.om;

import org.orbeon.saxon.pattern.NodeTest;
import org.orbeon.saxon.value.Value;
import org.orbeon.saxon.trans.XPathException;

/**
 * An iterator over an array of nodes. This is the same as
 * {@link ArrayIterator}, except that the iterator is an {@link AxisIterator}
 */
public class NodeArrayIterator extends ArrayIterator implements AxisIterator {

    public NodeArrayIterator(NodeInfo[] nodes) {
        super(nodes);
    }

    public NodeArrayIterator(NodeInfo[] nodes, int start, int end) {
        super(nodes, start, end);
    }

    /**
     * Move to the next node, without returning it. Returns true if there is
     * a next node, false if the end of the sequence has been reached. After
     * calling this method, the current node may be retrieved using the
     * current() function.
     */

    public boolean moveNext() {
        return (next() != null);
    }

    /**
     * Return an iterator over an axis, starting at the current node.
     *
     * @param axis the axis to iterate over, using a constant such as
     *             {@link Axis#CHILD}
     * @param test a predicate to apply to the nodes before returning them.
     * @throws NullPointerException if there is no current node
     */

    public AxisIterator iterateAxis(byte axis, NodeTest test) {
        return ((NodeInfo)current()).iterateAxis(axis, test);
    }

    /**
     * Return the atomized value of the current node.
     *
     * @return the atomized value.
     * @throws NullPointerException if there is no current node
     */

    public Value atomize() throws XPathException {
        return ((NodeInfo)current()).atomize();
    }

    /**
     * Return the string value of the current node.
     *
     * @return the string value, as an instance of CharSequence.
     * @throws NullPointerException if there is no current node
     */

    public CharSequence getStringValue() {
        return ((NodeInfo)current()).getStringValueCS();
    }

    /**
     * Get another iterator over the same items
     *
     * @return a new ArrayIterator
     */
    public SequenceIterator getAnother() {
        return new NodeArrayIterator((NodeInfo[])items, start, end);
    }

    /**
     * Get an iterator that processes the same items in reverse order
     *
     * @return a new ArrayIterator
     */
    public SequenceIterator getReverseIterator() {
        return new ReverseNodeArrayIterator((NodeInfo[])items, start, end);
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
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//

