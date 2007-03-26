package org.orbeon.saxon.instruct;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.expr.CollationMap;
import org.orbeon.saxon.event.Stripper;
import org.orbeon.saxon.functions.FunctionLibrary;
import org.orbeon.saxon.om.NamespaceConstant;
import org.orbeon.saxon.query.StaticQueryContext;
import org.orbeon.saxon.sort.CodepointCollator;
import org.orbeon.saxon.sort.IntHashMap;
import org.orbeon.saxon.sort.IntHashSet;
import org.orbeon.saxon.sort.IntIterator;
import org.orbeon.saxon.trans.*;

import java.io.Serializable;
import java.util.*;

/**
 * A compiled stylesheet or a query in executable form.
 * Note that the original stylesheet tree is not retained.
 */

public class Executable implements Serializable {

    // the Configuration options
    private transient Configuration config;

    // definitions of strip/preserve space action
    private Mode stripperRules;

    // boolean indicating whether any whitespace is stripped
    private boolean stripsWhitespace;

    // definitions of template rules
    private RuleManager ruleManager;

    // definitions of keys
    private KeyManager keyManager;

    // definitions of decimal formats
    private DecimalFormatManager decimalFormatManager;

    // the map of slots used for global variables and params
    private SlotManager globalVariableMap;

    // Index of global variables and parameters, by fingerprint
    // The key is the variable name fingerprint
    // The value is the compiled GlobalVariable object.
    private IntHashMap compiledGlobalVariables = new IntHashMap(32);

    // default collating sequence
    private String defaultCollationName;

    // default output properties (for the unnamed output format)
    private Properties defaultOutputProperties;

    // index of named templates.
    private IntHashMap namedTemplateTable = new IntHashMap(32);

    // count of the maximum number of local variables in the match pattern of any template rule
    private int largestPatternStackFrame = 0;

    // table of named collations defined in the stylesheet/query
    private CollationMap collationTable;

    // table of character maps
    private IntHashMap characterMapIndex;

    // location map for expressions in this executable
    private LocationMap locationMap;

    // hash table of query library modules
    private HashMap queryLibraryModules;

    // flag to indicate that source documents are to have their type annotations stripped
    private boolean stripsInputTypeAnnotations;

    // list of functions available in the static context
    private FunctionLibrary functionLibrary;

    // flag to indicate whether the principal language is for example XSLT or XQuery
    private int hostLanguage = Configuration.XSLT;

    // a list of required parameters, identified by the fingerprint of their names
    private IntHashSet requiredParams = null;

    // hash table of named (and unnamed) output declarations. This is assembled only
    // if there is a need for it: that is, if there is a call on xsl:result-document
    // with a format attribute computed at run-time
    private IntHashMap outputDeclarations = null;

    // a string explaining why this Executable can't be compiled, or null if it can
    private String reasonUnableToCompile = null;

    public Executable() {

    }

    /**
     * Set the configuration
     */

    public void setConfiguration(Configuration config) {
        this.config = config;
    }

    /**
     * Get the configuration
     */

    public Configuration getConfiguration() {
        return config;
    }

    /**
     * Set the host language
     */

    public void setHostLanguage(int language) {
        hostLanguage = language;
    }

    /**
     * Get the host language
     *
     * @return a value identifying the host language: {@link Configuration#XQUERY} or {@link Configuration#XSLT}
     *         or {@link Configuration#JAVA_APPLICATION}
     */

    public int getHostLanguage() {
        return hostLanguage;
    }

    /**
     * Set the RuleManager that handles template rules
     *
     * @param rm the RuleManager containing details of all the template rules
     */

    public void setRuleManager(RuleManager rm) {
        ruleManager = rm;
    }

    /**
     * Get the RuleManager which handles template rules
     *
     * @return the RuleManager registered with setRuleManager
     */

    public RuleManager getRuleManager() {
        return ruleManager;
    }

    /**
     * Get the named template table. Provided for use by tools allowing selection
     * of a transformation entry point from a supplied list.
     *
     * @return a hash table containing entries that map the names of named
     *         templates (in the form of namePool fingerprints) to the Template objects representing
     *         the compiled xsl:template element in the stylesheet.
     */

    public IntHashMap getNamedTemplateTable() {
        if (namedTemplateTable == null) {
            namedTemplateTable = new IntHashMap(32);
        }
        return namedTemplateTable;
    }

