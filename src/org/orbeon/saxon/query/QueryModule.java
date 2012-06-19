package org.orbeon.saxon.query;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.Platform;
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
import org.orbeon.saxon.sort.IntArraySet;
import org.orbeon.saxon.sort.StringCollator;
import org.orbeon.saxon.trace.ExpressionPresenter;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.*;
import org.orbeon.saxon.value.SequenceType;

import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * This class represents a query module, and includes information about the static context of the query module.
 * The class is intended for internal Saxon use. User settings that affect the static context are made in the
 * StaticQueryContext object, and those settings are copied to each QueryModule when the query module is compiled.
 */

public class QueryModule implements StaticContext {
    private boolean isMainModule;
    private Configuration config;
    private StaticQueryContext userQueryContext;
    private QueryModule topModule;
    private URI locationURI;
    private String baseURI;
    private String moduleNamespace;
    private short moduleNamespaceURICode;
    private HashMap explicitPrologNamespaces;
    private Stack activeNamespaces;
    private HashMap variables;           // global variables declared in this module
    private HashMap libraryVariables;    // all global variables defined in library modules
                                         // defined only on the top-level module
    private HashMap undeclaredVariables;
    private HashSet importedSchemata;    // The schema target namespaces imported into this module
    private HashMap loadedSchemata;      // For the top-level module only, all imported schemas for all modules,
                                         // Key is the targetNamespace, value is the set of absolutized location URIs
    private Executable executable;
    private List importers;  // A list of QueryModule objects representing the modules that import this one,
                             // Null for the main module
                             // This is needed *only* to implement the rules banning cyclic imports
    private FunctionLibraryList functionLibraryList;
    private XQueryFunctionLibrary globalFunctionLibrary;      // used only on a top-level module
    private int localFunctionLibraryNr;
    private int importedFunctionLibraryNr;
    private int unboundFunctionLibraryNr;
    private Set importedModuleNamespaces;
    private boolean inheritNamespaces = true;
    private boolean preserveNamespaces = true;
    private int constructionMode = Validation.PRESERVE;
    private String defaultFunctionNamespace;
    private String defaultElementNamespace;
    private boolean preserveSpace = false;
    private boolean defaultEmptyLeast = true;
    private String defaultCollationName;
    private int revalidationMode = Validation.SKIP;
    private boolean isUpdating = false;

    /**
     * Create a QueryModule for a main module, copying the data that has been set up in a
     * StaticQueryContext object
     * @param sqc the StaticQueryContext object from which this module is initialized
     */

    public QueryModule(StaticQueryContext sqc) throws XPathException {
        config = sqc.getConfiguration();
        isMainModule = true;
        topModule = this;
        activeNamespaces = new Stack();
        baseURI = sqc.getBaseURI();
        try {
            locationURI = (baseURI == null ? null : new URI(baseURI));
        } catch (URISyntaxException err) {
            throw new XPathException("Invalid location URI: " + baseURI);
        }
        executable = null;
        importers = null;
        init(sqc);
        resetFunctionLibraries();
        for (Iterator vars = sqc.iterateDeclaredGlobalVariables(); vars.hasNext(); ) {
            GlobalVariableDefinition var = (GlobalVariableDefinition)vars.next();
            declareVariable(var);
        }
    }

    /**
     * Create a QueryModule for a library module.
     * @param config the Saxon configuration
     * @param importer the module that imported this module. This may be null, in the case where
     * the library module is being imported into an XSLT stylesheet
     */

    public QueryModule(Configuration config, QueryModule importer) {
        this.config = config;
        importers = null;
        if (importer == null) {
            topModule = this;
        } else {
            topModule = importer.topModule;
            userQueryContext = importer.userQueryContext;
            importers = new ArrayList(2);
            importers.add(importer);
        }
        init(userQueryContext);
        activeNamespaces = new Stack();
        executable = null;
    }

    /**
     * Initialize data from a user-supplied StaticQueryContext object
     * @param sqc the user-supplied StaticQueryContext. Null if this is a library module imported
     * into XSLT.
     */

    private void init(StaticQueryContext sqc) {
        //reset();
        userQueryContext = sqc;
        variables = new HashMap(10);
        undeclaredVariables = new HashMap(5);
        if (isTopLevelModule()) {
            libraryVariables = new HashMap(10);
        }
        importedSchemata = null;
        importedModuleNamespaces = new HashSet(5);
        moduleNamespace = null;
        moduleNamespaceURICode = 0;
        activeNamespaces = new Stack();
        resetFunctionLibraries();
        explicitPrologNamespaces = new HashMap(10);
        if (sqc != null) {
            executable = sqc.getExecutable();
            inheritNamespaces = sqc.isInheritNamespaces();
            preserveNamespaces = sqc.isPreserveNamespaces();
            preserveSpace = sqc.isPreserveBoundarySpace();
            defaultEmptyLeast = sqc.isEmptyLeast();
            defaultFunctionNamespace = sqc.getDefaultFunctionNamespace();
            defaultElementNamespace = sqc.getDefaultElementNamespace();
            defaultCollationName = sqc.getDefaultCollationName();
            constructionMode = sqc.getConstructionMode();
            isUpdating = sqc.isUpdatingEnabled();
        }
    }

    /**
     * Supporting method to load an imported library module.
     * Used also by saxon:import-query in XSLT.
     * <p/>
     * This method is intended for internal use only.
     *
     * @param baseURI      The base URI and location URI of the module
     * @param executable   The Executable
     * @param importer     The importing query module (used to check for cycles). This is null
     *                     when loading a query module from XSLT.
     * @param query        The text of the query, after decoding and normalizing line endings
     * @param namespaceURI namespace of the query module to be loaded
     * @param allowCycles  True if cycles of module imports (disallowed by the spec) are to be permitted
     * @return The StaticQueryContext representing the loaded query module
     * @throws XPathException
     */

