package org.orbeon.saxon;

import org.xml.sax.XMLReader;

import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;

import org.orbeon.saxon.om.Validation;

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
    private XMLReader parser = null;
    private Boolean wrapDocument = null;

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
     * Create an AugmentedSource that wraps a given Source object (which must not itself be an
     * AugmentedSource)
     * @param source the Source object to be wrapped
     * @throws IllegalArgumentException if the wrapped source is an AugmentedSource
     */

    public static AugmentedSource makeAugmentedSource(Source source) {
        if (source instanceof AugmentedSource) {
            return (AugmentedSource)source;
        }
        return new AugmentedSource(source);
    }

    /**
     * Get the Source object wrapped by this AugmentedSource
     * @return the contained Source object
     */

    public Source getContainedSource() {
        return source;
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