package org.orbeon.saxon.evpull;

import org.orbeon.saxon.event.LocationProvider;
import org.orbeon.saxon.event.PipelineConfiguration;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.pull.NamespaceContextImpl;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.AtomicValue;
import org.orbeon.saxon.value.Whitespace;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.util.Iterator;
import java.util.Stack;
import java.util.NoSuchElementException;

/**
 * This class bridges EventIterator events to XMLStreamReader (Stax) events. That is, it acts
 * as an XMLStreamReader, fetching the underlying data from an EventIterator.
 * <p>
 * An EventIterator may provide access to any XDM sequence, whereas an XMLStreamReader always
 * reads a document. The conversion of a sequence to a document follows the rules for
 * "normalizing" a sequence in the Serialization specification: for example, atomic values are
 * converted into text nodes, with adjacent atomic values being space-separated.
 */
public class EventToStaxBridge implements XMLStreamReader {

    private EventIterator provider;
    private StartElementEvent startElementEvent;
    private Item currentItem;
    private Stack stack;    // holds instances of StartElementEvent; needed because namespace information
                            // (though not attributes) must be available at EndElement time

    private NamePool namePool;
    private boolean previousAtomic;
    private FastStringBuffer currentTextNode = new FastStringBuffer(100);
    private int currentStaxEvent = XMLStreamConstants.START_DOCUMENT;
    private XPathException pendingException = null;

    /**
     * Create a EventToStaxBridge instance, which wraps a Saxon EventIterator as a Stax XMLStreamReader
     * @param provider the Saxon EventIterator from which the events will be read
     * @param namePool the Saxon NamePool
     */

    public EventToStaxBridge(EventIterator provider, NamePool namePool) {
        this.namePool = namePool;
        this.provider = new NamespaceMaintainer(EventStackIterator.flatten(provider), namePool);
        this.stack = new Stack();
    }

    public int getAttributeCount() {
        if (currentStaxEvent != START_ELEMENT) {
            throw new IllegalStateException(""+currentStaxEvent);
        }
        return startElementEvent.getAttributeCount();
    }

    public boolean isAttributeSpecified(int i) {
        if (currentStaxEvent != START_ELEMENT) {
            throw new IllegalStateException(""+currentStaxEvent);
        }
        return true;
    }

    public QName getAttributeName(int i) {
        if (currentStaxEvent != START_ELEMENT) {
            throw new IllegalStateException(""+currentStaxEvent);
        }
        NodeInfo att = startElementEvent.getAttribute(i);
        return new QName(att.getURI(), att.getLocalPart(), att.getPrefix());
    }

    public String getAttributeLocalName(int i) {
        if (currentStaxEvent != START_ELEMENT) {
            throw new IllegalStateException(""+currentStaxEvent);
        }
        return startElementEvent.getAttribute(i).getLocalPart();
    }

    public String getAttributeNamespace(int i) {
        if (currentStaxEvent != START_ELEMENT) {
            throw new IllegalStateException(""+currentStaxEvent);
        }
        return startElementEvent.getAttribute(i).getURI();
    }

    public String getAttributePrefix(int i) {
        if (currentStaxEvent != START_ELEMENT) {
            throw new IllegalStateException(""+currentStaxEvent);
        }
        return startElementEvent.getAttribute(i).getPrefix();
    }

    public String getAttributeType(int i) {
        if (currentStaxEvent != START_ELEMENT) {
            throw new IllegalStateException(""+currentStaxEvent);
        }
        int type = startElementEvent.getAttribute(i).getTypeAnnotation();
        if (type == StandardNames.XS_ID) {
            return "ID";
        } else if (type == StandardNames.XS_IDREF) {
            return "IDREF";
        } else if (type == StandardNames.XS_IDREFS) {
            return "IDREFS";
        } else if (type == StandardNames.XS_NMTOKEN) {
            return "NMTOKEN";
        } else if (type == StandardNames.XS_NMTOKENS) {
            return "NMTOKENS";
        } else if (type == StandardNames.XS_ENTITY) {
            return "ENTITY";
        } else if (type == StandardNames.XS_ENTITIES) {
            return "ENTITIES";
        }
        return "CDATA";
    }

    public String getAttributeValue(int i) {
        if (currentStaxEvent != START_ELEMENT) {
            throw new IllegalStateException(""+currentStaxEvent);
        }
        return startElementEvent.getAttribute(i).getStringValue();
    }

    public String getAttributeValue(String uri, String local) {
        for (Iterator iter = startElementEvent.iterateAttributes(); iter.hasNext(); ) {
            NodeInfo att = (NodeInfo)iter.next();
            if (att.getURI().equals(uri) && att.getLocalPart().equals(local)) {
                return att.getStringValue();
            }
        }
        return null;
    }

    public int getEventType() {
        return currentStaxEvent;
    }

