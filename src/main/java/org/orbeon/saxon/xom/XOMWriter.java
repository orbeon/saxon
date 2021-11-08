package org.orbeon.saxon.xom;

import org.orbeon.saxon.event.PipelineConfiguration;
import org.orbeon.saxon.event.Receiver;
import org.orbeon.saxon.om.FastStringBuffer;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.trans.XPathException;
import nu.xom.*;

import java.util.Stack;

/**
  * XOMWriter is a Receiver that constructs a XOM document from the stream of events
  */

public class XOMWriter implements Receiver {

    private PipelineConfiguration pipe;
    private NamePool namePool;
    private Document document;
    private Stack ancestors = new Stack();
    private NodeFactory nodeFactory;
    private String systemId;
    private boolean implicitDocumentNode = false;
    private FastStringBuffer textBuffer = new FastStringBuffer(100);

    /**
     * Create a XOMWriter using the default node factory
     */

    public XOMWriter() {
        this.nodeFactory = new NodeFactory();
    }

    /**
     * Create a XOMWriter
     * @param factory the XOM NodeFactory to be used
     */

    public XOMWriter(NodeFactory factory) {
        if (factory == null) {
            throw new NullPointerException("factory");
        }
        this.nodeFactory = factory;
    }

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

    public void startDocument(int properties) throws XPathException {
        document = nodeFactory.startMakingDocument();
        document.setBaseURI(systemId);
        ancestors.push(document);
        textBuffer.setLength(0);
    }

    /**
     * Notify the end of a document node
     */

    public void endDocument() throws XPathException {
        nodeFactory.finishMakingDocument(document);
        ancestors.pop();
    }

    /**
    * Start of an element.
    */

    public void startElement (int nameCode, int typeCode, int locationId, int properties) throws XPathException {
        flush();
        String qname = namePool.getDisplayName(nameCode);
        String uri = namePool.getURI(nameCode);
        Element element;
        if (ancestors.isEmpty()) {
            startDocument(0);
            implicitDocumentNode = true;
        }
        if (ancestors.size() == 1) {
            element = nodeFactory.makeRootElement(qname, uri);
            document.setRootElement(element);
        } else {
            element = nodeFactory.startMakingElement(qname, uri);
        }
        if (element == null) {
            throw new XPathException("XOM node factory returned null");
        }
        ancestors.push(element);
    }

    public void namespace (int namespaceCode, int properties) throws XPathException {
        String prefix = namePool.getPrefixFromNamespaceCode(namespaceCode);
        String uri = namePool.getURIFromNamespaceCode(namespaceCode);
        ((Element)ancestors.peek()).addNamespaceDeclaration(prefix, uri);
    }

    public void attribute (int nameCode, int typeCode, CharSequence value, int locationId, int properties)
    throws XPathException {
        String qname = namePool.getDisplayName(nameCode);
        String uri = namePool.getURI(nameCode);
        Nodes nodes = nodeFactory.makeAttribute(qname, uri, value.toString(), Attribute.Type.CDATA);
        for (int n=0; n<nodes.size(); n++) {
            Node node = nodes.get(n);
            if (node instanceof Attribute) {
                ((Element)ancestors.peek()).addAttribute((Attribute) node);
            } else {
                ((Element)ancestors.peek()).appendChild(node);
            }
        }
    }

    public void startContent() throws XPathException {
        flush();
    }

    /**
    * End of an element.
    */

    public void endElement () throws XPathException {
        flush();
        Element element = (Element)ancestors.pop();
        Node parent = (Node)ancestors.peek();
        Nodes nodes = nodeFactory.finishMakingElement(element);
        if (parent == document) {
            if (implicitDocumentNode) {
                endDocument();
            }
        } else {
            for (int n=0; n<nodes.size(); n++) {
                Node node = nodes.get(n);
                if (node instanceof Attribute) {
                    ((Element)parent).addAttribute((Attribute) node);
                } else {
                    ((Element)parent).appendChild(node);
                }
            }
        }
    }

    /**
    * Character data.
    */

    public void characters (CharSequence chars, int locationId, int properties) throws XPathException {
        textBuffer.append(chars);
    }

    private void flush() {
        if (textBuffer.length() != 0) {
            Nodes nodes = nodeFactory.makeText(textBuffer.toString());
            for (int n=0; n<nodes.size(); n++) {
                Node node = nodes.get(n);
                if (node instanceof Attribute) {
                    ((Element)ancestors.peek()).addAttribute((Attribute) node);
                } else {
                    ((Element)ancestors.peek()).appendChild(node);
                }
            }
            textBuffer.setLength(0);
        }
    }


    /**
    * Handle a processing instruction.
    */

    public void processingInstruction (String target, CharSequence data, int locationId, int properties)
            throws XPathException {
        flush();
        Nodes nodes = nodeFactory.makeProcessingInstruction(target, data.toString());
        for (int n=0; n<nodes.size(); n++) {
            Node node = nodes.get(n);
            if (node instanceof Attribute) {
                ((Element)ancestors.peek()).addAttribute((Attribute) node);
            } else {
                ((Element)ancestors.peek()).appendChild(node);
            }
        }
    }

    /**
    * Handle a comment.
    */

    public void comment (CharSequence chars, int locationId, int properties) throws XPathException{
        flush();
        Nodes nodes = nodeFactory.makeComment(chars.toString());
        for (int n=0; n<nodes.size(); n++) {
            Node node = nodes.get(n);
            if (node instanceof Attribute) {
                ((Element)ancestors.peek()).addAttribute((Attribute) node);
            } else {
                ((Element)ancestors.peek()).appendChild(node);
            }
        }
    }

    /**
     * Get the constructed document node
     * @return the document node of the constructed XOM tree
     */

    public Document getDocument() {
        return document;
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