    public static QueryModule makeQueryModule (
            String baseURI, Executable executable, QueryModule importer,
            String query, String namespaceURI, boolean allowCycles) throws XPathException {
        Configuration config = executable.getConfiguration();
        QueryModule module = new QueryModule(config, importer);
        try {
            module.setLocationURI(new URI(baseURI));
        } catch (URISyntaxException e) {
            throw new XPathException("Invalid location URI " + baseURI, e);
        }
        module.setBaseURI(baseURI);
        module.setExecutable(executable);
        module.setModuleNamespace(namespaceURI);

        executable.addQueryLibraryModule(module);
        QueryParser qp = config.newQueryParser(importer.isUpdating());
        qp.setCompileWithTracing(config.isCompileWithTracing());
        qp.setDisableCycleChecks(allowCycles);
        qp.parseLibraryModule(query, module);

        if (module.getModuleNamespace() == null) {
            XPathException err = new XPathException("Imported module must be a library module");
            err.setErrorCode("XQST0059");
            err.setIsStaticError(true);
            throw err;
        }
        if (!module.getModuleNamespace().equals(namespaceURI)) {
            XPathException err = new XPathException("Imported module's namespace does not match requested namespace");
            err.setErrorCode("XQST0059");
            err.setIsStaticError(true);
            throw err;
        }

        return module;
    }

    /**
     * Reset function libraries
     */

    private void resetFunctionLibraries() {
        Configuration config = getConfiguration();
        if (isTopLevelModule()) {
            globalFunctionLibrary = new XQueryFunctionLibrary(config);
        }

        functionLibraryList = new FunctionLibraryList();
        functionLibraryList.addFunctionLibrary(
                SystemFunctionLibrary.getSystemFunctionLibrary(SystemFunctionLibrary.XPATH_ONLY));
        functionLibraryList.addFunctionLibrary(config.getVendorFunctionLibrary());
        functionLibraryList.addFunctionLibrary(new ConstructorFunctionLibrary(config));

        localFunctionLibraryNr = functionLibraryList.addFunctionLibrary(
                new XQueryFunctionLibrary(config));

        importedFunctionLibraryNr = functionLibraryList.addFunctionLibrary(
                new ImportedFunctionLibrary(this, getTopLevelModule().getGlobalFunctionLibrary()));

        if (config.isAllowExternalFunctions()) {
            Configuration.getPlatform().addFunctionLibraries(functionLibraryList, config, Configuration.XQUERY);
        }        

        unboundFunctionLibraryNr = functionLibraryList.addFunctionLibrary(
                new UnboundFunctionLibrary());
    }

    /**
     * Get the Saxon Configuration
     * @return the Saxon Configuration
     */

    public Configuration getConfiguration() {
        return config;
    }

    /**
     * Get the NamePool used for compiling expressions
     * @return the name pool
     */

    public NamePool getNamePool() {
        return config.getNamePool();
    }

    /**
     * Test whether this is a "top-level" module. This is true for a main module and also for a
     * module directly imported into an XSLT stylesheet. It may also be true in future for independently-compiled
     * modules
     * @return true if this is top-level module
     */

    public boolean isTopLevelModule() {
        return this == topModule;
    }

    /**
     * Ask whether this is a "main" module, in the sense of the XQuery language specification
     * @return true if this is a main module, false if it is a library model
     */

    public boolean isMainModule() {
        return isMainModule;
    }

    /**
     * Check whether this module is allowed to import a module with namespace N. Note that before
     * calling this we have already handled the exception case where a module imports another in the same
     * namespace (this is the only case where cycles are allowed, though as a late change to the spec they
     * are no longer useful, since they cannot depend on each other cyclically)
     * @param namespace the namespace to be tested
     * @return true if the import is permitted
     */

