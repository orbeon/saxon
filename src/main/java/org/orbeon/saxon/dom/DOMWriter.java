package org.orbeon.saxon.dom;
import org.orbeon.saxon.event.PipelineConfiguration;
import org.orbeon.saxon.event.Receiver;
import org.orbeon.saxon.event.ReceiverOptions;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.om.NamespaceConstant;
import org.orbeon.saxon.om.StandardNames;
import org.orbeon.saxon.trans.XPathException;
import org.w3c.dom.*;


/**
  * DOMWriter is a Receiver that attaches the result tree to a specified Node in a DOM Document
  */

public class DOMWriter implements Receiver {

    private PipelineConfiguration pipe;
    private NamePool namePool;
    private Node currentNode;
    private Document document;
    private Node nextSibling;
    private int level = 0;
    private boolean canNormalize = true;
    private String systemId;

    /**
    * Set the pipelineConfiguration
    */

    public void setPipelineConfiguration(PipelineConfiguration pipe) {
        this.pipe = pipe;
        namePool = pipe.getConfiguration().getNamePool();
    }

    /**
    * Get the pipeline configuration used for this document
    */

    public PipelineConfiguration getPipelineConfiguration() {
        return pipe;
    }

    /**
     * Set the System ID of the destination tree
     */

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    /**
     * Notify an unparsed entity URI.
     *
     * @param name     The name of the unparsed entity
     * @param systemID The system identifier of the unparsed entity
     * @param publicID The public identifier of the unparsed entity
     */

    public void setUnparsedEntity(String name, String systemID, String publicID) throws XPathException {
       // no-op
    }

    /**
     * Get the system identifier that was set with setSystemId.
     *
     * @return The system identifier that was set with setSystemId,
     *         or null if setSystemId was not called.
     */
    public String getSystemId() {
        return systemId;
    }

    /**
    * Start of the document.
    */

    public void open () {}

    /**
    * End of the document.
    */

    public void close () {}

    /**
     * Start of a document node.
    */

    public void startDocument(int properties) throws XPathException {}

    /**
     * Notify the end of a document node
     */

    public void endDocument() throws XPathException {}

    /**
    * Start of an element.
    */

    public void startElement (int nameCode, int typeCode, int locationId, int properties) throws XPathException {
        String qname = namePool.getDisplayName(nameCode);
        String uri = namePool.getURI(nameCode);
        try {
            Element element = document.createElementNS(("".equals(uri) ? null : uri), qname);
            if (nextSibling != null && level == 0) {
                currentNode.insertBefore(element, nextSibling);
            } else {
                currentNode.appendChild(element);
            }
            currentNode = element;
        } catch (DOMException err) {
            throw new XPathException(err);
        }
        level++;
    }

    public void namespace (int namespaceCode, int properties) throws XPathException {
        try {
        	String prefix = namePool.getPrefixFromNamespaceCode(namespaceCode);
    		String uri = namePool.getURIFromNamespaceCode(namespaceCode);
    		Element element = (Element)currentNode;
            if (!(uri.equals(NamespaceConstant.XML))) {
                if (prefix.length() == 0) {
                    element.setAttributeNS(NamespaceConstant.XMLNS, "xmlns", uri);
                } else {
                    element.setAttributeNS(NamespaceConstant.XMLNS, "xmlns:" + prefix, uri);

                }
            }
        } catch (DOMException err) {
            throw new XPathException(err);
        }
    }

    public void attribute (int nameCode, int typeCode, CharSequence value, int locationId, int properties)
    throws XPathException {
        String qname = namePool.getDisplayName(nameCode);
        String uri = namePool.getURI(nameCode);
        try {
    		Element element = (Element)currentNode;
            element.setAttributeNS(("".equals(uri) ? null : uri), qname, value.toString());
            // The following code assumes JDK 1.5 or JAXP 1.3
            if ((nameCode & NamePool.FP_MASK) == StandardNames.XML_ID || (properties & ReceiverOptions.IS_ID) != 0) {
                String localName = namePool.getLocalName(nameCode);
                element.setIdAttributeNS(uri, localName, true);
            }
        } catch (DOMException err) {
            throw new XPathException(err);
        }
    }

    public void startContent() throws XPathException {}

    /**
    * End of an element.
    */

    public void endElement () throws XPathException {
		if (canNormalize) {
	        try {
	            currentNode.normalize();
	        } catch (Throwable err) {
	        	canNormalize = false;
	        }      // in case it's a Level 1 DOM
	    }

        currentNode = currentNode.getParentNode();
        level--;
    }


    /**
    * Character data.
    */

    public void characters (CharSequence chars, int locationId, int properties) throws XPathException
    {
        try {
            Text text = document.createTextNode(chars.toString());
            if (nextSibling != null && level == 0) {
                currentNode.insertBefore(text, nextSibling);
            } else {
                currentNode.appendChild(text);
            }
        } catch (DOMException err) {
            throw new XPathException(err);
        }
    }


    /**
    * Handle a processing instruction.
    */

    public void processingInstruction (String target, CharSequence data, int locationId, int properties)
        throws XPathException
    {
        try {
            ProcessingInstruction pi =
                document.createProcessingInstruction(target, data.toString());
            if (nextSibling != null && level == 0) {
                currentNode.insertBefore(pi, nextSibling);
            } else {
                currentNode.appendChild(pi);
            }
        } catch (DOMException err) {
            throw new XPathException(err);
        }
    }

    /**
    * Handle a comment.
    */

    public void comment (CharSequence chars, int locationId, int properties) throws XPathException
    {
        try {
            Comment comment = document.createComment(chars.toString());
            if (nextSibling != null && level == 0) {
                currentNode.insertBefore(comment, nextSibling);
            } else {
                currentNode.appendChild(comment);
            }
        } catch (DOMException err) {
            throw new XPathException(err);
        }
    }

    /**
     * Set the attachment point for the new subtree
     * @param node the node to which the new subtree will be attached
    */

    public void setNode (Node node) {
        if (node == null) {
            return;
        }
        currentNode = node;
        if (node.getNodeType() == Node.DOCUMENT_NODE) {
            document = (Document)node;
        } else {
            document = currentNode.getOwnerDocument();
        }
    }

    /**
     * Set next sibling
     * @param nextSibling the node, which must be a child of the attachment point, before which the new subtree
     * will be created. If this is null the new subtree will be added after any existing children of the
     * attachment point.
     */

    public void setNextSibling(Node nextSibling) {
        this.nextSibling = nextSibling;
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
