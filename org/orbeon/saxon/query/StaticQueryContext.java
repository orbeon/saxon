package net.sf.saxon.query;
import net.sf.saxon.Configuration;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.expr.VariableDeclaration;
import net.sf.saxon.functions.*;
import net.sf.saxon.instruct.*;
import net.sf.saxon.om.*;
import net.sf.saxon.sort.CodepointCollator;
import net.sf.saxon.sort.CollationFactory;
import net.sf.saxon.xpath.XPathException;
import net.sf.saxon.xpath.StaticError;

import javax.xml.transform.Source;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * StaticQueryContext is the implementation of StaticContext used when processing XQuery
 * expressions. Note that some of the methods are intended for use internally by the
 * query processor itself.
 *
 * <p>Note that a StaticQueryContext object may be initialized with
     * context information that the query can use, but it is also modified when a query is compiled against
     * it: for example, namespaces, variables, and functions declared in the query prolog are registered in
     * the static context. Therefore, a StaticQueryContext object should not be used to compile more
     * than one query. </p>
*/

public class StaticQueryContext implements StaticContext {

	private Configuration config;
    private NamePool namePool;
	private HashMap passiveNamespaces;
    private Stack activeNamespaces;
	private HashMap collations;
	private HashMap variables;
    private List variableList;  // unlike the hashmap, this preserves the order in which variables were declared
    private HashSet importedSchemata;
	private String defaultCollationName;
    private String defaultFunctionNamespace;
    private short defaultElementNamespace;
    private String baseURI;
    private SlotManager stackFrameMap;  // map of the outermost local stackframe
    private String moduleNamespace;
    private short moduleNamespaceURICode;
    private int constructionMode;
    private Executable executable;

    private FunctionLibraryList functionLibraryList;
    private XQueryFunctionLibrary functions;

	/**
	* Create a StaticQueryContext using the default NamePool
	*/

	public StaticQueryContext(Configuration config) {
        this.config = config;
        this.namePool = config.getNamePool();
        reset();
	}

    /**
     * Reset the state of this StaticQueryContext to an uninitialized state
     */

    public void reset() {
        passiveNamespaces = new HashMap(10);
        activeNamespaces = new Stack();
        collations = new HashMap(5);
        variables = new HashMap(40);
        variableList = new ArrayList(40);
        functions = new XQueryFunctionLibrary(config, true);
        importedSchemata = new HashSet(5);
        defaultFunctionNamespace = NamespaceConstant.FN;
        defaultElementNamespace = NamespaceConstant.NULL_CODE;
        moduleNamespace = null;
        moduleNamespaceURICode = 0;
        constructionMode = Validation.PRESERVE;

        // Set up a "default default" collation based on the Java locale
        // String lang = Locale.getDefault().getLanguage();
        // defaultCollationName = "http://saxon.sf.net/collation?lang=" + lang + ";strength=tertiary";
        defaultCollationName = CodepointCollator.URI;
        declareCollation(defaultCollationName, CodepointCollator.getInstance());
//        try {
//            declareCollation(defaultCollationName, CollationFactory.makeCollationFromURI(defaultCollationName));
//        } catch (XPathException err) {
//            defaultCollationName = CodepointCollator.URI;
//            declareCollation(defaultCollationName, CodepointCollator.getInstance());
//        }

        functionLibraryList = new FunctionLibraryList();
        functionLibraryList.addFunctionLibrary(new SystemFunctionLibrary(config, false));
        functionLibraryList.addFunctionLibrary(config.getVendorFunctionLibrary());
        functionLibraryList.addFunctionLibrary(new ConstructorFunctionLibrary(config));
        if (config.isAllowExternalFunctions()) {
            functionLibraryList.addFunctionLibrary(new JavaExtensionLibrary(config));
        }
        functionLibraryList.addFunctionLibrary(functions);

        clearNamespaces();
    }

    /**
     * Set the Configuration options
     */

    public void setConfiguration(Configuration config) {
        this.config = config;
    }

    /**
     * Get the Configuration options
     */

    public Configuration getConfiguration() {
        return config;
    }

