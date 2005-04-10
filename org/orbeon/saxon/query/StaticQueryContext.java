package net.sf.saxon.query;
import net.sf.saxon.Configuration;
import net.sf.saxon.event.Builder;
import net.sf.saxon.event.Stripper;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.expr.VariableDeclaration;
import net.sf.saxon.functions.*;
import net.sf.saxon.instruct.*;
import net.sf.saxon.om.*;
import net.sf.saxon.sort.CodepointCollator;
import net.sf.saxon.sort.CollationFactory;
import net.sf.saxon.trans.StaticError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.AtomicType;
import net.sf.saxon.type.SchemaType;
import org.xml.sax.SAXParseException;

import javax.xml.transform.Source;
import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerException;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * StaticQueryContext is the implementation of StaticContext used when processing XQuery
 * expressions.
 * <p>
 * The StaticQueryContext object has two different usages. The application constructs a StaticQueryContext
 * and initializes it with information about the context, for example, default namespaces, base URI, and so on.
 * When a query is compiled using this StaticQueryContext, the query parser makes a copy of the StaticQueryContext
 * and uses this internally, modifying it with information obtained from the query prolog, as well as information
 * such as namespace and variable declarations that can occur at any point in the query. The query parser does
 * not modify the original StaticQueryContext supplied by the calling application, which may therefore be used
 * for compiling multiple queries, serially or even in multiple threads.
 * <p>
 * This class forms part of Saxon's published XQuery API.
 * Note that some of the methods are intended for use internally by the
 * query processor itself: these are labelled as such. Methods that
 * are considered stable are labelled with the JavaDoc "since" tag.
 * The value 8.4 indicates a method introduced at or before Saxon 8.4; other
 * values indicate the version at which the method was introduced.
 * <p>
 * In the longer term, this entire API may at some stage be superseded by a proposed
 * standard Java API for XQuery.
 * @since 8.4
*/

public class StaticQueryContext implements StaticContext {

    private Configuration config;
    private NamePool namePool;
	private HashMap passiveNamespaces;
    private HashSet explicitPrologNamespaces;
    private Stack activeNamespaces;
    private boolean inheritNamespaces = true;
    private boolean preserveNamespaces = true;
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
    private StaticQueryContext importer;
    private FunctionLibraryList functionLibraryList;
    private XQueryFunctionLibrary functions;

    private StaticQueryContext() {}

	/**
	* Create a StaticQueryContext using the default NamePool
     * @since 8.4
	*/

	public StaticQueryContext(Configuration config) {
        this.config = config;
        this.namePool = config.getNamePool();
        reset();
	}

    /**
     * Reset the state of this StaticQueryContext to an uninitialized state
     * @since 8.4
     */

    public void reset() {
        passiveNamespaces = new HashMap(10);
        explicitPrologNamespaces = new HashSet(10);
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
        defaultCollationName = NamespaceConstant.CodepointCollationURI;
        declareCollation(defaultCollationName, CodepointCollator.getInstance());
        functionLibraryList = new FunctionLibraryList();
        functionLibraryList.addFunctionLibrary(new SystemFunctionLibrary(SystemFunctionLibrary.XPATH_ONLY));
        functionLibraryList.addFunctionLibrary(config.getVendorFunctionLibrary());
        functionLibraryList.addFunctionLibrary(new ConstructorFunctionLibrary(config));
        if (config.isAllowExternalFunctions()) {
            functionLibraryList.addFunctionLibrary(config.getExtensionBinder());
        }
        functionLibraryList.addFunctionLibrary(functions);

        clearPassiveNamespaces();
    }

    /**
     * Make a copy of this StaticQueryContext. The StaticQueryContext that is constructed by a user
     * application and passed to Saxon when a query is compiled should not be modified by the query
     * compiler. Saxon therefore makes a copy of the StaticQueryContext and uses this copy internally,
     * to capture any changes to the StaticQueryContext defined in the query prolog.
     * @return a copy of this StaticQueryContext
     */

