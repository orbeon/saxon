package net.sf.saxon.tree;
import net.sf.saxon.Configuration;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.om.*;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.Type;

import java.util.ArrayList;
import java.util.HashMap;

/**
  * A node in the XML parse tree representing the Document itself (or equivalently, the root
  * node of the Document).<P>
  * @author Michael H. Kay
  */

public final class DocumentImpl extends ParentNodeImpl
    implements DocumentInfo {

    //private static int nextDocumentNumber = 0;

    private ElementImpl documentElement;

    private HashMap idTable = null;
    private int documentNumber;
    private HashMap entityTable = null;
    private HashMap elementList = null;
    //private StringBuffer characterBuffer;
    private Configuration config;
    private LineNumberMap lineNumberMap;
    private SystemIdMap systemIdMap = new SystemIdMap();

    public DocumentImpl() {
        parent = null;
    }

	/**
	* Set the Configuration that contains this document
	*/

	public void setConfiguration(Configuration config) {
		this.config = config;
		documentNumber = config.getNamePool().allocateDocumentNumber(this);
	}

    /**
     * Get the configuration previously set using setConfiguration
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
    * Set the top-level element of the document (variously called the root element or the
    * document element). Note that a DocumentImpl may represent the root of a result tree
    * fragment, in which case there is no document element.
    * @param e the top-level element
    */

    void setDocumentElement(ElementImpl e) {
        documentElement = e;
    }

    /**
    * Set the system id of this node
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
    * Get the base URI of this root node. For a root node the base URI is the same as the
    * System ID.
    */

    public String getBaseURI() {
        return getSystemId();
    }

    /**
    * Set the system id of an element in the document
    */

    void setSystemId(int seq, String uri) {
        if (uri==null) {
            uri = "";
        }
        systemIdMap.setSystemId(seq, uri);
    }


    /**
    * Get the system id of an element in the document
    */

    String getSystemId(int seq) {
        return systemIdMap.getSystemId(seq);
    }


    /**
    * Set line numbering on
    */

    public void setLineNumbering() {
        lineNumberMap = new LineNumberMap();
        lineNumberMap.setLineNumber(sequence, 0);
    }

    /**
    * Set the line number for an element. Ignored if line numbering is off.
    */

    void setLineNumber(int sequence, int line) {
        if (lineNumberMap != null) {
            lineNumberMap.setLineNumber(sequence, line);
        }
    }

    /**
    * Get the line number for an element. Return -1 if line numbering is off.
    */

    int getLineNumber(int sequence) {
        if (lineNumberMap != null) {
            return lineNumberMap.getLineNumber(sequence);
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
    * Get a character string that uniquely identifies this node
    * @return a string based on the document number
    */

    public String generateId() {
        return "d"+documentNumber;
    }

    /**
    * Get a list of all elements with a given name fingerprint
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
        return new ListIterator(list);
    }

    /**
    * Index all the ID attributes. This is done the first time the id() function
    * is used on this document
    */

    private void indexIDs() {
        if (idTable!=null) return;      // ID's are already indexed
        idTable = new HashMap(256);

        NodeImpl curr = this;
        NodeImpl root = curr;
        while(curr!=null) {
            if (curr.getNodeKind()==Type.ELEMENT) {
                ElementImpl e = (ElementImpl)curr;
                AttributeCollection atts = e.getAttributeList();
                for (int i=0; i<atts.getLength(); i++) {
                    if (atts.isId(i) && XMLChar.isValidNCName(atts.getValue(i).trim())) {
                        // don't index any invalid IDs - these can arise when using a non-validating parser
                        registerID(e, atts.getValue(i).trim());
                    }
                }
            }
            curr = curr.getNextInDocument(root);
        }
    }

    /**
    * Register a unique element ID. Fails if there is already an element with that ID.
    * @param e The Element having a particular unique ID value
    * @param id The unique ID value
    */

    private void registerID(NodeInfo e, String id) {
        // the XPath spec (5.2.1) says ignore the second ID if it's not unique
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
    * Set an unparsed entity URI associated with this document. For system use only, while
    * building the document.
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
