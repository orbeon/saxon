package net.sf.saxon.xpath;
import net.sf.saxon.Configuration;
import net.sf.saxon.type.AtomicType;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.expr.VariableDeclaration;
import net.sf.saxon.functions.*;
import net.sf.saxon.instruct.LocationMap;
import net.sf.saxon.instruct.SlotManager;
import net.sf.saxon.om.*;
import net.sf.saxon.sort.CollationFactory;
import net.sf.saxon.trans.StaticError;
import net.sf.saxon.trans.Variable;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.QNameValue;

import javax.xml.namespace.NamespaceContext;
import javax.xml.transform.SourceLocator;
import javax.xml.xpath.XPathFunctionResolver;
import javax.xml.xpath.XPathVariableResolver;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;

/**
* A StandaloneContext provides a context for parsing an XPath expression
* in a context other than a stylesheet. In particular, it is used to support
* the JAXP 1.3 XPath API. This API does not actually expose the StaticContext
* object directly; rather, the static context (namespaces, variables, and functions)
 * is manipulated through the XPath object, implemented in Saxon by the {@link XPathEvaluator}
*/

public class StandaloneContext implements StaticContext, NamespaceResolver {

	private NamePool namePool;
	private HashMap namespaces = new HashMap(10);
	private HashMap collations = new HashMap(10);
	private HashMap variables = new HashMap(20);
    private SlotManager stackFrameMap;
	private String defaultCollationName = null;
    private String baseURI = null;
    private Configuration config;
    private LocationMap locationMap = new LocationMap();
    private FunctionLibrary functionLibrary;
    private XPathFunctionLibrary xpathFunctionLibrary;
    private String defaultFunctionNamespace = NamespaceConstant.FN;
    private short defaultElementNamespace = NamespaceConstant.NULL_CODE;
    private boolean backwardsCompatible = false;

    private NamespaceContext namespaceContext;
    private XPathVariableResolver variableResolver;

	/**
	* Create a StandaloneContext using the default Configuration and NamePool
	*/

	public StandaloneContext() {
	    this(new Configuration());
	}

	/**
	* Create a StandaloneContext using a specific NamePool
	*/

	public StandaloneContext(Configuration config) {
        this.config = config;
		namePool = config.getNamePool();
        stackFrameMap = config.makeSlotManager();
		clearNamespaces();

        // Set up a default function library. This can be overridden using setFunctionLibrary()

        FunctionLibraryList lib = new FunctionLibraryList();
        lib.addFunctionLibrary(new SystemFunctionLibrary(SystemFunctionLibrary.XPATH_ONLY));
        lib.addFunctionLibrary(getConfiguration().getVendorFunctionLibrary());
        lib.addFunctionLibrary(new ConstructorFunctionLibrary(getConfiguration()));
        if (config.isAllowExternalFunctions()) {
            xpathFunctionLibrary = new XPathFunctionLibrary();
            lib.addFunctionLibrary(xpathFunctionLibrary);
            lib.addFunctionLibrary(config.getExtensionBinder());
        }
        functionLibrary = lib;
	}

	/**
	* Create a StandaloneContext using a specific Node. This node is used to initialize
	* the NamePool and also to establish the initial set of in-scope namespaces
	*/

	public StandaloneContext(NodeInfo node) {
	    DocumentInfo doc = node.getDocumentRoot();
	    if (doc==null) {
	        throw new IllegalArgumentException(
	                    "The node used to establish a standalone context must be in a tree whose root is a document node");
	    }
	    namePool = doc.getNamePool();
	    setNamespaces(node);
	}

    /**
     * Get the system configuration
     */

    public Configuration getConfiguration() {
        return config;
    }

    public LocationMap getLocationMap() {
        return locationMap;
    }

    public void setLocationMap(LocationMap locationMap) {
        this.locationMap = locationMap;
    }

	/**
	* Declare a namespace whose prefix can be used in expressions. Namespaces may either be
     * pre-declared (the traditional Saxon interface), or they may be resolved on demand
     * using a supplied NamespaceContext. When a prefix has to be resolved, the parser looks
     * first in the pre-declared namespaces, then in the supplied NamespaceContext object.
	* @param prefix The namespace prefix. Must not be null. Must not be the empty string
	* ("") - unqualified names in an XPath expression always refer to the null namespace.
	* @param uri The namespace URI. Must not be null.
	*/

