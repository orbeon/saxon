package org.orbeon.saxon.value;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.event.Receiver;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.pattern.NodeTest;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.Type;

import javax.xml.transform.SourceLocator;
import java.util.ArrayList;
import java.util.List;

/**
* This class represents a temporary tree whose root document node owns a single text node. <BR>
*/

public final class TextFragmentValue implements DocumentInfo, FingerprintedNode, SourceLocator {

    private CharSequence text;
    private String baseURI;
    private TextFragmentTextNode textNode = null;   // created on demand
    private Configuration config;
    private int documentNumber;

    /**
    * Constructor: create a result tree fragment containing a single text node
    * @param value a String containing the value
    * @param baseURI the base URI of the document node
    */

    public TextFragmentValue(CharSequence value, String baseURI) {
        this.text = value;
        this.baseURI = baseURI;
    }

	/**
	* Set the configuration (containing the name pool used for all names in this document)
	*/

	public void setConfiguration(Configuration config) {
        this.config = config;
		documentNumber = -1;    // the document number is allocated lazily because it can cause
                                // contention on the NamePool and is often not needed.
	}

    /**
     * Get the configuration previously set using setConfiguration
     * (or the default configuraton allocated automatically)
     */

    public Configuration getConfiguration() {
        return config;
    }

	/**
	* Get the name pool used for the names in this document
	*/

	public NamePool getNamePool() {
		return config.getNamePool();
	}

	/**
	* Get the unique document number
	*/

	public int getDocumentNumber() {
        if (documentNumber == -1) {
            documentNumber = config.getDocumentNumberAllocator().allocateDocumentNumber();
            // technically this isn't thread-safe; however, TextFragmentValues are invariably used within
            // a single thread
        }
	    return documentNumber;
	}

    /**
    * Return the type of node.
    * @return Type.DOCUMENT (always)
    */

    public final int getNodeKind() {
        return Type.DOCUMENT;
    }

    /**
    * Get the String Value
    */

    public String getStringValue() {
        return text.toString();
    }

    /**
     * Get the value of the item as a CharSequence. This is in some cases more efficient than
     * the version of the method that returns a String.
     */

    public CharSequence getStringValueCS() {
        return text;
    }

    /**
    * Determine whether this is the same node as another node
    * @return true if this Node object and the supplied Node object represent the
    * same node in the tree.
    */

    public boolean isSameNodeInfo(NodeInfo other) {
        return this==other;
    }

    /**
    * Get a character string that uniquely identifies this node
     * @param buffer the buffer to contain the generated ID
     */

    public void generateId(FastStringBuffer buffer) {
        buffer.append("tt");
        buffer.append(Integer.toString(getDocumentNumber()));
    }

    /**
    * Set the system ID (that is, the document URI property) for the document node.
     * @throws UnsupportedOperationException (always). This kind of tree does not have a document URI.
    */

    public void setSystemId(String systemId) {
        throw new UnsupportedOperationException("A temporary tree does not have a document URI");
    }

    /**
    * Get the system ID (the document URI) of the document node.
    */

    public String getSystemId() {
        return null;
    }

    /**
    * Get the base URI for the document node.
    */

    public String getBaseURI() {
        return baseURI;
    }

    /**
    * Determine the relative position of this node and another node, in document order.
    * The other node will always be in the same document.
    * @param other The other node, whose position is to be compared with this node
    * @return -1 if this node precedes the other node, +1 if it follows the other
    * node, or 0 if they are the same node. (In this case, isSameNode() will always
    * return true, and the two nodes will produce the same result for generateId())
    */

    public int compareOrder(NodeInfo other) {
        if (this==other) return 0;
        return -1;
    }

	/**
	* Get the name code of the node, used for displaying names
	*/

	public int getNameCode() {
	    return -1;
	}

	/**
	* Get the fingerprint of the node, used for matching names
	*/

	public int getFingerprint() {
	    return -1;
	}

    /**
    * Get the prefix part of the name of this node. This is the name before the ":" if any.
    * @return the prefix part of the name. For an unnamed node, return "".
    */