    public int getNamespaceCount() {
        if (currentStaxEvent != START_ELEMENT && currentStaxEvent != END_ELEMENT) {
            throw new IllegalStateException(""+currentStaxEvent);
        }
        int[] nscodes = startElementEvent.getLocalNamespaces();
        for (int i=0; i<nscodes.length; i++) {
            if (nscodes[i] == -1) {
                return i;
            }
        }
        return nscodes.length;
    }

    public String getText() {
        if (currentStaxEvent != CHARACTERS && currentStaxEvent != COMMENT) {
            throw new IllegalStateException(""+currentStaxEvent);
        }
        if (previousAtomic) {
            return currentTextNode.toString();
        } else {
            return currentItem.getStringValue();
        }
    }

    public int getTextLength() {
        if (currentStaxEvent != CHARACTERS && currentStaxEvent != COMMENT) {
            throw new IllegalStateException(""+currentStaxEvent);
        }
        return getText().length();
    }

    public int getTextStart() {
        if (currentStaxEvent != CHARACTERS && currentStaxEvent != COMMENT) {
            throw new IllegalStateException(""+currentStaxEvent);
        }
        return 0;
    }

    public char[] getTextCharacters() {
        if (currentStaxEvent != CHARACTERS && currentStaxEvent != COMMENT) {
            throw new IllegalStateException(""+currentStaxEvent);
        }
        String stringValue = getText();
        char[] chars = new char[stringValue.length()];
        stringValue.getChars(0, chars.length, chars, 0);
        return chars;
    }


    public int getTextCharacters(int sourceStart, char[] target, int targetStart, int length)
            throws XMLStreamException {
        if (currentStaxEvent != CHARACTERS && currentStaxEvent != COMMENT) {
            throw new IllegalStateException(""+currentStaxEvent);
        }
        if (targetStart < 0 || targetStart > target.length) {
            throw new IndexOutOfBoundsException("targetStart");
        }
        if (length < 0 || targetStart + length > target.length) {
            throw new IndexOutOfBoundsException("length");
        }
        String value = getText();
        if (sourceStart >= value.length()) {
            return 0;
        }
        int sourceEnd = sourceStart + length;
        if (sourceEnd > value.length()) {
            sourceEnd = value.length();
        }
        value.getChars(sourceStart, sourceEnd, target, targetStart);
        return sourceEnd - sourceStart;
    }

    public int next() throws XMLStreamException {
        if (pendingException != null) {
            throw new XMLStreamException(pendingException);
        }
        PullEvent p;
        try {
            p = provider.next();
        } catch (XPathException e) {
            throw new XMLStreamException(e);
        }
        if (p == null) {
            // The spec is ambivalent here; it also says IllegalStateException is appropriate
            throw new NoSuchElementException("end of stream");
        }
        startElementEvent = null;
        if (p instanceof StartDocumentEvent) {
            // STAX doesn't actually report START_DOCUMENT: it's the initial state before reading any events
            currentStaxEvent = XMLStreamConstants.START_DOCUMENT;
            return next();
        } else if (p instanceof StartElementEvent) {
            startElementEvent = (StartElementEvent)p;
            currentStaxEvent = XMLStreamConstants.START_ELEMENT;
            stack.push(p);
            return currentStaxEvent;
        } else if (p instanceof EndElementEvent) {
            currentStaxEvent = XMLStreamConstants.END_ELEMENT;
            startElementEvent = (StartElementEvent)stack.pop();
            return currentStaxEvent;
        } else if (p instanceof EndDocumentEvent) {
            currentStaxEvent = XMLStreamConstants.END_DOCUMENT;
            return currentStaxEvent;
        } else if (p instanceof NodeInfo) {
            currentItem = (NodeInfo)p;
            switch (((NodeInfo)p).getNodeKind()) {
                case Type.COMMENT:
                    currentStaxEvent = XMLStreamConstants.COMMENT;
                    return currentStaxEvent;
                case Type.PROCESSING_INSTRUCTION:
                    currentStaxEvent = XMLStreamConstants.PROCESSING_INSTRUCTION;
                    return currentStaxEvent;
                case Type.TEXT:
                    currentStaxEvent = XMLStreamConstants.CHARACTERS;
                    return currentStaxEvent;
                case Type.ATTRIBUTE:
                    throw new XMLStreamException("Encountered top-level attribute in sequence");
                default:
                    throw new AssertionError("Unexpected node kind (sequence not decomposed?)");
            }
        } else if (p instanceof AtomicValue) {
            currentItem = (AtomicValue)p;
            currentStaxEvent = XMLStreamConstants.CHARACTERS;
            previousAtomic = true;
            return currentStaxEvent;
        } else {
            throw new AssertionError("Unknown pull event");
        }
    }

