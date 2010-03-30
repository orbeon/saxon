package org.orbeon.saxon.s9api;

import org.orbeon.saxon.om.*;
import org.orbeon.saxon.pattern.NameTest;
import org.orbeon.saxon.query.QueryResult;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.Type;

import javax.xml.transform.Source;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * This class represents a node in the XDM data model. A Node is an {@link XdmItem}, and is therefore an
 * {@link XdmValue} in its own right, and may also participate as one item within a sequence value.
 * <p/>
 * <p>An XdmNode is implemented as a wrapper around an object of type {@link org.orbeon.saxon.om.NodeInfo}.
 * Because this is a key interface within Saxon, it is exposed via this API.</p>
 * <p/>
 * <p>The XdmNode interface exposes basic properties of the node, such as its name, its string value, and
 * its typed value. Navigation to other nodes is supported through a single method, {@link #axisIterator},
 * which allows other nodes to be retrieved by following any of the XPath axes.</p>
 * <p/>
 * <p>Note that node identity cannot be inferred from object identity. The same node may be represented
 * by different <code>XdmNode</code> instances at different times, or even at the same time. The equals()
 * method on this class can be used to test for node identity.</p>
 * @since 9.0
 */
public class XdmNode extends XdmItem {

    protected XdmNode(NodeInfo node) {
        super(node);
    }

    /**
     * Get the kind of node.
     * @return the kind of node, for example {@link XdmNodeKind#ELEMENT} or {@link XdmNodeKind#ATTRIBUTE}
     */

    public XdmNodeKind getNodeKind() {
        switch (getUnderlyingNode().getNodeKind()) {
            case Type.DOCUMENT:
                return XdmNodeKind.DOCUMENT;
            case Type.ELEMENT:
                return XdmNodeKind.ELEMENT;
            case Type.ATTRIBUTE:
                return XdmNodeKind.ATTRIBUTE;
            case Type.TEXT:
                return XdmNodeKind.TEXT;
            case Type.COMMENT:
                return XdmNodeKind.COMMENT;
            case Type.PROCESSING_INSTRUCTION:
                return XdmNodeKind.PROCESSING_INSTRUCTION;
            case Type.NAMESPACE:
                return XdmNodeKind.NAMESPACE;
            default:
                throw new IllegalStateException("nodeKind");
        }
    }

    /**
     * Get the name of the node, as a QName
     *
     * @return the name of the node. In the case of unnamed nodes (for example, text and comment nodes)
     *         return null.
     */

    public QName getNodeName() {
        int nc = getUnderlyingNode().getNameCode();
        if (nc == -1) {
            return null;
        }
        StructuredQName sqn = new StructuredQName(getUnderlyingNode().getNamePool(), nc);
        return new QName(sqn);
    }

    /**
     * Get the typed value of this node, as defined in XDM
     *
     * @return the typed value. If the typed value is atomic, this will be returned as an instance
     *         of {@link XdmAtomicValue}
     * @throws SaxonApiException if an error occurs obtaining the typed value, for example because
     *                           the node is an element with element-only content
     */

    public XdmValue getTypedValue() throws SaxonApiException {
        try {
            ValueRepresentation v = getUnderlyingNode().atomize();
            return XdmValue.wrap(v);
        } catch (XPathException e) {
            throw new SaxonApiException(e);
        }
    }

    /**
     * Get the line number of the node in a source document. For a document constructed using the document
     * builder, this is available only if the line numbering option was set when the document was built (and
     * then only for element nodes). If the line number is not available, the value -1 is returned. Line numbers
     * will typically be as reported by a SAX parser: this means that the line number for an element node is the
     * line number containing the closing ">" of the start tag.
     * @return the line number of the node, or -1 if not available.
     */

    public int getLineNumber() {
        return getUnderlyingNode().getLineNumber();
    }

    /**
     * Get a JAXP Source object corresponding to this node, allowing the node to be
     * used as input to transformations or queries
     * @return a Source object corresponding to this node
     */

    public Source asSource() {
        return getUnderlyingNode();
    }

    /**
     * Get an iterator over the nodes reachable from this node via a given axis.
     *
     * @param axis identifies which axis is to be navigated
     * @return an iterator over the nodes on the specified axis, starting from this node as the
     *         context node. The nodes are returned in axis order, that is, in document order for a forwards
     *         axis and in reverse document order for a reverse axis.
     */

    public XdmSequenceIterator axisIterator(Axis axis) {
        AxisIterator base = getUnderlyingNode().iterateAxis(axis.getAxisNumber());
        return new XdmSequenceIterator(base);
    }

    /**
     * Get an iterator over the nodes reachable from this node via a given axis, selecting only
     * those nodes with a specified name.
     *
     * @param axis identifies which axis is to be navigated
     * @param name identifies the name of the nodes to be selected. The selected nodes will be those
     *             whose node kind is the principal node kind of the axis (that is, attributes for the attribute
     *             axis, namespaces for the namespace axis, and elements for all other axes) whose name matches
     *             the specified name.
     *             <p>For example, specify <code>new QName("", "item")</code> to select nodes with local name
     *             "item", in no namespace.</p>
     * @return an iterator over the nodes on the specified axis, starting from this node as the
     *         context node. The nodes are returned in axis order, that is, in document order for a forwards
     *         axis and in reverse document order for a reverse axis.
     */

    public XdmSequenceIterator axisIterator(Axis axis, QName name) {
        int kind;
        switch (axis) {
            case ATTRIBUTE:
                kind = Type.ATTRIBUTE;
                break;
            case NAMESPACE:
                kind = Type.NAMESPACE;
                break;
            default:
                kind = Type.ELEMENT;
                break;
        }
        NamePool pool = getUnderlyingNode().getNamePool();
        int nameCode = pool.allocate("", name.getNamespaceURI(), name.getLocalName());
        NameTest test = new NameTest(kind, nameCode, pool);
        AxisIterator base = getUnderlyingNode().iterateAxis(axis.getAxisNumber(), test);
        return new XdmSequenceIterator(base);
    }

    /**
     * Get the parent of this node
     *
     * @return the parent of this node (a document or element node), or null if this node has no parent.
     */

    public XdmNode getParent() {
        return (XdmNode) XdmValue.wrap(getUnderlyingNode().getParent());
    }

    /**
     * Get the string value of a named attribute of this element
     * @param name the name of the required attribute
     * @return null if this node is not an element, or if this element has no
     *         attribute with the specified name. Otherwise return the string value of the
     *         selected attribute node.
     */

    public String getAttributeValue(QName name) {
        int fp = getUnderlyingNode().getNamePool().allocate("", name.getNamespaceURI(), name.getLocalName());
        return getUnderlyingNode().getAttributeValue(fp);
    }

    /**
     * Get the base URI of this node
     *
     * @return the base URI, as defined in the XDM model
     */

    public URI getBaseURI() {
        try {
            String uri = getUnderlyingNode().getBaseURI();
            if (uri == null) {
                return null;
            }
            return new URI(uri);
        } catch (URISyntaxException e) {
            throw new IllegalStateException("baseURI", e);
        }
    }

    /**
     * Get the document URI of this node.
     * @return the document URI, as defined in the XDM model. Returns null if no document URI is known
     * @since 9.1
     */

    public URI getDocumentURI() {
        try {
            String systemId = getUnderlyingNode().getSystemId();
            return (systemId == null || systemId.length() == 0 ? null : new URI(systemId));
        } catch (URISyntaxException e) {
            throw new IllegalStateException("documentURI");
        }        
    }



    /**
     * The hashcode is such that two XdmNode instances have the same hashCode if they represent the same
     * node. Note that the same node might be represented by different XdmNode objects, but these will
     * compare equal.
     *
     * @return a hashCode representing node identity
     */

    public int hashCode() {
        return getUnderlyingNode().hashCode();
    }

    /**
     * The <code>equals()</code> relation between two XdmNode objects is true if they both represent the same
     * node. That is, it corresponds to the "is" operator in XPath.
     *
     * @param other the object to be compared
     * @return true if and only if the other object is an XdmNode instance representing the same node
     */

    public boolean equals(Object other) {
        return other instanceof XdmNode &&
                getUnderlyingNode().isSameNodeInfo(((XdmNode) other).getUnderlyingNode());
    }

    /**
     * The toString() method returns a simple XML serialization of the node
     * with defaulted serialization parameters.
     *
     * <p>In the case of an element node, the result will be a well-formed
     * XML document serialized as defined in the W3C XSLT/XQuery serialization specification,
     * using options method="xml", indent="yes", omit-xml-declaration="yes".</p>
     *
     * <p>In the case of a document node, the result will be a well-formed
     * XML document provided that the document node contains exactly one element child,
     * and no text node children. In other cases it will be a well-formed external
     * general parsed entity.</p>
     * 
     * <p>In the case of an attribute node, the output is a string in the form
     * <code>name="value"</code>. The name will use the original namespace prefix.</p>
     * <p>Other nodes, such as text nodes, comments, and processing instructions, are
     * represented as they would appear in lexical XML.</p>
     *
     * <p><i>For more control over serialization, use the {@link Serializer} class.</i></p>
     *
     * @return a simple XML serialization of the node. Under error conditions the method
     *         may return an error message which will always begin with the label "Error: ".
     */

    public String toString() {
        NodeInfo node = getUnderlyingNode();

        if (node.getNodeKind() == Type.ATTRIBUTE) {
            String val = node.getStringValue().replace("\"", "&quot;");
            val = val.replace("<", "&lt;");
            val = val.replace("&", "&amp;");
            return node.getDisplayName() + "=\"" + val + '"';
        } else if (node.getNodeKind() == Type.NAMESPACE) {
            String val = node.getStringValue().replace("\"", "&quot;");
            val = val.replace("<", "&lt;");
            val = val.replace("&", "&amp;");
            String name = node.getDisplayName();
            name = (name.equals("") ? "xmlns" : "xmlns:" + name);
            return name + "=\"" + val + '"';
        }

        try {
            return QueryResult.serialize(node);
        } catch (XPathException err) {
            return("Error: " + err.getMessage());
        }
    }

    /**
     * Get the underlying Saxon implementation object representing this node. This provides
     * access to classes and methods in the Saxon implementation that may be subject to change
     * from one release to another.
     *
     * @return the underlying implementation object
     */

    public NodeInfo getUnderlyingNode() {
        return (NodeInfo) getUnderlyingValue();
    }

    /**
     * In the case of an XdmNode that wraps a node in an external object model such as DOM, JDOM,
     * XOM, or DOM4J, get the underlying wrapped node
     * @return the underlying external node if there is one, or null if this is not an XdmNode that
     * wraps such an external node
     * @since 9.1.0.2
     */

    public Object getExternalNode() {
        NodeInfo saxonNode = getUnderlyingNode();
        if (saxonNode instanceof VirtualNode) {
            Object externalNode = ((VirtualNode)saxonNode).getUnderlyingNode();
            return (externalNode instanceof NodeInfo ? null : externalNode);
        } else {
            return null;
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

