package net.sf.saxon.dom;
import net.sf.saxon.event.PipelineConfiguration;
import net.sf.saxon.event.Receiver;
import net.sf.saxon.event.SaxonLocator;
import net.sf.saxon.om.Name;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;
import org.w3c.dom.*;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.NamespaceSupport;

import java.util.HashMap;
import java.util.Iterator;

/**
* DOMSender.java: pseudo-SAX driver for a DOM source document.
* This class takes an existing
* DOM Document and walks around it in a depth-first traversal,
* calling a Receiver to process the nodes as it does so
*/

public class DOMSender implements SaxonLocator {
    private Receiver receiver;
    private PipelineConfiguration pipe;

    private NamespaceSupport nsSupport = new NamespaceSupport();
    private AttributesImpl attlist = new AttributesImpl();
    private String[] parts = new String[3];
    private String[] elparts = new String[3];
    private HashMap nsDeclarations = new HashMap(10);
    protected Node root = null;
    protected String systemId;

    /**
     * Set the pipeline configuration
     */

    public void setPipelineConfiguration(PipelineConfiguration pipe) {
        this.pipe = pipe;
    }

    /**
    * Set the receiver.
    * @param receiver The object to receive content events.
    */

    public void setReceiver (Receiver receiver) {
        this.receiver = receiver;
    }

    /**
    * Set the DOM Document that will be walked
    */

    public void setStartNode(Node start) {
        root = start;
    }

    /**
    * Set the systemId of the source document (which will also be
    * used for the destination)
    */

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    /**
    * Walk a document (traversing the nodes depth first)
    * @exception net.sf.saxon.trans.XPathException On any error in the document
    */

    public void send() throws XPathException {
        if (root==null) {
            throw new DynamicError("DOMSender: no start node defined");
        }
        if (receiver==null) {
            throw new DynamicError("DOMSender: no receiver defined");
        }

        receiver.setSystemId(systemId);
        pipe.setLocationProvider(this);
        receiver.setPipelineConfiguration(pipe);

        receiver.open();
        if (root instanceof Element) {
            sendElement((Element)root);
        } else {
            // walk the root node
            receiver.startDocument(0);
            walkNode(root);
            receiver.endDocument();
        }
        receiver.close();
    }

    /**
     * Walk a document starting from a particular element node. This has to make
     * sure that all the namespace declarations in scope for the element are
     * treated as if they were namespace declarations on the element itself.
     */

    private void sendElement(Element startNode) throws XPathException {
        Element node = startNode;
        NamedNodeMap topAtts = gatherNamespaces(node, false);
        while (true) {
            gatherNamespaces(node, true);
            Node parent = node.getParentNode();
            if (parent instanceof Element) {
                node = (Element)parent;
            } else {
                break;
            }
        }
        outputElement(startNode, topAtts);
    }

  /**
    * Walk an element of a document (traversing the children depth first)
    * @param node The DOM Element object to walk
    * @exception net.sf.saxon.trans.XPathException On any error in the document
    *
    */

    private void walkNode (Node node) throws XPathException {
        if (node.hasChildNodes()) {
            NodeList nit = node.getChildNodes();
            for (int i=0; i<nit.getLength(); i++) {
                Node child = nit.item(i);
                switch (child.getNodeType()) {
                    case Node.DOCUMENT_NODE:
                        break;                  // should not happen
                    case Node.ELEMENT_NODE:
                        Element element = (Element)child;
                        NamedNodeMap atts = gatherNamespaces(element, false);

                        outputElement(element, atts);

                        nsSupport.popContext();
                        break;
                    case Node.ATTRIBUTE_NODE:        // have already dealt with attributes
                        break;
                    case Node.PROCESSING_INSTRUCTION_NODE:
                        receiver.processingInstruction(
                            ((ProcessingInstruction)child).getTarget(),
                            ((ProcessingInstruction)child).getData(),
                                0, 0);
                        break;
                    case Node.COMMENT_NODE: {
                        String text = ((Comment)child).getData();
                        if (text!=null) {
                            receiver.comment(text, 0, 0);
                        }
                        break;
                    }
                    case Node.TEXT_NODE:
                    case Node.CDATA_SECTION_NODE: {
                        String text = ((CharacterData)child).getData();
                        if (text!=null) {
                            receiver.characters(text, 0, 0);
                        }
                        break;
                    }
                    case Node.ENTITY_REFERENCE_NODE:
                        walkNode(child);
                        break;
                    default:
                        break;                  // should not happen
                }
            }
        }

    }