    public int nextTag() throws XMLStreamException {
        if (pendingException != null) {
            throw new XMLStreamException(pendingException);
        }
        int eventType = next();
        while ((eventType == XMLStreamConstants.CHARACTERS && isWhiteSpace()) // skip whitespace
                || (eventType == XMLStreamConstants.CDATA && isWhiteSpace())
                // skip whitespace
                || eventType == XMLStreamConstants.SPACE
                || eventType == XMLStreamConstants.PROCESSING_INSTRUCTION
                || eventType == XMLStreamConstants.COMMENT
                ) {
            eventType = next();
        }
        if (eventType != XMLStreamConstants.START_ELEMENT && eventType != XMLStreamConstants.END_ELEMENT) {
            throw new XMLStreamException("expected start or end tag", getLocation());
        }
        return eventType;
    }

    public void close() throws XMLStreamException {
        if (pendingException != null) {
            throw new XMLStreamException(pendingException);
        }
    }

    public boolean hasName() {
        return currentStaxEvent == XMLStreamConstants.START_ELEMENT ||
                currentStaxEvent == XMLStreamConstants.END_ELEMENT;
    }

    public boolean hasNext() throws XMLStreamException {
        if (pendingException != null) {
            throw new XMLStreamException(pendingException);
        }
        return currentStaxEvent != XMLStreamConstants.END_DOCUMENT;
    }

    public boolean hasText() {
        return currentStaxEvent == XMLStreamConstants.CHARACTERS || currentStaxEvent == XMLStreamConstants.COMMENT;
    }

    public boolean isCharacters() {
        return currentStaxEvent == XMLStreamConstants.CHARACTERS;
    }

    public boolean isEndElement() {
        return currentStaxEvent == XMLStreamConstants.END_ELEMENT;
    }

    public boolean isStandalone() {
        return false;
    }

    public boolean isStartElement() {
        return currentStaxEvent == XMLStreamConstants.START_ELEMENT;
    }

    public boolean isWhiteSpace() {
        return currentStaxEvent == XMLStreamConstants.CHARACTERS && Whitespace.isWhite(getText());
    }

    public boolean standaloneSet() {
        return false;
    }


    public String getCharacterEncodingScheme() {
        return null;
    }

    public String getElementText() throws XMLStreamException {
        if (pendingException != null) {
            throw new XMLStreamException(pendingException);
        }
        if (getEventType() != XMLStreamConstants.START_ELEMENT) {
            throw new XMLStreamException("parser must be on START_ELEMENT to read next text", getLocation());
        }
        int eventType = next();
        StringBuffer content = new StringBuffer();
        while (eventType != XMLStreamConstants.END_ELEMENT) {
            if (eventType == XMLStreamConstants.CHARACTERS
                    || eventType == XMLStreamConstants.CDATA
                    || eventType == XMLStreamConstants.SPACE
                    || eventType == XMLStreamConstants.ENTITY_REFERENCE) {
                content.append(getText());
            } else if (eventType == XMLStreamConstants.PROCESSING_INSTRUCTION
                    || eventType == XMLStreamConstants.COMMENT) {
                // skipping
            } else if (eventType == XMLStreamConstants.END_DOCUMENT) {
                throw new XMLStreamException("unexpected end of document when reading element text content", getLocation());
            } else if (eventType == XMLStreamConstants.START_ELEMENT) {
                throw new XMLStreamException("element text content may not contain START_ELEMENT", getLocation());
            } else {
                throw new XMLStreamException("Unexpected event type " + eventType, getLocation());
            }
            eventType = next();
        }
        return content.toString();
    }

    public String getEncoding() {
        return null;
    }

    public String getLocalName() {
        if (currentStaxEvent != START_ELEMENT && currentStaxEvent != END_ELEMENT) {
            throw new IllegalStateException(""+currentStaxEvent);
        }
        return namePool.getLocalName(startElementEvent.getNameCode());
    }

    public String getNamespaceURI() {
        if (currentStaxEvent != START_ELEMENT && currentStaxEvent != END_ELEMENT) {
            return null;
        }
        return namePool.getURI(startElementEvent.getNameCode());
    }

    public String getPIData() {
        if (currentStaxEvent != XMLStreamConstants.PROCESSING_INSTRUCTION) {
            throw new IllegalStateException("Not positioned at a processing instruction");
        }
        return getText();
    }

    public String getPITarget() {
        if (currentStaxEvent != XMLStreamConstants.PROCESSING_INSTRUCTION) {
            throw new IllegalStateException("Not positioned at a processing instruction");
        }
        return namePool.getLocalName(((NodeInfo)currentItem).getNameCode());
    }

    public String getPrefix() {
        if (currentStaxEvent != START_ELEMENT && currentStaxEvent != END_ELEMENT) {
            return null;
        }
        return namePool.getPrefix(startElementEvent.getNameCode());
    }

