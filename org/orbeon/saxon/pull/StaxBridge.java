package org.orbeon.saxon.pull;


import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.event.*;
import org.orbeon.saxon.expr.ExpressionLocation;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.style.StandardNames;
import org.orbeon.saxon.tinytree.CharSlice;
import org.orbeon.saxon.tinytree.CompressedWhitespace;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.SaxonErrorCode;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.AtomicValue;
import org.orbeon.saxon.value.Whitespace;

import javax.xml.stream.*;
import javax.xml.stream.events.EntityDeclaration;
import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.List;
import java.util.ArrayList;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * This class implements the Saxon PullProvider API on top of a standard StAX parser
 * (or any other StAX XMLStreamReader implementation)
 */

public class StaxBridge implements PullProvider, SaxonLocator, SourceLocationProvider {

    private XMLStreamReader reader;
    private StaxAttributes attributes = new StaxAttributes();
    private StaxNamespaces namespaces = new StaxNamespaces();
    private PipelineConfiguration pipe;
    private List unparsedEntities = null;
    int currentEvent = START_OF_INPUT;
    int depth = 0;
    boolean ignoreIgnorable = false;

    public StaxBridge() {

    }

    /**
     * Supply an input stream containing XML to be parsed. A StAX parser is created using
     * the JAXP XMLInputFactory.
     * @param systemId The Base URI of the input document
     * @param inputStream the stream containing the XML to be parsed
     * @throws XPathException if an error occurs creating the StAX parser
     */

    public void setInputStream(String systemId, InputStream inputStream) throws XPathException {
        try {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            factory.setXMLReporter(new StaxErrorReporter());
            XMLStreamReader reader = factory.createXMLStreamReader(systemId, inputStream);
            this.reader = reader;
        } catch (XMLStreamException e) {
            throw new DynamicError(e);
        }
    }

    /**
     * Supply an XMLStreamReader: the events reported by this XMLStreamReader will be translated
     * into PullProvider events
     */

    public void setXMLStreamReader(XMLStreamReader reader) {
        this.reader = reader;
    }

    /**
     * Set configuration information. This must only be called before any events
     * have been read.
     */

    public void setPipelineConfiguration(PipelineConfiguration pipe) {
        this.pipe = new PipelineConfiguration(pipe);
        this.pipe.setLocationProvider(this);
        ignoreIgnorable = pipe.getConfiguration().getStripsWhiteSpace() != Whitespace.NONE;
    }

    /**
     * Get configuration information.
     */

    public PipelineConfiguration getPipelineConfiguration() {
        return pipe;
    }

    /**
     * Get the XMLStreamReader used by this StaxBridge. This is available only after
     * setInputStream() or setXMLStreamReader() has been called
     * @return the instance of XMLStreamReader allocated when setInputStream() was called,
     * or the instance supplied directly to setXMLStreamReader()
     */

    public XMLStreamReader getXMLStreamReader() {
        return reader;
    }

    /**
     * Get the name pool
     */

    public NamePool getNamePool() {
        return pipe.getConfiguration().getNamePool();
    }

    /**
     * Get the next event
     *
     * @return an integer code indicating the type of event. The code
     *         {@link #END_OF_INPUT} is returned at the end of the sequence.
     */

    public int next() throws XPathException {
        if (currentEvent == START_OF_INPUT) {
            // StAX isn't reporting START_DOCUMENT so we supply it ourselves
            currentEvent = START_DOCUMENT;
            return currentEvent;
        }
        if (currentEvent == END_OF_INPUT || currentEvent == END_DOCUMENT) {
            try {
                reader.close();
            } catch (XMLStreamException e) {
                //
            }
            return END_OF_INPUT;
        }
        try {
            if (reader.hasNext()) {
                int event = reader.next();
                //System.err.println("Read event " + event);
                currentEvent = translate(event);
            } else {
                currentEvent = END_OF_INPUT;
            }
        } catch (XMLStreamException e) {
            String message = e.getMessage();
            // Following code recognizes the messages produced by the Sun Zephyr parser
            if (message.startsWith("ParseError at")) {
                int c = message.indexOf("\nMessage: ");
                if (c > 0) {
                    message = message.substring(c+10);
                }
            }
            DynamicError err = new DynamicError("Error reported by XML parser: " + message);
            err.setErrorCode(SaxonErrorCode.SXXP0003);
            err.setLocator(translateLocation(e.getLocation()));
            throw err;
        }
        return currentEvent;
    }


