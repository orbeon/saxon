package org.orbeon.saxon.style;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.functions.FunctionLibrary;
import org.orbeon.saxon.expr.StaticContext;
import org.orbeon.saxon.expr.VariableDeclaration;
import org.orbeon.saxon.instruct.LocationMap;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.xpath.XPathException;
import org.orbeon.saxon.xpath.StaticError;

import java.util.Comparator;

/**
* An ExpressionContext represents the context for an XPath expression written
* in the stylesheet.
*/

public class ExpressionContext implements StaticContext {

	private StyleElement element;
	private NamePool namePool;

	public ExpressionContext(StyleElement styleElement) {
		element = styleElement;
		namePool = styleElement.getTargetNamePool();
	}

    /**
     * Get the system configuration
     */

    public Configuration getConfiguration() {
        return element.getPreparedStylesheet().getConfiguration();
    }

    /**
     * Get the location map
     */

    public LocationMap getLocationMap() {
        return element.getPrincipalStylesheet().getLocationMap();
    }

    /**
    * Issue a compile-time warning
    */

    public void issueWarning(String s) {
        element.issueWarning(s);
    }

    /**
    * Get the NamePool used for compiling expressions
    */

    public NamePool getNamePool() {
        return namePool;
    }

    /**
    * Get the System ID of the entity containing the expression (used for diagnostics)
    */

    public String getSystemId() {
    	return element.getSystemId();
    }

    /**
    * Get the line number of the expression within its containing entity
    * Returns -1 if no line number is available
    */

    public int getLineNumber() {
    	return element.getLineNumber();
    }

    /**
    * Get the Base URI of the element containing the expression, for resolving any
    * relative URI's used in the expression.
    * Used by the document() function.
    */

    public String getBaseURI() {
        return element.getBaseURI();
    }

    /**
    * Get the URI for a prefix, using this Element as the context for namespace resolution.
    * The default namespace will not be used when the prefix is empty.
    * @param prefix The prefix
    * @throws XPathException if the prefix is not declared
    */

    public String getURIForPrefix(String prefix) throws XPathException {
        try {
            return element.getURIForPrefix(prefix, false);
        } catch (NamespaceException err) {
            throw new StaticError(err);
        }
    }

    /**
    * Get a copy of the Namespace Context
    */

    public NamespaceResolver getNamespaceResolver() {
        return element.makeNamespaceContext();
    }

    /**
    * Get a fingerprint for a name, using this as the context for namespace resolution
    * @param qname The name as written, in the form "[prefix:]localname"
    * @param useDefault Defines the action when there is no prefix. If true, use
    * the default namespace URI (as for element names). If false, use no namespace URI
    * (as for attribute names).
    * @return -1 if the name is not already present in the name pool
    */

    public int getFingerprint(String qname, boolean useDefault) throws XPathException {

        String[] parts;
        try {
            parts = Name.getQNameParts(qname);
        } catch (QNameException err) {
            throw new StaticError(err.getMessage());
        }
        String prefix = parts[0];
        if (prefix.equals("")) {
            String uri = "";

            if (useDefault) {
                uri = getURIForPrefix(prefix);
            }

			return namePool.getFingerprint(uri, qname);

        } else {

            String uri = getURIForPrefix(prefix);
			return namePool.getFingerprint(uri, parts[1]);
        }
    }

    /**
    * Bind a variable to an object that can be used to refer to it
    * @param fingerprint The fingerprint of the variable name
    * @return a VariableDeclaration object that can be used to identify it in the Bindery
    * @throws org.orbeon.saxon.xpath.StaticError if the variable has not been declared
    */

    public VariableDeclaration bindVariable(int fingerprint) throws StaticError {
 	    return element.bindVariable(fingerprint);
    }

    /**
     * Get the function library containing all the in-scope functions available in this static
     * context
     */

    public FunctionLibrary getFunctionLibrary() {
        return element.getPrincipalStylesheet().getFunctionLibrary();
    }

    /**
    * Determine if an extension element is available
    * @throws XPathException if the name is invalid or the prefix is not declared
    */

    public boolean isElementAvailable(String qname) throws XPathException {
        try {
            String[] parts = Name.getQNameParts(qname);
            String uri = getURIForPrefix(parts[0]);

            return element.getPreparedStylesheet().
                                getStyleNodeFactory().isElementAvailable(uri, parts[1]);
        } catch (QNameException e) {
            throw new StaticError("Invalid element name. " + e.getMessage());
        }
    }

    /**
    * Get a named collation.
    * @param name The name of the required collation. Supply null to get the default collation.
    * @return the collation; or null if the required collation is not found.
    */

    public Comparator getCollation(String name) throws XPathException {
        return element.getPrincipalStylesheet().findCollation(name);
    }

    /**
    * Get the default collation. Return null if no default collation has been defined
    */

    public String getDefaultCollationName() {
        return element.getPrincipalStylesheet().getDefaultCollationName();
    }

    /**
    * Get the default XPath namespace, as a namespace code that can be looked up in the NamePool
    */

    public short getDefaultElementNamespace() {
        return element.getDefaultXPathNamespace();
    }

    /**
     * Get the default function namespace
     */

    public String getDefaultFunctionNamespace() {
        return NamespaceConstant.FN;
    }

    /**
    * Determine whether Backwards Compatible Mode is used
    */

    public boolean isInBackwardsCompatibleMode() {
        return element.backwardsCompatibleModeIsEnabled();
    }

    /**
     * Test whether a schema has been imported for a given namespace
     * @param namespace the target namespace of the required schema
     * @return true if a schema for this namespace has been imported
     */

    public boolean isImportedSchema(String namespace) {
        return getXSLStylesheet().isImportedSchema(namespace);
    }

    /**
    * Get the XSLStylesheet object
    */

    public XSLStylesheet getXSLStylesheet() {
        return element.getPrincipalStylesheet();
    }

    /**
     * Get the stylesheet element containing this XPath expression
     * @return the element in the tree representation of the source stylesheet
     */

    public StyleElement getStyleElement() {
        return element;
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