    /**
     * Get the named template with a given name.
     *
     * @param fingerprint The namepool fingerprint of the template name
     * @return The template (of highest import precedence) with this name if there is one;
     *         null if none is found.
     */

    public Template getNamedTemplate(int fingerprint) {
        return (Template)namedTemplateTable.get(fingerprint);
    }

    /**
     * Register the named template with a given name
     */

    public void putNamedTemplate(int fingerprint, Template template) {
        namedTemplateTable.put(fingerprint, template);
    }

    /**
     * Get the library containing all the in-scope functions in the static context
     *
     * @return the function libary
     */

    public FunctionLibrary getFunctionLibrary() {
        return functionLibrary;
    }

    /**
     * Set the library containing all the in-scope functions in the static context
     *
     * @param functionLibrary the function libary
     */

    public void setFunctionLibrary(FunctionLibrary functionLibrary) {
        //System.err.println("***" + this + " setFunctionLib to " + functionLibrary);
        this.functionLibrary = functionLibrary;
    }

    /**
     * Set the index of named character maps
     *
     * @param cmi a hash table that maps the names of character maps
     *            to the HashMap objects representing the character maps
     */

    public void setCharacterMapIndex(IntHashMap cmi) {
        characterMapIndex = cmi;
    }

    /**
     * Get the index of named character maps
     *
     * @return the hash table that maps the names of character maps
     *         to the HashMap objects representing the character maps
     */

    public IntHashMap getCharacterMapIndex() {
        if (characterMapIndex == null) {
            characterMapIndex = new IntHashMap(10);
        }
        return characterMapIndex;
    }

    /**
     * Set the rules determining which nodes are to be stripped from the tree
     *
     * @param rules a Mode object containing the whitespace stripping rules. A Mode
     *              is generally a collection of template rules, but it is reused here to represent
     *              a collection of stripping rules.
     */

    public void setStripperRules(Mode rules) {
        stripperRules = rules;
    }

    /**
     * Get the rules determining which nodes are to be stripped from the tree
     *
     * @return a Mode object containing the whitespace stripping rules. A Mode
     *         is generally a collection of template rules, but it is reused here to represent
     *         a collection of stripping rules.
     */

    public Mode getStripperRules() {
        return stripperRules;
    }

    /**
     * Indicate that the stylesheet does some whitespace stripping
     *
     * @param strips true if the stylesheet performs whitespace stripping
     *               of one or more elements.
     */

    public void setStripsWhitespace(boolean strips) {
        stripsWhitespace = strips;
    }

    /**
     * Create a Stripper which handles whitespace stripping definitions
     *
     * @return the constructed Stripper object
     */

    public Stripper newStripper() {
        return new Stripper(stripperRules);
    }

    /**
     * Determine whether this stylesheet does any whitespace stripping
     *
     * @return true if the stylesheet performs whitespace stripping
     *         of one or more elements.
     */

    public boolean stripsWhitespace() {
        return stripsWhitespace;
    }

    /**
     * Set whether source documents are to have their type annotations stripped
     */

    public void setStripsInputTypeAnnotations(boolean strips) {
        stripsInputTypeAnnotations = strips;
    }

    /**
     * Determine whether source documents are to have their type annotations stripped
     */

    public boolean stripsInputTypeAnnotations() {
        return stripsInputTypeAnnotations;
    }

    /**
     * Set the KeyManager which handles key definitions
     *
     * @param km the KeyManager containing the xsl:key definitions
     */

    public void setKeyManager(KeyManager km) {
        keyManager = km;
    }

    /**
     * Get the KeyManager which handles key definitions
     *
     * @return the KeyManager containing the xsl:key definitions
     */

    public KeyManager getKeyManager() {
        if (keyManager == null) {
            keyManager = new KeyManager(getConfiguration());
        }
        return keyManager;
    }

    /**
     * Set the default output properties (the properties for the unnamed output format)
     *
     * @param properties the output properties to be used when the unnamed output format
     *                   is selected
     */

    public void setDefaultOutputProperties(Properties properties) {
        defaultOutputProperties = properties;
    }

    /**
     * Get the default output properties
     *
     * @return the properties for the unnamed output format
     */