	public void declareNamespace(String prefix, String uri) {
	    if (prefix==null) {
	        throw new NullPointerException("Null prefix supplied to declareNamespace()");
	    }
	    if (uri==null) {
	        throw new NullPointerException("Null namespace URI supplied to declareNamespace()");
	    }
		namespaces.put(prefix, uri);
		namePool.allocateNamespaceCode(prefix, uri);
	}

    /**
     * Supply the NamespaceContext used to resolve namespaces. This supplements namespaces
     * that have been explicitly declared using {@link #declareNamespace} or
     * that have been implicitly declared using {@link #setNamespaces(net.sf.saxon.om.NodeInfo)}
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
	* Clear all the declared namespaces, except for the standard ones (xml, xslt, saxon, xdt).
     * This doesn't clear the namespace context set using {@link #setNamespaceContext}
	*/

	public void clearNamespaces() {
	    namespaces.clear();
		declareNamespace("xml", NamespaceConstant.XML);
		declareNamespace("xsl", NamespaceConstant.XSLT);
		declareNamespace("saxon", NamespaceConstant.SAXON);
		declareNamespace("xs", NamespaceConstant.SCHEMA);
		declareNamespace("xdt", NamespaceConstant.XDT);
        declareNamespace("", "");
	}

	/**
	* Clear all the declared namespaces, including the standard ones (xml, xslt, saxon).
     * Leave only the XML namespace and the default namespace (xmlns="")
	*/

	public void clearAllNamespaces() {
	    namespaces.clear();
		declareNamespace("xml", NamespaceConstant.XML);
		declareNamespace("", "");
	}

	/**
	* Set all the declared namespaces to be the namespaces that are in-scope for a given node.
	* In addition, the standard namespaces (xml, xslt, saxon) are declared.
	* @param node The node whose in-scope namespaces are to be used as the context namespaces.
	* Note that this will have no effect unless this node is an element.
	*/

	public void setNamespaces(NodeInfo node) {
	    namespaces.clear();
	    AxisIterator iter = node.iterateAxis(Axis.NAMESPACE);
	    while (true) {
	        NodeInfo ns = (NodeInfo)iter.next();
            if (ns == null) {
                return;
            }
	        declareNamespace(ns.getLocalPart(), ns.getStringValue());
	    }
	}

    /**
     * Set the base URI in the static context
     */

    public void setBaseURI(String baseURI) {
        this.baseURI = baseURI;
    }

    /**
    * Declare a named collation
    * @param name The name of the collation (technically, a URI)
    * @param comparator The Java Comparator used to implement the collating sequence
    * @param isDefault True if this is to be used as the default collation
    */

    public void declareCollation(String name, Comparator comparator, boolean isDefault) {
        collations.put(name, comparator);
        if (isDefault) {
            defaultCollationName = name;
        }
    }

    /**
     * Get the stack frame map containing the slot number allocations for the variables declared
     * in this static context
     */

    public SlotManager getStackFrameMap() {
        return stackFrameMap;
    }

    /**
    * Declare a variable. A variable may be declared before an expression referring
    * to it is compiled. Alternatively, a JAXP XPathVariableResolver may be supplied
     * to perform the resolution. A variable that has been explicitly declared is
     * used in preference.
     * @param qname Lexical QName identifying the variable. The namespace prefix, if
     * any, must have been declared before this method is called, or must be resolvable
     * using the namespace context.
     * @param initialValue The initial value of the variable. A Java object that can
     * be converted to an XPath value.
    */

