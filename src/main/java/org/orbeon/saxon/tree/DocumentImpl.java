package org.orbeon.saxon.tree;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.value.Whitespace;
import org.orbeon.saxon.event.Receiver;
import org.orbeon.saxon.event.Builder;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Collections;

/**
  * A node in the XML parse tree representing the Document itself (or equivalently, the root
  * node of the Document).
 *
 * <p>A DocumentImpl object may either represent a real document node, or it may represent an imaginary
 * container for a parentless element.</p>
  * @author Michael H. Kay
  */

public final class DocumentImpl extends ParentNodeImpl implements DocumentInfo {

    //private static int nextDocumentNumber = 0;

    private ElementImpl documentElement;

    private HashMap idTable = null;
    private int documentNumber;
    private String baseURI;
    private HashMap entityTable = null;
    private HashMap elementList = null;
    //private StringBuffer characterBuffer;
    private Configuration config;
    private LineNumberMap lineNumberMap;
    private SystemIdMap systemIdMap = new SystemIdMap();
    private boolean imaginary = false;

    /**
     * Create a DocumentImpl
     */

    public DocumentImpl() {
        parent = null;
    }

	/**
	 * Set the Configuration that contains this document
     * @param config the Saxon configuration
	*/

	public void setConfiguration(Configuration config) {
		this.config = config;
		documentNumber = config.getDocumentNumberAllocator().allocateDocumentNumber();
	}

    /**
     * Get the configuration previously set using setConfiguration
     * @return the Saxon configuration
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
     * Get a Builder suitable for building nodes that can be attached to this document.
     * @return a new TreeBuilder
     */

    public Builder newBuilder() {
        TreeBuilder builder = new TreeBuilder();
        builder.setAllocateSequenceNumbers(false);
        return builder;
    }

    /**
     * Set whether this is an imaginary document node
     * @param imaginary if true, this is an imaginary node - the tree is really rooted at the topmost element
     */

    public void setImaginary(boolean imaginary) {
        this.imaginary = imaginary;
    }

    /**
     * Ask whether this is an imaginary document node
     * @return true if this is an imaginary node - the tree is really rooted at the topmost element
     */

    public boolean isImaginary() {
        return imaginary;
    }

    /**
	* Get the unique document number
	*/

	public int getDocumentNumber() {
	    return documentNumber;
	}

    /**
    * Set the top-level element of the document (variously called the root element or the
    * document element). Note that a DocumentImpl may represent the root of a result tree
    * fragment, in which case there is no document element.
    * @param e the top-level element
    */

    void setDocumentElement(ElementImpl e) {
        documentElement = e;
    }

    /**
     * Copy the system ID and line number map from another document
     * (used when grafting a simplified stylesheet)
     * @param original the document whose system ID and line number maps are to be grafted
     * onto this tree
     */

    public void graftLocationMap(DocumentImpl original) {
        systemIdMap = original.systemIdMap;
        lineNumberMap = original.lineNumberMap;
    }

    /**
    * Set the system id (base URI) of this node
    */

    public void setSystemId(String uri) {
        if (uri==null) {
            uri = "";
        }
        systemIdMap.setSystemId(sequence, uri);
    }

    /**
    * Get the system id of this root node
    */

    public String getSystemId() {
        return systemIdMap.getSystemId(sequence);
    }

    /**
     * Set the base URI of this document node
     * @param uri the new base URI
     */

    public void setBaseURI(String uri) {
        baseURI = uri;
    }

    /**
    * Get the base URI of this root node.
     * @return the base URI
    */

    public String getBaseURI() {
        if (baseURI != null) {
            return baseURI;
        }
        return getSystemId();
    }


    /**
    * Set the system id of an element in the document
     * @param seq the sequence number of the element
     * @param uri the system identifier (base URI) of the element
    */

    void setSystemId(int seq, String uri) {
        if (uri==null) {
            uri = "";
        }
        systemIdMap.setSystemId(seq, uri);
    }


    /**
    * Get the system id of an element in the document
     * @param seq the sequence number of the element
     * @return the systemId (base URI) of the element
    */

    String getSystemId(int seq) {
        return systemIdMap.getSystemId(seq);
    }


    /**
    * Set line numbering on
    */

