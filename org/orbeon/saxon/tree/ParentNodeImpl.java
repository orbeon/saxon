package org.orbeon.saxon.tree;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.pattern.AnyNodeTest;
import org.orbeon.saxon.pattern.NodeTest;

/**
  * ParentNodeImpl is an implementation of a non-leaf node (specifically, an Element node
  * or a Document node)
  * @author Michael H. Kay
  */


abstract class ParentNodeImpl extends NodeImpl {

    private Object children = null;     // null for no children
                                        // a NodeInfo for a single child
                                        // a NodeInfo[] for >1 child

    protected int sequence;

    /**
    * Get the node sequence number (in document order). Sequence numbers are monotonic but not
    * consecutive. In the current implementation, parent nodes (elements and roots) have a zero
    * least-significant word, while namespaces, attributes, text nodes, comments, and PIs have
    * the top word the same as their owner and the bottom half reflecting their relative position.
    */

    protected final long getSequenceNumber() {
        return ((long)sequence)<<32;
    }

    /**
    * Determine if the node has any children.
    */

    public final boolean hasChildNodes() {
        return (children!=null);
    }

    /**
    * Get an enumeration of the children of this node
     * @param test A NodeTest to be satisfied by the child nodes, or null
     * if all child node are to be returned
    */

    public final AxisIterator enumerateChildren(NodeTest test) {
        if (children==null) {
            return EmptyIterator.getInstance();
        } else if (children instanceof NodeImpl) {
            NodeImpl child = (NodeImpl)children;
            if (test==null || test.matches(child)) {
                return SingletonIterator.makeIterator(child);
            } else {
                return EmptyIterator.getInstance();
            }
        } else {
            if (test==null || test instanceof AnyNodeTest) {
                return new ArrayIterator((NodeImpl[])children);
            } else {
                return new ChildEnumeration(this, test);
            }
        }
    }


    /**
    * Get the first child node of the element
    * @return the first child node of the required type, or null if there are no children
    */

    public final NodeInfo getFirstChild() {
        if (children==null) return null;
        if (children instanceof NodeImpl) return (NodeImpl)children;
        return ((NodeImpl[])children)[0];
    }

    /**
    * Get the last child node of the element
    * @return the last child of the element, or null if there are no children
    */

    public final NodeInfo getLastChild() {
        if (children==null) return null;
        if (children instanceof NodeImpl) return (NodeImpl)children;
        NodeImpl[] n = (NodeImpl[])children;
        return n[n.length-1];
    }

    /**
    * Get the nth child node of the element (numbering from 0)
    * @return the last child of the element, or null if there is no n'th child
    */

    protected final NodeImpl getNthChild(int n) {
        if (children==null) return null;
        if (children instanceof NodeImpl) {
            return (n==0 ? (NodeImpl)children : null);
        }
        NodeImpl[] nodes = (NodeImpl[])children;
        if (n<0 || n>=nodes.length) return null;
        return nodes[n];
    }


    /**
    * Return the string-value of the node, that is, the concatenation
    * of the character content of all descendent elements and text nodes.
    * @return the accumulated character content of the element, including descendant elements.
    */

    public String getStringValue() {
        return getStringValueCS().toString();
    }


    public CharSequence getStringValueCS() {
        FastStringBuffer sb = null;

        NodeImpl next = (NodeImpl)getFirstChild();
        while (next!=null) {
            if (next instanceof TextImpl) {
                if (sb==null) {
                    sb = new FastStringBuffer(1024);
                }
                sb.append(next.getStringValueCS());
            }
            next = next.getNextInDocument(this);
        }
        if (sb==null) return "";
        return sb.condense();
    }

    /**
    * Copy the string-value of this node to a given outputter
    */
/*
    public void copyStringValue(Receiver out) throws XPathException {
        NodeImpl next = (NodeImpl)getFirstChild();
        while (next!=null) {
            if (next.getItemType()==Type.TEXT) {
                next.copyStringValue(out);
            }
            next = next.getNextInDocument(this);
        }
    }
*/
    /**
    * Supply an array to be used for the array of children. For system use only.
    */

    public void useChildrenArray(NodeImpl[] array) {
        children = array;
    }

    /**
    * Add a child node to this node. For system use only. Note: normalizing adjacent text nodes
    * is the responsibility of the caller.
    */

    public void addChild(NodeImpl node, int index) {
        NodeImpl[] c;
        if (children == null) {
            c = new NodeImpl[10];
        } else if (children instanceof NodeImpl) {
            c = new NodeImpl[10];
            c[0] = (NodeImpl)children;
        } else {
            c = (NodeImpl[])children;
        }
        if (index >= c.length) {
            NodeImpl[] kids = new NodeImpl[c.length * 2];
            System.arraycopy(c, 0, kids, 0, c.length);
            c = kids;
        }
        c[index] = node;
        node.parent = this;
        node.index = index;
        children = c;
    }


    /**
    * Compact the space used by this node
    */

    public void compact(int size) {
        if (size==0) {
            children = null;
        } else if (size==1) {
            if (children instanceof NodeImpl[]) {
                children = ((NodeImpl[])children)[0];
            }
        } else {
            NodeImpl[] kids = new NodeImpl[size];
            System.arraycopy(children, 0, kids, 0, size);
            children = kids;
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
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
