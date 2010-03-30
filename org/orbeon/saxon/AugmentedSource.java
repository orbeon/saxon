package org.orbeon.saxon;

import org.orbeon.saxon.event.Builder;
import org.orbeon.saxon.event.ParseOptions;
import org.orbeon.saxon.event.ProxyReceiver;
import org.orbeon.saxon.om.StructuredQName;
import org.orbeon.saxon.om.Validation;
import org.orbeon.saxon.type.SchemaType;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.util.List;

/**
 * This class is an extension of the JAXP Source interface. The class can be used
 * wherever a JAXP Source object can be used, and it provides additional information
 * about the way that the Source is to be processed: for example, it indicates
 * whether or not it should be validated against a schema. Other options that can
 * be set include the SAX XMLReader to be used, and the choice of whether a source
 * in the form of an existing tree should be copied or wrapped.
 * @since 8.8
 */

public class AugmentedSource implements Source {

    private Source source;
    private ParseOptions options = new ParseOptions();
//    private int schemaValidation = Validation.DEFAULT;
//    private int dtdValidation = Validation.DEFAULT;
//    private StructuredQName topLevelElement;
//    private SchemaType topLevelType;
//    private XMLReader parser = null;
//    private Boolean wrapDocument = null;
//    private int treeModel = Builder.UNSPECIFIED_TREE_MODEL;
//    private int stripSpace;
//    private Boolean lineNumbering = null;
//    private Boolean xIncludeAware = null;
//    private boolean pleaseClose = false;
    private EntityResolver entityResolver = null;
//    private ErrorListener errorListener = null;
//    private List filters = null;
//    private boolean sourceIsXQJ = false;

    /**
     * Create an AugmentedSource that wraps a given Source object (which must not itself be an
     * AugmentedSource)
     * @param source the Source object to be wrapped. This must be an implementation of Source
     * that Saxon recognizes, or an implementation for which a {@link SourceResolver} has been
     * registered with the {@link Configuration}. The source must not itself be an
     * AugmentedSource.
     * <p>As an alternative to this constructor, consider using the factory method
     * {@link #makeAugmentedSource}, which does accept any kind of Source including an
     * AugmentedSource as input.</p>
     * @throws IllegalArgumentException if the wrapped source is an AugmentedSource
     * @since 8.8
     */

    private AugmentedSource(Source source) {
        if (source instanceof AugmentedSource) {
            throw new IllegalArgumentException("Contained source must not be an AugmentedSource");
        }
        this.source = source;
    }

    /**
     * Create an AugmentedSource that wraps a given Source object. If this is already
     * an AugmentedSource, the original AugmentedSource is returned. Note that this means that
     * setting any properties on the returned AugmentedSource will also affect the original.
     * @param source the Source object to be wrapped
     * @return an AugmentedSource
     * @since 8.8
     */

    public static AugmentedSource makeAugmentedSource(Source source) {
        if (source instanceof AugmentedSource) {
            return (AugmentedSource)source;
        }
        return new AugmentedSource(source);
    }

    /**
     * Add a filter to the list of filters to be applied to the raw input
     * @param filter the filter to be added
     */

    public void addFilter(ProxyReceiver filter) {
        options.addFilter(filter);
    }

    /**
     * Get the list of filters to be applied to the input. Returns null if there are no filters.
     * @return the list of filters, if there are any
     */

    public List getFilters() {
        return options.getFilters();
    }

    /**
     * Get the Source object wrapped by this AugmentedSource
     * @return the contained Source object
     * @since 8.8
     */

    public Source getContainedSource() {
        return source;
    }

    /**
     * Set the space-stripping action to be applied to the source document
     * @param stripAction one of {@link org.orbeon.saxon.value.Whitespace#IGNORABLE},
     * {@link org.orbeon.saxon.value.Whitespace#ALL}, or {@link org.orbeon.saxon.value.Whitespace#NONE}
     * @since 8.8
     */

    public void setStripSpace(int stripAction) {
        options.setStripSpace(stripAction);
    }

    /**
     * Get the space-stripping action to be applied to the source document
     * @return one of {@link org.orbeon.saxon.value.Whitespace#IGNORABLE},
     * {@link org.orbeon.saxon.value.Whitespace#ALL}, or {@link org.orbeon.saxon.value.Whitespace#NONE}
     * @since 8.8
     */

    public int getStripSpace() {
        return options.getStripSpace();
    }

    /**
     * Set the tree model to use. Default is the tiny tree
     * @param model one of {@link Builder#TINY_TREE} or {@link Builder#LINKED_TREE}
     * @since 8.9
     */

    public void setTreeModel(int model) {
        options.setTreeModel(model);
    }

    /**
     * Get the tree model that will be used.
     * @return one of {@link Builder#TINY_TREE} or {@link Builder#LINKED_TREE},
     * or {link Builder#UNSPECIFIED_TREE_MODEL} if no call on setTreeModel() has been made
     * @since 8.9
     */

    public int getTreeModel() {
        return options.getTreeModel();
    }