    public String getPrefix() {
        return "";
    }

    /**
    * Get the URI part of the name of this node. This is the URI corresponding to the
    * prefix, or the URI of the default namespace if appropriate.
    * @return The URI of the namespace of this node. For an unnamed node, or for
    * an element or attribute in the default namespace, return an empty string.
    */

    public String getURI() {
        return "";
    }

    /**
    * Get the display name of this node. For elements and attributes this is [prefix:]localname.
    * For unnamed nodes, it is an empty string.
    * @return The display name of this node.
    * For a node with no name, return an empty string.
    */

    public String getDisplayName() {
        return "";
    }

    /**
    * Get the local name of this node.
    * @return The local name of this node.
    * For a node with no name, return "".
    */

    public String getLocalPart() {
        return "";
    }

    /**
    * Determine whether the node has any children.
    * @return <code>true</code> if this node has any attributes,
    *   <code>false</code> otherwise.
    */

    public boolean hasChildNodes() {
        return !("".equals(text));
    }

    /**
     * Get line number
     *
     * @return the line number of the node in its original source document; or
     *         -1 if not available
     */

    public int getLineNumber() {
        return -1;
    }

    /**
     * Get the type annotation of this node, if any.
     * Returns -1 for kinds of nodes that have no annotation, and for elements annotated as
     * untyped, and attributes annotated as untypedAtomic.
     *
     * @return the type annotation of the node.
     * @see org.orbeon.saxon.type.Type
     */

    public int getTypeAnnotation() {
        return -1;
    }

    /**
     * Output all namespace nodes associated with this element. Does nothing if
     * the node is not an element.
     *
     * @param out              The relevant outputter
     * @param includeAncestors True if namespaces declared on ancestor
     */

    public void sendNamespaceDeclarations(Receiver out, boolean includeAncestors) {
    }

    /**
     * Get all namespace undeclarations and undeclarations defined on this element.
     *
     * @param buffer If this is non-null, and the result array fits in this buffer, then the result
     *               may overwrite the contents of this array, to avoid the cost of allocating a new array on the heap.
     * @return An array of integers representing the namespace declarations and undeclarations present on
     *         this element. For a node other than an element, return null. Otherwise, the returned array is a
     *         sequence of namespace codes, whose meaning may be interpreted by reference to the name pool. The
     *         top half word of each namespace code represents the prefix, the bottom half represents the URI.
     *         If the bottom half is zero, then this is a namespace undeclaration rather than a declaration.
     *         The XML namespace is never included in the list. If the supplied array is larger than required,
     *         then the first unused entry will be set to -1.
     *         <p/>
     *         <p>For a node other than an element, the method returns null.</p>
     */

    public int[] getDeclaredNamespaces(int[] buffer) {
        return null;
    }

    /**
     * Get the typed value of the item
     *
     * @return the typed value of the item. In general this will be a sequence
     */

    public SequenceIterator getTypedValue() {
        return SingletonIterator.makeIterator(new UntypedAtomicValue(text));
    }

    /**
     * Get the typed value. The result of this method will always be consistent with the method
     * {@link org.orbeon.saxon.om.Item#getTypedValue()}. However, this method is often more convenient and may be
     * more efficient, especially in the common case where the value is expected to be a singleton.
     *
     * @return the typed value. If requireSingleton is set to true, the result will always be an
     *         AtomicValue. In other cases it may be a Value representing a sequence whose items are atomic
     *         values.
     * @since 8.5
     */

    public Value atomize() {
        return new UntypedAtomicValue(text);
    }

    /**
     * Return the public identifier for the current document event.
     * <p/>
     * <p>The return value is the public identifier of the document
     * entity or of the external parsed entity in which the markup that
     * triggered the event appears.</p>
     *
     * @return A string containing the public identifier, or
     *         null if none is available.
     * @see #getSystemId
     */
    public String getPublicId() {
        return null;
    }

