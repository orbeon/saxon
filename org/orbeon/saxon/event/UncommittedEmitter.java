package net.sf.saxon.event;
import net.sf.saxon.om.NamespaceConstant;
import net.sf.saxon.om.Navigator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.Type;

import javax.xml.transform.OutputKeys;
import java.util.ArrayList;
import java.util.List;

/**
  * This class generates XML or HTML output depending on whether the first tag output is "<html>"
  * @author Michael H. Kay
  */

public class UncommittedEmitter extends Emitter {

    boolean committed = false;
    Receiver baseReceiver = null;
    List pending = null;

    public void open() throws XPathException {
        committed = false;
    }

    /**
    * End of document
    */

    public void close() throws XPathException {
        // empty output: must send a beginDocument()/endDocument() pair to the content handler
        if (!committed) {
            switchToXML();
        }
        baseReceiver.close();
    }

    /**
     * Start of a document node. Nothing is done at this stage: the opening of the output
     * file is deferred until some content is written to it.
    */

    public void startDocument(int properties) throws XPathException {
        if (baseReceiver != null) {
            baseReceiver.startDocument(properties);
        }
    }

    /**
     * Notify the end of a document node
     */

    public void endDocument() throws XPathException {
        if (baseReceiver != null) {
            baseReceiver.endDocument();
        }
    }


    /**
    * Produce character output using the current Writer. <BR>
    */

    public void characters(CharSequence chars, int locationId, int properties) throws XPathException {
        if (committed) {
            baseReceiver.characters(chars, locationId, properties);
        } else {
            if (pending==null) {
                pending = new ArrayList(10);
            }
            PendingNode node = new PendingNode();
            node.kind = Type.TEXT;
            node.name = null;
            node.content = chars;
            node.locationId = locationId;
            node.properties = properties;
            pending.add(node);
            if (!Navigator.isWhite(chars)) {
                switchToXML();
            }
        }
    }

    /**
    * Processing Instruction
    */

