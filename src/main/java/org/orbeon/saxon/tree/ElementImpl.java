package org.orbeon.saxon.tree;
import org.orbeon.saxon.event.CopyInformee;
import org.orbeon.saxon.event.Receiver;
import org.orbeon.saxon.event.ReceiverOptions;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.pattern.NodeKindTest;
import org.orbeon.saxon.sort.IntArraySet;
import org.orbeon.saxon.sort.IntHashSet;
import org.orbeon.saxon.sort.IntIterator;
import org.orbeon.saxon.sort.IntSet;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.Whitespace;

import java.util.Iterator;

/**
  * ElementImpl implements an element with no attributes or namespace declarations.<P>
  * This class is an implementation of NodeInfo. For elements with attributes or
  * namespace declarations, class ElementWithAttributes is used.
  * @author Michael H. Kay
  */


public class ElementImpl extends ParentNodeImpl implements NamespaceResolver {

    private static final AttributeCollectionImpl emptyAtts = new AttributeCollectionImpl(null);

    protected int nameCode;
    protected int typeCode;
    protected AttributeCollection attributeList;      // this excludes namespace attributes
    protected int[] namespaceList = null;             // list of namespace codes

    /**
    * Construct an empty ElementImpl
    */

    public ElementImpl() {}

    /**
     * Set the name code. Used when creating a dummy element in the Stripper
     * @param nameCode the integer name code representing the element name
    */

    public void setNameCode(int nameCode) {
    	this.nameCode = nameCode;
    }

    /**
     * Initialise a new ElementImpl with an element name
     * @param nameCode  Integer representing the element name, with namespaces resolved
     * @param typeCode  Integer representing the schema type of the element node
     * @param atts The attribute list: always null
     * @param parent  The parent node
     * @param sequenceNumber  Integer identifying this element within the document
     */

    public void initialise(int nameCode, int typeCode, AttributeCollectionImpl atts, NodeInfo parent,
                           int sequenceNumber) {
        this.nameCode = nameCode;
        this.typeCode = (typeCode == -1 ? StandardNames.XS_UNTYPED : typeCode);
        this.parent = (ParentNodeImpl)parent;
        sequence = sequenceNumber;
        attributeList = atts;
    }

    /**
     * Set location information for this node
     * @param systemId the base URI
     * @param line the line number if known
     * @param column the column number if known
     */

    public void setLocation(String systemId, int line, int column) {
        DocumentImpl root = parent.getPhysicalRoot();
        root.setLineAndColumn(sequence, line, column);
        root.setSystemId(sequence, systemId);
    }

    /**
    * Set the system ID of this node. This method is provided so that a NodeInfo
    * implements the javax.xml.transform.Source interface, allowing a node to be
    * used directly as the Source of a transformation
    */

    public void setSystemId(String uri) {
        getPhysicalRoot().setSystemId(sequence, uri);
    }

	/**
	* Get the root node
	*/

	public NodeInfo getRoot() {
        ParentNodeImpl up = parent;
        if (up == null || (up instanceof DocumentImpl && ((DocumentImpl)up).isImaginary())) {
            return this;
        } else {
            return up.getRoot();
        }
    }

    /**
     * Get the root node, if it is a document node.
     *
     * @return the DocumentInfo representing the containing document. If this
     *     node is part of a tree that does not have a document node as its
     *     root, returns null.
     * @since 8.4
     */

	public DocumentInfo getDocumentRoot() {
		NodeInfo root = getRoot();
        if (root instanceof DocumentInfo) {
            return (DocumentInfo)root;
        } else {
            return null;
        }
    }

    /**
    * Get the system ID of the entity containing this element node.
    */

    public final String getSystemId() {
        DocumentImpl root = getPhysicalRoot();
        return (root == null ? null : root.getSystemId(sequence));
    }

    /**
    * Get the base URI of this element node. This will be the same as the System ID unless
    * xml:base has been used.
    */

    public String getBaseURI() {
        return Navigator.getBaseURI(this);
    }


