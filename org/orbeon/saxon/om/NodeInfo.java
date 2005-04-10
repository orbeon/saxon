package net.sf.saxon.om;
import net.sf.saxon.Configuration;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.pattern.NodeTest;
import net.sf.saxon.trans.XPathException;

import javax.xml.transform.Source;

/**
 * The NodeInfo interface represents a node in Saxon's implementation of the XPath 2.0 data model.
 * <p>
 * Note that several NodeInfo objects may represent the same node. To test node identity, the
 * method {@link #isSameNodeInfo(NodeInfo)} should be used. An exception to this rule applies for
 * document nodes, where the correspondence between document nodes and DocumentInfo objects is one to
 * one. NodeInfo objects are never reused: a given NodeInfo object represents the same node for its entire
 * lifetime.
 * <p>
 * This is the primary interface for accessing trees in Saxon, and it forms part of the public
 * Saxon API. The only subclass of NodeInfo that applications should normally use is {@link DocumentInfo},
 * which represents a document node. Methods that form part of the public API are (since Saxon 8.4)
 * labelled with a JavaDoc "since" tag: classes and methods that have no such label should not be
 * regarded as stable interfaces.
 * <p>
 * The interface represented by this class is at a slightly higher level than the abstraction described
 * in the W3C data model specification, in that it includes support for the XPath axes, rather than exposing
 * the lower-level properties (such as "parent" and "children") directly. All navigation within trees,
 * except for a few convenience methods, is done by following the axes using the {@link #iterateAxis} method.
 * This allows different implementations of the XPath tree model to implement axis navigation in different ways.
 * Some implementations may choose to use the helper methods provided in class {@link Navigator}.
 * <p>
 * Note that the stability of this interface applies to classes that use the interface,
 * not to classes that implement it. The interface may be extended in future to add new methods.
 *
 * @author Michael H. Kay
 * @since 8.4
 */

public interface NodeInfo extends Source, Item, ValueRepresentation {

    int[] EMPTY_NAMESPACE_LIST = new int[0];

    /**
     * Get the kind of node. This will be a value such as {@link net.sf.saxon.type.Type#ELEMENT}
     * or {@link net.sf.saxon.type.Type#ATTRIBUTE}. There are seven kinds of node: documents, elements, attributes,
     * text, comments, processing-instructions, and namespaces.
     *
     * @return an integer identifying the kind of node. These integer values are the
     * same as those used in the DOM
     * @see net.sf.saxon.type.Type
     * @since 8.4
     */

    public int getNodeKind();

    /**
     * Determine whether this is the same node as another node.
     * <p>
     * Note that two different NodeInfo instances can represent the same conceptual node.
     * Therefore the "==" operator should not be used to test node identity. The equals()
     * method is not overridden for nodes, so it has the same effect as using "==".
     * <p>
     * Note: a.isSameNodeInfo(b) if and only if generateId(a)==generateId(b).
     * <p>
     * This method has the same semantics as isSameNode() in DOM Level 3, but
     * works on Saxon NodeInfo objects rather than DOM Node objects.
     *
     * @param other the node to be compared with this node
     * @return true if this NodeInfo object and the supplied NodeInfo object represent
     *      the same node in the tree.
     */

    public boolean isSameNodeInfo(NodeInfo other);

    /**
     * Get the System ID for the node. Note this is not the
     * same as the base URI: the base URI can be modified by xml:base, but
     * the system ID cannot. The base URI is used primarily for resolving
     * relative URIs within the content of the document. The system ID is
     * used primarily in conjunction with a line number, for identifying the
     * location of elements within the source XML, in particular when errors
     * are found.
     *
     * @return the System Identifier of the entity in the source document
     * containing the node, or null if not known.
     * @since 8.4
     */

    public String getSystemId();

    /**
     * Get the Base URI for the node, that is, the URI used for resolving a relative URI contained
     * in the node. This will be the same as the System ID unless xml:base has been used. Where the
     * node does not have a base URI of its own, the base URI of its parent node is returned.
     *
     * @return the base URI of the node. This may be null if the base URI is unknown.
     * @since 8.4
     */