    private void outputElement(Element element, NamedNodeMap atts) throws XPathException {
        String[] elparts2 = nsSupport.processName(element.getTagName(), elparts, false);
        if (elparts2==null) {
              throw new DynamicError("Undeclared namespace in " + element.getTagName());
        }
        String uri = elparts2[0];
        String local = elparts2[1];
        String prefix = Name.getPrefix(elparts2[2]);

        NamePool namePool = pipe.getConfiguration().getNamePool();
        int nameCode = namePool.allocate(prefix, uri, local);

        receiver.startElement(nameCode, -1, 0, 0);
        for (Iterator iter = nsDeclarations.keySet().iterator(); iter.hasNext();) {
            String nsprefix = (String)iter.next();
            String nsuri = (String)nsDeclarations.get(nsprefix);
            receiver.namespace(namePool.allocateNamespaceCode(nsprefix, nsuri), 0);
        }

        if (atts != null) {
            for (int a2=0; a2<atts.getLength(); a2++) {
                Attr att = (Attr)atts.item(a2);
                String attname = att.getName();
                if (!attname.equals("xmlns") && !attname.startsWith("xmlns:")) {
                    //System.err.println("Processing attribute " + attname);
                    String[] parts2 = nsSupport.processName(attname, parts, true);
                    if (parts2==null) {
                          throw new DynamicError("Undeclared namespace in " + attname);
                    }
                    String atturi = parts2[0];
                    String attlocal = parts2[1];
                    String attprefix = Name.getPrefix(parts2[2]);

                    int attCode = namePool.allocate(attprefix, atturi, attlocal);

                    receiver.attribute(attCode, -1, att.getValue(), 0, 0);
                }
            }
        }
        receiver.startContent();

        walkNode(element);

        receiver.endElement();
    }

    /**
     * Collect all the namespace attributes in scope for a given element
     * @param element
     * @param cumulative
     * @return
     */

    private NamedNodeMap gatherNamespaces(Element element, boolean cumulative) {
        if (!cumulative) {
            nsSupport.pushContext();
            attlist.clear();
            nsDeclarations.clear();
        }

        // we can't rely on namespace declaration attributes being present -
        // there may be undeclared namespace prefixes. (If the DOM is a Saxon
        // tree, there will be no namespace declaration attributes.) So we
        // declare all namespaces encountered, to be on the safe side.

        try {
            String prefix = element.getPrefix();
            String uri = element.getNamespaceURI();
            if (prefix==null) prefix="";
            if (uri==null) uri="";
            //System.err.println("Implicit Namespace: " + prefix + "=" + uri);
            if (nsDeclarations.get(prefix)==null) {
                nsSupport.declarePrefix(prefix, uri);
                nsDeclarations.put(prefix, uri);
            }
        } catch (Throwable err) {
            // it must be a level 1 DOM
        }

        NamedNodeMap atts = element.getAttributes();

        // Apparently the Oracle DOM returns null if there are no attributes:
        if (atts == null) {
            return null;
        }
        for (int a1=0; a1<atts.getLength(); a1++) {
            Attr att = (Attr)atts.item(a1);
            String attname = att.getName();
            if (attname.equals("xmlns")) {
                //System.err.println("Default namespace: " + att.getValue());
                if (nsDeclarations.get("")==null) {
                    String uri = att.getValue();
                    nsSupport.declarePrefix("", uri);
                    nsDeclarations.put("", uri);
                }
            } else if (attname.startsWith("xmlns:")) {
                //System.err.println("Namespace: " + attname.substring(6) + "=" + att.getValue());
                String prefix = attname.substring(6);
                if (nsDeclarations.get(prefix)==null) {
                    String uri = att.getValue();
                    nsSupport.declarePrefix(prefix, uri);
                    nsDeclarations.put(prefix, uri);
                }
            } else if (attname.indexOf(':')>=0) {
                try {
                    String prefix = att.getPrefix();
                    String uri = att.getNamespaceURI();
                    //System.err.println("Implicit Namespace: " + prefix + "=" + uri);
                    if (nsDeclarations.get(prefix)==null) {
                        nsSupport.declarePrefix(prefix, uri);
                        //contentHandler.startPrefixMapping(prefix, uri);
                        nsDeclarations.put(prefix, uri);
                    }
                } catch (Throwable err) {
                    // it must be a level 1 DOM
                }
            }
        }
        return atts;
    }

    // Implement the SAX Locator interface. This is needed to pass the base URI of nodes
    // to the receiver. We don't attempt to preserve the original base URI of each individual
    // node as it is copied, only the base URI of the document as a whole.

	public int getColumnNumber() {
		return -1;
	}

	public int getLineNumber() {
		return -1;
	}

	public String getPublicId() {
		return null;
	}

	public String getSystemId() {
		return systemId;
	}

    public String getSystemId(int locationId) {
        return getSystemId();
    }

    public int getLineNumber(int locationId) {
        return getLineNumber();
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