    /**
     * Determine whether the node has the is-nilled property
     *
     * @return true if the node has the is-nilled property
     */

    public boolean isNilled() {
        return (typeCode & NodeInfo.IS_NILLED) != 0;
    }

    /**
     * Set the type annotation on a node. This must only be called when the caller has verified (by validation)
     * that the node is a valid instance of the specified type. The call is ignored if the node is not an element
     * or attribute node.
     *
     * @param typeCode the type annotation (possibly including high bits set to indicate the isID, isIDREF, and
     *                 isNilled properties)
     */

    public void setTypeAnnotation(int typeCode) {
        if (typeCode == -1) {
            typeCode = StandardNames.XS_UNTYPED;
        }
        this.typeCode = typeCode;
    }

    /**
     * Get the type annotation of this node, if any
     * @return the type annotation, as the integer name code of the type name
     */

    public int getTypeAnnotation() {
        return typeCode & NamePool.FP_MASK;
    }

    /**
     * Set the line number of the element within its source document entity
     * @param line the line number
     * @param column the column number
    */

    public void setLineAndColumn(int line, int column) {
        DocumentImpl root = getPhysicalRoot();
        if (root != null) {
            root.setLineAndColumn(sequence, line, column);
        }
    }

    /**
    * Get the line number of the node within its source document entity
    */

    public int getLineNumber() {
        DocumentImpl root = getPhysicalRoot();
        if (root == null) {
            return -1;
        } else {
            return root.getLineNumber(sequence);
        }
    }

    /**
    * Get the line number of the node within its source document entity
    */

    public int getColumnNumber() {
        DocumentImpl root = getPhysicalRoot();
        if (root == null) {
            return -1;
        } else {
            return root.getColumnNumber(sequence);
        }
    }

    /**
	* Get the nameCode of the node. This is used to locate the name in the NamePool
	*/

	public int getNameCode() {
		return nameCode;
	}

    /**
     * Get a character string that uniquely identifies this node
     * @param buffer to contain the generated ID
     */

    public void generateId(FastStringBuffer buffer) {
        if (sequence >= 0) {
            getPhysicalRoot().generateId(buffer);
            buffer.append("e");
            buffer.append(Integer.toString(sequence));
        } else {
            parent.generateId(buffer);
            buffer.append("f");
            buffer.append(Integer.toString(index));
        }
    }

    /**
    * Return the kind of node.
    * @return Type.ELEMENT
    */

    public final int getNodeKind() {
        return Type.ELEMENT;
    }

    /**
    * Copy this node to a given outputter (supporting xsl:copy-of)
    * @param out The outputter
    * @param whichNamespaces indicates which namespaces should be output: all, none, or local
    * namespaces only (those not declared on the parent element)
    */

    public void copy(Receiver out, int whichNamespaces, boolean copyAnnotations, int locationId) throws XPathException {

        int typeCode = (copyAnnotations ? getTypeAnnotation() : StandardNames.XS_UNTYPED);
        if (locationId == 0 && out instanceof CopyInformee) {
            ((CopyInformee)out).notifyElementNode(this);
        }
        out.startElement(getNameCode(), typeCode, locationId, 0);

        // output the namespaces

        switch (whichNamespaces) {
            case NodeInfo.NO_NAMESPACES:
                break;
            case NodeInfo.LOCAL_NAMESPACES:
                int[] localNamespaces = getDeclaredNamespaces(null);
                for (int i=0; i<localNamespaces.length; i++) {
                    int ns = localNamespaces[i];
                    if (ns == -1) {
                        break;
                    }
                    out.namespace(ns, 0);
                }
                break;
            case NodeInfo.ALL_NAMESPACES:
                NamespaceCodeIterator.sendNamespaces(this, out);
                break;
        }

        // output the attributes

        if (attributeList != null) {
            for (int i=0; i<attributeList.getLength(); i++) {
                out.attribute(attributeList.getNameCode(i), StandardNames.XS_UNTYPED_ATOMIC,
                                   attributeList.getValue(i), 0, 0);
            }
        }

        out.startContent();

        // output the children

        int childNamespaces = (whichNamespaces==NO_NAMESPACES ? NO_NAMESPACES : LOCAL_NAMESPACES);
        NodeImpl next = (NodeImpl)getFirstChild();
        while (next!=null) {
            next.copy(out, childNamespaces, copyAnnotations, locationId);
            next = (NodeImpl)next.getNextSibling();
        }

        out.endElement();
    }