    /**
     * Convenience method for building Saxon's internal representation of a source XML
     * document. The document will be built using the default NamePool, which means that
     * any process that uses it must also use the default NamePool.
     * @param source Any javax.xml.transform.Source object representing the document against
     * which queries will be executed. Note that a Saxon {@link net.sf.saxon.om.DocumentInfo DocumentInfo}
     * (indeed any {@link net.sf.saxon.om.NodeInfo NodeInfo})
     * can be used as a Source. To use a third-party DOM Document as a source, create an instance of
     * {@link javax.xml.transform.dom.DOMSource DOMSource} to wrap it.
     * <p>For additional control over the way in which the source document is processed,
     * supply an {@link net.sf.saxon.AugmentedSource AugmentedSource} object and set appropriate options on the object.</p>
     * @return the DocumentInfo representing the root node of the resulting document object.
     */

    public DocumentInfo buildDocument(Source source) throws XPathException {
        return new QueryProcessor(this).buildDocument(source);
    }

    /**
     * Prepare an XQuery query for subsequent evaluation. The source text of the query
     * is supplied as a String. The base URI of the query is taken from the static context,
     * and defaults to the current working directory.
     * @param query The XQuery query to be evaluated, supplied as a string.
     * @return an XQueryExpression object representing the prepared expression
     * @throws net.sf.saxon.xpath.XPathException if the syntax of the expression is wrong,
     * or if it references namespaces, variables, or functions that have not been declared,
     * or contains other static errors.
    */

    public XQueryExpression compileQuery(String query) throws XPathException {
        return new QueryProcessor(this).compileQuery(query);
    }

    /**
    * Prepare an XQuery query for subsequent evaluation. The Query is supplied
     * in the form of a Reader. The base URI of the query is taken from the static context,
     * and defaults to the current working directory.
     * @param source A Reader giving access to the text of the XQuery query to be compiled.
     * @return an XPathExpression object representing the prepared expression.
     * @throws net.sf.saxon.xpath.XPathException if the syntax of the expression is wrong, or if it references namespaces,
     * variables, or functions that have not been declared, or any other static error is reported.
     * @throws java.io.IOException if a failure occurs reading the supplied input.
    */

    public XQueryExpression compileQuery(Reader source)
    throws XPathException, IOException {
        return new QueryProcessor(this).compileQuery(source);
    }

    public Executable getExecutable() {
        return executable;
    }

    public void setExecutable(Executable executable) {
        this.executable = executable;
    }

    public LocationMap getLocationMap() {
        return executable.getLocationMap();
    }

//    public void setLocationMap(LocationMap locationMap) {
//        this.locationMap = locationMap;
//    }

	/**
	* Declare a namespace whose prefix can be used in expressions. This is
     * a passive namespace, it won't be copied into the result tree. Passive
     * namespaces are never undeclared, and active namespaces override them.
	* @param prefix The namespace prefix. Must not be null.
	* @param uri The namespace URI. Must not be null.
	*/

	protected void declarePassiveNamespace(String prefix, String uri) {
	    if (prefix==null) {
	        throw new NullPointerException("Null prefix supplied to declarePassiveNamespace()");
	    }
	    if (uri==null) {
	        throw new NullPointerException("Null namespace URI supplied to declarePassiveNamespace()");
	    }
		passiveNamespaces.put(prefix, uri);
		namePool.allocateNamespaceCode(prefix, uri);
	}

    /**
     * Declare an active namespace
     */

    protected void declareActiveNamespace(String prefix, String uri) {
	    if (prefix==null) {
	        throw new NullPointerException("Null prefix supplied to declareActiveNamespace()");
	    }
	    if (uri==null) {
	        throw new NullPointerException("Null namespace URI supplied to declareActiveNamespace()");
	    }

        int nscode = namePool.allocateNamespaceCode(prefix, uri);
        ActiveNamespace entry = new ActiveNamespace();
        entry.prefix = prefix;
        entry.uri = uri;
        entry.code = nscode;
		activeNamespaces.push(entry);

        if (prefix.equals("")) {
            defaultElementNamespace = (short)(nscode & 0xffff);
        }

    }

	/**
	* Undeclare the most recently-declared active namespace
	*/

	public void undeclareNamespace() {
        ActiveNamespace entry = (ActiveNamespace)activeNamespaces.pop();
        if (entry.prefix.equals("")) {
            for (int i=activeNamespaces.size()-1; i>=0; i--) {
                if (((ActiveNamespace)activeNamespaces.get(i)).prefix.equals("")) {
                    defaultElementNamespace = (short)(((ActiveNamespace)activeNamespaces.get(i)).code & 0xffff);
                    return;
                }
            }
            defaultElementNamespace = 0;
        }
	}

