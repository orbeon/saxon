package org.orbeon.saxon.event;

import org.orbeon.saxon.om.StructuredQName;
import org.orbeon.saxon.om.Validation;
import org.orbeon.saxon.type.SchemaType;
import org.xml.sax.XMLReader;

import javax.xml.transform.ErrorListener;
import java.util.ArrayList;
import java.util.List;


/**
 * This class defines options for parsing a source document
 */

public class ParseOptions {

    private int schemaValidation = Validation.DEFAULT;
    private int dtdValidation = Validation.DEFAULT;
    private StructuredQName topLevelElement;
    private SchemaType topLevelType;
    private XMLReader parser = null;
    private Boolean wrapDocument = null;
    private int treeModel = Builder.UNSPECIFIED_TREE_MODEL;
    private int stripSpace;
    private Boolean lineNumbering = null;
    private Boolean xIncludeAware = null;
    private boolean pleaseClose = false;
    private ErrorListener errorListener = null;
    private List filters = null;
    private boolean sourceIsXQJ = false;

    /**
     * Add a filter to the list of filters to be applied to the raw input
     * @param filter the filter to be added
     */

    public void addFilter(ProxyReceiver filter) {
        if (filters == null) {
            filters = new ArrayList(5);
        }
        filters.add(filter);
    }

    /**
     * Get the list of filters to be applied to the input. Returns null if there are no filters.
     * @return the list of filters, if there are any
     */