    public void setLineNumbering() {
        lineNumberMap = new LineNumberMap();
        lineNumberMap.setLineAndColumn(sequence, 0, -1);
    }

    /**
     * Set the line number for an element. Ignored if line numbering is off.
     * @param sequence the sequence number of the element
     * @param line the line number of the element
     * @param column the column number of the element
    */

    void setLineAndColumn(int sequence, int line, int column) {
        if (lineNumberMap != null && sequence >= 0) {
            lineNumberMap.setLineAndColumn(sequence, line, column);
        }
    }

    /**
     * Get the line number for an element.
     * @param sequence the sequence number of the element
     * @return the line number for an element. Return -1 if line numbering is off, or if
     * the element was added subsequent to document creation by use of XQuery update
    */

    int getLineNumber(int sequence) {
        if (lineNumberMap != null && sequence >= 0) {
            return lineNumberMap.getLineNumber(sequence);
        }
        return -1;
    }

    /**
    * Get the column number for an element.
     * @param sequence the sequence number of the element
     * @return the column number for an element. Return -1 if line numbering is off, or if
     * the element was added subsequent to document creation by use of XQuery update
    */

    int getColumnNumber(int sequence) {
        if (lineNumberMap != null && sequence >= 0) {
            return lineNumberMap.getColumnNumber(sequence);
        }
        return -1;
    }


    /**
    * Get the line number of this root node.
    * @return 0 always
    */

    public int getLineNumber() {
        return 0;
    }

    /**
    * Return the type of node.
    * @return Type.DOCUMENT (always)
    */

    public final int getNodeKind() {
        return Type.DOCUMENT;
    }

    /**
    * Get next sibling - always null
    * @return null
    */

    public final NodeInfo getNextSibling() {
        return null;
    }

    /**
    * Get previous sibling - always null
    * @return null
    */

    public final NodeInfo getPreviousSibling()  {
        return null;
    }

    /**
     * Get the root (outermost) element.
     * @return the Element node for the outermost element of the document.
     */

