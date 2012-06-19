package org.orbeon.saxon.evpull;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.event.*;
import org.orbeon.saxon.expr.ExpressionLocation;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.pull.UnparsedEntity;
import org.orbeon.saxon.tinytree.CharSlice;
import org.orbeon.saxon.trans.SaxonErrorCode;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.Whitespace;

import javax.xml.stream.*;
import javax.xml.stream.events.EntityDeclaration;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * This class implements the Saxon EventIterator API on top of a standard StAX parser
 * (or any other StAX XMLStreamReader implementation)
 */

public class StaxToEventBridge implements EventIterator, SaxonLocator, SourceLocationProvider {

    private Configuration config;
    private XMLStreamReader reader;
    private PipelineConfiguration pipe;
    private List unparsedEntities = null;
    PullEvent currentEvent = null;
    int depth = 0;
    boolean ignoreIgnorable = false;

    /**
     * Create a new instance of the class
     */

    public StaxToEventBridge() {

    }

    /**
     * Supply an input stream containing XML to be parsed. A StAX parser is created using
     * the JAXP XMLInputFactory.
     * @param systemId The Base URI of the input document
     * @param inputStream the stream containing the XML to be parsed
     * @throws org.orbeon.saxon.trans.XPathException if an error occurs creating the StAX parser
     */

    public void setInputStream(String systemId, InputStream inputStream) throws XPathException {
        try {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            //XMLInputFactory factory = new WstxInputFactory();
            factory.setXMLReporter(new StaxErrorReporter());
            reader = factory.createXMLStreamReader(systemId, inputStream);
        } catch (XMLStreamException e) {
            throw new XPathException(e);
        }
    }

    /**
     * Supply an XMLStreamReader: the events reported by this XMLStreamReader will be translated
     * into EventIterator events
     * @param reader the supplier of XML events, typically an XML parser
     */

    public void setXMLStreamReader(XMLStreamReader reader) {
        this.reader = reader;
    }

    /**
     * Set configuration information. This must only be called before any events
     * have been read.
     * @param pipe the pipeline configuration
     */

    public void setPipelineConfiguration(PipelineConfiguration pipe) {
        this.pipe = new PipelineConfiguration(pipe);
        this.pipe.setLocationProvider(this);
        config = pipe.getConfiguration();
        ignoreIgnorable = config.getStripsWhiteSpace() != Whitespace.NONE;
    }

    /**
     * Get configuration information.
     * @return the pipeline configuration
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
     * @return the name pool
     */

    public NamePool getNamePool() {
        return pipe.getConfiguration().getNamePool();
    }

    /**
     * Get the next event
     * @return the next event; or null to indicate the end of the event stream
     */

    public PullEvent next() throws XPathException {
        if (currentEvent == null) {
            // StAX isn't reporting START_DOCUMENT so we supply it ourselves
            currentEvent = StartDocumentEvent.getInstance();
            return currentEvent;
        }
        if (currentEvent instanceof EndDocumentEvent) {
            try {
                reader.close();
            } catch (XMLStreamException e) {
                //
            }
            return null;
        }
        try {
            if (reader.hasNext()) {
                int event = reader.next();
                //System.err.println("Read event " + event);
                currentEvent = translate(event);
            } else {
                currentEvent = null;
            }
        } catch (XMLStreamException e) {
            String message = e.getMessage();
            // Following code recognizes the messages produced by the Sun Zephyr parser
            if (message.startsWith("ParseError at")) {
                int c = message.indexOf("\nMessage: ");
                if (c > 0) {
                    message = message.substring(c + 10);
                }
            }
            XPathException err = new XPathException("Error reported by XML parser: " + message);
            err.setErrorCode(SaxonErrorCode.SXXP0003);
            err.setLocator(translateLocation(e.getLocation()));
            throw err;
        }
        return currentEvent;
    }

    /**
     * Translate a StAX event into a Saxon PullEvent
     * @param event the StAX event
     * @return the Saxon PullEvent
     * @throws XPathException
     */