    /**
     * Return the character position where the current document event ends.
     * <p/>
     * <p><strong>Warning:</strong> The return value from the method
     * is intended only as an approximation for the sake of error
     * reporting; it is not intended to provide sufficient information
     * to edit the character content of the original XML document.</p>
     * <p/>
     * <p>The return value is an approximation of the column number
     * in the document entity or external parsed entity where the
     * markup that triggered the event appears.</p>
     *
     * @return The column number, or -1 if none is available.
     * @see #getLineNumber
     */
    public int getColumnNumber() {
        return -1;
    }

   /**
    * Get the value of a given attribute of this node
    * @param fingerprint The fingerprint of the attribute name
    * @return the attribute value if it exists or null if not
    */

    public String getAttributeValue(int fingerprint) {
        return null;
    }

    /**
    * Return an iteration over the nodes reached by the given axis from this node
    * @param axisNumber The axis to be iterated over
    * @return a AxisIterator that scans the nodes reached by the axis in turn.
    * @see org.orbeon.saxon.om.Axis
    */

    public AxisIterator iterateAxis(byte axisNumber) {
        switch (axisNumber) {
            case Axis.ANCESTOR:
            case Axis.ATTRIBUTE:
            case Axis.FOLLOWING:
            case Axis.FOLLOWING_SIBLING:
            case Axis.NAMESPACE:
            case Axis.PARENT:
            case Axis.PRECEDING:
            case Axis.PRECEDING_SIBLING:
            case Axis.PRECEDING_OR_ANCESTOR:
                return EmptyIterator.getInstance();

            case Axis.SELF:
            case Axis.ANCESTOR_OR_SELF:
                return SingletonIterator.makeIterator(this);

            case Axis.CHILD:
            case Axis.DESCENDANT:
                return SingletonIterator.makeIterator(getTextNode());

            case Axis.DESCENDANT_OR_SELF:
                Item[] nodes = {this, getTextNode()};
                return new ArrayIterator(nodes);

            default:
                 throw new IllegalArgumentException("Unknown axis number " + axisNumber);
        }
    }

    /**
    * Return an enumeration over the nodes reached by the given axis from this node
    * @param axisNumber The axis to be iterated over
    * @param nodeTest A pattern to be matched by the returned nodes
    * @return a AxisIterator that scans the nodes reached by the axis in turn.
    * @see org.orbeon.saxon.om.Axis
    */

    public AxisIterator iterateAxis(byte axisNumber, NodeTest nodeTest) {
        switch (axisNumber) {
            case Axis.ANCESTOR:
            case Axis.ATTRIBUTE:
            case Axis.FOLLOWING:
            case Axis.FOLLOWING_SIBLING:
            case Axis.NAMESPACE:
            case Axis.PARENT:
            case Axis.PRECEDING:
            case Axis.PRECEDING_SIBLING:
            case Axis.PRECEDING_OR_ANCESTOR:
                return EmptyIterator.getInstance();

            case Axis.SELF:
            case Axis.ANCESTOR_OR_SELF:
                return Navigator.filteredSingleton(this, nodeTest);

            case Axis.CHILD:
            case Axis.DESCENDANT:
                return Navigator.filteredSingleton(getTextNode(), nodeTest);

            case Axis.DESCENDANT_OR_SELF:
                List result = new ArrayList(2);
                if (nodeTest.matches(this)) {
                    result.add(this);
                }
                NodeInfo textNode2 = getTextNode();
                if (nodeTest.matches(textNode2)) {
                    result.add(textNode2);
                }
                return new ListIterator(result);

            default:
                 throw new IllegalArgumentException("Unknown axis number " + axisNumber);
        }
    }

    /**
     * Find the parent node of this node.
     * @return The Node object describing the containing element or root node.
     */

    public NodeInfo getParent() {
        return null;
    }

    /**
    * Get the root node
    * @return the NodeInfo representing the root of this tree
    */

    public NodeInfo getRoot() {
        return this;
    }

    /**
    * Get the root (document) node
    * @return the DocumentInfo representing the containing document
    */

    public DocumentInfo getDocumentRoot() {
        return this;
    }

    /**
    * Copy the result tree fragment value to a given Outputter
    */