    public void processingInstruction(String target, CharSequence data, int locationId, int properties) throws XPathException {
        if (committed) {
            baseReceiver.processingInstruction(target, data, locationId, properties);
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
            baseReceiver.comment(chars, locationId, properties);
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
    * This can only be called once: it switches to a substitute output generator for XML or HTML,
    * depending on whether the tag is "HTML".
    * @param nameCode The element name (tag)
     * @param typeCode The type annotation
     * @param properties Bit field holding special properties of the element
    */

    public void startElement(int nameCode, int typeCode, int locationId, int properties) throws XPathException {
        if (!committed) {
            String name = namePool.getLocalName(nameCode);
            short uriCode = namePool.getURICode(nameCode);

            if (name.equalsIgnoreCase("html") && uriCode==NamespaceConstant.NULL_CODE) {
                switchToHTML();
            } else if (name.equals("html") && namePool.getURIFromURICode(uriCode) == NamespaceConstant.XHTML) {
                // TODO: don't use XHTML in backwards compatibility mode
                switchToXHTML();
            } else {
                switchToXML();
            }
        }
        baseReceiver.startElement(nameCode, typeCode, locationId, properties);
    }

    /**
    * Notify a namespace. Namespaces are notified <b>after</b> the startElement event, and before
    * any children for the element. The namespaces that are reported are only required
    * to include those that are different from the parent element; however, duplicates may be reported.
    * A namespace must not conflict with any namespaces already used for element or attribute names.
    * @param namespaceCode an integer: the top half is a prefix code, the bottom half a URI code.
    * These may be translated into an actual prefix and URI using the name pool. A prefix code of
    * zero represents the empty prefix (that is, the default namespace). A URI code of zero represents
    * a URI of "", that is, a namespace undeclaration.
    * @throws IllegalStateException: attempt to output a namespace when there is no open element
    * start tag
    */

    public void namespace(int namespaceCode, int properties) throws XPathException {
        baseReceiver.namespace(namespaceCode, properties);
    }

    /**
    * Notify an attribute. Attributes are notified after the startElement event, and before any
    * children. Namespaces and attributes may be intermingled.
    * @param nameCode The name of the attribute, as held in the name pool
    * @param typeCode The type of the attribute, as held in the name pool
    * @param properties Bit significant value. The following bits are defined:
    *        <dd>DISABLE_ESCAPING</dd>           <dt>Disable escaping for this attribute</dt>
    *        <dd>NO_SPECIAL_CHARACTERS</dd>      <dt>Attribute value contains no special characters</dt>
    * @throws IllegalStateException: attempt to output an attribute when there is no open element
    * start tag
    */

    public void attribute(int nameCode, int typeCode, CharSequence value, int locationId, int properties)
    throws XPathException {
        baseReceiver.attribute(nameCode, typeCode, value, locationId, properties);
    }

    /**
    * Notify the start of the content, that is, the completion of all attributes and namespaces.
    * Note that the initial receiver of output from XSLT instructions will not receive this event,
    * it has to detect it itself. Note that this event is reported for every element even if it has
    * no attributes, no namespaces, and no content.
    */

    public void startContent() throws XPathException {
        baseReceiver.startContent();
    }

    /**
    * End of element
    */

    public void endElement() throws XPathException {
        baseReceiver.endElement();
    }

    /**
    * Switch to an XML emitter
    */

    private void switchToXML() throws XPathException {
        Emitter e = new XMLEmitter();
        baseReceiver = e;
        String indent = outputProperties.getProperty(OutputKeys.INDENT);
        if (indent!=null && indent.equals("yes")) {
            XMLIndenter in = new XMLIndenter();
            in.setUnderlyingReceiver(e);
            in.setPipelineConfiguration(pipelineConfig);
            in.setOutputProperties(outputProperties);
            baseReceiver = in;
        }
        String cdata = outputProperties.getProperty(OutputKeys.CDATA_SECTION_ELEMENTS);
        if (cdata!=null && cdata.length()>0) {
            CDATAFilter filter = new CDATAFilter();
            filter.setUnderlyingReceiver(e);
            filter.setPipelineConfiguration(pipelineConfig);
            filter.setOutputProperties(outputProperties);
            baseReceiver = filter;
        }
        switchTo(e);
    }

    /**
    * Switch to an XHTML emitter
    */

    private void switchToXHTML() throws XPathException {
        Emitter e = new XHTMLEmitter();
        baseReceiver = e;
        String indent = outputProperties.getProperty(OutputKeys.INDENT);
        if (indent==null || indent.equals("yes")) {
            XMLIndenter in = new XMLIndenter();
            in.setUnderlyingReceiver(e);
            in.setPipelineConfiguration(pipelineConfig);
            in.setOutputProperties(outputProperties);
            baseReceiver = in;
        }
        switchTo(e);
    }


    /**
    * Switch to an HTML emitter
    */

    private void switchToHTML() throws XPathException {
        Emitter e = new HTMLEmitter();
        baseReceiver = e;
        String indent = outputProperties.getProperty(OutputKeys.INDENT);
        if (indent==null || indent.equals("yes")) {
            HTMLIndenter in = new HTMLIndenter();
            in.setUnderlyingReceiver(e);
            in.setPipelineConfiguration(pipelineConfig);
            in.setOutputProperties(outputProperties);
            baseReceiver = in;
        }
        switchTo(e);
    }


    /**
    * Switch to a new underlying emitter
    */

    private void switchTo(Emitter emitter) throws XPathException {
        committed = true;
        emitter.setWriter(writer);
        emitter.setStreamResult(streamResult);
        emitter.characterSet = characterSet;
        emitter.setOutputProperties(outputProperties);
        emitter.setPipelineConfiguration(pipelineConfig);
        baseReceiver.open();
        baseReceiver.startDocument(0);
        if (pending!=null) {
            for (int i = 0; i < pending.size(); i++) {
                PendingNode node = (PendingNode)pending.get(i);
                switch (node.kind) {
                case Type.COMMENT:
                    emitter.comment(node.content, node.locationId, node.properties);
                    break;
                case Type.PROCESSING_INSTRUCTION:
                    emitter.processingInstruction(node.name, node.content, node.locationId, node.properties);
                    break;
                case Type.TEXT:
                    emitter.characters(node.content, node.locationId, node.properties);
                    break;
                }
            }
            pending = null;
        }
    }

    private static class PendingNode {
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