    public Properties getDefaultOutputProperties() {
        if (defaultOutputProperties == null) {
            defaultOutputProperties = new Properties();
        }
        return defaultOutputProperties;
    }

    /**
     * An a named output format
     *
     * @param fingerprint the name of the output format
     * @param properties  the properties of the output format
     */

    public void setOutputProperties(int fingerprint, Properties properties) {
        if (outputDeclarations == null) {
            outputDeclarations = new IntHashMap(5);
        }
        outputDeclarations.put(fingerprint, properties);
    }

    /**
     * Get a named output format
     *
     * @param fingerprint the name of the output format
     * @return properties the properties of the output format. Return null if there are
     *         no output properties with the given name
     */

    public Properties getOutputProperties(int fingerprint) {
        if (outputDeclarations == null) {
            return null;
        } else {
            Properties props = (Properties)outputDeclarations.get(fingerprint);
            if (props == null && fingerprint == -1) {
                props = new Properties();
            }
            return props;
        }
    }


    /**
     * Set the DecimalFormatManager which handles decimal-format definitions
     *
     * @param dfm the DecimalFormatManager containing the named xsl:decimal-format definitions
     */

    public void setDecimalFormatManager(DecimalFormatManager dfm) {
        decimalFormatManager = dfm;
    }

    /**
     * Get the DecimalFormatManager which handles decimal-format definitions
     *
     * @return the DecimalFormatManager containing the named xsl:decimal-format definitions
     */

    public DecimalFormatManager getDecimalFormatManager() {
        if (decimalFormatManager == null) {
            decimalFormatManager = new DecimalFormatManager();
        }
        return decimalFormatManager;
    }

    /**
     * Set the default collation
     *
     * @param name the name of the default collation
     */

    public void setDefaultCollationName(String name) {
        defaultCollationName = name;
    }

    /**
     * Get the name of the default collation
     *
     * @return the name of the default collation; this is the code point collation URI if no other default
     *         has been set up.
     */

    public String getDefaultCollationName() {
        if (defaultCollationName == null) {
            return NamespaceConstant.CODEPOINT_COLLATION_URI;
        } else {
            return defaultCollationName;
        }
    }

    /**
     * Get the default collation
     *
     * @return a Comparator that implements the default collation
     */

    public Comparator getDefaultCollation() {
        if (defaultCollationName == null) {
            return CodepointCollator.getInstance();
        } else {
            return getNamedCollation(defaultCollationName);
        }
    }

    /**
     * Set the table of collations
     *
     * @param table a hash table that maps collation names (URIs) to objects representing the
     *              collation information
     */

    public void setCollationTable(CollationMap table) {
        collationTable = table;
    }

    /**
     * Get the table of collations
     *
     * @return a hash table that maps collation names (URIs) to objects representing the
     *         collation information
     */

    public CollationMap getCollationTable() {
        return collationTable;
    }

    /**
     * Find a named collation.
     *
     * @param name identifies the name of the collation required; null indicates that the default
     *             collation is required
     * @return the requested collation, or null if the collation is not found
     */

    public Comparator getNamedCollation(String name) {
        if (collationTable == null) {
            collationTable = new CollationMap(config);
        }
        return (Comparator)collationTable.getNamedCollation(name);
    }

    /**
     * Add an XQuery library module to the configuration. The Executable maintains a table indicating
     * for each module namespace, the set of modules that have been loaded from that namespace. If a
     * module import is encountered that specifies no location hint, all the known modules for that
     * namespace are imported.
     */

    public void addQueryLibraryModule(StaticQueryContext module) {
        if (queryLibraryModules == null) {
            queryLibraryModules = new HashMap(5);
        }
        String uri = module.getModuleNamespace();
        List existing = (List)queryLibraryModules.get(uri);
        if (existing == null) {
            existing = new ArrayList(5);
            existing.add(module);
            queryLibraryModules.put(uri, existing);
        } else {
            existing.add(module);
        }
    }

    /**
     * Locate the known XQuery library modules for a given module namespace.
     *
     * @param namespace the module namespace URI
     * @return a list of items each of which is the StaticQueryContext representing a module, or
     *         null if the module namespace is unknown
     */

    public List getQueryLibraryModules(String namespace) {
        if (queryLibraryModules == null) {
            return null;
        }
        return (List)queryLibraryModules.get(namespace);
    }

