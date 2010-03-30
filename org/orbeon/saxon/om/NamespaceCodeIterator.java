package org.orbeon.saxon.om;

import org.orbeon.saxon.event.Receiver;
import org.orbeon.saxon.sort.EmptyIntIterator;
import org.orbeon.saxon.sort.IntHashSet;
import org.orbeon.saxon.sort.IntIterator;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.Type;

/**
 * This class provides an iterator over the namespace codes representing the in-scope namespaces
 * of any node. It relies on nodes to implement the method
 * {@link org.orbeon.saxon.om.NodeInfo#getDeclaredNamespaces(int[])}.
 *
 * <p>The result does not include the XML namespace.</p>
 */
public class NamespaceCodeIterator implements IntIterator {

    private NodeInfo element;
    private int index;
    private int next;
    private int[] localDeclarations;
    //IntHashSet declared;
    IntHashSet undeclared;

    /**
     * Factory method: create an iterator over the in-scope namespace codes for an element
     * @param element the element (or other node) whose in-scope namespaces are required. If this
     * is not an element, the result will be an empty iterator
     * @return an iterator over the namespace codes. A namespace code is an integer that represents
     * a prefix-uri binding; the prefix and URI can be obtained by reference to the name pool. This
     * iterator will represent all the in-scope namespaces, without duplicates, and respecting namespace
     * undeclarations. It does not include the XML namespace.
     */

    public static IntIterator iterateNamespaces(NodeInfo element) {
        if (element.getNodeKind() == Type.ELEMENT) {
            return new NamespaceCodeIterator(element);
        } else {
            return EmptyIntIterator.getInstance();
        }
    }

    /**
     * Send all the in-scope namespaces for a node to a specified receiver
     * @param element the element in question (the method does nothing if this is not an element)
     * @param receiver the receiver to which the namespaces are notified
     */

    public static void sendNamespaces(NodeInfo element, Receiver receiver) throws XPathException {
        if (element.getNodeKind() == Type.ELEMENT) {
            int[] undeclared = new int[8];
            int undeclaredSize = 0;
            int[] localDeclarations = element.getDeclaredNamespaces(null);
            while (true) {
                for (int i=0; i<localDeclarations.length;) {
                    int nsCode = localDeclarations[i++];
                    if (nsCode == -1) {
                        break;
                    }
                    short uriCode = (short)(nsCode & 0xffff);
                    short prefixCode = (short)(nsCode >> 16);
                    boolean isnew = true;
                    for (int j=0; j<undeclaredSize; j++) {
                        if (undeclared[j] == prefixCode) {
                            isnew = false;
                            break;
                        }
                    }
                    if (isnew) {
                        if (undeclared.length == undeclaredSize) {
                            int[] u2 = new int[undeclaredSize*2];
                            System.arraycopy(undeclared, 0, u2, 0, undeclaredSize);
                            undeclared = u2;
                        }
                        undeclared[undeclaredSize++] = prefixCode;
                        if (uriCode != 0) {
                            // it's new, and it's not an undeclaration, so send it
                            receiver.namespace(nsCode, 0);
                        }
                    }

                }
                element = element.getParent();
                if (element == null || element.getNodeKind() != Type.ELEMENT) {
                    return;
                }
                localDeclarations = element.getDeclaredNamespaces(localDeclarations);
            }
        }
    }

    private NamespaceCodeIterator(NodeInfo element) {
        this.element = element;
        undeclared = new IntHashSet(8);
        index = 0;
        localDeclarations = element.getDeclaredNamespaces(null);
    }

    public boolean hasNext() {
        if (next == -1) {
            return false;
        }
        advance();
        return next != -1;
    }

    public int next() {
        return next;
    }

    private void advance() {
        while (true) {
            boolean ascend = index >= localDeclarations.length;
            int nsCode = 0;
            if (!ascend) {
                nsCode = localDeclarations[index++];
                ascend = nsCode == -1;
            }
            if (ascend) {
                element = element.getParent();
                if (element != null && element.getNodeKind() == Type.ELEMENT) {
                    localDeclarations = element.getDeclaredNamespaces(localDeclarations);
                    index = 0;
                    continue;
                } else {
                    next = -1;
                    return;
                }
            }
            short uriCode = (short)(nsCode & 0xffff);
            short prefixCode = (short)(nsCode >> 16);
            if (uriCode == 0) {
                // this is an undeclaration
                undeclared.add(prefixCode);
            } else {
                if (undeclared.add(prefixCode)) {
                    // it was added, so it's new, so return it
                    next = nsCode;
                    return;
                }
                // else it wasn't added, so we've already seen it
            }
        }
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