    public Variable declareVariable(String qname, Object initialValue) throws XPathException {
        String prefix;
        String localName;
        try {
            String[] parts = Name.getQNameParts(qname);
            prefix = parts[0];
            localName = parts[1];
        } catch (QNameException err) {
            throw new StaticError("Invalid QName for variable: " + qname);
        }
        String uri = "";
        if (!("".equals(prefix))) {
            uri = getURIForPrefix(prefix);
        }
        Variable var = Variable.make(qname, getConfiguration());
        if (initialValue instanceof ValueRepresentation) {
            var.setXPathValue((ValueRepresentation)initialValue);
        } else {
            var.setValue(initialValue);
        }
        int fingerprint = namePool.allocate(prefix, uri, localName) & 0xfffff;
        variables.put(new Integer(fingerprint), var);
        stackFrameMap.allocateSlotNumber(fingerprint);
        return var;
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
        // otherwise, external functions are disabled for security reasons
    }

    public XPathFunctionResolver getXPathFunctionResolver() {
        if (xpathFunctionLibrary != null) {
            return xpathFunctionLibrary.getXPathFunctionResolver();
        } else {
            return null;
        }
    }

    /**
    * Get the NamePool used for compiling expressions
    */

    public NamePool getNamePool() {
        return namePool;
    }

    /**
    * Issue a compile-time warning. This method is used during XPath expression compilation to
    * output warning conditions. The default implementation writes the message to System.err. To
    * change the destination of messages, create a subclass of StandaloneContext that overrides
    * this method.
    */

    public void issueWarning(String s, SourceLocator locator) {
        System.err.println(s);
    }

    /**
    * Get the system ID of the container of the expression. Used to construct error messages.
    * @return "" always
    */

    public String getSystemId() {
        return "";
    }

    /**
    * Get the Base URI of the stylesheet element, for resolving any relative URI's used
    * in the expression.
    * Used by the document() function, resolve-uri(), etc.
    * @return "" if no base URI has been set
    */

    public String getBaseURI() {
        return baseURI==null ? "" : baseURI;
    }

    /**
    * Get the line number of the expression within that container.
    * Used to construct error messages.
    * @return -1 always
    */

    public int getLineNumber() {
        return -1;
    }

    /**
     * Get the URI for a prefix, using the declared namespaces as
     * the context for namespace resolution. The default namespace is NOT used
     * when the prefix is empty.
     * This method is provided for use by the XPath parser.
     * @param prefix The prefix
     * @throws net.sf.saxon.trans.XPathException if the prefix is not declared
    */

    public String getURIForPrefix(String prefix) throws XPathException {
        String uri = getURIForPrefix(prefix, false);
    	if (uri==null) {
    		throw new StaticError("Prefix " + prefix + " has not been declared");
    	}
    	return uri;
    }

    public NamespaceResolver getNamespaceResolver() {
        return this;
    }

    /**
     * Get the namespace URI corresponding to a given prefix. Return null
     * if the prefix is not in scope. This method first searches any namespaces
     * declared using {@link #declareNamespace(String, String)}, and then searches
     * any namespace context supplied using {@link #setNamespaceContext(javax.xml.namespace.NamespaceContext)}.
     * @param prefix the namespace prefix
     * @param useDefault true if the default namespace is to be used when the
     * prefix is ""
     * @return the uri for the namespace, or null if the prefix is not in scope.
     * Return "" if the prefix maps to the null namespace.
     */

    public String getURIForPrefix(String prefix, boolean useDefault) {
        if (prefix.equals("") && !useDefault) {
            return "";
        } else {
    	    String uri = (String)namespaces.get(prefix);
            if (uri == null && namespaceContext != null) {
                return namespaceContext.getNamespaceURI(prefix);
            } else {
                return uri;
            }
        }
    }

    /**
     * Get an iterator over all the prefixes declared in this namespace context. This will include
     * the default namespace (prefix="") and the XML namespace where appropriate. The iterator only
     * covers namespaces explicitly declared using {@link #declareNamespace(String, String)}; it does not
     * include namespaces declared using {@link #setNamespaceContext(javax.xml.namespace.NamespaceContext)},
     * because the JAXP {@link NamespaceContext} class provides no way to discover all the namespaces
     * available.
     */

    public Iterator iteratePrefixes() {
        return namespaces.keySet().iterator();
    }

