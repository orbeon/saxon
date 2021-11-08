package org.orbeon.saxon.style;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.trans.Err;
import org.orbeon.saxon.PreparedStylesheet;
import org.orbeon.saxon.event.LocationProvider;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.functions.Current;
import org.orbeon.saxon.instruct.*;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.pattern.*;
import org.orbeon.saxon.sort.SortKeyDefinition;
import org.orbeon.saxon.trace.InstructionInfo;
import org.orbeon.saxon.trace.Location;
import org.orbeon.saxon.trans.SaxonErrorCode;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.tree.ElementImpl;
import org.orbeon.saxon.type.*;
import org.orbeon.saxon.value.DecimalValue;
import org.orbeon.saxon.value.SequenceType;
import org.orbeon.saxon.value.Whitespace;
import org.xml.sax.Locator;

import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
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

public abstract class StyleElement extends ElementImpl
        implements Locator, Container, InstructionInfo {

    protected short[] extensionNamespaces = null;		// a list of URI codes
    private short[] excludedNamespaces = null;		// a list of URI codes
    protected BigDecimal version = null;
    protected StaticContext staticContext = null;
    protected XPathException validationError = null;
    protected int reportingCircumstances = REPORT_ALWAYS;
    protected String defaultXPathNamespace = null;
    protected String defaultCollationName = null;
    private boolean explaining = false;
    // true if saxon:explain="yes"
    private StructuredQName objectName;
    // for instructions that define an XSLT named object, the name of that object
    private XSLStylesheet containingStylesheet;

    // Conditions under which an error is to be reported

    public static final int REPORT_ALWAYS = 1;
    public static final int REPORT_UNLESS_FORWARDS_COMPATIBLE = 2;
    public static final int REPORT_IF_INSTANTIATED = 3;
    public static final int REPORT_UNLESS_FALLBACK_AVAILABLE = 4;

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

    /**
     * Make an expression visitor
     * @return the expression visitor
     */

    public ExpressionVisitor makeExpressionVisitor() {
        ExpressionVisitor visitor = ExpressionVisitor.make(staticContext);
        visitor.setExecutable(getExecutable());
        return visitor;
    }

//    public int getLineNumber() {
//        return lineNumber;
//    }
//
//    public void setLineNumber(int lineNumber) {
//        this.lineNumber = lineNumber;
//    }

    /**
     * Determine whether saxon:explain has been set to "yes"
     * @return true if saxon:explain has been set to "yes" on this element
     */

    protected boolean isExplaining() {
        return explaining;
    }

    /**
     * Make this node a substitute for a temporary one previously added to the tree. See
     * StyleNodeFactory for details. "A node like the other one in all things but its class".
     * Note that at this stage, the node will not yet be known to its parent, though it will
     * contain a reference to its parent; and it will have no children.
     * @param temp the element which this one is substituting for
     */

    public void substituteFor(StyleElement temp) {
        parent = temp.parent;
        attributeList = temp.attributeList;
        namespaceList = temp.namespaceList;
        nameCode = temp.nameCode;
        sequence = temp.sequence;
        extensionNamespaces = temp.extensionNamespaces;
        excludedNamespaces = temp.excludedNamespaces;
        version = temp.version;
        staticContext = temp.staticContext;
        validationError = temp.validationError;
        reportingCircumstances = temp.reportingCircumstances;
        //lineNumber = temp.lineNumber;
    }

    /**
     * Set a validation error. This is an error detected during construction of this element on the
     * stylesheet, but which is not to be reported until later.
     * @param reason the details of the error
     * @param circumstances a code identifying the circumstances under which the error is to be reported
     */

    protected void setValidationError(TransformerException reason,
                                      int circumstances) {
        validationError = XPathException.makeXPathException(reason);
        reportingCircumstances = circumstances;
    }

    /**
     * Determine whether this node is an instruction. The default implementation says it isn't.
     * @return true if this element is an instruction
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
        final TypeHierarchy th = getConfiguration().getTypeHierarchy();
        ItemType t = EmptySequenceTest.getInstance();
        AxisIterator children = iterateAxis(Axis.CHILD);
        while (true) {
            NodeInfo next = (NodeInfo)children.next();
            if (next == null) {
                return t;
            }
            if (next instanceof StyleElement) {
                ItemType ret = ((StyleElement)next).getReturnedItemType();
                if (ret != null) {
                    t = Type.getCommonSuperType(t, ret, th);
                }
            } else {
                t = Type.getCommonSuperType(t, NodeKindTest.TEXT, th);
            }
            if (t == AnyItemType.getInstance()) {
                return t;       // no point looking any further
            }
        }
    }

    /**
     * Mark tail-recursive calls on templates and functions.
     * For most instructions, this returns false.
     * @return true if one or more tail calls were identified
     */

    protected boolean markTailCalls() {
        return false;
    }

    /**
     * Determine whether this type of element is allowed to contain a sequence constructor
     * @return true if this instruction is allowed to contain a sequence constructor
     */

    protected boolean mayContainSequenceConstructor() {
        return false;
    }

    /**
     * Determine whether this type of element is allowed to contain an xsl:fallback
     * instruction
     * @return true if this element is allowed to contain an xsl:fallback
     */

    protected boolean mayContainFallback() {
        return mayContainSequenceConstructor();
    }

    /**
     * Determine whether this type of element is allowed to contain an xsl:param element
     * @return true if this element is allowed to contain an xsl:param
     */

    protected boolean mayContainParam() {
        return false;
    }

    /**
     * Get the containing XSLStylesheet element
     * @return the XSLStylesheet element representing the outermost element of the containing
     * stylesheet module. Exceptionally, return null if there is no containing XSLStylesheet element
     */

    public XSLStylesheet getContainingStylesheet() {
        if (containingStylesheet == null) {
            if (this instanceof XSLStylesheet) {
                containingStylesheet = (XSLStylesheet)this;
            } else {
                NodeInfo parent = getParent();
                if (parent instanceof StyleElement) {
                    containingStylesheet = ((StyleElement)parent).getContainingStylesheet();
                } else {
                    // this can happen when early errors are detected in a simplified stylesheet,
                    return null;
                }
            }
        }
        return containingStylesheet;
    }

    /**
     * Get the import precedence of this stylesheet element.
     * @return the import precedence. The actual numeric value is arbitrary, but a higher number
     * indicates a higher precedence.
     */

    public int getPrecedence() {
        return getContainingStylesheet().getPrecedence();
    }

    /**
     * Make a structured QName, using this Element as the context for namespace resolution, and
     * registering the code in the namepool. If the name is unprefixed, the
     * default namespace is <b>not</b> used.
     *
     * @param lexicalQName The lexical QName as written, in the form "[prefix:]localname". The name must have
     *              already been validated as a syntactically-correct QName. Leading and trailing whitespace
     *              will be trimmed
     * @return the StructuredQName representation of this lexical QName
     * @throws XPathException     if the qname is not a lexically-valid QName, or if the name
     *                            is in a reserved namespace.
     * @throws NamespaceException if the prefix of the qname has not been declared
     */

    public final StructuredQName makeQName(String lexicalQName)
            throws XPathException, NamespaceException {

        StructuredQName qName;
        try {
            qName = StructuredQName.fromLexicalQName(lexicalQName, false,
                getConfiguration().getNameChecker(), this);
        } catch (XPathException e) {
            e.setIsStaticError(true);
            if ("FONS0004".equals(e.getErrorCodeLocalPart())) {
                e.setErrorCode("XTSE0280");
            } else if ("FOCA0002".equals(e.getErrorCodeLocalPart())) {
                e.setErrorCode("XTSE0020");
            } else if (e.getErrorCodeLocalPart() == null) {
                e.setErrorCode("XTSE0020");
            }
            throw e;
        }
        if (NamespaceConstant.isReserved(qName.getNamespaceURI())) {
            XPathException err = new XPathException("Namespace prefix " +
                    qName.getPrefix() + " refers to a reserved namespace");
            err.setIsStaticError(true);
            err.setErrorCode("XTSE0080");
            throw err;
        }
        return qName;
    }


    /**
     * Make a NamespaceContext object representing the list of in-scope namespaces. This will
     * be a copy of the namespace context with no references to objects in the stylesheet tree,
     * so that it can be kept until run-time without locking the tree down in memory.
     * @return a copy of the namespace context
     */

    public SavedNamespaceContext makeNamespaceContext() {
        return new SavedNamespaceContext(getInScopeNamespaceCodes(), getNamePool());
    }

    /**
     * Get the namespace context of the instruction.
     * @return the namespace context. This method does not make a copy of the namespace context,
     * so a reference to the returned NamespaceResolver will lock the stylesheet tree in memory.
     */

    public NamespaceResolver getNamespaceResolver() {
        return this;
    }

    /**
     * Process the attributes of this element and all its children
     * @throws XPathException in the event of a static error being detected
     */

    protected void processAllAttributes() throws XPathException {
        if (!(this instanceof LiteralResultElement)) {
            processDefaultCollationAttribute(StandardNames.DEFAULT_COLLATION);
        }
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
                if (((StyleElement)child).explaining) {
                    // saxon:explain on any element in a template/function now causes an explanation at the
                    // level of the template/function
                    explaining = true;
                }
            }
        }
    }

    /**
     * Get an attribute value given the Clark name of the attribute (that is,
     * the name in {uri}local format).
     * @param clarkName the name of the attribute in {uri}local format
     * @return the value of the attribute if it exists, or null otherwise
     */

    public String getAttributeValue(String clarkName) {
        int fp = getNamePool().allocateClarkName(clarkName);
        return getAttributeValue(fp);
    }

    /**
     * Process the attribute list for the element. This is a wrapper method that calls
     * prepareAttributes (provided in the subclass) and traps any exceptions
     */

    protected final void processAttributes() throws XPathException {
        try {
            prepareAttributes();
        } catch (XPathException err) {
            compileError(err);
        }
    }

    /**
     * Check whether an unknown attribute is permitted.
     *
     * @param nc The name code of the attribute name
     * @throws XPathException (and reports the error) if this is an attribute
     * that is not permitted on the containing element
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
                (clarkName.equals(StandardNames.DEFAULT_COLLATION) ||
                        clarkName.equals(StandardNames.XPATH_DEFAULT_NAMESPACE) ||
                        clarkName.equals(StandardNames.EXTENSION_ELEMENT_PREFIXES) ||
                        clarkName.equals(StandardNames.EXCLUDE_RESULT_PREFIXES) ||
                        clarkName.equals(StandardNames.VERSION) ||
                        clarkName.equals(StandardNames.USE_WHEN))) {
            return;
        }

        if ("".equals(attributeURI) || NamespaceConstant.XSLT.equals(attributeURI)) {
            compileError("Attribute " + Err.wrap(getNamePool().getDisplayName(nc), Err.ATTRIBUTE) +
                    " is not allowed on element " + Err.wrap(getDisplayName(), Err.ELEMENT), "XTSE0090");
        }
    }


    /**
     * Set the attribute list for the element. This is called to process the attributes (note
     * the distinction from processAttributes in the superclass).
     * Must be supplied in a subclass
     */

    protected abstract void prepareAttributes() throws XPathException;

    /**
     * Find the last child instruction of this instruction. Returns null if
     * there are no child instructions, or if the last child is a text node.
     * @return the last child instruction, or null if there are no child instructions
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
     * Compile an XPath expression in the context of this stylesheet element
     * @param expression the source text of the XPath expression
     * @return the compiled expression tree for the XPath expression
     */

    public Expression makeExpression(String expression)
            throws XPathException {
        try {
            return ExpressionTool.make(expression,
                    staticContext,
                    0, Token.EOF,
                    getLineNumber(),
                    getPreparedStylesheet().isCompileWithTracing());
        } catch (XPathException err) {
            err.setLocator(this);
            compileError(err);
            ErrorExpression erexp = new ErrorExpression(err);
            erexp.setLocationId(allocateLocationId(getSystemId(), getLineNumber()));
            erexp.setContainer(this);
            return erexp;
        }
    }

    /**
     * Make a pattern in the context of this stylesheet element
     * @param pattern the source text of the pattern
     * @return the compiled pattern
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
     * @param expression the source text of the attribute value template
     * @return a compiled XPath expression that computes the value of the attribute (including
     * concatenating the results of embedded expressions with any surrounding fixed text)
     */

    protected Expression makeAttributeValueTemplate(String expression)
            throws XPathException {
        try {
            return AttributeValueTemplate.make(expression, getLineNumber(), staticContext);
        } catch (XPathException err) {
            compileError(err);
            return new StringLiteral(expression);
        }
    }

    /**
     * Process an attribute whose value is a SequenceType
     * @param sequenceType the source text of the attribute
     * @return the processed sequence type
     * @throws XPathException if the syntax is invalid or for example if it refers to a type
     * that is not in the static context
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
            StringTokenizer st1 = new StringTokenizer(ext, " \t\n\r", false);
            while (st1.hasMoreTokens()) {
                st1.nextToken();
                count++;
            }
            extensionNamespaces = new short[count];
            count = 0;
            StringTokenizer st2 = new StringTokenizer(ext, " \t\n\r", false);
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
                    compileError(err.getMessage(), "XTSE1430");
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
            if ("#all".equals(Whitespace.trim(ext))) {
                int[] codes = getInScopeNamespaceCodes();
                excludedNamespaces = new short[codes.length];
                for (int i = 0; i < codes.length; i++) {
                    excludedNamespaces[i] = (short)(codes[i] & 0xffff);
                }
            } else {
                // go round twice, once to count the values and next to add them to the array
                int count = 0;
                StringTokenizer st1 = new StringTokenizer(ext, " \t\n\r", false);
                while (st1.hasMoreTokens()) {
                    st1.nextToken();
                    count++;
                }
                excludedNamespaces = new short[count];
                count = 0;
                StringTokenizer st2 = new StringTokenizer(ext, " \t\n\r", false);
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
                        if (s.length() == 0 && uriCode==0) {
                            compileError("Cannot exclude the #default namespace when no default namespace is declared",
                                    "XTSE0809");
                        }
                    } catch (NamespaceException err) {
                        excludedNamespaces = null;
                        compileError(err.getMessage(), "XTSE0808");
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
        String v = Whitespace.trim(getAttributeValue(nc));
        if (v != null) {
            ConversionResult val = DecimalValue.makeDecimalValue(v, true);
            if (val instanceof ValidationFailure) {
                compileError("The version attribute must be a decimal literal", "XTSE0110");
                version = new BigDecimal("2.0");
            } else {
                version = ((DecimalValue)val).getDecimalValue();
            }
        }
    }

    /**
     * Get the numeric value of the version number on this element,
     * or inherited from its ancestors
     * @return the version number as a decimal
     */

    public BigDecimal getVersion() {
        if (version == null) {
            NodeInfo node = getParent();
            if (node instanceof StyleElement) {
                version = ((StyleElement)node).getVersion();
            } else {
                return new BigDecimal("2.0");    // defensive programming
            }
        }
        return version;
    }

    /**
     * Determine whether forwards-compatible mode is enabled for this element
     * @return true if forwards-compatible mode is enabled
     */

    public boolean forwardsCompatibleModeIsEnabled() {
        return getVersion().compareTo(BigDecimal.valueOf(2)) > 0;
    }

    /**
     * Determine whether backwards-compatible mode is enabled for this element
     * @return true if backwards compatable mode is enabled, that is, if this or an enclosing
     * element specifies [xsl:]version="1.0"
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
            StringTokenizer st = new StringTokenizer(v, " \t\n\r", false);
            while (st.hasMoreTokens()) {
                String uri = st.nextToken();
                if (uri.equals(NamespaceConstant.CODEPOINT_COLLATION_URI)) {
                    defaultCollationName = uri;
                    return;
                } else if (uri.startsWith("http://saxon.sf.net/")) {
                    defaultCollationName = uri;
                    return;
                } else {
                    URI collationURI;
                    try {
                        collationURI = new URI(uri);
                        if (!collationURI.isAbsolute()) {
                            URI base = new URI(getBaseURI());
                            collationURI = base.resolve(collationURI);
                            uri = collationURI.toString();
                        }
                    } catch (URISyntaxException err) {
                        compileError("default collation '" + uri + "' is not a valid URI");
                        uri = NamespaceConstant.CODEPOINT_COLLATION_URI;
                    }

                    if (uri.startsWith("http://saxon.sf.net/")) {
                        defaultCollationName = uri;
                        return;
                    }

                    if (getPrincipalStylesheet().getExecutable().getNamedCollation(uri) != null) {
                        defaultCollationName = uri;
                        return;
                    }

                    if (getPrincipalStylesheet().findCollation(uri) != null) {
                        defaultCollationName = uri;
                        return;
                    }
                }
                // if not recognized, try the next URI in order
            }
            compileError("No recognized collation URI found in default-collation attribute", "XTSE0125");
        }
    }

    /**
     * Get the default collation for this stylesheet element. If no default collation is
     * specified in the stylesheet, return the Unicode codepoint collation name.
     * @return the name of the default collation
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
        return NamespaceConstant.CODEPOINT_COLLATION_URI;
    }

    /**
     * Check whether a particular extension element namespace is defined on this node.
     * This checks this node only, not the ancestor nodes.
     * The implementation checks whether the prefix is included in the
     * [xsl:]extension-element-prefixes attribute.
     *
     * @param uriCode the namespace URI code being tested
     * @return true if this namespace is defined on this element as an extension element namespace
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
     * @return true if the URI is an extension element namespace URI
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
     * @return true if the namespace is excluded by virtue of an [xsl:]exclude-result-prefixes attribute
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
     * @return true if this namespace URI is a namespace excluded by virtue of exclude-result-prefixes
     * on this element or on an ancestor element
     */

    public boolean isExcludedNamespace(short uriCode) {
        if (uriCode == NamespaceConstant.XSLT_CODE || uriCode == NamespaceConstant.XML_CODE) {
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

    protected void processDefaultXPathNamespaceAttribute(String nc) {
        String v = getAttributeValue(nc);
        if (v != null) {
            defaultXPathNamespace = v;
        }
    }

    /**
     * Get the default XPath namespace for elements and types
     * @return the default namespace for elements and types.
     * Return {@link NamespaceConstant#NULL} for the non-namespace
    */

    protected String getDefaultXPathNamespace() {
        NodeInfo anc = this;
        while (anc instanceof StyleElement) {
            String x = ((StyleElement)anc).defaultXPathNamespace;
            if (x != null) {
                return x;
            }
            anc = anc.getParent();
        }
        return NamespaceConstant.NULL;
        // indicates that the default namespace is the null namespace
    }

    /**
     * Get the Schema type definition for a type named in the stylesheet (in a
     * "type" attribute).
     * @param typeAtt the value of the type attribute
     * @return the corresponding schema type
     * @throws XPathException if the type is not declared in an
     *                        imported schema, or is not a built-in type
     */

    public SchemaType getSchemaType(String typeAtt) throws XPathException {
        try {
            String[] parts = getConfiguration().getNameChecker().getQNameParts(typeAtt);
            String lname = parts[1];
            String uri;
            if ("".equals(parts[0])) {
                // Name is unprefixed: use the default-xpath-namespace
                uri = getDefaultXPathNamespace();
            } else {
                uri = getURIForPrefix(parts[0], false);
                if (uri == null) {
                    compileError("Namespace prefix for type annotation is undeclared", "XTSE1520");
                    return null;
                }
            }
            int nameCode = getNamePool().allocate(parts[0], uri, lname);
            if (uri.equals(NamespaceConstant.SCHEMA)) {
                if ("untyped".equals(lname)) {
                    compileError("Cannot validate a node as 'untyped'", "XTSE1520");
                }
                SchemaType t = BuiltInType.getSchemaType(StandardNames.getFingerprint(uri, lname));
                if (t == null) {
                    compileError("Unknown built-in type " + typeAtt, "XTSE1520");
                    return null;
                }
                return t;
            }

            // not a built-in type: look in the imported schemas

            if (!getPrincipalStylesheet().isImportedSchema(uri)) {
                compileError("There is no imported schema for the namespace of type " + typeAtt, "XTSE1520");
                return null;
            }
            SchemaType stype = getConfiguration().getSchemaType(nameCode & 0xfffff);
            if (stype == null) {
                compileError("There is no type named " + typeAtt + " in an imported schema", "XTSE1520");
            }
            return stype;

        } catch (QNameException err) {
            compileError("Invalid type name. " + err.getMessage(), "XTSE1520");
        }
        return null;
    }

    /**
     * Get the type annotation to use for a given schema type
     * @param schemaType the schema type
     * @return the corresponding numeric type annotation
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
     * to perform any necessary run-time type checking and/or conversion.
     * @param name the name of the attribute containing the expression to be checked (used for diagnostics)
     * @param exp the expression to be checked
     * @return the (possibly rewritten) expression after type checking
     */

    // Note: the typeCheck() call is done at the level of individual path expression; the optimize() call is done
    // for a template or function as a whole. We can't do it all at the function/template level because
    // the static context (e.g. namespaces) changes from one XPath expression to another.

    public Expression typeCheck(String name, Expression exp) throws XPathException {

        if (exp == null) {
            return null;
        }

        exp.setContainer(this);
            // temporary, until the instruction is compiled

        try {
            exp = makeExpressionVisitor().typeCheck(exp, Type.ITEM_TYPE);
            exp = ExpressionTool.resolveCallsToCurrentFunction(exp, getConfiguration());
//            if (explaining) {
//                System.err.println("Attribute '" + name + "' of element '" + getDisplayName() + "' at line " + getLineNumber() + ':');
//                System.err.println("Static type: " +
//                        SequenceType.makeSequenceType(exp.getItemType(), exp.getCardinality()));
//                System.err.println("Optimized expression tree:");
//                exp.display(10, getNamePool(), System.err);
//            }
            if (getPreparedStylesheet().isCompileWithTracing()) {
                InstructionDetails details = new InstructionDetails();
                details.setConstructType(Location.XPATH_IN_XSLT);
                details.setLineNumber(getLineNumber());
                details.setSystemId(getSystemId());
                details.setProperty("attribute-name", name);
                TraceWrapper trace = new TraceInstruction(exp, details);
                trace.setLocationId(allocateLocationId(getSystemId(), getLineNumber()));
                trace.setContainer(this);
                exp = trace;
            }
            return exp;
        } catch (XPathException err) {
            // we can't report a dynamic error such as divide by zero unless the expression
            // is actually executed.
            if (err.isStaticError() || err.isTypeError()) {
                compileError(err);
                return exp;
            } else {
                ErrorExpression erexp = new ErrorExpression(err);
                erexp.setLocationId(allocateLocationId(getSystemId(), getLineNumber()));
                return erexp;
            }
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
     * Allocate slots to any variables used within a pattern. This is needed only for "free-standing"
     * patterns such as template match or key match; other cases are handed by the containing PatternSponsor
     * @param pattern the pattern whose slots are to be allocated
     */

//    private void allocateSlots(Pattern pattern) {
//        if (pattern instanceof LocationPathPattern) {
//            ((LocationPathPattern)pattern).allocateSlots((ExpressionContext)getStaticContext(), 0);
//        }
//    }

    /**
     * Allocate space for range variables within predicates in the match pattern. The xsl:template
     * element has no XPath expressions among its attributes, so if this method is called on this
     * object it can only be because there are variables used in the match pattern. We work out
     * how many slots are needed for the match pattern in each template rule, and then apply-templates
     * can allocate a stack frame that is large enough for the most demanding match pattern in the
     * entire stylesheet.
     * @param match the pattern
     * @param frame the stackframe outline for this pattern
     */
    
    public void allocatePatternSlots(Pattern match, SlotManager frame) {
        int highWater = match.allocateSlots(getStaticContext(), frame, 0);
        //int highWater = frame.getNumberOfVariables();
        getPrincipalStylesheet().allocatePatternSlots(highWater);
    }

    /**
     * Type-check a pattern. This is called to check each pattern while the containing
     * instruction is being validated. It is not just a static type-check, it also adds code
     * to perform any necessary run-time type checking and/or conversion.
     * @param name the name of the attribute holding the pattern, for example "match": used in
     * diagnostics
     * @param pattern the compiled pattern
     * @return the original pattern, or a substitute pattern if it has been rewritten
     */

    public Pattern typeCheck(String name, Pattern pattern) throws XPathException {
        if (pattern == null) {
            return null;
        }
        try {
            pattern = pattern.analyze(makeExpressionVisitor(), Type.NODE_TYPE);
            boolean usesCurrent = false;
            if (pattern instanceof LocationPathPattern) {
                Iterator sub = pattern.iterateSubExpressions();
                while (sub.hasNext()) {
                    Expression filter = (Expression)sub.next();
                    if (ExpressionTool.callsFunction(filter, Current.FN_CURRENT)) {
                        usesCurrent = true;
                        break;
                    }
                }
                if (usesCurrent) {
                    Configuration config = getConfiguration();

                    LetExpression let = new LetExpression();
                    let.setVariableQName(new StructuredQName("saxon", NamespaceConstant.SAXON, "current" + hashCode()));
                    let.setRequiredType(SequenceType.SINGLE_ITEM);
                    let.setSequence(new ContextItemExpression());
                    let.setAction(Literal.makeEmptySequence());
                    PromotionOffer offer = new PromotionOffer(config.getOptimizer());
                    offer.action = PromotionOffer.REPLACE_CURRENT;
                    offer.containingExpression = let;
                    ((LocationPathPattern)pattern).resolveCurrent(let, offer, true);
                    //allocateSlots(let); //redundant, done again later
                }
            }
            return pattern;
        } catch (XPathException err) {
            // we can't report a dynamic error such as divide by zero unless the pattern
            // is actually executed. We don't have an error pattern available, so we
            // construct one
            if (err.isStaticError() || err.isTypeError()) {
                XPathException e2 = new XPathException("Error in " + name + " pattern", err);
                e2.setLocator(err.getLocator());
                e2.setErrorCode(err.getErrorCodeLocalPart());
                throw e2;
            } else {
                LocationPathPattern errpat = new LocationPathPattern();
                errpat.setExecutable(getExecutable());
                errpat.addFilter(new ErrorExpression(err));
                return errpat;
            }
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
            } else if (reportingCircumstances == REPORT_UNLESS_FALLBACK_AVAILABLE) {
                boolean hasFallback = false;
                AxisIterator kids = iterateAxis(Axis.CHILD);
                while (true) {
                    NodeInfo child = (NodeInfo)kids.next();
                    if (child == null) {
                        break;
                    }
                    if (child instanceof XSLFallback) {
                        hasFallback = true;
                        ((XSLFallback)child).validateSubtree();
                    }
                }
                if (!hasFallback) {
                    compileError(validationError);
                }

            }
        } else {
            try {
                validate();
            } catch (XPathException err) {
                compileError(err);
            }
            validateChildren();
            postValidate();
        }
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
                    ((StyleElement)child).compileError("An " + getDisplayName() + " element must not contain an " +
                            child.getDisplayName() + " element", "XTSE0010");
                }
                ((StyleElement)child).validateSubtree();
                lastChild = (StyleElement)child;
            }
        }
        if (lastChild instanceof XSLVariable &&
                !(this instanceof XSLStylesheet)) {
            lastChild.compileWarning("A variable with no following sibling instructions has no effect",
                    SaxonErrorCode.SXWN9001);
        }
    }

    /**
     * Check whether a given child is permitted for this element. This method is used when a non-instruction
     * child element such as xsl:sort is encountered in a context where instructions would normally be expected.
     * @param child the child that may or may not be permitted
     * @return true if the child is permitted.
     */

    protected boolean isPermittedChild(StyleElement child) {
        return false;
    }

    /**
     * Get the principal XSLStylesheet node. This gets the principal style sheet, i.e. the
     * one originally loaded, that forms the root of the import/include tree
     * @return the xsl:stylesheet element at the root of the principal stylesheet module.
     * Exceptionally (with early errors in a simplified stylesheet module) return null.
     */

    public XSLStylesheet getPrincipalStylesheet() {
        XSLStylesheet sheet = getContainingStylesheet();
        if (sheet == null) {
            return null;
        }
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
     * @return the PreparedStylesheet to which this stylesheet element belongs.
     * Exceptionally (with early errors in a simplified stylesheet module) return null.
     */

    public PreparedStylesheet getPreparedStylesheet() {
        XSLStylesheet principalStylesheet = getPrincipalStylesheet();
        if (principalStylesheet == null) {
            return null;
        }
        return principalStylesheet.getPreparedStylesheet();
    }

    /**
     * Check that the stylesheet element is within a sequence constructor
     *
     * @throws XPathException if not within a sequence constructor
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
     * @throws XPathException if invalid
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
                if (!Whitespace.isWhite(child.getStringValueCS())) {
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
     * @param errorCode the error to throw if it is not at the top level; defaults to XTSE0010
     * if the value is null
     * @throws XPathException if not at top level
     */

    public void checkTopLevel(String errorCode) throws XPathException {
        if (!(getParent() instanceof XSLStylesheet)) {
            compileError("Element must be used only at top level of stylesheet", (errorCode==null ? "XTSE0010" : errorCode));
        }
    }

    /**
     * Convenience method to check that the stylesheet element is empty
     *
     * @throws XPathException if it is not empty
     */

    public void checkEmpty() throws XPathException {
        if (hasChildNodes()) {
            compileError("Element must be empty", "XTSE0260");
        }
    }

    /**
     * Convenience method to report the absence of a mandatory attribute
     * @param attribute the name of the attribute whose absence is to be reported
     * @throws XPathException if the attribute is missing
     */

    public void reportAbsence(String attribute)
            throws XPathException {
        compileError("Element must have a \"" + attribute + "\" attribute", "XTSE0010");
    }


    /**
     * Compile the instruction on the stylesheet tree into an executable instruction
     * for use at run-time.
     * @param exec the Executable
     * @return either a ComputedExpression, or null. The value null is returned when compiling an instruction
     *         that returns a no-op, or when compiling a top-level object such as an xsl:template that compiles
     *         into something other than an instruction.
     */

    public abstract Expression compile(Executable exec) throws XPathException;

    /**
     * Compile the children of this instruction on the stylesheet tree, adding the
     * subordinate instructions to the parent instruction on the execution tree.
     * @param exec the Executable
     * @param iter Iterator over the children. This is used in the case where there are children
     * that are not part of the sequence constructor, for example the xsl:sort children of xsl:for-each;
     * the iterator can be positioned past such elements.
     * @param includeParams true if xsl:param elements are to be treated as child instructions (true
     * for templates but not for functions)
     * @return an Expression tree representing the children of this instruction
     */

    public Expression compileSequenceConstructor(Executable exec, SequenceIterator iter, boolean includeParams)
            throws XPathException {

        Expression result = Literal.makeEmptySequence();
        int locationId = allocateLocationId(getSystemId(), getLineNumber());
        while (true) {
            int lineNumber = getLineNumber();
            NodeInfo node = ((NodeInfo)iter.next());
            if (node == null) {
                return result;
            }
            if (node instanceof StyleElement) {
                lineNumber = node.getLineNumber();  // this is to get a line number for the next text node
            }
            if (node.getNodeKind() == Type.TEXT) {
                // handle literal text nodes by generating an xsl:value-of instruction
                AxisIterator lookahead = node.iterateAxis(Axis.FOLLOWING_SIBLING);
                NodeInfo sibling = (NodeInfo)lookahead.next();
                if (!(sibling instanceof XSLParam || sibling instanceof XSLSort)) {
                    // The test for XSLParam and XSLSort is to eliminate whitespace nodes that have been retained
                    // because of xml:space="preserve"
                    ValueOf text = new ValueOf(new StringLiteral(node.getStringValue()), false, false);
                    text.setLocationId(allocateLocationId(getSystemId(), lineNumber));
                    result = Block.makeBlock(result, text);
                    result.setLocationId(locationId);
                }

            } else if (node instanceof XSLVariable) {
                Expression var = ((XSLVariable)node).compileLocalVariable(exec);
                if (var == null) {
                    // this means that the variable declaration is redundant
                    //continue;
                } else {
                    LocalVariable lv = (LocalVariable)var;
                    Expression tail = compileSequenceConstructor(exec, iter, includeParams);
                    if (tail == null || Literal.isEmptySequence(tail)) {
                        // this doesn't happen, because if there are no instructions following
                        // a variable, we'll have taken the var==null path above
                        return result;
                    } else {
                        LetExpression let = new LetExpression();
                        let.setRequiredType(lv.getRequiredType());
                        let.setVariableQName(lv.getVariableQName());
                        let.setSequence(lv.getSelectExpression());
                        let.setAction(tail);
                        ((XSLVariable)node).fixupBinding(let);
                        locationId = allocateLocationId(node.getSystemId(), node.getLineNumber());
                        let.setLocationId(locationId);
                        if (getPreparedStylesheet().isCompileWithTracing()) {
                            TraceExpression t = new TraceExpression(let);
                            t.setConstructType(Location.LET_EXPRESSION);
                            t.setObjectName(lv.getVariableQName());
                            t.setSystemId(node.getSystemId());
                            t.setLineNumber(node.getLineNumber());
                            result = Block.makeBlock(result, t);
                        } else {
                            result =  Block.makeBlock(result, let);
                        }
                        result.setLocationId(locationId);
                    }
                }


            } else if (node instanceof StyleElement) {
                StyleElement snode = (StyleElement)node;
                Expression child;
                if (snode.validationError != null && !(this instanceof AbsentExtensionElement)) {
                    child = fallbackProcessing(exec, snode);

                } else {
                    child = snode.compile(exec);
                    if (child != null) {
                        if (child.getContainer() == null) {
                            // for the time being, the XSLT stylesheet element acts as the container
                            // for the XPath expressions within. This will later be replaced by a
                            // compiled template, variable, or other top-level construct
                            child.setContainer(this);
                        }
                        locationId = allocateLocationId(getSystemId(), snode.getLineNumber());
                        child.setLocationId(locationId);
                        if (includeParams || !(node instanceof XSLParam)) {
                            if (getPreparedStylesheet().isCompileWithTracing()) {
                                child = makeTraceInstruction(snode, child);
                            }
                        }
                    }
                }
                result = Block.makeBlock(result, child);
                if (result != null) {
                    result.setLocationId(locationId);
                }
            }
        }
    }


    /**
     * Create a trace instruction to wrap a real instruction
     * @param source the parent element
     * @param child the compiled expression tree for the instruction to be traced
     * @return a wrapper instruction that performs the tracing (if activated at run-time)
     */

    protected static TraceWrapper makeTraceInstruction(StyleElement source, Expression child) {
        if (child instanceof TraceWrapper) {
            return (TraceWrapper)child;
            // this can happen, for example, after optimizing a compile-time xsl:if
        }

        TraceWrapper trace = new TraceInstruction(child, source);
        trace.setLocationId(source.allocateLocationId(source.getSystemId(), source.getLineNumber()));
        trace.setContainer(source);
        return trace;
    }

    /**
     * Perform fallback processing. Generate fallback code for an extension
     * instruction that is not recognized by the implementation.
     * @param exec the Executable
     * @param instruction The unknown extension instruction
     * @return the expression tree representing the fallback code
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
                    b = Literal.makeEmptySequence();
                }
                if (fallback == null) {
                    fallback = b;
                } else {
                    fallback = Block.makeBlock(fallback, b);
                    fallback.setLocationId(
                                allocateLocationId(getSystemId(), getLineNumber()));
                }
            }
        }
        if (fallback != null) {
            return fallback;
        } else {
            return new ErrorExpression(instruction.validationError);
//            compileError(instruction.validationError);
//            return EmptySequence.getInstance();
        }

    }

    /**
     * Allocate a location identifier
     * @param systemId identifies the module containing the instruction
     * @param lineNumber the line number of the instruction
     * @return an integer location ID which can be used to report the location of the instruction,
     * by reference to a {@link LocationProvider}
     */

    protected int allocateLocationId(String systemId, int lineNumber) {
        return getStaticContext().getLocationMap().allocateLocationId(systemId, lineNumber);
    }

    /**
     * Construct sort keys for a SortedIterator
     *
     * @return an array of SortKeyDefinition objects if there are any sort keys;
     *         or null if there are none.
     */

    protected SortKeyDefinition[] makeSortKeys() throws XPathException {
        // handle sort keys if any

        int numberOfSortKeys = 0;
        AxisIterator kids = iterateAxis(Axis.CHILD);
        while (true) {
            Item child = kids.next();
            if (child == null) {
                break;
            }
            if (child instanceof XSLSort) {
                ((XSLSort)child).compile(getExecutable());
                if (numberOfSortKeys != 0 && ((XSLSort)child).getStable() != null) {
                    compileError("stable attribute may appear only on the first xsl:sort element", "XTSE1017");
                }
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
                    keys[k++] = ((XSLSort)child).getSortKeyDefinition().simplify(makeExpressionVisitor());
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
     *             Or null, if these are not required.
     * @return an array of AttributeList instructions representing the compiled attribute sets
     */

    protected AttributeSet[] getAttributeSets(String use, List list)
            throws XPathException {

        if (list == null) {
            list = new ArrayList(4);
        }

        XSLStylesheet stylesheet = getPrincipalStylesheet();
        List toplevel = stylesheet.getTopLevel();

        StringTokenizer st = new StringTokenizer(use, " \t\n\r", false);
        while (st.hasMoreTokens()) {
            String asetname = st.nextToken();
            StructuredQName fprint;
            try {
                fprint = makeQName(asetname);
            } catch (NamespaceException err) {
                compileError(err.getMessage(), "XTSE0710");
                fprint = null;
            } catch (XPathException err) {
                compileError(err.getMessage(), "XTSE0710");
                fprint = null;
            }
            boolean found = false;

            // search for the named attribute set, using all of them if there are several with the
            // same name

            for (int i = 0; i < toplevel.size(); i++) {
                if (toplevel.get(i) instanceof XSLAttributeSet) {
                    XSLAttributeSet t = (XSLAttributeSet)toplevel.get(i);
                    if (t.getAttributeSetName().equals(fprint)) {
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
     * @param exec the Executable
     * @param tunnel true if the tunnel="yes" parameters are wanted, false to get
     * @param caller the calling instruction (for example xsl:apply-templates
     * @return an array of WithParam objects for either the ordinary parameters
     * or the tunnel parameters
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
                    ExpressionTool.copyLocationInfo(caller, p);
                    array[count++] = p;
                }

            }
        }
    }

    /**
     * Report an error with diagnostic information
     * @param error contains information about the error
     * @throws XPathException always, after reporting the error to the ErrorListener
     */

    protected void compileError(XPathException error)
            throws XPathException {
        error.setIsStaticError(true);
        // Set the location of the error if there is not current location information,
        // or if the current location information is local to the XPath expression
        if (error.getLocator() == null ||
                error.getLocator() instanceof ExpressionLocation ||
                error.getLocator() instanceof Expression) {
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
            throw XPathException.makeXPathException(err2);
        }
    }

    /**
     * Report a static error in the stylesheet
     * @param message the error message
     * @throws XPathException always, after reporting the error to the ErrorListener
     */

    protected void compileError(String message)
            throws XPathException {
        XPathException tce = new XPathException(message);
        tce.setLocator(this);
        compileError(tce);
    }

    /**
     * Compile time error, specifying an error code
     *
     * @param message   the error message
     * @param errorCode the error code. May be null if not known or not defined
     * @throws XPathException
     */

    protected void compileError(String message, String errorCode) throws XPathException {
        XPathException tce = new XPathException(message);
        tce.setErrorCode(errorCode);
        tce.setLocator(this);
        compileError(tce);
    }

    protected void undeclaredNamespaceError(String prefix, String errorCode) throws XPathException {
        if (errorCode == null) {
            errorCode = "XTSE0280";
        }
        compileError("Undeclared namespace prefix " + Err.wrap(prefix), errorCode);
    }

    protected void compileWarning(String message, String errorCode)
            throws XPathException {
        XPathException tce = new XPathException(message);
        tce.setErrorCode(errorCode);
        tce.setLocator(this);
        PreparedStylesheet pss = getPreparedStylesheet();
        if (pss != null) {
            pss.reportWarning(tce);
        }
    }

    /**
     * Report a warning to the error listener
     * @param error an exception containing the warning text
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

   /**
    * Report a warning to the error listener
    * @param message the warning message text
    * @param locator the location of the problem in the source stylesheet
    */

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
     * @return true if the element is a child of the xsl:stylesheet element
     */

    public boolean isTopLevel() {
        return (getParent() instanceof XSLStylesheet);
    }

    /**
     * Bind a variable used in this element to the compiled form of the XSLVariable element in which it is
     * declared
     *
     * @param qName The name of the variable
     * @return the XSLVariableDeclaration (that is, an xsl:variable or xsl:param instruction) for the variable
     * @throws XPathException if the variable has not been declared
     */

    public XSLVariableDeclaration bindVariable(StructuredQName qName) throws XPathException {
        XSLVariableDeclaration binding = getVariableBinding(qName);
        if (binding == null) {
            XPathException err = new XPathException("Variable " + qName.getDisplayName() +
                    " has not been declared");
            err.setErrorCode("XPST0008");
            err.setIsStaticError(true);
            throw err;
        }
        return binding;
    }

    /**
     * Bind a variable used in this element to the declaration in the stylesheet
     *
     * @param qName The absolute name of the variable (including namespace URI)
     * @return the XSLVariableDeclaration, or null if it has not been declared
     */

    private XSLVariableDeclaration getVariableBinding(StructuredQName qName) {
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
                    if (var.getVariableQName().equals(qName)) {
                        return var;
                    }
                }
            }
        }

        // Now check for a global variable
        // we rely on the search following the order of decreasing import precedence.

        XSLStylesheet root = getPrincipalStylesheet();
        return root.getGlobalVariable(qName);
    }

    /**
     * Get a FunctionCall declared using an xsl:function element in the stylesheet
     *
     * @param qName       the name of the function
     * @param arity       the number of arguments in the function call. The value -1
     *                    indicates that any arity will do (this is used to support the function-available() function).
     * @return the XSLFunction object representing the function declaration
     *         in the stylesheet, or null if no such function is defined.
     */

    public XSLFunction getStylesheetFunction(StructuredQName qName, int arity) {

        // we rely on the search following the order of decreasing import precedence.

        XSLStylesheet root = getPrincipalStylesheet();
        List toplevel = root.getTopLevel();
        for (int i = toplevel.size() - 1; i >= 0; i--) {
            Object child = toplevel.get(i);
            if (child instanceof XSLFunction &&
                    ((XSLFunction)child).getObjectName().equals(qName) &&
                    (arity == -1 || ((XSLFunction)child).getNumberOfArguments() == arity)) {
                return (XSLFunction)child;
            }
        }
        return null;
    }

    /**
     * Get a list of all stylesheet functions, excluding any that are masked by one of higher precedence
     * @return a list of all stylesheet functions. The members of the list are instances of class XSLFunction
     */

    public List getAllStylesheetFunctions() {
        // The performance of this algorithm is appalling, but it's only used for diagnostic explain output
        List output = new ArrayList();
        XSLStylesheet root = getPrincipalStylesheet();
        List toplevel = root.getTopLevel();
        for (int i = toplevel.size() - 1; i >= 0; i--) {
            Object child = toplevel.get(i);
            if (child instanceof XSLFunction) {
                StructuredQName name = ((XSLFunction)child).getObjectName();
                int arity = ((XSLFunction)child).getNumberOfArguments();
                if (getStylesheetFunction(name, arity) == child) {
                    output.add(child);
                }
            }
        }
        return output;
    }

    /**
     * Get the type of construct. This will be a constant in
     * class {@link Location}. This method is part of the {@link InstructionInfo} interface
     */

    public int getConstructType() {
        return getFingerprint();
    }

    /**
     * Get a name identifying the object of the expression, for example a function name, template name,
     * variable name, key name, element name, etc. This is used only where the name is known statically.
     * If there is no name, the value will be null.
     * @return the name of the object declared in this element, if any
     */

    public StructuredQName getObjectName() {
        return objectName;
    }

    /**
     * Set the object name, for example the name of a function, variable, or template declared on this element
     * @param qName the object name as a QName
     */

    public void setObjectName(StructuredQName qName) {
        objectName = qName;
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

    public String getSystemId(long locationId) {
        return getSystemId();
    }

    public int getLineNumber(long locationId) {
        return getLineNumber();
    }

    public int getColumnNumber(long locationId) {
        return getColumnNumber();
    }     

    /**
     * Get the host language (XSLT, XQuery, XPath) used to implement the code in this container
     * @return typically {@link org.orbeon.saxon.Configuration#XSLT} or {@link org.orbeon.saxon.Configuration#XQUERY}
     */

    public int getHostLanguage() {
        return Configuration.XSLT;
    }

    /**
      * Replace one subexpression by a replacement subexpression
      * @param original the original subexpression
      * @param replacement the replacement subexpression
      * @return true if the original subexpression is found
      */

    public boolean replaceSubExpression(Expression original, Expression replacement) {
        throw new IllegalArgumentException("Invalid replacement");
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