    public StaticQueryContext copy() {
        StaticQueryContext n = new StaticQueryContext();
        n.config = config;
        n.namePool = namePool;
        n.passiveNamespaces = new HashMap(passiveNamespaces);
        n.explicitPrologNamespaces = new HashSet(explicitPrologNamespaces);
        n.activeNamespaces = new Stack();
        n.inheritNamespaces = inheritNamespaces;
        n.preserveNamespaces = preserveNamespaces;
        n.collations = new HashMap(collations);
        n.variables = new HashMap(variables);
        n.variableList = new ArrayList(variableList);
        n.importedSchemata = new HashSet(importedSchemata);
        n.defaultCollationName = defaultCollationName;
        n.defaultFunctionNamespace = defaultFunctionNamespace;
        n.defaultElementNamespace = defaultElementNamespace;
        n.baseURI = baseURI;
        n.stackFrameMap = stackFrameMap;
        n.moduleNamespace = moduleNamespace;
        n.moduleNamespaceURICode = moduleNamespaceURICode;
        n.constructionMode = constructionMode;
        n.executable = executable;
        n.importer = importer;
        n.functionLibraryList = (FunctionLibraryList)functionLibraryList.copy();
        List list = n.functionLibraryList.getLibraryList();
        for (int i=0; i<list.size(); i++) {
            if (list.get(i) instanceof XQueryFunctionLibrary) {
                n.functions = (XQueryFunctionLibrary)list.get(i);
                break;
            }
        }
        return n;
    }

    /**
     * Set the Configuration options
     * @since 8.4
     */

    public void setConfiguration(Configuration config) {
        this.config = config;
    }

    /**
     * Get the Configuration options
     * @since 8.4
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
     * @since 8.4
     */

    public DocumentInfo buildDocument(Source source) throws XPathException {
        Stripper stripper = null;
        if (config.isStripsAllWhiteSpace()) {
            stripper = AllElementStripper.getInstance();
            stripper.setStripAll();
        }
        try {
            NodeInfo contextNode = Builder.build(source, stripper, config);
            return contextNode.getDocumentRoot();
        } catch (XPathException err) {
            Throwable cause = err.getException();
            if (cause != null && cause instanceof SAXParseException) {
                // This generally means the error was already reported.
                // But if a RuntimeException occurs in Saxon during a callback from
                // the Crimson parser, Crimson wraps this in a SAXParseException without
                // reporting it further.
                SAXParseException spe = (SAXParseException)cause;
                cause = spe.getException();
                if (cause instanceof RuntimeException) {
                    try {
                        config.getErrorListener().fatalError(err);
                    } catch (TransformerException e) {
                        //
                    }
                }
            } else {
                while (err.getException() instanceof XPathException) {
                    err = (XPathException)err.getException();
                }
                try {
                    config.getErrorListener().fatalError(err);
                } catch (TransformerException e) {
                    //
                }
            }
            throw err;
        }
    }

    /**
     * Prepare an XQuery query for subsequent evaluation. The source text of the query
     * is supplied as a String. The base URI of the query is taken from the static context,
     * and defaults to the current working directory.
     * @param query The XQuery query to be evaluated, supplied as a string.
     * @return an XQueryExpression object representing the prepared expression
     * @throws net.sf.saxon.trans.XPathException if the syntax of the expression is wrong,
     * or if it references namespaces, variables, or functions that have not been declared,
     * or contains other static errors.
     * @since 8.4
    */

    public XQueryExpression compileQuery(String query) throws XPathException {
        QueryParser qp = new QueryParser();
        XQueryExpression queryExp = qp.makeXQueryExpression(query, copy(), config);
        return queryExp;
    }

    /**
    * Prepare an XQuery query for subsequent evaluation. The Query is supplied
     * in the form of a Reader. The base URI of the query is taken from the static context,
     * and defaults to the current working directory.
     * @param source A Reader giving access to the text of the XQuery query to be compiled.
     * @return an XPathExpression object representing the prepared expression.
     * @throws net.sf.saxon.trans.XPathException if the syntax of the expression is wrong, or if it references namespaces,
     * variables, or functions that have not been declared, or any other static error is reported.
     * @throws java.io.IOException if a failure occurs reading the supplied input.
     * @since 8.4
    */