    private int translate(int event) throws XPathException {
            //System.err.println("EVENT " + event);
            switch (event) {
                case XMLStreamConstants.ATTRIBUTE:
                    return ATTRIBUTE;
                case XMLStreamConstants.CDATA:
                    return TEXT;
                case XMLStreamConstants.CHARACTERS:
                    if (depth == 0 && reader.isWhiteSpace()) {
                        return next();
//                    } else if (reader.isWhiteSpace()) {
//                        return next();
                    } else {
//                        System.err.println("TEXT[" + new String(reader.getTextCharacters(), reader.getTextStart(), reader.getTextLength()) + "]");
//                        System.err.println("  ARRAY length " + reader.getTextCharacters().length + "[" + new String(reader.getTextCharacters(), 0, reader.getTextStart() + reader.getTextLength()) + "]");
//                        System.err.println("  START: " + reader.getTextStart() + " LENGTH " + reader.getTextLength());
                        return TEXT;
                    }
                case XMLStreamConstants.COMMENT:
                    return COMMENT;
                case XMLStreamConstants.DTD:
                    unparsedEntities = (List)reader.getProperty("javax.xml.stream.entities");
                    return next();
                case XMLStreamConstants.END_DOCUMENT:
                    return END_DOCUMENT;
                case XMLStreamConstants.END_ELEMENT:
                    depth--;
                    return END_ELEMENT;
                case XMLStreamConstants.ENTITY_DECLARATION:
                    return next();
                case XMLStreamConstants.ENTITY_REFERENCE:
                    return next();
                case XMLStreamConstants.NAMESPACE:
                    return NAMESPACE;
                case XMLStreamConstants.NOTATION_DECLARATION:
                    return next();
                case XMLStreamConstants.PROCESSING_INSTRUCTION:
                    return PROCESSING_INSTRUCTION;
                case XMLStreamConstants.SPACE:
                    if (depth == 0) {
                        return next();
                    } else if (ignoreIgnorable) {
                        // (Brave attempt, but Woodstox doesn't seem to report ignorable whitespace)
                        return next();
                    } else {
                        return TEXT;
                    }
                case XMLStreamConstants.START_DOCUMENT:
                    return next();  // we supplied the START_DOCUMENT ourselves
                    //return START_DOCUMENT;
                case XMLStreamConstants.START_ELEMENT:
                    depth++;
                    return START_ELEMENT;
                default:
                    throw new IllegalStateException("Unknown StAX event " + event);


            }
    }

    /**
     * Get the event most recently returned by next(), or by other calls that change
     * the position, for example getStringValue() and skipToMatchingEnd(). This
     * method does not change the position of the PullProvider.
     *
     * @return the current event
     */

    public int current() {
        return currentEvent;
    }

    /**
     * Get the attributes associated with the current element. This method must
     * be called only after a START_ELEMENT event has been notified. The contents
     * of the returned AttributeCollection are guaranteed to remain unchanged
     * until the next START_ELEMENT event, but may be modified thereafter. The object
     * should not be modified by the client.
     * <p/>
     * <p>Attributes may be read before or after reading the namespaces of an element,
     * but must not be read after the first child node has been read, or after calling
     * one of the methods skipToEnd(), getStringValue(), or getTypedValue().</p>
     *
     * @return an AttributeCollection representing the attributes of the element
     *         that has just been notified.
     */

    public AttributeCollection getAttributes() throws XPathException {
        return attributes;
    }

    /**
     * Get the namespace declarations associated with the current element. This method must
     * be called only after a START_ELEMENT event has been notified. In the case of a top-level
     * START_ELEMENT event (that is, an element that either has no parent node, or whose parent
     * is not included in the sequence being read), the NamespaceDeclarations object returned
     * will contain a namespace declaration for each namespace that is in-scope for this element
     * node. In the case of a non-top-level element, the NamespaceDeclarations will contain
     * a set of namespace declarations and undeclarations, representing the differences between
     * this element and its parent.
     * <p/>
     * <p>It is permissible for this method to return namespace declarations that are redundant.</p>
     * <p/>
     * <p>The NamespaceDeclarations object is guaranteed to remain unchanged until the next START_ELEMENT
     * event, but may then be overwritten. The object should not be modified by the client.</p>
     * <p/>
     * <p>Namespaces may be read before or after reading the attributes of an element,
     * but must not be read after the first child node has been read, or after calling
     * one of the methods skipToEnd(), getStringValue(), or getTypedValue().</p>*
     */

