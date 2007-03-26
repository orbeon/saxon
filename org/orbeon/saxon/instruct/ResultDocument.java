package org.orbeon.saxon.instruct;

import org.orbeon.saxon.Controller;
import org.orbeon.saxon.OutputURIResolver;
import org.orbeon.saxon.Err;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.functions.EscapeURI;
import org.orbeon.saxon.sort.IntHashMap;
import org.orbeon.saxon.sort.IntIterator;
import org.orbeon.saxon.event.SaxonOutputKeys;
import org.orbeon.saxon.event.SequenceReceiver;
import org.orbeon.saxon.event.StandardOutputResolver;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.pattern.NoNodeTest;
import org.orbeon.saxon.style.StandardNames;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.trans.SaxonErrorCode;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.SchemaType;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.value.Value;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamResult;
import java.io.PrintStream;
import java.io.File;
import java.util.*;

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
    private String baseURI;     // needed only for saxon:next-in-chain
    private int validationAction;
    private SchemaType schemaType;
    private IntHashMap serializationAttributes;
    private NamespaceResolver nsResolver;

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
     */

    public void setContent(Expression content) {
        this.content = content;
        adoptChildExpression(content);
    }

    /**
     * Simplify an expression. This performs any static optimization (by rewriting the expression
     * as a different expression). The default implementation does nothing.
     * @return the simplified expression
     * @throws org.orbeon.saxon.trans.XPathException
     *          if an error is discovered during expression rewriting
     */

    public Expression simplify(StaticContext env) throws XPathException {
        content = content.simplify(env);
        if (href != null) {
            href = href.simplify(env);
        }
        for (IntIterator it = serializationAttributes.keyIterator(); it.hasNext();) {
            int key = it.next();
            Expression value = (Expression)serializationAttributes.get(key);
            if (!(value instanceof Value)) {
                value = value.simplify(env);
                serializationAttributes.put(key, value);
            }
        }
        return this;
    }

    public Expression typeCheck(StaticContext env, ItemType contextItemType) throws XPathException {
        content = content.typeCheck(env, contextItemType);
        adoptChildExpression(content);
        if (href != null) {
            href = href.typeCheck(env, contextItemType);
            adoptChildExpression(href);
        }
        if (formatExpression != null) {
            formatExpression = formatExpression.typeCheck(env, contextItemType);
            adoptChildExpression(formatExpression);
        }
        for (IntIterator it = serializationAttributes.keyIterator(); it.hasNext();) {
            int key = it.next();
            Expression value = (Expression)serializationAttributes.get(key);
            if (!(value instanceof Value)) {
                value = value.typeCheck(env, contextItemType);
                adoptChildExpression(value);
                serializationAttributes.put(key, value);
            }
        }
        return this;
    }

    public Expression optimize(Optimizer opt, StaticContext env, ItemType contextItemType) throws XPathException {
        content = content.optimize(opt, env, contextItemType);
        adoptChildExpression(content);
        if (href != null) {
            href = href.optimize(opt, env, contextItemType);
            adoptChildExpression(href);
        }
        if (formatExpression != null) {
            formatExpression = formatExpression.optimize(opt, env, contextItemType);
            adoptChildExpression(formatExpression);
            // TODO: if the formatExpression is now a constant, could get the output properties now
        }
        for (IntIterator it = serializationAttributes.keyIterator(); it.hasNext();) {
            int key = it.next();
            Expression value = (Expression)serializationAttributes.get(key);
            if (!(value instanceof Value)) {
                value = value.optimize(opt, env, contextItemType);
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
            if (!(value instanceof Value)) {
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
     * @param th
     */

    public ItemType getItemType(TypeHierarchy th) {
        return NoNodeTest.getInstance();
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
        for (Iterator it = serializationAttributes.valueIterator(); it.hasNext();) {
            list.add(it.next());
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
                String base = controller.getBaseOutputURI();
                if (base == null && config.isAllowExternalFunctions()) {
                    // if calling external functions is allowed, then the stylesheet is trusted, so
                    // we allow it to write to files relative to the current directory
                    base = new File(System.getProperty("user.dir")).toURI().toString();
                }
                if (base != null) {
                    base = EscapeURI.iriToUri(base).toString();  //TODO: avoid doing this repeatedly
                }

                resolver = controller.getOutputURIResolver();

                String hrefValue = EscapeURI.iriToUri(href.evaluateAsString(context)).toString();
                try {
                    result = resolver.resolve(hrefValue, base);
                } catch (Exception err) {
                    throw new DynamicError("Exception thrown by OutputURIResolver", err);
                }
                if (result == null) {
                    resolver = StandardOutputResolver.getInstance();
                    result = resolver.resolve(hrefValue, base);
                }
            } catch (TransformerException e) {
                throw DynamicError.makeDynamicError(e);
            }
        }

        if (!controller.checkUniqueOutputDestination(result.getSystemId())) {
            DynamicError err = new DynamicError(
                    "Cannot write more than one result document to the same URI, or write to a URI that has been read: " +
                    result.getSystemId());
            err.setXPathContext(context);
            err.setErrorCode("XTDE1490");
            throw err;
            // TODO: writing to a document that has previously been read should be error code XTDE1500
        } else {
            controller.addUnavailableOutputDestination(result.getSystemId());
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

        if (formatExpression != null) { //TODO: we're taking this path even when format was specified statically
            // format was an AVT and now needs to be computed
            String format = formatExpression.evaluateAsString(context);
            String[] parts;
            try {
                parts = controller.getConfiguration().getNameChecker().getQNameParts(format);
            } catch (QNameException e) {
                DynamicError err = new DynamicError(
                        "The requested output format " + Err.wrap(format) + " is not a valid QName");
                err.setErrorCode("XTDE1460");
                err.setXPathContext(context);
                throw err;
            }
            String uri = nsResolver.getURIForPrefix(parts[0], false);
            if (uri == null) {
                DynamicError err = new DynamicError(
                        "The namespace prefix in the format name " + format + " is undeclared");
                err.setErrorCode("XTDE1460");
                err.setXPathContext(context);
                throw err;
            }
            int fp = namePool.allocate(parts[0], uri, parts[1]) & NamePool.FP_MASK;
            computedGlobalProps = getExecutable().getOutputProperties(fp);
            if (computedGlobalProps == null) {
                DynamicError err = new DynamicError("There is no xsl:output format named " + format);
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
            setSerializationProperty(computedLocalProps, parts[0], parts[1],
                    localProperties.getProperty(key), nsResolver, true, checker);
        }

        // Now add the properties that were specified as AVTs

        if (serializationAttributes.size() > 0) {
            for (IntIterator it = serializationAttributes.keyIterator(); it.hasNext();) {
                int key = it.next();
                Expression exp = (Expression) serializationAttributes.get(key);
                String value = exp.evaluateAsString(context);
                String lname = namePool.getLocalName(key);
                String uri = namePool.getURI(key);
                try {
                    setSerializationProperty(computedLocalProps, uri, lname, value, nsResolver, false, checker);
                } catch (DynamicError e) {
                    if (NamespaceConstant.SAXON.equals(e.getErrorCodeNamespace()) &&
                            "warning".equals(e.getErrorCodeLocalPart())) {
                        try {
                            context.getController().getErrorListener().warning(e);
                        } catch (TransformerException e2) {
                            throw DynamicError.makeDynamicError(e2);
                        }
                    } else {
                        e.setXPathContext(context);
                        e.setLocator(getSourceLocator());
                        throw e;
                    }
                }
            }
        }

        String nextInChain = computedLocalProps.getProperty(SaxonOutputKeys.NEXT_IN_CHAIN);
        if (nextInChain != null) {
            try {
                result = controller.prepareNextStylesheet(nextInChain, baseURI, result);
            } catch (TransformerException e) {
                throw DynamicError.makeDynamicError(e);
            }
        }

        // TODO: cache the serializer and reuse it if the serialization properties are fixed at
        // compile time (that is, if serializationAttributes.isEmpty). Need to save the serializer
        // in a form where the final output destination can be changed.

        c2.changeOutputDestination(computedLocalProps,
                result,
                true,
                Configuration.XSLT, validationAction,
                schemaType);
        SequenceReceiver out = c2.getReceiver();

        out.startDocument(0);
        content.process(c2);
        out.endDocument();
        out.close();
        if (resolver != null) {
            try {
                resolver.close(result);
            } catch (TransformerException e) {
                throw DynamicError.makeDynamicError(e);
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
     * @param checker
     */

    public static void setSerializationProperty(Properties details, String uri, String lname,
                                                String value, NamespaceResolver nsResolver,
                                                boolean prevalidated, NameChecker checker)
            throws XPathException {
        if (uri.equals("")) {
            if (lname.equals(StandardNames.METHOD)) {
                if (value.equals("xml") || value.equals("html") ||
                        value.equals("text") || value.equals("xhtml") || prevalidated) {
                    details.put(OutputKeys.METHOD, value);
                } else {
                    String[] parts;
                    try {
                        parts = checker.getQNameParts(value);
                        String prefix = parts[0];
                        if (prefix.equals("")) {
                            DynamicError err = new DynamicError("method must be xml, html, xhtml, or text, or a prefixed name");
                            err.setErrorCode("XTSE1570");
                            throw err;
                        } else {
                            String muri = nsResolver.getURIForPrefix(prefix, false);
                            if (muri==null) {
                                DynamicError err = new DynamicError("Namespace prefix '" + prefix + "' has not been declared");
                                err.setErrorCode("XTSE1570");
                                throw err;
                            }
                            details.put(OutputKeys.METHOD, '{' + muri + '}' + parts[1]);
                        }
                    } catch (QNameException e) {
                        DynamicError err = new DynamicError("Invalid method name. " + e.getMessage());
                        err.setErrorCode("XTSE1570");
                        throw err;
                    }
                }
            } else

            if (lname.equals(StandardNames.OUTPUT_VERSION)) {
                details.put(OutputKeys.VERSION, value);
            } else

            if (lname.equals("byte-order-mark")) {
                if (prevalidated || value.equals("yes") || value.equals("no")) {
                    details.put(SaxonOutputKeys.BYTE_ORDER_MARK, value);
                } else {
                    DynamicError err = new DynamicError("byte-order-mark value must be 'yes' or 'no'");
                    err.setErrorCode("XTDE0030");
                    throw err;
                }
            } else

            if (lname.equals(StandardNames.INDENT)) {
                if (prevalidated || value.equals("yes") || value.equals("no")) {
                    details.put(OutputKeys.INDENT, value);
                } else {
                    DynamicError err = new DynamicError("indent must be 'yes' or 'no'");
                    err.setErrorCode("XTDE0030");
                    throw err;
                }
            } else

            if (lname.equals(StandardNames.ENCODING)) {
                details.put(OutputKeys.ENCODING, value);
            } else

            if (lname.equals(StandardNames.MEDIA_TYPE)) {
                details.put(OutputKeys.MEDIA_TYPE, value);
            } else

            if (lname.equals(StandardNames.DOCTYPE_SYSTEM)) {
                details.put(OutputKeys.DOCTYPE_SYSTEM, value);
            } else

            if (lname.equals(StandardNames.DOCTYPE_PUBLIC)) {
                details.put(OutputKeys.DOCTYPE_PUBLIC, value);
            } else

            if (lname.equals(StandardNames.OMIT_XML_DECLARATION)) {
                if (prevalidated || value.equals("yes") || value.equals("no")) {
                    details.put(OutputKeys.OMIT_XML_DECLARATION, value);
                } else {
                    DynamicError err = new DynamicError("omit-xml-declaration attribute must be 'yes' or 'no'");
                    err.setErrorCode("XTDE0030");
                    throw err;
                }
            } else

            if (lname.equals(StandardNames.STANDALONE)) {
                if (prevalidated || value.equals("yes") || value.equals("no") || value.equals("omit")) {
                    details.put(OutputKeys.STANDALONE, value);
                } else {
                    DynamicError err = new DynamicError("standalone attribute must be 'yes' or 'no' or 'omit'");
                    err.setErrorCode("XTDE0030");
                    throw err;
                }
            } else

            if (lname.equals(StandardNames.CDATA_SECTION_ELEMENTS)) {
                String existing = details.getProperty(OutputKeys.CDATA_SECTION_ELEMENTS);
                if (existing == null) {
                    existing = "";
                }
                String s = "";
                StringTokenizer st = new StringTokenizer(value);
                while (st.hasMoreTokens()) {
                    String displayname = st.nextToken();
                    if (prevalidated) {
                        s += ' ' + displayname;
                    } else {
                        try {
                            String[] parts = checker.getQNameParts(displayname);
                            String muri = nsResolver.getURIForPrefix(parts[0], true);
                            if (muri==null) {
                                DynamicError err = new DynamicError("Namespace prefix '" + parts[0] + "' has not been declared");
                                err.setErrorCode("XTDE0030");
                                throw err;
                            }
                            s += " {" + muri + '}' + parts[1];
                        } catch (QNameException err) {
                            DynamicError e = new DynamicError("Invalid CDATA element name. " + err.getMessage());
                            e.setErrorCode("XTDE0030");
                            throw e;
                        }
                    }

                    details.put(OutputKeys.CDATA_SECTION_ELEMENTS, existing + s);
                }
            } else

            if (lname.equals(StandardNames.USE_CHARACTER_MAPS)) {
                // The use-character-maps attribute is always turned into a Clark-format name at compile time
                String existing = details.getProperty(SaxonOutputKeys.USE_CHARACTER_MAPS);
                if (existing == null) {
                    existing = "";
                }
                details.put(SaxonOutputKeys.USE_CHARACTER_MAPS, existing + value);
            } else


            if (lname.equals(StandardNames.UNDECLARE_PREFIXES)) {
                if (prevalidated || value.equals("yes") || value.equals("no")) {
                    details.put(SaxonOutputKeys.UNDECLARE_PREFIXES, value);
                } else {
                    DynamicError err = new DynamicError("undeclare-namespaces value must be 'yes' or 'no'");
                    err.setErrorCode("XTDE0030");
                    throw err;
                }
            } else

            if (lname.equals(StandardNames.INCLUDE_CONTENT_TYPE)) {
                if (prevalidated || value.equals("yes") || value.equals("no")) {
                    details.put(SaxonOutputKeys.INCLUDE_CONTENT_TYPE, value);
                } else {
                    DynamicError err = new DynamicError("include-content-type attribute must be 'yes' or 'no'");
                    err.setErrorCode("XTDE0030");
                    throw err;
                }
            } else

            if (lname.equals(StandardNames.ESCAPE_URI_ATTRIBUTES)) {
                if (prevalidated || value.equals("yes") || value.equals("no")) {
                    details.put(SaxonOutputKeys.ESCAPE_URI_ATTRIBUTES, value);
                } else {
                    DynamicError err = new DynamicError("escape-uri-attributes value must be 'yes' or 'no'");
                    err.setErrorCode("XTDE0030");
                    throw err;
                }
            } else

            if (lname.equals(StandardNames.NORMALIZATION_FORM)) {
                if (XML11Char.isXML11ValidNmtoken(value)) {
//                if (prevalidated || value.equals("NFC") || value.equals("NFD") ||
//                        value.equals("NFKC") || value.equals("NFKD")) {
                    details.put(SaxonOutputKeys.NORMALIZATION_FORM, value);
                } else if (value.equals("none")) {
                    // do nothing
                } else {
                    DynamicError err = new DynamicError("normalization-form must be a valid NMTOKEN");
                    err.setErrorCode("XTDE0030");
                    throw err;
                }
            }

        } else if (uri.equals(NamespaceConstant.SAXON)) {

            if (lname.equals("character-representation")) {
                details.put(SaxonOutputKeys.CHARACTER_REPRESENTATION, value);
            } else

            if (lname.equals("indent-spaces")) {
                try {
                    Integer.parseInt(value);
                    details.put(OutputKeys.INDENT, "yes");
                    details.put(SaxonOutputKeys.INDENT_SPACES, value);
                } catch (NumberFormatException err) {
                    DynamicError e = new DynamicError("saxon:indent-spaces must be an integer");
                    e.setErrorCode(NamespaceConstant.SAXON, SaxonErrorCode.SXWN9002);
                    throw e;
                }
            } else

            if (lname.equals("next-in-chain")) {
                DynamicError e = new DynamicError("saxon:next-in-chain value cannot be specified dynamically");
                e.setErrorCode(NamespaceConstant.SAXON, SaxonErrorCode.SXWN9004);
                throw e;
            } else

            if (lname.equals("require-well-formed")) {
                if (prevalidated || value.equals("yes") || value.equals("no")) {
                    details.put(SaxonOutputKeys.REQUIRE_WELL_FORMED, value);
                } else {
                    DynamicError e = new DynamicError("saxon:require-well-formed value must be 'yes' or 'no'");
                    e.setErrorCode(NamespaceConstant.SAXON, SaxonErrorCode.SXWN9003);
                    throw e;
                }
            }

        } else {

            // deal with user-defined attributes
            details.put('{' + uri + '}' + lname, value);
        }

    }

    /**
     * Diagnostic print of expression structure. The expression is written to the System.err
     * output stream
     *
     * @param level indentation level for this expression
     @param out
     @param config
     */

    public void display(int level, PrintStream out, Configuration config) {
        out.println(ExpressionTool.indent(level) + "result-document");
        content.display(level + 1, out, config);
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
// Additional Contributor(s): Brett Knights [brett@knightsofthenet.com]
//
