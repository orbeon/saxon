package org.orbeon.saxon.pull;

import org.orbeon.saxon.om.AttributeCollection;
import org.orbeon.saxon.om.FastStringBuffer;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.Whitespace;
import org.orbeon.saxon.xpath.NamespaceContextImpl;
import org.orbeon.saxon.expr.ExpressionLocation;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.SourceLocator;

/**
 * This class bridges PullProvider events to XMLStreamReader (Stax) events. That is, it acts
 * as an XMLStreamReader, fetching the underlying data from a PullProvider.
 * <p>
 * A PullProvider may provide access to any XDM sequence, whereas an XMLStreamReader always
 * reads a document. The conversion of a sequence to a document follows the rules for
 * "normalizing" a sequence in the Serialization specification: for example, atomic values are
 * converted into text nodes, with adjacent atomic values being space-separated.
 */
public class PullToStax implements XMLStreamReader {

    private PullNamespaceReducer provider;
    private NamePool namePool;
    private boolean previousAtomic;
    private FastStringBuffer currentTextNode = new FastStringBuffer(100);
    private int currentStaxEvent;
    private XPathException pendingException = null;

    public PullToStax(PullProvider provider) {
        if (provider instanceof PullNamespaceReducer) {
            this.provider = (PullNamespaceReducer)provider;
        } else {
            this.provider = new PullNamespaceReducer(provider);
        }
        this.namePool = provider.getPipelineConfiguration().getConfiguration().getNamePool();
    }

    public int getAttributeCount() {
        try {
            return provider.getAttributes().getLength();
        } catch (XPathException e) {
            pendingException = e;
            return 0;
        }
    }

    public boolean isAttributeSpecified(int i) {
        return true;
    }

    public QName getAttributeName(int i) {
        try {
            AttributeCollection atts = provider.getAttributes();
            return new QName(atts.getURI(i), atts.getLocalName(i), atts.getPrefix(i));
        } catch (XPathException e) {
            pendingException = e;
            return new QName("http://saxon.sf.net/error", "error", "");
        }
    }

    public String getAttributeLocalName(int i) {
        try {
            return provider.getAttributes().getLocalName(i);
        } catch (XPathException e) {
            pendingException = e;
            return "error";
        }
    }

    public String getAttributeNamespace(int i) {
        try {
            return provider.getAttributes().getURI(i);
        } catch (XPathException e) {
            pendingException = e;
            return "http://saxon.sf.net/error";
        }
    }

    public String getAttributePrefix(int i) {
        try {
            return provider.getAttributes().getPrefix(i);
        } catch (XPathException e) {
            pendingException = e;
            return "";
        }
    }

    public String getAttributeType(int i) {
        try {
            AttributeCollection ac = provider.getAttributes();
            if (ac.isId(i)) {
                return "ID";
            } else if (ac.isIdref(i)) {
                return "IDREFS";
            } else {
                return "CDATA";
            }
        } catch (XPathException e) {
            pendingException = e;
            return "CDATA";
        }
    }

    public String getAttributeValue(int i) {
        try {
            return provider.getAttributes().getValue(i);
        } catch (XPathException e) {
            pendingException = e;
            return "error";
        }
    }

    public String getAttributeValue(String uri, String local) {
        try {
            return provider.getAttributes().getValue(uri, local);
        } catch (XPathException e) {
            pendingException = e;
            return "";
        }
    }

    public int getEventType() {
        return currentStaxEvent;
    }

    public int getNamespaceCount() {
        // TODO: this only works on startElement. To return the value on endElement, we need to hold a stack.
        try {
            return provider.getNamespaceDeclarations().getNumberOfNamespaces();
        } catch (XPathException e) {
            pendingException = e;
            return 0;
        }
    }

    public String getText() {
        if (previousAtomic) {
            return currentTextNode.toString();
        } else {
            try {
                return provider.getStringValue().toString();
            } catch (XPathException e) {
                pendingException = e;
                return "";
            }
        }
    }

    public int getTextLength() {
        if (previousAtomic) {
            return currentTextNode.length();
        } else {
            try {
                return provider.getStringValue().length();
            } catch (XPathException e) {
                pendingException = e;
                return 0;
            }
        }
    }

    public int getTextStart() {
        return 0;
    }

    public char[] getTextCharacters() {
        if (previousAtomic) {
            return currentTextNode.getCharArray();
        } else {
            try {
                String stringValue = provider.getStringValue().toString();
                char[] chars = new char[stringValue.length()];
                stringValue.getChars(0, chars.length, chars, 0);
                return chars;
            } catch (XPathException e) {
                pendingException = e;
                return new char[0];
            }
        }
    }


    public int getTextCharacters(int sourceStart, char[] target, int targetStart, int length) throws XMLStreamException {
        if (pendingException != null) {
            throw new XMLStreamException(pendingException);
        }
        if (previousAtomic) {
            int end = sourceStart + length;
            if (end > currentTextNode.length()) {
                end = currentTextNode.length();
            }
            currentTextNode.getChars(sourceStart, end, target, targetStart);
            return end - sourceStart;
        } else {
            try {
                String stringValue = provider.getStringValue().subSequence(sourceStart, sourceStart + length).toString();
                stringValue.getChars(0, length, target, targetStart);
                return length;
            } catch (XPathException e) {
                pendingException = e;
                return 0;
            }
        }
    }