    public NamespaceDeclarations getNamespaceDeclarations() throws XPathException {
        return namespaces;
    }

    /**
     * Skip the current subtree. This method may be called only immediately after
     * a START_DOCUMENT or START_ELEMENT event. This call returns the matching
     * END_DOCUMENT or END_ELEMENT event; the next call on next() will return
     * the event following the END_DOCUMENT or END_ELEMENT.
     */

    public int skipToMatchingEnd() throws XPathException {
        switch (currentEvent) {
            case START_DOCUMENT:
                currentEvent = END_DOCUMENT;
                return currentEvent;
            case START_ELEMENT:
                try {
                    int skipDepth = 0;
                    while (reader.hasNext()) {
                        int event = reader.next();
                        if (event == XMLStreamConstants.START_ELEMENT) {
                            skipDepth++;
                        } else if (event == XMLStreamConstants.END_ELEMENT) {
                            if (skipDepth-- == 0) {
                                currentEvent = END_ELEMENT;
                                return currentEvent;
                            }
                        }
                    }
                } catch (XMLStreamException e) {
                    throw new DynamicError(e);
                }
                throw new IllegalStateException(
                        "Element start has no matching element end");
            default:
                throw new IllegalStateException(
                        "Cannot call skipToMatchingEnd() except when at start of element or document");

        }
    }

    /**
     * Close the event reader. This indicates that no further events are required.
     * It is not necessary to close an event reader after {@link #END_OF_INPUT} has
     * been reported, but it is recommended to close it if reading terminates
     * prematurely. Once an event reader has been closed, the effect of further
     * calls on next() is undefined.
     */

    public void close() {
        try {
            reader.close();
        } catch (XMLStreamException e) {
            //
        }
    }

    /**
     * Get the nameCode identifying the name of the current node. This method
     * can be used after the {@link #START_ELEMENT}, {@link #PROCESSING_INSTRUCTION},
     * {@link #ATTRIBUTE}, or {@link #NAMESPACE} events. With some PullProvider implementations,
     * including this one, it can also be used after {@link #END_ELEMENT}.
     * If called at other times, the result is undefined and may result in an IllegalStateException.
     * If called when the current node is an unnamed namespace node (a node representing the default namespace)
     * the returned value is -1.
     *
     * @return the nameCode. The nameCode can be used to obtain the prefix, local name,
     *         and namespace URI from the name pool.
     */

    public int getNameCode() {
        if (currentEvent == START_ELEMENT || currentEvent == END_ELEMENT) {
            String local = reader.getLocalName();
            String uri = reader.getNamespaceURI();
            String prefix = reader.getPrefix();
            if (prefix==null) {
                prefix = "";
            }
            if (uri == null) {
                uri = "";
            }
            return getNamePool().allocate(prefix, uri, local);
            //TODO: keep a local cache as in ReceivingContentHandler
        } else if (currentEvent == PROCESSING_INSTRUCTION) {
            String local = reader.getPITarget();
            return getNamePool().allocate("", "", local);
        } else {
            throw new IllegalStateException();
        }
    }

    /**
     * Get the fingerprint of the name of the element. This is similar to the nameCode, except that
     * it does not contain any information about the prefix: so two elements with the same fingerprint
     * have the same name, excluding prefix. This method
     * can be used after the {@link #START_ELEMENT}, {@link #PROCESSING_INSTRUCTION},
     * {@link #ATTRIBUTE}, or {@link #NAMESPACE} events.
     * If called at other times, the result is undefined and may result in an IllegalStateException.
     * If called when the current node is an unnamed namespace node (a node representing the default namespace)
     * the returned value is -1.
     *
     * @return the fingerprint. The fingerprint can be used to obtain the local name
     *         and namespace URI from the name pool.
     */

    public int getFingerprint() {
        int nc = getNameCode();
        if (nc == -1) {
            return -1;
        } else {
            return nc & NamePool.FP_MASK;
        }
    }

