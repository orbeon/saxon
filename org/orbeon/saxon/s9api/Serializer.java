package org.orbeon.saxon.s9api;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.Controller;
import org.orbeon.saxon.event.PipelineConfiguration;
import org.orbeon.saxon.event.Receiver;
import org.orbeon.saxon.event.SaxonOutputKeys;
import org.orbeon.saxon.event.SerializerFactory;
import org.orbeon.saxon.om.Name11Checker;
import org.orbeon.saxon.trans.XPathException;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * A Serializer takes a tree representation of XML and turns it into lexical XML markup.
 *
 * <p><i>Note that this is XML serialization in the sense of the W3C XSLT and XQuery specifications.
 * This has nothing to do with the serialization of Java objects, or the {@link java.io.Serializable}
 * interface.</i></p>
 *
 * <p>The serialization may be influenced by a number of serialization parameters. A parameter has a name,
 * which is an instance of {@link Serializer.Property}, and a value, which is expressed as a string.
 * The effect of most of the properties is as described in the W3C specification
 * <a href="http://www.w3.org/TR/xslt-xquery-serialization/">XSLT 2.0 and XQuery 1.0 Serialization</a>.
 * Saxon supports all the serialization parameters defined in that specification, together with some
 * additional parameters, whose property names are prefixed "SAXON_".
 */
@SuppressWarnings({"ForeachStatement"})
public class Serializer implements Destination {

    private Map<Property, String> properties = new HashMap<Property, String>(10);
    private StreamResult result = new StreamResult();

    public enum Property {
        /**
         * Serialization method: xml, html, xhtml, or text
         */
        METHOD                  (OutputKeys.METHOD),
        /**
         * Version of output method, for example "1.0" or "1.1" for XML
         */
        VERSION                 (OutputKeys.VERSION),
        /**
         * Character encoding of output stream
         */
        ENCODING                (OutputKeys.ENCODING),
        /**
         * Set to "yes" if the XML declaration is to be omitted from the output file
         */
        OMIT_XML_DECLARATION    (OutputKeys.OMIT_XML_DECLARATION),
        /**
         * Set to "yes", "no", or "omit" to indicate the required value of the standalone attribute
         * in the XML declaration of the output file
         */
        STANDALONE              (OutputKeys.STANDALONE),
        /**
         * Set to any string to indicate that the output is to include a DOCTYPE declaration with this public id
         */
        DOCTYPE_PUBLIC          (OutputKeys.DOCTYPE_PUBLIC),
        /**
         * Set to any string to indicate that the output is to include a DOCTYPE declaration with this system id
         */
        DOCTYPE_SYSTEM          (OutputKeys.DOCTYPE_SYSTEM),
        /**
         * Space-separated list of QNames (in Clark form) of elements
         * whose content is to be wrapped in CDATA sections
         */
        CDATA_SECTION_ELEMENTS  (OutputKeys.CDATA_SECTION_ELEMENTS),
        /**
         * Set to "yes" or "no" to indicate whether indentation is required
         */
        INDENT                  (OutputKeys.INDENT),
        /**
         * Set to indicate the media type (MIME type) of the output
         */
        MEDIA_TYPE              (OutputKeys.MEDIA_TYPE),
        /**
         * List of names of character maps to be used. Character maps can only be specified in an XSLT
         * stylesheet.
         */
        USE_CHARACTER_MAPS      (SaxonOutputKeys.USE_CHARACTER_MAPS),
        /**
         * For HTML and XHTML, set to "yes" or "no" to indicate whether a &lt;meta&gt; element is to be
         * written to indicate the content type and encoding
         */
        INCLUDE_CONTENT_TYPE    (SaxonOutputKeys.INCLUDE_CONTENT_TYPE),
        /**
         * Set to "yes" or "no" to indicate (for XML 1.1) whether namespace that go out of scope should
         * be undeclared
         */
        UNDECLARE_PREFIXES      (SaxonOutputKeys.UNDECLARE_PREFIXES),
        /**
         * Set to "yes" or "no" to indicate (for HTML and XHTML) whether URI-valued attributes should be
         * percent-encoded
         */
        ESCAPE_URI_ATTRIBUTES   (SaxonOutputKeys.ESCAPE_URI_ATTRIBUTES),
        /**
         * Set to "yes" or "no" to indicate whether a byte order mark is to be written
         */
        BYTE_ORDER_MARK         (SaxonOutputKeys.BYTE_ORDER_MARK),
        /**
         * Set to the name of a Unicode normalization form: "NFC", "NFD", "NFKC", or "NFKD", or
         * "none" to indicate no normalization
         */
        NORMALIZATION_FORM      (SaxonOutputKeys.NORMALIZATION_FORM),