    /**
     * Set whether or not schema validation of this source is required
     * @param option one of {@link Validation#STRICT},
     * {@link Validation#LAX}, {@link Validation#STRIP},
     * {@link Validation#PRESERVE}, {@link Validation#DEFAULT}
     * @since 8.8
     *
     */

    public void setSchemaValidationMode(int option) {
        options.setSchemaValidationMode(option);
    }

    /**
     * Get whether or not schema validation of this source is required
     * @return the validation mode requested, or {@link Validation#DEFAULT}
     * to use the default validation mode from the Configuration.
     * @since 8.8
     */

    public int getSchemaValidation() {
        return options.getSchemaValidationMode();
    }

    /**
     * Set the name of the top-level element for validation.
     * If a top-level element is set then the document
     * being validated must have this as its outermost element
     * @param elementName the QName of the required top-level element, or null to unset the value
     * @since 9.0
     */

    public void setTopLevelElement(StructuredQName elementName) {
        options.setTopLevelElement(elementName);
    }

    /**
     * Get the name of the top-level element for validation.
     * If a top-level element is set then the document
     * being validated must have this as its outermost element
     * @return the QName of the required top-level element, or null if no value is set
     * @since 9.0
     */

    public StructuredQName getTopLevelElement() {
        return options.getTopLevelElement();
    }

    /**
     * Set the type of the top-level element for validation.
     * If this is set then the document element is validated against this type
     * @param type the schema type required for the document element, or null to unset the value
     * @since 9.0
     */

    public void setTopLevelType(SchemaType type) {
        options.setTopLevelType(type);
    }

    /**
     * Get the type of the document element for validation.
     * If this is set then the document element of the document
     * being validated must have this type
     * @return the type of the required top-level element, or null if no value is set
     * @since 9.0
     */

    public SchemaType getTopLevelType() {
        return options.getTopLevelType();
    }

    /**
      * Set whether or not DTD validation of this source is required
      * @param option one of {@link Validation#STRICT},
      * {@link Validation#STRIP}, {@link Validation#DEFAULT}
      * @since 8.8
      */

     public void setDTDValidationMode(int option) {
         options.setDTDValidationMode(option);
     }

     /**
      * Get whether or not DTD validation of this source is required
      * @return the validation mode requested, or {@link Validation#DEFAULT}
      * to use the default validation mode from the Configuration.
      * @since 8.8
      */

     public int getDTDValidation() {
         return options.getDTDValidationMode();
     }


    /**
     * Set whether line numbers are to be maintained in the constructed document
     * @param lineNumbering true if line numbers are to be maintained
     * @since 8.8
     */

    public void setLineNumbering(boolean lineNumbering) {
        options.setLineNumbering(lineNumbering);
    }

    /**
     * Get whether line numbers are to be maintained in the constructed document
     * @return true if line numbers are maintained
     * @since 8.8
     */

    public boolean isLineNumbering() {
        return options.isLineNumbering();
    }

    /**
     * Determine whether setLineNumbering() has been called
     * @return true if setLineNumbering() has been called
     * @since 8.9
     */

    public boolean isLineNumberingSet()  {
        return options.isLineNumberingSet();
    }

    /**
     * Set the SAX parser (XMLReader) to be used
     * @param parser the SAX parser
     * @since 8.8
     */

    public void setXMLReader(XMLReader parser) {
        options.setXMLReader(parser);
        if (source instanceof SAXSource) {
            ((SAXSource)source).setXMLReader(parser);
        }
    }

    /**
     * Get the SAX parser (XMLReader) to be used
     * @return the parser
     * @since 8.8
     */

    public XMLReader getXMLReader() {
        XMLReader parser = options.getXMLReader();
        if (parser != null) {
            return parser;
        } else if (source instanceof SAXSource) {
            return ((SAXSource)source).getXMLReader();
        } else {
            return null;
        }
    }

    /**
     * Assuming that the contained Source is a node in a tree, indicate whether a tree should be created
     * as a view of this supplied tree, or as a copy.
     * @param wrap if true, the node in the supplied Source is wrapped, to create a view. If false, the node
     * and its contained subtree is copied. If null, the system default is chosen.
     * @since 8.8
     */

    public void setWrapDocument(Boolean wrap) {
        options.setWrapDocument(wrap);
    }

    /**
       Assuming that the contained Source is a node in a tree, determine whether a tree will be created
     * as a view of this supplied tree, or as a copy.
     * @return if true, the node in the supplied Source is wrapped, to create a view. If false, the node
     * and its contained subtree is copied. If null, the system default is chosen.
     * @since 8.8
     */

    public Boolean getWrapDocument() {
        return options.getWrapDocument();
    }

    /**
     * Set the System ID. This sets the System Id on the underlying Source object.
     * @param id the System ID. This provides a base URI for the document, and also the result
     * of the document-uri() function
     * @since 8.8
     */

    public void setSystemId(String id) {
        source.setSystemId(id);
    }

