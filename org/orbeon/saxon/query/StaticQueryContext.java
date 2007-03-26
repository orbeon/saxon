package org.orbeon.saxon.query;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.StandardErrorListener;
import org.orbeon.saxon.event.Builder;
import org.orbeon.saxon.event.Stripper;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.functions.ConstructorFunctionLibrary;
import org.orbeon.saxon.functions.FunctionLibrary;
import org.orbeon.saxon.functions.FunctionLibraryList;
import org.orbeon.saxon.functions.SystemFunctionLibrary;
import org.orbeon.saxon.instruct.*;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.pattern.CombinedNodeTest;
import org.orbeon.saxon.pattern.ContentTypeTest;
import org.orbeon.saxon.pattern.NodeTest;
import org.orbeon.saxon.sort.IntHashMap;
import org.orbeon.saxon.trans.StaticError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.AnyItemType;
import org.orbeon.saxon.type.AtomicType;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.SchemaType;
import org.orbeon.saxon.value.SequenceType;
import org.xml.sax.SAXParseException;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.Source;
import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.*;

/**
 * StaticQueryContext is the implementation of StaticContext used when processing XQuery
 * expressions.
 * <p/>
 * The StaticQueryContext object has two different usages. The application constructs a StaticQueryContext
 * and initializes it with information about the context, for example, default namespaces, base URI, and so on.
 * When a query is compiled using this StaticQueryContext, the query parser makes a copy of the StaticQueryContext
 * and uses this internally, modifying it with information obtained from the query prolog, as well as information
 * such as namespace and variable declarations that can occur at any point in the query. The query parser does
 * not modify the original StaticQueryContext supplied by the calling application, which may therefore be used
 * for compiling multiple queries, serially or even in multiple threads.
 * <p/>
 * This class forms part of Saxon's published XQuery API.
 * Note that some of the methods are intended for use internally by the
 * query processor itself: these are labelled as such. Methods that
 * are considered stable are labelled with the JavaDoc "since" tag.
 * The value 8.4 indicates a method introduced at or before Saxon 8.4; other
 * values indicate the version at which the method was introduced.
 * <p/>
 * In the longer term, this entire API may at some stage be superseded by a proposed
 * standard Java API for XQuery.
 *
 * @since 8.4
 */

public class StaticQueryContext implements StaticContext {

    private boolean isMainModule;
    private Configuration config;
    private NamePool namePool;
    private String locationURI;
    private String moduleNamespace;
    private String baseURI;
    private HashMap passiveNamespaces;
    private HashSet explicitPrologNamespaces;
    private Stack activeNamespaces;
    private boolean inheritNamespaces = true;
    private boolean preserveNamespaces = true;
    private NamespaceResolver externalNamespaceResolver = null;
    private CollationMap collations;
    private IntHashMap variables;           // global variables declared in this module
    private IntHashMap libraryVariables;    // all variables defined in library modules
                                            // defined only on the top-level module
    private IntHashMap undeclaredVariables;
    private HashSet importedSchemata;
    private String defaultFunctionNamespace;
    private short defaultElementNamespace;
    private ItemType requiredContextItemType = AnyItemType.getInstance();
    private SlotManager stackFrameMap;  // map of the outermost local stackframe
    private short moduleNamespaceURICode;
    private ModuleURIResolver moduleURIResolver;
    private int constructionMode;
    private Executable executable;
    private List importers;  // A list of StaticQueryContext objects representing the modules that import this one,
                            // Null for the main module
    private FunctionLibraryList functionLibraryList;
    private XQueryFunctionLibrary globalFunctionLibrary;      // used only on a top-level module
    private int localFunctionLibraryNr;
    private int importedFunctionLibraryNr;
    private int unboundFunctionLibraryNr;
    private Set importedModuleNamespaces;
    private ErrorListener errorListener;

    /**
     * Private constructor used when copying a context
     */

    private StaticQueryContext() {
    }

    /**
     * Create a StaticQueryContext using a given Configuration. This creates a StaticQueryContext for a main module
     * (that is, a module that is not a library module).
     *
     * @since 8.4
     */

    public StaticQueryContext(Configuration config) {
        this.config = config;
        this.namePool = config.getNamePool();
        this.errorListener = config.getErrorListener();
        this.moduleURIResolver = config.getModuleURIResolver();
        if (errorListener instanceof StandardErrorListener) {
            errorListener = ((StandardErrorListener)errorListener).makeAnother(Configuration.XQUERY);
            ((StandardErrorListener)errorListener).setRecoveryPolicy(Configuration.DO_NOT_RECOVER);
        }
        isMainModule = true;
        collations = new CollationMap(config);
        reset();
    }

    /**
     * Create a StaticQueryContext for a library module.
     * @param importer the module that imported this module. This may be null, in which case the
     * library module is treated as a "top-level" library module.
     */

    public StaticQueryContext(Configuration config, StaticQueryContext importer) {
        this.config = config;
        this.namePool = config.getNamePool();
        isMainModule = false;
        if (importers == null && importer != null) {
            importers = new ArrayList(2);
            importers.add(importer);
        }
        reset();
    }

    /**
     * Reset the state of this StaticQueryContext to an uninitialized state
     *
     * @since 8.4
     */

