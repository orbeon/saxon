package net.sf.saxon.dom;
import net.sf.saxon.event.Emitter;
import net.sf.saxon.om.NamespaceConstant;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;
import org.w3c.dom.*;


/**
  * DOMEmitter is an Emitter that attaches the result tree to a specified Node in a DOM Document
  */

public class DOMEmitter extends Emitter
{
    protected Node currentNode;
    protected Document document;
    private boolean canNormalize = true;

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
            Element element = document.createElementNS(uri, qname);
            currentNode.appendChild(element);
            currentNode = element;
        } catch (DOMException err) {
            throw new DynamicError(err);
        }
    }

    public void namespace (int namespaceCode, int properties) throws XPathException {
        try {
        	String prefix = namePool.getPrefixFromNamespaceCode(namespaceCode);
    		String uri = namePool.getURIFromNamespaceCode(namespaceCode);
    		Element element = (Element)currentNode;
            if (!(uri.equals(NamespaceConstant.XML))) {
                if (prefix.equals("")) {
                    element.setAttributeNS("http://www.w3.org/2000/xmlns/",
                                           "xmlns", uri);
                } else {
                    element.setAttributeNS("http://www.w3.org/2000/xmlns/",
                                            "xmlns:" + prefix, uri);

                }
            }
        } catch (DOMException err) {
            throw new DynamicError(err);
        }
    }

    public void attribute (int nameCode, int typeCode, CharSequence value, int locationId, int properties)
    throws XPathException {
        String qname = namePool.getDisplayName(nameCode);
        String uri = namePool.getURI(nameCode);
        try {
    		Element element = (Element)currentNode;
            element.setAttributeNS(uri, qname, value.toString());
        } catch (DOMException err) {
            throw new DynamicError(err);
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

    }


    /**
    * Character data.
    */

    public void characters (CharSequence chars, int locationId, int properties) throws XPathException
    {
        try {
            Text text = document.createTextNode(chars.toString());
            currentNode.appendChild(text);
        } catch (DOMException err) {
            throw new DynamicError(err);
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
            currentNode.appendChild(pi);
        } catch (DOMException err) {
            throw new DynamicError(err);
        }
    }

    /**
    * Handle a comment.
    */

    public void comment (CharSequence chars, int locationId, int properties) throws XPathException
    {
        try {
            Comment comment = document.createComment(chars.toString());
            currentNode.appendChild(comment);
        } catch (DOMException err) {
            throw new DynamicError(err);
        }
    }

    /**
    * Set output destination
    */

    public void setNode (Node node) {
        if (node == null) {
            return;
        }
        currentNode = node;
        if (node instanceof Document) {
            document = (Document)node;
        } else {
            document = currentNode.getOwnerDocument();
        }
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
