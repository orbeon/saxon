package net.sf.saxon.tree;
import net.sf.saxon.event.LocationCopier;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.om.*;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.Type;
import net.sf.saxon.pattern.AnyNodeTest;

import java.util.*;

/**
  * A node in the XML parse tree representing an XML element.<P>
  * This class is an implementation of NodeInfo
  * @author Michael H. Kay
  * @version 8 August 2000: separated from ElementImpl
  */

// The name of the element and its attributes are now namespace-resolved by the
// parser. However, this class retains the ability to do namespace resolution for other
// names, for example variable and template names in a stylesheet.

public class ElementWithAttributes extends ElementImpl {

    protected AttributeCollection attributeList;      // this excludes namespace attributes
    protected int[] namespaceList = null;             // list of namespace codes
            // note that this namespace list includes only the namespaces actually defined on
            // this element, not those inherited from outer elements.


    /**
    * Initialise a new ElementWithAttributes with an element name and attribute list
    * @param nameCode The element name, with namespaces resolved
    * @param atts The attribute list, after namespace processing
    * @param parent The parent node
    */

    public void initialise(int nameCode, AttributeCollectionImpl atts, NodeInfo parent,
                            String baseURI, int lineNumber, int sequenceNumber) {
        this.nameCode = nameCode;
        this.attributeList = atts;
        this.parent = (ParentNodeImpl)parent;
        this.sequence = sequenceNumber;
        this.root = (DocumentImpl)parent.getDocumentRoot();
        root.setLineNumber(sequenceNumber, lineNumber);
        root.setSystemId(sequenceNumber, baseURI);
    }

    /**
    * Set the namespace declarations for the element
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
        if (prefix.equals("") && !useDefault) {
            return "";
        }

		NamePool pool = getNamePool();
		int prefixCode = pool.getCodeForPrefix(prefix);
		if (prefixCode==-1) {
		    return null;
		}
        try {
            short uriCode = getURICodeForPrefixCode(prefixCode);
            return pool.getURIFromURICode(uriCode);
        } catch (NamespaceException e) {
            return null;
        }
    }

    /**
     * Get an iterator over all the prefixes declared in this namespace context. This will include
     * the default namespace (prefix="") and the XML namespace where appropriate
     */

    public Iterator iteratePrefixes() {
        Set inScope = new HashSet(10);
        Set outOfScope = new HashSet(10);
        inScope.add("");
        inScope.add("xml");
        gatherNamespacePrefixes(getNamePool(), inScope, outOfScope);
        return inScope.iterator();
    }

    /**
    * Search the NamespaceList for a given prefix, returning the corresponding URI.
    * @param prefix The prefix to be matched. To find the default namespace, supply ""
    * @return The URI code corresponding to this namespace. If it is an unnamed default namespace,
    * return Namespace.NULL_CODE.
    * @throws NamespaceException if the prefix has not been declared on this NamespaceList.
    */

    public short getURICodeForPrefix(String prefix) throws NamespaceException {
        if (prefix.equals("xml")) return NamespaceConstant.XML_CODE;

		NamePool pool = getNamePool();
		int prefixCode = pool.getCodeForPrefix(prefix);
		if (prefixCode==-1) {
		    throw new NamespaceException(prefix);
		}
		return getURICodeForPrefixCode(prefixCode);
    }

