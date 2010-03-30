package org.orbeon.saxon.tree;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.pattern.AnyNodeTest;
import org.orbeon.saxon.pattern.NodeTest;
import org.orbeon.saxon.type.Type;

/**
  * ParentNodeImpl is an implementation of a non-leaf node (specifically, an Element node
  * or a Document node)
  * @author Michael H. Kay
  */


abstract class ParentNodeImpl extends NodeImpl {

    protected Object children = null;     // null for no children
                                          // a NodeInfo for a single child
                                          // a NodeInfo[] for >1 child

    protected int sequence;               // sequence number allocated during original tree creation.
                                          // set to -1 for nodes added subsequently by XQuery update

    /**
     * Get the node sequence number (in document order). Sequence numbers are monotonic but not
     * consecutive. In the current implementation, parent nodes (elements and document nodes) have a zero
     * least-significant word, while namespaces, attributes, text nodes, comments, and PIs have
     * the top word the same as their owner and the bottom half reflecting their relative position.
     * For nodes added by XQUery Update, the sequence number is -1L
     * @return the sequence number if there is one, or -1L otherwise.
    */

    protected final long getSequenceNumber() {
        return (sequence == -1 ? -1L : ((long)sequence)<<32);
    }

    /**
    * Determine if the node has any children.
    */

    public final boolean hasChildNodes() {
        return (children!=null);
    }

    /**
     * Determine how many children the node has
     * @return the number of children of this parent node
     */

    public int getNumberOfChildren() {
        if (children == null) {
            return 0;
        } else if (children instanceof NodeImpl) {
            return 1;
        } else {
            return ((NodeInfo[])children).length;
        }
    }

    /**
     * Get an enumeration of the children of this node
     * @param test A NodeTest to be satisfied by the child nodes, or null
     * if all child node are to be returned
     * @return an iterator over the children of this node
    */

