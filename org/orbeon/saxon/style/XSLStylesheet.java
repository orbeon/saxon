package net.sf.saxon.style;

import net.sf.saxon.Configuration;
import net.sf.saxon.PreparedStylesheet;
import net.sf.saxon.expr.ComputedExpression;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.functions.*;
import net.sf.saxon.instruct.Executable;
import net.sf.saxon.instruct.LocationMap;
import net.sf.saxon.om.*;
import net.sf.saxon.query.XQueryFunction;
import net.sf.saxon.query.XQueryFunctionLibrary;
import net.sf.saxon.sort.CodepointCollator;
import net.sf.saxon.sort.CollationFactory;
import net.sf.saxon.trans.*;
import net.sf.saxon.type.Type;

import javax.xml.transform.TransformerConfigurationException;
import java.util.*;

/**
 * An xsl:stylesheet or xsl:transform element in the stylesheet. <br>
 * Note this element represents a stylesheet module, not necessarily
 * the whole stylesheet.
 */

public class XSLStylesheet extends StyleElement {

    Executable exec = new Executable();

    // the Location Map keeps track of modules and line numbers of expressions and instructions
    private LocationMap locationMap = new LocationMap();

    // index of global variables and parameters, by fingerprint
    // (overridden variables are excluded).
    // Used at compile-time only, except for debugging
    private HashMap globalVariableIndex = new HashMap(20);

    // the name pool used for names that will be needed at run-time, notably
    // the names used in XPath expressions and patterns, but also key names, parameter names, etc
    private NamePool targetNamePool;

    // true if this stylesheet was included by xsl:include, false if it is the
    // principal stylesheet or if it was imported
    private boolean wasIncluded = false;

    // the import precedence for top-level elements in this stylesheet
    private int precedence = 0;

    // the lowest precedence of any stylesheet imported by this one
    private int minImportPrecedence = 0;

    // the StyleSheet that included or imported this one; null for the principal stylesheet
    private XSLStylesheet importer = null;

    // the PreparedStylesheet object used to load this stylesheet
    private PreparedStylesheet stylesheet;

    // the top-level elements in this logical stylesheet (after include/import)
    private List topLevel;

    // table of named templates. Key is the integer fingerprint of the template name;
    // value is the XSLTemplate object in the source stylesheet.
    private HashMap templateIndex = new HashMap(20);

    // the value of the inputTypeAnnotations attribute on this module, combined with the values
    // on all imported/included modules. This is a combination of the bit-significant values
    // ANNOTATION_STRIP and ANNOTATION_PRESERVE.
    private int inputAnnotations = 0;
    public static final int ANNOTATION_STRIP = 1;
    public static final int ANNOTATION_PRESERVE = 2;

    // table of imported schemas
    private HashSet schemaIndex = new HashSet(10);

    // table of functions imported from XQuery library modules
    private XQueryFunctionLibrary queryFunctions;

    // function library for external Java functions
    private JavaExtensionLibrary javaFunctions;

    // media type (MIME type) of principal output
    //private String mediaType;

    // namespace aliases. This information is needed at compile-time only
    private int numberOfAliases = 0;
    private ArrayList namespaceAliasList = new ArrayList(5);
    private short[] aliasSCodes;
    private int[] aliasNCodes;

    // count of the maximum number of local variables in xsl:template match patterns
    private int largestPatternStackFrame = 0;

    // default validation
    private int defaultValidation = Validation.STRIP;

    // library of functions that are in-scope for XPath expressions in this stylesheet
    private FunctionLibraryList functionLibrary;

    /**
     * Create link to the owning PreparedStylesheet object
     */

    public void setPreparedStylesheet(PreparedStylesheet sheet) {
        Configuration config = sheet.getConfiguration();
        stylesheet = sheet;
        targetNamePool = sheet.getTargetNamePool();
        exec.setConfiguration(config);
        exec.setRuleManager(new RuleManager());
        exec.setLocationMap(locationMap);

        functionLibrary = new FunctionLibraryList();
        functionLibrary.addFunctionLibrary(new SystemFunctionLibrary(SystemFunctionLibrary.FULL_XSLT));
        functionLibrary.addFunctionLibrary(new StylesheetFunctionLibrary(this, true));
        functionLibrary.addFunctionLibrary(config.getVendorFunctionLibrary());
        functionLibrary.addFunctionLibrary(new ConstructorFunctionLibrary(config));
        queryFunctions = new XQueryFunctionLibrary(config, false);
        functionLibrary.addFunctionLibrary(queryFunctions);
        if (getConfiguration().isAllowExternalFunctions()) {
            javaFunctions = new JavaExtensionLibrary(config);
            functionLibrary.addFunctionLibrary(javaFunctions);
        }
        functionLibrary.addFunctionLibrary(new StylesheetFunctionLibrary(this, false));

    }

