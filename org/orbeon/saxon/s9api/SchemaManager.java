package org.orbeon.saxon.s9api;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.event.Receiver;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.SchemaException;
import org.orbeon.saxon.type.SchemaURIResolver;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.Source;

/**
 * The SchemaManager is used to load schema documents, and to set options for the way in which they are loaded.
 * At present all the resulting schema components are held in a single pool owned by the Processor object. 
 */
public class SchemaManager {

    private Configuration config;
    private ErrorListener errorListener;

    protected SchemaManager(Configuration config) {
        this.config = config;
        this.errorListener = null;
    }

    /**
     * Set the ErrorListener to be used while loading and validating schema documents
     * @param listener The error listener to be used. This is notified of all errors detected during the
     * compilation.
     */

    public void setErrorListener(ErrorListener listener) {
        this.errorListener = listener;
    }

    /**
     * Get the ErrorListener being used while loading and validating schema documents
     * @return listener The error listener in use. This is notified of all errors detected during the
     * compilation. Returns null if no user-supplied ErrorListener has been set.
     */

    public ErrorListener getErrorListener() {
        return errorListener;
    }

    /**
     * Set the SchemaURIResolver to be used during schema loading. This SchemaURIResolver, despite its name,
     * is <b>not</b> used for resolving relative URIs against a base URI; it is used for dereferencing
     * an absolute URI (after resolution) to return a {@link javax.xml.transform.Source} representing the
     * location where a schema document can be found.
     *
     * <p>This SchemaURIResolver is used to dereference the URIs appearing in <code>xs:import</code>,
     * <code>xs:include</code>, and <code>xs:redefine</code> declarations.
     *
     * @param resolver the SchemaURIResolver to be used during schema loading.
     */

    public void setSchemaURIResolver(SchemaURIResolver resolver) {
        config.setSchemaURIResolver(resolver);
    }

    /**
     * Get the SchemaURIResolver to be used during schema loading.
     *
     * @return the URIResolver used during stylesheet compilation. Returns null if no user-supplied
     * URIResolver has been set.
     */

    public SchemaURIResolver getSchemaURIResolver() {
        return config.getSchemaURIResolver();
    }

    /**
     * Load a schema document from a given Source. The schema components derived from this schema
     * document are added to the cache of schema components maintained by this SchemaManager
     * @param source the document containing the schema. The getSystemId() method applied to this Source
     * must return a base URI suitable for resolving <code>xs:include</code> and <code>xs:import</code>
     * directives.
     * @throws SaxonApiException if the schema document is not valid, or if its contents are inconsistent
     * with the schema components already held by this SchemaManager. 
     */

    public void load(Source source) throws SaxonApiException {
        try {
            config.addSchemaSource(source, errorListener);
        } catch (SchemaException e) {
            throw new SaxonApiException(e);
        }
    }

    /**
     * Import a precompiled Schema Component Model from a given Source. The schema components derived from this schema
     * document are added to the cache of schema components maintained by this SchemaManager
     * @param source the XML file containing the schema component model, as generated by a previous call on
     * {@link #exportComponents}
     */

    public void importComponents(Source source) throws SaxonApiException {
        try {
            config.importComponents(source);
        } catch (XPathException err) {
            throw new SaxonApiException(err);
        }
    }

    /**
     * Export a precompiled Schema Component Model containing all the components (except built-in components)
     * that have been loaded into this Processor.
     * @param destination the destination to recieve the precompiled Schema Component Model in the form of an
     * XML document
     */

    public void exportComponents(Destination destination) throws SaxonApiException {
        try {
            Receiver out = destination.getReceiver(config);
            config.exportComponents(out);
        } catch (XPathException e) {
            throw new SaxonApiException(e);
        }
    }


    /**
     * Create a SchemaValidator which can be used to validate instance documents against the schema held by this
     * SchemaManager
     * @return a new SchemaValidator
     */

    public SchemaValidator newSchemaValidator() {
        return new SchemaValidator(config);
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