    public void copy(Receiver out, int whichNamespaces, boolean copyAnnotations, int locationId)
    throws XPathException {
        out.characters(text, 0, 0);
    }

    /**
    * Get the element with a given ID.
    * @param id The unique ID of the required element
    * @return null (this kind of tree contains no elements)
    */

    public NodeInfo selectID(String id) {
        return null;
    }

    /**
    * Get the unparsed entity with a given name
    * @param name the name of the entity
    * @return the URI and public ID of the entity if there is one, or null if not
    */

    public String[] getUnparsedEntity(String name) {
        return null;
    }


    /**
    * Make an instance of the text node
    */

    private TextFragmentTextNode getTextNode() {
        if (textNode==null) {
            textNode = new TextFragmentTextNode();
        }
        return textNode;
    }

    /**
    * Inner class representing the text node; this is created on demand
    */

    private class TextFragmentTextNode implements NodeInfo, FingerprintedNode, SourceLocator {

        /**
        * Set the system ID for the entity containing the node.
        */

        public void setSystemId(String systemId) {}

        /**
         * Get the configuration
         */

        public Configuration getConfiguration() {
            return config;
        }

        /**
         * Get the name pool for this node
         * @return the NamePool
         */

        public NamePool getNamePool() {
            return config.getNamePool();
        }

        /**
        * Return the type of node.
        * @return Type.TEXT (always)
        */

        public final int getNodeKind() {
            return Type.TEXT;
        }

        /**
        * Get the String Value
        */

        public String getStringValue() {
            return text.toString();
        }

        /**
         * Get the value of the item as a CharSequence. This is in some cases more efficient than
         * the version of the method that returns a String.
         */

        public CharSequence getStringValueCS() {
            return text;
        }

        /**
        * Determine whether this is the same node as another node
        * @return true if this Node object and the supplied Node object represent the
        * same node in the tree.
        */

        public boolean isSameNodeInfo(NodeInfo other) {
            return this==other;
        }

        /**
        * Get a character string that uniquely identifies this node
         */

        public void generateId(FastStringBuffer buffer) {
            buffer.append("tt");
            buffer.append(Integer.toString(getDocumentNumber()));
            buffer.append("t1");
        }

        /**
        * Get the system ID for the entity containing the node.
        */

        public String getSystemId() {
            return null;
        }

        /**
        * Get the base URI for the node. Default implementation for child nodes gets
        * the base URI of the parent node.
        */

        public String getBaseURI() {
            return baseURI;
        }

        /**
        * Determine the relative position of this node and another node, in document order.
        * The other node will always be in the same document.
        * @param other The other node, whose position is to be compared with this node
        * @return -1 if this node precedes the other node, +1 if it follows the other
        * node, or 0 if they are the same node. (In this case, isSameNode() will always
        * return true, and the two nodes will produce the same result for generateId())
        */

        public int compareOrder(NodeInfo other) {
            if (this==other) return 0;
            return +1;
        }

    	/**
    	* Get the name code of the node, used for displaying names
    	*/

    	public int getNameCode() {
    	    return -1;
    	}

    	/**
    	* Get the fingerprint of the node, used for matching names
    	*/

    	public int getFingerprint() {
    	    return -1;
    	}


        /**
        * Get the prefix part of the name of this node. This is the name before the ":" if any.
        * @return the prefix part of the name. For an unnamed node, return "".
        */

        public String getPrefix() {
            return "";
        }

        /**
        * Get the URI part of the name of this node. This is the URI corresponding to the
        * prefix, or the URI of the default namespace if appropriate.
        * @return The URI of the namespace of this node. For an unnamed node, or for
        * an element or attribute in the default namespace, return an empty string.
        */

        public String getURI() {
            return "";
        }

        /**
        * Get the display name of this node. For elements and attributes this is [prefix:]localname.
        * For unnamed nodes, it is an empty string.
        * @return The display name of this node.
        * For a node with no name, return an empty string.
        */

        public String getDisplayName() {
            return "";
        }

        /**
        * Get the local name of this node.
        * @return The local name of this node.
        * For a node with no name, return "".
        */