    /**
     * Get the owning PreparedStylesheet object
     */

    public PreparedStylesheet getPreparedStylesheet() {
        if (importer != null) {
            return importer.getPreparedStylesheet();
        }
        return stylesheet;
    }

    /**
     * Get the run-time Executable object
     */

    public Executable getExecutable() {
        return exec;
    }

    /**
     * Get the function library. Available only on the principal stylesheet module
     */

    public FunctionLibrary getFunctionLibrary() {
        return functionLibrary;
    }

    /**
     * Get the locationMap object
     */

    public LocationMap getLocationMap() {
        return locationMap;
    }

    /**
     * Get the namepool to be used at run-time, this namepool holds the names used in
     * all XPath expressions and patterns
     */

    public NamePool getTargetNamePool() {
        return targetNamePool;
    }

    /**
     * Get the RuleManager which handles template rules
     */

    public RuleManager getRuleManager() {
        return exec.getRuleManager();
    }

    /**
     * Get the rules determining which nodes are to be stripped from the tree
     */

    protected Mode getStripperRules() {
        if (exec.getStripperRules() == null) {
            exec.setStripperRules(new Mode(Mode.STRIPPER_MODE));
        }
        return exec.getStripperRules();
    }

    /**
     * Determine whether this stylesheet does any whitespace stripping
     */