    /**
     * Bind a variable used in an XPath Expression to the XSLVariable element in which it is declared.
     * This method is provided for use by the XPath parser, and it should not be called by the user of
     * the API, or overridden, unless variables are to be declared using a mechanism other than the
     * declareVariable method of this class.
     * <p>
     * If the variable has been explicitly declared using {@link #declareVariable(String, Object)},
     * that value is used; otherwise if a variable resolved has been supplied using
     * {@link #setXPathVariableResolver(javax.xml.xpath.XPathVariableResolver)} then that is used.
     * @throws StaticError If no variable with the given name is found, or if the value supplied
     * for the variable cannot be converted to an XPath value.
     */

    public VariableDeclaration bindVariable(int fingerprint) throws StaticError {
        Variable var = (Variable)variables.get(new Integer(fingerprint));
        if (var!=null) {
            return var;
        }
        // bindVariable is called at compile time, but the JAXP variable resolver
        // is designed to be called at run time. So we need to create a variable now,
        // which will call the variableResolver when called upon to return the run-time value
        if (variableResolver != null) {
            QNameValue qname = new QNameValue(namePool, fingerprint);

            return new JAXPVariable(qname, variableResolver);
        }
        throw new StaticError("Undeclared variable in a standalone expression");

    }

    /**
     * Get the function library containing all the in-scope functions available in this static
     * context
     */

    public FunctionLibrary getFunctionLibrary() {
        return functionLibrary;
    }

    /**
     * Set the function library to be used
     */

    public void setFunctionLibrary(FunctionLibrary lib) {
        functionLibrary = lib;
    }

    /**
    * Get a named collation.
    * @return the collation identified by the given name, as set previously using declareCollation.
    * Return null if no collation with this name is found.
    */

    public Comparator getCollation(String name) {
        try {
            return CollationFactory.makeCollationFromURI(name, getConfiguration());
        } catch (XPathException e) {
            return null;
        }
    }

    /**
    * Get the name of the default collation.
    * @return the name of the default collation; or the name of the codepoint collation
    * if no default collation has been defined
    */

    public String getDefaultCollationName() {
        if (defaultCollationName != null) {
            return defaultCollationName;
        } else {
            return NamespaceConstant.CodepointCollationURI;
        }
    }

    /**
     * Set the default namespace for element and type names
     */

    public void setDefaultElementNamespace(String uri) {
        defaultElementNamespace = namePool.allocateCodeForURI(uri);
    }

    /**
    * Get the default XPath namespace, as a namespace code that can be looked up in the NamePool
    */

    public short getDefaultElementNamespace() {
        return defaultElementNamespace;
    }

    /**
     * Set the default function namespace
     */

    public void setDefaultFunctionNamespace(String uri) {
        defaultFunctionNamespace = uri;
    }

    /**
     * Get the default function namespace
     */

    public String getDefaultFunctionNamespace() {
        return defaultFunctionNamespace;
    }

    /**
     * Set XPath 1.0 backwards compatibility mode
     * @param backwardsCompatible if true, expressions will be evaluated with
     *  XPath 1.0 compatibility mode set to true.
     */

    public void setBackwardsCompatibilityMode(boolean backwardsCompatible) {
        this.backwardsCompatible = true;
    }
    /**
     * Determine whether Backwards Compatible Mode is used
     * @return false; XPath 1.0 compatibility mode is not supported in the standalone
     * XPath API
     */

    public boolean isInBackwardsCompatibleMode() {
        return backwardsCompatible;
    }

    /**
     * Determine whether a Schema for a given target namespace has been imported. Note that the
     * in-scope element declarations, attribute declarations and schema types are the types registered
     * with the (schema-aware) configuration, provided that their namespace URI is registered
     * in the static context as being an imported schema namespace. (A consequence of this is that
     * within a Configuration, there can only be one schema for any given namespace, including the
     * null namespace).
     * @return This implementation always returns false: the standalone XPath API does not support
     * schema-aware processing.
     */

    public boolean isImportedSchema(String namespace) {
        return false;
    }

    /**
     * Determine whether a built-in type is available in this context. This method caters for differences
     * between host languages as to which set of types are built in.
     *
     * @param type the supposedly built-in type. This will always be a type in the
     *             XS or XDT namespace.
     * @return true if this type can be used in this static context
     */

    public boolean isAllowedBuiltInType(AtomicType type) {
        return true;
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
