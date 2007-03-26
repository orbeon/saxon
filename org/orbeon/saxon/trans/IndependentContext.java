package org.orbeon.saxon.trans;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.sort.IntHashMap;
import org.orbeon.saxon.value.QNameValue;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.functions.ConstructorFunctionLibrary;
import org.orbeon.saxon.functions.FunctionLibrary;
import org.orbeon.saxon.functions.FunctionLibraryList;
import org.orbeon.saxon.functions.SystemFunctionLibrary;
import org.orbeon.saxon.instruct.LocationMap;
import org.orbeon.saxon.instruct.SlotManager;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.type.AtomicType;
import org.orbeon.saxon.type.Type;

import javax.xml.transform.SourceLocator;
import java.io.Serializable;
import java.util.*;

/**
* An IndependentContext provides a context for parsing an expression or pattern appearing
* in a context other than a stylesheet.
 *
 * This class was formerly named StandaloneContext. It has forked from that class, so that
 * StandaloneContext could be changed to meet the demands of the JAXP 1.3 XPath API.
*/

public class IndependentContext implements StaticContext, NamespaceResolver, Serializable {

	private NamePool namePool;
	private HashMap namespaces = new HashMap(10);
	private CollationMap collations;
	private IntHashMap variables = new IntHashMap(20);
    private SlotManager stackFrameMap;
    private String baseURI = null;
    private Configuration config;
    private LocationMap locationMap = new LocationMap();
    private FunctionLibrary functionLibrary;
    private String defaultFunctionNamespace = NamespaceConstant.FN;
    private NamespaceResolver externalResolver = null;
    private Set importedSchemaNamespaces = Collections.EMPTY_SET;
    private short defaultElementNamespaceCode = NamespaceConstant.NULL_CODE;

    public IndependentContext() {
        this(new Configuration());
    }

    /**
	* Create an IndependentContext using a specific Configuration
	*/

	public IndependentContext(Configuration config) {
        this.config = config;
		namePool = config.getNamePool();
        stackFrameMap = config.makeSlotManager();
        collations = new CollationMap(config);
		clearNamespaces();

        // Set up a default function library. This can be overridden using setFunctionLibrary()

        FunctionLibraryList lib = new FunctionLibraryList();
        lib.addFunctionLibrary(new SystemFunctionLibrary(SystemFunctionLibrary.XPATH_ONLY));
        lib.addFunctionLibrary(getConfiguration().getVendorFunctionLibrary());
        lib.addFunctionLibrary(new ConstructorFunctionLibrary(getConfiguration()));
        if (config.isAllowExternalFunctions()) {
            lib.addFunctionLibrary(config.getExtensionBinder());
        }
        functionLibrary = lib;
    }

    /**
     * Create a copy of this IndependentContext. All aspects of the context are copied
     * except for declared variables.
     */

    public IndependentContext copy() {
        IndependentContext ic = new IndependentContext(config);
        ic.namespaces = new HashMap(namespaces);
        ic.collations = new CollationMap(collations);
        ic.variables = new IntHashMap(10);
        ic.baseURI = baseURI;
        ic.locationMap = locationMap;
        ic.functionLibrary = functionLibrary;
        ic.defaultFunctionNamespace = defaultFunctionNamespace;
        ic.importedSchemaNamespaces = importedSchemaNamespaces;
        ic.externalResolver = externalResolver;
        ic.defaultElementNamespaceCode = defaultElementNamespaceCode;
        return ic;
    }

    /**
     * Get the system configuration
     */

    public Configuration getConfiguration() {
        return config;
    }

    /**
     * Construct a dynamic context for early evaluation of constant subexpressions
     */

    public XPathContext makeEarlyEvaluationContext() {
        return new EarlyEvaluationContext(getConfiguration(), collations);
    }


    public LocationMap getLocationMap() {
        return locationMap;
    }

    public void setLocationMap(LocationMap locationMap) {
        this.locationMap = locationMap;
    }

