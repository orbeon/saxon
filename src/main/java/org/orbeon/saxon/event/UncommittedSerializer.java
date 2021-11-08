package org.orbeon.saxon.event;
import org.orbeon.saxon.om.NamespaceConstant;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.Whitespace;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * This class is used when the decision on which serialization method to use has to be delayed until the first
 * element is read. It buffers comments and processing instructions until that happens; then when the first
 * element arrives it creates a real serialization pipeline and uses that for future output.
 * @author Michael H. Kay
 */

public class UncommittedSerializer extends ProxyReceiver {

    boolean committed = false;
    List pending = null;
    Result finalResult;
    Properties outputProperties;

    /**
     * Create an uncommitted Serializer
     * @param finalResult the output destination
     * @param outputProperties the serialization properties
     */

    public UncommittedSerializer(Result finalResult, Properties outputProperties) {
        this.finalResult = finalResult;
        this.outputProperties = outputProperties;
        setUnderlyingReceiver(new Sink());
    }

    public void open() throws XPathException {
        committed = false;
    }

    /**
    * End of document
    */

    public void close() throws XPathException {
        // empty output: must send a beginDocument()/endDocument() pair to the content handler
        if (!committed) {
            switchToMethod("xml");
        }
        getUnderlyingReceiver().close();
    }

    /**
    * Produce character output using the current Writer. <BR>
    */

    public void characters(CharSequence chars, int locationId, int properties) throws XPathException {
        if (committed) {
            getUnderlyingReceiver().characters(chars, locationId, properties);
        } else {
            if (pending==null) {
                pending = new ArrayList(10);
            }
            PendingNode node = new PendingNode();
            node.kind = Type.TEXT;
            node.name = null;
            node.content = chars.toString();    // needs to be immutable
            node.locationId = locationId;
            node.properties = properties;
            pending.add(node);
            if (!Whitespace.isWhite(chars)) {
                switchToMethod("xml");
            }
        }
    }

    /**
    * Processing Instruction
    */

    public void processingInstruction(String target, CharSequence data, int locationId, int properties) throws XPathException {
        if (committed) {
            getUnderlyingReceiver().processingInstruction(target, data, locationId, properties);
        } else {
            if (pending==null) {
                pending = new ArrayList(10);
            }
            PendingNode node = new PendingNode();
            node.kind = Type.PROCESSING_INSTRUCTION;
            node.name = target;
            node.content = data;
            node.locationId = locationId;
            node.properties = properties;
            pending.add(node);
        }
    }

    /**
    * Output a comment
    */

    public void comment (CharSequence chars, int locationId, int properties) throws XPathException {
        if (committed) {
            getUnderlyingReceiver().comment(chars, locationId, properties);
        } else {
           if (pending==null) {
                pending=new ArrayList(10);
            }
            PendingNode node = new PendingNode();
            node.kind = Type.COMMENT;
            node.name = null;
            node.content = chars;
            node.locationId = locationId;
            node.properties = properties;
            pending.add(node);
        }
    }

    /**
    * Output an element start tag. <br>
    * This can only be called once: it switches to a substitute output generator for XML, XHTML, or HTML,
    * depending on the element name.
    * @param nameCode The element name (tag)
     * @param typeCode The type annotation
     * @param properties Bit field holding special properties of the element
     */

    public void startElement(int nameCode, int typeCode, int locationId, int properties) throws XPathException {
        if (!committed) {
            NamePool namePool = getNamePool();
            String name = namePool.getLocalName(nameCode);
            short uriCode = namePool.getURICode(nameCode);
            if (name.equalsIgnoreCase("html") && uriCode==NamespaceConstant.NULL_CODE) {
                switchToMethod("html");
            } else if (name.equals("html") && namePool.getURIFromURICode(uriCode).equals(NamespaceConstant.XHTML)) {
                String version = outputProperties.getProperty(SaxonOutputKeys.STYLESHEET_VERSION);
                if ("1".equals(version)) {
                    switchToMethod("xml");
                } else {
                    switchToMethod("xhtml");
                }
            } else {
                switchToMethod("xml");
            }
        }
        getUnderlyingReceiver().startElement(nameCode, typeCode, locationId, properties);
    }

    /**
     * Switch to a specific emitter once the output method is known
     * @param method the method to switch to (xml, html, xhtml)
    */

    private void switchToMethod(String method) throws XPathException {
        Properties newProperties = new Properties(outputProperties);
        newProperties.setProperty(OutputKeys.METHOD, method);
        SerializerFactory sf = getConfiguration().getSerializerFactory();
        Receiver target = sf.getReceiver(finalResult, getPipelineConfiguration(), newProperties);
        committed = true;
        target.open();
        target.startDocument(0);
        if (pending!=null) {
            for (int i = 0; i < pending.size(); i++) {
                PendingNode node = (PendingNode)pending.get(i);
                switch (node.kind) {
                case Type.COMMENT:
                    target.comment(node.content, node.locationId, node.properties);
                    break;
                case Type.PROCESSING_INSTRUCTION:
                    target.processingInstruction(node.name, node.content, node.locationId, node.properties);
                    break;
                case Type.TEXT:
                    target.characters(node.content, node.locationId, node.properties);
                    break;
                }
            }
            pending = null;
        }
        setUnderlyingReceiver(target);
    }

    /**
     * A text, comment, or PI node that hasn't been output yet because we don't yet know what output
     * method to use
     */

    private static final class PendingNode {
        int kind;
        String name;
        CharSequence content;
        int properties;
        int locationId;
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
