package org.orbeon.saxon.query;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.StandardErrorListener;
import org.orbeon.saxon.expr.CollationMap;
import org.orbeon.saxon.expr.Literal;
import org.orbeon.saxon.instruct.Executable;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.sort.NamedCollation;
import org.orbeon.saxon.sort.StringCollator;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.AnyItemType;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.value.SequenceType;
import org.orbeon.saxon.value.Value;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.Source;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.*;

/**
 * StaticQueryContext contains information used to build a StaticContext for use when processing XQuery
 * expressions.
 *
 * <p>Despite its name, <code>StaticQueryContext</code> no longer implements the <code>StaticContext</code>
 * interface, which means it cannot be used directly by Saxon when parsing a query. Instead it is first copied
 * to create a <code>QueryModule</code> object, which does implement the <code>StaticContext</code> interface.
 *
 * <p>The application constructs a StaticQueryContext
 * and initializes it with information about the context, for example, default namespaces, base URI, and so on.
 * When a query is compiled using this StaticQueryContext, the query parser makes a copy of the StaticQueryContext
 * and uses this internally, modifying it with information obtained from the query prolog, as well as information
 * such as namespace and variable declarations that can occur at any point in the query. The query parser does
 * not modify the original StaticQueryContext supplied by the calling application, which may therefore be used
 * for compiling multiple queries, serially or even in multiple threads.</p>
 *
 * <p>This class forms part of Saxon's published XQuery API. Methods that
 * are considered stable are labelled with the JavaDoc "since" tag.
 * The value 8.4 indicates a method introduced at or before Saxon 8.4; other
 * values indicate the version at which the method was introduced.</p>
 *
 * <p>In the longer term, this entire API may at some stage be superseded by a proposed
 * standard Java API for XQuery.</p>
 *
 * @since 8.4
 */

public class StaticQueryContext {

    private Configuration config;
    private NamePool namePool;
    private String baseURI;
    private HashMap userDeclaredNamespaces;
    private Set userDeclaredVariables;
    private Executable executable;
    private boolean inheritNamespaces = true;
    private boolean preserveNamespaces = true;
    private int constructionMode = Validation.PRESERVE;
    private NamespaceResolver externalNamespaceResolver = null;
    private CollationMap collations;
    private String defaultFunctionNamespace;
    private String defaultElementNamespace;
    private ItemType requiredContextItemType = AnyItemType.getInstance();
    private boolean preserveSpace = false;
    private boolean defaultEmptyLeast = true;
    private ModuleURIResolver moduleURIResolver;
    private ErrorListener errorListener;
    private boolean tracing;
    private boolean generateCode = false;
    private boolean isUpdating = false;

    /**
     * Private constructor used when copying a context
     */

    protected StaticQueryContext() {
    }

    /**
     * Create a StaticQueryContext using a given Configuration. This creates a StaticQueryContext for a main module
     * (that is, a module that is not a library module).
     * @param config the Saxon Configuration
     *
     * @since 8.4
     */

    public StaticQueryContext(Configuration config) {
        this.config = config;
        namePool = config.getNamePool();
        errorListener = config.getErrorListener();
        moduleURIResolver = config.getModuleURIResolver();
        if (errorListener instanceof StandardErrorListener) {
            errorListener = ((StandardErrorListener)errorListener).makeAnother(Configuration.XQUERY);
            ((StandardErrorListener)errorListener).setRecoveryPolicy(Configuration.DO_NOT_RECOVER);
        }
        collations = new CollationMap(config);
        reset();
    }



    /**
     * Reset the state of this StaticQueryContext to an uninitialized state
     *
     * @since 8.4
     */

    public void reset() {
        userDeclaredNamespaces = new HashMap(10);
        externalNamespaceResolver = null;
        collations = new CollationMap(config);
        errorListener = config.getErrorListener();
        if (errorListener instanceof StandardErrorListener) {
            errorListener = ((StandardErrorListener)errorListener).makeAnother(Configuration.XQUERY);
            ((StandardErrorListener)errorListener).setRecoveryPolicy(Configuration.DO_NOT_RECOVER);
        }
        constructionMode = getConfiguration().isSchemaAware(Configuration.XQUERY) ?
                Validation.PRESERVE : Validation.STRIP;
        preserveSpace = false;
        defaultEmptyLeast = true;
        requiredContextItemType = AnyItemType.getInstance();
        defaultFunctionNamespace = NamespaceConstant.FN;
        defaultElementNamespace = NamespaceConstant.NULL;
        moduleURIResolver = config.getModuleURIResolver();
        collations.setDefaultCollationName(NamespaceConstant.CODEPOINT_COLLATION_URI);
        clearNamespaces();
        generateCode = false;
    }