    public ElementImpl getDocumentElement() {
        return documentElement;
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
    * @return the DocumentInfo representing this document
    */

    public DocumentInfo getDocumentRoot() {
        return this;
    }

    /**
     * Get the physical root of the tree. This may be an imaginary document node: this method
     * should be used only when control information held at the physical root is required
     * @return the document node, which may be imaginary
     */

    public DocumentImpl getPhysicalRoot() {
        return this;
    }

    /**
     * Get a character string that uniquely identifies this node
     *  @param buffer a buffer into which will be placed a string based on the document number
     *
     */

    public void generateId(FastStringBuffer buffer) {
        buffer.append('d');
        buffer.append(Integer.toString(documentNumber));
    }

    /**
     * Get a list of all elements with a given name fingerprint
     * @param fingerprint the fingerprint of the required element name
     * @return an iterator over all the elements with this name
    */

    AxisIterator getAllElements(int fingerprint) {
        Integer elkey = new Integer(fingerprint);
        if (elementList==null) {
            elementList = new HashMap(500);
        }
        ArrayList list = (ArrayList)elementList.get(elkey);
        if (list==null) {
            list = new ArrayList(500);
            NodeImpl next = getNextInDocument(this);
            while (next!=null) {
                if (next.getNodeKind()==Type.ELEMENT &&
                        next.getFingerprint() == fingerprint) {
                    list.add(next);
                }
                next = next.getNextInDocument(this);
            }
            elementList.put(elkey, list);
        }
        return new NodeListIterator(list);
    }

    /**
     * Remove a node from any indexes when it is detached from the tree
     * @param node the node to be removed from all indexes
     */

    public void deIndex(NodeImpl node) {
        // TODO: remove from xsl:key indexes (can exist in XQuery as a result of optimization!)
        if (node instanceof ElementImpl) {
            if (elementList!=null) {
                Integer elkey = new Integer(node.getFingerprint());
                ArrayList list = (ArrayList)elementList.get(elkey);
                if (list==null) {
                    return;
                }
                list.remove(node);
            }
            if (node.isId()) {
                deregisterID(node.getStringValue());
            }
        } else if (node instanceof AttributeImpl) {
            if (node.isId()) {
                deregisterID(node.getStringValue());
            }
        }
    }

    /**
    * Index all the ID attributes. This is done the first time the id() function
    * is used on this document, or the first time that id() is called after a sequence of updates
    */

    private void indexIDs() {
        if (idTable!=null) {
            return;      // ID's are already indexed
        }
        idTable = new HashMap(256);
        NameChecker checker = getConfiguration().getNameChecker();

        NodeImpl curr = this;
        NodeImpl root = curr;
        while(curr!=null) {
            if (curr.getNodeKind()==Type.ELEMENT) {
                //noinspection ConstantConditions
                ElementImpl e = (ElementImpl)curr;
                AttributeCollection atts = e.getAttributeList();
                for (int i=0; i<atts.getLength(); i++) {
                    if (atts.isId(i) && checker.isValidNCName(Whitespace.trim(atts.getValue(i)))) {
                        // don't index any invalid IDs - these can arise when using a non-validating parser
                        registerID(e, Whitespace.trim(atts.getValue(i)));
                    }
                }
            }
            curr = curr.getNextInDocument(root);
        }
    }

    /**
    * Register a unique element ID. Does nothing if there is already an element with that ID.
    * @param e The Element having a particular unique ID value
    * @param id The unique ID value
    */

    protected void registerID(NodeInfo e, String id) {
        // the XPath spec (5.2.1) says ignore the second ID if it's not unique
        if (idTable == null) {
            idTable = new HashMap(256);
        }
        Object old = idTable.get(id);
        if (old==null) {
            idTable.put(id, e);
        }
    }

    /**
    * Get the element with a given ID.
    * @param id The unique ID of the required element, previously registered using registerID()
    * @return The NodeInfo for the given ID if one has been registered, otherwise null.
    */

    public NodeInfo selectID(String id) {
        if (idTable==null) indexIDs();
        return (NodeInfo)idTable.get(id);
    }

    /**
     * Remove the entry for a given ID (when nodes are deleted). Does nothing if the id value is not
     * present in the index.
     * @param id The id value
     */

    protected void deregisterID(String id) {
        id = Whitespace.trim(id);
        if (idTable != null) {
            idTable.remove(id);
        }
    }

    /**
     * Set an unparsed entity URI associated with this document. For system use only, while
     * building the document.
     * @param name the entity name
     * @param uri the system identifier of the unparsed entity
     * @param publicId the public identifier of the unparsed entity
    */

    void setUnparsedEntity(String name, String uri, String publicId) {
        // System.err.println("setUnparsedEntity( " + name + "," + uri + ")");
        if (entityTable==null) {
            entityTable = new HashMap(10);
        }
        String[] ids = new String[2];
        ids[0] = uri;
        ids[1] = publicId;
        entityTable.put(name, ids);
    }

    /**
     * Get the list of unparsed entities defined in this document
     * @return an Iterator, whose items are of type String, containing the names of all
     *         unparsed entities defined in this document. If there are no unparsed entities or if the
     *         information is not available then an empty iterator is returned
     */

    public Iterator getUnparsedEntityNames() {
        if (entityTable == null) {
            return Collections.EMPTY_LIST.iterator();
        } else {
            return entityTable.keySet().iterator();
        }
    }

    /**
    * Get the unparsed entity with a given name
    * @param name the name of the entity
    * @return if the entity exists, return an array of two Strings, the first holding the system ID
    * of the entity, the second holding the public ID if there is one, or null if not. If the entity
    * does not exist, return null.    * @return the URI of the entity if there is one, or empty string if not
    */

    public String[] getUnparsedEntity(String name) {
        if (entityTable==null) {
            return null;
        }
        return (String[])entityTable.get(name);
    }

    /**
    * Copy this node to a given outputter
    */

    public void copy(Receiver out, int whichNamespaces, boolean copyAnnotations, int locationId) throws XPathException {
        out.startDocument(0);
        NodeImpl next = (NodeImpl)getFirstChild();
        while (next!=null) {
            next.copy(out, whichNamespaces, copyAnnotations, locationId);
            next = (NodeImpl)next.getNextSibling();
        }
        out.endDocument();
    }


    /**
     * Replace the string-value of this node
     *
     * @param stringValue the new string value
     */

    public void replaceStringValue(CharSequence stringValue) {
        throw new UnsupportedOperationException("Cannot replace the value of a document node");
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
