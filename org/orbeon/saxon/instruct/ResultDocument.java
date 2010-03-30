package org.orbeon.saxon.instruct;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.Controller;
import org.orbeon.saxon.trans.Err;
import org.orbeon.saxon.OutputURIResolver;
import org.orbeon.saxon.event.SaxonOutputKeys;
import org.orbeon.saxon.event.SequenceReceiver;
import org.orbeon.saxon.event.StandardOutputResolver;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.functions.EscapeURI;
import org.orbeon.saxon.functions.Serialize;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.pattern.EmptySequenceTest;
import org.orbeon.saxon.sort.IntHashMap;
import org.orbeon.saxon.sort.IntIterator;
import org.orbeon.saxon.trace.ExpressionPresenter;
import org.orbeon.saxon.trans.SaxonErrorCode;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.SchemaType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.type.TypeHierarchy;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamResult;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * The compiled form of an xsl:result-document element in the stylesheet.
 * <p>
 * The xsl:result-document element takes an attribute href="filename". The filename will
 * often contain parameters, e.g. {position()} to ensure that a different file is produced
 * for each element instance.
 * <p>
 * There is a further attribute "format" which determines the format of the
 * output file, it identifies the name of an xsl:output element containing the output
 * format details. In addition, individual serialization properties may be specified as attributes.
 * These are attribute value templates, so they may need to be computed at run-time.
 */

public class ResultDocument extends Instruction {

    private Expression href;
    private Expression formatExpression;    // null if format was known at compile time
    private Expression content;
    private Properties globalProperties;
    private Properties localProperties;
    private String baseURI;     // needed only for saxon:next-in-chain, or with fn:put()
    private int validationAction;
    private SchemaType schemaType;
    private IntHashMap serializationAttributes;
    private NamespaceResolver nsResolver;
    private Expression dynamicOutputElement;    // used in saxon:result-document() extension function
    private boolean resolveAgainstStaticBase = false;        // used with fn:put()

    /**
     * Create a result-document instruction
     * @param globalProperties  properties defined on static xsl:output
     * @param localProperties   non-AVT properties defined on result-document element
     * @param href              href attribute of instruction
     * @param formatExpression  format attribute of instruction
     * @param baseURI           base URI of the instruction
     * @param validationAction  for example {@link Validation#STRICT}
     * @param schemaType        schema type against which output is to be validated
     * @param serializationAttributes  computed local properties
     * @param nsResolver        namespace resolver
     */

    public ResultDocument(Properties globalProperties,      // properties defined on static xsl:output
                          Properties localProperties,       // non-AVT properties defined on result-document element
                          Expression href,
                          Expression formatExpression,      // AVT defining the output format
                          String baseURI,
                          int validationAction,
                          SchemaType schemaType,
                          IntHashMap serializationAttributes,  // computed local properties only
                          NamespaceResolver nsResolver) {
        this.globalProperties = globalProperties;
        this.localProperties = localProperties;
        this.href = href;
        this.formatExpression = formatExpression;
        this.baseURI = baseURI;
        this.validationAction = validationAction;
        this.schemaType = schemaType;
        this.serializationAttributes = serializationAttributes;
        this.nsResolver = nsResolver;
        adoptChildExpression(href);
        for (Iterator it = serializationAttributes.valueIterator(); it.hasNext();) {
            adoptChildExpression((Expression) it.next());
        }
    }

    /**
     * Set the expression that constructs the content
     * @param content the expression defining the content of the result document
     */

    public void setContent(Expression content) {
        this.content = content;
        adoptChildExpression(content);
    }

    /**
     * Set an expression that evaluates to a run-time xsl:output element, used in the saxon:result-document()
     * extension function designed for use in XQuery
     * @param exp the expression whose result should be an xsl:output element
     */

    public void setDynamicOutputElement(Expression exp) {
        dynamicOutputElement = exp;
    }

    /**
     * Set whether the the instruction should resolve the href relative URI against the static
     * base URI (rather than the dynamic base output URI)
     * @param staticBase set to true by fn:put(), to resolve against the static base URI of the query.
     * Default is false, which causes resolution against the base output URI obtained dynamically
     * from the Controller
     */