    public String getBaseURI();

    /**
     * Get line number. Line numbers are not maintained by default, except for
     * stylesheets and schema documents. Line numbering can be requested using the
     * -l option on the command line, or by setting options on the TransformerFactory
     * or the Configuration before the source document is built.
     * <p>
     * The granularity of line numbering is normally the element level: for other nodes
     * such as text nodes and attributes, the line number of the parent element will normally be returned.
     * <p>
     * In the case of a tree constructed by taking input from a SAX parser, the line number will reflect the
     * SAX rules: that is, the line number of an element is the line number where the start tag ends. This
     * may be a little confusing where elements have many attributes spread over multiple lines, or where
     * single attributes (as can easily happen with XSLT 2.0 stylesheets) occupy several lines.
     * <p>
     * In the case of a tree constructed by a stylesheet or query, the line number may reflect the line in
     * the stylesheet or query that caused the node to be constructed.
     * <p>
     * The line number can be read from within an XPath expression using the Saxon extension function
     * saxon:line-number()
     *
     * @return the line number of the node in its original source document; or
     *      -1 if not available
     * @since 8.4
     */

    public int getLineNumber();

    /**
     * Determine the relative position of this node and another node, in document order.
     * <p>
     * The other node must always be in the same tree; the effect of calling this method
     * when the two nodes are in different trees is undefined. To obtain a global ordering
     * of nodes, the application should first compare the result of getDocumentNumber(),
     * and only if the document number is the same should compareOrder() be called.
     *
     * @param other The other node, whose position is to be compared with this
     *      node
     * @return -1 if this node precedes the other node, +1 if it follows the
     *     other node, or 0 if they are the same node. (In this case,
     *     isSameNode() will always return true, and the two nodes will
     *     produce the same result for generateId())
     * @since 8.4
     */

    public int compareOrder(NodeInfo other);

    /**
     * Return the string value of the node. This is normally the string value as defined in
     * the XPath data model, except that no distinction is made between strings and untyped atomic values.
     * <p>
     * The interpretation of this depends on the type
     * of node. For an element it is the accumulated character content of the element,
     * including descendant elements.
     * <p>
     * This method returns the string value as if the node were untyped. Unlike the string value
     * accessor in the XPath 2.0 data model, it does not report an error if the element has a complex
     * type, instead it returns the concatenation of the descendant text nodes as it would if the element
     * were untyped.
     *
     * @return the string value of the node
     * @since 8.4
     */

    public String getStringValue();

	/**
	 * Get name code. The name code is a coded form of the node name: two nodes
	 * with the same name code have the same namespace URI, the same local name,
	 * and the same prefix. By masking the name code with {@link NamePool#FP_MASK}, you get a
	 * fingerprint: two nodes with the same fingerprint have the same local name
	 * and namespace URI.
	 *
	 * @return an integer name code, which may be used to obtain the actual node
	 *     name from the name pool. For unnamed nodes (text nodes, comments, document nodes,
     *     and namespace nodes for the default namespace), returns -1.
	 * @see net.sf.saxon.om.NamePool#allocate allocate
	 * @see net.sf.saxon.om.NamePool#getFingerprint getFingerprint
     * @since 8.4
	 */

	public int getNameCode();

	/**
	 * Get fingerprint. The fingerprint is a coded form of the expanded name
	 * of the node: two nodes
	 * with the same name code have the same namespace URI and the same local name.
     * The fingerprint contains no information about the namespace prefix. For a name
     * in the null namespace, the fingerprint is the same as the name code.
	 *
	 * @return an integer fingerprint; two nodes with the same fingerprint have
	 *     the same expanded QName. For unnamed nodes (text nodes, comments, document nodes,
     *     and namespace nodes for the default namespace), returns -1.
     * @since 8.4
	 */

	public int getFingerprint();