    public boolean mayImportModule(String namespace) {
        if (namespace.equals(moduleNamespace)) {
            return false;
        }
        if (importers == null) {
            return true;
        }
        for (int i=0; i<importers.size(); i++) {
            if (!((QueryModule)importers.get(i)).mayImportModule(namespace)) {
                return false;
            }
        }
        return true;
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
     */

    public void setPreserveNamespaces(boolean inherit) {
        preserveNamespaces = inherit;
    }

    /**
     * Get the namespace copy mode
     *
     * @return true if namespaces are preserved, false if not
     */

    public boolean isPreserveNamespaces() {
        return preserveNamespaces;
    }

    /**
     * Set the construction mode for this module
     *
     * @param mode one of {@link org.orbeon.saxon.om.Validation#STRIP}, {@link org.orbeon.saxon.om.Validation#PRESERVE}
     */

    public void setConstructionMode(int mode) {
        constructionMode = mode;
    }

    /**
     * Get the current construction mode
     *
     * @return one of {@link org.orbeon.saxon.om.Validation#STRIP}, {@link org.orbeon.saxon.om.Validation#PRESERVE}
     */

    public int getConstructionMode() {
        return constructionMode;
    }

    /**
     * Set the policy for preserving boundary space
     * @param preserve true if boundary space is to be preserved, false if it is to be stripped
     */

    public void setPreserveBoundarySpace(boolean preserve) {
        preserveSpace = preserve;
    }

    /**
     * Ask whether the policy for boundary space is "preserve" or "strip"
     * @return true if the policy is to preserve boundary space, false if it is to strip it
     */

    public boolean isPreserveBoundarySpace() {
        return preserveSpace;
    }

    /**
     * Set the option for where an empty sequence appears in the collation order, if not otherwise
     * specified in the "order by" clause
     * @param least true if the empty sequence is considered less than any other value (the default),
     * false if it is considered greater than any other value
     */

    public void setEmptyLeast(boolean least) {
        defaultEmptyLeast = least;
    }

    /**
     * Ask what is the option for where an empty sequence appears in the collation order, if not otherwise
     * specified in the "order by" clause
     * @return true if the empty sequence is considered less than any other value (the default),
     * false if it is considered greater than any other value
     */

    public boolean isEmptyLeast() {
        return defaultEmptyLeast;
    }


    /**
     * Get the function library object that holds details of global functions
     * @return the library of global functions
     */

    public XQueryFunctionLibrary getGlobalFunctionLibrary() {
        return globalFunctionLibrary;
    }

    /**
     * Get the function library object that holds details of imported functions
     * @return the library of imported functions
     */

    public ImportedFunctionLibrary getImportedFunctionLibrary() {
        return (ImportedFunctionLibrary)functionLibraryList.get(importedFunctionLibraryNr);
    }

    /**
     * Register that this module imports a particular module namespace
     * <p>This method is intended for internal use.</p>
     *
     * @param uri the URI of the imported namespace.
     */

    public void addImportedNamespace(String uri) {
        if (importedModuleNamespaces == null) {
            importedModuleNamespaces = new HashSet(5);
        }
        importedModuleNamespaces.add(uri);
        getImportedFunctionLibrary().addImportedNamespace(uri);
    }

    /**
     * Ask whether this module directly imports a particular namespace
     * <p>This method is intended for internal use.</p>
     * @param uri the URI of the possibly-imported namespace.
     * @return true if the schema for the namespace has been imported
     */

    public boolean importsNamespace(String uri) {
        return importedModuleNamespaces != null &&
                importedModuleNamespaces.contains(uri);
    }

    /**
     * Test whether this module imports a particular namespace directly or indirectly
     * <p>This method is intended for internal use.</p>
     * @param uri the URI of the possibly-imported namespace.
     * @return true if the schema for the namespace has been imported
     */

    public boolean importsNamespaceIndirectly(String uri) {
        if (importsNamespace(uri)) {
            return true;
        }
        for (Iterator it = iterateImportedNamespaces(); it.hasNext();) {
            String moduleURI = (String)it.next();
            List list = executable.getQueryLibraryModules(moduleURI);
            for (Iterator i2 = list.iterator(); i2.hasNext();) {
                QueryModule sqc = (QueryModule)i2.next();
                if (sqc.importsNamespaceIndirectly(uri)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Get an iterator over all the module namespaces that this module imports
     * @return an iterator over the imported namespaces (delivered as strings)
     */

    public Iterator iterateImportedNamespaces() {
        if (importedModuleNamespaces == null) {
            return Collections.EMPTY_LIST.iterator();
        }
        return importedModuleNamespaces.iterator();
    }

    /**
     * Get the QueryModule for the top-level module. This will normally be a main module,
     * but in the case of saxon:import-query it will be the library module that is imported into
     * the stylesheet
     * @return the StaticQueryContext object associated with the top level module
     */

    public QueryModule getTopLevelModule() {
        return topModule;
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
     * Get the StaticQueryContext object containing options set up by the user
     * @return the user-created StaticQueryContext object
     */

    public StaticQueryContext getUserQueryContext() {
        return userQueryContext;
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
     * Set the namespace for a library module.
     * <p/>
     * This method is for internal use only.
     * @param uri the module namespace URI of the library module
     */

    public void setModuleNamespace(String uri) {
        moduleNamespace = uri;
        moduleNamespaceURICode = getNamePool().getCodeForURI(uri);
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
     * @param uri the location URI
     */

    public void setLocationURI(URI uri) {
        locationURI = uri;
    }

    /**
     * Get the location URI for a module
     * @return the location URI
     */

    public URI getLocationURI() {
        return locationURI;
    }

    /**
     * Get the System ID for a module
     * @return the location URI
     */

    public String getSystemId() {
        return (locationURI == null ? null : locationURI.toString());
    }

    /**
     * Set the base URI for a module
     * @param uri the base URI
     */

    public void setBaseURI(String uri) {
        baseURI = uri;
    }

    /**
     * Get the base URI for a module
     * @return the base URI
     */

    public String getBaseURI() {
        return baseURI;
    }


    /**
     * Get the stack frame map for global variables.
     * <p/>
     * This method is intended for internal use.
     * @return the stack frame map (a SlotManager) for global variables.
     */

    public SlotManager getGlobalStackFrameMap() {
        return executable.getGlobalVariableMap();
    }

    /**
     * Declare a global variable. A variable must normally be declared before an expression referring
     * to it is compiled, but there are exceptions where a set of modules in the same namespace
     * import each other cyclically. Global variables are normally declared in the Query Prolog, but
     * they can also be predeclared using the Java API. All global variables are held in the QueryModule
     * for the main module. The fact that a global variable is present therefore does not mean that it
     * is visible: there are two additional conditions (a) the module namespace must be imported into the
     * module where the reference appears, and (b) the declaration must not be in the same module and textually
     * after the reference.
     *
     * <p>Note that the same VariableDeclaration object cannot be used with more than one query.  This is because
     * the VariableDeclaration is modified internally to hold a list of references to all the places where
     * the variable is used.</p>
     *
     * @param var the Variable declaration being declared
     */

    public void declareVariable(VariableDeclaration var) throws XPathException {
        StructuredQName key = var.getVariableQName();
        if (variables.get(key) != null) {
            VariableDeclaration old = (VariableDeclaration)variables.get(key);
            if (old == var) {
                // do nothing
            } else {
                String oldloc = "";
                if (old instanceof GlobalVariableDefinition && var instanceof GlobalVariableDefinition) {
                    oldloc = " (see line " + ((GlobalVariableDefinition)old).getLineNumber();
                    String oldSysId = ((GlobalVariableDefinition)old).getSystemId();
                    if (oldSysId != null &&
                            !oldSysId.equals(((GlobalVariableDefinition)var).getSystemId())) {
                        oldloc += " in module ((GlobalVariableDefinition)old).getSystemId()";
                    }
                    oldloc += ')';
                }
                XPathException err = new XPathException("Duplicate definition of global variable "
                        + var.getVariableQName().getDisplayName()
                        + oldloc);
                err.setErrorCode("XQST0049");
                err.setIsStaticError(true);
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

        final HashMap libVars = getTopLevelModule().libraryVariables;
        GlobalVariableDefinition old = (GlobalVariableDefinition)libVars.get(key);
        if (old == null || old == var) {
            // do nothing
        } else {
            XPathException err = new XPathException("Duplicate definition of global variable "
                    + var.getVariableQName().getDisplayName()
                    + " (see line " + old.getLineNumber() + " in module " + old.getSystemId() + ')');
            err.setErrorCode("XQST0049");
            err.setIsStaticError(true);
            if (var instanceof GlobalVariableDefinition) {
                ExpressionLocation loc = new ExpressionLocation();
                loc.setLineNumber(((GlobalVariableDefinition)var).getLineNumber());
                loc.setSystemId(((GlobalVariableDefinition)var).getSystemId());
                err.setLocator(loc);
            }
            throw err;
        }

        if (!isMainModule()) {
            libVars.put(key, var);
        }
    }

    /**
     * Fixup all references to global variables.
     * <p/>
     * This method is for internal use by the Query Parser only.
     * @param globalVariableMap a SlotManager that holds details of the assignment of slots to global variables.
     * @return a list containing the global variable definitions.
     */

    public List fixupGlobalVariables(SlotManager globalVariableMap) throws XPathException {
        List varDefinitions = new ArrayList(20);
        Iterator[] iters = {variables.values().iterator(), libraryVariables.values().iterator()};
        for (int i=0; i<2; i++) {
            while (iters[i].hasNext()) {
                GlobalVariableDefinition var = (GlobalVariableDefinition)iters[i].next();
                int slot = globalVariableMap.allocateSlotNumber(var.getVariableQName());
                GlobalVariable gv = var.getCompiledVariable();
                if (gv == null) {
                    var.compile(getExecutable(), slot);
                }
                if (!varDefinitions.contains(var)) {
                    varDefinitions.add(var);
                }
            }
        }
        return varDefinitions;
    }

    /**
     * Look for module cycles. This is a restriction introduced in the PR specification because of
     * difficulties in defining the formal semantics.
     *
     * <p>[Definition: A module M1 directly depends on another module M2 (different from M1) if a
     *  variable or function declared in M1 depends on a variable or function declared in M2.]
     * It is a static error [err:XQST0093] to import a module M1 if there exists a sequence
     * of modules M1 ... Mi ... M1 such that each module directly depends on the next module
     * in the sequence (informally, if M1 depends on itself through some chain of module dependencies.)</p>
     *
     * @param referees a Stack containing the chain of module import references leading to this
     * module
     * @param lineNumber used for diagnostics
     */

    public void lookForModuleCycles(Stack referees, int lineNumber) throws XPathException {
        if (referees.contains(this)) {
            int s = referees.indexOf(this);
            referees.push(this);
            String message = "Circular dependency between modules. ";
            for (int i = s; i < referees.size() - 1; i++) {
                QueryModule next = (QueryModule)referees.get(i + 1);
                if (i == s) {
                    message += "Module " + getSystemId() + " references module " + next.getSystemId();
                } else {
                    message += ", which references module " + next.getSystemId();
                }
            }
            message += '.';
            XPathException err = new XPathException(message);
            err.setErrorCode("XQST0093");
            err.setIsStaticError(true);
            ExpressionLocation loc = new ExpressionLocation();
            loc.setSystemId(getSystemId());
            loc.setLineNumber(lineNumber);
            err.setLocator(loc);
            throw err;
        } else {
            referees.push(this);
            Iterator viter = getModuleVariables();
            while (viter.hasNext()) {
                GlobalVariableDefinition gv = (GlobalVariableDefinition)viter.next();
                List list = new ArrayList(10);
                Expression select = gv.getCompiledVariable().getSelectExpression();
                if (select != null) {
                    ExpressionTool.gatherReferencedVariables(select, list);
                    for (int i=0; i<list.size(); i++) {
                        Binding b = (Binding)list.get(i);
                        if (b instanceof GlobalVariable) {
                            String uri = ((GlobalVariable)b).getSystemId();
                            StructuredQName qName = b.getVariableQName();
                            boolean synthetic = NamespaceConstant.SAXON.equals(qName.getNamespaceURI()) && "gg".equals(qName.getPrefix());
                            if (!synthetic && !uri.equals(getSystemId())) {
                                QueryModule sqc = executable.getQueryModuleWithSystemId(uri, topModule);
                                sqc.lookForModuleCycles(referees, ((GlobalVariable)b).getLineNumber());
                            }
                        }
                    }
                    list.clear();
                    ExpressionTool.gatherCalledFunctions(select, list);
                    for (int i=0; i<list.size(); i++) {
                        UserFunction f = (UserFunction)list.get(i);
                        String uri = f.getSystemId();
                        if (!uri.equals(getSystemId())) {
                            QueryModule sqc = executable.getQueryModuleWithSystemId(uri, topModule);
                            sqc.lookForModuleCycles(referees, f.getLineNumber());
                        }
                    }
                }
            }
            Iterator fiter = getLocalFunctionLibrary().getFunctionDefinitions();
            while (fiter.hasNext()) {
                XQueryFunction gf = (XQueryFunction)fiter.next();
                List list = new ArrayList(10);
                Expression body = gf.getUserFunction().getBody();
                if (body != null) {
                    ExpressionTool.gatherReferencedVariables(body, list);
                    for (int i=0; i<list.size(); i++) {
                        Binding b = (Binding)list.get(i);
                        if (b instanceof GlobalVariable) {
                            String uri = ((GlobalVariable)b).getSystemId();
                            StructuredQName qName = b.getVariableQName();
                            boolean synthetic = NamespaceConstant.SAXON.equals(qName.getNamespaceURI()) && "gg".equals(qName.getPrefix());
                            if (!synthetic && !uri.equals(getSystemId())) {
                                QueryModule sqc = executable.getQueryModuleWithSystemId(uri, topModule);
                                sqc.lookForModuleCycles(referees, ((GlobalVariable)b).getLineNumber());
                            }
                        }
                    }
                    list.clear();
                    ExpressionTool.gatherCalledFunctions(body, list);
                    for (int i=0; i<list.size(); i++) {
                        UserFunction f = (UserFunction)list.get(i);
                        String uri = f.getSystemId();
                        if (!uri.equals(getSystemId())) {
                            QueryModule sqc = executable.getQueryModuleWithSystemId(uri, topModule);
                            sqc.lookForModuleCycles(referees, f.getLineNumber());
                        }
                    }
                }
            }
            referees.pop();
        }
    }

    /**
     * Get global variables declared in this module
     * @return an Iterator whose items are GlobalVariableDefinition objects
     */

    public Iterator getModuleVariables() {
        return variables.values().iterator();
    }

    /**
     * Check for circular definitions of global variables.
     * <p>This method is intended for internal use</p>
     * @param compiledVars a list of {@link GlobalVariableDefinition} objects to be checked
     */

    public void checkForCircularities(List compiledVars, XQueryFunctionLibrary globalFunctionLibrary) throws XPathException {
        Iterator iter = compiledVars.iterator();
        Stack stack = null;
        while (iter.hasNext()) {if (stack == null) {stack = new Stack();}
            GlobalVariableDefinition gvd = (GlobalVariableDefinition)iter.next();
            GlobalVariable gv = gvd.getCompiledVariable();
            gv.lookForCycles(stack, globalFunctionLibrary);
        }
    }


    /**
     * Perform type checking on global variables.
     * <p>This method is intended for internal use</p>
     * @param compiledVars a list of {@link GlobalVariableDefinition} objects to be checked
     */

    public void typeCheckGlobalVariables(List compiledVars) throws XPathException {
        ExpressionVisitor visitor = ExpressionVisitor.make(this);
        visitor.setExecutable(getExecutable());
        Iterator iter = compiledVars.iterator();
        //Stack stack = new Stack();
        while (iter.hasNext()) {
            GlobalVariableDefinition gvd = (GlobalVariableDefinition)iter.next();
            //GlobalVariable gv = gvd.getCompiledVariable();
            //gv.lookForCycles(stack);
            gvd.typeCheck(visitor);
        }
    }

    /**
     * Bind a variable used in a query to the expression in which it is declared.
     * <p/>
     * This method is provided for use by the XQuery parser, and it should not be called by the user of
     * the API, or overridden, unless variables are to be declared using a mechanism other than the
     * declareVariable method of this class.
     * @param qName the name of the variable to be bound
     * @return a VariableReference object representing a reference to a variable on the abstract syntac rtee of
     * the query.
     */

    public VariableReference bindVariable(StructuredQName qName) throws XPathException {
        VariableDeclaration var = (VariableDeclaration)variables.get(qName);
        if (var == null) {
            String uri = qName.getNamespaceURI();
            if (importsNamespace(uri)) {
                QueryModule main = getTopLevelModule();
                var = (VariableDeclaration)main.libraryVariables.get(qName);
                if (var == null) {
                    // If the namespace has been imported there's the possibility that
                    // the variable declaration hasn't yet been read, because of the limited provision
                    // for cyclic imports
                    UndeclaredVariable uvar = new UndeclaredVariable();
                    uvar.setVariableQName(qName);
                    VariableReference ref = new VariableReference();
                    uvar.registerReference(ref);
                    undeclaredVariables.put(qName, uvar);
                    return ref;
                } else {
                    GlobalVariableDefinition gvar = ((GlobalVariableDefinition)var);
                    checkImportedType(gvar.getRequiredType(), gvar);
                }
            } else {
                // If the namespace hasn't been imported then we might as well throw the error right away
                XPathException err = new XPathException("Unresolved reference to variable");
                err.setErrorCode("XPST0008");
                err.setIsStaticError(true);
                // the message isn't used...
                throw err;
            }
        }
        VariableReference vref = new VariableReference();
        var.registerReference(vref);
        return vref;
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
     * @see org.orbeon.saxon.functions.FunctionLibraryList
     */

    public void setFunctionLibraryList(FunctionLibraryList functionLibrary) {
        functionLibraryList = functionLibrary;
    }

    /**
     * Get the function library containing all the in-scope functions available in this static
     * context (that is, the functions available in this query module).
     * <p/>
     * This method is provided for use by advanced applications.
     * The details of the interface are subject to change.
     *
     * @return the FunctionLibrary used. For XQuery, this will always be a FunctionLibraryList.
     * @see org.orbeon.saxon.functions.FunctionLibraryList
     */

    public FunctionLibrary getFunctionLibrary() {
        return functionLibraryList;
    }

    /**
     * Get the functions declared locally within this module
     * @return a FunctionLibrary object containing the function declarations
     */

    public XQueryFunctionLibrary getLocalFunctionLibrary() {
        return (XQueryFunctionLibrary)functionLibraryList.get(localFunctionLibraryNr);
    }

    /**
     * Register a user-defined XQuery function.
     * <p/>
     * This method is intended for internal use only.
     * @param function the function being declared
     */

    public void declareFunction(XQueryFunction function) throws XPathException {
        Configuration config = getConfiguration();
        if (function.getNumberOfArguments() == 1) {
            StructuredQName name = function.getFunctionName();
            int fingerprint = config.getNamePool().getFingerprint(name.getNamespaceURI(), name.getLocalName());
            if (fingerprint != -1) {
                SchemaType t = config.getSchemaType(fingerprint);
                if (t != null && t.isAtomicType()) {
                    XPathException err = new XPathException("Function name " + function.getDisplayName() +
                            " clashes with the name of the constructor function for an atomic type");
                    err.setErrorCode("XQST0034");
                    err.setIsStaticError(true);
                    throw err;
                }
            }
        }
        XQueryFunctionLibrary local = getLocalFunctionLibrary();
        local.declareFunction(function);
        QueryModule main = getTopLevelModule();
        main.globalFunctionLibrary.declareFunction(function);
    }

    /**
     * Bind function calls that could not be bound when first encountered. These
     * will either be forwards references to functions declared later in the same query module,
     * or in modules that are being imported recursively, or errors.
     * <p/>
     * This method is for internal use only.
     *
     * @throws org.orbeon.saxon.trans.XPathException if a function call refers to a function that has
     *                                        not been declared
     */

    public void bindUnboundFunctionCalls() throws XPathException {
        UnboundFunctionLibrary lib = (UnboundFunctionLibrary)functionLibraryList.get(unboundFunctionLibraryNr);
        lib.bindUnboundFunctionCalls(functionLibraryList, getConfiguration());
    }

    /**
     * Fixup all references to global functions. This method is called
     * on completion of query parsing. Each XQueryFunction is required to
     * bind all references to that function to the object representing the run-time
     * executable code of the function.
     * <p/>
     * This method is for internal use only. It is called only on the StaticQueryContext for the main
     * query body (not for library modules).
     */

    public void fixupGlobalFunctions() throws XPathException {
        globalFunctionLibrary.fixupGlobalFunctions(this);
    }

    /**
     * Optimize the body of all global functions.
     * <p/>
     * This method is for internal use only. It is called only on the StaticQueryContext for the main
     * query body (not for library modules).
     */

    public void optimizeGlobalFunctions() throws XPathException {
        globalFunctionLibrary.optimizeGlobalFunctions();
    }


    /**
     * Output "explain" information about each declared function.
     * <p/>
     * This method is intended primarily for internal use.
     * @param out the expression presenter used to display the output
     */

    public void explainGlobalFunctions(ExpressionPresenter out) {
        globalFunctionLibrary.explainGlobalFunctions(out);
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
     * @return the user-defined function
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
        for (Iterator iter = undeclaredVariables.values().iterator(); iter.hasNext();) {
            UndeclaredVariable uv = (UndeclaredVariable)iter.next();
            StructuredQName qName = uv.getVariableQName();
            VariableDeclaration var = (VariableDeclaration)variables.get(qName);
            if (var == null) {
                String uri = qName.getNamespaceURI();
                if (importsNamespace(uri)) {
                    QueryModule main = getTopLevelModule();
                    var = (VariableDeclaration)main.libraryVariables.get(qName);
                }
            }
            if (var == null) {
                XPathException err = new XPathException("Unresolved reference to variable $" +
                        uv.getVariableQName().getDisplayName());
                err.setErrorCode("XPST0008");
                err.setIsStaticError(true);
                throw err;
            } else {
                GlobalVariableDefinition gvar = ((GlobalVariableDefinition)var);
                checkImportedType(gvar.getRequiredType(), gvar);
                uv.transferReferences(var);
            }
        }
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
     * @param baseURI The base URI against which the locationURIs are to be absolutized
     * @param locationURIs a list of strings containing the absolutized URIs of the "location hints" supplied
     * for this schema
     * @since 8.4
     */

    public void addImportedSchema(String targetNamespace, String baseURI, List locationURIs) {
        if (importedSchemata == null) {
            importedSchemata = new HashSet(5);
        }
        importedSchemata.add(targetNamespace);
        HashMap loadedSchemata = getTopLevelModule().loadedSchemata;
        if (loadedSchemata == null) {
            loadedSchemata = new HashMap(5);
            getTopLevelModule().loadedSchemata = loadedSchemata;
        }
        HashSet entries = (HashSet)loadedSchemata.get(targetNamespace);
        if (entries == null) {
            entries = new HashSet(locationURIs.size());
            loadedSchemata.put(targetNamespace, entries);
        }
        Platform platform = Configuration.getPlatform();
        for (Iterator iter = locationURIs.iterator(); iter.hasNext();) {
            String relative = (String)iter.next();
            try {
                URI abs = platform.makeAbsolute(relative, baseURI);
                entries.add(abs.toString());
            } catch (URISyntaxException e) {
                // ignore the URI if it's not valid
            }
        }
    }

    /**
     * For the top-level module only, get all the schema modules imported anywhere in the query.
     * @return a Map whose key is the target namespace of a set of schema documents, and whose
     * value is a Set containing the absolutized location URIs ("hints") of the locations from
     * which those schema documents were loaded, as strings.
     */

    public Map getAllImportedSchemata() {
        return loadedSchemata;
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
        return importedSchemata != null && importedSchemata.contains(namespace);
    }

    /**
     * Get the set of imported schemas
     *
     * @return a Set, the set of URIs representing the names of imported schemas
     */

    public Set getImportedSchemaNamespaces() {
        if (importedSchemata == null) {
            return Collections.EMPTY_SET;
        } else {
            return importedSchemata;
        }
    }

    /**
     * Report a fatal error in the query (via the registered ErrorListener)
     * @param err the error to be signalled
     */

    public void reportFatalError(XPathException err) {
        if (!err.hasBeenReported()) {
            try {
                if (userQueryContext == null) {
                    config.getErrorListener().fatalError(err);
                } else {
                    userQueryContext.getErrorListener().fatalError(err);
                }
            } catch (TransformerException e) {
                // ignore secondary errors
            }
            err.setHasBeenReported();
        }
    }

    /**
     * Check that all the types used in the signature of an imported function
     * are available in the module of the caller of the function
     * @param fd the declaration of the imported function
     * @throws XPathException if an error is found
     */

    public void checkImportedFunctionSignature(XQueryFunction fd) throws XPathException {
        checkImportedType(fd.getResultType(), fd);
        for (int a=0; a<fd.getNumberOfArguments(); a++) {
            SequenceType argType = fd.getArgumentTypes()[a];
            checkImportedType(argType, fd);
        }
    }

    /**
     * Check that a SequenceType used in the definition of an imported variable or function
     * is available in the importing module
     * @param importedType the type that is to be checked
     * @param declaration the containing query or function definition
     * @throws org.orbeon.saxon.trans.XPathException if an error is fonnd.
     */

    public void checkImportedType(SequenceType importedType, Declaration declaration)
            throws XPathException {
        ItemType type = importedType.getPrimaryType();
        if (type instanceof AnyItemType) {
            return;
        }
        if (type.isAtomicType()) {
            int f = ((AtomicType)type).getFingerprint();
            checkSchemaNamespaceImported(f, declaration);
        } else if (type instanceof ContentTypeTest) {
            SchemaType annotation = ((ContentTypeTest)type).getSchemaType();
            int f = annotation.getFingerprint();
            checkSchemaNamespaceImported(f, declaration);
        } else if (type instanceof CombinedNodeTest) {
            NodeTest[] tests = ((CombinedNodeTest)type).getComponentNodeTests();
            for (int i=0; i<tests.length; i++) {
                SequenceType st = SequenceType.makeSequenceType(tests[i], StaticProperty.EXACTLY_ONE);
                checkImportedType(st, declaration);
            }
        }
    }

    /**
     * Construct a dynamic context for early evaluation of constant subexpressions
     * @return a dynamic context object
     */

    public XPathContext makeEarlyEvaluationContext() {
        return new EarlyEvaluationContext(getConfiguration(), userQueryContext.getCollationMap());
    }


    /**
     * Get a named collation.
     *
     * @param name The name of the required collation. Supply null to get the default collation.
     * @return the collation; or null if the required collation is not found.
     */

    public StringCollator getCollation(String name) {
        return userQueryContext.getCollation(name);
    }

    /**
     * Get the name of the default collation.
     *
     * @return the name of the default collation; or the name of the codepoint collation
     *         if no default collation has been defined
     */

    public String getDefaultCollationName() {
        return defaultCollationName;
    }

    /**
     * Set the name of the default collation
     * @param collation the URI of the default collation
     */

    public void setDefaultCollationName(String collation) {
        defaultCollationName = collation;
    }

    /**
     * Register a namespace that is explicitly declared in the prolog of the query module.
     *
     * @param prefix   The namespace prefix. Must not be null.
     * @param uri      The namespace URI. Must not be null. The value "" (zero-length string) is used
     *                 to undeclare a namespace; it is not an error if there is no existing binding for
     *                 the namespace prefix.
     */

    public void declarePrologNamespace(String prefix, String uri) throws XPathException {
        if (prefix == null) {
            throw new NullPointerException("Null prefix supplied to declarePrologNamespace()");
        }
        if (uri == null) {
            throw new NullPointerException("Null namespace URI supplied to declarePrologNamespace()");
        }
        if ((prefix.equals("xml") != uri.equals(NamespaceConstant.XML))) {
            XPathException err = new XPathException("Invalid declaration of the XML namespace");
            err.setErrorCode("XQST0070");
            err.setIsStaticError(true);
            throw err;
        }
        if (explicitPrologNamespaces.get(prefix) != null) {
            XPathException err = new XPathException("Duplicate declaration of namespace prefix \"" + prefix + '"');
            err.setErrorCode("XQST0033");
            err.setIsStaticError(true);
            throw err;
        } else {
            explicitPrologNamespaces.put(prefix, uri);
            getNamePool().allocateNamespaceCode(prefix, uri);
        }
    }

    /**
      * Declare an active namespace, that is, a namespace which as well as affecting the static
      * context of the query, will also be copied to the result tree when element constructors
      * are evaluated. When searching for a prefix-URI binding, active namespaces are searched
      * first, then passive namespaces. Active namespaces are later undeclared (in reverse sequence)
      * using {@link #undeclareNamespace()}.
      * <p/>
      * This method is intended for internal use only.
      * @param prefix the namespace prefix
      * @param uri the namespace URI
      */

     public void declareActiveNamespace(String prefix, String uri) {
         if (prefix == null) {
             throw new NullPointerException("Null prefix supplied to declareActiveNamespace()");
         }
         if (uri == null) {
             throw new NullPointerException("Null namespace URI supplied to declareActiveNamespace()");
         }

         int nscode = getNamePool().allocateNamespaceCode(prefix, uri);
         ActiveNamespace entry = new ActiveNamespace();
         entry.prefix = prefix;
         entry.uri = uri;
         entry.code = nscode;
         activeNamespaces.push(entry);

//         if (prefix.length() == 0) {
//             defaultElementNamespace = uri;
//         }

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
         activeNamespaces.pop();
     }


    /**
     * Get the URI for a prefix.
     * This method is used by the XQuery parser to resolve namespace prefixes.
     * <p/>
     * This method is intended primarily for internal use.
     *
     * @param prefix The prefix
     * @return the corresponding namespace URI
     * @throws org.orbeon.saxon.trans.XPathException (with error code XPST0081)
     *          if the prefix has not been declared
     */

    public String getURIForPrefix(String prefix) throws XPathException {
        String uri = checkURIForPrefix(prefix);
        if (uri == null) {
            XPathException err = new XPathException("Prefix " + prefix + " has not been declared");
            err.setErrorCode("XPST0081");
            err.setIsStaticError(true);
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
     * @param prefix The prefix. Supply "" to obtain the default namespace for elements and types.
     * @return the corresponding namespace URI, or null if the prefix has not
     *         been declared. If the prefix is "" and the default namespace is the non-namespace,
     *         return "".
     */

    public String checkURIForPrefix(String prefix) {
        // Search the active namespaces first, then the passive ones.
        if (activeNamespaces != null) {
            for (int i = activeNamespaces.size() - 1; i >= 0; i--) {
                if (((ActiveNamespace)activeNamespaces.get(i)).prefix.equals(prefix)) {
                    return ((ActiveNamespace)activeNamespaces.get(i)).uri;
                }
            }
        }
        if (prefix.length() == 0) {
            return defaultElementNamespace;
        }
        String uri = (String)explicitPrologNamespaces.get(prefix);
        if (uri != null) {
            // A zero-length URI means the prefix was undeclared in the prolog, and we mustn't look elsewhere
            return (uri.length() == 0 ? null : uri);
        }

        if (userQueryContext != null) {
            uri = userQueryContext.getNamespaceForPrefix(prefix);
            if (uri != null) {
                return uri;
            }
            NamespaceResolver externalResolver = userQueryContext.getExternalNamespaceResolver();
            if (externalResolver != null) {
                return externalResolver.getURIForPrefix(prefix, true);
            }
        }
        return null;
    }


    /**
     * Get the default XPath namespace for elements and types. Note that this is not necessarily
     * the default namespace declared in the query prolog; within an expression, it may change in response
     * to namespace declarations on element constructors.
     *
     * @return the default namespace, or NamespaceConstant.NULL for the non-namespace
     */

    public String getDefaultElementNamespace() {
        return checkURIForPrefix("");
    }

    /**
     * Set the default element namespace as declared in the query prolog
     * @param uri the default namespace for elements and types
     */

    public void setDefaultElementNamespace(String uri) {
        defaultElementNamespace = uri;
    }

    /**
     * Get the default function namespace
     *
     * @return the default namespace for function names
     */

    public String getDefaultFunctionNamespace() {
        return defaultFunctionNamespace;
    }

    /**
     * Set the default function namespace
     * @param uri the default namespace for functions
     */

    public void setDefaultFunctionNamespace(String uri) {
        defaultFunctionNamespace = uri;
    }

    /**
     * Set the revalidation mode. This is used only if XQuery Updates are in use, in other cases
     * the value is ignored.
     * @param mode the revalidation mode. This must be one of {@link Validation#STRICT},
     * {@link Validation#LAX}, or {@link Validation#SKIP}
     */

    public void setRevalidationMode(int mode) {
        if (mode==Validation.STRICT || mode==Validation.LAX || mode==Validation.SKIP) {
            revalidationMode = mode;
        } else {
            throw new IllegalArgumentException("Invalid mode " + mode);
        }
    }

     /**
     * Get the revalidation mode. This is used only if XQuery Updates are in use, in other cases
     * the value is ignored.
     * @return the revalidation mode. This will be one of {@link Validation#STRICT},
     * {@link Validation#LAX}, or {@link Validation#SKIP}
     */

    public int getRevalidationMode() {
         return revalidationMode;
     }

    /**
     * Get an array containing the namespace codes of all active namespaces.
     * <p/>
     * This method is for internal use only.
     * @return an array of namespace codes. A namespace code is an int that holds a prefix code in the
     * top half and a uri code in the bottom half.
     */

    public int[] getActiveNamespaceCodes() {
        if (activeNamespaces == null) {
            return IntArraySet.EMPTY_INT_ARRAY;
        }
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
     * to save the namespace context for use at run-time. Note that unlike other implementations of
     * StaticContext, the state of the QueryModule changes as the query is parsed, with different namespaces
     * in scope at different times. It's therefore necessary to compute the whole namespace context each time.
     * <p/>
     * This method is for internal use only.
     */

    public NamespaceResolver getNamespaceResolver() {
        List externalNamespaceCodes = null;
        NamespaceResolver externalResolver = userQueryContext.getExternalNamespaceResolver();
        if (externalResolver != null) {
            externalNamespaceCodes = new ArrayList();
            Iterator iter = externalResolver.iteratePrefixes();
            while (iter.hasNext()) {
                String prefix = (String)iter.next();
                String uri = externalResolver.getURIForPrefix(prefix, true);
                int nscode = getNamePool().allocateNamespaceCode(prefix, uri);
                externalNamespaceCodes.add(new Integer(nscode));
            }
        }
        HashMap userDeclaredNamespaces = userQueryContext.getUserDeclaredNamespaces();
        int[] active = getActiveNamespaceCodes();
        int[] nscodes = new int[explicitPrologNamespaces.size() + userDeclaredNamespaces.size() + active.length +
                (externalNamespaceCodes == null ? 0 : externalNamespaceCodes.size())];

        int used = 0;
        NamePool namePool = getNamePool();
        for (Iterator iter = userDeclaredNamespaces.keySet().iterator(); iter.hasNext();) {
            String prefix = (String)iter.next();
            String uri = (String)userDeclaredNamespaces.get(prefix);
            nscodes[used++] = namePool.getNamespaceCode(prefix, uri);
        }
        for (Iterator iter = explicitPrologNamespaces.keySet().iterator(); iter.hasNext();) {
            String prefix = (String)iter.next();
            String uri = (String)explicitPrologNamespaces.get(prefix);
            nscodes[used++] = namePool.getNamespaceCode(prefix, uri);
        }
        for (int a = 0; a < active.length; a++) {
            nscodes[used++] = active[a];
        }

        if (externalNamespaceCodes != null) {
            for (int a = 0; a < externalNamespaceCodes.size(); a++) {
                nscodes[used++] = ((Integer)externalNamespaceCodes.get(a)).intValue();
            }
        }

        return new SavedNamespaceContext(nscodes, namePool);
    }

    /**
     * Issue a compile-time warning. This method is used during XQuery expression compilation to
     * output warning conditions.
     * <p/>
     * This method is intended for internal use only.
     */

    public void issueWarning(String s, SourceLocator locator) {
        XPathException err = new XPathException(s);
        err.setLocator(locator);
        try {
            getConfiguration().getErrorListener().warning(err);
        } catch (TransformerException e) {
            // ignore any error thrown
        }
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
     * Determine whether Backwards Compatible Mode is used
     *
     * @return false; XPath 1.0 compatibility mode is not supported in XQuery
     * @since 8.4
     */

    public boolean isInBackwardsCompatibleMode() {
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

    public boolean isAllowedBuiltInType(BuiltInAtomicType type) {
        return true;
    }

    /**
     * Set whether the query is allowed to be updating
     * @param updating true if the query may use the XQuery Update facility (requires Saxon-SA)
     * @since 9.1
     */

    public void setUpdating(boolean updating) {
        isUpdating = updating;
    }

    /**
     * Ask whether the query is allowed to be updating
     * @return true if the query is allowed to use the XQuery Update facility
     * @since 9.1
     */

    public boolean isUpdating() {
        return isUpdating;
    }

    /**
     * Check that the namespace of a given name is the namespace of an imported schema
     * @param fingerprint the fingerprint of the "given name"
     * @param declaration the declaration of the variable or function that has this given name
     * @throws org.orbeon.saxon.trans.XPathException (error XQST0036) if the namespace is not present in a schema
     * imported by the importing query module
     */

    private void checkSchemaNamespaceImported(int fingerprint, Declaration declaration)
            throws XPathException {
        String uri = getNamePool().getURI(fingerprint);
        if (!uri.equals(NamespaceConstant.SCHEMA) && !uri.equals(NamespaceConstant.ANONYMOUS) &&
                !uri.equals(NamespaceConstant.JAVA_TYPE) &&  !isImportedSchema(uri)) {
            String msg = "Schema component " + getNamePool().getDisplayName(fingerprint) + " used in ";
            if (declaration instanceof GlobalVariableDefinition) {
                msg += "declaration of imported variable " +
                        ((GlobalVariableDefinition)declaration).getVariableQName().getDisplayName();
            } else {
                msg += "signature of imported function " +
                        ((XQueryFunction)declaration).getDisplayName();
            }
            msg += " is not declared in any schema imported by ";
            String module = getModuleNamespace();
            if (module == null) {
                msg += "the main query module";
            } else {
                msg += "query module " + module;
            }
            XPathException err = new XPathException(msg);
            err.setErrorCode("XQST0036");
            err.setIsStaticError(true);
            err.setLocator(declaration);
            throw err;
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
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//