    /**
     * Set the Configuration options
     * @param config the Saxon Configuration
     * @throws IllegalArgumentException if the configuration supplied is different from the existing
     *                                  configuration
     * @since 8.4
     */

    public void setConfiguration(Configuration config) {
        if (this.config != null && this.config != config) {
            throw new IllegalArgumentException("Configuration cannot be changed dynamically");
        }
        this.config = config;
        namePool = config.getNamePool();
    }

    /**
     * Get the Configuration options
     * @return the Saxon configuration
     * @since 8.4
     */

    public Configuration getConfiguration() {
        return config;
    }

    /**
     * Set the Executable to contain this query. Normally a query constitutes its own Executable,
     * and the executable will then be created automatically. This method is used when the query is to
     * share the same executable as a host program, specifically, an XSLT stylesheet that imports the
     * query.
     * @param executable the executable
     */

    public void setExecutable(Executable executable) {
        this.executable = executable;
    }

    /**
     * Get the executable containing this query
     * @return the executable (or null if not set)
     */

    public Executable getExecutable() {
        return executable;
    }

    /**
     * Set the Base URI of the query
     * @param baseURI the base URI of the query
     * @since 8.4
     */

    public void setBaseURI(String baseURI) {
        this.baseURI = baseURI;
    }

    /**
     * Convenience method for building Saxon's internal representation of a source XML
     * document. The document will be built using Configuration (and NamePool) associated
     * with this StaticQueryContext.
     *
     * <p>This method is retained for backwards compatibility; however, it is merely a wrapper
     * around the method {@link Configuration#buildDocument}, which should be called in preference.</p>
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
        return config.buildDocument(source);
    }

    /**
     * Ask whether compile-time generation of trace code was requested
     * @since 9.0
     * @return true if compile-time generation of code was requested
     */

    public boolean isCompileWithTracing() {
        return tracing;
    }

    /**
     * Request compile-time generation of trace code (or not)
     * @param trace true if compile-time generation of trace code is required
     * @since 9.0
     */

    public void setCompileWithTracing(boolean trace) {
        tracing = trace;
    }

    /**
     * Indicate that the query should be optimized with a view to generating Java code.
     * This inhibits some rewrites to constructs for which code generation is not possible.
     * @param generateCode true if Java code is to be generated as the final output
     */

    public void setGeneratingJavaCode(boolean generateCode) {
        this.generateCode = generateCode;
    }

    /**
     * Ask whether this query is to be optimized with a view to generating Java code.
     * This inhibits some rewrites to constructs for which code generation is not possible.
     * @return true if Java code is to be generated as the final output
     */