    /**
     * Get the local part of the name of this node. This is the name after the ":" if any.
     *
     * @return the local part of the name. For an unnamed node, returns "". Unlike the DOM
     * interface, this returns the full name in the case of a non-namespaced name.
     * @since 8.4
     */

    public String getLocalPart();

    /**
     * Get the URI part of the name of this node. This is the URI corresponding to the
     * prefix, or the URI of the default namespace if appropriate.
     *
     * @return The URI of the namespace of this node. For an unnamed node,
     *     or for a node with an empty prefix, returns an empty
     *     string.
     * @since 8.4
     */

    public String getURI();

    /**
     * Get the display name of this node, in the form of a lexical QName.
     * For elements and attributes this is [prefix:]localname.
     * For unnamed nodes, it is an empty string.
     *
     * @return The display name of this node. For a node with no name, returns
     *     an empty string.
     * @since 8.4
     */

    public String getDisplayName();

    /**
     * Get the prefix of the name of the node. This is defined only for elements and attributes.
     * If the node has no prefix, or for other kinds of node, returns a zero-length string.
     * @return The prefix of the name of the node.
     * @since 8.4
     */

    public String getPrefix();

    /**
     * Get the configuration used to build the tree containing this node.
     * @return the Configuration
     * @since 8.4
     */

    public Configuration getConfiguration();

   /**
    * Get the NamePool that holds the namecode for this node
    * @return the namepool
    * @since 8.4
    */

    public NamePool getNamePool();

    /**
     * Get the type annotation of this node, if any. The type annotation is represented as an integer;
     * this is the fingerprint of the name of the type, as defined in the name pool. Anonymous types
     * are given a system-defined name. The value of the type annotation can be used to retrieve the
     * actual schema type definition using the method {@link Configuration#getSchemaType}.
     *
     * @return the type annotation of the node.
     * Returns -1 for kinds of nodes that have no annotation, and for elements annotated as
     * untyped, and attributes annotated as untypedAtomic.
     * <p>
     * Note that the distinction between the types xs:anyType and xdt:untyped is not yet fully implemented
     * in Saxon: -1 is used to represent either of these values.
     * @since 8.4
     */

    public int getTypeAnnotation();

    /**
     * Get the NodeInfo object representing the parent of this node
     *
     * @return the parent of this node; null if this node has no parent
     * @since 8.4
     */

    public NodeInfo getParent();

    /**
     * Return an iteration over all the nodes reached by the given axis from this node
     *
     * @exception UnsupportedOperationException if the namespace axis is
     *     requested and this axis is not supported for this implementation.
     * @param axisNumber an integer identifying the axis; one of the constants
     *      defined in class {@link net.sf.saxon.om.Axis}
     * @return an AxisIterator that scans the nodes reached by the axis in
     *     turn.
     * @see net.sf.saxon.om.Axis
     * @since 8.4
     */

    public AxisIterator iterateAxis(byte axisNumber);


    /**
     * Return an iteration over all the nodes reached by the given axis from this node
     * that match a given NodeTest
     *
     * @exception UnsupportedOperationException if the namespace axis is
     *     requested and this axis is not supported for this implementation.
     * @param axisNumber an integer identifying the axis; one of the constants
     *      defined in class {@link net.sf.saxon.om.Axis}
     * @param nodeTest A pattern to be matched by the returned nodes; nodes
     * that do not match this pattern are not included in the result
     * @return a NodeEnumeration that scans the nodes reached by the axis in
     *     turn.
     * @see net.sf.saxon.om.Axis
     * @since 8.4
     */

    public AxisIterator iterateAxis(byte axisNumber, NodeTest nodeTest);

    /**
     * Get the string value of a given attribute of this node
     *
     * @param fingerprint The fingerprint of the attribute name
     * @return the attribute value if it exists, or null if it does not exist. Always returns null
     * if this node is not an element.
     * @since 8.4
     */

    public String getAttributeValue(int fingerprint);

    /**
     * Get the root node of the tree containing this node
     *
     * @return the NodeInfo representing the top-level ancestor of this node.
     *     This will not necessarily be a document node. If this node has no parent,
     *     then the method returns this node.
     * @since 8.4
     */