	/**
	* Clear all the declared namespaces, except for the standard ones (xml, saxon, etc)
	*/

	public void clearNamespaces() {
        if (passiveNamespaces != null) {
            passiveNamespaces.clear();
            declarePassiveNamespace("xml", NamespaceConstant.XML);
            declarePassiveNamespace("saxon", NamespaceConstant.SAXON);
            declarePassiveNamespace("xs", NamespaceConstant.SCHEMA);
            declarePassiveNamespace("fn", NamespaceConstant.FN);
            declarePassiveNamespace("xdt", NamespaceConstant.XDT);
            declarePassiveNamespace("xsi", NamespaceConstant.SCHEMA_INSTANCE);
            declarePassiveNamespace("local", NamespaceConstant.LOCAL);
            declarePassiveNamespace("", "");
        }
	}

    /**
     * Get the URI for a prefix.
     * This method is used by the XQuery parser to resolve namespace prefixes.
     * @param prefix The prefix
     * @return the corresponding namespace URI
     * @throws XPathException if the prefix has not been declared
    */

    public String getURIForPrefix(String prefix) throws XPathException {
    	String uri = checkURIForPrefix(prefix);
    	if (uri==null) {
    		throw new StaticError("Prefix " + prefix + " has not been declared");
    	}
    	return uri;
    }

    /**
     * Get the URI for a prefix if there is one, return null if not.
     * This method is used by the XQuery parser to resolve namespace prefixes.
     * @param prefix The prefix
     * @return the corresponding namespace URI, or null if the prefix has not
     * been declared.
    */

    public String checkURIForPrefix(String prefix) {
        // Search the active namespaces first, then the passive ones.
        for (int i=activeNamespaces.size()-1; i>=0; i--) {
            if (((ActiveNamespace)activeNamespaces.get(i)).prefix.equals(prefix)) {
                return ((ActiveNamespace)activeNamespaces.get(i)).uri;
            }
        }
    	return (String)passiveNamespaces.get(prefix);
    }

    /**
     * Get an array containing the namespace codes of all active
     * namespaces.
     */

    public int[] getActiveNamespaceCodes() {
        int[] nscodes = new int[activeNamespaces.size()];
        int used = 0;
        HashSet prefixes = new HashSet(10);
        for (int n=activeNamespaces.size()-1; n>=0; n--) {
            ActiveNamespace an = (ActiveNamespace)activeNamespaces.get(n);
            if (!prefixes.contains(an.prefix)) {
                prefixes.add(an.prefix);
                nscodes[used++] = an.code;
            }
        }
        if (used < nscodes.length) {
            int[] nscodes2 = new int[used];
            System.arraycopy(nscodes, 0, nscodes2, 0, used);
            nscodes = nscodes2;
        }
        return nscodes;
    }

    /**
    * Get a copy of the Namespace Context. This method is used internally
     * by the query parser when a construct is encountered that needs
     * to save the namespace context for use at run-time.
    */

    public NamespaceResolver getNamespaceResolver() {
        int[] active = getActiveNamespaceCodes();
        int[] nscodes = new int[passiveNamespaces.size() + active.length];

        int used = 0;
        for (Iterator iter = passiveNamespaces.keySet().iterator(); iter.hasNext(); ) {
            String prefix = (String)iter.next();
            String uri = (String)passiveNamespaces.get(prefix);
            nscodes[used++] = namePool.getNamespaceCode(prefix, uri);;
        }
        for (int a=0; a<active.length; a++) {
            nscodes[used++] = active[a];
        }
//        if (used < nscodes.length) {
//            int[] nscodes2 = new int[used];
//            System.arraycopy(nscodes, 0, nscodes2, 0, used);
//            nscodes = nscodes2;
//        }
        return new NamespaceContext(nscodes, namePool);
    }

    /**
     * Get the default function namespace
     * @return the default function namespace (defaults to the fn: namespace)
     */

    public String getDefaultFunctionNamespace() {
        return defaultFunctionNamespace;
    }

    /**
     * Set the default function namespace
     * @param defaultFunctionNamespace The namespace to be used for unprefixed function calls
     */

    public void setDefaultFunctionNamespace(String defaultFunctionNamespace) {
        this.defaultFunctionNamespace = defaultFunctionNamespace;
    }

    /**
     * Set the default element namespace
     */

    protected void setDefaultElementNamespace(String uri) {
        int nscode = namePool.allocateNamespaceCode("", uri);
        defaultElementNamespace = (short)(nscode & 0xffff);
        declarePassiveNamespace("", uri);
    }