    protected final AxisIterator enumerateChildren(NodeTest test) {
        if (children==null) {
            return EmptyIterator.getInstance();
        } else if (children instanceof NodeImpl) {
            NodeImpl child = (NodeImpl)children;
            if (test == null || test instanceof AnyNodeTest) {
                return SingleNodeIterator.makeIterator(child);
            } else {
                return Navigator.filteredSingleton(child, test);
            }
        } else {
            if (test == null || test instanceof AnyNodeTest) {
                return new NodeArrayIterator((NodeImpl[])children);
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
     * @param n identifies the required child
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
     * Remove a given child
     * @param child the child to be removed
     */

    protected void removeChild(NodeImpl child) {
        if (children == null) {
            return;
        }
        if (children == child) {
            children = null;
            return;
        }
        NodeImpl[] nodes = (NodeImpl[])children;
        for (int i=0; i<nodes.length; i++) {
            if (nodes[i] == child) {
                if (nodes.length == 2) {
                    children = nodes[1-i];
                } else {
                    NodeImpl[] n2 = new NodeImpl[nodes.length - 1];
                    if (i > 0) {
                        System.arraycopy(nodes, 0, n2, 0, i);
                    }
                    if (i < nodes.length - 1) {
                        System.arraycopy(nodes, i+1, n2, i, nodes.length-i-1);
                    }
                    children = cleanUpChildren(n2);
                }
                break;
            }
        }
    }

    /**
     * Tidy up the children of the node. Merge adjacent text nodes; remove zero-length text nodes;
     * reallocate index numbers to each of the children
     * @param children the existing children
     * @return the replacement array of children
     */

    private NodeImpl[] cleanUpChildren(NodeImpl[] children) {
        boolean prevText = false;
        int j = 0;
        NodeImpl[] c2 = new NodeImpl[children.length];
        for (int i=0; i<children.length; i++) {
            NodeImpl node = children[i];
            if (node instanceof TextImpl) {
                if (prevText) {
                    TextImpl prev = ((TextImpl)c2[j-1]);
                    prev.replaceStringValue(prev.getStringValue() + node.getStringValue());
                } else if (node.getStringValue().length() > 0) {
                    prevText = true;
                    node.index = j;
                    c2[j++] = node;
                }
            } else {
                node.index = j;
                c2[j++] = node;
                prevText = false;
            }
        }
        if (j == c2.length) {
            return c2;
        } else {
            NodeImpl[] c3 = new NodeImpl[j];
            System.arraycopy(c2, 0, c3, 0, j);
            return c3;
        }
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
     * Supply an array to be used for the array of children. For system use only.
     * @param array the array to be used
    */

    protected void useChildrenArray(NodeImpl[] array) {
        children = array;
    }

    /**
     * Add a child node to this node. For system use only. Note: normalizing adjacent text nodes
     * is the responsibility of the caller.
     * @param node the node to be added as a child of this node
     * @param index the position where the child is to be added
    */

    public synchronized void addChild(NodeImpl node, int index) {
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
     * Insert copies of a sequence of nodes as children of this node.
     * <p/>
     * <p>This method takes no action unless the target node is a document node or element node. It also
     * takes no action in respect of any supplied nodes that are not elements, text nodes, comments, or
     * processing instructions.</p>
     * <p/>
     * <p>The supplied nodes will be copied to form the new children. Adjacent text nodes will be merged, and
     * zero-length text nodes removed.</p>
     *
     * @param source  the nodes to be inserted
     * @param atStart true if the new nodes are to be inserted before existing children; false if they are
     * @param inherit true if the inserted nodes are to inherit the namespaces that are in-scope for their
     * new parent; false if such namespaces should be undeclared on the children
     */

    public void insertChildren(NodeInfo[] source, boolean atStart, boolean inherit) {
        if (atStart) {
            insertChildrenAt(source, 0, inherit);
        } else {
            insertChildrenAt(source, getNumberOfChildren(), inherit);
        }
    }

    /**
     * Insert children before or after a given existing child
     * @param source the children to be inserted
     * @param index the position before which they are to be inserted: 0 indicates insertion before the
     * first child, 1 insertion before the second child, and so on.
     * @param inherit true if the inserted nodes are to inherit the namespaces that are in-scope for their
     * new parent; false if such namespaces should be undeclared on the children
     */

    protected synchronized void insertChildrenAt(NodeInfo[] source, int index, boolean inherit) {
        if (source.length == 0) {
            return;
        }
        for (int i=0; i<source.length; i++) {
            NodeImpl child = (NodeImpl)source[i];
            child.parent = this;
            if (child instanceof ElementImpl) {
                // If the child has no xmlns="xxx" declaration, then add an xmlns="" to prevent false inheritance
                // from the new parent
                ((ElementImpl)child).fixupInsertedNamespaces(inherit);
            }
        }
        if (children == null) {
            if (source.length == 1) {
                children = source[0];
            } else {
                NodeImpl[] n2 = new NodeImpl[source.length];
                System.arraycopy(source, 0, n2, 0, source.length);
                children = n2;
            }
        } else if (children instanceof NodeImpl) {
            int adjacent = (index==0 ? 0 : source.length - 1);
            if (children instanceof TextImpl && source[adjacent] instanceof TextImpl) {
                if (index == 0) {
                    ((TextImpl)source[adjacent]).replaceStringValue(
                            source[adjacent].getStringValue() + ((TextImpl)children).getStringValue());
                } else {
                    ((TextImpl)source[adjacent]).replaceStringValue(
                            ((TextImpl)children).getStringValue() + source[adjacent].getStringValue());
                }
                NodeImpl[] n2 = new NodeImpl[source.length];
                System.arraycopy(source, 0, n2, 0, source.length);
                children = n2;
            } else {
                NodeImpl[] n2 = new NodeImpl[source.length + 1];
                if (index == 0) {
                    System.arraycopy(source, 0, n2, 0, source.length);
                    n2[source.length] = (NodeImpl)children;
                } else {
                    n2[0] = (NodeImpl)children;
                    System.arraycopy(source, 0, n2, 1, source.length);
                }
                children = cleanUpChildren(n2);
            }
        } else {
            NodeImpl[] n0 = (NodeImpl[])children;
            NodeImpl[] n2 = new NodeImpl[n0.length + source.length];
            System.arraycopy(n0, 0, n2, 0, index);
            System.arraycopy(source, 0, n2, index, source.length);
            System.arraycopy(n0, index, n2, index+source.length, n0.length - index);
            children = cleanUpChildren(n2);
        }
    }

    /**
     * Replace child at a given index by new children
     * @param source the children to be inserted
     * @param index the position at which they are to be inserted: 0 indicates replacement of the
     * first child, replacement of the second child, and so on. The effect is undefined if index
     * is out of range
     * @param inherit set to true if the new child elements are to inherit the in-scope namespaces
     * of their new parent
     * @throws IllegalArgumentException if any of the replacement nodes is not an element, text,
     * comment, or processing instruction node
     */

    protected synchronized void replaceChildrenAt(NodeInfo[] source, int index, boolean inherit) {
        if (children == null) {
            return;
        }
        for (int i=0; i<source.length; i++) {
            NodeImpl child = (NodeImpl)source[i];
            child.parent = this;
            int kind = child.getNodeKind();
            switch (kind) {
                case Type.ELEMENT:
                    // If the child has no xmlns="xxx" declaration, then add an xmlns="" to prevent false inheritance
                    // from the new parent
                    ((ElementImpl)child).fixupInsertedNamespaces(inherit);
                    break;
                case Type.COMMENT:
                case Type.TEXT:
                case Type.PROCESSING_INSTRUCTION:
                    break;
                default:
                    throw new IllegalArgumentException(
                            "Replacement child node is not an element, text, comment, or PI");

            }
        }
        if (children instanceof NodeImpl) {
            if (source.length == 0) {
                children = null;
            } else if (source.length == 1) {
                children = source[0];
            } else {
                NodeImpl[] n2 = new NodeImpl[source.length];
                System.arraycopy(source, 0, n2, 0, source.length);
                children = cleanUpChildren(n2);
            }
        } else {
            NodeImpl[] n0 = (NodeImpl[])children;
            NodeImpl[] n2 = new NodeImpl[n0.length + source.length - 1];
            System.arraycopy(n0, 0, n2, 0, index);
            System.arraycopy(source, 0, n2, index, source.length);
            System.arraycopy(n0, index + 1, n2, index+source.length, n0.length - index - 1);
            children = cleanUpChildren(n2);
        }
    }


    /**
     * Compact the space used by this node
     * @param size the number of actual children
     */

    public synchronized void compact(int size) {
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