    public NodeInfo getRoot();

    /**
     * Get the root node, if it is a document node.
     *
     * @return the DocumentInfo representing the containing document. If this
     *     node is part of a tree that does not have a document node as its
     *     root, returns null.
     * @since 8.4
     */

    public DocumentInfo getDocumentRoot();

    /**
     * Determine whether the node has any children.
     * <p>
     * Note: the result is equivalent to <br />
     * <code>iterateAxis(Axis.CHILD).next() != null</code>
     *
     * @return True if the node has one or more children
     * @since 8.4
     */

    public boolean hasChildNodes();

    /**
     * Get a character string that uniquely identifies this node.
     * Note: a.isSameNode(b) if and only if generateId(a)==generateId(b)
     *
     * @return a string that uniquely identifies this node, across all
     *     documents. (Changed in Saxon 7.5. Previously this method returned
     *     an id that was unique within the current document, and the calling
     *     code prepended a document id).
     * @since 8.4
     */

    public String generateId();

    /**
     * Get the document number of the document containing this node. For a free-standing
     * orphan node, just return the hashcode.
     * @since 8.4
     */

    public int getDocumentNumber();

    /**
     * Copy this node to a given outputter.
     * <p>
     * This method is primarily for internal use. It should not be considered a stable
     * part of the Saxon API.
     *
     * @exception XPathException
     * @param out the Receiver to which the node should be copied
     * @param whichNamespaces in the case of an element, controls
     *     which namespace nodes should be copied. Values are {@link #NO_NAMESPACES},
     *     {@link #LOCAL_NAMESPACES}, {@link #ALL_NAMESPACES}
     * @param copyAnnotations indicates whether the type annotations
     *     of element and attribute nodes should be copied
     * @param locationId If non-zero, identifies the location of the instruction
     *     that requested this copy. If zero, indicates that the location information
     *     for the original node is to be copied; in this case the Receiver must be
     *     a LocationCopier
     */

    public void copy(Receiver out, int whichNamespaces, boolean copyAnnotations, int locationId) throws XPathException;

    /**
     * Don't copy any namespace nodes.
     */

    int NO_NAMESPACES = 0;

    /**
     * Copy namespaces declared (or undeclared) on this element, but not namespaces inherited from a parent element
     */
    int LOCAL_NAMESPACES = 1;

    /**
     * Copy all in-scope namespaces
     */
    int ALL_NAMESPACES = 2;

    /**
     * Output all namespace declarations associated with this element. Does nothing if
     * the node is not an element.
     * <p>
     * This method is primarily for internal use. It should not be considered a stable part of the
     * Saxon API.
     *
     * @param out The relevant Receiver
     * @param includeAncestors True if namespaces declared on ancestor
     *     elements must be output; false if it is known that these are
     *
     */

    public void sendNamespaceDeclarations(Receiver out, boolean includeAncestors)
        throws XPathException;

    /**
     * Get all namespace undeclarations and undeclarations defined on this element.
     * <p>
     * This method is intended primarily for internal use. User applications needing
     * information about the namespace context of a node should use <code>iterateAxis(Axis.NAMESPACE)</code>.
     *
     * @param buffer If this is non-null, and the result array fits in this buffer, then the result
     * may overwrite the contents of this array, to avoid the cost of allocating a new array on the heap.
     * @return An array of integers representing the namespace declarations and undeclarations present on
     * this element. For a node other than an element, return null. Otherwise, the returned array is a
     * sequence of namespace codes, whose meaning may be interpreted by reference to the name pool. The
     * top half word of each namespace code represents the prefix, the bottom half represents the URI.
     * If the bottom half is zero, then this is a namespace undeclaration rather than a declaration.
     * The XML namespace is never included in the list. If the supplied array is larger than required,
     * then the first unused entry will be set to -1.
     * <p>
     * For a node other than an element, the method returns null.</p>
     */

    public int[] getDeclaredNamespaces(int[] buffer);

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