    public XQueryExpression compileQuery(Reader source)
    throws XPathException, IOException {
        char[] buffer = new char[4096];
        StringBuffer sb = new StringBuffer(4096);
        while (true) {
            int n = source.read(buffer);
            if (n>0) {
                sb.append(buffer, 0, n);
            } else {
                break;
            }
        }
        return compileQuery(sb.toString());
    }

    /**
     * Get the Executable, an object representing the compiled query and its environment.
     * <p>
     * This method is intended for internal use only.
     * @return the Executable
     */

    public Executable getExecutable() {
        return executable;
    }

    /**
     * Set the executable.
     * <p>
     * This method is intended for internal use only.
     * @param executable the Executable
     *
     */

    public void setExecutable(Executable executable) {
        this.executable = executable;
    }

   /**
     * Get the LocationMap, an data structure used to identify the location of compiled expressions within
    * the query source text.
     * <p>
     * This method is intended for internal use only.
     * @return the LocationMap
     */

    public LocationMap getLocationMap() {
        return executable.getLocationMap();
    }

    /**
    * Declare a namespace whose prefix can be used in expressions. This is
    * a passive namespace, it won't be copied into the result tree. Passive
    * namespaces are never undeclared, and active namespaces override them.
    * @param prefix The namespace prefix. Must not be null.
     * @param uri The namespace URI. Must not be null.
     * @param explicit This parameter is set to true when Saxon calls the method internally to
     * define a namespace declared within the query prolog. It should normally be set to false
     * in the case of a call from a user application.
     * @since 8.4
     */

	public void declarePassiveNamespace(String prefix, String uri, boolean explicit) throws StaticError {
	    if (prefix==null) {
	        throw new NullPointerException("Null prefix supplied to declarePassiveNamespace()");
	    }
	    if (uri==null) {
	        throw new NullPointerException("Null namespace URI supplied to declarePassiveNamespace()");
	    }
        if (explicit) {
            if (explicitPrologNamespaces.contains(prefix)) {
                StaticError err = new StaticError("Duplicate declaration of namespace prefix \"" + prefix + '"');
                err.setErrorCode("XQST0033");
                throw err;
            }
            explicitPrologNamespaces.add(prefix);
        }
		passiveNamespaces.put(prefix, uri);
		namePool.allocateNamespaceCode(prefix, uri);
	}

    /**
     * Declare an active namespace, that is, a namespace which as well as affecting the static
     * context of the query, will also be copied to the result tree when element constructors
     * are evaluated. When searching for a prefix-URI binding, active namespaces are searched
     * first, then passive namespaces. Active namespaces may be undeclared (in reverse sequence)
     * using {@link #undeclareNamespace()}.
     * <p>
     * This method is intended for internal use only.
     */