	/**
	* Declare a namespace whose prefix can be used in expressions
	* @param prefix The namespace prefix. Must not be null. Supplying "" sets the
     * default element namespace.
	* @param uri The namespace URI. Must not be null.
	*/

	public void declareNamespace(String prefix, String uri) {
	    if (prefix==null) {
	        throw new NullPointerException("Null prefix supplied to declareNamespace()");
	    }
	    if (uri==null) {
	        throw new NullPointerException("Null namespace URI supplied to declareNamespace()");
	    }
        if ("".equals(prefix)) {
            defaultElementNamespaceCode = (short)(namePool.allocateNamespaceCode(prefix, uri) & 0xffff);
        } else {
		    namespaces.put(prefix, uri);
		    namePool.allocateNamespaceCode(prefix, uri);
        }
	}

	/**
	 * Clear all the declared namespaces, except for the standard ones (xml, xslt, saxon, xdt).
     * This also resets the default element namespace to the "null" namespace
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
     * Leave only the XML namespace and the default namespace (xmlns="").
     * This also resets the default element namespace to the "null" namespace.
	*/

	public void clearAllNamespaces() {
	    namespaces.clear();
		declareNamespace("xml", NamespaceConstant.XML);
		declareNamespace("", "");
	}

	/**
	* Declares all the namespaces that are in-scope for a given node, removing all previous
    * namespace declarations.
	* In addition, the standard namespaces (xml, xslt, saxon) are declared. This method also
    * sets the default element namespace to be the same as the default namespace for this node.
	* @param node The node whose in-scope namespaces are to be used as the context namespaces.
	* If the node is an attribute, text node, etc, then the namespaces of its parent element are used.
	*/

	public void setNamespaces(NodeInfo node) {
	    namespaces.clear();
        int kind = node.getNodeKind();
        if (kind == Type.ATTRIBUTE || kind == Type.TEXT ||
                kind == Type.COMMENT || kind == Type.PROCESSING_INSTRUCTION ||
                kind == Type.NAMESPACE) {
            node = node.getParent();
        }
        if (node == null) {
            return;
        }

	    AxisIterator iter = node.iterateAxis(Axis.NAMESPACE);
	    while (true) {
	        NodeInfo ns = (NodeInfo)iter.next();
            if (ns == null) {
                return;
            }
            String prefix = ns.getLocalPart();
            if ("".equals(prefix)) {
                setDefaultElementNamespace(ns.getStringValue());
            } else {
	            declareNamespace(ns.getLocalPart(), ns.getStringValue());
            }
	    }
	}

    /**
     * Set an external namespace resolver. If this is set, then all resolution of namespace
     * prefixes is delegated to the external namespace resolver, and namespaces declared
     * individually on this IndependentContext object are ignored.
     */