    private PullEvent translate(int event) throws XPathException {
            //System.err.println("EVENT " + event);
            switch (event) {
                case XMLStreamConstants.ATTRIBUTE:
                    return next();          // attributes are reported as part of StartElement
                case XMLStreamConstants.CDATA:
                case XMLStreamConstants.CHARACTERS:
                    if (depth == 0 && reader.isWhiteSpace()) {
                        return next();
                    } else {
                        Orphan o = new Orphan(config);
                        o.setNodeKind(Type.TEXT);
                        CharSlice value = new CharSlice(
                            reader.getTextCharacters(), reader.getTextStart(), reader.getTextLength());
                        o.setStringValue(value);
                        return o;
                    }
                case XMLStreamConstants.COMMENT: {
                    Orphan o = new Orphan(config);
                    o.setNodeKind(Type.COMMENT);
                    CharSlice value = new CharSlice(
                            reader.getTextCharacters(), reader.getTextStart(), reader.getTextLength());
                    o.setStringValue(value);
                    return o;
                }
                case XMLStreamConstants.DTD:
                    unparsedEntities = (List)reader.getProperty("javax.xml.stream.entities");
                    return next();
                case XMLStreamConstants.END_DOCUMENT:
                    return EndDocumentEvent.getInstance();
                case XMLStreamConstants.END_ELEMENT:
                    depth--;
                    return EndElementEvent.getInstance();
                case XMLStreamConstants.ENTITY_DECLARATION:
                    return next();
                case XMLStreamConstants.ENTITY_REFERENCE:
                    return next();
                case XMLStreamConstants.NAMESPACE:
                    return next();      // namespaces are reported as part of StartElement
                case XMLStreamConstants.NOTATION_DECLARATION:
                    return next();
                case XMLStreamConstants.PROCESSING_INSTRUCTION:{
                    Orphan o = new Orphan(config);
                    o.setNodeKind(Type.PROCESSING_INSTRUCTION);
                    String local = reader.getPITarget();
                    o.setNameCode(getNamePool().allocate("", "", local));
                    o.setStringValue(reader.getText());
                    return o;
                }
                case XMLStreamConstants.SPACE:
                    if (depth == 0) {
                        return next();
                    } else if (ignoreIgnorable) {
                        // (Brave attempt, but Woodstox doesn't seem to report ignorable whitespace)
                        return next();
                    } else {
                        Orphan o = new Orphan(config);
                        o.setNodeKind(Type.TEXT);
                        o.setStringValue(reader.getText());
                        return o;
                    }
                case XMLStreamConstants.START_DOCUMENT:
                    return next();  // we supplied the START_DOCUMENT ourselves
                case XMLStreamConstants.START_ELEMENT:
                    depth++;
                    StartElementEvent see = new StartElementEvent(pipe);
                    String elocal = reader.getLocalName();
                    String euri = reader.getNamespaceURI();
                    String eprefix = reader.getPrefix();
                    if (eprefix == null) {
                        eprefix = "";
                    }
                    if (euri == null) {
                        euri = "";
                    }
                    int enc = getNamePool().allocate(eprefix, euri, elocal);
                    see.setNameCode(enc);
                    see.setTypeCode(StandardNames.XS_UNTYPED);
                    int attCount = reader.getAttributeCount();
                    for (int index=0; index<attCount; index++) {
                        String local = reader.getAttributeLocalName(index);
                        String uri = reader.getAttributeNamespace(index);
                        String prefix = reader.getAttributePrefix(index);
                        if (prefix == null) {
                            prefix = "";
                        }
                        if (uri == null) {
                            uri = "";
                        }
                        int nc = getNamePool().allocate(prefix, uri, local);
                        Orphan o = new Orphan(config);
                        o.setNodeKind(Type.ATTRIBUTE);
                        o.setNameCode(nc);
                        o.setStringValue(reader.getAttributeValue(index));
                        see.addAttribute(o);
                    }
                    see.namespaceFixup();
                    return see;
                default:
                    throw new IllegalStateException("Unknown StAX event " + event);


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

    public String getSystemId(long locationId) {
        return getSystemId();
    }

    public int getLineNumber(long locationId) {
        return getLineNumber();
    }

    public int getColumnNumber(long locationId) {
        return getColumnNumber();
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
      * Translate a StAX Location object to a Saxon Locator
      * @param location the StAX Location object
      * @return a Saxon/SAX SourceLocator object
      */

     private ExpressionLocation translateLocation(Location location) {
         ExpressionLocation loc = new ExpressionLocation();
         if (location != null) {
             loc.setLineNumber(location.getLineNumber());
             loc.setColumnNumber(location.getColumnNumber());
             loc.setSystemId(location.getSystemId());
             //loc.setPublicId(location.getPublicId());
         }
         return loc;
     }


    /**
     * Error reporting class for StAX parser errors
     */

    private class StaxErrorReporter implements XMLReporter {

        public void report(String message, String errorType,
                           Object relatedInformation, Location location)
                throws XMLStreamException {
            ExpressionLocation loc = translateLocation(location);
            XPathException err = new XPathException("Error reported by XML parser: " + message + " (" + errorType + ')');
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
     * @param args command line arguments
     */

    public static void main(String[] args) throws Exception {
        for (int i=0; i<1; i++) {
            long startTime = System.currentTimeMillis();
            PipelineConfiguration pipe = new Configuration().makePipelineConfiguration();
            StaxToEventBridge puller = new StaxToEventBridge();
            File f = new File(args[0]);
            puller.setInputStream(f.toURI().toString(), new FileInputStream(f));
            puller.setPipelineConfiguration(pipe);
            XMLEmitter emitter = new XMLEmitter();
            emitter.setPipelineConfiguration(pipe);
            emitter.setOutputProperties(new Properties());
            if (args.length > 1) {
                emitter.setOutputStream(new FileOutputStream(args[1]));
            } else {
                emitter.setOutputStream(System.out);
            }
            NamespaceReducer r = new NamespaceReducer(emitter);

            EventIteratorToReceiver.copy(puller, r);
            System.err.println("Elapsed time: " + (System.currentTimeMillis() - startTime) + "ms");
        }
    }


    /**
     * Determine whether the EventIterator returns a flat sequence of events, or whether it can return
     * nested event iterators
     *
     * @return true if the next() method is guaranteed never to return an EventIterator
     */

    public boolean isFlatSequence() {
        return false; 
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