    /**
     * Fix up global variables and functions in all query modules. This is done right at the end, because
     * recursive imports are permitted
     */

    public void fixupQueryModules(StaticQueryContext main) throws XPathException {

        main.bindUnboundVariables();
        if (queryLibraryModules != null) {
            Iterator iter = queryLibraryModules.values().iterator();
            while (iter.hasNext()) {
                List modules = (List)iter.next();
                Iterator iter2 = modules.iterator();
                while (iter2.hasNext()) {
                    StaticQueryContext env = (StaticQueryContext)iter2.next();
                    env.bindUnboundVariables();
                }
            }
        }
        List compiledVars = main.fixupGlobalVariables(main.getGlobalStackFrameMap());


        main.bindUnboundFunctionCalls();
        if (queryLibraryModules != null) {
            Iterator iter = queryLibraryModules.values().iterator();
            while (iter.hasNext()) {
                List modules = (List)iter.next();
                Iterator iter2 = modules.iterator();
                while (iter2.hasNext()) {
                    StaticQueryContext env = (StaticQueryContext)iter2.next();
                    env.bindUnboundFunctionCalls();
                }
            }
        }
        main.fixupGlobalFunctions();

        main.typeCheckGlobalVariables(compiledVars);
    }

    /**
     * Set the space requirements for variables used in template match patterns
     *
     * @param patternLocals The largest number of local variables used in the match pattern of any template rule
     */

    public void setPatternSlotSpace(int patternLocals) {
        largestPatternStackFrame = patternLocals;
    }

    /**
     * Get the global variable map
     *
     * @return the SlotManager defining the allocation of slots to global variables
     */

    public SlotManager getGlobalVariableMap() {
        if (globalVariableMap == null) {
            globalVariableMap = config.makeSlotManager();
        }
        return globalVariableMap;
    }

    /**
     * Get the index of global variables
     *
     * @return the index of global variables. This is a HashMap in which the key is the integer fingerprint
     *         of the variable name, and the value is the GlobalVariable object representing the compiled global variable
     */

    public IntHashMap getCompiledGlobalVariables() {
        return compiledGlobalVariables;
    }

    /**
     * Register a global variable
     */

    public void registerGlobalVariable(GlobalVariable variable) {
        compiledGlobalVariables.put(variable.getVariableFingerprint(), variable);
    }

    /**
     * Allocate space in bindery for all the variables needed
     *
     * @param bindery The bindery to be initialized
     */

    public void initialiseBindery(Bindery bindery) {
        bindery.allocateGlobals(getGlobalVariableMap());
    }

    /**
     * Determine the size of the stack frame needed for evaluating match patterns
     */

    public int getLargestPatternStackFrame() {
        return largestPatternStackFrame;
    }

    /**
     * Set the location map
     */

    public void setLocationMap(LocationMap map) {
        locationMap = map;
    }

    /**
     * Get the location map
     */

    public LocationMap getLocationMap() {
        return locationMap;
    }

    /**
     * Add a required parameter
     */

    public void addRequiredParam(int fingerprint) {
        if (requiredParams == null) {
            requiredParams = new IntHashSet(5);
        }
        requiredParams.add(fingerprint);
    }

    /**
     * Check that all required parameters have been supplied
     */

    public void checkAllRequiredParamsArePresent(GlobalParameterSet params) throws XPathException {
        if (requiredParams == null) {
            return;
        }
        IntIterator iter = requiredParams.iterator();
        while (iter.hasNext()) {
            int req = iter.next();
            if (params == null || params.get(req) == null) {
                DynamicError err = new DynamicError("No value supplied for required parameter " +
                        config.getNamePool().getDisplayName(req));
                err.setErrorCode("XTDE0050");
                throw err;
            }
        }
    }


    /**
     * If this Executable can't be compiled, set a message explaining why
     */

    public void setReasonUnableToCompile(String reason) {
        reasonUnableToCompile = reason;
    }

    /**
     * Determine whether this executable can be compiled; and if it can't, return the reason why
     *
     * @return null if the executable can be compiled, or a message otherwise
     */

    public String getReasonUnableToCompile() {
        return reasonUnableToCompile;
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
// Contributor(s):
// Portions marked "e.g." are from Edwin Glaser (edwin@pannenleiter.de)
//