    public void setNamespaceResolver(NamespaceResolver resolver) {
        this.externalResolver = resolver;
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
        collations.setNamedCollation(name, comparator);
        if (isDefault) {
            collations.setDefaultCollationName(name);
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
     * to it is compiled. The initial value of the variable will be the empty sequence
     * @param qname The name of the variable
     * @return a Variable object representing information about the variable that has been
     * declared.
    */

    public Variable declareVariable(QNameValue qname) {
        Variable var = Variable.make(qname, getConfiguration());
        var.setXPathValue(null);
        int fingerprint = qname.allocateNameCode(getNamePool()) & 0xfffff;
        variables.put(fingerprint, var);
        int slot = stackFrameMap.allocateSlotNumber(fingerprint);
        var.setSlotNumber(slot);
        return var;
    }

    /**
    * Declare a variable. A variable may be declared before an expression referring
    * to it is compiled. Alternatively, a JAXP XPathVariableResolver may be supplied
     * to perform the resolution. A variable that has been explicitly declared is
     * used in preference.
     * @param qname Lexical QName identifying the variable. The namespace prefix, if
     * any, must have been declared before this method is called, or must be resolvable
     * using the namespace context.
    */

    public Variable declareVariable(String qname) throws XPathException {
        String prefix;
        String localName;
        final NameChecker checker = getConfiguration().getNameChecker();
        try {
            String[] parts = checker.getQNameParts(qname);
            prefix = parts[0];
            localName = parts[1];
        } catch (QNameException err) {
            throw new StaticError("Invalid QName for variable: " + qname);
        }
        String uri = "";
        if (!("".equals(prefix))) {
            uri = getURIForPrefix(prefix);
        }
        QNameValue q = new QNameValue(prefix, uri, localName, checker);
        Variable var = Variable.make(q, getConfiguration());
        int fingerprint = namePool.allocate(prefix, uri, localName) & 0xfffff;
        variables.put(fingerprint, var);
        int slot = stackFrameMap.allocateSlotNumber(fingerprint);
        var.setSlotNumber(slot);
        return var;
    }

    /**
     * Get the slot number allocated to a particular variable
     * @return the slot number, or -1 if the variable has not been declared
     */

    public int getSlotNumber(QNameValue qname) {
        int fingerprint = qname.allocateNameCode(getNamePool()) & 0xfffff;
        Variable var = (Variable)variables.get(fingerprint);
        if (var == null) {
            return -1;
        }
        return var.getLocalSlotNumber();
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
     * @throws org.orbeon.saxon.trans.XPathException if the prefix is not declared
    */

    public String getURIForPrefix(String prefix) throws XPathException {
        String uri = getURIForPrefix(prefix, false);
    	if (uri==null) {
    		throw new StaticError("Prefix " + prefix + " has not been declared");
    	}
    	return uri;
    }

    public NamespaceResolver getNamespaceResolver() {
        if (externalResolver != null) {
            return externalResolver;
        } else {
            return this;
        }
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
        if (externalResolver != null) {
            return externalResolver.getURIForPrefix(prefix, useDefault);
        }
        if (prefix.equals("") && !useDefault) {
            if (useDefault) {
                return namePool.getURIFromURICode(defaultElementNamespaceCode);
            } else {
                return "";
            }
        } else {
    	    return (String)namespaces.get(prefix);
        }
    }

    /**
     * Get an iterator over all the prefixes declared in this namespace context. This will include
     * the default namespace (prefix="") and the XML namespace where appropriate
     */

    public Iterator iteratePrefixes() {
        if (externalResolver != null) {
            return externalResolver.iteratePrefixes();
        } else {
            return namespaces.keySet().iterator();
        }
    }

    /**
    * Bind a variable used in an XPath Expression to the XSLVariable element in which it is declared.
    * This method is provided for use by the XPath parser, and it should not be called by the user of
    * the API, or overridden, unless variables are to be declared using a mechanism other than the
    * declareVariable method of this class.
    */

    public VariableReference bindVariable(int fingerprint) throws StaticError {
        Variable var = (Variable)variables.get(fingerprint);
        if (var==null) {
            throw new StaticError("Undeclared variable in a standalone expression");
        } else {
            return new VariableReference(var);
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
        return collations.getNamedCollation(name);
    }

    /**
    * Get the name of the default collation.
    * @return the name of the default collation; or the name of the codepoint collation
    * if no default collation has been defined
    */

    public String getDefaultCollationName() {
        return collations.getDefaultCollationName();
    }

    /**
    * Get the default XPath namespace, as a namespace code that can be looked up in the NamePool
    */

    public short getDefaultElementNamespace() {
        return defaultElementNamespaceCode;
    }

    /**
     * Set the default namespace for elements and types
     */

    public void setDefaultElementNamespace(String uri) {
        int nsCode = namePool.allocateNamespaceCode("", uri);
        defaultElementNamespaceCode = (short)(nsCode & 0xffff);
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
        return importedSchemaNamespaces.contains(namespace);
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

    /**
     * Get the set of imported schemas
     *
     * @return a Set, the set of URIs representing the names of imported schemas
     */

    public Set getImportedSchemaNamespaces() {
        return importedSchemaNamespaces;
    }

    public void setImportedSchemaNamespaces(Set namespaces) {
        importedSchemaNamespaces = namespaces;
    }

//    public void setSchemaImporter(StaticContext importer) {
//        schemaImporter = importer;
//    }

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