    /**
     * Delete this node (that is, detach it from its parent)
     */

    public void delete() {
        DocumentImpl root = getPhysicalRoot();
        super.delete();
        if (root != null) {
            AxisIterator iter = iterateAxis(Axis.DESCENDANT_OR_SELF, NodeKindTest.ELEMENT);
            while (true) {
                ElementImpl n = (ElementImpl)iter.next();
                int atts = attributeList.getLength();
                for (int index=0; index<atts; index++) {
                    if (attributeList.isId(index)) {
                        root.deregisterID(attributeList.getValue(index));
                    }
                }
                if (n == null) {
                    break;
                }
                root.deIndex(n);
            }
        }
    }

    /**
     * Rename this node
     *
     * @param newNameCode the NamePool code of the new name
     */

    public void rename(int newNameCode) {
        nameCode = newNameCode;
        int nscode = getNamePool().getNamespaceCode(newNameCode);
        int prefixCode = nscode>>16 & 0xffff;
        short uc = getURICodeForPrefixCode(prefixCode);
        if (uc == -1) {
            addNamespace(nscode, false);
        } else if (uc != (nscode&0xffff)) {
            throw new IllegalArgumentException(
                    "Namespace binding of new name conflicts with existing namespace binding");
        }
    }

    /**
     * Add a namespace binding (that is, a namespace node) to this element. This call has no effect if applied
     * to a node other than an element.
     * @param nscode The namespace code representing the (prefix, uri) pair of the namespace binding to be
     * added. If the target element already has a namespace binding with this (prefix, uri) pair, the call has
     * no effect. If the target element currently has a namespace binding with this prefix and a different URI, an
     * exception is raised.
     * @param inherit If true, the new namespace binding will be inherited by any children of the target element
     * that do not already have a namespace binding for the specified prefix, recursively.
     * If false, the new namespace binding will not be inherited.
     * @throws IllegalArgumentException if the target element already has a namespace binding for this prefix,
     * or if the namespace code represents a namespace undeclaration
     */

    public void addNamespace(int nscode, boolean inherit) {
        if ((nscode&0xffff) == 0) {
            throw new IllegalArgumentException("Cannot add a namespace undeclaration");
        }
        addNamespaceInternal(nscode, true);

        // The data model is such that namespaces are inherited by default. If inheritance is NOT requested,
        // we must process the children to add namespace undeclarations
        if (hasChildNodes() && !inherit) {
            int undecl = nscode & 0xffff0000;
            AxisIterator kids = enumerateChildren(NodeKindTest.ELEMENT);
            while (true) {
                ElementImpl child = (ElementImpl)kids.next();
                if (child == null) {
                    break;
                }
                child.addNamespaceInternal(undecl, false);
            }
        }
    }

    private void addNamespaceInternal(int nscode, boolean externalCall) {
        if (namespaceList == null) {
            namespaceList = new int[]{nscode};
        } else {
            for (int i=0; i<namespaceList.length; i++) {
                if (namespaceList[i] == nscode) {
                    return;
                }
                if ((namespaceList[i]&0xffff0000) == (nscode&0xffff0000)) {
                    if ((namespaceList[i]&0x0000ffff) == 0) {
                        // this is an undeclaration; replace it with the new declaration
                        namespaceList[i] = nscode;
                    } else if (externalCall) {
                        throw new IllegalArgumentException("New namespace conflicts with existing namespace binding");
                    } else {
                        return;
                    }
                }
            }
            int len = namespaceList.length;
            int[] ns2 = new int[len + 1];
            System.arraycopy(namespaceList, 0, ns2, 0, len);
            ns2[len] = nscode;
            namespaceList = ns2;
        }
    }