    /**
     * Get the System ID. This gets the System Id on the underlying Source object.
     * @return the System ID: effectively the base URI.
     * @since 8.8
     */

    public String getSystemId() {
        return source.getSystemId();
    }

    /**
     * <p>Set state of XInclude processing.</p>
     * <p/>
     * <p>If XInclude markup is found in the document instance, should it be
     * processed as specified in <a href="http://www.w3.org/TR/xinclude/">
     * XML Inclusions (XInclude) Version 1.0</a>.</p>
     * <p/>
     * <p>XInclude processing defaults to <code>false</code>.</p>
     *
     * @param state Set XInclude processing to <code>true</code> or
     *              <code>false</code>
     * @since 8.9
     */
    public void setXIncludeAware(boolean state) {
        options.setXIncludeAware(state);
    }

    /**
     * <p>Determine whether setXIncludeAware() has been called.</p>
     *
     * @return true if setXIncludeAware() has been called
     * @since 8.9
     */

    public boolean isXIncludeAwareSet() {
        return options.isXIncludeAwareSet();
    }

    /**
     * <p>Get state of XInclude processing.</p>
     *
     * @return current state of XInclude processing. Default value is false.
     * @since 8.9
     */

    public boolean isXIncludeAware() {
        return options.isXIncludeAware();
    }

    /**
     * Set an EntityResolver to be used when parsing
     * @param resolver the EntityResolver to be used
     * @since 8.9
     * @deprecated since 9.1 (this method has never had any effect, other than allowing the EntityResolver to
     * be retrieved using the <code>getEntityResolver()</code> method)
     */

    public void setEntityResolver(EntityResolver resolver) {
        entityResolver = resolver;
    }

    /**
     * Get the EntityResolver that will be used when parsing
     * @return the EntityResolver, if one has been set using {@link #setEntityResolver},
     * otherwise null.
     * @since 8.9
     * @deprecated since 9.1 (setting an EntityResolver has never had any effect)
     */

    public EntityResolver getEntityResolver() {
        return entityResolver;
    }

    /**
     * Set an ErrorListener to be used when parsing
     * @param listener the ErrorListener to be used
     * @since 8.9
     */

    public void setErrorListener(ErrorListener listener) {
        options.setErrorListener(listener);
    }

    /**
     * Get the ErrorListener that will be used when parsing
     * @return the ErrorListener, if one has been set using {@link #setErrorListener},
     * otherwise null.
     * @since 8.9
     */

    public ErrorListener getErrorListener() {
        return options.getErrorListener();
    }



    /**
     * Set whether or not the user of this Source is encouraged to close it as soon as reading is finished.
     * Normally the expectation is that any Stream in a StreamSource will be closed by the component that
     * created the Stream. However, in the case of a Source returned by a URIResolver, there is no suitable
     * interface (the URIResolver has no opportunity to close the stream). Also, in some cases such as reading
     * of stylesheet modules, it is possible to close the stream long before control is returned to the caller
     * who supplied it. This tends to make a difference on .NET, where a file often can't be opened if there
     * is a stream attached to it.
     * @param close true if the source should be closed as soon as it has been consumed
     * @since 8.8
     */

    public void setPleaseCloseAfterUse(boolean close) {
        options.setPleaseCloseAfterUse(close);
    }

    /**
     * Determine whether or not the user of this Source is encouraged to close it as soon as reading is
     * finished.
     * @return true if the source should be closed as soon as it has been consumed
     * @since 8.8
     */

    public boolean isPleaseCloseAfterUse() {
        return options.isPleaseCloseAfterUse();
    }

    /**
     * Indicate that this Source is supporting the weird XQJ createItemFromDocument(XMLReader) method.
     * This contains a preinitialized XMLReader that needs to be invoked in a special way
     * @param flag set to true if this is a special XQJ SAXSource
     */

    public void setSourceIsXQJ(boolean flag) {
        options.setSourceIsXQJ(flag);
    }

    /**
     * Ask whether this Source is supporting the weird XQJ createItemFromDocument(XMLReader) method.
     * This contains a preinitialized XMLReader that needs to be invoked in a special way
     * @return true if this is a special XQJ SAXSource
     */

    public boolean sourceIsXQJ() {
        return options.sourceIsXQJ();
    }

    /**
     * Close any resources held by this Source. This only works if the underlying Source is one that is
     * recognized as holding closable resources.
     * @since 8.8
     */

    public void close() {
        try {
            if (source instanceof StreamSource) {
                StreamSource ss = (StreamSource)source;
                if (ss.getInputStream() != null) {
                    ss.getInputStream().close();
                }
                if (ss.getReader() != null) {
                    ss.getReader().close();
                }
            } else if (source instanceof SAXSource) {
                InputSource is = ((SAXSource)source).getInputSource();
                if (is != null) {
                    if (is.getByteStream() != null) {
                        is.getByteStream().close();
                    }
                    if (is.getCharacterStream() != null) {
                        is.getCharacterStream().close();
                    }
                }
            }
        } catch (IOException err) {
            // no action
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