    /**
     * Get the string value of the current element, text node, processing-instruction,
     * or top-level attribute or namespace node, or atomic value.
     * <p/>
     * <p>In other situations the result is undefined and may result in an IllegalStateException.</p>
     * <p/>
     * <p>If the most recent event was a {@link #START_ELEMENT}, this method causes the content
     * of the element to be read. The current event on completion of this method will be the
     * corresponding {@link #END_ELEMENT}. The next call of next() will return the event following
     * the END_ELEMENT event.</p>
     *
     * @return the String Value of the node in question, defined according to the rules in the
     *         XPath data model.
     */

    public CharSequence getStringValue() throws XPathException {
        switch (currentEvent) {
            case TEXT:
                CharSlice cs = new CharSlice(reader.getTextCharacters(), reader.getTextStart(), reader.getTextLength());
                return CompressedWhitespace.compress(cs);

            case COMMENT:
                return new CharSlice(reader.getTextCharacters(), reader.getTextStart(), reader.getTextLength());

            case PROCESSING_INSTRUCTION:
                String s = reader.getPIData();
                // The BEA parser includes the separator space in the value,
                // which isn't part of the XPath data model
                return Whitespace.removeLeadingWhitespace(s);

            case START_ELEMENT:
                FastStringBuffer combinedText = null;
                try {
                    int depth = 0;
                    while (reader.hasNext()) {
                        int event = reader.next();
                        if (event == XMLStreamConstants.CHARACTERS) {
                            if (combinedText == null) {
                                combinedText = new FastStringBuffer(1024);
                            }
                            combinedText.append(
                                        reader.getTextCharacters(), reader.getTextStart(), reader.getTextLength());
                        } else if (event == XMLStreamConstants.START_ELEMENT) {
                            depth++;
                        } else if (event == XMLStreamConstants.END_ELEMENT) {
                            if (depth-- == 0) {
                                currentEvent = END_ELEMENT;
                                if (combinedText != null) {
                                    return combinedText.condense();
                                } else {
                                    return "";
                                }
                            }
                        }
                    }
                } catch (XMLStreamException e) {
                    throw new DynamicError(e);
                }
            default:
                throw new IllegalStateException("getStringValue() called when current event is " + currentEvent);

        }
    }

    /**
     * Get an atomic value. This call may be used only when the last event reported was
     * ATOMIC_VALUE. This indicates that the PullProvider is reading a sequence that contains
     * a free-standing atomic value; it is never used when reading the content of a node.
     */

    public AtomicValue getAtomicValue() {
        throw new IllegalStateException();
    }

    /**
     * Get the location of the current event. The location is returned as an integer.
     * This is a value that can be passed to the {@link org.orbeon.saxon.event.LocationProvider}
     * held by the {@link org.orbeon.saxon.event.PipelineConfiguration} to get real location information (line number,
     * system Id, etc). For an event stream representing a real document, the location information
     * should identify the location in the lexical XML source. For a constructed document, it should
     * identify the location in the query or stylesheet that caused the node to be created.
     * A value of zero can be returned if no location information is available.
     */

    public int getLocationId() {
        return 0;
    }

    /**
     * Get the type annotation of the current attribute or element node, or atomic value.
     * The result of this method is undefined unless the most recent event was START_ELEMENT,
     * ATTRIBUTE, or ATOMIC_VALUE.
     *
     * @return the type annotation. This code is the fingerprint of a type name, which may be
     *         resolved to a {@link org.orbeon.saxon.type.SchemaType} by access to the Configuration.
     */

    public int getTypeAnnotation() {
        return -1;
    }

    /**
     * Get the location of the current event.
     * For an event stream representing a real document, the location information
     * should identify the location in the lexical XML source. For a constructed document, it should
     * identify the location in the query or stylesheet that caused the node to be created.
     * A value of null can be returned if no location information is available.
     */

    public SourceLocator getSourceLocator() {
        return translateLocation(reader.getLocation());
    }

    /**
     * Translate a StAX Location object to a Saxon Locator
     */

    private ExpressionLocation translateLocation(Location location) {
        ExpressionLocation loc = new ExpressionLocation();
        if (location != null) {
            loc.setLineNumber(location.getLineNumber());
            loc.setColumnNumber(location.getColumnNumber());
            loc.setSystemId(location.getSystemId());
            loc.setPublicId(location.getPublicId());
        }
        return loc;
    }