        /**
         * Saxon extension: set to an integer (represented as a string) giving the number of spaces
         * by which each level of nesting should be indented. Default is 3.
         */
        SAXON_INDENT_SPACES                 (SaxonOutputKeys.INDENT_SPACES),
        /**
         * Saxon extension: set to a space-separated list of element names, in Clark notation,
         * within which no content is to be indented. This is typically because the element contains
         * mixed content in which whitespace is significant.
         */
        SAXON_SUPPRESS_INDENTATION          (SaxonOutputKeys.SUPPRESS_INDENTATION),
        /**
         * Saxon extension: set to a space-separated list of element names, in Clark notation,
         * representing elements that will be preceded by an extra blank line in the output in addition
         * to normal indentation.
         */
        SAXON_DOUBLE_SPACE                  (SaxonOutputKeys.DOUBLE_SPACE),
        /**
         * Saxon extension for internal use: used in XSLT to tell the serializer whether the
         * stylesheet used version="1.0" or version="2.0"
         */
        SAXON_STYLESHEET_VERSION            (SaxonOutputKeys.STYLESHEET_VERSION),
        /**
         * Saxon extension to indicate how characters outside the encoding should be represented,
         * for example "hex" for hexadecimal character references, "decimal" for decimal character references
         */
        SAXON_CHARACTER_REPRESENTATION      (SaxonOutputKeys.CHARACTER_REPRESENTATION),
        /**
         * Saxon extension to indicate that output should not be serialized, but should be further transformed.
         * The property gives the relative URI of a stylesheet to be applied. Note that the {@link Serializer}
         * class does not recognize this property.
         */
        SAXON_NEXT_IN_CHAIN                 (SaxonOutputKeys.NEXT_IN_CHAIN),
        /**
         * Saxon extension, indicate the base URI against which {@link Property#SAXON_NEXT_IN_CHAIN} should be
         * resolved.
         */
        SAXON_NEXT_IN_CHAIN_BASE_URI        (SaxonOutputKeys.NEXT_IN_CHAIN_BASE_URI),
        /**
         * Saxon extension for use when output is sent to a SAX ContentHandler: indicates that the output
         * is required to be well-formed (exactly one top-level element)
         */
        SAXON_REQUIRE_WELL_FORMED           (SaxonOutputKeys.REQUIRE_WELL_FORMED),
        /**
         * Saxon extension, indicates that the output of a query is to be wrapped before serialization,
         * such that each item in the result sequence is enclosed in an element indicating its type
         */
        SAXON_WRAP                          (SaxonOutputKeys.WRAP),
        /**
         * Saxon extension for internal use in XSLT, indicates that this output document is the implicitly
         * created result tree as distinct from a tree created using &lt;xsl:result-document&gt;
         */
        SAXON_IMPLICIT_RESULT_DOCUMENT      (SaxonOutputKeys.IMPLICIT_RESULT_DOCUMENT),
        /**
         * Saxon extension for interfacing with debuggers; indicates that the location information is
         * available for events in this output stream
         */
        SAXON_SUPPLY_SOURCE_LOCATOR         (SaxonOutputKeys.SUPPLY_SOURCE_LOCATOR);

        private String name;

        private Property(String name) {
            this.name = name;
        }

        /**
         * Get the name of the property expressed as a QName in Clark notation.
         * The namespace will be null for standard serialization properties,
         * and will be the Saxon namespace <code>http://saxon.sf.net/</code> for Saxon extensions
         * @return the name of the serialization property as a QName in Clark notation, {uri}local
         */

        public String toString() {
            return name;
        }
    }

    /**
     * Set the value of a serialization property. Any existing value of the property is overridden.
     * If the supplied value is null, any existing value of the property is removed.
     *
     * <p>Example:</p>
     * <p><code>serializer.setOutputProperty(Serializer.Property.METHOD, "xml");</code></p>
     *
     * <p>Any serialization properties supplied via this interface take precedence over serialization
     * properties defined in the source stylesheet or query.</p>
     *
     * @param property The name of the property to be set
     * @param value The value of the property, as a string. The format is generally as defined
     * in the <code>xsl:output</code> declaration in XSLT: this means that boolean properties, for
     * example, are represented using the strings "yes" and "no". Properties whose values are QNames,
     * such as <code>cdata-section-elements</code> are expressed using the Clark representation of
     * a QName, that is "{uri}local". Multi-valued properties (again, <code>cdata-section-elements</code>
     * is an example) are expressed as a space-separated list.
     * @throws IllegalArgumentException if the value of the property is invalid. The property is
     * validated individually; invalid combinations of properties will be detected only when the properties
     * are actually used to serialize an XML event stream.
     */