    public boolean stripsWhitespace() {
        for (int i = 0; i < topLevel.size(); i++) {
            NodeInfo s = (NodeInfo) topLevel.get(i);
            if (s.getFingerprint() == StandardNames.XSL_STRIP_SPACE) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the KeyManager which handles key definitions
     */

    public KeyManager getKeyManager() {
        if (exec.getKeyManager() == null) {
            exec.setKeyManager(new KeyManager(getConfiguration()));
        }
        return exec.getKeyManager();
    }

    /**
     * Get the DecimalFormatManager which handles decimal-format definitions
     */

    public DecimalFormatManager getDecimalFormatManager() {
        if (exec.getDecimalFormatManager() == null) {
            exec.setDecimalFormatManager(new DecimalFormatManager());
        }
        return exec.getDecimalFormatManager();
    }

    /**
     * Register a named collation (actually a Comparator)
     */

    public void setCollation(String name, Comparator collation, boolean isDefault) {
        if (exec.getCollationTable() == null) {
            exec.setCollationTable(new HashMap(20));
        }
        exec.getCollationTable().put(name, collation);
        if (isDefault) {
            exec.setDefaultCollationName(name);
        }
    }

    /**
     * Find a named collation. Note this method should only be used at compile-time, before declarations
     * have been pre-processed. After that time, use getCollation().
     * @param name identifies the name of the collation required; null indicates that the default
     * collation is required
     * @return null if the collation is not found
     */

    protected Comparator findCollation(String name) {

        if (name.equals(NamespaceConstant.CodepointCollationURI)) {
            return CodepointCollator.getInstance();
        }

        // First try to find it in the table

        Comparator c = null;
        if (name == null) {
            name = exec.getDefaultCollationName();
        }
        if (exec.getCollationTable() != null) {
            c = (Comparator) exec.getCollationTable().get(name);
        }
        if (c != null) return c;

        // At compile-time, the collation might not yet be in the table. So search for it

        XSLStylesheet stylesheet = getPrincipalStylesheet();
        List toplevel = stylesheet.getTopLevel();

        // search for a matching collation name, starting at the end in case of duplicates.
        // this also ensures we get the one with highest import precedence.
        for (int i = toplevel.size() - 1; i >= 0; i--) {
            if (toplevel.get(i) instanceof SaxonCollation) {
                SaxonCollation t = (SaxonCollation) toplevel.get(i);
                if (name == null && t.isDefaultCollation()) {
                    exec.setDefaultCollationName(t.getCollationName());
                    return t.getCollator();
                } else if (t.getCollationName().equals(name)) {
                    return t.getCollator();
                }
            }
        }

        // if it's not defined in the stylesheet, it might be a standard URI

        if (name == null) {
            return null;
        }

        try {
            return CollationFactory.makeCollationFromURI(name);
        } catch (XPathException e) {
            return null;
        }
    }

    /**
     * Get the name of the default collation
     */

    public String getDefaultCollationName() {
        return exec.getDefaultCollationName();
    }

    /**
     * Get a character map, identified by the fingerprint of its name.
     * Search backwards through the stylesheet.
     * @param fingerprint The fingerprint of the character map name,
     * in the target namepool.
     * @return the identified character map, or null if not found
     */

    public XSLCharacterMap getCharacterMap(int fingerprint) {
        for (int i = topLevel.size() - 1; i >= 0; i--) {
            if (topLevel.get(i) instanceof XSLCharacterMap) {
                XSLCharacterMap t = (XSLCharacterMap) topLevel.get(i);
                if (t.getCharacterMapFingerprint() == fingerprint) {
                    return t;
                }
            }
        }
        return null;
    }

    /**
     * Set the import precedence of this stylesheet
     */

    public void setPrecedence(int prec) {
        precedence = prec;
    }

    /**
     * Get the import precedence of this stylesheet
     */

    public int getPrecedence() {
        if (wasIncluded) return importer.getPrecedence();
        return precedence;
    }

    /**
     * Get the minimum import precedence of this stylesheet, that is, the lowest precedence
     * of any stylesheet imported by this one
     */

    public int getMinImportPrecedence() {
        return minImportPrecedence;
    }

    /**
     * Set the minimum import precedence of this stylesheet, that is, the lowest precedence
     * of any stylesheet imported by this one
     */

    public void setMinImportPrecedence(int precedence) {
        minImportPrecedence = precedence;
    }

    /**
     * Set the StyleSheet that included or imported this one.
     */

    public void setImporter(XSLStylesheet importer) {
        this.importer = importer;
    }

    /**
     * Get the StyleSheet that included or imported this one.
     * @return null if this is the principal stylesheet
     */

    public XSLStylesheet getImporter() {
        return importer;
    }

    /**
     * Indicate that this stylesheet was included (by its "importer") using an xsl:include
     * statement as distinct from xsl:import
     */

    public void setWasIncluded() {
        wasIncluded = true;
    }

    /**
     * Get the top level elements in this stylesheet, after applying include/import
     */

    public List getTopLevel() {
        return topLevel;
    }

    /**
     * Allocate a slot number for a global variable or parameter
     */

    public int allocateGlobalSlot(int fingerprint) {
        return exec.getGlobalVariableMap().allocateSlotNumber(fingerprint);
    }

    /**
     * Ensure there is enough space for local variables or parameters when evaluating the match pattern of
     * template rules
     */

    public void allocatePatternSlots(int n) {
        if (n > largestPatternStackFrame) {
            largestPatternStackFrame = n;
        }
    }

    /**
     * Prepare the attributes on the stylesheet element
     */

    public void prepareAttributes() throws TransformerConfigurationException {

        String inputTypeAnnotationsAtt = null;
        AttributeCollection atts = getAttributeList();
        for (int a = 0; a < atts.getLength(); a++) {

            int nc = atts.getNameCode(a);
            String f = getNamePool().getClarkName(nc);
            if (f == StandardNames.VERSION) {
                processVersionAttribute(f);
            } else if (f == StandardNames.ID) {
                //
            } else if (f == StandardNames.EXTENSION_ELEMENT_PREFIXES) {
                //
            } else if (f == StandardNames.EXCLUDE_RESULT_PREFIXES) {
                //
            } else if (f == StandardNames.DEFAULT_VALIDATION) {
                defaultValidation = Validation.getCode(atts.getValue(a));
                if (defaultValidation == Validation.INVALID) {
                    compileError("Invalid value for default-validation attribute. " +
                            "Permitted values are (strict, lax, preserve, strip)", "XT0020");
                }
            } else if (f == StandardNames.INPUT_TYPE_ANNOTATIONS) {
                inputTypeAnnotationsAtt = atts.getValue("", f);
            } else {
                checkUnknownAttribute(nc);
            }
        }
        if (version == null) {
            reportAbsence("version");
        }

        if (inputTypeAnnotationsAtt != null) {
            if (inputTypeAnnotationsAtt.equals("strip")) {
                setInputTypeAnnotations(ANNOTATION_STRIP);
            } else if (inputTypeAnnotationsAtt.equals("preserve")) {
                setInputTypeAnnotations(ANNOTATION_PRESERVE);
            } else if (inputTypeAnnotationsAtt.equals("unspecified")) {
                //
            } else {
                compileError("Invalid value for input-type-annotations attribute. " +
                             "Permitted values are (strip, preserve, unspecified)", "XT0020");
            }
        }
    }

    /**
     * Get the value of the default validation attribute
     */

    public int getDefaultValidation() {
        return defaultValidation;
    }


    /**
     * Get the value of the input-type-annotations attribute, for this module alone.
     * The value is an or-ed combination of the two bits
     * {@link #ANNOTATION_STRIP} and {@link #ANNOTATION_PRESERVE}
     */

    public int getInputTypeAnnotationsAttribute() throws TransformerConfigurationException {
        String inputTypeAnnotationsAtt = getAttributeValue(StandardNames.INPUT_TYPE_ANNOTATIONS);
        if (inputTypeAnnotationsAtt != null) {
            if (inputTypeAnnotationsAtt.equals("strip")) {
                setInputTypeAnnotations(ANNOTATION_STRIP);
            } else if (inputTypeAnnotationsAtt.equals("preserve")) {
                setInputTypeAnnotations(ANNOTATION_PRESERVE);
            } else if (inputTypeAnnotationsAtt.equals("unspecified")) {
                //
            } else {
                compileError("Invalid value for input-type-annotations attribute. " +
                             "Permitted values are (strip, preserve, unspecified)", "XT0020");
            }
        }
        return inputAnnotations;
    }


    /**
     * Get the value of the input-type-annotations attribute, for this module combined with that
     * of all included/imported modules. The value is an or-ed combination of the two bits
     * {@link #ANNOTATION_STRIP} and {@link #ANNOTATION_PRESERVE}
     */

    public int getInputTypeAnnotations() {
        return inputAnnotations;
    }

    /**
     * Set the value of the input-type-annotations attribute, for this module combined with that
     * of all included/imported modules. The value is an or-ed combination of the two bits
     * {@link #ANNOTATION_STRIP} and {@link #ANNOTATION_PRESERVE}
     */

    public void setInputTypeAnnotations(int annotations) throws TransformerConfigurationException {
        inputAnnotations |= annotations;
        if (inputAnnotations == (ANNOTATION_STRIP | ANNOTATION_PRESERVE)) {
            compileError("One stylesheet module specifies input-type-annotations='strip', " +
                    "another specifies input-type-annotations='preserve'", "XT0265");
        }
    }

    /**
     * Get the declared namespace alias for a given namespace URI code if there is one.
     * If there is more than one, we get the last.
     * @param uriCode The code of the uri used in the stylesheet.
     * @return The namespace code to be used (prefix in top half, uri in bottom half): return -1
     * if no alias is defined
     */

    protected int getNamespaceAlias(short uriCode) {

        // if there are several matches, the last in stylesheet takes priority;
        // but the list is in reverse stylesheet order
        for (int i = 0; i < numberOfAliases; i++) {
            if (uriCode == aliasSCodes[i]) {
                return aliasNCodes[i];
            }
        }
        return uriCode;
    }

    /**
     * Determine if a namespace is included in the result-prefix of a namespace-alias
     */

    protected boolean isAliasResultNamespace(short uriCode) {
        for (int i = 0; i < numberOfAliases; i++) {
            if (uriCode == (aliasNCodes[i] & 0xffff)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Validate this element
     */

    public void validate() throws TransformerConfigurationException {
        if (validationError != null) {
            compileError(validationError);
        }
        if (!(getParent() instanceof DocumentInfo)) {
            compileError(getDisplayName() + " must be the outermost element", "XT0010");
        }

        AxisIterator kids = iterateAxis(Axis.CHILD);
        while(true) {
            NodeInfo curr = (NodeInfo)kids.next();
            if (curr == null) break;
            if (curr.getNodeKind() == Type.TEXT ||
                    curr instanceof XSLTemplate ||
                    curr instanceof XSLImport ||
                    curr instanceof XSLInclude ||
                    curr instanceof XSLAttributeSet ||
                    curr instanceof XSLCharacterMap ||
                    curr instanceof XSLDecimalFormat ||
                    curr instanceof XSLFunction ||
                    curr instanceof XSLImportSchema ||
                    curr instanceof XSLKey ||
                    curr instanceof XSLNamespaceAlias ||
                    curr instanceof XSLOutput ||
                    curr instanceof XSLParam ||
                    curr instanceof XSLPreserveSpace ||
                    curr instanceof XSLVariable ||
                    curr instanceof XSLParam ||
                    curr instanceof DataElement) {
                // all is well
            } else if (!NamespaceConstant.XSLT.equals(curr.getURI()) && !"".equals(curr.getURI())) {
                // elements in other namespaces are allowed and ignored
            } else if (curr instanceof AbsentExtensionElement && ((StyleElement)curr).forwardsCompatibleModeIsEnabled()) {
                // this is OK: an unknown XSLT element is allowed in forwards compatibility mode
            } else {
                ((StyleElement)curr).compileError("Element " + curr.getDisplayName() +
                        " must not appear directly within " + getDisplayName(), "XT0010");
            }
        }
    }

    /**
     * Preprocess does all the processing possible before the source document is available.
     * It is done once per stylesheet, so the stylesheet can be reused for multiple source
     * documents. The method is called only on the XSLStylesheet element representing the
     * principal stylesheet module
     */

    public void preprocess() throws TransformerConfigurationException {

        // process any xsl:include and xsl:import elements

        spliceIncludes();

        // build indexes for selected top-level elements

        buildIndexes();

        // process the attributes of every node in the tree

        processAllAttributes();

        // collect any namespace aliases

        collectNamespaceAliases();

        // fix up references from XPath expressions to variables and functions, for static typing

        for (int i = 0; i < topLevel.size(); i++) {
            Object node = topLevel.get(i);
            if (node instanceof StyleElement) {
                ((StyleElement) node).fixupReferences();
            }
        }

        // Validate the whole logical style sheet (i.e. with included and imported sheets)

        validate();
        for (int i = 0; i < topLevel.size(); i++) {
            Object node = topLevel.get(i);
            if (node instanceof StyleElement) {
                ((StyleElement) node).validateSubtree();
            }
        }
    }

    /**
     * Process xsl:include and xsl:import elements.
     */

    public void spliceIncludes() throws TransformerConfigurationException {

        boolean foundNonImport = false;
        topLevel = new ArrayList(50);
        minImportPrecedence = precedence;
        StyleElement previousElement = this;

        AxisIterator kids = iterateAxis(Axis.CHILD);

        while (true) {
            NodeInfo child = (NodeInfo) kids.next();
            if (child == null) {
                break;
            }
            if (child.getNodeKind() == Type.TEXT) {
                // in an embedded stylesheet, white space nodes may still be there
                if (!Navigator.isWhite(child.getStringValueCS())) {
                    previousElement.compileError(
                            "No character data is allowed between top-level elements");
                }

            } else if (child instanceof DataElement) {
                foundNonImport = true;
            } else {
                previousElement = (StyleElement) child;
                if (child instanceof XSLGeneralIncorporate) {
                    XSLGeneralIncorporate xslinc = (XSLGeneralIncorporate) child;
                    xslinc.processAttributes();

                    if (xslinc.isImport()) {
                        if (foundNonImport) {
                            xslinc.compileError("xsl:import elements must come first", "XT0010");
                        }
                    } else {
                        foundNonImport = true;
                    }

                    // get the included stylesheet. This follows the URL, builds a tree, and splices
                    // in any indirectly-included stylesheets.

                    XSLStylesheet inc =
                            xslinc.getIncludedStylesheet(this, precedence);
                    if (inc == null) return;  // error has been reported

                    // after processing the imported stylesheet and any others it brought in,
                    // adjust the import precedence of this stylesheet if necessary

                    if (xslinc.isImport()) {
                        precedence = inc.getPrecedence() + 1;
                    } else {
                        precedence = inc.getPrecedence();
                        inc.setMinImportPrecedence(minImportPrecedence);
                        inc.setWasIncluded();
                    }

                    // copy the top-level elements of the included stylesheet into the top level of this
                    // stylesheet. Normally we add these elements at the end, in order, but if the precedence
                    // of an element is less than the precedence of the previous element, we promote it.
                    // This implements the requirement in the spec that when xsl:include is used to
                    // include a stylesheet, any xsl:import elements in the included document are moved
                    // up in the including document to after any xsl:import elements in the including
                    // document.

                    List incchildren = inc.topLevel;
                    for (int j = 0; j < incchildren.size(); j++) {
                        StyleElement elem = (StyleElement) incchildren.get(j);
                        int last = topLevel.size() - 1;
                        if (last < 0 || elem.getPrecedence() >= ((StyleElement) topLevel.get(last)).getPrecedence()) {
                            topLevel.add(elem);
                        } else {
                            while (last >= 0 && elem.getPrecedence() < ((StyleElement) topLevel.get(last)).getPrecedence()) {
                                last--;
                            }
                            topLevel.add(last + 1, elem);
                        }
                    }
                } else {
                    foundNonImport = true;
                    topLevel.add(child);
                }
            }
        }
    }

    /**
     * Build indexes for selected top-level declarations
     */

    private void buildIndexes() throws TransformerConfigurationException {
    // Scan the declarations in reverse order
        for (int i = topLevel.size() - 1; i >= 0; i--) {
            Object node = topLevel.get(i);
            if (node instanceof XSLTemplate) {
                indexNamedTemplate((XSLTemplate) node);
            } else if (node instanceof XSLVariableDeclaration) {
                indexVariableDeclaration((XSLVariableDeclaration) node);
            } else if (node instanceof XSLNamespaceAlias) {
                namespaceAliasList.add(node);
                numberOfAliases++;
            } else if (node instanceof XSLImportSchema) {
                ((XSLImportSchema) node).readSchema();
            } else if (node instanceof XSLDecimalFormat) {
                ((XSLDecimalFormat) node).register();
            } else if (node instanceof SaxonImportQuery) {
                ((SaxonImportQuery) node).importModule();
            }
        }
    }

    /**
     * Index a global xsl:variable or xsl:param element
     * @param var The XSLVariable or XSLParam element
     * @throws TransformerConfigurationException
     */

    private void indexVariableDeclaration(XSLVariableDeclaration var) throws TransformerConfigurationException {
        int fingerprint = var.getVariableFingerprint();
        //System.err.println("fingerprint = " + fingerprint);
        if (fingerprint != -1) {
            Integer key = new Integer(fingerprint);
            // see if there is already a global variable with this precedence
            XSLVariableDeclaration other = (XSLVariableDeclaration) globalVariableIndex.get(key);
            if (other == null) {
                // this is the first
                globalVariableIndex.put(key, var);
            } else {
                // check the precedences
                int thisPrecedence = var.getPrecedence();
                int otherPrecedence = other.getPrecedence();
                if (thisPrecedence == otherPrecedence) {
                    var.compileError("Duplicate global variable declaration (see line " +
                            other.getLineNumber() + " of " + other.getSystemId() + ')', "XT0630");
                } else if (thisPrecedence < otherPrecedence) {
                    var.setRedundant();
                } else {
                    // can't happen, but we'll play safe
                    other.setRedundant();
                    globalVariableIndex.put(key, var);
                }
            }
        }
    }

    /**
     * Add a named template to the index
     * @param template The Template object
     * @throws TransformerConfigurationException
     */
    private void indexNamedTemplate(XSLTemplate template) throws TransformerConfigurationException {
        int fingerprint = template.getTemplateFingerprint();
        if (fingerprint != -1) {
            Integer key = new Integer(fingerprint);
            // see if there is already a named template with this precedence
            XSLTemplate other = (XSLTemplate) templateIndex.get(key);
            if (other == null) {
                // this is the first
                templateIndex.put(key, template);
            } else {
                // check the precedences
                int thisPrecedence = template.getPrecedence();
                int otherPrecedence = other.getPrecedence();
                if (thisPrecedence == otherPrecedence) {
                    template.compileError("Duplicate named template (see line " +
                            other.getLineNumber() + " of " + other.getSystemId() + ')', "XT0660");
                } else if (thisPrecedence < otherPrecedence) {
                    //template.setRedundantNamedTemplate();
                } else {
                    // can't happen, but we'll play safe
                    //other.setRedundantNamedTemplate();
                    templateIndex.put(key, template);
                }
            }
            exec.getNamedTemplateTable().put(key, template.getCompiledTemplate());
        }
    }

    /**
     * Collect any namespace aliases
     */

    private void collectNamespaceAliases() throws TransformerConfigurationException {
        aliasSCodes = new short[numberOfAliases];
        aliasNCodes = new int[numberOfAliases];
        int precedenceBoundary = 0;
        int currentPrecedence = -1;
        // Note that we are processing the list in reverse stylesheet order,
        // that is, highest precedence first.
        for (int i = 0; i < numberOfAliases; i++) {
            XSLNamespaceAlias xna = (XSLNamespaceAlias) namespaceAliasList.get(i);
            short scode = xna.getStylesheetURICode();
            int ncode = xna.getResultNamespaceCode();
            int prec = xna.getPrecedence();

            // check that there isn't a conflict with another xsl:namespace-alias
            // at the same precedence

            if (currentPrecedence != prec) {
                currentPrecedence = prec;
                precedenceBoundary = i;
            }

            for (int j = precedenceBoundary; j < i; j++) {
                if (scode == aliasSCodes[j]) {
                    if ((ncode & 0xffff) != (aliasNCodes[j] & 0xffff)) {
                        xna.compileError("More than one alias is defined for the same namespace prefix", "XT0810");
                    }
                }
            }

            aliasSCodes[i] = scode;
            aliasNCodes[i] = ncode;
        }
        namespaceAliasList = null;  // throw it in the garbage
    }

    protected boolean hasNamespaceAliases() {
        return numberOfAliases > 0;
    }

    /**
     * Process the attributes of every node in the stylesheet
     */

    public void processAllAttributes() throws TransformerConfigurationException {
        prepareAttributes();
        if (topLevel == null) return;   // can happen if xsl:stylesheet appears in the wrong place
        for (int i = 0; i < topLevel.size(); i++) {
            Object s = topLevel.get(i);
            if (s instanceof StyleElement) {
                try {
                    ((StyleElement) s).processAllAttributes();
                } catch (TransformerConfigurationException err) {
                    ((StyleElement) s).compileError(err);
                }
            }
        }
    }

    /**
     * Get the global variable or parameter with a given fingerprint (taking
     * precedence rules into account)
     */

    public XSLVariableDeclaration getGlobalVariable(int fingerprint) {
        return (XSLVariableDeclaration) globalVariableIndex.get(new Integer(fingerprint));
    }

    /**
     * Create an output properties object representing the xsl:output elements in the stylesheet.
     * @param fingerprint The name of the output format required. If set to -1, gathers
     * information for the unnamed output format
     * @return the Properties object containing the details of the specified output format
     * @throws TransformerConfigurationException if a named output format does not exist in
     * the stylesheet
     */

    public Properties gatherOutputProperties(int fingerprint) throws TransformerConfigurationException {
        boolean found = (fingerprint == -1);
        Properties details = new Properties();
        for (int i = 0; i < topLevel.size(); i++) {
            Object s = topLevel.get(i);
            if (s instanceof XSLOutput) {
                XSLOutput xo = (XSLOutput) s;
                if (xo.getOutputFingerprint() == fingerprint) {
                    found = true;
                    xo.gatherOutputProperties(details);
                }
            }
        }
        if (!found) {
            throw new TransformerConfigurationException("Requested output format has not been defined");
        }
        return details;
    }

    /**
     * Declare an imported XQuery function
     */

    protected void declareXQueryFunction(XQueryFunction function) throws XPathException {
        queryFunctions.declareFunction(function);
    }

    /**
     * Declare a URI that maps to a Java class containing extension functions
     */

    protected void declareJavaClass(String uri, Class theClass) {
        javaFunctions.declareJavaClass(uri, theClass);
    }

    /**
     * Get an imported schema with a given namespace
     * @param targetNamespace The target namespace of the required schema.
     * Supply an empty string for the default namespace
     * @return the required Schema, or null if no such schema has been imported
     */

    protected boolean isImportedSchema(String targetNamespace) {
        return schemaIndex.contains(targetNamespace);
    }

    protected void addImportedSchema(String targetNamespace) {
        schemaIndex.add(targetNamespace);
    }

    /**
     * Compile the stylesheet to create an executable.
     */

    public Executable compileStylesheet() throws TransformerConfigurationException {

        try {

            // If any XQuery functions were imported, fix up all function calls
            // registered against these functions.
            try {
                queryFunctions.bindUnboundFunctionCalls();
                Iterator qf = queryFunctions.getFunctionDefinitions();
                while (qf.hasNext()) {
                    XQueryFunction f = (XQueryFunction) qf.next();
                    f.fixupReferences(getStaticContext());
                }
            } catch (XPathException e) {
                compileError(e);
            }

            // Call compile method for each top-level object in the stylesheet

            for (int i = 0; i < topLevel.size(); i++) {
                NodeInfo node = (NodeInfo) topLevel.get(i);
                if (node instanceof StyleElement) {
                    StyleElement snode = (StyleElement) node;
                    //int module = putModuleNumber(snode.getSystemId());
                    Expression inst = snode.compile(exec);

                    if (inst instanceof ComputedExpression) {
                        ((ComputedExpression) inst).setLocationId(allocateLocationId(getSystemId(), snode.getLineNumber()));
                    }
                }
            }

            // Fix up references to the default default decimal format

            if (exec.getDecimalFormatManager() != null) {
                try {
                    exec.getDecimalFormatManager().fixupDefaultDefault();
                } catch (StaticError err) {
                    compileError(err.getMessage(), err.getErrorCodeLocalPart());
                }
            }

            exec.setStripsWhitespace(stripsWhitespace());
            exec.setDefaultOutputProperties(gatherOutputProperties(-1));
            exec.setPatternSlotSpace(largestPatternStackFrame);
            exec.setStripsInputTypeAnnotations(inputAnnotations == ANNOTATION_STRIP);

            // Build the index of named character maps

            for (int i = 0; i < topLevel.size(); i++) {
                if (topLevel.get(i) instanceof XSLCharacterMap) {
                    XSLCharacterMap t = (XSLCharacterMap) topLevel.get(i);
                    if (!t.isRedundant()) {
                        int fp = t.getCharacterMapFingerprint();
                        HashMap map = new HashMap(20);
                        t.assemble(map);
                        if (exec.getCharacterMapIndex() == null) {
                            exec.setCharacterMapIndex(new HashMap(20));
                        }
                        exec.getCharacterMapIndex().put(new Integer(fp), map);
                    }
                }
            }

            // Build a run-time function library. This supports the use of function-available()
            // with a dynamic argument, and extensions such as saxon:evaluate().

            ExecutableFunctionLibrary overriding = new ExecutableFunctionLibrary(getConfiguration());
            ExecutableFunctionLibrary underriding = new ExecutableFunctionLibrary(getConfiguration());

            List toplevel = getTopLevel();
            for (int i=0; i<toplevel.size(); i++) {
                Object child = toplevel.get(i);
                if (child instanceof XSLFunction) {
                    XSLFunction func = (XSLFunction)child;
                    if (func.isOverriding()) {
                        overriding.addFunction(func.getCompiledFunction());
                    } else {
                        underriding.addFunction(func.getCompiledFunction());
                    }
                }
            }

            FunctionLibraryList libraryList = new FunctionLibraryList();
            libraryList.addFunctionLibrary(new SystemFunctionLibrary(SystemFunctionLibrary.FULL_XSLT));
            libraryList.addFunctionLibrary(overriding);
            libraryList.addFunctionLibrary(getConfiguration().getVendorFunctionLibrary());
            libraryList.addFunctionLibrary(new ConstructorFunctionLibrary(getConfiguration()));
            libraryList.addFunctionLibrary(queryFunctions);
            if (getConfiguration().isAllowExternalFunctions()) {
                libraryList.addFunctionLibrary(javaFunctions);
            }
            libraryList.addFunctionLibrary(underriding);
            exec.setFunctionLibrary(libraryList);


            return exec;

        } catch (RuntimeException err) {
        // if syntax errors were reported earlier, then exceptions may occur during this phase
        // due to inconsistency of data structures. We can ignore these exceptions as they
        // will go away when the user corrects the stylesheet
            if (getPreparedStylesheet().getErrorCount() > 0) {
                // do nothing
                return exec;
            } else {
                // rethrow the exception
                throw err;
            }
        }

    }

    /**
     * Dummy compile() method to satisfy the interface
     */

    public Expression compile(Executable exec) {
        return null;
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
