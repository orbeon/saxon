package org.orbeon.saxon.tinytree;
import org.orbeon.saxon.event.Builder;
import org.orbeon.saxon.event.ReceiverOptions;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.xpath.DynamicError;
import org.orbeon.saxon.xpath.XPathException;


/**
  * The TinyBuilder class is responsible for taking a stream of SAX events and constructing
  * a Document tree, using the "TinyTree" implementation.
  *
  * @author Michael H. Kay
  */

public class TinyBuilder extends Builder  {

    private int currentDepth = 0;
    private int nodeNr = 0;             // this is the local sequence within this document
    private boolean ended = false;
    private int[] sizeParameters;       // estimate of number of nodes, attributes, namespaces, characters

    public void setSizeParameters(int[] params) {
        sizeParameters = params;
    }

    private int[] prevAtDepth = new int[100];
            // this array is scaffolding used while constructing the tree, it is
            // not present in the final tree.

    public void createDocument () {
        if (sizeParameters==null) {
            currentDocument = new TinyDocumentImpl();
        } else {
            currentDocument = new TinyDocumentImpl(sizeParameters[0],
                                    sizeParameters[1], sizeParameters[2], sizeParameters[3]);
        }
        TinyDocumentImpl doc = (TinyDocumentImpl)currentDocument;
        doc.setSystemId(getSystemId());
        doc.setConfiguration(config);
    }

    /**
    * Callback interface for SAX: not for application use
    */

    public void open () throws XPathException
    {
        // System.err.println("Builder: " + this + " Start document");
        //failed = false;
        if (started) {
            // this happens when using an IdentityTransformer
            return;
        }
        started = true;

        if (currentDocument==null) {
            // normal case
            createDocument();
        } else {
            // document node supplied by user
            if (!(currentDocument instanceof TinyDocumentImpl)) {
                throw new DynamicError("Root node supplied is of wrong type");
            }
            if (currentDocument.hasChildNodes()) {
                throw new DynamicError("Supplied document is not empty");
            }
            currentDocument.setConfiguration(config);
        }

        //currentNode = currentDocument;
        currentDepth = 0;

        TinyDocumentImpl doc = (TinyDocumentImpl)currentDocument;
        if (lineNumbering) {
            doc.setLineNumbering();
        }

        doc.addNode(Type.DOCUMENT, 0, 0, 0, -1);
        prevAtDepth[0] = 0;
        prevAtDepth[1] = -1;
        doc.next[0] = -1;

        currentDepth++;

        super.open();

    }

    /**
    * Callback interface for SAX: not for application use
    */

    public void close () throws XPathException
    {
             // System.err.println("TinyBuilder: " + this + " End document");

        if (ended) return;  // happens when using an IdentityTransformer
        ended = true;

        prevAtDepth[currentDepth] = -1;

        ((TinyDocumentImpl)currentDocument).condense();

        super.close();
    }

    /**
    * Notify the start tag of an element
    */

    public void startElement (int nameCode, int typeCode, int locationId, int properties) throws XPathException
    {
        //System.err.println("TinyBuilder Start element (" + nameCode + "," + typeCode + ")");

        TinyDocumentImpl doc = (TinyDocumentImpl)currentDocument;

		nodeNr = doc.addNode(Type.ELEMENT, currentDepth, -1, -1, nameCode);

		if (typeCode != -1) {
		    doc.setElementAnnotation(nodeNr, typeCode);
		}

        int prev = prevAtDepth[currentDepth];
        if (prev > 0) {
            doc.next[prev] = nodeNr;
        }
        doc.next[nodeNr] = prevAtDepth[currentDepth - 1];   // *O* owner pointer in last sibling
        prevAtDepth[currentDepth] = nodeNr;
        currentDepth++;

        if (currentDepth == prevAtDepth.length) {
            int[] p2 = new int[currentDepth*2];
            System.arraycopy(prevAtDepth, 0, p2, 0, currentDepth);
            prevAtDepth = p2;
        }
        prevAtDepth[currentDepth] = -1;

        if (locator != null) {
            doc.setSystemId(nodeNr, locator.getSystemId(locationId));
            if (lineNumbering) {
                doc.setLineNumber(nodeNr, locator.getLineNumber(locationId));
            }
        }
    }

    public void namespace(int namespaceCode, int properties) throws XPathException {
        ((TinyDocumentImpl)currentDocument).addNamespace( nodeNr, namespaceCode );
    }