    public String getVersion() {
        return "1.0";
    }

    public String getNamespacePrefix(int i) {
        if (currentStaxEvent != START_ELEMENT && currentStaxEvent != END_ELEMENT) {
            throw new IllegalStateException(""+currentStaxEvent);
        }
        int nscode = startElementEvent.getLocalNamespaces()[i];
        return namePool.getPrefixFromNamespaceCode(nscode);
    }

    public String getNamespaceURI(int i) {
        if (currentStaxEvent != START_ELEMENT && currentStaxEvent != END_ELEMENT) {
            throw new IllegalStateException(""+currentStaxEvent);
        }
        int nscode = startElementEvent.getLocalNamespaces()[i];
        return namePool.getURIFromNamespaceCode(nscode);
    }

    public NamespaceContext getNamespaceContext() {
        return new NamespaceContextImpl((NamespaceResolver)provider);
    }

    public QName getName() {
        if (currentStaxEvent != START_ELEMENT && currentStaxEvent != END_ELEMENT) {
            throw new IllegalStateException(""+currentStaxEvent);
        }
        int nc = startElementEvent.getNameCode();
        return new QName(namePool.getURI(nc), namePool.getLocalName(nc), namePool.getPrefix(nc));
    }

    public Location getLocation() {
        if (startElementEvent != null) {
            PipelineConfiguration pipe = startElementEvent.getPipelineConfiguration();
            final LocationProvider provider = pipe.getLocationProvider();
            final int locationId = startElementEvent.getLocationId();
            return new Location() {
                public int getCharacterOffset() {
                    return -1;
                }

                public int getColumnNumber() {
                    return provider.getColumnNumber(locationId);
                }

                public int getLineNumber() {
                    return provider.getLineNumber(locationId);
                }

                public String getPublicId() {
                    return null;
                }

                public String getSystemId() {
                    return provider.getSystemId(locationId);
                }
            };
        } else if (currentItem instanceof NodeInfo) {
            final NodeInfo node = (NodeInfo)currentItem;
            return new Location() {
                public int getCharacterOffset() {
                    return -1;
                }

                public int getColumnNumber() {
                    return node.getColumnNumber();
                }

                public int getLineNumber() {
                    return node.getLineNumber();
                }

                public String getPublicId() {
                    return null;
                }

                public String getSystemId() {
                    return node.getSystemId();
                }
            };

        } else {
            return DummyLocation.THE_INSTANCE;
        }
    }
    public Object getProperty(String s) throws IllegalArgumentException {
        return null;
    }

    public void require(int event, String uri, String local) throws XMLStreamException {
        if (pendingException != null) {
            throw new XMLStreamException(pendingException);
        }
        if (currentStaxEvent != event) {
            throw new XMLStreamException("Required event type is " + event + ", actual event is " + currentStaxEvent);
        }
        if (uri != null && !uri.equals(getNamespaceURI())) {
            throw new XMLStreamException("Required namespace is " + uri + ", actual is " + getNamespaceURI());
        }
        if (local != null && !local.equals(getLocalName())) {
            throw new XMLStreamException("Required local name is " + local + ", actual is " + getLocalName());
        }
    }

    public String getNamespaceURI(String prefix) {
        if (prefix.equals("xmlns")) {
            return NamespaceConstant.XMLNS;
        }
        return ((NamespaceResolver)provider).getURIForPrefix(prefix, true);
    }

    public static class DummyLocation  implements Location{
        public static final Location THE_INSTANCE = new DummyLocation();

        private DummyLocation() {}

        public int getCharacterOffset() {
            return -1;
        }

        public int getColumnNumber() {
            return -1;
        }

        public int getLineNumber() {
            return -1;
        }

        public java.lang.String getPublicId() {
            return null;
        }

        public java.lang.String getSystemId() {
            return null;
        }
    }

    /**
     * Temporary test program
     * @param args command line arguments. First argument is a file containing an XML document
     * @throws Exception
     */

//    public static void main(String[] args) throws Exception {
//        Configuration config = new Configuration();
//        DocumentInfo doc = config.buildDocument(new StreamSource(new File(args[0])));
//        PipelineConfiguration pipe = config.makePipelineConfiguration();
//        pipe.setHostLanguage(Configuration.XQUERY);
//        EventIterator ei = new Decomposer(doc, pipe);
//        XMLStreamReader sr = new EventToStaxBridge(ei, config.getNamePool());
//        StaxBridge bridge = new StaxBridge();
//        bridge.setXMLStreamReader(sr);
//
//        bridge.setPipelineConfiguration(pipe);
//        Receiver out = config.getSerializerFactory().getReceiver(new StreamResult(System.out), pipe, new Properties());
//        PullPushCopier copier = new PullPushCopier(bridge, out);
//        copier.copy();
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
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//