    /**
     * Replace the string-value of this node
     *
     * @param stringValue the new string value
     */

    public void replaceStringValue(CharSequence stringValue) {
        if (stringValue.length() == 0) {
            children = null;
        } else {
            children = new TextImpl(this, stringValue.toString());
        }
    }

    /**
     * Add an attribute to this element node.
     * <p/>
     * <p>If this node is not an element, or if the supplied node is not an attribute, the method
     * takes no action. If the element already has an attribute with this name, the existing attribute
     * is replaced.</p>
     *
     * @param nameCode the name of the new attribute
     * @param typeCode the type annotation of the new attribute
     * @param value the string value of the new attribute
     * @param properties properties including IS_ID and IS_IDREF properties
     */

   public void putAttribute(int nameCode, int typeCode, CharSequence value, int properties) {
        if (attributeList == null || attributeList.getLength() == 0) {
            attributeList = new AttributeCollectionImpl(getConfiguration());
        }
        AttributeCollectionImpl atts = (AttributeCollectionImpl)attributeList;
        int index = atts.getIndexByFingerprint(nameCode & NamePool.FP_MASK);
        if (index == -1) {
            atts.addAttribute(nameCode, typeCode, value.toString(), 0, 0);
        } else {
            if (atts.isId(index)) {
                DocumentImpl root = getPhysicalRoot();
                root.deregisterID(atts.getValue(index));
            }
            atts.setAttribute(index, nameCode, typeCode, value.toString(), 0, 0);
        }
        if ((properties & ReceiverOptions.IS_ID) != 0) {
            DocumentImpl root = getPhysicalRoot();
            if (root != null) {
                root.registerID(this, Whitespace.trim(value));
            }
        }
    }

    /**
     * Remove an attribute from this element node
     * @param nameCode the name of the attribute to be removed
     */

    public void removeAttribute(int nameCode) {
        AttributeCollectionImpl atts = (AttributeCollectionImpl)getAttributeList();
        int fp = nameCode & NamePool.FP_MASK;
        int index = atts.getIndexByFingerprint(fp);
        if (index >= 0 && atts.isId(index)) {
            DocumentImpl root = getPhysicalRoot();
            root.deregisterID(atts.getValue(index));
        }
        atts.removeAttribute(fp);
    }


    /**
     * Remove type information from this node (and its ancestors, recursively).
     * This method implements the upd:removeType() primitive defined in the XQuery Update specification
     *
     */

    public void removeTypeAnnotation() {
        int t = getTypeAnnotation();
        if (t != StandardNames.XS_UNTYPED) {
            typeCode = StandardNames.XS_ANY_TYPE;
            parent.removeTypeAnnotation();
        }
    }

    /**
     * Set the namespace declarations for the element
     * @param namespaces the list of namespace codes
     * @param namespacesUsed the number of entries in the list that are used
    */

    public void setNamespaceDeclarations(int[] namespaces, int namespacesUsed) {
        namespaceList = new int[namespacesUsed];
        System.arraycopy(namespaces, 0, namespaceList, 0, namespacesUsed);
    }

    /**
     * Get the namespace URI corresponding to a given prefix. Return null
     * if the prefix is not in scope.
     *
     * @param prefix     the namespace prefix. May be the zero-length string, indicating
     *                   that there is no prefix. This indicates either the default namespace or the
     *                   null namespace, depending on the value of useDefault.
     * @param useDefault true if the default namespace is to be used when the
     *                   prefix is "". If false, the method returns "" when the prefix is "".
     * @return the uri for the namespace, or null if the prefix is not in scope.
     *         The "null namespace" is represented by the pseudo-URI "".
     */