        public String getLocalPart() {
            return "";
        }

        /**
        * Determine whether the node has any children.
        * @return <code>true</code> if this node has any attributes,
        *   <code>false</code> otherwise.
        */

        public boolean hasChildNodes() {
            return false;
        }

        /**
        * Get the value of a given attribute of this node
        * @param fingerprint The fingerprint of the attribute name
        * @return the attribute value if it exists or null if not
        */

        public String getAttributeValue(int fingerprint) {
            return null;
        }

        /**
         * Get line number
         *
         * @return the line number of the node in its original source document; or
         *         -1 if not available
         */

        public int getLineNumber() {
            return -1;
        }

        /**
         * Get the type annotation of this node, if any.
         * <p>The result is undefined for nodes other than elements and attributes.</p>
         *
         * @return the type annotation of the node.
         * @see org.orbeon.saxon.type.Type
         */

        public int getTypeAnnotation() {
            return -1;
        }

        /**
         * Get the document number of the document containing this node. For a free-standing
         * orphan node, just return the hashcode.
         */

        public int getDocumentNumber() {
            return getDocumentRoot().getDocumentNumber();
        }

        /**
         * Output all namespace nodes associated with this element. Does nothing if
         * the node is not an element.
         *
         * @param out              The relevant outputter
         * @param includeAncestors True if namespaces declared on ancestor
         */

        public void sendNamespaceDeclarations(Receiver out, boolean includeAncestors) {
        }

        /**
         * Get all namespace undeclarations and undeclarations defined on this element.
         *
         * @param buffer If this is non-null, and the result array fits in this buffer, then the result
         *               may overwrite the contents of this array, to avoid the cost of allocating a new array on the heap.
         * @return An array of integers representing the namespace declarations and undeclarations present on
         *         this element. For a node other than an element, return null. Otherwise, the returned array is a
         *         sequence of namespace codes, whose meaning may be interpreted by reference to the name pool. The
         *         top half word of each namespace code represents the prefix, the bottom half represents the URI.
         *         If the bottom half is zero, then this is a namespace undeclaration rather than a declaration.
         *         The XML namespace is never included in the list. If the supplied array is larger than required,
         *         then the first unused entry will be set to -1.
         *         <p/>
         *         <p>For a node other than an element, the method returns null.</p>
         */

        public int[] getDeclaredNamespaces(int[] buffer) {
            return null;
        }

        /**
         * Get the typed value of the item
         *
         * @return the typed value of the item. In general this will be a sequence
         * @throws org.orbeon.saxon.trans.XPathException
         *          where no typed value is available, e.g. for
         *          an element with complex content
         */

        public SequenceIterator getTypedValue() throws XPathException {
            return SingletonIterator.makeIterator(new UntypedAtomicValue(text));
        }

        /**
         * Get the typed value. The result of this method will always be consistent with the method
         * {@link org.orbeon.saxon.om.Item#getTypedValue()}. However, this method is often more convenient and may be
         * more efficient, especially in the common case where the value is expected to be a singleton.
         *
         * @return the typed value. If requireSingleton is set to true, the result will always be an
         *         AtomicValue. In other cases it may be a Value representing a sequence whose items are atomic
         *         values.
         * @since 8.5
         */

        public Value atomize() throws XPathException {
            return new UntypedAtomicValue(text);
        }

        /**
         * Return the public identifier for the current document event.
         * <p/>
         * <p>The return value is the public identifier of the document
         * entity or of the external parsed entity in which the markup that
         * triggered the event appears.</p>
         *
         * @return A string containing the public identifier, or
         *         null if none is available.
         * @see #getSystemId
         */
        public String getPublicId() {
            return null;
        }

        /**
         * Return the character position where the current document event ends.
         * <p/>
         * <p><strong>Warning:</strong> The return value from the method
         * is intended only as an approximation for the sake of error
         * reporting; it is not intended to provide sufficient information
         * to edit the character content of the original XML document.</p>
         * <p/>
         * <p>The return value is an approximation of the column number
         * in the document entity or external parsed entity where the
         * markup that triggered the event appears.</p>
         *
         * @return The column number, or -1 if none is available.
         * @see #getLineNumber
         */
        public int getColumnNumber() {
            return -1;
        }