    /**
     * Implement the Saxon AttributeCollection interface over the StAX interface.
     */

    private class StaxAttributes implements AttributeCollection {

        /**
         * Set the location provider. This must be set if the methods getSystemId() and getLineNumber()
         * are to be used to get location information for an attribute.
         */

        public void setLocationProvider(LocationProvider provider) {

        }

        /**
         * Return the number of attributes in the list.
         *
         * @return The number of attributes in the list.
         */

        public int getLength() {
            return reader.getAttributeCount();
        }

        /**
         * Get the namecode of an attribute (by position).
         *
         * @param index The position of the attribute in the list.
         * @return The namecode of the attribute
         */

        public int getNameCode(int index) {
            String local = reader.getAttributeLocalName(index);
            String uri = reader.getAttributeNamespace(index);
            String prefix = reader.getAttributePrefix(index);
            if (prefix == null) {
                prefix = "";
            }
            if (uri == null) {
                uri = "";
            }
            return getNamePool().allocate(prefix, uri, local);
            // TODO: the JavaDoc for XMLStreamReader doesn't say what happens if index is out of range.
            // The interface definition for PullProvider states that null/-1 is returned.
        }

        /**
         * Get the type annotation of an attribute (by position).
         *
         * @param index The position of the attribute in the list.
         * @return The type annotation, as the fingerprint of the type name.
         * The bit {@link org.orbeon.saxon.om.NodeInfo.IS_DTD_TYPE} represents a DTD-derived type.
         */

        public int getTypeAnnotation(int index) {
            String type = reader.getAttributeType(index);
            if ("ID".equals(type)) {
                return StandardNames.XS_ID | NodeInfo.IS_DTD_TYPE;
            }
            return StandardNames.XDT_UNTYPED_ATOMIC;
        }

        /**
         * Get the locationID of an attribute (by position)
         *
         * @param index The position of the attribute in the list.
         * @return The location identifier of the attribute. This can be supplied
         *         to a {@link org.orbeon.saxon.event.LocationProvider} in order to obtain the
         *         actual system identifier and line number of the relevant location
         */

        public int getLocationId(int index) {
            return 0;
        }

        /**
         * Get the systemId part of the location of an attribute, at a given index.
         * <p/>
         * <p>Attribute location information is not available from a SAX parser, so this method
         * is not useful for getting the location of an attribute in a source document. However,
         * in a Saxon result document, the location information represents the location in the
         * stylesheet of the instruction used to generate this attribute, which is useful for
         * debugging.</p>
         *
         * @param index the required attribute
         * @return the systemId of the location of the attribute
         */

        public String getSystemId(int index) {
            return reader.getLocation().getSystemId();
        }

        /**
         * Get the line number part of the location of an attribute, at a given index.
         * <p/>
         * <p>Attribute location information is not available from a SAX parser, so this method
         * is not useful for getting the location of an attribute in a source document. However,
         * in a Saxon result document, the location information represents the location in the
         * stylesheet of the instruction used to generate this attribute, which is useful for
         * debugging.</p>
         *
         * @param index the required attribute
         * @return the line number of the location of the attribute
         */

        public int getLineNumber(int index) {
            return reader.getLocation().getLineNumber();
        }

        /**
         * Get the properties of an attribute (by position)
         *
         * @param index The position of the attribute in the list.
         * @return The properties of the attribute. This is a set
         *         of bit-settings defined in class {@link org.orbeon.saxon.event.ReceiverOptions}. The
         *         most interesting of these is {{@link org.orbeon.saxon.event.ReceiverOptions#DEFAULTED_ATTRIBUTE},
         *         which indicates an attribute that was added to an element as a result of schema validation.
         */

        public int getProperties(int index) {
            int properties = 0;
            if (!reader.isAttributeSpecified(index)) {
                properties |= ReceiverOptions.DEFAULTED_ATTRIBUTE;
            }
            if (isIdref(index)) {
                properties |= (ReceiverOptions.IS_IDREF | ReceiverOptions.ID_IDREF_CHECKED);
            }
            return properties;
        }