    public boolean isGeneratingJavaCode() {
        return generateCode;
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
     * Set the construction mode for this module
     *
     * @param mode one of {@link org.orbeon.saxon.om.Validation#STRIP}, {@link org.orbeon.saxon.om.Validation#PRESERVE}
     * @since 8.4
     */

    public void setConstructionMode(int mode) {
        constructionMode = mode;
    }

    /**
     * Get the current construction mode
     *
     * @return one of {@link org.orbeon.saxon.om.Validation#STRIP}, {@link org.orbeon.saxon.om.Validation#PRESERVE}
     * @since 8.4
     */

    public int getConstructionMode() {
        return constructionMode;
    }

    /**
     * Prepare an XQuery query for subsequent evaluation. The source text of the query
     * is supplied as a String. The base URI of the query is taken from the static context,
     * and defaults to the current working directory.
     *
     * <p>Note that this interface makes the caller responsible for decoding the query and
     * presenting it as a string of characters. This means it is likely that any encoding
     * specified in the query prolog will be ignored.</p>
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
        QueryParser qp = getConfiguration().newQueryParser(isUpdating);
        qp.setCompileWithTracing(isCompileWithTracing() || config.isCompileWithTracing());
        QueryModule mainModule = new QueryModule(this);
        mainModule.setExecutable(executable);
        return qp.makeXQueryExpression(query, mainModule, config);
    }

    /**
     * Prepare an XQuery query for subsequent evaluation. The Query is supplied
     * in the form of a Reader. The base URI of the query is taken from the static context,
     * and defaults to the current working directory.
     *
     * <p>Note that this interface makes the Reader responsible for decoding the query and
     * presenting it as a stream of characters. This means it is likely that any encoding
     * specified in the query prolog will be ignored. Also, some implementations of Reader
     * cannot handle a byte order mark.</p>
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
     * Declare a namespace whose prefix can be used in expressions. This is
     * equivalent to declaring a prefix in the Query prolog.
     * Any namespace declared in the Query prolog overrides a namespace declared using
     * this API.
     *
     * @param prefix   The namespace prefix. Must not be null. Setting this to "" means that the
     *                 namespace will be used as the default namespace for elements and types.
     * @param uri      The namespace URI. Must not be null. The value "" (zero-length string) is used
     *                 to undeclare a namespace; it is not an error if there is no existing binding for
     *                 the namespace prefix.
     * @throws NullPointerException if either the prefix or URI is null
     * @throws IllegalArgumentException if the prefix is "xml" and the namespace is not the XML namespace, or vice
     * versa.
     * @since 9.0
     */

    public void declareNamespace(String prefix, String uri) {
        if (prefix == null) {
            throw new NullPointerException("Null prefix supplied to declareNamespace()");
        }
        if (uri == null) {
            throw new NullPointerException("Null namespace URI supplied to declareNamespace()");
        }
        if ((prefix.equals("xml") != uri.equals(NamespaceConstant.XML))) {
            throw new IllegalArgumentException("Misdeclaration of XML namespace");
        }
        if (prefix.length() == 0) {
            defaultElementNamespace = (uri==null ? "" : uri);
        }
        if (uri.length() == 0 && prefix.length() != 0) {
            userDeclaredNamespaces.remove(prefix);
        } else {
            userDeclaredNamespaces.put(prefix, uri);
            namePool.allocateNamespaceCode(prefix, uri);
        }
    }

    /**
     * Declare a namespace whose prefix can be used in expressions. This is
     * equivalent to declaring a prefix in the Query prolog. The term "passive"
     * was a term from a draft XQuery proposal indicating a namespace that won't
     * be copied into the result tree. Passive namespaces are never undeclared.
     * Any namespace declared in the Query prolog overrides a namespace declared using
     * this API.
     *
     * @param prefix   The namespace prefix. Must not be null.
     * @param uri      The namespace URI. Must not be null. The value "" (zero-length string) is used
     *                 to undeclare a namespace; it is not an error if there is no existing binding for
     *                 the namespace prefix.
     * @param explicit Must be false (the value true was previously reserved for internal use, but is
     *                 no longer permitted)
     * @since 8.4
     * @deprecated since 9.0. Use {@link #declareNamespace}
     */

    public void declarePassiveNamespace(String prefix, String uri, boolean explicit) throws XPathException {
        if (explicit) {
            throw new IllegalArgumentException("explicit must be false");
        }
        declareNamespace(prefix, uri);
    }

    /**
     * Clear all the user-declared namespaces
     *
     * @since 9.0
     */

    public void clearNamespaces() {
        userDeclaredNamespaces.clear();
        declareNamespace("xml", NamespaceConstant.XML);
        declareNamespace("xs", NamespaceConstant.SCHEMA);
        declareNamespace("xsi", NamespaceConstant.SCHEMA_INSTANCE);
        declareNamespace("fn", NamespaceConstant.FN);
        declareNamespace("local", NamespaceConstant.LOCAL);
        declareNamespace("saxon", NamespaceConstant.SAXON);
        declareNamespace("", "");

    }

    /**
     * Get the map of user-declared namespaces
     * @return the user-declared namespaces
     */

    protected HashMap getUserDeclaredNamespaces() {
        return userDeclaredNamespaces;
    }

    /**
     * Clear all the declared passive namespaces, except for the standard ones (xml, saxon, etc)
     *
     * @since 8.4
     * @deprecated since 9.0 - use {@link #clearNamespaces}
     */

    public void clearPassiveNamespaces() {
        clearNamespaces();
    }


    /**
     * Get the namespace prefixes that have been declared using the method {@link #declareNamespace}
     * @return an iterator that returns the namespace prefixes that have been explicitly declared, as
     * strings. The default namespace for elements and types will be included, using the prefix "".
     * @since 9.0
     */

    public Iterator iterateDeclaredPrefixes() {
        return userDeclaredNamespaces.keySet().iterator();
    }

    /**
     * Get the namespace URI for a given prefix, which must have been declared using the method
     * {@link #declareNamespace}. Note that this method will not call the external namespace resolver
     * to resolve the prefix.
     * @param prefix the namespace prefix, or "" to represent the null prefix
     * @return the namespace URI. Returns "" to represent the non-namespace,
     * null to indicate that the prefix has not been declared
     */

    public String getNamespaceForPrefix(String prefix) {
        return (String)userDeclaredNamespaces.get(prefix);
    }

    /**
     * Set an external namespace resolver. If a namespace prefix cannot be resolved using any
     * other mechanism, then as a last resort the external namespace resolver is called to
     * obtain a URI for the given prefix.
     *
     * <p><i>Changed in Saxon 9.0 so that the namespaces resolved by the external namespace resolver
     * are available at run-time, just like namespaces declared in the query prolog. In consequence,
     * the supplied NamespaceResolver must now implement the
     * {@link org.orbeon.saxon.om.NamespaceResolver#iteratePrefixes()} method.</i></p>
     *
     * @param resolver the external namespace resolver
     */

    public void setExternalNamespaceResolver(NamespaceResolver resolver) {
        externalNamespaceResolver = resolver;
    }

    /**
     * Get the external namespace resolver that has been registered using
     * setExternalNamespaceResolver(), if any.
     * @return the external namespace resolver
     */

    public NamespaceResolver getExternalNamespaceResolver() {
        return externalNamespaceResolver;
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
     * @param uri the namespace URI to be used as the default namespace for elements and types
     * @since 8.4
     */

    public void setDefaultElementNamespace(String uri) {
        defaultElementNamespace = uri;
        declareNamespace("", uri);
    }

    /**
     * Get the default namespace for elements and types
     * @return the namespace URI to be used as the default namespace for elements and types
     * @since 8.9 Modified in 8.9 to return the namespace URI as a string rather than an integer code
     */

    public String getDefaultElementNamespace() {
        return defaultElementNamespace;
    }

    /**
     * Declare a global variable. This has the same effect as including a global variable declaration
     * in the Query Prolog of the main query module. A static error occurs when compiling the query if the
     * query prolog contains a declaration of the same variable.
     * @param qName the qualified name of the variable
     * @param type the declared type of the variable
     * @param value the initial value of the variable. May be null if the variable is external.
     * @param external true if the variable is external, that is, if its value may be set at run-time.
     * @throws NullPointerException if the value is null, unless the variable is external
     * @throws XPathException if the value of the variable is not consistent with its type.
     * @since 9.1
     */

    public void declareGlobalVariable(
            StructuredQName qName, SequenceType type, ValueRepresentation value, boolean external)
            throws XPathException {
        if (value == null && !external) {
            throw new NullPointerException("No initial value for declared variable");
        }
        Value val = Value.asValue(value);
        if (!type.matches(val, getConfiguration())) {
            throw new XPathException("Value of declared variable does not match its type");
        }
        GlobalVariableDefinition var = new GlobalVariableDefinition();
        var.setVariableQName(qName);
        var.setRequiredType(type);
        var.setValueExpression(Literal.makeLiteral(val));
        var.setIsParameter(external);
        if (userDeclaredVariables == null) {
            userDeclaredVariables = new HashSet();
        }
        userDeclaredVariables.add(var);
    }

    /**
     * Iterate over all the declared global variables
     * @return an iterator over all the global variables that have been declared. They are returned
     * as instances of class {@link GlobalVariableDefinition}
     * @since 9.1
     */

    public Iterator iterateDeclaredGlobalVariables() {
        if (userDeclaredVariables == null) {
            return Collections.EMPTY_LIST.iterator();
        } else {
            return userDeclaredVariables.iterator();
        }
    }

    /**
     * Clear all declared global variables
     * @since 9.1
     */

    public void clearDeclaredGlobalVariables() {
        userDeclaredVariables = null;
    }

    /**
     * Set a user-defined ModuleURIResolver for resolving URIs used in "import module"
     * declarations in the XQuery prolog.
     * This will be used for resolving URIs in XQuery "import module" declarations, overriding
     * any ModuleURIResolver that was specified as part of the configuration.
     * @param resolver the ModuleURIResolver to be used
     */

    public void setModuleURIResolver(ModuleURIResolver resolver) {
        moduleURIResolver = resolver;
    }

    /**
     * Get the user-defined ModuleURIResolver for resolving URIs used in "import module"
     * declarations in the XQuery prolog; returns null if none has been explicitly set either
     * on the StaticQueryContext or on the Configuration.
     * @return the registered ModuleURIResolver
     */

    public ModuleURIResolver getModuleURIResolver() {
        return moduleURIResolver;
    }


    /**
     * Declare a named collation. Collations are only available in a query if this method
     * has been called externally to declare the collation and associate it with an
     * implementation, in the form of a Java Comparator. The default collation is the
     * Unicode codepoint collation, unless otherwise specified.
     *
     * @param name       The name of the collation (technically, a URI)
     * @param comparator The Java Comparator used to implement the collating sequence
     * @since 8.4.
     */

    public void declareCollation(String name, Comparator comparator) {
        declareCollation(name, new NamedCollation(name, comparator));
    }


    /**
     * Declare a named collation. Collations are only available in a query if this method
     * has been called externally to declare the collation and associate it with an
     * implementation, in the form of a Java StringCollator. The default collation is the
     * Unicode codepoint collation, unless otherwise specified.
     *
     * @param name       The name of the collation (technically, a URI)
     * @param comparator The Java Comparator used to implement the collating sequence
     * @since 8.9.
     */

    public void declareCollation(String name, StringCollator comparator) {
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

    public StringCollator getCollation(String name) {
        return collations.getNamedCollation(name);
    }

    /**
     * Get the collation map
     * @return the collation map, which identifies all the known collations
     */

    public CollationMap getCollationMap() {
        return collations;
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
     * @return the CollationMap containing all the collations defined in this static context
     */

    public CollationMap getAllCollations() {
        return new CollationMap(collations);
    }

    /**
     * Declare the static type of the context item. If this type is declared, and if a context item
     * is supplied when the query is invoked, then the context item must conform to this type (no
     * type conversion will take place to force it into this type).
     * @param type the required type of the context item
     */

    public void setRequiredContextItemType(ItemType type) {
        requiredContextItemType = type;
    }

    /**
     * Get the required type of the context item. If no type has been explicitly declared for the context
     * item, an instance of AnyItemType (representing the type item()) is returned.
     * @return the required type of the context item
     */

    public ItemType getRequiredContextItemType() {
        return requiredContextItemType;
    }

    /**
     * Get the NamePool used for compiling expressions
     * @return the name pool
     * @since 8.4
     */

    public NamePool getNamePool() {
        return namePool;
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
     * Set the policy for preserving boundary space
     * @param preserve true if boundary space is to be preserved, false if it is to be stripped
     * @since 9.0
     */

    public void setPreserveBoundarySpace(boolean preserve) {
        preserveSpace = preserve;
    }

    /**
     * Ask whether the policy for boundary space is "preserve" or "strip"
     * @return true if the policy is to preserve boundary space, false if it is to strip it
     * @since 9.0
     */

    public boolean isPreserveBoundarySpace() {
        return preserveSpace;
    }

    /**
     * Set the option for where an empty sequence appears in the collation order, if not otherwise
     * specified in the "order by" clause
     * @param least true if the empty sequence is considered less than any other value (the default),
     * false if it is considered greater than any other value
     * @since 9.0
     */

    public void setEmptyLeast(boolean least) {
        defaultEmptyLeast = least;
    }

    /**
     * Ask what is the option for where an empty sequence appears in the collation order, if not otherwise
     * specified in the "order by" clause
     * @return true if the empty sequence is considered less than any other value (the default),
     * false if it is considered greater than any other value
     * @since 9.0
     */

    public boolean isEmptyLeast() {
        return defaultEmptyLeast;
    }

    /**
     * Set the ErrorListener to be used to report compile-time errors in a query. This will also
     * be the default for the run-time error listener used to report dynamic errors
     * @param listener the ErrorListener to be used
     */

    public void setErrorListener(ErrorListener listener) {
        errorListener = listener;
    }

    /**
     * Get the ErrorListener in use for this static context
     * @return the registered ErrorListener
     */

    public ErrorListener getErrorListener() {
        if (errorListener == null) {
            errorListener = config.getErrorListener();
        }
        return errorListener;
    }

    /**
     * Say whether the query is allowed to be updating. XQuery update syntax will be rejected
     * during query compilation unless this flag is set.
     * @param updating true if the query is allowed to use the XQuery Update facility
     * (requires Saxon-SA). If set to false, the query must not be an updating query. If set
     * to true, it may be either an updating or a non-updating query.
     * @since 9.1
     */

    public void setUpdatingEnabled(boolean updating) {
        isUpdating = updating;
    }

    /**
     * Ask whether the query is allowed to be updating
     * @return true if the query is allowed to use the XQuery Update facility. Note that this
     * does not necessarily mean that the query is an updating query; but if the value is false,
     * the it must definitely be non-updating.
     * @since 9.1
     */

    public boolean isUpdatingEnabled() {
        return isUpdating;
    }

//    public static void main(String[] args) throws Exception {
//        StaticQueryContext c = new StaticQueryContext(new Configuration());
//        c.declareGlobalVariable(
//                new StructuredQName("", "", "ping"),
//                SequenceType.SINGLE_STRING,
//                new StringValue("pong"),
//                true
//        );
//        XQueryExpression exp = c.compileQuery("$ping");
//        DynamicQueryContext env = new DynamicQueryContext(c.getConfiguration());
//        //env.setParameterValue("ping", new StringValue("pang"));
//        exp.run(env, new StreamResult(System.out), null);
//
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
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
