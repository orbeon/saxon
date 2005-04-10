package net.sf.saxon.style;

import net.sf.saxon.Err;
import net.sf.saxon.PreparedStylesheet;
import net.sf.saxon.event.LocationProvider;
import net.sf.saxon.expr.*;
import net.sf.saxon.instruct.*;
import net.sf.saxon.om.*;
import net.sf.saxon.pattern.*;
import net.sf.saxon.sort.SortKeyDefinition;
import net.sf.saxon.trace.InstructionInfo;
import net.sf.saxon.trace.Location;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.StaticError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.trans.SaxonErrorCode;
import net.sf.saxon.tree.ElementWithAttributes;
import net.sf.saxon.type.*;
import net.sf.saxon.value.*;
import org.xml.sax.Locator;

import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Abstract superclass for all element nodes in the stylesheet. <BR>
 * Note: this class implements Locator. The element
 * retains information about its own location in the stylesheet, which is useful when
 * an XSL error is found.
 */

public abstract class StyleElement extends ElementWithAttributes
        implements Locator, Container, InstructionInfo {

    protected short[] extensionNamespaces = null;		// a list of URI codes
    private short[] excludedNamespaces = null;		// a list of URI codes
    protected BigDecimal version = null;
    protected StaticContext staticContext = null;
    protected StaticError validationError = null;
    protected int reportingCircumstances = REPORT_ALWAYS;
    protected String defaultXPathNamespace = null;
    protected String defaultCollationName = null;
    private int lineNumber;     // maintained here because it's more efficient
    // than using the lineNumberMap
    private boolean explaining = false;
    // true if saxon:explain="yes"
    private int objectNameCode = -1;
    // for instructions that define an XSLT named object, the name of that object

    // Conditions under which an error is to be reported

    public static final int REPORT_ALWAYS = 1;
    public static final int REPORT_UNLESS_FORWARDS_COMPATIBLE = 2;
    public static final int REPORT_IF_INSTANTIATED = 3;

    /**
     * Constructor
     */

    public StyleElement() {
    }

    public Executable getExecutable() {
        return getPrincipalStylesheet().getExecutable();
    }

    /**
     * Get the LocationProvider allowing location identifiers to be resolved.
     */

    public LocationProvider getLocationProvider() {
        return getExecutable().getLocationMap();
    }

    /**
     * Get the namepool to be used at run-time, this namepool holds the names used in
     * all XPath expressions and patterns
     */

    public NamePool getTargetNamePool() {
        return getPrincipalStylesheet().getTargetNamePool();
    }

    /**
     * Get the static context for expressions on this element
     *
     * @return the static context
     */

    public StaticContext getStaticContext() {
        if (staticContext == null) {
            staticContext = new ExpressionContext(this);
        }
        return staticContext;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    /**
     * Make this node a substitute for a temporary one previously added to the tree. See
     * StyleNodeFactory for details. "A node like the other one in all things but its class".
     * Note that at this stage, the node will not yet be known to its parent, though it will
     * contain a reference to its parent; and it will have no children.
     */

    public void substituteFor(StyleElement temp) {
        this.parent = temp.parent;
        this.attributeList = temp.attributeList;
        this.namespaceList = temp.namespaceList;
        this.nameCode = temp.nameCode;
        this.sequence = temp.sequence;
        this.extensionNamespaces = temp.extensionNamespaces;
        this.excludedNamespaces = temp.excludedNamespaces;
        this.version = temp.version;
        this.root = temp.root;
        this.staticContext = temp.staticContext;
        this.validationError = temp.validationError;
        this.reportingCircumstances = temp.reportingCircumstances;
        this.lineNumber = temp.lineNumber;
    }

    /**
     * Set a validation error
     */

    protected void setValidationError(TransformerException reason,
                                      int circumstances) {
        validationError = StaticError.makeStaticError(reason);
        reportingCircumstances = circumstances;
    }

    /**
     * Determine whether this node is an instruction. The default implementation says it isn't.
     */

    public boolean isInstruction() {
        return false;
    }

    /**
     * Determine the type of item returned by this instruction (only relevant if
     * it is an instruction). Default implementation returns Type.ITEM, indicating
     * that we don't know, it might be anything. Returns null in the case of an element
     * such as xsl:sort or xsl:variable that can appear in a sequence constructor but
     * contributes nothing to the result sequence.
     *
     * @return the item type returned
     */

    protected ItemType getReturnedItemType() {
        return AnyItemType.getInstance();
    }

    /**
     * Get the most general type of item returned by the children of this instruction
     *
     * @return the lowest common supertype of the item types returned by the children
     */

    protected ItemType getCommonChildItemType() {
        ItemType t = NoNodeTest.getInstance();
        AxisIterator children = iterateAxis(Axis.CHILD);
        while (true) {
            NodeInfo next = (NodeInfo)children.next();
            if (next == null) {
                return t;
            }
            if (next instanceof StyleElement) {
                ItemType ret = ((StyleElement)next).getReturnedItemType();
                if (ret != null) {
                    t = Type.getCommonSuperType(t, ret);
                }
            } else {
                t = Type.getCommonSuperType(t, NodeKindTest.TEXT);
            }
            if (t == AnyItemType.getInstance()) {
                return t;       // no point looking any further
            }
        }
    }

    /**
     * Mark tail-recursive calls on templates and functions.
     * For most instructions, this does nothing.
     */

    public void markTailCalls() {
        // no-op
    }

    /**
     * Determine whether this type of element is allowed to contain a sequence constructor
     */

    public boolean mayContainSequenceConstructor() {
        return false;
    }

    /**
     * Determine whether this type of element is allowed to contain an xsl:fallback
     * instruction
     */

    public boolean mayContainFallback() {
        return mayContainSequenceConstructor();
    }

    /**
     * Get the containing XSLStylesheet element
     */

    public XSLStylesheet getContainingStylesheet() {
        NodeInfo next = this;
        while (!(next instanceof XSLStylesheet)) {
            next = next.getParent();
        }
        return (XSLStylesheet)next;
    }

    /**
     * Get the import precedence of this stylesheet element.
     */

    public int getPrecedence() {
        return getContainingStylesheet().getPrecedence();
    }

    /**
     * Get the URI for a namespace prefix using the in-scope namespaces for
     * this element in the stylesheet
     *
     * @param prefix     The namespace prefix: may be the empty string
     * @param useDefault True if the default namespace is to be used when the
     *                   prefix is "".
     * @return the namespace URI if the prefix is bound to a namespace; "" if the
     * prefix ("") is bound to no namespace; null if the prefix is not bound.
     */

//    public String getURIForPrefix(String prefix, boolean useDefault)  {
//        if ("".equals(prefix) && !useDefault) {
//            return "";
//        } else {
//            try {
//                short uricode = getURICodeForPrefix(prefix);
//                return getNamePool().getURIFromURICode(uricode);
//            } catch (NamespaceException e) {
//                return null;
//            }
//        }
//    }

    /**
     * Make a NameCode, using this Element as the context for namespace resolution, and
     * registering the code in the namepool. If the name is unprefixed, the
     * default namespace is <b>not</b> used.
     *
     * @param qname The name as written, in the form "[prefix:]localname". The name must have
     *              already been validated as a syntactically-correct QName.
     * @throws XPathException     if the qname is not a lexically-valid QName, or if the name
     *                            is in a reserved namespace.
     * @throws NamespaceException if the prefix of the qname has not been declared
     */

    public final int makeNameCode(String qname)
            throws XPathException, NamespaceException {

        NamePool namePool = getTargetNamePool();
        String[] parts;
        try {
            parts = Name.getQNameParts(qname);
        } catch (QNameException err) {
            StaticError e2 = new StaticError(err.getMessage());
            e2.setErrorCode("XTSE0020");
            throw e2;
        }
        String prefix = parts[0];
        if ("".equals(prefix)) {
            return namePool.allocate(prefix, (short)0, qname);

        } else {

            String uri = getURIForPrefix(prefix, false);
            if (uri == null) {
                throw new NamespaceException(prefix);
            }
            if (NamespaceConstant.isReserved(uri)) {
                StaticError err = new StaticError("Namespace prefix " + prefix + " refers to a reserved namespace");
                err.setErrorCode("XTSE0080");
                throw err;
            }
            return namePool.allocate(prefix, uri, parts[1]);
        }

    }

    /**
     * Make a NamespaceContext object representing the list of in-scope namespaces. The NamePool
     * used for numeric codes in the NamespaceContext will be the target name pool.
     */

    public SavedNamespaceContext makeNamespaceContext() {
        return new SavedNamespaceContext(getInScopeNamespaceCodes(), getNamePool());
    }

    /**
     * Process the attributes of this element and all its children
     */

    public void processAllAttributes() throws XPathException {
        staticContext = new ExpressionContext(this);
        processAttributes();
        AxisIterator kids = iterateAxis(Axis.CHILD);
        while (true) {
            NodeInfo child = (NodeInfo)kids.next();
            if (child == null) {
                return;
            }
            if (child instanceof StyleElement) {
                ((StyleElement)child).processAllAttributes();
            }
        }
    }

    /**
     * Get an attribute value given the Clark name of the attribute (that is,
     * the name in {uri}local format).
     */

    public String getAttributeValue(String clarkName) {
        int fp = getNamePool().allocateClarkName(clarkName);
        return getAttributeValue(fp);
    }

    /**
     * Process the attribute list for the element. This is a wrapper method that calls
     * prepareAttributes (provided in the subclass) and traps any exceptions
     */

    public final void processAttributes() throws XPathException {
        try {
            prepareAttributes();
        } catch (XPathException err) {
            if (forwardsCompatibleModeIsEnabled()) {
                setValidationError(err, REPORT_IF_INSTANTIATED);
            } else {
                compileError(err);
            }
        }
    }

    /**
     * Check whether an unknown attribute is permitted.
     *
     * @param nc The name code of the attribute name
     */

    protected void checkUnknownAttribute(int nc) throws XPathException {

        String attributeURI = getNamePool().getURI(nc);
        String elementURI = getURI();
        String clarkName = getNamePool().getClarkName(nc);

        if (clarkName.equals(StandardNames.SAXON_EXPLAIN)) {
            explaining = "yes".equals(getAttributeValue(nc & 0xfffff));
        }

        if (forwardsCompatibleModeIsEnabled()) {
            // then unknown attributes are permitted and ignored
            return;
        }

        // allow xsl:extension-element-prefixes etc on an extension element

        if (isInstruction() &&
                clarkName.startsWith('{' + NamespaceConstant.XSLT) &&
                !(elementURI.equals(NamespaceConstant.XSLT)) &&
                (clarkName.endsWith("}default-collation") ||
                clarkName.endsWith("}xpath-default-namespace") ||
                clarkName.endsWith("}extension-element-prefixes") ||
                clarkName.endsWith("}exclude-result-prefixes") ||
                clarkName.endsWith("}version") ||
                clarkName.endsWith("}use-when"))) {
            return;
        }

        // allow standard attributes on an XSLT element

        if (elementURI.equals(NamespaceConstant.XSLT) &&
                (clarkName == StandardNames.DEFAULT_COLLATION ||
                clarkName == StandardNames.XPATH_DEFAULT_NAMESPACE ||
                clarkName == StandardNames.EXTENSION_ELEMENT_PREFIXES ||
                clarkName == StandardNames.EXCLUDE_RESULT_PREFIXES ||
                clarkName == StandardNames.VERSION ||
                clarkName == StandardNames.USE_WHEN)) {
            return;
        }

        if ("".equals(attributeURI) || NamespaceConstant.XSLT.equals(attributeURI)) {
            compileError("Attribute " + Err.wrap(getNamePool().getDisplayName(nc), Err.ATTRIBUTE) +
                    " is not allowed on element " + Err.wrap(getDisplayName(), Err.ELEMENT), "XTSE0010");
        }
    }


    /**
     * Set the attribute list for the element. This is called to process the attributes (note
     * the distinction from processAttributes in the superclass).
     * Must be supplied in a subclass
     */

    public abstract void prepareAttributes() throws XPathException;

    /**
     * Find the last child instruction of this instruction. Returns null if
     * there are no child instructions, or if the last child is a text node.
     */

    protected StyleElement getLastChildInstruction() {
        StyleElement last = null;
        AxisIterator kids = iterateAxis(Axis.CHILD);
        while (true) {
            NodeInfo child = (NodeInfo)kids.next();
            if (child == null) {
                return last;
            }
            if (child instanceof StyleElement) {
                last = (StyleElement)child;
            } else {
                last = null;
            }
        }
    }

    /**
     * Make an expression in the context of this stylesheet element
     */

    public Expression makeExpression(String expression)
            throws XPathException {
        try {
            Expression exp = ExpressionTool.make(expression,
                    staticContext,
                    0, Token.EOF,
                    getLineNumber());
            return exp;
        } catch (XPathException err) {
            err.setLocator(this);
            if (!forwardsCompatibleModeIsEnabled()) {
                compileError(err);
            } else if (err.isTypeError()) {
                compileError(err);
            }
            ErrorExpression erexp = new ErrorExpression(err);
            erexp.setLocationId(allocateLocationId(getSystemId(), getLineNumber()));
            erexp.setParentExpression(this);
            return erexp;
        }
    }

    /**
     * Make a pattern in the context of this stylesheet element
     */

    public Pattern makePattern(String pattern)
            throws XPathException {
        try {
            return Pattern.make(pattern, staticContext, getPrincipalStylesheet().getExecutable());
        } catch (XPathException err) {
            compileError(err);
            return new NodeTestPattern(AnyNodeTest.getInstance());
        }
    }

    /**
     * Make an attribute value template in the context of this stylesheet element
     */

    public Expression makeAttributeValueTemplate(String expression)
            throws XPathException {
        try {
            return AttributeValueTemplate.make(expression, getLineNumber(), staticContext);
        } catch (XPathException err) {
            compileError(err);
            return new StringValue(expression);
        }
    }

    /**
     * Process an attribute whose value is a SequenceType
     */

    public SequenceType makeSequenceType(String sequenceType)
            throws XPathException {
        getStaticContext();
        try {
            ExpressionParser parser = new ExpressionParser();
            return parser.parseSequenceType(sequenceType, staticContext);
        } catch (XPathException err) {
            compileError(err);
// recovery path after reporting an error, e.g. undeclared namespace prefix
            return SequenceType.ANY_SEQUENCE;
        }
    }

    /**
     * Process the [xsl:]extension-element-prefixes attribute if there is one
     *
     * @param nc the Clark name  of the attribute required
     */

    protected void processExtensionElementAttribute(String nc)
            throws XPathException {
        String ext = getAttributeValue(nc);
        if (ext != null) {
            // go round twice, once to count the values and next to add them to the array
            int count = 0;
            StringTokenizer st1 = new StringTokenizer(ext);
            while (st1.hasMoreTokens()) {
                st1.nextToken();
                count++;
            }
            extensionNamespaces = new short[count];
            count = 0;
            StringTokenizer st2 = new StringTokenizer(ext);
            while (st2.hasMoreTokens()) {
                String s = st2.nextToken();
                if ("#default".equals(s)) {
                    s = "";
                }
                try {
                    short uriCode = getURICodeForPrefix(s);
                    extensionNamespaces[count++] = uriCode;
                } catch (NamespaceException err) {
                    extensionNamespaces = null;
                    compileError(err.getMessage(), "XT0280");
                }
            }
        }
    }

    /**
     * Process the [xsl:]exclude-result-prefixes attribute if there is one
     *
     * @param nc the Clark name of the attribute required
     */

    protected void processExcludedNamespaces(String nc)
            throws XPathException {
        String ext = getAttributeValue(nc);
        if (ext != null) {
            if ("#all".equals(ext.trim())) {
                int[] codes = getInScopeNamespaceCodes();
                excludedNamespaces = new short[codes.length];
                for (int i = 0; i < codes.length; i++) {
                    excludedNamespaces[i] = (short)(codes[i] & 0xffff);
                }
            } else {
                // go round twice, once to count the values and next to add them to the array
                int count = 0;
                StringTokenizer st1 = new StringTokenizer(ext);
                while (st1.hasMoreTokens()) {
                    st1.nextToken();
                    count++;
                }
                excludedNamespaces = new short[count];
                count = 0;
                StringTokenizer st2 = new StringTokenizer(ext);
                while (st2.hasMoreTokens()) {
                    String s = st2.nextToken();
                    if ("#default".equals(s)) {
                        s = "";
                    } else if ("#all".equals(s)) {
                        compileError("In exclude-result-prefixes, cannot mix #all with other values", "XTSE0020");
                    }
                    try {
                        short uriCode = getURICodeForPrefix(s);
                        excludedNamespaces[count++] = uriCode;
                    } catch (NamespaceException err) {
                        excludedNamespaces = null;
                        compileError(err.getMessage(), "XTSE0280");
                    }
                }
            }
        }
    }

    /**
     * Process the [xsl:]version attribute if there is one
     *
     * @param nc the Clark name of the attribute required
     */

    protected void processVersionAttribute(String nc) throws XPathException {
        String v = getAttributeValue(nc);
        if (v != null) {
            AtomicValue val = DecimalValue.makeDecimalValue(v, true);
            if (val instanceof ValidationErrorValue) {
                compileError("The version attribute must be a decimal literal");
            }
            version = ((DecimalValue)val).getValue();
        }
    }

    /**
     * Get the numeric value of the version number on this element,
     * or inherited from its ancestors
     */

    public BigDecimal getVersion() {
        if (version == null) {
            NodeInfo node = getParent();
            if (node instanceof StyleElement) {
                version = ((StyleElement)node).getVersion();
            } else {
                version = new BigDecimal("2.0");    // defensive programming
            }
        }
        return version;
    }

    /**
     * Determine whether forwards-compatible mode is enabled for this element
     */

    public boolean forwardsCompatibleModeIsEnabled() {
        return getVersion().compareTo(BigDecimal.valueOf(2)) > 0;
    }

    /**
     * Determine whether backwards-compatible mode is enabled for this element
     */

    public boolean backwardsCompatibleModeIsEnabled() {
        return getVersion().compareTo(BigDecimal.valueOf(2)) < 0;
    }

    /**
     * Process the [xsl:]default-xpath-namespace attribute if there is one
     *
     * @param nc the Clark name of the attribute required
     */

    protected void processDefaultCollationAttribute(String nc) throws XPathException {
        String v = getAttributeValue(nc);
        if (v != null) {
            StringTokenizer st = new StringTokenizer(v);
            while (st.hasMoreTokens()) {
                String uri = st.nextToken();
                if (uri.equals(NamespaceConstant.CodepointCollationURI)) {
                    defaultCollationName = uri;
                    return;
                } else if (uri.startsWith("http://saxon.sf.net/")) {
                    defaultCollationName = uri;
                    return;
                } else if (getPrincipalStylesheet().getExecutable().getNamedCollation(uri) != null) {
                    defaultCollationName = uri;
                    return;
                }
                // if not recognized, try the next URI in order
            }
            compileError("No recognized collation URI found in default-collation attribute", "XTSE0125");
        }
    }

    /**
     * Get the default collation for this element
     */

    protected String getDefaultCollationName() {
        StyleElement e = this;
        while (true) {
            if (e.defaultCollationName != null) {
                return e.defaultCollationName;
            }
            NodeInfo p = e.getParent();
            if (!(p instanceof StyleElement)) {
                break;
            }
            e = (StyleElement)p;
        }
        return getPrincipalStylesheet().getDefaultCollationName();
    }

    /**
     * Check whether a particular extension element namespace is defined on this node.
     * This checks this node only, not the ancestor nodes.
     * The implementation checks whether the prefix is included in the
     * [xsl:]extension-element-prefixes attribute.
     *
     * @param uriCode the namespace URI code being tested
     */

    protected boolean definesExtensionElement(short uriCode) {
        if (extensionNamespaces == null) {
            return false;
        }
        for (int i = 0; i < extensionNamespaces.length; i++) {
            if (extensionNamespaces[i] == uriCode) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check whether a namespace uri defines an extension element. This checks whether the
     * namespace is defined as an extension namespace on this or any ancestor node.
     *
     * @param uriCode the namespace URI code being tested
     */

    public boolean isExtensionNamespace(short uriCode) {
        NodeInfo anc = this;
        while (anc instanceof StyleElement) {
            if (((StyleElement)anc).definesExtensionElement(uriCode)) {
                return true;
            }
            anc = anc.getParent();
        }
        return false;
    }

    /**
     * Check whether this node excludes a particular namespace from the result.
     * This method checks this node only, not the ancestor nodes.
     *
     * @param uriCode the code of the namespace URI being tested
     */

    protected boolean definesExcludedNamespace(short uriCode) {
        if (excludedNamespaces == null) {
            return false;
        }
        for (int i = 0; i < excludedNamespaces.length; i++) {
            if (excludedNamespaces[i] == uriCode) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check whether a namespace uri defines an namespace excluded from the result.
     * This checks whether the namespace is defined as an excluded namespace on this
     * or any ancestor node.
     *
     * @param uriCode the code of the namespace URI being tested
     */

    public boolean isExcludedNamespace(short uriCode) {
        if (uriCode == NamespaceConstant.XSLT_CODE) {
            return true;
        }
        if (isExtensionNamespace(uriCode)) {
            return true;
        }
        NodeInfo anc = this;
        while (anc instanceof StyleElement) {
            if (((StyleElement)anc).definesExcludedNamespace(uriCode)) {
                return true;
            }
            anc = anc.getParent();
        }
        return false;
    }

    /**
     * Process the [xsl:]default-xpath-namespace attribute if there is one
     *
     * @param nc the Clark name of the attribute required
     */

    protected void processDefaultXPathNamespaceAttribute(String nc) throws TransformerConfigurationException {
        String v = getAttributeValue(nc);
        if (v != null) {
            defaultXPathNamespace = v;
        }
    }

    /**
     * Get the default XPath namespace code applicable to this element
     */

    protected short getDefaultXPathNamespace() {
        NodeInfo anc = this;
        while (anc instanceof StyleElement) {
            String x = ((StyleElement)anc).defaultXPathNamespace;
            if (x != null) {
                return getTargetNamePool().allocateCodeForURI(x);
            }
            anc = anc.getParent();
        }
        return NamespaceConstant.NULL_CODE;
        // indicates that the default namespace is the null namespace
    }

    /**
     * Get the Schema type definition for a type named in the stylesheet (in a
     * "type" attribute).
     *
     * @throws XPathException
     *          if the type is not declared in an
     *          imported schema, or is not a built-in type
     */

    public SchemaType getSchemaType(String typeAtt) throws XPathException {
        try {
            String[] parts = Name.getQNameParts(typeAtt);
            String lname = parts[1];
            String uri;
            if ("".equals(parts[0])) {
                // Name is unprefixed: use the default-xpath-namespace
                short uricode = getDefaultXPathNamespace();
                uri = getTargetNamePool().getURIFromURICode(uricode);
                nameCode = getTargetNamePool().allocate(parts[0], uricode, lname);
            } else {
                uri = getURIForPrefix(parts[0], false);
                if (uri == null) {
                    compileError("Namespace prefix for type annotation is undeclared", "XTSE0280");
                    return null;
                }
            }
            int nameCode = getTargetNamePool().allocate(parts[0], uri, lname);
            if (uri.equals(NamespaceConstant.SCHEMA)) {
                SchemaType t = BuiltInSchemaFactory.getSchemaType(StandardNames.getFingerprint(uri, lname));
                if (t == null) {
                    compileError("Unknown built-in type " + typeAtt);
                    return null;
                }
                return t;
            } else if (NamespaceConstant.isXDTNamespace(uri)) {
                ItemType t = Type.getBuiltInItemType(NamespaceConstant.XDT, lname);
                if (t == null) {
                    if ("untyped".equals(lname)) {
                        compileError("Cannot validate a node as 'untyped'");
                    } else {
                        compileError("Unknown built-in type " + typeAtt);
                    }
                }
                return (BuiltInAtomicType)t;
            }

            // not a built-in type: look in the imported schemas

            if (!getPrincipalStylesheet().isImportedSchema(uri)) {
                compileError("There is no imported schema for the namespace of type " + typeAtt);
                return null;
            }
            SchemaType stype = getConfiguration().getSchemaType(nameCode & 0xfffff);
            if (stype == null) {
                compileError("There is no type named " + typeAtt + " in an imported schema");
            }
            return stype;

        } catch (QNameException err) {
            compileError("Invalid type name. " + err.getMessage());
        }
        return null;
    }

    /**
     * Get the type annotation to use for a given schema type
     */

    public int getTypeAnnotation(SchemaType schemaType) {
        if (schemaType != null) {
            return schemaType.getFingerprint();
        } else {
            return -1;
        }
    }

    /**
     * Check that the stylesheet element is valid. This is called once for each element, after
     * the entire tree has been built. As well as validation, it can perform first-time
     * initialisation. The default implementation does nothing; it is normally overriden
     * in subclasses.
     */

    public void validate() throws XPathException {
    }

    /**
     * Hook to allow additional validation of a parent element immediately after its
     * children have been validated.
     */

    public void postValidate() throws XPathException {
    }

    /**
     * Type-check an expression. This is called to check each expression while the containing
     * instruction is being validated. It is not just a static type-check, it also adds code
     * to perform any necessary run-time type checking and/or conversion. It also allocates slot numbers
     * for range variables declared within the expression.
     */

    // TODO: we're doing two stages of type-checking, often redundantly: one at the level of individual XPath
    // expressions, one at the function/template level. We can't do it all at the function/template level because
    // the static context (e.g. namespaces) changes from one XPath expression to another. It really needs to
    // be split into two phases.

    public Expression typeCheck(String name, Expression exp) throws XPathException {

        if (exp == null) {
            return null;
        }
        
        ExpressionTool.makeParentReferences(exp);
        if (exp instanceof ComputedExpression) {
            ((ComputedExpression)exp).setParentExpression(this);
            // temporary, until the instruction is compiled
        }

        try {
            exp = exp.analyze(staticContext, Type.ITEM_TYPE);
            if (explaining) {
                System.err.println("Attribute '" + name + "' of element '" + getDisplayName() + "' at line " + getLineNumber() + ':');
                System.err.println("Static type: " +
                        SequenceType.makeSequenceType(exp.getItemType(), exp.getCardinality()));
                System.err.println("Optimized expression tree:");
                exp.display(10, getNamePool(), System.err);
            }
            if (getConfiguration().getTraceListener() != null) {
                InstructionDetails details = new InstructionDetails();
                details.setConstructType(Location.XPATH_IN_XSLT);
                details.setLineNumber(getLineNumber());
                details.setSystemId(getSystemId());
                details.setProperty("attribute-name", name);
                TraceWrapper trace = new TraceInstruction(exp, details);
                trace.setLocationId(allocateLocationId(getSystemId(), getLineNumber()));
                trace.setParentExpression(this);
                exp = trace;
            }
            //allocateSlots(exp);
            return exp;
        } catch (DynamicError err) {
            // we can't report a dynamic error such as divide by zero unless the expression
            // is actually executed.
            if (err.isTypeError()) {
                compileError(err);
                return exp;
            } else {
                ErrorExpression erexp = new ErrorExpression(err);
                erexp.setLocationId(allocateLocationId(getSystemId(), getLineNumber()));
                return erexp;
            }
        } catch (XPathException err) {
            compileError(err);
            ErrorExpression erexp = new ErrorExpression(err);
            erexp.setLocationId(allocateLocationId(getSystemId(), getLineNumber()));
            return erexp;
        }
    }

    /**
     * Allocate slots in the local stack frame to range variables used in an XPath expression
     *
     * @param exp the XPath expression for which slots are to be allocated
     */

    public void allocateSlots(Expression exp) {
        SlotManager slotManager = getContainingSlotManager();
        if (slotManager == null) {
            throw new AssertionError("Slot manager has not been allocated");
            // previous code: ExpressionTool.allocateSlots(exp, 0, null);
        } else {
            int firstSlot = slotManager.getNumberOfVariables();
            int highWater = ExpressionTool.allocateSlots(exp, firstSlot, slotManager);
            if (highWater > firstSlot) {
                slotManager.setNumberOfVariables(highWater);
                // This algorithm is not very efficient because it never reuses
                // a slot when a variable goes out of scope. But at least it is safe.
                // Note that range variables within XPath expressions need to maintain
                // a slot until the instruction they are part of finishes, e.g. in
                // xsl:for-each.
            }
        }
    }

    /**
     * Type-check a pattern. This is called to check each pattern while the containing
     * instruction is being validated. It is not just a static type-check, it also adds code
     * to perform any necessary run-time type checking and/or conversion.
     */

    public Pattern typeCheck(String name, Pattern pat) throws XPathException {
        if (pat == null) {
            return null;
        }
        try {
            return pat.analyze(staticContext, Type.NODE_TYPE);
        } catch (DynamicError err) {
            // we can't report a dynamic error such as divide by zero unless the pattern
            // is actually executed. We don't have an error pattern available, so we
            // construct one
            LocationPathPattern errpat = new LocationPathPattern();
            errpat.addFilter(new ErrorExpression(err));
            return errpat;
        } catch (XPathException err) {
            StaticError e2 = new StaticError("Error in " + name + " pattern", err);
            e2.setLocator(err.getLocator());
            e2.setErrorCode(err.getErrorCodeLocalPart());
            throw e2;
        }
    }

    /**
     * Fix up references from XPath expressions. Overridden for function declarations
     * and variable declarations
     */

    public void fixupReferences() throws XPathException {
        AxisIterator kids = iterateAxis(Axis.CHILD);
        while (true) {
            NodeInfo child = (NodeInfo)kids.next();
            if (child == null) {
                return;
            }
            if (child instanceof StyleElement) {
                ((StyleElement)child).fixupReferences();
            }
        }
    }

    /**
     * Get the SlotManager for the containing Procedure definition
     *
     * @return the SlotManager associated with the containing Function, Template, etc,
     *         or null if there is no such containing Function, Template etc.
     */

    public SlotManager getContainingSlotManager() {
        NodeInfo node = this;
        while (true) {
            NodeInfo next = node.getParent();
            if (next instanceof XSLStylesheet) {
                if (node instanceof StylesheetProcedure) {
                    return ((StylesheetProcedure)node).getSlotManager();
                } else {
                    return null;
                }
            }
            node = next;
        }
    }


    /**
     * Recursive walk through the stylesheet to validate all nodes
     */

    public void validateSubtree() throws XPathException {
        if (validationError != null) {
            if (reportingCircumstances == REPORT_ALWAYS) {
                compileError(validationError);
            } else if (reportingCircumstances == REPORT_UNLESS_FORWARDS_COMPATIBLE
                    && !forwardsCompatibleModeIsEnabled()) {
                compileError(validationError);
            }
        }
        try {
            validate();
        } catch (XPathException err) {
            if (forwardsCompatibleModeIsEnabled()) {
                setValidationError(err, REPORT_IF_INSTANTIATED);
            } else {
                compileError(err);
            }
        }

        validateChildren();
        postValidate();
    }

    /**
     * Validate the children of this node, recursively. Overridden for top-level
     * data elements.
     */

    protected void validateChildren() throws XPathException {
        boolean containsInstructions = mayContainSequenceConstructor();
        AxisIterator kids = iterateAxis(Axis.CHILD);
        StyleElement lastChild = null;
        while (true) {
            NodeInfo child = (NodeInfo)kids.next();
            if (child == null) {
                break;
            }
            if (child instanceof StyleElement) {
                if (containsInstructions && !((StyleElement)child).isInstruction()
                        && !isPermittedChild((StyleElement)child)) {
                    ((StyleElement)child).compileError(
                            "An " + getDisplayName() + " element must not contain an " +
                            child.getDisplayName() + " element", "XTSE0010");
                }
                ((StyleElement)child).validateSubtree();
                lastChild = (StyleElement)child;
            }
        }
        if (lastChild instanceof XSLVariable &&
                !(this instanceof XSLStylesheet)) {
            lastChild.compileWarning(
                    "A variable with no following sibling instructions has no effect",
                    SaxonErrorCode.SXWN9001);
        }
    }

    /**
     * Specify that certain children are permitted for this element
     */

    protected boolean isPermittedChild(StyleElement child) {
        return false;
    }

    /**
     * Get the principal XSLStylesheet node. This gets the principal style sheet, i.e. the
     * one originally loaded, that forms the root of the import/include tree
     */

    protected XSLStylesheet getPrincipalStylesheet() {
        XSLStylesheet sheet = getContainingStylesheet();
        while (true) {
            XSLStylesheet next = sheet.getImporter();
            if (next == null) {
                return sheet;
            }
            sheet = next;
        }
    }

    /**
     * Get the PreparedStylesheet object.
     *
     * @return the PreparedStylesheet to which this stylesheet element belongs
     */

    public PreparedStylesheet getPreparedStylesheet() {
        return getPrincipalStylesheet().getPreparedStylesheet();
    }

    /**
     * Check that the stylesheet element is within a sequence constructor
     *
     * @throws XPathException
     *          if not within a sequence constructor
     */

    public void checkWithinTemplate() throws XPathException {
//        Parent elements now check their children, not the other way around
//        StyleElement parent = (StyleElement)getParent();
//        if (!parent.mayContainSequenceConstructor()) {
//            compileError("Element must be used only within a sequence constructor", "XT0010");
//        }
    }

    /**
     * Check that among the children of this element, any xsl:sort elements precede any other elements
     *
     * @param sortRequired true if there must be at least one xsl:sort element
     * @throws XPathException
     *          if invalid
     */

    protected void checkSortComesFirst(boolean sortRequired) throws XPathException {
        AxisIterator kids = iterateAxis(Axis.CHILD);
        boolean sortFound = false;
        boolean nonSortFound = false;
        while (true) {
            NodeInfo child = (NodeInfo)kids.next();
            if (child == null) {
                break;
            }
            if (child instanceof XSLSort) {
                if (nonSortFound) {
                    ((XSLSort)child).compileError("Within " + getDisplayName() +
                            ", xsl:sort elements must come before other instructions", "XTSE0010");
                }
                sortFound = true;
            } else if (child.getNodeKind() == Type.TEXT) {
                // with xml:space=preserve, white space nodes may still be there
                if (!Navigator.isWhite(child.getStringValueCS())) {
                    nonSortFound = true;
                }
            } else {
                nonSortFound = true;
            }
        }
        if (sortRequired && !sortFound) {
            compileError(getDisplayName() + " must have at least one xsl:sort child", "XTSE0010");
        }
    }

    /**
     * Convenience method to check that the stylesheet element is at the top level
     *
     * @throws XPathException
     *          if not at top level
     */

    public void checkTopLevel(String errorCode) throws XPathException {
        if (errorCode == null) {
            errorCode = "XTSE0010";
        }
        if (!(getParent() instanceof XSLStylesheet)) {
            compileError("Element must be used only at top level of stylesheet", errorCode);
        }
    }

    /**
     * Convenience method to check that the stylesheet element is empty
     *
     * @throws XPathException
     *          if it is not empty
     */

    public void checkEmpty() throws XPathException {
        if (hasChildNodes()) {
            compileError("Element must be empty", "XTSE0260");
        }
    }

    /**
     * Convenience method to report the absence of a mandatory attribute
     *
     * @throws XPathException
     *          if the attribute is missing
     */

    public void reportAbsence(String attribute)
            throws XPathException {
        compileError("Element must have a \"" + attribute + "\" attribute", "XTSE0010");
    }


    /**
     * Compile the instruction on the stylesheet tree into an executable instruction
     * for use at run-time.
     *
     * @return either a ComputedExpression, or null. The value null is returned when compiling an instruction
     *         that returns a no-op, or when compiling a top-level object such as an xsl:template that compiles
     *         into something other than an instruction.
     */

    public abstract Expression compile(Executable exec) throws XPathException;

    /**
     * Compile the children of this instruction on the stylesheet tree, adding the
     * subordinate instructions to the parent instruction on the execution tree.
     *
     * @return the array of children
     */

    public Expression compileSequenceConstructor(Executable exec, AxisIterator iter, boolean includeParams)
            throws XPathException {

        int lineNumber = getLineNumber();
        NodeInfo node = (NodeInfo)iter.next();
        if (node == null) {
            return null;
        }
        if (node instanceof StyleElement) {
            lineNumber = node.getLineNumber();  // this is to get a line number for the next text node
        }
        if (node.getNodeKind() == Type.TEXT) {
            // handle literal text nodes by generating an xsl:value-of instruction
            ValueOf text = new ValueOf(new StringValue(node.getStringValueCS()), false);
            text.setLocationId(allocateLocationId(getSystemId(), lineNumber));
            Expression tail = compileSequenceConstructor(exec, iter, includeParams);
            if (tail == null) {
                return text;
            } else {
                Block e = Block.makeBlock(text, tail);
                e.setLocationId(allocateLocationId(getSystemId(), getLineNumber()));
                return e;
            }

        } else if (node instanceof XSLVariable) {
            Expression var = ((XSLVariable)node).newCompile(exec);
            if (var == null) {
                // this can happen if the variable declaration is redundant
                return compileSequenceConstructor(exec, iter, includeParams);
            } else {
                LocalVariable lv = (LocalVariable)var;
                Expression tail = compileSequenceConstructor(exec, iter, includeParams);
                if (tail == null) {
                    // this doesn't happen, because if there are no instructions following
                    // a variable, we'll have taken the var==null path above
                    return null;
                } else {
                    LetExpression let = new LetExpression();
                    RangeVariableDeclaration rvar = new RangeVariableDeclaration();
                    rvar.setRequiredType(lv.getRequiredType());
                    rvar.setNameCode(lv.getNameCode());
                    rvar.setVariableName(lv.getVariableName());
                    rvar.setReferenceList(((XSLVariable)node).getReferenceList());
                    let.setVariableDeclaration(rvar);
                    let.setSequence(lv.getSelectExpression());
                    //let.setSlotNumber(lv.getSlotNumber());
                    let.setAction(tail);
                    ((XSLVariable)node).fixupBinding(let);
                    let.setLocationId(allocateLocationId(node.getSystemId(), node.getLineNumber()));
                    if (getConfiguration().getTraceListener() != null) {
                        TraceExpression t = new TraceExpression(let);
                        t.setConstructType(Location.LET_EXPRESSION);
                        t.setObjectNameCode(lv.getNameCode());
                        t.setSystemId(node.getSystemId());
                        t.setLineNumber(node.getLineNumber());
                        return t;
                    }
                    return let;
                }
            }


        } else if (node instanceof StyleElement) {
            StyleElement snode = (StyleElement)node;
            if (snode.validationError != null) {
                return fallbackProcessing(exec, snode);

            } else {
                Expression child = snode.compile(exec);
                if (child instanceof ComputedExpression) {
                    ComputedExpression childi = (ComputedExpression)child;
                    childi.setLocationId(allocateLocationId(getSystemId(), snode.getLineNumber()));
                }
                if (child != null) {
                    if (includeParams || !(node instanceof XSLParam)) {
                        if (getConfiguration().getTraceListener() != null) {
                            TraceWrapper trace = makeTraceInstruction(snode, child);
                            child = trace;
                        }
                    }
                }
                Expression tail = compileSequenceConstructor(exec, iter, includeParams);
                if (tail == null) {
                    return child;
                } else if (child == null) {
                    return tail;
                } else {
                    //AppendExpression e = new AppendExpression(child, Token.COMMA, tail);
                    Block e = Block.makeBlock(child, tail);
                    e.setLocationId(allocateLocationId(getSystemId(), getLineNumber()));
                    return e;
                }
            }
        }
        return null;
    }

    /**
     * Create a trace instruction to wrap a real instruction
     */

    protected static TraceWrapper makeTraceInstruction(StyleElement source, Expression child) {
        if (child instanceof TraceWrapper) {
            return (TraceWrapper)child;
            // this can happen, for example, after optimizing a compile-time xsl:if
        }

        TraceWrapper trace = new TraceInstruction(child, source);
        trace.setLocationId(source.allocateLocationId(source.getSystemId(), source.getLineNumber()));
        return trace;
    }

    /**
     * Perform fallback processing. Generate fallback code for an extension
     * instruction that is not recognized by the implementation.
     *
     * @param instruction The unknown extension instruction
     */

    protected Expression fallbackProcessing(Executable exec, StyleElement instruction)
            throws XPathException {
        // process any xsl:fallback children; if there are none,
        // generate code to report the original failure reason
        Expression fallback = null;
        AxisIterator kids = instruction.iterateAxis(Axis.CHILD);
        while (true) {
            NodeInfo child = (NodeInfo)kids.next();
            if (child == null) {
                break;
            }
            if (child instanceof XSLFallback) {
                //fallback.setLocationId(allocateLocationId(getSystemId(), child.getLineNumber()));
                //((XSLFallback)child).compileChildren(exec, fallback, true);
                Expression b = ((XSLFallback)child).compileSequenceConstructor(exec, child.iterateAxis(Axis.CHILD), true);
                if (b == null) {
                    b = EmptySequence.getInstance();
                }
                if (fallback == null) {
                    fallback = b;
                } else {
                    //fallback = new AppendExpression(fallback, Token.COMMA,  b);
                    fallback = Block.makeBlock(fallback, b);
                }
            }
        }
        if (fallback != null) {
            return fallback;
        } else {
            DeferredError deferred = new DeferredError(instruction.getNameCode(), instruction.validationError);
            deferred.setLocationId(allocateLocationId(getSystemId(), getLineNumber()));
            deferred.setParentExpression(this);
            return deferred;
        }

    }

    /**
     * Allocate a location identifier
     */

    public int allocateLocationId(String systemId, int lineNumber) {
        return getStaticContext().getLocationMap().allocateLocationId(systemId, lineNumber);
    }

    /**
     * Construct sort keys for a SortedIterator
     *
     * @return an array of SortKeyDefinition objects if there are any sort keys;
     *         or null if there are none.
     */

    protected SortKeyDefinition[] makeSortKeys() {
        // handle sort keys if any

        int numberOfSortKeys = 0;
        AxisIterator kids = iterateAxis(Axis.CHILD);
        while (true) {
            Item child = kids.next();
            if (child == null) {
                break;
            }
            if (child instanceof XSLSort) {
                numberOfSortKeys++;
            }
        }

        if (numberOfSortKeys > 0) {
            SortKeyDefinition[] keys = new SortKeyDefinition[numberOfSortKeys];
            kids = iterateAxis(Axis.CHILD);
            int k = 0;
            while (true) {
                NodeInfo child = (NodeInfo)kids.next();
                if (child == null) {
                    break;
                }
                if (child instanceof XSLSort) {
                    keys[k++] = ((XSLSort)child).getSortKeyDefinition();
                }
            }
            return keys;

        } else {
            return null;
        }
    }

    /**
     * Get the list of attribute-sets associated with this element.
     * This is used for xsl:element, xsl:copy, xsl:attribute-set, and on literal
     * result elements
     *
     * @param use  the original value of the [xsl:]use-attribute-sets attribute
     * @param list an empty list to hold the list of XSLAttributeSet elements in the stylesheet tree.
     *              Or null, if these are not required.
     * @return an array of AttributeList instructions representing the compiled attribute sets
     */

    protected AttributeSet[] getAttributeSets(String use, List list)
            throws XPathException {

        if (list == null) {
            list = new ArrayList(4);
        }

        XSLStylesheet stylesheet = getPrincipalStylesheet();
        List toplevel = stylesheet.getTopLevel();

        StringTokenizer st = new StringTokenizer(use);
        while (st.hasMoreTokens()) {
            String asetname = st.nextToken();
            int fprint;
            try {
                fprint = makeNameCode(asetname) & 0xfffff;
            } catch (NamespaceException err) {
                compileError(err.getMessage(), "XTSE0710");
                fprint = -1;
            } catch (XPathException err) {
                compileError(err.getMessage(), "XTSE0710");
                fprint = -1;
            }
            boolean found = false;

            // search for the named attribute set, using all of them if there are several with the
            // same name

            for (int i = 0; i < toplevel.size(); i++) {
                if (toplevel.get(i) instanceof XSLAttributeSet) {
                    XSLAttributeSet t = (XSLAttributeSet)toplevel.get(i);
                    if (t.getAttributeSetFingerprint() == fprint) {
                        list.add(t);
                        found = true;
                    }
                }
            }

            if (!found) {
                compileError("No attribute-set exists named " + asetname, "XTSE0710");
            }
        }

        AttributeSet[] array = new AttributeSet[list.size()];
        for (int i = 0; i < list.size(); i++) {
            XSLAttributeSet aset = (XSLAttributeSet)list.get(i);
            aset.incrementReferenceCount();
            array[i] = aset.getInstruction();
        }
        return array;
    }

    /**
     * Get the list of xsl:with-param elements for a calling element (apply-templates,
     * call-template, apply-imports, next-match). This method can be used to get either
     * the tunnel parameters, or the non-tunnel parameters.
     *
     * @param tunnel true if the tunnel="yes" parameters are wanted, false to get
     * @param caller
     */

    protected WithParam[] getWithParamInstructions(Executable exec, boolean tunnel, Instruction caller)
    throws XPathException {
        int count = 0;
        AxisIterator kids = iterateAxis(Axis.CHILD);
        while (true) {
            NodeInfo child = (NodeInfo)kids.next();
            if (child == null) {
                break;
            }
            if (child instanceof XSLWithParam) {
                XSLWithParam wp = (XSLWithParam)child;
                if (wp.isTunnelParam() == tunnel) {
                    count++;
                }
            }
        }
        WithParam[] array = new WithParam[count];
        count = 0;
        kids = iterateAxis(Axis.CHILD);
        while (true) {
            NodeInfo child = (NodeInfo)kids.next();
            if (child == null) {
                return array;
            }
            if (child instanceof XSLWithParam) {
                XSLWithParam wp = (XSLWithParam)child;
                if (wp.isTunnelParam() == tunnel) {
                    WithParam p = (WithParam)wp.compile(exec);
                    p.setParentExpression(caller);
                    array[count++] = p;
                }

            }
        }
    }

    /**
     * Construct an exception with diagnostic information
     */

    protected void compileError(TransformerException error)
            throws XPathException {

        // Set the location of the error if there is not current location information,
        // or if the current location information is local to the XPath expression
        if (error.getLocator() == null || error.getLocator() instanceof ExpressionLocation) {
            error.setLocator(this);
        }
        PreparedStylesheet pss = getPreparedStylesheet();
        try {
            if (pss == null) {
                // it is null before the stylesheet has been fully built
                throw error;
            } else {
                pss.reportError(error);
            }
        } catch (TransformerException err2) {
            if (err2.getLocator() == null) {
                err2.setLocator(this);
            }
            throw StaticError.makeStaticError(err2);
        }
    }

    protected void compileError(String message)
            throws XPathException {
        StaticError tce = new StaticError(message);
        tce.setLocator(this);
        compileError(tce);
    }

    /**
     * Compile time error, specifying an error code
     *
     * @param message   the error message
     * @param errorCode the error code. May be null if not known or not defined
     * @throws XPathException
     *
     */

    protected void compileError(String message, String errorCode) throws XPathException {
        StaticError tce = new StaticError(message);
        tce.setErrorCode(errorCode);
        tce.setLocator(this);
        compileError(tce);
    }

    protected void undeclaredNamespaceError(String prefix, String errorCode) throws XPathException {
        if (errorCode==null) {
            errorCode = "XTSE0280";
        }
        compileError("Undeclared namespace prefix " + Err.wrap(prefix), errorCode);
    }

    protected void compileWarning(String message, String errorCode)
            throws XPathException {
        StaticError tce = new StaticError(message);
        tce.setErrorCode(errorCode);
        tce.setLocator(this);
        PreparedStylesheet pss = getPreparedStylesheet();
        if (pss != null) {
            pss.reportWarning(tce);
        }
    }

    /**
     * Construct an exception with diagnostic information
     */

    protected void issueWarning(TransformerException error) {
        if (error.getLocator() == null) {
            error.setLocator(this);
        }
        PreparedStylesheet pss = getPreparedStylesheet();
        if (pss != null) {
            // it is null before the stylesheet has been fully built - ignore it
            pss.reportWarning(error);
        }
    }

    protected void issueWarning(String message, SourceLocator locator) {
        TransformerConfigurationException tce =
                new TransformerConfigurationException(message);
        if (locator == null) {
            tce.setLocator(this);
        } else {
            tce.setLocator(locator);
        }
        issueWarning(tce);
    }

    /**
     * Test whether this is a top-level element
     */

    public boolean isTopLevel() {
        return (getParent() instanceof XSLStylesheet);
    }

    /**
     * Bind a variable used in this element to the compiled form of the XSLVariable element in which it is
     * declared
     *
     * @param fingerprint The fingerprint of the name of the variable
     * @return the XSLVariableDeclaration (that is, an xsl:variable or xsl:param instruction) for the variable
     * @throws net.sf.saxon.trans.StaticError if the variable has not been declared
     */

    public XSLVariableDeclaration bindVariable(int fingerprint) throws StaticError {
        XSLVariableDeclaration binding = getVariableBinding(fingerprint);
        if (binding == null) {
            StaticError err = new StaticError("Variable " + getTargetNamePool().getDisplayName(fingerprint) +
                    " has not been declared");
            err.setErrorCode("XPST0008");
            throw err;
        }
        return binding;
    }

    /**
     * Bind a variable used in this element to the declaration in the stylesheet
     *
     * @param fprint The absolute name of the variable (prefixed by namespace URI)
     * @return the XSLVariableDeclaration, or null if it has not been declared
     */

    private XSLVariableDeclaration getVariableBinding(int fprint) {
        NodeInfo curr = this;
        NodeInfo prev = this;

        // first search for a local variable declaration
        if (!isTopLevel()) {
            AxisIterator preceding = curr.iterateAxis(Axis.PRECEDING_SIBLING);
            while (true) {
                curr = (NodeInfo)preceding.next();
                while (curr == null) {
                    curr = prev.getParent();
                    while (curr instanceof XSLFallback) {
                        // a local variable is not visible within a sibling xsl:fallback element
                        curr = curr.getParent();
                    }
                    prev = curr;
                    if (curr.getParent() instanceof XSLStylesheet) {
                        break;   // top level
                    }
                    preceding = curr.iterateAxis(Axis.PRECEDING_SIBLING);
                    curr = (NodeInfo)preceding.next();
                }
                if (curr.getParent() instanceof XSLStylesheet) {
                    break;
                }
                if (curr instanceof XSLVariableDeclaration) {
                    XSLVariableDeclaration var = (XSLVariableDeclaration)curr;
                    if (var.getVariableFingerprint() == fprint) {
                        return var;
                    }
                }
            }
        }

        // Now check for a global variable
        // we rely on the search following the order of decreasing import precedence.

        XSLStylesheet root = getPrincipalStylesheet();
        XSLVariableDeclaration var = root.getGlobalVariable(fprint);
        return var;
    }

    /**
     * List the variables that are in scope for this stylesheet element.
     * Designed for a debugger, not used by the processor.
     * @return two Enumeration of Strings, the global ones [0] and the local ones [1]
     */



    /**
     * Get a FunctionCall declared using an xsl:function element in the stylesheet
     *
     * @param fingerprint the fingerprint of the name of the function
     * @param arity       the number of arguments in the function call. The value -1
     *                    indicates that any arity will do (this is used to support the function-available() function).
     * @return the XSLFunction object representing the function declaration
     *         in the stylesheet, or null if no such function is defined.
     */

    public XSLFunction getStylesheetFunction(int fingerprint, int arity) {

        // we rely on the search following the order of decreasing import precedence.

        XSLStylesheet root = getPrincipalStylesheet();
        List toplevel = root.getTopLevel();
        for (int i = toplevel.size() - 1; i >= 0; i--) {
            Object child = toplevel.get(i);
            if (child instanceof XSLFunction &&
                    ((XSLFunction)child).getFunctionFingerprint() == fingerprint &&
                    (arity == -1 || ((XSLFunction)child).getNumberOfArguments() == arity)) {
                XSLFunction func = (XSLFunction)child;
                return func;
            }
        }
        return null;
    }

    /**
     * Get the type of construct. This will be a constant in
     * class {@link Location}. This method is part of the {@link InstructionInfo} interface
     */

    public int getConstructType() {
        return getFingerprint();
    }

    /**
     * Get the name of the instruction. This is applicable only when the construct type
     * is Location.INSTRUCTION.
     */

    public int getInstructionFingerprint() {
        return getFingerprint();
    }

    /**
     * Get a name identifying the object of the expression, for example a function name, template name,
     * variable name, key name, element name, etc. This is used only where the name is known statically.
     * If there is no name, the value will be -1.
     */

    public int getObjectNameCode() {
        return objectNameCode;
    }

    /**
     * Get a fingerprint identifying the object of the expression, for example a function name, template name,
     * variable name, key name, element name, etc. This is used only where the name is known statically.
     * If there is no name, the value will be -1.
     */

    public int getObjectFingerprint() {
        return (objectNameCode == -1 ? -1 : objectNameCode & 0xfffff);
    }

    /**
     * Set the object name code
     */

    public void setObjectNameCode(int nameCode) {
        objectNameCode = nameCode;
    }

    /**
     * Get the namespace context of the instruction.
     */

    public NamespaceResolver getNamespaceResolver() {
        return makeNamespaceContext();
    }

    /**
     * Get the value of a particular property of the instruction. This is part of the
     * {@link InstructionInfo} interface for run-time tracing and debugging. The properties
     * available include all the attributes of the source instruction (named by the attribute name):
     * these are all provided as string values.
     *
     * @param name The name of the required property
     * @return The value of the requested property, or null if the property is not available
     */

    public Object getProperty(String name) {
        return getAttributeValue(name);
    }

    /**
     * Get an iterator over all the properties available. The values returned by the iterator
     * will be of type String, and each string can be supplied as input to the getProperty()
     * method to retrieve the value of the property.
     */

    public Iterator getProperties() {
        NamePool pool = getNamePool();
        List list = new ArrayList(10);
        AxisIterator it = iterateAxis(Axis.ATTRIBUTE);
        while (true) {
            NodeInfo a = (NodeInfo)it.next();
            if (a == null) {
                break;
            }
            list.add(pool.getClarkName(a.getNameCode()));
        }
        return list.iterator();
    }

    public String getSystemId(int locationId) {
        return getSystemId();
    }

    public int getLineNumber(int locationId) {
        return getLineNumber();
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
//
