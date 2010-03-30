package org.orbeon.saxon.xpath;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.expr.Container;
import org.orbeon.saxon.expr.StaticContext;
import org.orbeon.saxon.expr.VariableReference;
import org.orbeon.saxon.instruct.SlotManager;
import org.orbeon.saxon.om.NamespaceConstant;
import org.orbeon.saxon.om.NamespaceResolver;
import org.orbeon.saxon.om.StructuredQName;
import org.orbeon.saxon.sxpath.AbstractStaticContext;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.SchemaException;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.transform.Source;
import javax.xml.xpath.XPathFunctionResolver;
import javax.xml.xpath.XPathVariableResolver;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

/**
* A StandaloneContext provides a context for parsing an XPath expression
* in a context other than a stylesheet. In particular, it is used to support
* the JAXP 1.3 XPath API. The JAXP API does not actually expose the StaticContext
* object directly; rather, the static context (namespaces, variables, and functions)
 * is manipulated through the XPath object, implemented in Saxon by the {@link XPathEvaluator}
*/

public class JAXPXPathStaticContext extends AbstractStaticContext
        implements StaticContext, NamespaceResolver, Container {

    private SlotManager stackFrameMap;
    private XPathFunctionLibrary xpathFunctionLibrary;

    private NamespaceContext namespaceContext = new MinimalNamespaceContext();
    private XPathVariableResolver variableResolver;

	/**
	* Create a StandaloneContext using a specific Configuration.
    * @param config the Configuration. For schema-aware XPath expressions, this must be a SchemaAwareConfiguration.
	*/

	public JAXPXPathStaticContext(Configuration config) {
        setConfiguration(config);
        stackFrameMap = config.makeSlotManager();
        setDefaultFunctionLibrary();
        xpathFunctionLibrary = new XPathFunctionLibrary();
        addFunctionLibrary(xpathFunctionLibrary);
	}

    /**
     * Supply the NamespaceContext used to resolve namespaces.
     */

    public void setNamespaceContext(NamespaceContext context) {
        this.namespaceContext = context;
    }

    /**
     * Get the NamespaceContext that was set using {@link #setNamespaceContext}
     */

    public NamespaceContext getNamespaceContext() {
        return namespaceContext;
    }

    /**
     * Get the stack frame map containing the slot number allocations for the variables declared
     * in this static context
     */

    public SlotManager getStackFrameMap() {
        return stackFrameMap;
    }

    /**
     * Set an XPathVariableResolver. This is used to resolve variable references
     * if no variable has been explicitly declared.
     * @param resolver A JAXP 1.3 XPathVariableResolver
     */

    public void setXPathVariableResolver(XPathVariableResolver resolver) {
        this.variableResolver = resolver;
    }

    /**
     * Get the XPathVariableResolver
     */

    public XPathVariableResolver getXPathVariableResolver() {
        return variableResolver;
    }

    public void setXPathFunctionResolver(XPathFunctionResolver xPathFunctionResolver) {
        if (xpathFunctionLibrary != null) {
            xpathFunctionLibrary.setXPathFunctionResolver(xPathFunctionResolver);
        }
    }

    public XPathFunctionResolver getXPathFunctionResolver() {
        if (xpathFunctionLibrary != null) {
            return xpathFunctionLibrary.getXPathFunctionResolver();
        } else {
            return null;
        }
    }

    /**
     * Get the URI for a prefix, using the declared namespaces as
     * the context for namespace resolution. The default namespace is NOT used
     * when the prefix is empty.
     * This method is provided for use by the XPath parser.
     * @param prefix The prefix
     * @throws org.orbeon.saxon.trans.XPathException if the prefix is not declared
    */

    public String getURIForPrefix(String prefix) throws XPathException {
        String uri = getURIForPrefix(prefix, false);
    	if (uri==null) {
    		throw new XPathException("Prefix " + prefix + " has not been declared");
    	}
    	return uri;
    }

    public NamespaceResolver getNamespaceResolver() {
        return this;
    }

    /**
     * Get the namespace URI corresponding to a given prefix. Return null
     * if the prefix is not in scope. This method searches
     * any namespace context supplied using {@link #setNamespaceContext(javax.xml.namespace.NamespaceContext)}.
     * @param prefix the namespace prefix
     * @param useDefault true if the default namespace for elements and types is to be used when the
     * prefix is ""
     * @return the uri for the namespace, or null if the prefix is not in scope.
     * Return "" if the prefix maps to the null namespace.
     */

    public String getURIForPrefix(String prefix, boolean useDefault) {
        if (prefix.equals("")) {
            if (useDefault) {
                return getDefaultElementNamespace();
            } else {
                return "";
            }
        } else {
            return namespaceContext.getNamespaceURI(prefix);
        }
    }

    /**
     * Get an iterator over all the prefixes declared in this namespace context. This method is implemented
     * only in the case where the NamespaceContext supplied using {@link #setNamespaceContext} is an
     * instance of Saxon's {@link NamespaceResolver} class. In other cases the method throws an
     * UnsupportedOperationException
     * @return an iterator over all the inscope namespace prefixes, if available
     * @throws UnsupportedOperationException if the NamespaceContext object is not a NamespaceResolver.
     */

    public Iterator iteratePrefixes() {
        if (namespaceContext instanceof NamespaceResolver) {
            return ((NamespaceResolver)namespaceContext).iteratePrefixes();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Bind a variable used in an XPath Expression to the XSLVariable element in which it is declared.
     * This method is provided for use by the XPath parser, and it should not be called by the user of
     * the API.
     *
     * @throws XPathException if no VariableResolver has been supplied.
     * @param qName
     */

    public final VariableReference bindVariable(StructuredQName qName) throws XPathException {
        // bindVariable is called at compile time, but the JAXP variable resolver
        // is designed to be called at run time. So we need to create a variable now,
        // which will call the variableResolver when called upon to return the run-time value
        if (variableResolver != null) {
            return new VariableReference(new JAXPVariable(qName, variableResolver));
        } else {
            throw new XPathException(
                    "Variable is used in XPath expression, but no JAXP VariableResolver is available");
        }
    }

    /**
     * Import a schema. This is possible only if the schema-aware version of Saxon is being used,
     * and if the Configuration is a SchemaAwareConfiguration. Having imported a schema, the types
     * defined in that schema become part of the static context.
     * @param source A Source object identifying the schema document to be loaded
     * @throws org.orbeon.saxon.type.SchemaException if the schema contained in this document is invalid
     * @throws UnsupportedOperationException if the configuration is not schema-aware
     */

    public void importSchema(Source source) throws SchemaException {
        getConfiguration().addSchemaSource(source, getConfiguration().getErrorListener());
    }

    /**
     * Determine whether a Schema for a given target namespace has been imported. Note that the
     * in-scope element declarations, attribute declarations and schema types are the types registered
     * with the (schema-aware) configuration, provided that their namespace URI is registered
     * in the static context as being an imported schema namespace. (A consequence of this is that
     * within a Configuration, there can only be one schema for any given namespace, including the
     * null namespace).
     * @return true if schema components for the given namespace have been imported into the
     * schema-aware configuration
     */

    public boolean isImportedSchema(String namespace) {
        return getConfiguration().isSchemaAvailable(namespace);
    }

    /**
     * Get the set of imported schemas
     *
     * @return a Set, the set of URIs representing the names of imported schemas
     */

    public Set getImportedSchemaNamespaces() {
        return getConfiguration().getImportedNamespaces();
    }

    /**
     * Define a minimal namespace context for use when no user-defined namespace context has been
     * registered
     */

    private static class MinimalNamespaceContext implements NamespaceContext, NamespaceResolver {

        /**
         * Get the namespace URI bound to a prefix in the current scope.</p>
         * @param prefix the prefix to look up
         * @return Namespace URI bound to prefix in the current scope
         */
        public String getNamespaceURI(String prefix) {
            if (prefix == null) {
                throw new IllegalArgumentException("prefix");
            } else if (prefix.equals(XMLConstants.DEFAULT_NS_PREFIX)) {
                return ""; //XMLConstants.NULL_NS_URI;
            } else if (prefix.equals("xml")) {
                return NamespaceConstant.XML;
            } else if (prefix.equals("xs")) {
                return NamespaceConstant.SCHEMA;
            } else if (prefix.equals("xsi")) {
                return NamespaceConstant.SCHEMA_INSTANCE;
            } else if (prefix.equals("saxon")) {
                return NamespaceConstant.SAXON;
            } else {
                return null;
            }
        }

        /**
         * <p>Get prefix bound to Namespace URI in the current scope.</p>
         * @throws UnsupportedOperationException (always)
         */
        public String getPrefix(String namespaceURI) {
            throw new UnsupportedOperationException();
        }

        /**
         * <p>Get all prefixes bound to a Namespace URI in the current
         * @throws UnsupportedOperationException (always)
         */
        public Iterator getPrefixes(String namespaceURI) {
            throw new UnsupportedOperationException();
        }

        /**
         * Get an iterator over all the prefixes declared in this namespace context. This will include
         * the default namespace (prefix="") and the XML namespace where appropriate
         */

        public Iterator iteratePrefixes() {
            String[] prefixes = {"", "xml", "xs", "xsi", "saxon"};
            return Arrays.asList(prefixes).iterator();
        }

        /**
         * Get the namespace URI corresponding to a given prefix. Return null
         * if the prefix is not in scope.
         *
         * @param prefix     the namespace prefix. May be the zero-length string, indicating
         *                   that there is no prefix. This indicates either the default namespace or the
         *                   null namespace, depending on the value of useDefault.
         * @param useDefault true if the default namespace is to be used when the
         *                   prefix is "". If false, the method returns "" when the prefix is "".
         * @return the uri for the namespace, or null if the prefix is not in scope.
         *         The "null namespace" is represented by the pseudo-URI "".
         */

        public String getURIForPrefix(String prefix, boolean useDefault) {
            return getNamespaceURI(prefix);
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
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