    public String getURIForPrefix(String prefix, boolean useDefault) {
        if (prefix.equals("xml")) {
            return NamespaceConstant.XML;
        }
        if (prefix.length() == 0 && !useDefault) {
            return "";
        }

		NamePool pool = getNamePool();
		int prefixCode = pool.getCodeForPrefix(prefix);
		if (prefixCode==-1) {
		    return null;
		}
        short uriCode = getURICodeForPrefixCode(prefixCode);
        if (uriCode == -1) {
            return null;
        }
        return pool.getURIFromURICode(uriCode);
    }

    /**
     * Get an iterator over all the prefixes declared in this namespace context. This will include
     * the default namespace (prefix="") and the XML namespace where appropriate
     */

    public Iterator iteratePrefixes() {
        return new Iterator() {
            private NamePool pool = null;
            private IntIterator iter = NamespaceCodeIterator.iterateNamespaces(ElementImpl.this);
            public boolean hasNext() {
                return (pool == null || iter.hasNext());
            }
            public Object next() {
                if (pool == null) {
                    pool = getNamePool();
                    return "xml";
                } else {
                    return pool.getPrefixFromNamespaceCode(iter.next());
                }
            }
            public void remove() {
                throw new UnsupportedOperationException("remove");
            }
        };
    }

    /**
    * Search the NamespaceList for a given prefix, returning the corresponding URI.
    * @param prefix The prefix to be matched. To find the default namespace, supply ""
    * @return The URI code corresponding to this namespace. If it is an unnamed default namespace,
    * return Namespace.NULL_CODE.
    * @throws org.orbeon.saxon.om.NamespaceException if the prefix has not been declared on this NamespaceList.
    */

    public short getURICodeForPrefix(String prefix) throws NamespaceException {
        if (prefix.equals("xml")) return NamespaceConstant.XML_CODE;

		NamePool pool = getNamePool();
		int prefixCode = pool.getCodeForPrefix(prefix);
		if (prefixCode==-1) {
		    throw new NamespaceException(prefix);
		}
		short uc = getURICodeForPrefixCode(prefixCode);
        if (uc == -1) {
            throw new NamespaceException(getNamePool().getPrefixFromNamespaceCode(prefixCode<<16));
        }
        return uc;
    }

    /**
     * Get the URI bound to a given prefix in the in-scope namespaces of this element
     * @param prefixCode the prefix code as a 16-bit integer
     * @return the uri code as a 16-bit integer, or -1 if there is no in-scope binding for this prefix
     */

    protected short getURICodeForPrefixCode(int prefixCode) {
        if (namespaceList!=null) {
            for (int i=0; i<namespaceList.length; i++) {
                if ((namespaceList[i]>>16) == prefixCode) {
                    short u = (short)(namespaceList[i] & 0xffff);
                    return (u==0 && prefixCode!=0 ? (short)-1 : u);
                }
            }
        }
        NodeInfo next = parent;
        if (next.getNodeKind()==Type.DOCUMENT) {
            // prefixCode==0 represents the empty namespace prefix ""
            if (prefixCode==0) {
                return NamespaceConstant.NULL_CODE;
            }
            return -1;
        } else {
            return ((ElementImpl)next).getURICodeForPrefixCode(prefixCode);
        }
	}

    /**
    * Search the NamespaceList for a given URI, returning the corresponding prefix.
    * @param uri The URI to be matched.
    * @return The prefix corresponding to this URI. If not found, return null. If there is
    * more than one prefix matching the URI, the first one found is returned. If the URI matches
    * the default namespace, return an empty string.
    */

    public String getPrefixForURI(String uri) {
        if (uri.equals(NamespaceConstant.XML)) return "xml";

		NamePool pool = getNamePool();
		int uriCode = pool.getCodeForURI(uri);
		if (uriCode<0) return null;
		return getPrefixForURICode(uriCode);
	}

    private String getPrefixForURICode(int code) {
        if (namespaceList!=null) {
            for (int i=0; i<namespaceList.length; i++) {
                if ((namespaceList[i] & 0xffff) == code) {
                    return getNamePool().getPrefixFromNamespaceCode(namespaceList[i]);
                }
            }
        }
        NodeInfo next = parent;
        if (next instanceof DocumentInfo) {
            return null;
        } else {
            return ((ElementImpl)next).getPrefixForURICode(code);
        }
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
        return (namespaceList == null ? IntArraySet.EMPTY_INT_ARRAY : namespaceList);
    }