        /**
         * Get the prefix of the name of an attribute (by position).
         *
         * @param index The position of the attribute in the list.
         * @return The prefix of the attribute name as a string, or null if there
         *         is no attribute at that position. Returns "" for an attribute that
         *         has no prefix.
         */

        public String getPrefix(int index) {
            return getNamePool().getPrefix(getNameCode(index));
        }

        /**
         * Get the lexical QName of an attribute (by position).
         *
         * @param index The position of the attribute in the list.
         * @return The lexical QName of the attribute as a string, or null if there
         *         is no attribute at that position.
         */

        public String getQName(int index) {
            return getNamePool().getDisplayName(getNameCode(index));
        }

        /**
         * Get the local name of an attribute (by position).
         *
         * @param index The position of the attribute in the list.
         * @return The local name of the attribute as a string, or null if there
         *         is no attribute at that position.
         */

        public String getLocalName(int index) {
            return reader.getAttributeLocalName(index);
        }

        /**
         * Get the namespace URI of an attribute (by position).
         *
         * @param index The position of the attribute in the list.
         * @return The local name of the attribute as a string, or null if there
         *         is no attribute at that position.
         */

        public String getURI(int index) {
            return reader.getAttributeNamespace(index);
        }

        /**
         * Get the index of an attribute (by name).
         *
         * @param uri       The namespace uri of the attribute.
         * @param localname The local name of the attribute.
         * @return The index position of the attribute
         */

        public int getIndex(String uri, String localname) {
            for (int i=0; i<getLength(); i++) {
                if (getLocalName(i) == localname && getURI(i) == uri) {
                    return i;
                }
            }
            return -1;
        }

        /**
         * Get the index, given the fingerprint
         */

        public int getIndexByFingerprint(int fingerprint) {
            return getIndex(getNamePool().getURI(fingerprint), getNamePool().getLocalName(fingerprint));
        }

        /**
         * Get the attribute value using its fingerprint
         */

        public String getValueByFingerprint(int fingerprint) {
            return getValue(getIndexByFingerprint(fingerprint));
        }

        /**
         * Get the value of an attribute (by name).
         *
         * @param uri       The namespace uri of the attribute.
         * @param localname The local name of the attribute.
         * @return The index position of the attribute
         */

        public String getValue(String uri, String localname) {
            return reader.getAttributeValue(uri, localname);
        }

        /**
         * Get the value of an attribute (by position).
         *
         * @param index The position of the attribute in the list.
         * @return The attribute value as a string, or null if
         *         there is no attribute at that position.
         */

        public String getValue(int index) {
            return reader.getAttributeValue(index);
        }

        /**
         * Determine whether a given attribute has the is-ID property set
         */

        public boolean isId(int index) {
            return reader.getAttributeType(index).equals("ID");
        }

        /**
         * Determine whether a given attribute has the is-idref property set
         */

        public boolean isIdref(int index) {
            String attributeType = reader.getAttributeType(index);
            return attributeType.equals("IDREF") || attributeType.equals("IDREFS");
        }
    }

    public class StaxNamespaces implements NamespaceDeclarations {
        /**
         * Get the number of declarations (and undeclarations) in this list.
         */

        public int getNumberOfNamespaces() {
            return reader.getNamespaceCount();
        }

        /**
         * Get the prefix of the n'th declaration (or undeclaration) in the list,
         * counting from zero.
         *
         * @param index the index identifying which declaration is required.
         * @return the namespace prefix. For a declaration or undeclaration of the
         *         default namespace, this is the zero-length string.
         * @throws IndexOutOfBoundsException if the index is out of range.
         */

        public String getPrefix(int index) {
            String p = reader.getNamespacePrefix(index);
            return (p==null ? "" : p);
        }

        /**
         * Get the namespace URI of the n'th declaration (or undeclaration) in the list,
         * counting from zero.
         *
         * @param index the index identifying which declaration is required.
         * @return the namespace URI. For a namespace undeclaration, this is the
         *         zero-length string.
         * @throws IndexOutOfBoundsException if the index is out of range.
         */

        public String getURI(int index) {
            String uri = reader.getNamespaceURI(index);
            return (uri==null ? "" : uri);
        }

