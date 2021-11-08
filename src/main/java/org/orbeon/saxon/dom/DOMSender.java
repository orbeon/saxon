package org.orbeon.saxon.dom;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.event.PipelineConfiguration;
import org.orbeon.saxon.event.Receiver;
import org.orbeon.saxon.event.SaxonLocator;
import org.orbeon.saxon.event.SourceLocationProvider;
import org.orbeon.saxon.om.NameChecker;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.om.StandardNames;
import org.orbeon.saxon.trans.XPathException;
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

public class DOMSender implements SaxonLocator, SourceLocationProvider {
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
     * @param pipe the pipeline configuration
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
     * @param start the root node from which the tree walk will start
    */

    public void setStartNode(Node start) {
        root = start;
    }

    /**
     * Set the systemId of the source document (which will also be
     * used for the destination)
     * @param systemId the systemId of the source document
    */

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    /**
    * Walk a document (traversing the nodes depth first)
    * @exception org.orbeon.saxon.trans.XPathException On any error in the document
    */

    public void send() throws XPathException {
        if (root==null) {
            throw new XPathException("DOMSender: no start node defined");
        }
        if (receiver==null) {
            throw new XPathException("DOMSender: no receiver defined");
        }

        receiver.setSystemId(systemId);
        pipe.setLocationProvider(this);
        receiver.setPipelineConfiguration(pipe);

        receiver.open();
        switch (root.getNodeType()) {
            case Node.DOCUMENT_NODE:
            case Node.DOCUMENT_FRAGMENT_NODE:
                receiver.startDocument(0);
                walkNode(root);
                receiver.endDocument();
                break;
            case Node.ELEMENT_NODE:
                sendElement((Element)root);
                break;
            case Node.TEXT_NODE:
            case Node.CDATA_SECTION_NODE:
                receiver.characters(((CharacterData)root).getData(), 0, 0);
                break;
            case Node.COMMENT_NODE:
                receiver.comment(((Comment)root).getData(), 0, 0);
                break;
            case Node.PROCESSING_INSTRUCTION_NODE:
                receiver.processingInstruction(
                        ((ProcessingInstruction)root).getTarget(),
                        ((ProcessingInstruction)root).getData(), 0, 0);
                break;
            default:
                throw new XPathException("DOMSender: unsupported kind of start node (" + root.getNodeType() + ")");
        }
        receiver.close();
    }

    /**
     * Walk a document starting from a particular element node. This has to make
     * sure that all the namespace declarations in scope for the element are
     * treated as if they were namespace declarations on the element itself.
     * @param startNode the start element node from which the walk will start
     */

    private void sendElement(Element startNode) throws XPathException {
        Element node = startNode;
        NamedNodeMap topAtts = gatherNamespaces(node, false);
        while (true) {
            gatherNamespaces(node, true);
            Node parent = node.getParentNode();
            if (parent != null && parent.getNodeType() == Node.ELEMENT_NODE) {
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
    * @exception org.orbeon.saxon.trans.XPathException On any error in the document
    *
    */

    private void walkNode (Node node) throws XPathException {
        if (node.hasChildNodes()) {
            NodeList nit = node.getChildNodes();
            final int len = nit.getLength();
            for (int i=0; i<len; i++) {
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
        final Configuration config = pipe.getConfiguration();
        String[] elparts2 = nsSupport.processName(element.getTagName(), elparts, false);
        if (elparts2==null) {
              throw new XPathException("Undeclared namespace in " + element.getTagName());
        }
        String uri = elparts2[0];
        String local = elparts2[1];
        String prefix = NameChecker.getPrefix(elparts2[2]);

        NamePool namePool = config.getNamePool();
        int nameCode = namePool.allocate(prefix, uri, local);

        receiver.startElement(nameCode, StandardNames.XS_UNTYPED, 0, 0);
        for (Iterator iter = nsDeclarations.keySet().iterator(); iter.hasNext();) {
            String nsprefix = (String)iter.next();
            String nsuri = (String)nsDeclarations.get(nsprefix);
            receiver.namespace(namePool.allocateNamespaceCode(nsprefix, nsuri), 0);
        }

        if (atts != null) {
            final int len = atts.getLength();
            for (int a2=0; a2<len; a2++) {
                Attr att = (Attr)atts.item(a2);
                String attname = att.getName();
                if (attname.startsWith("xmlns") && (attname.equals("xmlns") || attname.startsWith("xmlns:"))) {
                    // do nothing
                } else {
                    //System.err.println("Processing attribute " + attname);
                    String[] parts2 = nsSupport.processName(attname, parts, true);
                    if (parts2==null) {
                          throw new XPathException("Undeclared namespace in " + attname);
                    }
                    String atturi = parts2[0];
                    String attlocal = parts2[1];
                    String attprefix = NameChecker.getPrefix(parts2[2]);

                    int attCode = namePool.allocate(attprefix, atturi, attlocal);

                    receiver.attribute(attCode, StandardNames.XS_UNTYPED_ATOMIC, att.getValue(), 0, 0);
                }
            }
        }
        receiver.startContent();

        walkNode(element);

        receiver.endElement();
    }

    /**
     * Collect all the namespace attributes in scope for a given element. The namespace
     * declaration attributes are added to the nsDeclarations map (which records namespaces
     * declared for this element only), and are stacked on the stack maintated by the nsSupport
     * object.
     * @param element The element whose namespace declarations are required
     * @param cumulative If true, the namespace declarations on this element are added to the
     * current context, without creating a new context. If false, a new namespace context is
     * created.
     * @return The NamedNodeMap representing the set of all attributes (ordinary attributes plus
     * namespace declarations) on this element.
     */

    private NamedNodeMap gatherNamespaces(Element element, boolean cumulative) {
        if (!cumulative) {
            nsSupport.pushContext();
            attlist.clear();
            nsDeclarations.clear();
        }

        // we can't rely on namespace declaration attributes being present -
        // there may be undeclared namespace prefixes. So we
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
        int alen = atts.getLength();
        for (int a1=0; a1<alen; a1++) {
            Attr att = (Attr)atts.item(a1);
            String attname = att.getName();
            if (attname.equals("xmlns")) {
                //System.err.println("Default namespace: " + att.getValue());
                String uri = att.getValue();
                if (nsDeclarations.get("")==null || !nsDeclarations.get("").equals(uri)) {
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

    public String getSystemId(long locationId) {
        return getSystemId();
    }

    public int getLineNumber(long locationId) {
        return getLineNumber();
    }

    public int getColumnNumber(long locationId) {
        return getColumnNumber();
    }

//    public static void main(String[] args) throws Exception {
//        Configuration config = new Configuration();
//        StaticQueryContext sqc = new StaticQueryContext(config);
//        DocumentInfo doc = sqc.buildDocument(new StreamSource("file:///MyJava/samples/styles/books.xsl"));
//        Document dom = (Document)NodeOverNodeInfo.wrap(doc);
//        DOMSender sender = new DOMSender();
//        final PipelineConfiguration pipe = config.makePipelineConfiguration();
//        sender.setPipelineConfiguration(pipe);
//        sender.setStartNode(dom);
//        Receiver r = (config.getSerializerFactory().getReceiver(
//                new StreamResult(System.out), pipe, new Properties()
//        ));
//        NamespaceReducer nr = new NamespaceReducer();
//        nr.setPipelineConfiguration(pipe);
//        nr.setUnderlyingReceiver(r);
//        sender.setReceiver(nr);
//        sender.send();
//
//    }

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