    public void setOutputProperty(Property property, String value) {
        try {
            SaxonOutputKeys.checkOutputProperty(property.toString(), value, Name11Checker.getInstance());
        } catch (XPathException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
        if (value == null) {
            properties.remove(property);
        } else {
            properties.put(property, value);
        }
    }

    /**
     * Get the value of a serialization property
     * @param property the name of the required property
     * @return the value of the required property as a string, or null if the property has
     * not been given any value.
     */

    public String getOutputProperty(Property property) {
        return properties.get(property);
    }

    /**
     * Set the destination of the serialized output, as a Writer.
     *
     * <p>Note that when this option is used, the serializer does not perform character
     * encoding. This also means that it never replaces special characters with XML numeric
     * character references. The final encoding is the responsibility of the supplied Writer.</p>
     *
     * <p>Closing the writer after use is the responsibility of the caller.</p>
     *
     * <p>Calling this method has the side-effect of setting the OutputStream and OutputFile to null.</p>
     *
     * @param writer the Writer to which the serialized XML output will be written.
     */

    public void setOutputWriter(Writer writer) {
        result.setOutputStream(null);
        result.setSystemId((String)null);
        result.setWriter(writer);
    }

    /**
     * Set the destination of the serialized output, as an OutputStream.
     *
     * <p>Closing the output stream after use is the responsibility of the caller.</p>
     *
     * <p>Calling this method has the side-effect of setting the OutputWriter and OutputFile to null.</p>
     *
     * @param stream the OutputStream to which the serialized XML output will be written.
     */

    public void setOutputStream(OutputStream stream) {
        result.setWriter(null);
        result.setSystemId((String)null);
        result.setOutputStream(stream);
    }

    /**
     * Set the destination of the serialized output, as a File.
     *
     * <p>Calling this method has the side-effect of setting the current OutputWriter
     * and OutputStream to null.</p>
     * @param file the File to which the serialized XML output will be written.
     */

    public void setOutputFile(File file) {
        result.setOutputStream(null);
        result.setWriter(null);
        result.setSystemId(file);
    }

    /**
     * Get the current output destination.
     *
     * @return an OutputStream, Writer, or File, depending on the previous calls to
     * {@link #setOutputStream}, {@link #setOutputWriter}, or {@link #setOutputFile}
     */

    public Object getOutputDestination() {
        if (result.getOutputStream() != null) {
            return result.getOutputStream();
        }
        if (result.getWriter() != null) {
            return result.getWriter();
        }
        String systemId = result.getSystemId();
        if (systemId != null) {
            try {
                return new File(new URI(systemId));
            } catch (URISyntaxException e) {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Return a receiver to which Saxon will send events. This method is provided
     * primarily for system use, though it could also be called by user applications
     * wanting to make use of the Saxon serializer.
     * @param config The Saxon configuration. This is an internal implementation object
     * held within the {@link Processor}
     * @return a receiver to which XML events will be sent
     */

    public Receiver getReceiver(Configuration config) throws SaxonApiException {
        return getReceiver(config, null, null);
    }

    /**
     * Return a receiver to which Saxon will send events. This method is provided
     * primarily for system use, though it could also be called by user applications
     * wanting to make use of the Saxon serializer.
     * @param config The Saxon configuration.
     * @param controller The Saxon controller (of the transformation). May be null.
     * @param predefinedProperties values of serialization properties defined within a query or stylesheet,
     * which will be used in the event that no value for the corresponding property has been defined in
     * the Serializer itself. May be null if no serialization properties have been predefined.
     * @return a receiver to which XML events will be sent
     */

    protected Receiver getReceiver(Configuration config, Controller controller, Properties predefinedProperties) throws SaxonApiException {
        try {
            SerializerFactory sf = config.getSerializerFactory();
            PipelineConfiguration pipe = (controller==null ? config.makePipelineConfiguration() : controller.makePipelineConfiguration());
            Properties props  = new Properties();
            for (Property p : properties.keySet()) {
                String value = properties.get(p);
                props.setProperty(p.toString(), value);
            }
            if (predefinedProperties != null) {
                Enumeration eps = predefinedProperties.propertyNames();
                while (eps.hasMoreElements()) {
                    String name = (String)eps.nextElement();
                    String value = predefinedProperties.getProperty(name);
                    if (props.getProperty(name) == null) {
                        props.setProperty(name, value);
                    } else if (name.equals(OutputKeys.CDATA_SECTION_ELEMENTS) || name.equals(SaxonOutputKeys.SUPPRESS_INDENTATION)) {
                        props.setProperty(name, props.getProperty(name) + " " + value);
                    }
                }
            }
            return sf.getReceiver(result, pipe, props);
        } catch (XPathException e) {
            throw new SaxonApiException(e);
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