    public void attribute(int nameCode, int typeCode, CharSequence value, int locationId, int properties)
    throws XPathException {
        // System.err.println("attribute " + nameCode + "=" + value);

        if ((properties & ReceiverOptions.DISABLE_ESCAPING) != 0) {
            DynamicError err = new DynamicError("Cannot disable output escaping when writing a tree");
            err.setErrorCode("XT1610");
            throw err;
        }

        ((TinyDocumentImpl)currentDocument).addAttribute(
                nodeNr, nameCode, typeCode, value, properties);
    }

    public void startContent() {
        nodeNr++;
    }

    /**
    * Callback interface for SAX: not for application use
    */

    public void endElement () throws XPathException
    {
        //System.err.println("End element ()");

        prevAtDepth[currentDepth] = -1;
        currentDepth--;
    }

    /**
    * Callback interface for SAX: not for application use
    */

    public void characters (CharSequence chars, int locationId, int properties) throws XPathException
    {
         // System.err.println("Characters: " + chars.toString());
        TinyDocumentImpl doc = (TinyDocumentImpl)currentDocument;
        if (chars.length()>0) {
            if ((properties & ReceiverOptions.DISABLE_ESCAPING) != 0) {
                DynamicError err = new DynamicError("Cannot disable output escaping when writing a tree");
                err.setErrorCode("XT1610");
                throw err;
            }
            int bufferStart = doc.charBufferLength;
            doc.appendChars(chars);
            int n=doc.numberOfNodes-1;
            if (doc.nodeKind[n] == Type.TEXT && doc.depth[n] == currentDepth) {
                doc.beta[n] += chars.length();
            } else {
                nodeNr = doc.addNode(Type.TEXT, currentDepth, bufferStart, chars.length(), -1);

                int prev = prevAtDepth[currentDepth];
                if (prev > 0) {
                    doc.next[prev] = nodeNr;
                }
                doc.next[nodeNr] = prevAtDepth[currentDepth - 1];   // *O* owner pointer in last sibling
                prevAtDepth[currentDepth] = nodeNr;
            }
        }
    }

    /**
    * Callback interface for SAX: not for application use<BR>
    */

    public void processingInstruction (String piname, CharSequence remainder, int locationId, int properties) throws XPathException
    {
    	// System.err.println("Builder: PI " + piname);

        TinyDocumentImpl doc = (TinyDocumentImpl)currentDocument;
        if (doc.commentBuffer==null) {
            doc.commentBuffer = new StringBuffer();
        }
        int s = doc.commentBuffer.length();
        doc.commentBuffer.append(remainder.toString());
        int nameCode = namePool.allocate("", "", piname);

        nodeNr = doc.addNode(Type.PROCESSING_INSTRUCTION, currentDepth, s, remainder.length(),
        			 nameCode);

        int prev = prevAtDepth[currentDepth];
        if (prev > 0) {
            doc.next[prev] = nodeNr;
        }
        doc.next[nodeNr] = prevAtDepth[currentDepth - 1];   // *O* owner pointer in last sibling
        prevAtDepth[currentDepth] = nodeNr;

            // TODO: handle PI Base URI
            //if (locator!=null) {
            //    pi.setLocation(locator.getSystemId(), locator.getLineNumber());
            //}
    }

    /**
    * Callback interface for SAX: not for application use
    */

    public void comment (CharSequence chars, int locationId, int properties) throws XPathException {
        TinyDocumentImpl doc = (TinyDocumentImpl)currentDocument;
        if (doc.commentBuffer==null) {
            doc.commentBuffer = new StringBuffer();
        }
        int s = doc.commentBuffer.length();
        doc.commentBuffer.append(chars.toString());
        nodeNr = doc.addNode(Type.COMMENT, currentDepth, s, chars.length(), -1);

        int prev = prevAtDepth[currentDepth];
        if (prev > 0) {
            doc.next[prev] = nodeNr;
        }
        doc.next[nodeNr] = prevAtDepth[currentDepth - 1];   // *O* owner pointer in last sibling
        prevAtDepth[currentDepth] = nodeNr;

    }

    /**
    * Set an unparsed entity in the document
    */

    public void setUnparsedEntity(String name, String uri, String publicId) {
        ((TinyDocumentImpl)currentDocument).setUnparsedEntity(name, uri, publicId);
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
