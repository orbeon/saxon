package org.orbeon.saxon.pull;

import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.instruct.DocumentInstr;
import org.orbeon.saxon.om.DocumentInfo;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.type.Type;

/**
 * A document node whose construction is deferred.
 * </p>
 * (TODO) NOTE: this class is an exception to the general rule that for document nodes, node identity implies object identity
 */

public class UnconstructedDocument extends UnconstructedParent implements DocumentInfo {

    public UnconstructedDocument(DocumentInstr instruction, XPathContext context) {
        super(instruction, context);
    }

    /**
     * Get name code. The name code is a coded form of the node name: two nodes
     * with the same name code have the same namespace URI, the same local name,
     * and the same prefix. By masking the name code with &0xfffff, you get a
     * fingerprint: two nodes with the same fingerprint have the same local name
     * and namespace URI.
     *
     * @return an integer name code, which may be used to obtain the actual node
     *         name from the name pool
     * @see net.sf.saxon.om.NamePool#allocate allocate
     * @see net.sf.saxon.om.NamePool#getFingerprint getFingerprint
     */

    public int getNameCode() {
        return -1;
    }

    public int getNodeKind() {
        return Type.DOCUMENT;
    }

    /**
     * Get fingerprint. The fingerprint is a coded form of the expanded name
     * of the node: two nodes
     * with the same name code have the same namespace URI and the same local name.
     * A fingerprint of -1 should be returned for a node with no name.
     *
     * @return an integer fingerprint; two nodes with the same fingerprint have
     *         the same expanded QName
     */

    public int getFingerprint() {
        return -1;
    }

    /**
     * Get the local part of the name of this node. This is the name after the ":" if any.
     *
     * @return the local part of the name. For an unnamed node, returns "". Unlike the DOM
     *         interface, this returns the full name in the case of a non-namespaced name.
     */

    public String getLocalPart() {
        return "";
    }

    /**
     * Get the URI part of the name of this node. This is the URI corresponding to the
     * prefix, or the URI of the default namespace if appropriate.
     *
     * @return The URI of the namespace of this node. For an unnamed node,
     *         or for a node with an empty prefix, return an empty
     *         string.
     */

    public String getURI() {
        return "";
    }

    /**
     * Get the display name of this node. For elements and attributes this is [prefix:]localname.
     * For unnamed nodes, it is an empty string.
     *
     * @return The display name of this node. For a node with no name, return
     *         an empty string.
     */

    public String getDisplayName() {
        return "";
    }

    /**
     * Get the prefix of the name of the node. This is defined only for elements and attributes.
     * If the node has no prefix, or for other kinds of node, return a zero-length string.
     *
     * @return The prefix of the name of the node.
     */

    public String getPrefix() {
        return "";
    }

    /**
     * Get the root node, if it is a document node.
     *
     * @return the DocumentInfo representing the containing document. If this
     *         node is part of a tree that does not have a document node as its
     *         root, return null.
     */

    public DocumentInfo getDocumentRoot() {
        return this;
    }

    /**
     * Get the element with a given ID, if any
     *
     * @param id the required ID value
     * @return the element with the given ID, or null if there is no such ID
     *         present (or if the parser has not notified attributes as being of
     *         type ID)
     * @since 8.4
     */

    public NodeInfo selectID(String id) {
        if (node == null) {
            tryToConstruct();
        }
        return ((DocumentInfo)node).selectID(id);
    }

    /**
     * Get the unparsed entity with a given name
     *
     * @param name the name of the entity
     * @return if the entity exists, return an array of two Strings, the first
     *         holding the system ID of the entity, the second holding the public
     *         ID if there is one, or null if not. If the entity does not exist,
     *         the method returns null. Applications should be written on the
     *         assumption that this array may be extended in the future to provide
     *         additional information.
     * @since 8.4
     */

    public String[] getUnparsedEntity(String name) {
        return null;
    }

}