    public int next() throws XMLStreamException {
        if (pendingException != null) {
            throw new XMLStreamException(pendingException);
        }
        int p;
        try {
            p = provider.next();
        } catch (XPathException e) {
            throw new XMLStreamException(e);
        }
        switch (p) {
            case PullProvider.START_OF_INPUT:
                return next();
            case PullProvider.ATOMIC_VALUE:
                currentTextNode.setLength(0);
                if (previousAtomic) {
                    currentTextNode.append(' ');
                }
                currentTextNode.append(provider.getAtomicValue().getStringValue());
                currentStaxEvent = XMLStreamConstants.CHARACTERS;
                break;
            case PullProvider.START_DOCUMENT:
                currentStaxEvent = XMLStreamConstants.START_DOCUMENT;
                break;
            case PullProvider.END_DOCUMENT:
                currentStaxEvent = XMLStreamConstants.END_DOCUMENT;
                break;
            case PullProvider.START_ELEMENT:
                currentStaxEvent = XMLStreamConstants.START_ELEMENT;
                break;
            case PullProvider.END_ELEMENT:
                currentStaxEvent = XMLStreamConstants.END_ELEMENT;
                break;
            case PullProvider.TEXT:
                currentStaxEvent = XMLStreamConstants.CHARACTERS;
                break;
            case PullProvider.COMMENT:
                currentStaxEvent = XMLStreamConstants.COMMENT;
                break;
            case PullProvider.PROCESSING_INSTRUCTION:
                currentStaxEvent = XMLStreamConstants.PROCESSING_INSTRUCTION;
                break;
            case PullProvider.END_OF_INPUT:
                currentStaxEvent = XMLStreamConstants.END_DOCUMENT;
                break;
            case PullProvider.ATTRIBUTE:
                throw new XMLStreamException("Free-standing attributes cannot be serialized");
            case PullProvider.NAMESPACE:
                throw new XMLStreamException("Free-standing namespace nodes cannot be serialized");
            default:
                throw new IllegalStateException("Unknown Stax event " + p);

        }
        previousAtomic = (p == PullProvider.ATOMIC_VALUE);
        return currentStaxEvent;
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
        provider.close();
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
        try {
            return currentStaxEvent == XMLStreamConstants.CHARACTERS && Whitespace.isWhite(provider.getStringValue());
        } catch (XPathException e) {
            pendingException = e;
            return false;
        }
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
        return namePool.getLocalName(provider.getNameCode());
    }

    public String getNamespaceURI() {
        return namePool.getURI(provider.getNameCode());
    }

    public String getPIData() {
        if (currentStaxEvent != XMLStreamConstants.PROCESSING_INSTRUCTION) {
            throw new IllegalStateException("Not positioned at a processing instruction");
        }
        try {
            return provider.getStringValue().toString();
        } catch (XPathException e) {
            pendingException = e;
            return "";
        }
    }

    public String getPITarget() {
        if (currentStaxEvent != XMLStreamConstants.PROCESSING_INSTRUCTION) {
            throw new IllegalStateException("Not positioned at a processing instruction");
        }
        return namePool.getLocalName(provider.getNameCode());
    }

    public String getPrefix() {
        return namePool.getPrefix(provider.getNameCode());
    }


    public String getVersion() {
        return "1.0";
    }


    public String getNamespacePrefix(int i) {
        try {
            return provider.getNamespaceDeclarations().getPrefix(i);
        } catch (XPathException e) {
            pendingException = e;
            return "";
        }
    }

    public String getNamespaceURI(int i) {
        try {
            return provider.getNamespaceDeclarations().getURI(i);
        } catch (XPathException e) {
            pendingException = e;
            return "";
        }
    }

    public NamespaceContext getNamespaceContext() {
        return new NamespaceContextImpl(provider);
    }

    public QName getName() {
        int nc = provider.getNameCode();
        return new QName(namePool.getURI(nc), namePool.getLocalName(nc), namePool.getPrefix(nc));
    }

    public Location getLocation() {
        SourceLocator sourceLocator = provider.getSourceLocator();
        if (sourceLocator == null) {
            sourceLocator = new ExpressionLocation();
        }
        return new SourceStreamLocation(sourceLocator);
    }

    public Object getProperty(String s) throws IllegalArgumentException {
        return null;  //TODO
    }

    public void require(int event, String uri, String local) throws XMLStreamException {
        if (pendingException != null) {
            throw new XMLStreamException(pendingException);
        }
        if (currentStaxEvent != event) {
            throw new XMLStreamException("Required event type is " + event + ", actual event is " + currentStaxEvent);
        }
        if (uri != null && uri != getNamespaceURI()) {
            throw new XMLStreamException("Required namespace is " + uri + ", actual is " + getNamespaceURI());
        }
        if (local != null && local != getLocalName()) {
            throw new XMLStreamException("Required local name is " + local + ", actual is " + getLocalName());
        }
    }

    public String getNamespaceURI(String s) {
        return provider.getURIForPrefix(s, true);
    }

    /**
     * Bridge a SAX SourceLocator to a javax.xml.stream.Location
     */

    public class SourceStreamLocation implements javax.xml.stream.Location {

        private SourceLocator locator;
        public SourceStreamLocation(SourceLocator locator) {
            this.locator = locator;
        }

        public int getCharacterOffset() {
            return -1;
        }

        public int getColumnNumber() {
            return locator.getColumnNumber();
        }

        public int getLineNumber() {
            return locator.getLineNumber();
        }

        public String getPublicId() {
            return locator.getPublicId();
        }

        public String getSystemId() {
            return locator.getSystemId();
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
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//

