package net.sf.saxon.value;
import net.sf.saxon.Configuration;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.om.*;
import net.sf.saxon.pattern.NodeTest;
import net.sf.saxon.type.Type;
import net.sf.saxon.xpath.XPathException;

/**
* This class represents a temporary tree whose root document node owns a single text node. <BR>
*/

public final class TextFragmentValue extends AbstractNode implements DocumentInfo {

    private CharSequence text;
    private String systemId;
    private TextFragmentTextNode textNode = null;   // created on demand
    private Configuration config;
    private int documentNumber;

    /**
    * Constructor: create a result tree fragment containing a single text node
    * @param value a String containing the value
    */

    public TextFragmentValue(CharSequence value, String systemId) {
        this.text = value;
        this.systemId = systemId;
    }

	/**
	* Set the name pool used for all names in this document (actually, there aren't any, but
	* we have to support the DocumentInfo interface...
	*/

//	public void setNamePool(NamePool pool) {
//		namePool = pool;
//		documentNumber = pool.allocateDocumentNumber(this);
//	}

	/**
	* Set the configuration (containing the name pool used for all names in this document)
	*/

	public void setConfiguration(Configuration config) {
        this.config = config;
		documentNumber = config.getNamePool().allocateDocumentNumber(this);
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
    * Determine whether this is the same node as another node
    * @return true if this Node object and the supplied Node object represent the
    * same node in the tree.
    */

    public boolean isSameNodeInfo(NodeInfo other) {
        return this==other;
    }

    /**
    * Get a character string that uniquely identifies this node
    * @return a string.
    */

    public String generateId() {
        return "tt" + getDocumentNumber();
    }

    /**
    * Set the system ID for the entity containing the node.
    */

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    /**
    * Get the system ID for the entity containing the node.
    */

    public String getSystemId() {
        return systemId;
    }

    /**
    * Get the base URI for the node. Default implementation for child nodes gets
    * the base URI of the parent node.
    */

    public String getBaseURI() {
        return systemId;
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
     * Returns whether this node has any attributes.
     * @return <code>true</code> if this node has any attributes,
     *   <code>false</code> otherwise.
     * @since DOM Level 2
     */

    public boolean hasAttributes() {
        return false;
    }

    /**
     * Find the value of a given attribute of this node. <BR>
     * This method is defined on all nodes to meet XSL requirements, but for nodes
     * other than elements it will always return null.
     * @param uri the namespace uri of an attribute
     * @param localName the local name of an attribute
     * @return the value of the attribute, if it exists, otherwise null
     */

//    public String getAttributeValue( String uri, String localName ) {
//        return null;
//    }

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
    * @see net.sf.saxon.om.Axis
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
    * @see net.sf.saxon.om.Axis
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
                return SingletonIterator.makeIterator(this);

            case Axis.CHILD:
            case Axis.DESCENDANT:
                NodeInfo textNode = getTextNode();
                if (nodeTest.matches(textNode)) {
                    return SingletonIterator.makeIterator(textNode);
                } else {
                    return EmptyIterator.getInstance();
                }

            case Axis.DESCENDANT_OR_SELF:
                NodeInfo textNode2 = getTextNode();
                if (nodeTest.matches(textNode2)) {
                    Item[] nodes = {this, textNode2};
                    return new ArrayIterator(nodes);
                } else {
                    return SingletonIterator.makeIterator(this);
                }

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

    private class TextFragmentTextNode extends AbstractNode {

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
        * Determine whether this is the same node as another node
        * @return true if this Node object and the supplied Node object represent the
        * same node in the tree.
        */

        public boolean isSameNodeInfo(NodeInfo other) {
            return this==other;
        }

        /**
        * Get a character string that uniquely identifies this node
        * @return a string.
        */

        public String generateId() {
            return "tt" + getDocumentNumber() + "t1";
        }

        /**
        * Get the system ID for the entity containing the node.
        */

        public String getSystemId() {
            return systemId;
        }

        /**
        * Get the base URI for the node. Default implementation for child nodes gets
        * the base URI of the parent node.
        */

        public String getBaseURI() {
            return systemId;
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
         * Returns whether this node has any attributes.
         * @return <code>true</code> if this node has any attributes,
         *   <code>false</code> otherwise.
         * @since DOM Level 2
         */

        public boolean hasAttributes() {
            return false;
        }

        /**
         * Find the value of a given attribute of this node. <BR>
         * This method is defined on all nodes to meet XSL requirements, but for nodes
         * other than elements it will always return null.
         * @param uri the namespace uri of an attribute
         * @param localName the local name of an attribute
         * @return the value of the attribute, if it exists, otherwise null
         */

//        public String getAttributeValue( String uri, String localName ) {
//            return null;
//        }

        /**
        * Get the value of a given attribute of this node
        * @param fingerprint The fingerprint of the attribute name
        * @return the attribute value if it exists or null if not
        */

        public String getAttributeValue(int fingerprint) {
            return null;
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
                    if (nodeTest.matches(TextFragmentValue.this)) {
                        return SingletonIterator.makeIterator(TextFragmentValue.this);
                    } else {
                        return EmptyIterator.getInstance();
                    }

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
                    if (nodeTest.matches(this)) {
                        return SingletonIterator.makeIterator(this);
                    } else {
                        return EmptyIterator.getInstance();
                    }

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