    public List getFilters() {
        return filters;
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
     * Set the tree model to use. Default is the tiny tree
     * @param model one of {@link org.orbeon.saxon.event.Builder#TINY_TREE} or
     * {@link org.orbeon.saxon.event.Builder#LINKED_TREE}
     */

    public void setTreeModel(int model) {
        if (model != Builder.TINY_TREE && model != Builder.LINKED_TREE) {
            throw new IllegalArgumentException("model must be Builder.TINY_TREE or Builder.LINKED_TREE");
        }
        treeModel = model;
    }

    /**
     * Get the tree model that will be used.
     * @return one of {@link org.orbeon.saxon.event.Builder#TINY_TREE} or {@link org.orbeon.saxon.event.Builder#LINKED_TREE},
     * or {link Builder#UNSPECIFIED_TREE_MODEL} if no call on setTreeModel() has been made
     */

    public int getTreeModel() {
        return treeModel;
    }

    /**
     * Set whether or not schema validation of this source is required
     * @param option one of {@link org.orbeon.saxon.om.Validation#STRICT},
     * {@link org.orbeon.saxon.om.Validation#LAX}, {@link org.orbeon.saxon.om.Validation#STRIP},
     * {@link org.orbeon.saxon.om.Validation#PRESERVE}, {@link org.orbeon.saxon.om.Validation#DEFAULT}
     */

    public void setSchemaValidationMode(int option) {
        schemaValidation = option;
    }

    /**
     * Get whether or not schema validation of this source is required
     * @return the validation mode requested, or {@link org.orbeon.saxon.om.Validation#DEFAULT}
     * to use the default validation mode from the Configuration.

     */

    public int getSchemaValidationMode() {
        return schemaValidation;
    }

    /**
     * Set the name of the top-level element for validation.
     * If a top-level element is set then the document
     * being validated must have this as its outermost element
     * @param elementName the QName of the required top-level element, or null to unset the value
     */

    public void setTopLevelElement(StructuredQName elementName) {
        topLevelElement = elementName;
    }

    /**
     * Get the name of the top-level element for validation.
     * If a top-level element is set then the document
     * being validated must have this as its outermost element
     * @return the QName of the required top-level element, or null if no value is set
     * @since 9.0
     */

    public StructuredQName getTopLevelElement() {
        return topLevelElement;
    }

    /**
     * Set the type of the top-level element for validation.
     * If this is set then the document element is validated against this type
     * @param type the schema type required for the document element, or null to unset the value
     */

    public void setTopLevelType(SchemaType type) {
        topLevelType = type;
    }

    /**
     * Get the type of the document element for validation.
     * If this is set then the document element of the document
     * being validated must have this type
     * @return the type of the required top-level element, or null if no value is set
     */

    public SchemaType getTopLevelType() {
        return topLevelType;
    }

    /**
      * Set whether or not DTD validation of this source is required
      * @param option one of {@link org.orbeon.saxon.om.Validation#STRICT},
      * {@link org.orbeon.saxon.om.Validation#STRIP}, {@link org.orbeon.saxon.om.Validation#DEFAULT}
      */

     public void setDTDValidationMode(int option) {
         dtdValidation = option;
     }

     /**
      * Get whether or not DTD validation of this source is required
      * @return the validation mode requested, or {@link org.orbeon.saxon.om.Validation#DEFAULT}
      * to use the default validation mode from the Configuration.
      */

     public int getDTDValidationMode() {
         return dtdValidation;
     }


    /**
     * Set whether line numbers are to be maintained in the constructed document
     * @param lineNumbering true if line numbers are to be maintained
     */

    public void setLineNumbering(boolean lineNumbering) {
        this.lineNumbering = Boolean.valueOf(lineNumbering);
    }

    /**
     * Get whether line numbers are to be maintained in the constructed document
     * @return true if line numbers are maintained
     */

    public boolean isLineNumbering() {
        return lineNumbering != null && lineNumbering.booleanValue();
    }

    /**
     * Determine whether setLineNumbering() has been called
     * @return true if setLineNumbering() has been called
     */

    public boolean isLineNumberingSet()  {
        return lineNumbering != null;
    }

    /**
     * Set the SAX parser (XMLReader) to be used
     * @param parser the SAX parser
     */

    public void setXMLReader(XMLReader parser) {
        this.parser = parser;
    }

    /**
     * Get the SAX parser (XMLReader) to be used
     * @return the parser
     */

    public XMLReader getXMLReader() {
        return parser;
    }

    /**
     * Assuming that the contained Source is a node in a tree, indicate whether a tree should be created
     * as a view of this supplied tree, or as a copy.
     * @param wrap if true, the node in the supplied Source is wrapped, to create a view. If false, the node
     * and its contained subtree is copied. If null, the system default is chosen.
     */

    public void setWrapDocument(Boolean wrap) {
        wrapDocument = wrap;
    }

    /**
       Assuming that the contained Source is a node in a tree, determine whether a tree will be created
     * as a view of this supplied tree, or as a copy.
     * @return if true, the node in the supplied Source is wrapped, to create a view. If false, the node
     * and its contained subtree is copied. If null, the system default is chosen.
     * @since 8.8
     */

    public Boolean getWrapDocument() {
        return wrapDocument;
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
        xIncludeAware = Boolean.valueOf(state);
    }

    /**
     * <p>Determine whether setXIncludeAware() has been called.</p>
     *
     * @return true if setXIncludeAware() has been called
     */

    public boolean isXIncludeAwareSet() {
        return (xIncludeAware != null);
    }

    /**
     * <p>Get state of XInclude processing.</p>
     *
     * @return current state of XInclude processing. Default value is false.
     */

    public boolean isXIncludeAware() {
        return xIncludeAware != null && xIncludeAware.booleanValue();
    }

    /**
     * Set an ErrorListener to be used when parsing
     * @param listener the ErrorListener to be used
     */

    public void setErrorListener(ErrorListener listener) {
        errorListener = listener;
    }

    /**
     * Get the ErrorListener that will be used when parsing
     * @return the ErrorListener, if one has been set using {@link #setErrorListener},
     * otherwise null.
     */

    public ErrorListener getErrorListener() {
        return errorListener;
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
     */

    public void setPleaseCloseAfterUse(boolean close) {
        pleaseClose = close;
    }

    /**
     * Determine whether or not the user of this Source is encouraged to close it as soon as reading is
     * finished.
     * @return true if the source should be closed as soon as it has been consumed
     */

    public boolean isPleaseCloseAfterUse() {
        return pleaseClose;
    }

    /**
     * Indicate that this Source is supporting the weird XQJ createItemFromDocument(XMLReader) method.
     * This contains a preinitialized XMLReader that needs to be invoked in a special way
     * @param flag set to true if this is a special XQJ SAXSource
     */

    public void setSourceIsXQJ(boolean flag) {
        sourceIsXQJ = flag;
    }

    /**
     * Ask whether this Source is supporting the weird XQJ createItemFromDocument(XMLReader) method.
     * This contains a preinitialized XMLReader that needs to be invoked in a special way
     * @return true if this is a special XQJ SAXSource
     */

    public boolean sourceIsXQJ() {
        return sourceIsXQJ;
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