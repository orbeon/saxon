package org.orbeon.saxon.dotnet;
import cli.System.Xml.*;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.event.PipelineConfiguration;
import org.orbeon.saxon.event.Receiver;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.om.NamespaceConstant;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.XPathException;


/**
  * DotNetDomBuilder is a Receiver that constructs an XmlDocument, the .NET implementation of a DOM
  */

public class DotNetDomBuilder implements Receiver {

    protected PipelineConfiguration pipe;
    protected NamePool namePool;
    protected String systemId;
    protected XmlNode currentNode;
    protected XmlDocument document;

    /**
    * Set the namePool in which all name codes can be found
    */

    public void setPipelineConfiguration(PipelineConfiguration pipe) {
        this.pipe = pipe;
        this.namePool = pipe.getConfiguration().getNamePool();
    }

    /**
    * Get the pipeline configuration used for this document
    */

    public PipelineConfiguration getPipelineConfiguration() {
        return pipe;
    }

    /**
    * Get the configuration used for this document
    */

    public Configuration getConfiguration() {
        return pipe.getConfiguration();
    }

    /**
    * Set the System ID
    */

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    /**
    * Get the System ID
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
        document = new XmlDocument();
        currentNode = document;
    }

    /**
     * Notify the end of a document node
     */

    public void endDocument() throws XPathException {}

    /**
    * Start of an element.
    */

    public void startElement (int nameCode, int typeCode, int locationId, int properties) throws XPathException {
        if (document == null) {
            document = new XmlDocument();
            currentNode = document;
        }
        String prefix = namePool.getPrefix(nameCode);
        String local = namePool.getLocalName(nameCode);
        String uri = namePool.getURI(nameCode);
        try {
            XmlElement element = document.CreateElement(prefix, local, uri);
            currentNode.AppendChild(element);
            currentNode = element;
        } catch (Exception err) {
            throw new DynamicError(err);
        }
    }

    public void namespace (int namespaceCode, int properties) throws XPathException {
        try {
        	String prefix = namePool.getPrefixFromNamespaceCode(namespaceCode);
    		String uri = namePool.getURIFromNamespaceCode(namespaceCode);
    		XmlElement element = (XmlElement)currentNode;
            if (!(uri.equals(NamespaceConstant.XML))) {
                if (prefix.equals("")) {
                    element.SetAttribute("xmlns", uri);
                } else {
                    // an odd way to do it, but using SetAttribute hits problems
                    XmlAttribute decl = document.CreateAttribute("xmlns", prefix, "http://www.w3.org/2000/xmlns/");
                    decl.set_InnerText(uri);
                    element.get_Attributes().Append(decl);
                }
            }
        } catch (Exception err) {
            throw new DynamicError(err);
        }
    }

    public void attribute (int nameCode, int typeCode, CharSequence value, int locationId, int properties)
    throws XPathException {
        String qname = namePool.getDisplayName(nameCode);
        String uri = namePool.getURI(nameCode);
        try {
    		XmlElement element = (XmlElement)currentNode;
            element.SetAttribute(qname, uri, value.toString());
        } catch (Exception err) {
            throw new DynamicError(err);
        }
    }

    public void startContent() throws XPathException {}

    /**
    * End of an element.
    */

    public void endElement () throws XPathException {
        currentNode.Normalize();
        currentNode = currentNode.get_ParentNode();

    }


    /**
    * Character data.
    */

    public void characters (CharSequence chars, int locationId, int properties) throws XPathException {
        try {
            XmlText text = document.CreateTextNode(chars.toString());
            currentNode.AppendChild(text);
        } catch (Exception err) {
            throw new DynamicError(err);
        }
    }


    /**
    * Handle a processing instruction.
    */

    public void processingInstruction (String target, CharSequence data, int locationId, int properties)
        throws XPathException {
        try {
            XmlProcessingInstruction pi =
                document.CreateProcessingInstruction(target, data.toString());
            currentNode.AppendChild(pi);
        } catch (Exception err) {
            throw new DynamicError(err);
        }
    }

    /**
    * Handle a comment.
    */

    public void comment (CharSequence chars, int locationId, int properties) throws XPathException {
        try {
            XmlComment comment = document.CreateComment(chars.toString());
            currentNode.AppendChild(comment);
        } catch (Exception err) {
            throw new DynamicError(err);
        }
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
     * Get the constructed document
     */

    public XmlDocument getDocumentNode() {
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
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