    public void reset() {
        passiveNamespaces = new HashMap(10);
        explicitPrologNamespaces = new HashSet(10);
        activeNamespaces = new Stack();
        externalNamespaceResolver = null;
        collations = new CollationMap(config);
        variables = new IntHashMap(10);
        undeclaredVariables = new IntHashMap(5);
        errorListener = config.getErrorListener();
        if (errorListener instanceof StandardErrorListener) {
            errorListener = ((StandardErrorListener)errorListener).makeAnother(Configuration.XQUERY);
            ((StandardErrorListener)errorListener).setRecoveryPolicy(Configuration.DO_NOT_RECOVER);
        }
        if (isTopLevelModule()) {
            globalFunctionLibrary = new XQueryFunctionLibrary(config);
            libraryVariables = new IntHashMap(10);
        }
        requiredContextItemType = AnyItemType.getInstance();
        importedSchemata = new HashSet(5);
        importedModuleNamespaces = new HashSet(5);
        defaultFunctionNamespace = NamespaceConstant.FN;
        defaultElementNamespace = NamespaceConstant.NULL_CODE;
        moduleNamespace = null;
        moduleNamespaceURICode = 0;
        moduleURIResolver = config.getModuleURIResolver();
        constructionMode = Validation.PRESERVE;
        collations.setDefaultCollationName(NamespaceConstant.CODEPOINT_COLLATION_URI);
        functionLibraryList = new FunctionLibraryList();
        functionLibraryList.addFunctionLibrary(new SystemFunctionLibrary(SystemFunctionLibrary.XPATH_ONLY));
        functionLibraryList.addFunctionLibrary(config.getVendorFunctionLibrary());
        functionLibraryList.addFunctionLibrary(new ConstructorFunctionLibrary(config));
        if (config.isAllowExternalFunctions()) {
            functionLibraryList.addFunctionLibrary(config.getExtensionBinder());
        }
        localFunctionLibraryNr = functionLibraryList.addFunctionLibrary(
                new XQueryFunctionLibrary(config));

        importedFunctionLibraryNr = functionLibraryList.addFunctionLibrary(
                new ImportedFunctionLibrary(this, getTopLevelModule(this).getGlobalFunctionLibrary()));

        unboundFunctionLibraryNr = functionLibraryList.addFunctionLibrary(
                new UnboundFunctionLibrary());

        clearPassiveNamespaces();
    }

    /**
     * Test whether this is a "top-level" module. This is true for a main module and also for a
     * module directly imported into an XSLT stylesheet. It may also be true in future for independently-compiled
     * modules
     */

    public boolean isTopLevelModule() {
        return importers == null;
    }

    /**
     * Test whether this is a "main" module, in the sense of the XQuery language specification
     */

    public boolean isMainModule() {
        return isMainModule;
    }

    /**
     * Check whether this module is allowed to import a module with namespace N. Note that before
     * calling this we have already handled the exception case where a module imports another in the same
     * namespace (the only case where cycles are allowed)
     */

    public boolean mayImport(String namespace) {
        if (namespace.equals(moduleNamespace)) {
            return false;
        }
        if (importers == null) {
            return true;
        }
        for (int i=0; i<importers.size(); i++) {
            if (!((StaticQueryContext)importers.get(i)).mayImport(namespace)) {
                return false;
            }
        }
        return true;
    }

    public XQueryFunctionLibrary getGlobalFunctionLibrary() {
        return globalFunctionLibrary;
    }

    public ImportedFunctionLibrary getImportedFunctionLibrary() {
        return (ImportedFunctionLibrary)functionLibraryList.get(importedFunctionLibraryNr);
    }

    /**
     * Register that this module imports a particular module namespace
     */

    public void addImportedNamespace(String uri) {
        if (importedModuleNamespaces == null) {
            importedModuleNamespaces = new HashSet(5);
        }
        importedModuleNamespaces.add(uri);
        getImportedFunctionLibrary().addImportedNamespace(uri);
    }

    /**
     * Test whether this module directly imports a particular namespace
     */

    public boolean importsNamespace(String uri) {
        return importedModuleNamespaces != null &&
                importedModuleNamespaces.contains(uri);
    }

    /**
     * Test whether this module imports a particular namespace directly or indirectly
     */