    /**
    * Get the default XPath namespace, as a namespace URI code that can be looked up in the NamePool
    */

    public short getDefaultElementNamespace() {
        return defaultElementNamespace;
    }

    /**
     * Set the namespace for a library module
     */

    public void setModuleNamespace(String uri) {
        moduleNamespace = uri;
        moduleNamespaceURICode = namePool.getCodeForURI(uri);
    }

    /**
     * Get the namespace of the current library module.
     * @return the module namespace, or null if this is a main module
     */

    public String getModuleNamespace() {
        return moduleNamespace;
    }

    /**
     * Get the namesapce code of the current library module.
     * @return the module namespace, or null if this is a main module
     */

    public short getModuleNamespaceCode() {
        return moduleNamespaceURICode;
    }

    /**
    * Declare a named collation. Collations are only available in a query if this method
     * has been called externally to declare the collation and associate it with an
     * implementation, in the form of a Java Comparator. The default collation is the
     * Unicode codepoint collation, unless otherwise specified.
    * @param name The name of the collation (technically, a URI)
    * @param comparator The Java Comparator used to implement the collating sequence
    */

    public void declareCollation(String name, Comparator comparator) {
        collations.put(name, comparator);
    }

    /**
     * Set the default collation. The collation that is specified must be one that has
     * been previously registered using the declareCollation() method.
     * @param name The collation name
     * @throws XPathException if the collation name has not been registered
     */

    public void declareDefaultCollation(String name) throws XPathException {
        Comparator c = getCollation(name);
        if (c==null) {
            throw new StaticError("Collation " + name + " is not recognized");
        }
        defaultCollationName = name;
    }

    /**
    * Get a named collation.
    * @return the collation identified by the given name, as set previously using declareCollation.
    * Return null if no collation with this name is found.
    */