    /**
     * Get the list of in-scope namespaces for this element as an array of
     * namespace codes. (Used by LiteralResultElement)
     * @return the list of namespaces
    */

    public int[] getInScopeNamespaceCodes() {
    	return NamespaceIterator.getInScopeNamespaceCodes(this);
    }

    /**
     * Ensure that a child element being inserted into a tree has the right namespace declarations.
     * Redundant declarations should be removed. If the child is in the null namespace but the parent has a default
     * namespace, xmlns="" should be added. If inherit is false, namespace undeclarations should be added for all
     * namespaces that are declared on the parent but not on the child.
     * @param inherit true if the child is to inherit the inscope namespaces of its new parent
     */

    protected void fixupInsertedNamespaces(boolean inherit) {
        if (parent.getNodeKind() == Type.DOCUMENT) {
            return;
        }

        IntSet childNamespaces = new IntHashSet();
        if (namespaceList != null) {
            for (int i=0; i<namespaceList.length; i++) {
                childNamespaces.add(namespaceList[i]);
            }
        }

        NamespaceResolver inscope = new InscopeNamespaceResolver(parent);
        NamePool pool = getNamePool();

        // If the child is in the null namespace but the parent has a default namespace, xmlns="" should be added.

        if (getURI().length()==0 && inscope.getURIForPrefix("", true).length()!=0) {
            childNamespaces.add(0);
        }

        // Namespaces present on the parent but not on the child should be undeclared (if requested)

        if (!inherit) {
            Iterator it = inscope.iteratePrefixes();
            while (it.hasNext()) {
                String prefix = (String)it.next();
                int prefixCode = pool.getCodeForPrefix(prefix)<<16;
                boolean found = false;
                if (namespaceList != null) {
                    for (int i=0; i<namespaceList.length; i++) {
                        if ((namespaceList[i] & 0xffff) == prefixCode) {
                            found = true;
                            break;
                        }
                    }
                }
                if (!found) {
                    childNamespaces.add(prefixCode);
                }
            }
        }

        // Redundant namespaces should be removed

        if (namespaceList != null) {
            for (int i=0; i<namespaceList.length; i++) {
                int nscode = namespaceList[i];
                String prefix = pool.getPrefixFromNamespaceCode(nscode);
                String uri = pool.getURIFromNamespaceCode(nscode);
                String parentUri = inscope.getURIForPrefix(prefix, true);
                if (parentUri != null && parentUri.equals(uri)) {
                    // the namespace declaration is redundant
                    childNamespaces.remove(nscode);
                }
            }
        }
        int[] n2 = new int[childNamespaces.size()];
        int j = 0;
        IntIterator ii = childNamespaces.iterator();
        while (ii.hasNext()) {
            n2[j++] = ii.next();
        }
        namespaceList = n2;
    }

    /**
    * Get the attribute list for this element.
    * @return The attribute list. This will not include any
    * namespace attributes. The attribute names will be in expanded form, with prefixes
    * replaced by URIs
    */

    public AttributeCollection getAttributeList() {
        return (attributeList == null ? emptyAtts : attributeList);
    }

    /**
    * Get the value of a given attribute of this node
    * @param fingerprint The fingerprint of the attribute name
    * @return the attribute value if it exists or null if not
    */

    public String getAttributeValue(int fingerprint) {
    	return (attributeList == null ? null : attributeList.getValueByFingerprint(fingerprint));
    }

    /**
     * Get the value of a given attribute of this node
     * @param uri the namespace URI of the attribute name, or "" if the attribute is not in a namepsace
     * @param localName the local part of the attribute name
    *  @return the attribute value if it exists or null if not
    */

    public String getAttributeValue(String uri, String localName) {
    	return (attributeList == null ? null : attributeList.getValue(uri, localName));
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