        /**
         * Get the n'th declaration in the list in the form of a namespace code. Namespace
         * codes can be translated into a prefix and URI by means of methods in the
         * NamePool
         *
         * @param index the index identifying which declaration is required.
         * @return the namespace code. This is an integer whose upper half indicates
         *         the prefix (0 represents the default namespace), and whose lower half indicates
         *         the URI (0 represents an undeclaration).
         * @throws IndexOutOfBoundsException if the index is out of range.
         * @see org.orbeon.saxon.om.NamePool#getPrefixFromNamespaceCode(int)
         * @see org.orbeon.saxon.om.NamePool#getURIFromNamespaceCode(int)
         */

        public int getNamespaceCode(int index) {
            return getNamePool().allocateNamespaceCode(getPrefix(index), getURI(index));
        }

        /**
         * Get all the namespace codes, as an array.
         *
         * @param buffer a sacrificial array that the method is free to use to contain the result.
         *               May be null.
         * @return an integer array containing namespace codes. The array may be filled completely
         *         with namespace codes, or it may be incompletely filled, in which case a -1 integer acts
         *         as a terminator.
         */

        public int[] getNamespaceCodes(int[] buffer) {
            if (buffer.length < getNumberOfNamespaces()) {
                buffer = new int[getNumberOfNamespaces()];
            }
            for (int i=0; i<getNumberOfNamespaces(); i++) {
                buffer[i] = getNamespaceCode(i);
            }
            return buffer;
        }

    }

    /**
     * Return the public identifier for the current document event.
     * <p/>
     * <p>The return value is the public identifier of the document
     * entity or of the external parsed entity in which the markup
     * triggering the event appears.</p>
     *
     * @return A string containing the public identifier, or
     *         null if none is available.
     * @see #getSystemId
     */
    public String getPublicId() {
        return reader.getLocation().getPublicId();
    }

    /**
     * Return the system identifier for the current document event.
     * <p/>
     * <p>The return value is the system identifier of the document
     * entity or of the external parsed entity in which the markup
     * triggering the event appears.</p>
     * <p/>
     * <p>If the system identifier is a URL, the parser must resolve it
     * fully before passing it to the application.  For example, a file
     * name must always be provided as a <em>file:...</em> URL, and other
     * kinds of relative URI are also resolved against their bases.</p>
     *
     * @return A string containing the system identifier, or null
     *         if none is available.
     * @see #getPublicId
     */
    public String getSystemId() {
        return reader.getLocation().getSystemId();
    }

    /**
     * Return the line number where the current document event ends.
     * Lines are delimited by line ends, which are defined in
     * the XML specification.
     * <p/>
     * <p><strong>Warning:</strong> The return value from the method
     * is intended only as an approximation for the sake of diagnostics;
     * it is not intended to provide sufficient information
     * to edit the character content of the original XML document.
     * In some cases, these "line" numbers match what would be displayed
     * as columns, and in others they may not match the source text
     * due to internal entity expansion.  </p>
     * <p/>
     * <p>The return value is an approximation of the line number
     * in the document entity or external parsed entity where the
     * markup triggering the event appears.</p>
     * <p/>
     * <p>If possible, the SAX driver should provide the line position
     * of the first character after the text associated with the document
     * event.  The first line is line 1.</p>
     *
     * @return The line number, or -1 if none is available.
     * @see #getColumnNumber
     */
    public int getLineNumber() {
        return reader.getLocation().getLineNumber();
    }

    /**
     * Return the column number where the current document event ends.
     * This is one-based number of Java <code>char</code> values since
     * the last line end.
     * <p/>
     * <p><strong>Warning:</strong> The return value from the method
     * is intended only as an approximation for the sake of diagnostics;
     * it is not intended to provide sufficient information
     * to edit the character content of the original XML document.
     * For example, when lines contain combining character sequences, wide
     * characters, surrogate pairs, or bi-directional text, the value may
     * not correspond to the column in a text editor's display. </p>
     * <p/>
     * <p>The return value is an approximation of the column number
     * in the document entity or external parsed entity where the
     * markup triggering the event appears.</p>
     * <p/>
     * <p>If possible, the SAX driver should provide the line position
     * of the first character after the text associated with the document
     * event.  The first column in each line is column 1.</p>
     *
     * @return The column number, or -1 if none is available.
     * @see #getLineNumber
     */
    public int getColumnNumber() {
        return reader.getLocation().getColumnNumber();
    }