    public Comparator getCollation(String name) {
        Comparator c = (Comparator)collations.get(name);
        if (c != null) {
            return c;
        }
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
     * Get a HashMap that maps all registered collations to Comparators.
     * Note that this returns a snapshot copy of the data held by the static context.
     * This method is provided for internal use by the query processor.
     */

    public HashMap getAllCollations() {
        return new HashMap(collations);
    }

    /**
     * Get the stack frame map for global variables
     */

    public SlotManager getGlobalStackFrameMap() {
        return executable.getGlobalVariableMap();
    }

    /**
    * Declare a global variable. A variable must be declared before an expression referring
    * to it is compiled. Global variables are normally declared in the Query Prolog, but
    * they can also be predeclared using this API.
    */

    public void declareVariable(VariableDeclaration var) throws StaticError {
        int key = var.getNameCode();
        Integer keyObj = new Integer(key);
        if (variables.get(keyObj) != null) {
            throw new StaticError(
                    "Duplicate definition of global variable " + var.getVariableName());
        }
        variables.put(keyObj, var);
        variableList.add(var);
    }

    /**
     * Fixup all references to global variables. This method is for internal use by
     * the Query Parser only.
     */

    public void fixupGlobalVariables(SlotManager globalVariableMap) throws StaticError {
        Iterator iter = variableList.iterator();
        while (iter.hasNext()) {
            GlobalVariableDefinition var = (GlobalVariableDefinition)iter.next();
            try {
                int slot = globalVariableMap.allocateSlotNumber(var.getNameCode()&0xfffff);
                var.compile(this, slot);
            } catch (XPathException err) {
                if (err instanceof StaticError) {
                    throw (StaticError)err;
                } else {
                    throw new StaticError(err);
                }
            }
        }
    }

    /**
     * Produce "explain" output for all global variables
     */

    public void explainGlobalVariables() {
        Iterator iter = variableList.iterator();
        while (iter.hasNext()) {
            GlobalVariableDefinition var = (GlobalVariableDefinition)iter.next();
            var.explain(getNamePool());
        }
    }

    /**
     * Get an iterator over the variables defined in this module
     * @return an Iterator, whose items are VariableDeclaration objects.  It returns
     * all variables known to this module including those imported from elsewhere; they
     * can be distinguished by their namespace. The variables are returned in order of
     * declaration.
     */

    public Iterator getVariableDeclarations() {
        return variableList.iterator();
    }

    /**
     * Get the stack frame map for local variables in the "main" query expression
     */

    public SlotManager getStackFrameMap() {
        if (stackFrameMap == null) {
            stackFrameMap = getConfiguration().makeSlotManager();
        }
        return stackFrameMap;
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
     * Set the Base URI of the query
     */

    public void setBaseURI(String baseURI) {
        this.baseURI = baseURI;
    }

    /**
    * Get the system ID of the container of the expression. Used to construct error messages.
    * @return the Base URI
    */

    public String getSystemId() {
        return baseURI;
    }

    /**
    * Get the Base URI of the query, for resolving any relative URI's used
    * in the expression.
    * Used by the document() function.
    * @return "" always
    */

    public String getBaseURI() {
        return baseURI;
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
    * Bind a variable used in a query to the expression in which it is declared.
    * This method is provided for use by the XQuery parser, and it should not be called by the user of
    * the API, or overridden, unless variables are to be declared using a mechanism other than the
    * declareVariable method of this class.
    */

    public VariableDeclaration bindVariable(int fingerprint) throws StaticError {

        VariableDeclaration var = (VariableDeclaration)variables.get(new Integer(fingerprint));
        if (var==null) {
            throw new StaticError("Undeclared variable in query");
        } else {
            return var;
        }
    }

    /**
     * Get the function library containing all the in-scope functions available in this static
     * context
     */

    public FunctionLibrary getFunctionLibrary() {
        return functionLibraryList;
    }

    /**
     * Register a user-defined XQuery function
     */

    public void declareFunction(XQueryFunction function) throws StaticError {
        int fp = function.getFunctionFingerprint();
//        int arity = function.getNumberOfArguments();
//        Long keyObj = new Long(((long)arity)<<32 + (long)fp);
//        if (functions.get(keyObj) != null) {
//            throw new XPathException.Static("Duplicate definition of function " +
//                    namePool.getDisplayName(fp));
//        }
        if (moduleNamespace != null &&
                namePool.getURICode(fp) != moduleNamespaceURICode) {
            throw new StaticError("Function " + namePool.getDisplayName(fp) +
                                            " is not defined in the module namespace"
                                            );
        }
        //functions.put(keyObj, function);
        functions.declareFunction(function);
    }

    /**
     * Bind function calls that could not be bound when first encountered. These
     * will either be forwards references to functions declared later in the query,
     * or errors. This method is for internal use.
     * @throws net.sf.saxon.xpath.StaticError if a function call refers to a function that has
     * not been declared
     */

    protected void bindUnboundFunctionCalls() throws StaticError {
        functions.bindUnboundFunctionCalls();
//        Iterator iter = unboundFunctionCalls.iterator();
//        while (iter.hasNext()) {
//            UserFunctionCall ufc = (UserFunctionCall)iter.next();
//            int fp = ufc.getFunctionNameCode() & 0xfffff;
//            int arity = ufc.getNumberOfArguments();
//            Long keyObj = new Long(((long)arity)<<32 + (long)fp);
//
//            // First try user-written functions
//
//            XQueryFunction fd = (XQueryFunction)functions.get(keyObj);
//            if (fd != null) {
//                ufc.setStaticType(fd.getResultType());
//                fd.registerReference(ufc);
//            } else {
//                throw new XPathException.Static("Function " +
//                        namePool.getDisplayName(fp) +
//                        " has not been declared",
//                        ExpressionTool.getLocator(ufc));
//            }
//        }
//
//         /*
//
//            // We've failed - no implementation of this function was found
//
//            throw new XPathException.Static("Unknown function: " + qname);
//
//        }  */

    }

    /**
     * Get an iterator over the Functions defined in this module
     * @return an Iterator, whose items are {@link XQueryFunction} objects. It returns
     * all function known to this module including those imported from elsewhere; they
     * can be distinguished by their namespace.
     */

    public Iterator getFunctionDefinitions() {
        return functions.getFunctionDefinitions();
    }

    /**
     * Fixup all references to global functions. This method is called
     * on completion of query parsing. Each XQueryFunction is required to
     * bind all references to that function to the object representing the run-time
     * executable code of the function.
     * <p>
     * This method is for internal use.
     */

    protected void fixupGlobalFunctions() throws StaticError {
        functions.fixupGlobalFunctions(this);
    }

    /**
     * Output "explain" information about each declared function
     */

     public void explainGlobalFunctions() throws XPathException {
        functions.explainGlobalFunctions();
    }

    /**
     * Get the function with a given name and arity. This method is provided so that XQuery functions
     * can be called directly from a Java application. Note that there is no type checking or conversion
     * of arguments when this is done: the arguments must be provided in exactly the form that the function
     * signature declares them.
     * @param uri the uri of the function name
     * @param localName the local part of the function name
     * @param arity the number of arguments.
     */

    public UserFunction getUserDefinedFunction(String uri, String localName, int arity) {
        return functions.getUserDefinedFunction(uri, localName, arity);
    }

    /**
     * Determine whether Backwards Compatible Mode is used
     * @return false; XPath 1.0 compatibility mode is not supported in XQuery
     */

    public boolean isInBackwardsCompatibleMode() {
        return false;
    }

    /**
     * Add an imported schema to this static context.
     * @param targetNamespace The target namespace of the schema to be added
     */

    public void addImportedSchema(String targetNamespace) {
        if (importedSchemata == null) {
            importedSchemata = new HashSet(5);
        }
        importedSchemata.add(targetNamespace);
    }

    /**
     * Get the schema for a given namespace, if it has been imported
     * @param namespace The namespace of the required schema. Supply "" for
     * a no-namespace schema.
     * @return The schema if found, or null if not found.
     */

    public boolean isImportedSchema(String namespace) {
        if (importedSchemata == null) {
            return false;
        }
        return importedSchemata.contains(namespace);
    }

    /**
     * Add a new validation mode to the stack of validation modes
     */

    public void setConstructionMode(int mode) {
        constructionMode = mode;
    }

    /**
     * Get the current validation mode
     */

    public int getConstructionMode() {
        return constructionMode;
    }

    /**
     * Load another query module
     */

    protected StaticQueryContext loadModule(String namespaceURI, String locationURI) throws StaticError {
        return loadQueryModule(config, executable, baseURI, namespaceURI, locationURI);
    }

    /**
     * Supporting method to load a query module. Used also by saxon:import-query in XSLT.
     * @param config The configuration
     * @param executable The Executation
     * @param baseURI The base URI of the invoking module
     * @param namespaceURI namespace of the query module to be loaded
     * @param locationURI location hint of the query module to be loaded
     * @return The StaticQueryContext representing the loaded query module
     * @throws net.sf.saxon.xpath.StaticError
     */

    public static StaticQueryContext loadQueryModule(Configuration config, Executable executable, String baseURI, String namespaceURI, String locationURI)
    throws StaticError {

        StaticQueryContext mod = executable.getQueryLibraryModule(namespaceURI);
        if (mod != null) {
            return mod;
        }

        if (locationURI == null) {
            throw new StaticError(
                    "import module must either specify a known namespace or a location");
        }
        // Resolve relative URI

        URL absoluteURL;
        if (baseURI==null) {    // no base URI available
            try {
                // the href might be an absolute URL
                absoluteURL = new URL(locationURI);
            } catch (MalformedURLException err) {
                // it isn't
                throw new StaticError("Cannot resolve URI (no base URI available)", err);
            }
        } else {
            try {
                absoluteURL = new URL(new URL(baseURI), locationURI);
            } catch (MalformedURLException err) {
                throw new StaticError("Cannot resolve relative URI", err);
            }
        }
        InputStream is;
        try {
            is = absoluteURL.openStream();
            BufferedReader reader = new BufferedReader(
                                        new InputStreamReader(is));

            StringBuffer sb = new StringBuffer(2048);
            char[] buffer = new char[2048];
            int actual;
            while (true) {
                actual = reader.read(buffer, 0, 2048);
                if (actual<0) break;
                sb.append(buffer, 0, actual);
            }
            reader.close();
            is.close();
            StaticQueryContext module = new StaticQueryContext(config);
            module.setBaseURI(absoluteURL.toString());
            module.setExecutable(executable);
            QueryParser qp = new QueryParser();
            qp.parseLibraryModule(sb.toString(), module);
            if (module.getModuleNamespace() == null) {
                throw new StaticError(
                        "Imported module must be a library module");
            }
            if (!module.getModuleNamespace().equals(namespaceURI)) {
                throw new StaticError(
                        "Imported module's namespace does not match requested namespace");
            }
            executable.addQueryLibraryModule(module);
            return module;
        } catch (java.io.IOException ioErr) {
            throw new StaticError(ioErr);
        }
    }

    /**
     * Inner class containing information about an active namespace entry
     */

    private static class ActiveNamespace {
        public String prefix;
        public String uri;
        public int code;
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
