package org.orbeon.saxon;

import org.orbeon.saxon.om.Validation;
import org.orbeon.saxon.event.ProxyReceiver;
import org.xml.sax.XMLReader;
import org.xml.sax.InputSource;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.sax.SAXSource;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;

/**
 * This class wraps a JAXP Source object to provide an extended Source object that
 * contains options indicating how the Source should be processed: for example,
 * whether or not it should be validated against a schema. Other options that can
 * be set include the SAX XMLReader to be used, and the choice of whether a source
 * in the form of an existing tree should be copied or wrapped.
 */

public class AugmentedSource implements Source {

    private Source source;
    private int schemaValidation = Validation.DEFAULT;
    private int dtdValidation = Validation.DEFAULT;
    private XMLReader parser = null;
    private Boolean wrapDocument = null;
    private int stripSpace;
    private boolean lineNumbering = false;
    private boolean pleaseClose = false;
    private List filters = null;

    /**
     * Create an AugmentedSource that wraps a given Source object (which must not itself be an
     * AugmentedSource)
     * @param source the Source object to be wrapped
     * @throws IllegalArgumentException if the wrapped source is an AugmentedSource
     */

    private AugmentedSource(Source source) {
        if (source instanceof AugmentedSource) {
            throw new IllegalArgumentException("Contained source must not be an AugmentedSource");
        }
        this.source = source;
    }

    /**
     * Create an AugmentedSource that wraps a given Source object. If this is already
     * an AugmentedSource, the original AugmentedSource is returned.
     * @param source the Source object to be wrapped
     */

    public static AugmentedSource makeAugmentedSource(Source source) {
        if (source instanceof AugmentedSource) {
            return (AugmentedSource)source;
        }
        return new AugmentedSource(source);
    }

    /**
     * Add a filter to the list of filters to be applied to the raw input
     */

    public void addFilter(ProxyReceiver filter) {
        if (filters == null) {
            filters = new ArrayList(5);
        }
        filters.add(filter);
    }

    /**
     * Get the list of filters to be applied to the input. Returns null if there are no filters.
     */

    public List getFilters() {
        return filters;
    }

    /**
     * Get the Source object wrapped by this AugmentedSource
     * @return the contained Source object
     */

    public Source getContainedSource() {
        return source;
    }

    /**
     * Set the space-stripping action to be applied to the source document
     * @param stripAction one of {@link org.orbeon.saxon.value.Whitespace#IGNORABLE},
     * {@link org.orbeon.saxon.value.Whitespace#ALL}, or {@link org.orbeon.saxon.value.Whitespace#NONE}
     */

    public void setStripSpace(int stripAction) {
        stripSpace = stripAction;
    }

    /**
     * Get the space-stripping action to be applied to the source document
     * @return one of {@link org.orbeon.saxon.value.Whitespace#IGNORABLE},
     * {@link org.orbeon.saxon.value.Whitespace#ALL}, or {@link org.orbeon.saxon.value.Whitespace#NONE}
     */

    public int getStripSpace() {
        return stripSpace;
    }

    /**
     * Set whether or not schema validation of this source is required
     * @param option one of {@link Validation#STRICT},
     * {@link Validation#LAX}, {@link Validation#STRIP},
     * {@link Validation#PRESERVE}, {@link Validation#DEFAULT}
     *
     */

    public void setSchemaValidationMode(int option) {
        schemaValidation = option;
    }

    /**
     * Get whether or not schema validation of this source is required
     * @return the validation mode requested, or {@link Validation#DEFAULT}
     * to use the default validation mode from the Configuration.
     */

    public int getSchemaValidation() {
        return schemaValidation;
    }

    /**
      * Set whether or not DTD validation of this source is required
      * @param option one of {@link Validation#STRICT},
      * {@link Validation#STRIP}, {@link Validation#DEFAULT}
      */

     public void setDTDValidationMode(int option) {
         dtdValidation = option;
     }

     /**
      * Get whether or not DTD validation of this source is required
      * @return the validation mode requested, or {@link Validation#DEFAULT}
      * to use the default validation mode from the Configuration.
      */

     public int getDTDValidation() {
         return dtdValidation;
     }


    /**
     * Set whether line numbers are to be maintained in the constructed document
     * @param lineNumbering
     */

    public void setLineNumbering(boolean lineNumbering) {
        this.lineNumbering = lineNumbering;
    }

    /**
     * Get whether line numbers are to be maintained in the constructed document
     * @return true if line numbers are maintained
     */

    public boolean isLineNumbering() {
        return lineNumbering;
    }

    /**
     * Set the SAX parser (XMLReader) to be used
     * @param parser
     */

    public void setXMLReader(XMLReader parser) {
        this.parser = parser;
        if (source instanceof SAXSource) {
            ((SAXSource)source).setXMLReader(parser);
        }
    }

    public XMLReader getXMLReader() {
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
     */

    public void setWrapDocument(Boolean wrap) {
        this.wrapDocument = wrap;
    }

    /**
       Assuming that the contained Source is a node in a tree, determine whether a tree will be created
     * as a view of this supplied tree, or as a copy.
     * @return if true, the node in the supplied Source is wrapped, to create a view. If false, the node
     * and its contained subtree is copied. If null, the system default is chosen.
     */

    public Boolean getWrapDocument() {
        return wrapDocument;
    }

    /**
     * Set the System ID. This sets the System Id on the underlying Source object.
     * @param id the System ID.
     */

    public void setSystemId(String id) {
        source.setSystemId(id);
    }

    /**
     * Get the System ID. This gets the System Id on the underlying Source object.
     * @return the System ID.
     */

    public String getSystemId() {
        return source.getSystemId();
    }

    /**
     * Set whether or not the user of this Source is encouraged to close it as soon as reading is finished.
     * Normally the expectation is that any Stream in a StreamSource will be closed by the component that
     * created the Stream. However, in the case of a Source returned by a URIResolver, there is no suitable
     * interface (the URIResolver has no opportunity to close the stream). Also, in some cases such as reading
     * of stylesheet modules, it is possible to close the stream long before control is returned to the caller
     * who supplied it. This tends to make a difference on .NET, where a file often can't be opened if there
     * is a stream attached to it.
     */

    public void setPleaseCloseAfterUse(boolean close) {
        pleaseClose = close;
    }

    /**
     * Determine whether or not the user of this Source is encouraged to close it as soon as reading is
     * finished.
     */

    public boolean isPleaseCloseAfterUse() {
        return pleaseClose;
    }

    /**
     * Close any resources held by this Source. This only works if the underlying Source is one that is
     * recognized as holding closable resources.
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