    public void setUseStaticBaseUri(boolean staticBase) {
        resolveAgainstStaticBase = staticBase;
    }

    /**
     * Simplify an expression. This performs any static optimization (by rewriting the expression
     * as a different expression). The default implementation does nothing.
     * @return the simplified expression
     * @throws org.orbeon.saxon.trans.XPathException
     *          if an error is discovered during expression rewriting
     * @param visitor an expression visitor
     */

    public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        content = visitor.simplify(content);
        href = visitor.simplify(href);
        for (IntIterator it = serializationAttributes.keyIterator(); it.hasNext();) {
            int key = it.next();
            Expression value = (Expression)serializationAttributes.get(key);
            if (!(value instanceof Literal)) {
                value = visitor.simplify(value);
                serializationAttributes.put(key, value);
            }
        }
        return this;
    }

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        content = visitor.typeCheck(content, contextItemType);
        adoptChildExpression(content);
        if (href != null) {
            href = visitor.typeCheck(href, contextItemType);
            adoptChildExpression(href);
        }
        if (formatExpression != null) {
            formatExpression = visitor.typeCheck(formatExpression, contextItemType);
            adoptChildExpression(formatExpression);
        }
        for (IntIterator it = serializationAttributes.keyIterator(); it.hasNext();) {
            int key = it.next();
            Expression value = (Expression)serializationAttributes.get(key);
            if (!(value instanceof Literal)) {
                value = visitor.typeCheck(value, contextItemType);
                adoptChildExpression(value);
                serializationAttributes.put(key, value);
            }
        }
        try {
            DocumentInstr.checkContentSequence(visitor.getStaticContext(), content, validationAction, schemaType);
        } catch (XPathException err) {
            err.maybeSetLocation(this);
            throw err;
        }
        return this;
    }

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        content = visitor.optimize(content, contextItemType);
        adoptChildExpression(content);
        if (href != null) {
            href = visitor.optimize(href, contextItemType);
            adoptChildExpression(href);
        }
        if (formatExpression != null) {
            formatExpression = visitor.optimize(formatExpression, contextItemType);
            adoptChildExpression(formatExpression);
            // TODO: if the formatExpression is now a constant, could get the output properties now
        }
        for (IntIterator it = serializationAttributes.keyIterator(); it.hasNext();) {
            int key = it.next();
            Expression value = (Expression)serializationAttributes.get(key);
            if (!(value instanceof Literal)) {
                value = visitor.optimize(value, contextItemType);
                adoptChildExpression(value);
                serializationAttributes.put(key, value);
            }
        }
        return this;
    }

    public int getIntrinsicDependencies() {
        return StaticProperty.HAS_SIDE_EFFECTS;
    }

    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    public Expression copy() {
        throw new UnsupportedOperationException("copy");
    }

    /**
     * Handle promotion offers, that is, non-local tree rewrites.
     * @param offer The type of rewrite being offered
     * @throws XPathException
     */

    protected void promoteInst(PromotionOffer offer) throws XPathException {
        content = doPromotion(content, offer);
        if (href != null) {
            href = doPromotion(href, offer);
        }
        for (IntIterator it = serializationAttributes.keyIterator(); it.hasNext();) {
            int key = it.next();
            Expression value = (Expression)serializationAttributes.get(key);
            if (!(value instanceof Literal)) {
                value = doPromotion(value, offer);
                serializationAttributes.put(key, value);
            }
        }
    }

    /**
     * Get the name of this instruction for diagnostic and tracing purposes
     *  (the string "xsl:result-document")
     */

    public int getInstructionNameCode() {
        return StandardNames.XSL_RESULT_DOCUMENT;
    }

    /**
     * Get the item type of the items returned by evaluating this instruction
     * @return the static item type of the instruction. This is empty: the result-document instruction
     * returns nothing.
     * @param th the type hierarchy cache
     */

    public ItemType getItemType(TypeHierarchy th) {
        return EmptySequenceTest.getInstance();
    }



    /**
     * Get all the XPath expressions associated with this instruction
     * (in XSLT terms, the expression present on attributes of the instruction,
     * as distinct from the child instructions in a sequence construction)
     */

    public Iterator iterateSubExpressions() {
        ArrayList list = new ArrayList(6);
        list.add(content);
        if (href != null) {
            list.add(href);
        }
        if (formatExpression != null) {
            list.add(formatExpression);
        }
        for (Iterator it = serializationAttributes.valueIterator(); it.hasNext();) {
            list.add(it.next());
        }
        if (dynamicOutputElement != null) {
            list.add(dynamicOutputElement);
        }
        return list.iterator();
    }

   /**
     * Replace one subexpression by a replacement subexpression
     * @param original the original subexpression
     * @param replacement the replacement subexpression
     * @return true if the original subexpression is found
     */

    public boolean replaceSubExpression(Expression original, Expression replacement) {
        boolean found = false;
        if (content == original) {
            content = replacement;
            found = true;
        }
        if (href == original) {
            href = replacement;
            found = true;
        }
        for (IntIterator it = serializationAttributes.keyIterator(); it.hasNext();) {
            int k = it.next();
            if (serializationAttributes.get(k) == original) {
                serializationAttributes.put(k, replacement);
                found = true;
            }
        }
        if (dynamicOutputElement == original) {
            dynamicOutputElement = replacement;
            found = true;
        }
        return found;
    }

    public TailCall processLeavingTail(XPathContext context) throws XPathException {
        final Controller controller = context.getController();
        final Configuration config = controller.getConfiguration();
        final NamePool namePool = config.getNamePool();
        XPathContext c2 = context.newMinorContext();
        c2.setOrigin(this);

        Result result;
        OutputURIResolver resolver = null;

        if (href == null) {
            result = controller.getPrincipalResult();
        } else {
            try {
                String base;
                if (resolveAgainstStaticBase) {
                    base = baseURI;
                } else {
                    base = controller.getCookedBaseOutputURI();
                }

                resolver = controller.getOutputURIResolver();

                String hrefValue = EscapeURI.iriToUri(href.evaluateAsString(context)).toString();
                try {
                    result = resolver.resolve(hrefValue, base);
                } catch (Exception err) {
                    throw new XPathException("Exception thrown by OutputURIResolver", err);
                }
                if (result == null) {
                    resolver = StandardOutputResolver.getInstance();
                    result = resolver.resolve(hrefValue, base);
                }
            } catch (TransformerException e) {
                throw XPathException.makeXPathException(e);
            }
        }

        if (controller.getDocumentPool().find(result.getSystemId()) != null) {
            XPathException err = new XPathException("Cannot write to a URI that has already been read: " +
                    result.getSystemId());
            err.setXPathContext(context);
            err.setErrorCode("XTRE1500");
            throw err;
        }

        if (!controller.checkUniqueOutputDestination(result.getSystemId())) {
            XPathException err = new XPathException("Cannot write more than one result document to the same URI: " +
                    result.getSystemId());
            err.setXPathContext(context);
            err.setErrorCode("XTDE1490");
            throw err;
        } else {
            controller.addUnavailableOutputDestination(result.getSystemId());
            controller.setThereHasBeenAnExplicitResultDocument();
        }

        boolean timing = controller.getConfiguration().isTiming();
        if (timing) {
            String dest = result.getSystemId();
            if (dest == null) {
                if (result instanceof StreamResult) {
                    dest = "anonymous output stream";
                } else if (result instanceof SAXResult) {
                    dest = "SAX2 ContentHandler";
                } else if (result instanceof DOMResult) {
                    dest = "DOM tree";
                } else {
                    dest = result.getClass().getName();
                }
            }
            System.err.println("Writing to " + dest);
        }

        Properties computedGlobalProps = globalProperties;

        if (formatExpression != null) { 
            // format was an AVT and now needs to be computed
            CharSequence format = formatExpression.evaluateAsString(context);
            String[] parts;
            try {
                parts = controller.getConfiguration().getNameChecker().getQNameParts(format);
            } catch (QNameException e) {
                XPathException err = new XPathException("The requested output format " + Err.wrap(format) + " is not a valid QName");
                err.setErrorCode("XTDE1460");
                err.setXPathContext(context);
                throw err;
            }
            String uri = nsResolver.getURIForPrefix(parts[0], false);
            if (uri == null) {
                XPathException err = new XPathException("The namespace prefix in the format name " + format + " is undeclared");
                err.setErrorCode("XTDE1460");
                err.setXPathContext(context);
                throw err;
            }
            StructuredQName qName = new StructuredQName(parts[0], uri, parts[1]);
            computedGlobalProps = getExecutable().getOutputProperties(qName);
            if (computedGlobalProps == null) {
                XPathException err = new XPathException("There is no xsl:output format named " + format);
                err.setErrorCode("XTDE1460");
                err.setXPathContext(context);
                throw err;
            }

        }

        // Now combine the properties specified on xsl:result-document with those specified on xsl:output

        Properties computedLocalProps = new Properties(computedGlobalProps);

        // First handle the properties with fixed values on xsl:result-document

        final NameChecker checker = config.getNameChecker();
        for (Iterator citer=localProperties.keySet().iterator(); citer.hasNext();) {
            String key = (String)citer.next();
            String[] parts = NamePool.parseClarkName(key);
            try {
                setSerializationProperty(computedLocalProps, parts[0], parts[1],
                        localProperties.getProperty(key), nsResolver, true, checker);
            } catch (XPathException e) {
                e.maybeSetLocation(this);
                throw e;
            }
        }

        // Now add the properties that were specified as AVTs

        if (serializationAttributes.size() > 0) {
            for (IntIterator it = serializationAttributes.keyIterator(); it.hasNext();) {
                int key = it.next();
                Expression exp = (Expression) serializationAttributes.get(key);
                String value = exp.evaluateAsString(context).toString();
                String lname = namePool.getLocalName(key);
                String uri = namePool.getURI(key);
                try {
                    setSerializationProperty(computedLocalProps, uri, lname, value, nsResolver, false, checker);
                } catch (XPathException e) {
                    e.maybeSetLocation(this);
                    e.maybeSetContext(context);
                    if (NamespaceConstant.SAXON.equals(e.getErrorCodeNamespace()) &&
                            "warning".equals(e.getErrorCodeLocalPart())) {
                        try {
                            context.getController().getErrorListener().warning(e);
                        } catch (TransformerException e2) {
                            throw XPathException.makeXPathException(e2);
                        }
                    } else {
                        throw e;
                    }
                }
            }
        }

        // Handle properties specified using a dynamic xsl:output element
        // (Used when the instruction is generated from a saxon:result-document extension function call)

        if (dynamicOutputElement != null) {
            Item outputArg = dynamicOutputElement.evaluateItem(context);
            if (!(outputArg instanceof NodeInfo &&
                    ((NodeInfo)outputArg).getNodeKind() == Type.ELEMENT &&
                    ((NodeInfo)outputArg).getFingerprint() == StandardNames.XSL_OUTPUT)) {
                XPathException err = new XPathException(
                        "The third argument of saxon:result-document must be an <xsl:output> element");
                err.setLocator(this);
                err.setXPathContext(context);
                throw err;
            }
            Properties dynamicProperties = new Properties();
            Serialize.processXslOutputElement((NodeInfo)outputArg, dynamicProperties, context);
            for (Iterator it = dynamicProperties.keySet().iterator(); it.hasNext();) {
                String key = (String)it.next();
                StructuredQName name = StructuredQName.fromClarkName(key);
                String value = dynamicProperties.getProperty(key);
                try {
                    setSerializationProperty(
                        computedLocalProps, name.getNamespaceURI(), name.getLocalName(),
                            value, nsResolver, false, checker);
                } catch (XPathException e) {
                    e.maybeSetLocation(this);
                    e.maybeSetContext(context);
                    throw e;
                }
            }
        }

        String nextInChain = computedLocalProps.getProperty(SaxonOutputKeys.NEXT_IN_CHAIN);
        if (nextInChain != null) {
            try {
                result = controller.prepareNextStylesheet(nextInChain, baseURI, result);
            } catch (TransformerException e) {
                throw XPathException.makeXPathException(e);
            }
        }

        // TODO: cache the serializer and reuse it if the serialization properties are fixed at
        // compile time (that is, if serializationAttributes.isEmpty). Need to save the serializer
        // in a form where the final output destination can be changed.

        c2.changeOutputDestination(computedLocalProps,
                result,
                true,
                Configuration.XSLT,
                validationAction,
                schemaType);
        SequenceReceiver out = c2.getReceiver();

        out.open();
        out.startDocument(0);
        content.process(c2);
        out.endDocument();
        out.close();
        if (resolver != null) {
            try {
                resolver.close(result);
            } catch (TransformerException e) {
                throw XPathException.makeXPathException(e);
            }
        }
        return null;
    }

     /**
     * Validate a serialization property and add its value to a Properties collection
     * @param details the properties to be updated
     * @param uri the uri of the property name
     * @param lname the local part of the property name
     * @param value the value of the serialization property. In the case of QName-valued values,
     * this will use lexical QNames if prevalidated is false, Clark-format names otherwise
     * @param nsResolver resolver for lexical QNames; not needed if prevalidated
     * @param prevalidated true if values are already known to be valid and lexical QNames have been
     * expanded into Clark notation
     * @param checker the XML 1.0 or 1.1 name checker
     */

    public static void setSerializationProperty(Properties details, String uri, String lname,
                                                String value, NamespaceResolver nsResolver,
                                                boolean prevalidated, NameChecker checker)
            throws XPathException {
         // TODO: combine this code with SaxonOutputKeys.checkOutputProperty()
        if (uri.length() == 0) {
            if (lname.equals(StandardNames.METHOD)) {
                if (value.equals("xml") || value.equals("html") ||
                        value.equals("text") || value.equals("xhtml") || prevalidated) {
                    details.setProperty(OutputKeys.METHOD, value);
                } else {
                    String[] parts;
                    try {
                        parts = checker.getQNameParts(value);
                        String prefix = parts[0];
                        if (prefix.length() == 0) {
                            XPathException err = new XPathException("method must be xml, html, xhtml, or text, or a prefixed name");
                            err.setErrorCode("XTSE1570");
                            err.setIsStaticError(true);
                            throw err;
                        } else {
                            String muri = nsResolver.getURIForPrefix(prefix, false);
                            if (muri==null) {
                                XPathException err = new XPathException("Namespace prefix '" + prefix + "' has not been declared");
                                err.setErrorCode("XTSE1570");
                                err.setIsStaticError(true);
                                throw err;
                            }
                            details.setProperty(OutputKeys.METHOD, '{' + muri + '}' + parts[1]);
                        }
                    } catch (QNameException e) {
                        XPathException err = new XPathException("Invalid method name. " + e.getMessage());
                        err.setErrorCode("XTSE1570");
                        err.setIsStaticError(true);
                        throw err;
                    }
                }
            } else

            if (lname.equals(StandardNames.OUTPUT_VERSION) || lname.equals(StandardNames.VERSION)) {
                details.setProperty(OutputKeys.VERSION, value);
            } else

            if (lname.equals(StandardNames.BYTE_ORDER_MARK)) {
                if (prevalidated || value.equals("yes") || value.equals("no")) {
                    details.setProperty(SaxonOutputKeys.BYTE_ORDER_MARK, value);
                } else {
                    XPathException err = new XPathException("byte-order-mark value must be 'yes' or 'no'");
                    err.setErrorCode("XTDE0030");
                    throw err;
                }
            } else

            if (lname.equals(StandardNames.INDENT)) {
                if (prevalidated || value.equals("yes") || value.equals("no")) {
                    details.setProperty(OutputKeys.INDENT, value);
                } else {
                    XPathException err = new XPathException("indent must be 'yes' or 'no'");
                    err.setErrorCode("XTDE0030");
                    throw err;
                }
            } else

            if (lname.equals(StandardNames.ENCODING)) {
                details.setProperty(OutputKeys.ENCODING, value);
            } else

            if (lname.equals(StandardNames.MEDIA_TYPE)) {
                details.setProperty(OutputKeys.MEDIA_TYPE, value);
            } else

            if (lname.equals(StandardNames.DOCTYPE_SYSTEM)) {
                details.setProperty(OutputKeys.DOCTYPE_SYSTEM, value);
            } else

            if (lname.equals(StandardNames.DOCTYPE_PUBLIC)) {
                details.setProperty(OutputKeys.DOCTYPE_PUBLIC, value);
            } else

            if (lname.equals(StandardNames.OMIT_XML_DECLARATION)) {
                if (prevalidated || value.equals("yes") || value.equals("no")) {
                    details.setProperty(OutputKeys.OMIT_XML_DECLARATION, value);
                } else {
                    XPathException err = new XPathException("omit-xml-declaration attribute must be 'yes' or 'no'");
                    err.setErrorCode("XTDE0030");
                    throw err;
                }
            } else

            if (lname.equals(StandardNames.STANDALONE)) {
                if (prevalidated || value.equals("yes") || value.equals("no") || value.equals("omit")) {
                    details.setProperty(OutputKeys.STANDALONE, value);
                } else {
                    XPathException err = new XPathException("standalone attribute must be 'yes' or 'no' or 'omit'");
                    err.setErrorCode("XTDE0030");
                    throw err;
                }
            } else

            if (lname.equals(StandardNames.CDATA_SECTION_ELEMENTS)) {
                String existing = details.getProperty(OutputKeys.CDATA_SECTION_ELEMENTS);
                if (existing == null) {
                    existing = "";
                }
                String s = parseListOfElementNames(value, nsResolver, prevalidated, checker, "XTDE0030");
                details.setProperty(OutputKeys.CDATA_SECTION_ELEMENTS, existing + s);

            } else

            if (lname.equals(StandardNames.USE_CHARACTER_MAPS)) {
                // The use-character-maps attribute is always turned into a Clark-format name at compile time
                String existing = details.getProperty(SaxonOutputKeys.USE_CHARACTER_MAPS);
                if (existing == null) {
                    existing = "";
                }
                details.setProperty(SaxonOutputKeys.USE_CHARACTER_MAPS, existing + value);
            } else


            if (lname.equals(StandardNames.UNDECLARE_PREFIXES)) {
                if (prevalidated || value.equals("yes") || value.equals("no")) {
                    details.setProperty(SaxonOutputKeys.UNDECLARE_PREFIXES, value);
                } else {
                    XPathException err = new XPathException("undeclare-namespaces value must be 'yes' or 'no'");
                    err.setErrorCode("XTDE0030");
                    throw err;
                }
            } else

            if (lname.equals(StandardNames.INCLUDE_CONTENT_TYPE)) {
                if (prevalidated || value.equals("yes") || value.equals("no")) {
                    details.setProperty(SaxonOutputKeys.INCLUDE_CONTENT_TYPE, value);
                } else {
                    XPathException err = new XPathException("include-content-type attribute must be 'yes' or 'no'");
                    err.setErrorCode("XTDE0030");
                    throw err;
                }
            } else

            if (lname.equals(StandardNames.ESCAPE_URI_ATTRIBUTES) || lname.equals("escape-uri-attibutes")) {
                        // misspelling was in Saxon 9.0 and previous releases
                if (prevalidated || value.equals("yes") || value.equals("no")) {
                    details.setProperty(SaxonOutputKeys.ESCAPE_URI_ATTRIBUTES, value);
                } else {
                    XPathException err = new XPathException("escape-uri-attributes value must be 'yes' or 'no'");
                    err.setErrorCode("XTDE0030");
                    throw err;
                }
            } else

            if (lname.equals(StandardNames.NORMALIZATION_FORM)) {
                if (Name11Checker.getInstance().isValidNmtoken(value)) {
//                if (prevalidated || value.equals("NFC") || value.equals("NFD") ||
//                        value.equals("NFKC") || value.equals("NFKD")) {
                    details.setProperty(SaxonOutputKeys.NORMALIZATION_FORM, value);
                } else if (value.equals("none")) {
                    // do nothing
                } else {
                    XPathException err = new XPathException("normalization-form must be a valid NMTOKEN");
                    err.setErrorCode("XTDE0030");
                    throw err;
                }
            } else {
                // Normally detected statically, but not with saxon:serialize
                XPathException err = new XPathException("Unknown serialization property " + lname);
                err.setErrorCode("XTDE0030");
                throw err;
            }

        } else if (uri.equals(NamespaceConstant.SAXON)) {

            if (lname.equals("character-representation")) {
                details.setProperty(SaxonOutputKeys.CHARACTER_REPRESENTATION, value);
            } else

            if (lname.equals("indent-spaces")) {
                try {
                    Integer.parseInt(value);
                    details.setProperty(OutputKeys.INDENT, "yes");
                    details.setProperty(SaxonOutputKeys.INDENT_SPACES, value);
                } catch (NumberFormatException err) {
                    XPathException e = new XPathException("saxon:indent-spaces must be an integer");
                    e.setErrorCode(NamespaceConstant.SAXON, SaxonErrorCode.SXWN9002);
                    throw e;
                }
            } else

            if (lname.equals("suppress-indentation")) {
                String existing = details.getProperty(SaxonOutputKeys.SUPPRESS_INDENTATION);
                if (existing == null) {
                    existing = "";
                }
                String s = parseListOfElementNames(value, nsResolver, prevalidated, checker, "XTDE0030");
                details.setProperty(SaxonOutputKeys.SUPPRESS_INDENTATION, existing + s);
            } else

            if (lname.equals("double-space")) {
                String existing = details.getProperty(SaxonOutputKeys.DOUBLE_SPACE);
                if (existing == null) {
                    existing = "";
                }
                String s = parseListOfElementNames(value, nsResolver, prevalidated, checker, "XTDE0030");
                details.setProperty(SaxonOutputKeys.SUPPRESS_INDENTATION, existing + s);
            } else

            if (lname.equals("next-in-chain")) {
                XPathException e = new XPathException("saxon:next-in-chain value cannot be specified dynamically");
                e.setErrorCode(NamespaceConstant.SAXON, SaxonErrorCode.SXWN9004);
                throw e;
            } else

            if (lname.equals("require-well-formed")) {
                if (prevalidated || value.equals("yes") || value.equals("no")) {
                    details.setProperty(SaxonOutputKeys.REQUIRE_WELL_FORMED, value);
                } else {
                    XPathException e = new XPathException("saxon:require-well-formed value must be 'yes' or 'no'");
                    e.setErrorCode(NamespaceConstant.SAXON, SaxonErrorCode.SXWN9003);
                    throw e;
                }
            }

        } else {

            // deal with user-defined attributes
            details.setProperty('{' + uri + '}' + lname, value);
        }

    }

    public static String parseListOfElementNames(
            String value, NamespaceResolver nsResolver, boolean prevalidated, NameChecker checker, String errorCode)
            throws XPathException {
        String s = "";
        StringTokenizer st = new StringTokenizer(value, " \t\n\r", false);
        while (st.hasMoreTokens()) {
            String displayname = st.nextToken();
            if (prevalidated) {
                s += ' ' + displayname;
            } else {
                try {
                    String[] parts = checker.getQNameParts(displayname);
                    String muri = nsResolver.getURIForPrefix(parts[0], true);
                    if (muri==null) {
                        XPathException err = new XPathException("Namespace prefix '" + parts[0] + "' has not been declared");
                        err.setErrorCode(errorCode);
                        throw err;
                    }
                    s += " {" + muri + '}' + parts[1];
                } catch (QNameException err) {
                    XPathException e = new XPathException("Invalid element name. " + err.getMessage());
                    e.setErrorCode(errorCode);
                    throw e;
                }
            }
        }
        return s;
    }

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void explain(ExpressionPresenter out) {
        out.startElement("resultDocument");
        content.explain(out);
        out.endElement();
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