        /**
         * Return an enumeration over the nodes reached by the given axis from this node
         * @param axisNumber the axis to be iterated over
         * @return a AxisIterator that scans the nodes reached by the axis in turn.
         */

         public AxisIterator iterateAxis(byte axisNumber) {
             switch (axisNumber) {
                 case Axis.ANCESTOR:
                 case Axis.PARENT:
                 case Axis.PRECEDING_OR_ANCESTOR:
                     return SingletonIterator.makeIterator(TextFragmentValue.this);

                 case Axis.ANCESTOR_OR_SELF:
                     Item[] nodes = {this, TextFragmentValue.this};
                     return new ArrayIterator(nodes);

                 case Axis.ATTRIBUTE:
                 case Axis.CHILD:
                 case Axis.DESCENDANT:
                 case Axis.FOLLOWING:
                 case Axis.FOLLOWING_SIBLING:
                 case Axis.NAMESPACE:
                 case Axis.PRECEDING:
                 case Axis.PRECEDING_SIBLING:
                     return EmptyIterator.getInstance();

                 case Axis.SELF:
                 case Axis.DESCENDANT_OR_SELF:
                     return SingletonIterator.makeIterator(this);

                 default:
                      throw new IllegalArgumentException("Unknown axis number " + axisNumber);
             }
         }


        /**
        * Return an enumeration over the nodes reached by the given axis from this node
        * @param axisNumber the axis to be iterated over
        * @param nodeTest A pattern to be matched by the returned nodes
        * @return a AxisIterator that scans the nodes reached by the axis in turn.
        */

        public AxisIterator iterateAxis( byte axisNumber, NodeTest nodeTest) {
            switch (axisNumber) {
                case Axis.ANCESTOR:
                case Axis.PARENT:
                case Axis.PRECEDING_OR_ANCESTOR:
                    return Navigator.filteredSingleton(TextFragmentValue.this, nodeTest);

                case Axis.ANCESTOR_OR_SELF:
                    boolean matchesDoc = nodeTest.matches(TextFragmentValue.this);
                    boolean matchesText = nodeTest.matches(this);
                    if (matchesDoc && matchesText) {
                        Item[] nodes = {this, TextFragmentValue.this};
                        return new ArrayIterator(nodes);
                    } else if (matchesDoc && !matchesText) {
                        return SingletonIterator.makeIterator(TextFragmentValue.this);
                    } else if (matchesText && !matchesDoc) {
                        return SingletonIterator.makeIterator(this);
                    } else {
                        return EmptyIterator.getInstance();
                    }

                case Axis.ATTRIBUTE:
                case Axis.CHILD:
                case Axis.DESCENDANT:
                case Axis.FOLLOWING:
                case Axis.FOLLOWING_SIBLING:
                case Axis.NAMESPACE:
                case Axis.PRECEDING:
                case Axis.PRECEDING_SIBLING:
                    return EmptyIterator.getInstance();

                case Axis.SELF:
                case Axis.DESCENDANT_OR_SELF:
                    return Navigator.filteredSingleton(this, nodeTest);

                default:
                     throw new IllegalArgumentException("Unknown axis number " + axisNumber);
            }
        }

        /**
         * Find the parent node of this node.
         * @return The Node object describing the containing element or root node.
         */

        public NodeInfo getParent() {
            return TextFragmentValue.this;
        }

        /**
        * Get the root node
        * @return the NodeInfo representing the root of this tree
        */

        public NodeInfo getRoot() {
            return TextFragmentValue.this;
        }

        /**
        * Get the root (document) node
        * @return the DocumentInfo representing the containing document
        */

        public DocumentInfo getDocumentRoot() {
            return TextFragmentValue.this;
        }

        /**
        * Copy the node to a given Outputter
        */

        public void copy(Receiver out, int namespaces, boolean copyAnnotations, int locationId)
        throws XPathException {
            out.characters(text, 0, 0);
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