    private short getURICodeForPrefixCode(int prefixCode) throws NamespaceException {
        if (namespaceList!=null) {
            for (int i=0; i<namespaceList.length; i++) {
                if ((namespaceList[i]>>16) == prefixCode) {
                    return (short)(namespaceList[i] & 0xffff);
                }
            }
        }
        NodeInfo next = parent;
        while (true) {
	        if (next.getNodeKind()==Type.DOCUMENT) {
	        	// prefixCode==0 represents the empty namespace prefix ""
	            if (prefixCode==0) return NamespaceConstant.NULL_CODE;
	            throw new NamespaceException(getNamePool().getPrefixFromNamespaceCode(prefixCode<<16));
	        } else if (next instanceof ElementWithAttributes) {
	            return ((ElementWithAttributes)next).getURICodeForPrefixCode(prefixCode);
	        } else {
	        	next = next.getParent();
	        }
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
        while (true) {
	        if (next instanceof DocumentInfo) {
	            return null;
	        } else if (next instanceof ElementWithAttributes) {
	            return ((ElementWithAttributes)next).getPrefixForURICode(code);
	        } else {
	        	next = next.getParent();
	        }
	    }
    }

    private void gatherNamespacePrefixes(NamePool pool, Set inScope, Set outOfScope) {
        if (namespaceList!=null) {
            for (int i=0; i<namespaceList.length; i++) {
            	int nscode = namespaceList[i];
                String prefix = pool.getPrefixFromNamespaceCode(nscode);
                if ((nscode & 0xffff) == 0) {
                    // this is an undeclaration
                    outOfScope.add(prefix);
                } else if (!outOfScope.contains(prefix)) {
                    inScope.add(prefix);
                    outOfScope.add(prefix);
                }
            }
        }

        // now add the namespaces defined on the ancestor nodes

        NodeInfo parent = getParent();
        while (parent != null) {
            if (parent instanceof ElementWithAttributes) {
                ((ElementWithAttributes)parent).gatherNamespacePrefixes(pool, inScope, outOfScope);
            }
        }
    }


    /**
    * Output all namespace nodes associated with this element.
    * @param out The relevant outputter
     */

    public void sendNamespaceDeclarations(Receiver out, boolean includeAncestors) throws XPathException {

        if (namespaceList!=null) {
            for (int i=0; i<namespaceList.length; i++) {
                out.namespace(namespaceList[i], 0);
            }
        }

        // now add the namespaces defined on the ancestor nodes. We rely on the outputter
        // to eliminate multiple declarations of the same prefix

        if (includeAncestors) {
            if (parent.getNodeKind()!=Type.DOCUMENT) {
                parent.sendNamespaceDeclarations(out, true);
            }
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
        return namespaceList;
    }

    /**
    * Get the list of in-scope namespaces for this element as an array of
    * namespace codes. (Used by LiteralResultElement)
    */

    public int[] getInScopeNamespaceCodes() {
    	return new NamespaceIterator(this, null).getInScopeNamespaceCodes();
    }

    /**
    * Get the attribute list for this element.
    * @return The attribute list. This will not include any
    * namespace attributes. The attribute names will be in expanded form, with prefixes
    * replaced by URIs
    */

    public AttributeCollection getAttributeList() {
        return attributeList;
    }

    /**
    * Get the value of a given attribute of this node
    * @param fingerprint The fingerprint of the attribute name
    * @return the attribute value if it exists or null if not
    */

    public String getAttributeValue(int fingerprint) {
    	return attributeList.getValueByFingerprint(fingerprint);
    }

    /**
    * Copy this node to a given outputter (supporting xsl:copy-of)
    * @param out The outputter
    * @param whichNamespaces indicates which namespaces should be output: all, none, or local
    * namespaces only (those not declared on the parent element)
    */

    public void copy(Receiver out, int whichNamespaces, boolean copyAnnotations, int locationId) throws XPathException {

        int typeCode = (copyAnnotations ? getTypeAnnotation() : -1);
        if (locationId == 0 && out instanceof LocationCopier) {
            out.setSystemId(getSystemId());
            ((LocationCopier)out).setLineNumber(getLineNumber());
        }
        out.startElement(getNameCode(), typeCode, locationId, 0);

        // output the namespaces

        if (whichNamespaces != NO_NAMESPACES) {
            sendNamespaceDeclarations(out, whichNamespaces==ALL_NAMESPACES);
        }

        // output the attributes

        for (int i=0; i<attributeList.getLength(); i++) {
            out.attribute(attributeList.getNameCode(i), -1,
                               attributeList.getValue(i), 0, 0);
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