    public void declareActiveNamespace(String prefix, String uri) {
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
	 * Undeclare the most recently-declared active namespace. This method is called
     * when a namespace declaration goes out of scope (while processing an element end tag).
     * It is NOT called when an XML 1.1-style namespace undeclaration is encountered.
     * <p>
     * This method is intended for internal use only.
     * @see #declareActiveNamespace(String, String)
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
            String defaultNS = (String)passiveNamespaces.get("");
            if ("".equals(defaultNS)) {
                defaultElementNamespace = NamespaceConstant.NULL_CODE;
            } else {
                defaultElementNamespace = getNamePool().getCodeForURI(defaultNS);
            }
        }
	}

	/**
	* Clear all the declared passive namespaces, except for the standard ones (xml, saxon, etc)
     * @since 8.4
	*/

	public void clearPassiveNamespaces() {
        try {
            if (passiveNamespaces != null) {
                passiveNamespaces.clear();
                declarePassiveNamespace("xml", NamespaceConstant.XML, false);
                declarePassiveNamespace("saxon", NamespaceConstant.SAXON, false);
                declarePassiveNamespace("xs", NamespaceConstant.SCHEMA, false);
                declarePassiveNamespace("fn", NamespaceConstant.FN, false);
                declarePassiveNamespace("xdt", NamespaceConstant.XDT, false);
                declarePassiveNamespace("xsi", NamespaceConstant.SCHEMA_INSTANCE, false);
                declarePassiveNamespace("local", NamespaceConstant.LOCAL, false);
                declarePassiveNamespace("", "", false);
            }
        } catch (StaticError staticError) {
            // can't happen when third argument is "false"
            throw new IllegalStateException("Internal Failure initializing namespace declarations");
        }
    }

    /**
     * Get the URI for a prefix.
     * This method is used by the XQuery parser to resolve namespace prefixes.
     * <p>
     * This method is intended primarily for internal use.
     * @param prefix The prefix
     * @return the corresponding namespace URI
     * @throws net.sf.saxon.trans.XPathException if the prefix has not been declared
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
     * <p>
     * This method is intended primarily for internal use.
     * @param prefix The prefix. Supply "" to obtain the default namespace.
     * @return the corresponding namespace URI, or null if the prefix has not
     * been declared. If the prefix is "" and the default namespace is the non-namespace,
     * return "".
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
     * <p>
     * This method is for internal use only.
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
     * <p>
     * This method is for internal use only.
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

        return new SavedNamespaceContext(nscodes, namePool);
    }

    /**
     * Get the default function namespace
     * @return the default function namespace (defaults to the fn: namespace)
     * @since 8.4
     */

    public String getDefaultFunctionNamespace() {
        return defaultFunctionNamespace;
    }

    /**
     * Set the default function namespace
     * @param defaultFunctionNamespace The namespace to be used for unprefixed function calls
     * @since 8.4
     */

    public void setDefaultFunctionNamespace(String defaultFunctionNamespace) {
        this.defaultFunctionNamespace = defaultFunctionNamespace;
    }

    /**
     * Set the default element namespace
     * @since 8.4
     */

    public void setDefaultElementNamespace(String uri) throws StaticError {
        int nscode = namePool.allocateNamespaceCode("", uri);
        defaultElementNamespace = (short)(nscode & 0xffff);
        declarePassiveNamespace("", uri, true);
    }

    /**
     * Get the default XPath namespace, as a namespace URI code that can be looked up in the NamePool
     * @since 8.4
    */

    public short getDefaultElementNamespace() {
        return defaultElementNamespace;
    }

    /**
     * Set the namespace for a library module.
     * <p>
     * This method is for internal use only.
     */

    public void setModuleNamespace(String uri) {
        moduleNamespace = uri;
        moduleNamespaceURICode = namePool.getCodeForURI(uri);
    }

    /**
     * Get the namespace of the current library module.
     * <p>
     * This method is intended primarily for internal use.
     * @return the module namespace, or null if this is a main module
     */

    public String getModuleNamespace() {
        return moduleNamespace;
    }

    /**
     * Get the namesapce code of the current library module.
     * <p>
     * This method is intended primarily for internal use.
     * @return the module namespace, or null if this is a main module
     */

    public short getModuleNamespaceCode() {
        return moduleNamespaceURICode;
    }

    /**
     * Set the namespace inheritance mode
     * @param inherit true if namespaces are inherited, false if not
     * @since 8.4
     */

    public void setInheritNamespaces(boolean inherit) {
        inheritNamespaces = inherit;
    }

    /**
     * Get the namespace inheritance mode
     * @return true if namespaces are inherited, false if not
     * @since 8.4
     */

    public boolean isInheritNamespaces() {
        return inheritNamespaces;
    }

    /**
     * Set the namespace copy mode
     * @param inherit true if namespaces are preserved, false if not
     * @since 8.4
     */

    public void setPreserveNamespaces(boolean inherit) {
        preserveNamespaces = inherit;
    }

    /**
     * Get the namespace copy mode
     * @return true if namespaces are preserved, false if not
     * @since 8.4
     */

    public boolean isPreserveNamespaces() {
        return preserveNamespaces;
    }
    /**
    * Declare a named collation. Collations are only available in a query if this method
     * has been called externally to declare the collation and associate it with an
     * implementation, in the form of a Java Comparator. The default collation is the
     * Unicode codepoint collation, unless otherwise specified.
    * @param name The name of the collation (technically, a URI)
    * @param comparator The Java Comparator used to implement the collating sequence
     * @since 8.4
    */

    public void declareCollation(String name, Comparator comparator) {
        collations.put(name, comparator);
    }

    /**
     * Set the default collation. The collation that is specified must be one that has
     * been previously registered using the declareCollation() method.
     * @param name The collation name
     * @throws XPathException if the collation name has not been registered
     * @since 8.4
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
     * @since 8.4
    */

    public Comparator getCollation(String name) {
        Comparator c = (Comparator)collations.get(name);
        if (c != null) {
            return c;
        }
        try {
            return CollationFactory.makeCollationFromURI(name, config);
        } catch (XPathException e) {
            return null;
        }
    }

    /**
    * Get the name of the default collation.
    * @return the name of the default collation; or the name of the codepoint collation
    * if no default collation has been defined
     * @since 8.4
    */

    public String getDefaultCollationName() {
        if (defaultCollationName != null) {
            return defaultCollationName;
        } else {
            return NamespaceConstant.CodepointCollationURI;
        }
    }

    /**
     * Get a HashMap that maps all registered collations to Comparators.
     * Note that this returns a snapshot copy of the data held by the static context.
     * This method is provided for internal use by the query processor.
     * <p>
     * This method is intended for internal use.
     */

    public HashMap getAllCollations() {
        return new HashMap(collations);
    }

    /**
     * Get the stack frame map for global variables.
     * <p>
     * This method is intended for internal use.
     */

    public SlotManager getGlobalStackFrameMap() {
        return executable.getGlobalVariableMap();
    }

    /**
    * Declare a global variable. A variable must be declared before an expression referring
    * to it is compiled. Global variables are normally declared in the Query Prolog, but
    * they can also be predeclared using this API.
     * @since 8.4
    */

    public void declareVariable(VariableDeclaration var) throws StaticError {
        int key = var.getNameCode() & NamePool.FP_MASK;
        Integer keyObj = new Integer(key);
        if (variables.get(keyObj) != null) {
            StaticError err = new StaticError(
                    "Duplicate definition of global variable " + var.getVariableName());
            err.setErrorCode("XQST0049");
            throw err;
        }
        variables.put(keyObj, var);
        variableList.add(var);
    }

    /**
     * Fixup all references to global variables.
     * <p>
     * This method is for internal use by the Query Parser only.
     */

    public void fixupGlobalVariables(SlotManager globalVariableMap) throws StaticError {
        Iterator iter = variableList.iterator();
        while (iter.hasNext()) {
            GlobalVariableDefinition var = (GlobalVariableDefinition)iter.next();
            try {
                int slot = globalVariableMap.allocateSlotNumber(var.getNameCode()&NamePool.FP_MASK);
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
     * Produce "explain" output for all global variables.
     * <p>
     * This method is intended primarily for internal use.
     */

    public void explainGlobalVariables() {
        Iterator iter = variableList.iterator();
        while (iter.hasNext()) {
            GlobalVariableDefinition var = (GlobalVariableDefinition)iter.next();
            var.explain(getNamePool());
        }
    }

    /**
     * Get an iterator over the variables defined in this module.
     * <p>
     * This methos is intended primarily for internal use.
     * @return an Iterator, whose items are VariableDeclaration objects.  It returns
     * all variables known to this module including those imported from elsewhere; they
     * can be distinguished by their namespace. The variables are returned in order of
     * declaration.
     */

    public Iterator getVariableDeclarations() {
        return variableList.iterator();
    }

    /**
     * Get the stack frame map for local variables in the "main" query expression.
     * <p>
     * This method is intended for internal use only.
     */

    public SlotManager getStackFrameMap() {
        if (stackFrameMap == null) {
            stackFrameMap = getConfiguration().makeSlotManager();
        }
        return stackFrameMap;
    }

    /**
    * Get the NamePool used for compiling expressions
     * @since 8.4
    */

    public NamePool getNamePool() {
        return namePool;
    }

    /**
    * Issue a compile-time warning. This method is used during XQuery expression compilation to
    * output warning conditions.
     * <p>
     * This method is intended for internal use only.
    */

    public void issueWarning(String s, SourceLocator locator) {
        StaticError err = new StaticError(s);
        err.setLocator(locator);
        try {
            config.getErrorListener().warning(err);
        } catch (TransformerException e) {
            // ignore any error thrown
        }
    }

    /**
     * Set the Base URI of the query
     * @since 8.4
     */

    public void setBaseURI(String baseURI) {
        this.baseURI = baseURI;
    }

    /**
    * Get the system ID of the container of the expression. Used to construct error messages.
     * Note that the systemID and the Base URI are currently identical, but they might be distinguished
     * in the future.
    * @return the Base URI
     * @since 8.4
    */

    public String getSystemId() {
        return baseURI;
    }

    /**
    * Get the Base URI of the query, for resolving any relative URI's used
    * in the expression.
     * Note that the systemID and the Base URI are currently identical, but they might be distinguished
     * in the future.
    * Used by the document() function.
    * @return the base URI of the query
     * @since 8.4
    */

    public String getBaseURI() {
        return baseURI;
    }

    /**
    * Get the line number of the expression within that container.
    * Used to construct error messages. This method is provided to satisfy the StaticContext interface,
     * but the value is meaningful only for XPath expressions within a document such as a stylesheet.
    * @return -1 always
    */

    public int getLineNumber() {
        return -1;
    }


    /**
    * Bind a variable used in a query to the expression in which it is declared.
     * <p>
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
     * Set the function library used for binding any function call appearing within the query module.
     * <p>
     * This method is available for use by advanced applications. The details of the FunctionLibrary
     * interface are subject to change. Applications using this interface take responsibility for
     * ensuring that the results conform to the constraints imposed by the XQuery language specification,
     * for example that one function within a query module can call other functions defined in the same
     * query module.
     * @param functionLibrary   the FunctionLibrary to be used. This will typically be a
     * FunctionLibraryList; in most cases it will be a slightly modified copy of a FunctionLibraryList
     * constructed by the system and obtained using the {@link #getFunctionLibrary} method.
     * @see FunctionLibraryList
     */

    public void setFunctionLibraryList(FunctionLibraryList functionLibrary) {
        this.functionLibraryList = functionLibrary;
    }
    /**
     * Get the function library containing all the in-scope functions available in this static
     * context (that is, the functions available in this query module).
     * <p>
     * This method is provided for use by advanced applications.
     * The details of the interface are subject to change.
     *
     * @return the FunctionLibrary used. For XQuery, this will always be a FunctionLibraryList.
     * @see FunctionLibraryList
     */

    public FunctionLibrary getFunctionLibrary() {
        return functionLibraryList;
    }

    /**
     * Register a user-defined XQuery function.
     * <p>
     * This method is intended for internal use only.
     */

    public void declareFunction(XQueryFunction function) throws StaticError {
        if (function.getNumberOfArguments() == 1) {
            SchemaType t = config.getSchemaType(function.getNameCode() & NamePool.FP_MASK);
            if (t != null && t instanceof AtomicType) {
                StaticError err = new StaticError("Function name " + function.getFunctionDisplayName(getNamePool()) +
                        " clashes with the name of the constructor function for an atomic type");
                err.setErrorCode("XQST0034");
                throw err;
            }
        }
        functions.declareFunction(function);
    }

    /**
     * Bind function calls that could not be bound when first encountered. These
     * will either be forwards references to functions declared later in the query,
     * or errors.
     * <p>
     * This method is for internal use only.
     * @throws net.sf.saxon.trans.StaticError if a function call refers to a function that has
     * not been declared
     */

    protected void bindUnboundFunctionCalls() throws StaticError {
        functions.bindUnboundFunctionCalls();
    }

    /**
     * Get an iterator over the Functions defined in this module.
     * <p>
     * This method is intended primarily for internal use.
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
     * This method is for internal use only.
     */

    protected void fixupGlobalFunctions() throws XPathException {
        functions.fixupGlobalFunctions(this);
    }

    /**
     * Output "explain" information about each declared function.
     * <p>
     * This method is intended primarily for internal use.
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
     * @since 8.4
     */

    public UserFunction getUserDefinedFunction(String uri, String localName, int arity) {
        return functions.getUserDefinedFunction(uri, localName, arity);
    }

    /**
     * Determine whether Backwards Compatible Mode is used
     * @return false; XPath 1.0 compatibility mode is not supported in XQuery
     * @since 8.4
     */

    public boolean isInBackwardsCompatibleMode() {
        return false;
    }

    /**
     * Add an imported schema to this static context. A query module can reference
     * types in a schema provided two conditions are satisfied: the schema containing those
     * types has been loaded into the Configuration, and the target namespace has been imported
     * by this query module. This method achieves the second of these conditions. It does not
     * cause the schema to be loaded.
     * <p>
     * @param targetNamespace The target namespace of the schema to be added
     * @since 8.4
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
     * @since 8.4
     */

    public boolean isImportedSchema(String namespace) {
        if (importedSchemata == null) {
            return false;
        }
        return importedSchemata.contains(namespace);
    }

    /**
     * Determine whether a built-in type is available in this context. This method caters for differences
     * between host languages as to which set of types are built in.
     *
     * @param type the supposedly built-in type. This will always be a type in the
     *                    XS or XDT namespace.
     * @return true if this type can be used in this static context
     */

    public boolean isAllowedBuiltInType(AtomicType type) {
        return true;
    }

    /**
     * Set the construction mode for this module
     * @param mode one of {@link Validation#STRIP}, {@link Validation#PRESERVE}
     * @since 8.4
     */

    public void setConstructionMode(int mode) {
        constructionMode = mode;
    }

    /**
     * Get the current validation mode
     * @return one of {@link Validation#STRIP}, {@link Validation#PRESERVE}
     * @since 8.4
     */

    public int getConstructionMode() {
        return constructionMode;
    }

    /**
     * Load another query module.
     * <p>
     * This method is intended for internal use only.
     */

    protected StaticQueryContext loadModule(String namespaceURI, String locationURI) throws StaticError {
        // Check that this import would not create a cycle
        StaticQueryContext parent = importer;
        while (parent != null) {
            if (namespaceURI.equals(parent.moduleNamespace)) {
                StaticError err = new StaticError("A module cannot import itself directly or indirectly");
                err.setErrorCode("XQST0073");
                throw err;
            }
            parent = parent.importer;
        }
        // load the requested module
        return loadQueryModule(config, executable, baseURI, namespaceURI, locationURI, this);
    }

    /**
     * Supporting method to load a query module. Used also by saxon:import-query in XSLT.
     * <p>
     * This method is intended for internal use only.
     * @param config The configuration
     * @param executable The Executation
     * @param baseURI The base URI of the invoking module
     * @param namespaceURI namespace of the query module to be loaded
     * @param locationURI location hint of the query module to be loaded
     * @param importer The importing query module (used to check for cycles)
     * @return The StaticQueryContext representing the loaded query module
     * @throws net.sf.saxon.trans.StaticError
     */

    public static StaticQueryContext loadQueryModule(
            Configuration config,
            Executable executable,
            String baseURI,
            String namespaceURI,
            String locationURI,
            StaticQueryContext importer)
    throws StaticError {

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
            module.importer = importer;
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
