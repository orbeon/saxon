package org.orbeon.saxon.xpath;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.expr.StaticContext;
import org.orbeon.saxon.expr.VariableDeclaration;
import org.orbeon.saxon.functions.*;
import org.orbeon.saxon.instruct.LocationMap;
import org.orbeon.saxon.instruct.SlotManager;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.sort.CodepointCollator;
import org.orbeon.saxon.sort.CollationFactory;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;

/**
* A StandaloneContext provides a context for parsing an expression or pattern appearing
* in a context other than a stylesheet.
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
    private String defaultFunctionNamespace = NamespaceConstant.FN;

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
        lib.addFunctionLibrary(new SystemFunctionLibrary(getConfiguration(), false));
        lib.addFunctionLibrary(getConfiguration().getVendorFunctionLibrary());
        lib.addFunctionLibrary(new ConstructorFunctionLibrary(getConfiguration()));
        if (config.isAllowExternalFunctions()) {
            lib.addFunctionLibrary(new JavaExtensionLibrary(getConfiguration()));
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
	* Declare a namespace whose prefix can be used in expressions
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
	* Clear all the declared namespaces, except for the standard ones (xml, xslt, saxon, xdt)
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
    * Declare a variable. A variable must be declared before an expression referring
    * to it is compiled.
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
        Variable var = Variable.make(qname);
        var.setValue(initialValue);
        int fingerprint = namePool.allocate(prefix, uri, localName) & 0xfffff;
        variables.put(new Integer(fingerprint), var);
        stackFrameMap.allocateSlotNumber(fingerprint);
        return var;
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

    public void issueWarning(String s) {
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
     * @throws XPathException if the prefix is not declared
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
     * if the prefix is not in scope.
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
    	    return (String)namespaces.get(prefix);
        }
    }

    /**
     * Use this NamespaceContext to resolve a lexical QName
     * @param qname the lexical QName; this must have already been lexically validated
     * @param useDefault true if the default namespace is to be used to resolve an unprefixed QName
     * @param pool the NamePool to be used
     * @return the integer fingerprint that uniquely identifies this name
     */

    public int getFingerprint(String qname, boolean useDefault, NamePool pool) {
        // TODO: implement this!
        return -1;
    }

    /**
     * Get an iterator over all the prefixes declared in this namespace context. This will include
     * the default namespace (prefix="") and the XML namespace where appropriate
     */

    public Iterator iteratePrefixes() {
        // TODO: implement this!
        return null;
    }

    /**
    * Bind a variable used in an XPath Expression to the XSLVariable element in which it is declared.
    * This method is provided for use by the XPath parser, and it should not be called by the user of
    * the API, or overridden, unless variables are to be declared using a mechanism other than the
    * declareVariable method of this class.
    */

    public VariableDeclaration bindVariable(int fingerprint) throws StaticError {
        Variable var = (Variable)variables.get(new Integer(fingerprint));
        if (var==null) {
            throw new StaticError("Undeclared variable in a standalone expression");
        } else {
            return var;
        }
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
            return CollationFactory.makeCollationFromURI(name);
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
            return CodepointCollator.URI;
        }
    }

    /**
    * Get the default XPath namespace, as a namespace code that can be looked up in the NamePool
    */

    public short getDefaultElementNamespace() {
        return NamespaceConstant.NULL_CODE;
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
     * Determine whether Backwards Compatible Mode is used
     * @return false; XPath 1.0 compatibility mode is not supported in the standalone
     * XPath API
     */

    public boolean isInBackwardsCompatibleMode() {
        return false;
    }

    public boolean isImportedSchema(String namespace) {
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
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