    public String getSystemId(int locationId) {
        return getSystemId();
    }

    public int getLineNumber(int locationId) {
        return getLineNumber();
    }

    /**
     * Get a list of unparsed entities.
     *
     * @return a list of unparsed entities, or null if the information is not available, or
     *         an empty list if there are no unparsed entities. Each item in the list will
     *         be an instance of {@link org.orbeon.saxon.pull.UnparsedEntity}
     */

    public List getUnparsedEntities() {
        if (unparsedEntities == null) {
            return null;
        }
        List list = new ArrayList(unparsedEntities.size());
        for (int i=0; i<unparsedEntities.size(); i++) {
            Object ent = unparsedEntities.get(i);
            String name = null;
            String systemId = null;
            String publicId = null;
            String baseURI = null;
            if (ent instanceof EntityDeclaration) {
                // This is what we would expect from the StAX API spec
                EntityDeclaration ed = (EntityDeclaration)ent;
                name = ed.getName();
                systemId = ed.getSystemId();
                publicId = ed.getPublicId();
                baseURI = ed.getBaseURI();
            } else if (ent.getClass().getName().equals("com.ctc.wstx.ent.UnparsedExtEntity")) {
                // Woodstox 3.0.0 returns this: use introspection to get the data we need
                try {
                    Class woodstoxClass = ent.getClass();
                    Class[] noArgs = new Class[0];
                    Method method = woodstoxClass.getMethod("getName", noArgs);
                    name = (String)method.invoke(ent, (Object[]) noArgs);
                    method = woodstoxClass.getMethod("getSystemId", noArgs);
                    systemId = (String)method.invoke(ent, (Object[]) noArgs);
                    method = woodstoxClass.getMethod("getPublicId", noArgs);
                    publicId = (String)method.invoke(ent, (Object[]) noArgs);
                    method = woodstoxClass.getMethod("getBaseURI", noArgs);
                    baseURI = (String)method.invoke(ent, (Object[]) noArgs);
                } catch (NoSuchMethodException e) {
                    //
                } catch (IllegalAccessException e) {
                    //
                } catch (InvocationTargetException e) {
                    //
                }
            }
            if (name != null) {
                try {
                    systemId = new URI(baseURI).resolve(systemId).toString();
                } catch (URISyntaxException err) {
                    //
                }
                UnparsedEntity ue = new UnparsedEntity();
                ue.setName(name);
                ue.setSystemId(systemId);
                ue.setPublicId(publicId);
                ue.setBaseURI(baseURI);
                list.add(ue);
            }
        }
        return list;
    }

    /**
     * Error reporting class for StAX parser errors
     */

    private class StaxErrorReporter implements XMLReporter {

        public void report(String message, String errorType,
                           Object relatedInformation, Location location)
                throws XMLStreamException {
            ExpressionLocation loc = translateLocation(location);
            DynamicError err = new DynamicError("Error reported by XML parser: " + message + " (" + errorType + ')');
            err.setLocator(loc);
            try {
                pipe.getErrorListener().error(err);
            } catch (TransformerException e) {
                throw new XMLStreamException(e);
            }
        }

    }

    /**
     * Simple test program
     * Usage: java StaxBridge in.xml [out.xml]
     */

    public static void main(String[] args) throws Exception {
        for (int i=0; i<1; i++) {
            long startTime = System.currentTimeMillis();
            PipelineConfiguration pipe = new Configuration().makePipelineConfiguration();
            StaxBridge puller = new StaxBridge();
            File f = new File(args[0]);
            puller.setInputStream(f.toURI().toString(), new FileInputStream(f));
            XMLEmitter emitter = new XMLEmitter();
            emitter.setPipelineConfiguration(pipe);
            emitter.setOutputProperties(new Properties());
            if (args.length > 1) {
                emitter.setOutputStream(new FileOutputStream(args[1]));
            } else {
                emitter.setOutputStream(System.out);
            }
            NamespaceReducer r = new NamespaceReducer();
            r.setUnderlyingReceiver(emitter);
            puller.setPipelineConfiguration(pipe);
            r.setPipelineConfiguration(pipe);
            new PullPushCopier(puller, r).copy();
            System.err.println("Elapsed time: " + (System.currentTimeMillis() - startTime) + "ms");
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