    public boolean importsNamespaceIndirectly(String uri) {
        if (importsNamespace(uri)) {
            return true;
        }
        for (Iterator it = iterateImportedNamespaces(); it.hasNext();) {
            String moduleURI = (String)it.next();
            List list = executable.getQueryLibraryModules(moduleURI);
            for (Iterator i2 = list.iterator(); i2.hasNext();) {
                StaticQueryContext sqc = (StaticQueryContext)i2.next();
                if (sqc.importsNamespaceIndirectly(uri)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Get an iterator over all the module namespaces that this module imports
     */

    public Iterator iterateImportedNamespaces() {
        if (importedModuleNamespaces == null) {
            return Collections.EMPTY_LIST.iterator();
        }
        return importedModuleNamespaces.iterator();
    }

    /**
     * Get the Static Query Context for the top-level module. This will normally be a main module,
     * but in the case of saxon:import-query it will be the library module that is imported into
     * the stylesheet
     */

    public StaticQueryContext getTopLevelModule(StaticQueryContext start) {
        if (importers == null) {
            return this;
        }
        // There may be import cycles of modules using the same namespace. So first we
        // try to find an importer in a different namespace; if this fails we try the first
        // importer. Somewhere in the cycle there must be a module that was imported from a different
        // namespace (we hope!)
        for (int i=0; i<importers.size(); i++) {
            StaticQueryContext importer = (StaticQueryContext)importers.get(i);
            if (!start.importsNamespace(importer.getModuleNamespace())) {
                return importer.getTopLevelModule(start);
            }
        }
        return ((StaticQueryContext)importers.get(0)).getTopLevelModule(start);
    }

    /**
     * Make a copy of this StaticQueryContext. The StaticQueryContext that is constructed by a user
     * application and passed to Saxon when a query is compiled should not be modified by the query
     * compiler. Saxon therefore makes a copy of the StaticQueryContext and uses this copy internally,
     * to capture any changes to the StaticQueryContext defined in the query prolog.
     *
     * @return a copy of this StaticQueryContext
     */

    public StaticQueryContext copy() {
        StaticQueryContext n = new StaticQueryContext();
        n.config = config;
        n.namePool = namePool;
        n.isMainModule = isMainModule;
        n.passiveNamespaces = new HashMap(passiveNamespaces);
        n.explicitPrologNamespaces = new HashSet(explicitPrologNamespaces);
        n.activeNamespaces = new Stack();
        n.externalNamespaceResolver = externalNamespaceResolver;
        n.inheritNamespaces = inheritNamespaces;
        n.preserveNamespaces = preserveNamespaces;
        n.collations = new CollationMap(collations);
        n.variables = variables.copy();
        n.undeclaredVariables = undeclaredVariables.copy();
        if (libraryVariables != null) {
            n.libraryVariables = libraryVariables.copy();
        }
        //n.variableList = new ArrayList(variableList);
        n.importedSchemata = new HashSet(importedSchemata);
        n.defaultFunctionNamespace = defaultFunctionNamespace;
        n.defaultElementNamespace = defaultElementNamespace;
        n.locationURI = locationURI;
        n.baseURI = baseURI;
        n.requiredContextItemType = requiredContextItemType;
        n.stackFrameMap = stackFrameMap;
        n.moduleNamespace = moduleNamespace;
        n.moduleNamespaceURICode = moduleNamespaceURICode;
        n.moduleURIResolver = moduleURIResolver;
        n.constructionMode = constructionMode;
        n.executable = executable;
        n.importers = importers;
        n.functionLibraryList = (FunctionLibraryList)functionLibraryList.copy();
        n.localFunctionLibraryNr = localFunctionLibraryNr;
        n.importedFunctionLibraryNr = importedFunctionLibraryNr;
        n.unboundFunctionLibraryNr = unboundFunctionLibraryNr;
        n.globalFunctionLibrary = globalFunctionLibrary;
        n.importedModuleNamespaces = new HashSet(importedModuleNamespaces);
        n.getImportedFunctionLibrary().setImportingModule(n);
        return n;
    }

    /**
     * Set the Configuration options
     *
     * @throws IllegalArgumentException if the configuration supplied is different from the existing
     *                                  configuration
     * @since 8.4
     * @deprecated This method serves no purpose, since it is not possible to change the configuration
     *             once the StaticQueryContext has been initialized.
     */

    public void setConfiguration(Configuration config) {
        if (this.config != config) {
            throw new IllegalArgumentException("Configuration cannot be changed dynamically");
        }
        this.config = config;
    }

    /**
     * Get the Configuration options
     *
     * @since 8.4
     */

    public Configuration getConfiguration() {
        return config;
    }

    /**
     * Construct a dynamic context for early evaluation of constant subexpressions
     */

    public XPathContext makeEarlyEvaluationContext() {
        return new EarlyEvaluationContext(config, collations);
    }

    /**
     * Convenience method for building Saxon's internal representation of a source XML
     * document. The document will be built using the default NamePool, which means that
     * any process that uses it must also use the default NamePool.
     *
     * @param source Any javax.xml.transform.Source object representing the document against
     *               which queries will be executed. Note that a Saxon {@link org.orbeon.saxon.om.DocumentInfo DocumentInfo}
     *               (indeed any {@link org.orbeon.saxon.om.NodeInfo NodeInfo})
     *               can be used as a Source. To use a third-party DOM Document as a source, create an instance of
     *               {@link javax.xml.transform.dom.DOMSource DOMSource} to wrap it.
     *               <p>For additional control over the way in which the source document is processed,
     *               supply an {@link org.orbeon.saxon.AugmentedSource AugmentedSource} object and set appropriate
     *               options on the object.</p>
     * @return the DocumentInfo representing the root node of the resulting document object.
     * @since 8.4
     */

    public DocumentInfo buildDocument(Source source) throws XPathException {
        Source s2 = config.getSourceResolver().resolveSource(source, config);
        if (s2 != null) {
            source = s2;
        }
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
                    reportFatalError(err);
                }
            } else {
                while (err.getException() instanceof XPathException) {
                    err = (XPathException)err.getException();
                }
                reportFatalError(err);
            }
            throw err;
        }
    }

    /**
     * Prepare an XQuery query for subsequent evaluation. The source text of the query
     * is supplied as a String. The base URI of the query is taken from the static context,
     * and defaults to the current working directory.
     *
     * @param query The XQuery query to be evaluated, supplied as a string.
     * @return an XQueryExpression object representing the prepared expression
     * @throws org.orbeon.saxon.trans.XPathException
     *          if the syntax of the expression is wrong,
     *          or if it references namespaces, variables, or functions that have not been declared,
     *          or contains other static errors.
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
     *
     * @param source A Reader giving access to the text of the XQuery query to be compiled.
     * @return an XPathExpression object representing the prepared expression.
     * @throws org.orbeon.saxon.trans.XPathException
     *                             if the syntax of the expression is wrong, or if it references namespaces,
     *                             variables, or functions that have not been declared, or any other static error is reported.
     * @throws java.io.IOException if a failure occurs reading the supplied input.
     * @since 8.4
     */

    public XQueryExpression compileQuery(Reader source)
            throws XPathException, IOException {
        char[] buffer = new char[4096];
        StringBuffer sb = new StringBuffer(4096);
        while (true) {
            int n = source.read(buffer);
            if (n > 0) {
                sb.append(buffer, 0, n);
            } else {
                break;
            }
        }
        return compileQuery(sb.toString());
    }

    /**
     * Prepare an XQuery query for subsequent evaluation. The Query is supplied
     * in the form of a InputStream, with an optional encoding. If the encoding is not specified,
     * the query parser attempts to obtain the encoding by inspecting the input stream: it looks specifically
     * for a byte order mark, and for the encoding option in the version declaration of an XQuery prolog.
     * The encoding defaults to UTF-8.
     * The base URI of the query is taken from the static context,
     * and defaults to the current working directory.
     *
     * @param source   An InputStream giving access to the text of the XQuery query to be compiled, as a stream
     *                 of octets
     * @param encoding The encoding used to translate characters to octets in the query source. The parameter
     *                 may be null: in this case the query parser attempts to infer the encoding by inspecting the source,
     *                 and if that fails, it assumes UTF-8 encoding
     * @return an XPathExpression object representing the prepared expression.
     * @throws org.orbeon.saxon.trans.XPathException
     *                             if the syntax of the expression is wrong, or if it references namespaces,
     *                             variables, or functions that have not been declared, or any other static error is reported.
     * @throws java.io.IOException if a failure occurs reading the supplied input.
     * @since 8.5
     */

    public XQueryExpression compileQuery(InputStream source, String encoding)
            throws XPathException, IOException {
        String query = QueryReader.readInputStream(source, encoding, config.getNameChecker());
        return compileQuery(query);
    }

    /**
     * Get the Executable, an object representing the compiled query and its environment.
     * <p/>
     * This method is intended for internal use only.
     *
     * @return the Executable
     */

    public Executable getExecutable() {
        return executable;
    }

    /**
     * Set the executable.
     * <p/>
     * This method is intended for internal use only.
     *
     * @param executable the Executable
     */

    public void setExecutable(Executable executable) {
        this.executable = executable;
    }

    /**
     * Get the LocationMap, an data structure used to identify the location of compiled expressions within
     * the query source text.
     * <p/>
     * This method is intended for internal use only.
     *
     * @return the LocationMap
     */

    public LocationMap getLocationMap() {
        return executable.getLocationMap();
    }

    /**
     * Declare a namespace whose prefix can be used in expressions. This is
     * a passive namespace, it won't be copied into the result tree. Passive
     * namespaces are never undeclared, and active namespaces override them.
     *
     * @param prefix   The namespace prefix. Must not be null.
     * @param uri      The namespace URI. Must not be null. The value "" (zero-length string) is used
     *                 to undeclare a namespace; it is not an error if there is no existing binding for
     *                 the namespace prefix.
     * @param explicit This parameter is set to true when Saxon calls the method internally to
     *                 define a namespace declared within the query prolog. It should normally be set to false
     *                 in the case of a call from a user application.
     * @since 8.4
     */

    public void declarePassiveNamespace(String prefix, String uri, boolean explicit) throws StaticError {
        if (prefix == null) {
            throw new NullPointerException("Null prefix supplied to declarePassiveNamespace()");
        }
        if (uri == null) {
            throw new NullPointerException("Null namespace URI supplied to declarePassiveNamespace()");
        }
        if ((prefix.equals("xml") != uri.equals(NamespaceConstant.XML))) {
            StaticError err = new StaticError("Invalid declaration of the XML namespace");
            err.setErrorCode("XQST0070");
            throw err;
        }
        if (explicit) {
            if (explicitPrologNamespaces.contains(prefix)) {
                StaticError err = new StaticError("Duplicate declaration of namespace prefix \"" + prefix + '"');
                err.setErrorCode("XQST0033");
                throw err;
            //} else if (uri.equals("") && !prefix.equals("")) {
            //    explicitPrologNamespaces.remove(prefix);
            } else {
                explicitPrologNamespaces.add(prefix);
            }
        }
        if (uri.equals("") && !prefix.equals("")) {
            passiveNamespaces.remove(prefix);
        } else {
            passiveNamespaces.put(prefix, uri);
            namePool.allocateNamespaceCode(prefix, uri);
        }
    }

    /**
     * Declare an active namespace, that is, a namespace which as well as affecting the static
     * context of the query, will also be copied to the result tree when element constructors
     * are evaluated. When searching for a prefix-URI binding, active namespaces are searched
     * first, then passive namespaces. Active namespaces may be undeclared (in reverse sequence)
     * using {@link #undeclareNamespace()}.
     * <p/>
     * This method is intended for internal use only.
     */

    public void declareActiveNamespace(String prefix, String uri) {
        if (prefix == null) {
            throw new NullPointerException("Null prefix supplied to declareActiveNamespace()");
        }
        if (uri == null) {
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
     * <p/>
     * This method is intended for internal use only.
     *
     * @see #declareActiveNamespace(String, String)
     */

    public void undeclareNamespace() {
        ActiveNamespace entry = (ActiveNamespace)activeNamespaces.pop();
        if (entry.prefix.equals("")) {
            for (int i = activeNamespaces.size() - 1; i >= 0; i--) {
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
     *
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
     * Set an external namespace resolver. If a namespace prefix cannot be resolved using any
     * other mechanism, then as a last resort the external namespace resolver is called to
     * obtain a URI for the given prefix.
     * <p>
     * Note that the external namespace resolver is used only for namespace prefixes that are
     * resolved at compile time. Where namespace prefixes are resolved at run-time (which happens
     * very rarely with XQuery: the only case is to resolve the computed name used in a computed
     * element or attribute constructor), the external namespace resolver is not invoked.
     * <p>
     * Although the supplied object must implement the NamespaceResolver interface, the only method
     * actually used is the method {@link NamespaceResolver#getURIForPrefix(String, boolean)}. Other
     * methods may throw an UnsupportedOperationException.
     */

    public void setExternalNamespaceResolver(NamespaceResolver resolver) {
        externalNamespaceResolver = resolver;
    }

    /**
     * Get the external namespace resolver that has been registered using
     * setExternalNamespaceResolver(), if any.
     */

    public NamespaceResolver getExternalNamespaceResolver() {
        return externalNamespaceResolver;
    }

    /**
     * Get the URI for a prefix.
     * This method is used by the XQuery parser to resolve namespace prefixes.
     * <p/>
     * This method is intended primarily for internal use.
     *
     * @param prefix The prefix
     * @return the corresponding namespace URI
     * @throws org.orbeon.saxon.trans.XPathException
     *          if the prefix has not been declared
     */

    public String getURIForPrefix(String prefix) throws XPathException {
        String uri = checkURIForPrefix(prefix);
        if (uri == null) {
            StaticError err = new StaticError("Prefix " + prefix + " has not been declared");
            err.setErrorCode("XPST0081");
            throw err;
        }
        return uri;
    }

    /**
     * Get the URI for a prefix if there is one, return null if not.
     * This method is used by the XQuery parser to resolve namespace prefixes.
     * <p/>
     * This method is intended primarily for internal use.
     *
     * @param prefix The prefix. Supply "" to obtain the default namespace.
     * @return the corresponding namespace URI, or null if the prefix has not
     *         been declared. If the prefix is "" and the default namespace is the non-namespace,
     *         return "".
     */

    public String checkURIForPrefix(String prefix) {
        // Search the active namespaces first, then the passive ones.
        for (int i = activeNamespaces.size() - 1; i >= 0; i--) {
            if (((ActiveNamespace)activeNamespaces.get(i)).prefix.equals(prefix)) {
                return ((ActiveNamespace)activeNamespaces.get(i)).uri;
            }
        }
        String uri = (String)passiveNamespaces.get(prefix);
        if (uri != null) {
            return uri;
        }
        if (externalNamespaceResolver != null) {
            return externalNamespaceResolver.getURIForPrefix(prefix, true);
        }
        return null;
    }

    /**
     * Get an array containing the namespace codes of all active
     * namespaces.
     * <p/>
     * This method is for internal use only.
     */

    public int[] getActiveNamespaceCodes() {
        int[] nscodes = new int[activeNamespaces.size()];
        int used = 0;
        HashSet prefixes = new HashSet(10);
        for (int n = activeNamespaces.size() - 1; n >= 0; n--) {
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
     * <p/>
     * This method is for internal use only.
     */

    public NamespaceResolver getNamespaceResolver() {
        int[] active = getActiveNamespaceCodes();
        int[] nscodes = new int[passiveNamespaces.size() + active.length];

        int used = 0;
        for (Iterator iter = passiveNamespaces.keySet().iterator(); iter.hasNext();) {
            String prefix = (String)iter.next();
            String uri = (String)passiveNamespaces.get(prefix);
            nscodes[used++] = namePool.getNamespaceCode(prefix, uri);
        }
        for (int a = 0; a < active.length; a++) {
            nscodes[used++] = active[a];
        }

        return new SavedNamespaceContext(nscodes, namePool);
    }

    /**
     * Get the default function namespace
     *
     * @return the default function namespace (defaults to the fn: namespace)
     * @since 8.4
     */

    public String getDefaultFunctionNamespace() {
        return defaultFunctionNamespace;
    }

    /**
     * Set the default function namespace
     *
     * @param defaultFunctionNamespace The namespace to be used for unprefixed function calls
     * @since 8.4
     */

    public void setDefaultFunctionNamespace(String defaultFunctionNamespace) {
        this.defaultFunctionNamespace = defaultFunctionNamespace;
    }

    /**
     * Set the default element namespace
     *
     * @since 8.4
     */

    public void setDefaultElementNamespace(String uri) throws StaticError {
        int nscode = namePool.allocateNamespaceCode("", uri);
        defaultElementNamespace = (short)(nscode & 0xffff);
        declarePassiveNamespace("", uri, true);
    }

    /**
     * Get the default XPath namespace, as a namespace URI code that can be looked up in the NamePool
     *
     * @since 8.4
     */

    public short getDefaultElementNamespace() {
        return defaultElementNamespace;
    }

    /**
     * Set the namespace for a library module.
     * <p/>
     * This method is for internal use only.
     */

    public void setModuleNamespace(String uri) {
        moduleNamespace = uri;
        moduleNamespaceURICode = namePool.getCodeForURI(uri);
    }

    /**
     * Get the namespace of the current library module.
     * <p/>
     * This method is intended primarily for internal use.
     *
     * @return the module namespace, or null if this is a main module
     */

    public String getModuleNamespace() {
        return moduleNamespace;
    }

    /**
     * Get the namesapce code of the current library module.
     * <p/>
     * This method is intended primarily for internal use.
     *
     * @return the module namespace, or null if this is a main module
     */

    public short getModuleNamespaceCode() {
        return moduleNamespaceURICode;
    }

    /**
     * Set the location URI for a module
     */

    public void setLocationURI(String uri) {
        locationURI = uri;
    }

    /**
     * Get the location URI for a module
     */

    public String getLocationURI() {
        return locationURI;
    }

    /**
     * Set a user-defined ModuleURIResolver for resolving URIs used in "import module"
     * declarations in the XQuery prolog.
     * This will be used for resolving URIs in XQuery "import module" declarations, overriding
     * any ModuleURIResolver that was specified as part of the configuration.
     */

    public void setModuleURIResolver(ModuleURIResolver resolver) {
        moduleURIResolver = resolver;
    }

    /**
     * Get the user-defined ModuleURIResolver for resolving URIs used in "import module"
     * declarations in the XQuery prolog; returns null if none has been explicitly set either
     * on the StaticQueryContext or on the Configuration.
     */

    public ModuleURIResolver getModuleURIResolver() {
        return moduleURIResolver;
    }


    /**
     * Set the namespace inheritance mode
     *
     * @param inherit true if namespaces are inherited, false if not
     * @since 8.4
     */

    public void setInheritNamespaces(boolean inherit) {
        inheritNamespaces = inherit;
    }

    /**
     * Get the namespace inheritance mode
     *
     * @return true if namespaces are inherited, false if not
     * @since 8.4
     */

    public boolean isInheritNamespaces() {
        return inheritNamespaces;
    }

    /**
     * Set the namespace copy mode
     *
     * @param inherit true if namespaces are preserved, false if not
     * @since 8.4
     */

    public void setPreserveNamespaces(boolean inherit) {
        preserveNamespaces = inherit;
    }

    /**
     * Get the namespace copy mode
     *
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
     *
     * @param name       The name of the collation (technically, a URI)
     * @param comparator The Java Comparator used to implement the collating sequence
     * @since 8.4
     */

    public void declareCollation(String name, Comparator comparator) {
        collations.setNamedCollation(name, comparator);
    }

    /**
     * Set the default collation.
     * @param name The collation name, as specified in the query prolog. The name
     * is not validated until it is used.
     * @since 8.4. Changed in 8.6 so it no longer validates the collation name: this is
     * because the base URI is not necessarily known at the point where the default
     * collation is declared.
     */

    public void declareDefaultCollation(String name)  {
        collations.setDefaultCollationName(name);
    }

    /**
     * Get a named collation.
     * @param name the name of the collation, as an absolute URI
     * @return the collation identified by the given name, as set previously using declareCollation.
     *         If no collation with this name has been declared, the method calls the CollationURIResolver
     *         to locate a collation with this name.
     *         Return null if no collation with this name is found.
     * @since 8.4
     */

    public Comparator getCollation(String name) {
        return collations.getNamedCollation(name);
    }

    /**
     * Get the name of the default collation.
     *
     * @return the name of the default collation; or the name of the codepoint collation
     *         if no default collation has been defined. The name is returned in the form
     *         it was specified; that is, it is not yet resolved against the base URI. (This
     *         is because the base URI declaration can follow the default collation declaration
     *         in the query prolog.) If no default collation has been specified, the "default default"
     *         (that is, the Unicode codepoint collation) is returned.
     * @since 8.4
     */

    public String getDefaultCollationName() {
        return collations.getDefaultCollationName();
    }

    /**
     * Get a HashMap that maps all registered collations to Comparators.
     * Note that this returns a snapshot copy of the data held by the static context.
     * This method is provided for internal use by the query processor.
     * <p/>
     * This method is intended for internal use.
     */

    public CollationMap getAllCollations() {
        return new CollationMap(collations);
    }

    /**
     * Declare the static type of the context item. If this type is declared, and if a context item
     * is supplied when the query is invoked, then the context item must conform to this type (no
     * type conversion will take place to force it into this type).
     * @param type the required context item type
     */

    public void setRequiredContextItemType(ItemType type) {
        requiredContextItemType = type;
    }

    /**
     * Get the required type of the context item. If no type has been explicitly declared for the context
     * item, an instance of AnyItemType (representing the type item()) is returned.
     */

    public ItemType getRequiredContextItemType() {
        return requiredContextItemType;
    }

    /**
     * Get the stack frame map for global variables.
     * <p/>
     * This method is intended for internal use.
     */

    public SlotManager getGlobalStackFrameMap() {
        return executable.getGlobalVariableMap();
    }

    /**
     * Declare a global variable. A variable must normally be declared before an expression referring
     * to it is compiled, but there are exceptions where a set of modules in the same namespace
     * import each other cyclically. Global variables are normally declared in the Query Prolog, but
     * they can also be predeclared using this API. All global variables are held in the StaticQueryContext
     * for the main module. The fact that a global variable is present therefore does not mean that it
     * is visible: there are two additional conditions (a) the namespace must be imported into the
     * module where the reference appears, and (b) the declaration must not be in the same module and textually
     * after the reference.
     *
     * <p>Note that the same VariableDeclation object cannot be used with more than one query.  This is because
     * the VariableDeclaration is modified internally to hold a list of references to all the places where
     * the variable is used.</p>
     *
     * @since 8.4
     */

    public void declareVariable(VariableDeclaration var) throws StaticError {
        int key = var.getNameCode() & NamePool.FP_MASK;
        if (variables.get(key) != null) {
            GlobalVariableDefinition old = (GlobalVariableDefinition)variables.get(key);
            // TODO: must it be a GlobalVariableDefinition?  What happens if not?
            if (old == var) {
                // do nothing
            } else {
                StaticError err = new StaticError("Duplicate definition of global variable "
                        + var.getVariableName()
                        + " (see line " + old.getLineNumber() /*+ " in module " + old.getSystemId()*/ + ')');
                err.setErrorCode("XQST0049");
                if (var instanceof GlobalVariableDefinition) {
                    ExpressionLocation loc = new ExpressionLocation();
                    loc.setLineNumber(((GlobalVariableDefinition)var).getLineNumber());
                    loc.setSystemId(((GlobalVariableDefinition)var).getSystemId());
                    err.setLocator(loc);
                }
                throw err;
            }
        }
        variables.put(key, var);

        final IntHashMap libVars = getTopLevelModule(this).libraryVariables;
        GlobalVariableDefinition old = (GlobalVariableDefinition)libVars.get(key);
        if (old == null || old == var) {
            // do nothing
        } else {
            StaticError err = new StaticError("Duplicate definition of global variable "
                    + var.getVariableName()
                    + " (see line " + old.getLineNumber() + " in module " + old.getSystemId() + ')');
            err.setErrorCode("XQST0049");
            if (var instanceof GlobalVariableDefinition) {
                ExpressionLocation loc = new ExpressionLocation();
                loc.setLineNumber(((GlobalVariableDefinition)var).getLineNumber());
                loc.setSystemId(((GlobalVariableDefinition)var).getSystemId());
                err.setLocator(loc);
            }
            throw err;
        }

        if (!isTopLevelModule()) {
            libVars.put(key, var);
        }
    }

    /**
     * Fixup all references to global variables.
     * <p/>
     * This method is for internal use by the Query Parser only.
     */

    public List fixupGlobalVariables(SlotManager globalVariableMap) throws StaticError {
        List compiledVars = new ArrayList(20);
        Iterator[] iters = {variables.valueIterator(), libraryVariables.valueIterator()};
        for (int i=0; i<2; i++) {
            while (iters[i].hasNext()) {
                GlobalVariableDefinition var = (GlobalVariableDefinition)iters[i].next();
                try {
                    int slot = globalVariableMap.allocateSlotNumber(var.getNameCode() & NamePool.FP_MASK);
                    GlobalVariable gv = var.getCompiledVariable();
                    if (gv == null) {
                        gv = var.compile(this, slot);
                    }
                    if (!compiledVars.contains(gv)) {
                        compiledVars.add(gv);
                    }
                } catch (XPathException err) {
                    throw StaticError.makeStaticError(err);
                }
            }
        }
        return compiledVars;
    }

    /**
     * Get global variables declared in this module
     * @return an Iterator whose items are GlobalVariableDefinition objects
     */

    public Iterator getModuleVariables() {
        return variables.valueIterator();
    }

    /**
     * Get references to undeclared variables.
     * return a list of XPathException objects, one for each undeclared variable
     */

//     public List getUndeclaredVariables(SlotManager globalVariableMap) throws StaticError {
//        List undeclaredVars = new ArrayList(20);
//        Iterator iter = variables.valueIterator();
//        while (iter.hasNext()) {
//            GlobalVariableDefinition var = (GlobalVariableDefinition)iter.next();
//            if (var instanceof UndeclaredVariable) {
//                Iterator refs = var.iterateReferences();
//                while (refs.hasNext()) {
//                    VariableReference ref = (VariableReference)refs.next();
//                    StaticError err = new StaticError("Unresolved reference to variable $" + var.getVariableName());
//                    err.setLocator(ref);
//                    err.setErrorCode("XPST0008");
//                    undeclaredVars.add(err);
//                }
//                // Note: not an error if there are no references
//            } else {
//                // A GlobalVariableDeclaration that was introduced by importing may still need to have its
//                // references bound
//                Iterator refs = var.iterateReferences();
//                while (refs.hasNext()) {
//                    VariableReference ref = (VariableReference)refs.next();
//                    if (ref.getBinding() == null) {
//                        ref.fixup(var.getCompiledVariable());
//                    }
//                }
//            }
//        }
//        return undeclaredVars;
//    }

    public void typeCheckGlobalVariables(List compiledVars) throws StaticError {
        try {
            Iterator iter = compiledVars.iterator();
            Stack stack = new Stack();
            while (iter.hasNext()) {
                GlobalVariable gv = (GlobalVariable)iter.next();
                gv.lookForCycles(stack);
                GlobalVariableDefinition.typeCheck(this, gv);
            }
        } catch (XPathException e) {
            throw StaticError.makeStaticError(e);
        }
    }

    /**
     * Produce "explain" output for all global variables.
     * <p/>
     * This method is intended primarily for internal use.
     */

    public void explainGlobalVariables() {
        Iterator iter = variables.valueIterator();
        while (iter.hasNext()) {
            GlobalVariableDefinition var = (GlobalVariableDefinition)iter.next();
            var.explain(getConfiguration());
        }
    }

    /**
     * Get an iterator over the variables defined in this module.
     * <p/>
     * This method is intended primarily for internal use.
     *
     * @return an Iterator, whose items are VariableDeclaration objects.  It returns
     *         all variables known to this module including those imported from elsewhere; they
     *         can be distinguished by their namespace. The variables are returned in order of
     *         declaration.
     */

//    public Iterator getVariableDeclarations() {
//        return variableList.iterator();
//    }

    /**
     * Get the stack frame map for local variables in the "main" query expression.
     * <p/>
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
     *
     * @since 8.4
     */

    public NamePool getNamePool() {
        return namePool;
    }

    /**
     * Issue a compile-time warning. This method is used during XQuery expression compilation to
     * output warning conditions.
     * <p/>
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
     *
     * @since 8.4
     */

    public void setBaseURI(String baseURI) {
        this.baseURI = baseURI;
    }

    /**
     * Get the system ID of the container of the expression. Used to construct error messages.
     * Note that the systemID and the Base URI are currently identical, but they might be distinguished
     * in the future.
     *
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
     *
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
     *
     * @return -1 always
     */

    public int getLineNumber() {
        return -1;
    }


    /**
     * Bind a variable used in a query to the expression in which it is declared.
     * <p/>
     * This method is provided for use by the XQuery parser, and it should not be called by the user of
     * the API, or overridden, unless variables are to be declared using a mechanism other than the
     * declareVariable method of this class.
     */

    public VariableReference bindVariable(int fingerprint) throws StaticError {
        VariableDeclaration var = (VariableDeclaration)variables.get(fingerprint);
        if (var == null) {
            String uri = getNamePool().getURI(fingerprint);
            //String local = getNamePool().getLocalName(fingerprint);
            if (importedModuleNamespaces.contains(uri)) {
                StaticQueryContext main = getTopLevelModule(this);
                var = (VariableDeclaration)main.libraryVariables.get(fingerprint);
                if (var == null) {
                    // If the namespace has been imported there's the possibility that
                    // the variable declaration hasn't yet been read, because of the limited provision
                    // for cyclic imports
                    UndeclaredVariable uvar = new UndeclaredVariable();
                    uvar.setNameCode(fingerprint);
                    uvar.setVariableName(getNamePool().getDisplayName(fingerprint));
                    VariableReference ref = new VariableReference(uvar);
                    undeclaredVariables.put(fingerprint, uvar);
                    return ref;
                } else {
                    GlobalVariableDefinition gvar = ((GlobalVariableDefinition)var);
                    checkImportedType(gvar.getRequiredType(), gvar);
                }
            } else {
                // If the namespace hasn't been imported then we might as well throw the error right away
                StaticError err = new StaticError("Unresolved reference to variable");
                err.setErrorCode("XPST0008");
                    // the message isn't used...
                throw err;
            }
        }
        return new VariableReference(var);
    }

    /**
     * Set the function library used for binding any function call appearing within the query module.
     * <p/>
     * This method is available for use by advanced applications. The details of the FunctionLibrary
     * interface are subject to change. Applications using this interface take responsibility for
     * ensuring that the results conform to the constraints imposed by the XQuery language specification,
     * for example that one function within a query module can call other functions defined in the same
     * query module.
     *
     * @param functionLibrary the FunctionLibrary to be used. This will typically be a
     *                        FunctionLibraryList; in most cases it will be a slightly modified copy of a FunctionLibraryList
     *                        constructed by the system and obtained using the {@link #getFunctionLibrary} method.
     * @see FunctionLibraryList
     */

    public void setFunctionLibraryList(FunctionLibraryList functionLibrary) {
        this.functionLibraryList = functionLibrary;
    }

    /**
     * Get the function library containing all the in-scope functions available in this static
     * context (that is, the functions available in this query module).
     * <p/>
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
     * Get the functions declared locally within this module
     */

    public XQueryFunctionLibrary getLocalFunctionLibrary() {
        return (XQueryFunctionLibrary)functionLibraryList.get(localFunctionLibraryNr);
    }

    /**
     * Register a user-defined XQuery function.
     * <p/>
     * This method is intended for internal use only.
     */

    public void declareFunction(XQueryFunction function) throws StaticError {
        if (function.getNumberOfArguments() == 1) {
            SchemaType t = config.getSchemaType(function.getNameCode() & NamePool.FP_MASK);
            if (t != null && t.isAtomicType()) {
                StaticError err = new StaticError("Function name " + function.getFunctionDisplayName(getNamePool()) +
                        " clashes with the name of the constructor function for an atomic type");
                err.setErrorCode("XQST0034");
                throw err;
            }
        }
        XQueryFunctionLibrary local = getLocalFunctionLibrary();
        local.declareFunction(function);
        StaticQueryContext main = getTopLevelModule(this);
        main.globalFunctionLibrary.declareFunction(function);
    }

    /**
     * Bind function calls that could not be bound when first encountered. These
     * will either be forwards references to functions declared later in the same query module,
     * or in modules that are being imported recursively, or errors.
     * <p/>
     * This method is for internal use only.
     *
     * @throws org.orbeon.saxon.trans.StaticError if a function call refers to a function that has
     *                                        not been declared
     */

    public void bindUnboundFunctionCalls() throws XPathException {
        UnboundFunctionLibrary lib = (UnboundFunctionLibrary)functionLibraryList.get(unboundFunctionLibraryNr);
        lib.bindUnboundFunctionCalls(functionLibraryList, config);
    }

    /**
     * Fixup all references to global functions. This method is called
     * on completion of query parsing. Each XQueryFunction is required to
     * bind all references to that function to the object representing the run-time
     * executable code of the function.
     * <p/>
     * This method is for internal use only.
     */

    public void fixupGlobalFunctions() throws XPathException {
        globalFunctionLibrary.fixupGlobalFunctions(this);
    }

    /**
     * Output "explain" information about each declared function.
     * <p/>
     * This method is intended primarily for internal use.
     */

    public void explainGlobalFunctions() throws XPathException {
        globalFunctionLibrary.explainGlobalFunctions();
    }

    /**
     * Get the function with a given name and arity. This method is provided so that XQuery functions
     * can be called directly from a Java application. Note that there is no type checking or conversion
     * of arguments when this is done: the arguments must be provided in exactly the form that the function
     * signature declares them.
     *
     * @param uri       the uri of the function name
     * @param localName the local part of the function name
     * @param arity     the number of arguments.
     * @since 8.4
     */

    public UserFunction getUserDefinedFunction(String uri, String localName, int arity) {
        return globalFunctionLibrary.getUserDefinedFunction(uri, localName, arity);
    }

    /**
     * Bind unbound variables (these are typically variables that reference another module
     * participating in a same-namespace cycle, since local forwards references are not allowed
     */

    public void bindUnboundVariables() throws XPathException {
        for (Iterator iter = undeclaredVariables.valueIterator(); iter.hasNext();) {
            UndeclaredVariable uv = (UndeclaredVariable)iter.next();
            int fingerprint = uv.getNameCode() & NamePool.FP_MASK;
            VariableDeclaration var = (VariableDeclaration)variables.get(fingerprint);
            if (var == null) {
                String uri = getNamePool().getURI(fingerprint);
                if (importedModuleNamespaces.contains(uri)) {
                    StaticQueryContext main = getTopLevelModule(this);
                    var = (VariableDeclaration)main.libraryVariables.get(fingerprint);
                }
            }
            if (var == null) {
                StaticError err = new StaticError("Unresolved reference to variable $" + uv.getVariableName());
                err.setErrorCode("XPST0008");
                throw err;
            } else {
                GlobalVariableDefinition gvar = ((GlobalVariableDefinition)var);
                checkImportedType(gvar.getRequiredType(), gvar);
                uv.transferReferences(var);
            }
        }
    }

    /**
     * Determine whether Backwards Compatible Mode is used
     *
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
     * <p/>
     *
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
     *
     * @param namespace The namespace of the required schema. Supply "" for
     *                  a no-namespace schema.
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
     * Get the set of imported schemas
     *
     * @return a Set, the set of URIs representing the names of imported schemas
     */

    public Set getImportedSchemaNamespaces() {
        return importedSchemata;
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
     * Set the construction mode for this module
     *
     * @param mode one of {@link Validation#STRIP}, {@link Validation#PRESERVE}
     * @since 8.4
     */

    public void setConstructionMode(int mode) {
        constructionMode = mode;
    }

    /**
     * Get the current validation mode
     *
     * @return one of {@link Validation#STRIP}, {@link Validation#PRESERVE}
     * @since 8.4
     */

    public int getConstructionMode() {
        return constructionMode;
    }

    /**
     * Supporting method to load a query module. Used also by saxon:import-query in XSLT.
     * <p/>
     * This method is intended for internal use only.
     *
     * @param baseURI      The base URI and location URI of the module
     * @param executable   The Executable
     * @param importer     The importing query module (used to check for cycles). This is null
     *                     when loading a query module from XSLT.
     * @param query        The text of the query, after decoding and normalizing line endings
     * @param namespaceURI namespace of the query module to be loaded
     * @return The StaticQueryContext representing the loaded query module
     * @throws org.orbeon.saxon.trans.StaticError
     */

    public static StaticQueryContext makeStaticQueryContext(
            String baseURI, Executable executable, StaticQueryContext importer,
            String query, String namespaceURI) throws StaticError {
        Configuration config = executable.getConfiguration();
        StaticQueryContext module = new StaticQueryContext(config, importer);
        module.setLocationURI(baseURI);
        module.setBaseURI(baseURI);
        module.setModuleNamespace(namespaceURI);
        module.setExecutable(executable);
        if (importer != null) {
            module.setModuleURIResolver(importer.getModuleURIResolver());
        }

        executable.addQueryLibraryModule(module);
        QueryParser qp = new QueryParser();
        qp.parseLibraryModule(query, module);
        if (module.getModuleNamespace() == null) {
            StaticError err = new StaticError("Imported module must be a library module");
            err.setErrorCode("XQST0059");
            throw err;
        }
        if (!module.getModuleNamespace().equals(namespaceURI)) {
            StaticError err = new StaticError("Imported module's namespace does not match requested namespace");
            err.setErrorCode("XQST0059");
            throw err;
        }

        return module;
    }

    /**
     * Set the ErrorListener to be used to report compile-time errors in a query. This will also
     * be the default for the run-time error listener used to report dynamic errors
     */

    public void setErrorListener(ErrorListener listener) {
        this.errorListener = listener;
    }

    /**
     * Get the ErrorListener in use for this static context
     */

    public ErrorListener getErrorListener() {
        if (errorListener == null) {
            errorListener = config.getErrorListener();
        }
        return errorListener;
    }

    /**
     * Report a fatal error in the query (via the registered ErrorListener)
     */

    public void reportFatalError(XPathException err) {
        if (!err.hasBeenReported()) {
            try {
                getErrorListener().fatalError(err);
            } catch (TransformerException e) {
                // ignore secondary errors
            }
            err.setHasBeenReported();
        }
    }

    /**
     * Check that a SequenceType used in the definition of an imported variable or function
     * is available in the importing module
     */

    public void checkImportedType(SequenceType importedType, Declaration declaration)
            throws StaticError {
        ItemType type = importedType.getPrimaryType();
        if (type instanceof AnyItemType) {
            return;
        }
        if (type.isAtomicType()) {
            int f = ((AtomicType)type).getFingerprint();
            QueryReader.checkSchemaNamespaceImported(this, f, declaration);
        } else if (type instanceof ContentTypeTest) {
            SchemaType annotation = ((ContentTypeTest)type).getSchemaType();
            int f = annotation.getFingerprint();
            QueryReader.checkSchemaNamespaceImported(this, f, declaration);
        } else if (type instanceof CombinedNodeTest) {
            NodeTest[] tests = ((CombinedNodeTest)type).getComponentNodeTests();
            for (int i=0; i<tests.length; i++) {
                SequenceType st = SequenceType.makeSequenceType(tests[1], StaticProperty.EXACTLY_ONE);
                checkImportedType(st, declaration);
            }
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
