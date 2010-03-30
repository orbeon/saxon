package org.orbeon.saxon.xpath;
import org.orbeon.saxon.AugmentedSource;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.dom.DocumentWrapper;
import org.orbeon.saxon.dom.NodeWrapper;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.expr.ExpressionTool;
import org.orbeon.saxon.expr.ExpressionVisitor;
import org.orbeon.saxon.expr.XPathContextMajor;
import org.orbeon.saxon.instruct.Executable;
import org.orbeon.saxon.instruct.SlotManager;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.trans.SaxonErrorCode;
import org.orbeon.saxon.type.SchemaException;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.Value;
import org.orbeon.saxon.value.Whitespace;
import org.xml.sax.InputSource;
import org.w3c.dom.Node;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
  * <p>XPathEvaluator provides a simple API for standalone XPath processing (that is,
  * executing XPath expressions in the absence of an XSLT stylesheet). It is an implementation
  * of the JAXP 1.3 XPath interface, with additional methods provided (a) for backwards
  * compatibility (b) to give extra control over the XPath evaluation, and (c) to support
  * XPath 2.0.</p>
  *
  * <p>It is intended to evolve this so that it only supports the JAXP style of operation.
  * Some of the methods are therefore marked as deprecated in this release, and will be
  * dropped in a future release.</p>
  *
  * <p>For an alternative XPath API, offering more direct access to Saxon capabilities,
  * see {@link org.orbeon.saxon.sxpath.XPathEvaluator}.</p>
  *
  * <p>Note that the <code>XPathEvaluator</code> links to a Saxon {@link Configuration}
  * object. By default a new <code>Configuration</code> is created automatically. In many
  * applications, however, it is desirable to share a configuration. The default configuration
  * is not schema aware. All source documents used by XPath expressions under this evaluator
  * must themselves be built using the <code>Configuration</code> used by this evaluator.</p>
  *
  * @author Michael H. Kay
  */

public class XPathEvaluator implements XPath {

    private Configuration config;
    private NodeInfo contextNode = null;
    private JAXPXPathStaticContext staticContext;
    private boolean stripSpace = false;

    /**
     * Default constructor. Creates an XPathEvaluator with a default configuration and name pool.
     * The default Configuration is not schema-aware.
     */

    public XPathEvaluator() {
        this(new Configuration());
    }

    /**
     * Construct an XPathEvaluator with a specified configuration.
     * @param config the configuration to be used. If schema-aware XPath expressions are to be used,
     * this must be a SchemaAwareConfiguration.
     */
    public XPathEvaluator(Configuration config) {
        this.config = config;
        staticContext = new JAXPXPathStaticContext(config);
    }

    /**
    * Construct an XPathEvaluator to process a particular source document. This is equivalent to
    * using the default constructor and immediately calling setSource().
    * @param source The source document (or a specific node within it).
    */

    public XPathEvaluator(Source source) throws org.orbeon.saxon.trans.XPathException {
        if (source instanceof NodeInfo) {
            config = ((NodeInfo)source).getDocumentRoot().getConfiguration();
        } else {
            config = new Configuration();
        }
        staticContext = new JAXPXPathStaticContext(config);
        setSource(source);
    }

    /**
     * Get the Configuration used by this XPathEvaluator
     * @return the Configuration used by this XPathEvaluator
     */

    public Configuration getConfiguration() {
        return config;
    }

    /**
     * Indicate whether all whitespace text nodes in the source document are to be
     * removed. This option has no effect unless it is called before the call on setSource(),
     * and unless the Source supplied to setSource() is a SAXSource or StreamSource.
     * @param strip True if all whitespace text nodes are to be stripped from the source document,
     * false otherwise. The default if the method is not called is false.
     * @deprecated since 8.9. The preferred way to define options for the way in which source
     * documents are built is to use the class {@link org.orbeon.saxon.AugmentedSource} for any
     * of the methods expecting a {@link Source} object.
    */

    public void setStripSpace(boolean strip) {
        stripSpace = strip;
    }

    /**
     * Supply the document against which XPath expressions are to be executed. This
     * method must be called before preparing or executing an XPath expression.
     * Setting a new source document clears all the namespaces that have been declared.
     * @param source Any javax.xml.transform.Source object representing the document against
     * which XPath expressions will be executed. Note that a Saxon {@link org.orbeon.saxon.om.DocumentInfo DocumentInfo}
     * (indeed any {@link org.orbeon.saxon.om.NodeInfo NodeInfo})
     * can be used as a Source. To use a third-party DOM Document as a source, create an instance of
     * {@link javax.xml.transform.dom.DOMSource DOMSource} to wrap it.
     *  <p>The Source object supplied also determines the initial setting
     * of the context item. In most cases the context node will be the root of the supplied document;
     * however, if a NodeInfo or DOMSource is supplied it can be any node in the document. </p>
     * @return the NodeInfo of the start node in the resulting document object.
    */

    public NodeInfo setSource(Source source) throws org.orbeon.saxon.trans.XPathException {
        if (source instanceof DOMSource) {
            Node node = ((DOMSource)source).getNode();
            String baseURI = source.getSystemId();
            DocumentWrapper documentWrapper = new DocumentWrapper(node.getOwnerDocument(), baseURI, config);
            NodeWrapper nodeWrapper = documentWrapper.wrap(node);
            if (stripSpace) {
                StrippedDocument sdoc = new StrippedDocument(documentWrapper, AllElementStripper.getInstance());
                return sdoc.wrap(nodeWrapper);
            } else {
                return nodeWrapper;
            }
        } else if (source instanceof NodeInfo) {
            NodeInfo origin = (NodeInfo)source;
            if (!origin.getConfiguration().isCompatible(config)) {
                throw new org.orbeon.saxon.trans.XPathException(
                        "Supplied node must be built using the same or a compatible Configuration",
                        SaxonErrorCode.SXXP0004);
            }
            if (stripSpace) {
                StrippedDocument sdoc = new StrippedDocument(origin.getDocumentRoot(), AllElementStripper.getInstance());
                return sdoc.wrap(origin);
            } else {
                return origin;
            }
        } else {
            if (stripSpace) {
                AugmentedSource as = AugmentedSource.makeAugmentedSource(source);
                as.setStripSpace(Whitespace.ALL);
                source = as;
            }
            return config.buildDocument(source);
        }
    }

    /**
     * Set the static context for compiling XPath expressions. This provides control over the
     * environment in which the expression is compiled, for example it allows namespace prefixes to
     * be declared, variables to be bound and functions to be defined. For most purposes, the static
     * context can be defined by providing and tailoring an instance of the JAXPXPathStaticContext class.
     * Until this method is called, a default static context is used, in which no namespaces are defined
     * other than the standard ones (xml, xslt, and saxon), and no variables or functions (other than the
     * core XPath functions) are available.
     * @param context the static context
     * @throws IllegalArgumentException if the supplied static context uses a different and incompatible
     * Configuration from the one used in this XPathEvaluator
    */

    public void setStaticContext(JAXPXPathStaticContext context) {
        if (!config.isCompatible(context.getConfiguration())) {
            throw new IllegalArgumentException("Supplied static context uses a different and incompatible Configuration");
        }
        staticContext = context;
    }

    /**
     * Get the current static context
     * @return the static context
    */

    public JAXPXPathStaticContext getStaticContext() {
        return staticContext;
    }

    /**
     * Get the executable
     * @return the executable
     */

    public Executable getExecutable() {
        return staticContext.getExecutable();
    }

    /**
     * Prepare an XPath expression for subsequent evaluation.
     * @param expression The XPath expression to be evaluated, supplied as a string.
     * @return an XPathExpression object representing the prepared expression
     * @throws org.orbeon.saxon.trans.XPathException if the syntax of the expression is wrong, or if it references namespaces,
     * variables, or functions that have not been declared.
     * @deprecated since Saxon 8.9 - use {@link #compile(String)}
    */

    public XPathExpressionImpl createExpression(String expression) throws org.orbeon.saxon.trans.XPathException {
        return createExpressionInternal(expression);
    }

    private XPathExpressionImpl createExpressionInternal(String expression) throws org.orbeon.saxon.trans.XPathException {
        Expression exp = ExpressionTool.make(expression, staticContext, 0, -1, 1, false);
        ExpressionVisitor visitor = ExpressionVisitor.make(staticContext);
        visitor.setExecutable(getExecutable());
        exp = visitor.typeCheck(exp, Type.ITEM_TYPE);
        SlotManager map = staticContext.getConfiguration().makeSlotManager();
        ExpressionTool.allocateSlots(exp, 0, map);
        exp.setContainer(staticContext);
        XPathExpressionImpl xpe = new XPathExpressionImpl(exp, getExecutable());
        xpe.setStackFrameMap(map);
        if (contextNode != null) {
            xpe.privatelySetContextNode(contextNode);
        }
        return xpe;
    }

    /**
     * Set the context node. This provides the context node for any expressions executed after this
     * method is called, including expressions that were prepared before it was called.
     * @param node The node to be used as the context node. The node must be within a tree built using
     * the same Saxon {@link Configuration} as used by this XPathEvaluator.
     * @deprecated since Saxon 8.9 - use the various method defined in the JAXP interface definition,
     * which allow a NodeInfo object to be supplied as the value of the Source argument
     * @throws IllegalArgumentException if the supplied node was built using the wrong Configuration
    */

    public void setContextNode(NodeInfo node) {
        if (!node.getConfiguration().isCompatible(config)) {
            throw new IllegalArgumentException(
                    "Supplied node must be built using the same or a compatible Configuration");
        }
        contextNode = node;
    }

    /**
     * Prepare and execute an XPath expression, supplied as a string, and returning the results
     * as a List.
     * @param expression The XPath expression to be evaluated, supplied as a string.
     * @return The results of the expression, as a List. The List represents the sequence
     * of items returned by the expression. Each item in the list will either be an object
     *  representing a node, or a Java object representing an atomic value.
     * The types of Java object that may be included in the list, and the XML Schema data types that they
     * correspond to, are as follows:<p>
     * <ul>
     * <li>Boolean (xs:boolean)</li>
     * <li>String (xs:string)</li>
     * <li>BigDecimal (xs:decimal)</li>
     * <li>Long (xs:integer and its derived types)</li>
     * <li>Double (xs:double)</li>
     * <li>Float (xs:float)</li>
     * <li>Date (xs:date, xs:dateTime)</li>
     * </ul>
     * @deprecated since Saxon 8.9 - use the various method defined in the JAXP interface definition
    */

    public List evaluate(String expression) throws org.orbeon.saxon.trans.XPathException {
        Expression exp = ExpressionTool.make(expression, staticContext,0,-1,1, false);
        ExpressionVisitor visitor = ExpressionVisitor.make(staticContext);
        visitor.setExecutable(getExecutable());
        exp = visitor.typeCheck(exp, Type.ITEM_TYPE);
        SlotManager map = staticContext.getConfiguration().makeSlotManager();
        ExpressionTool.allocateSlots(exp, 0, map);
        XPathContextMajor context = new XPathContextMajor(contextNode, staticContext.getExecutable());
        context.openStackFrame(map);
        SequenceIterator iterator = exp.iterate(context);
        ArrayList list = new ArrayList(20);
        while (true) {
            Item item = iterator.next();
            if (item == null) {
                return list;
            }
            list.add(Value.convertToJava(item));
        }
    }

    public void reset() {
        contextNode = null;
        stripSpace = false;
        staticContext = new JAXPXPathStaticContext(config);
    }

    /**
     * Set XPath 1.0 compatibility mode on or off (by default, it is false). This applies
     * to any XPath expression compiled while this option is in force.
     * @param compatible true if XPath 1.0 compatibility mode is to be set to true, false
     * if it is to be set to false.
     */

    public void setBackwardsCompatible(boolean compatible) {
        staticContext.setBackwardsCompatibilityMode(compatible);
    }

    /**
     * Get the value of XPath 1.0 compatibility mode
     * @return true if XPath 1.0 compatibility mode is set
     */

    public boolean isBackwardsCompatible() {
        return staticContext.isInBackwardsCompatibleMode();
    }

    /**
     * Set the resolver for XPath variables
     * @param xPathVariableResolver a resolver for variables
     */

    public void setXPathVariableResolver(XPathVariableResolver xPathVariableResolver) {
        staticContext.setXPathVariableResolver(xPathVariableResolver);
    }

    /**
     * Get the resolver for XPath variables
     * @return the resolver, if one has been set
     */
    public XPathVariableResolver getXPathVariableResolver() {
        return staticContext.getXPathVariableResolver();
    }

    /**
     * Set the resolver for XPath functions
     * @param xPathFunctionResolver a resolver for XPath function calls
     */

    public void setXPathFunctionResolver(XPathFunctionResolver xPathFunctionResolver) {
        staticContext.setXPathFunctionResolver(xPathFunctionResolver);
    }

    /**
     * Get the resolver for XPath functions
     * @return the resolver, if one has been set
     */

    public XPathFunctionResolver getXPathFunctionResolver() {
        return staticContext.getXPathFunctionResolver();
    }

    /**
     * Set the namespace context to be used.
     * @param namespaceContext The namespace context
     */

    public void setNamespaceContext(NamespaceContext namespaceContext) {
        staticContext.setNamespaceContext(namespaceContext);
    }

    /**
     * Get the namespace context, if one has been set using {@link #setNamespaceContext}
     * @return the namespace context if set, or null otherwise
     */

    public NamespaceContext getNamespaceContext() {
        return staticContext.getNamespaceContext();
    }

    /**
     * Import a schema. This is possible only if the schema-aware version of Saxon is being used,
     * and if the Configuration is a SchemaAwareConfiguration. Having imported a schema, the types
     * defined in that schema become part of the static context.
     * @param source A Source object identifying the schema document to be loaded
     * @throws org.orbeon.saxon.type.SchemaException if the schema contained in this document is invalid
     * @throws UnsupportedOperationException if the configuration is not schema-aware
     */

    public void importSchema(Source source) throws SchemaException {
        staticContext.importSchema(source);
    }

    /**
     * Compile an XPath 2.0 expression
     * @param expr the XPath 2.0 expression to be compiled, as a string
     * @return the compiled form of the expression
     * @throws XPathExpressionException if there are any static errors in the expression.
     * Note that references to undeclared variables are not treated as static errors, because
     * variables are not pre-declared using this API.
     */
    public XPathExpression compile(String expr) throws XPathExpressionException {
        if (expr == null) {
            throw new NullPointerException("expr");
        }
        try {
            return createExpressionInternal(expr);
        } catch (org.orbeon.saxon.trans.XPathException e) {
            throw new XPathExpressionException(e);
        }
    }

    /**
     * Single-shot method to compile and execute an XPath 2.0 expression.
     * @param expr The XPath 2.0 expression to be compiled and executed
     * @param node The context node for evaluation of the expression.
     *
     * <p>This may be a NodeInfo object, representing a node in Saxon's native
     * implementation of the data model, or it may be a node in any supported
     * external object model: DOM, JDOM, DOM4J, or XOM, or any other model for
     * which support has been configured in the Configuration. Note that the
     * supporting libraries for the chosen model must be on the class path.</p>
     *
     * <p><b>Contrary to the interface specification, Saxon does not supply an empty
     * document when the value is null. This is because Saxon supports multiple object models,
     * and it's unclear what kind of document node would be appropriate. Instead, Saxon uses
     * the node supplied to the {@link #setContextNode} method if available, and if none
     * is available, executes the XPath expression with the context item undefined.</p></p>
     *
     * @param qName The type of result required. For details, see
     *  {@link XPathExpressionImpl#evaluate(Object, javax.xml.namespace.QName)}
     * @return the result of evaluating the expression, returned as described in
     *  {@link XPathExpressionImpl#evaluate(Object, javax.xml.namespace.QName)}
     * @throws XPathExpressionException if any static or dynamic error occurs
     * in evaluating the expression.
     */

    public Object evaluate(String expr, Object node, QName qName) throws XPathExpressionException {
        XPathExpression exp = compile(expr);
        return exp.evaluate(node, qName);
    }

    /**
     * Single-shot method to compile an execute an XPath 2.0 expression, returning
     * the result as a string.
     * @param expr The XPath 2.0 expression to be compiled and executed
     * @param node The context node for evaluation of the expression
     *
     * <p>This may be a NodeInfo object, representing a node in Saxon's native
     * implementation of the data model, or it may be a node in any supported
     * external object model: DOM, JDOM, DOM4J, or XOM, or any other model for
     * which support has been configured in the Configuration. Note that the
     * supporting libraries for the chosen model must be on the class path.</p>
     *
     * <p><b>Contrary to the interface specification, Saxon does not supply an empty
     * document when the value is null. This is because Saxon supports multiple object models,
     * and it's unclear what kind of document node would be appropriate. Instead, Saxon uses
     * the node supplied to the {@link #setContextNode} method if available, and if none
     * is available, executes the XPath expression with the context item undefined.</p></p>
     * @return the result of evaluating the expression, converted to a string as if
     * by calling the XPath string() function
     * @throws XPathExpressionException if any static or dynamic error occurs
     * in evaluating the expression.
     */

    public String evaluate(String expr, Object node) throws XPathExpressionException {
        XPathExpression exp = compile(expr);
        return exp.evaluate(node);
    }

    /**
     * Single-shot method to parse and build a source document, and
     * compile an execute an XPath 2.0 expression, against that document
     * @param expr The XPath 2.0 expression to be compiled and executed
     * @param inputSource The source document: this will be parsed and built into a tree,
     * and the XPath expression will be executed with the root node of the tree as the
     * context node.
     * @param qName The type of result required. For details, see
     *  {@link XPathExpressionImpl#evaluate(Object, javax.xml.namespace.QName)}
     * @return the result of evaluating the expression, returned as described in
     *  {@link XPathExpressionImpl#evaluate(Object, javax.xml.namespace.QName)}
     * @throws XPathExpressionException if any static or dynamic error occurs
     * in evaluating the expression.
     * @throws NullPointerException if any of the three arguments is null
     */

    public Object evaluate(String expr, InputSource inputSource, QName qName) throws XPathExpressionException {
        if (expr == null) {
            throw new NullPointerException("expr");
        }
        if (inputSource == null) {
            throw new NullPointerException("inputSource");
        }
        if (qName == null) {
            throw new NullPointerException("qName");
        }
        XPathExpression exp = compile(expr);
        return exp.evaluate(inputSource, qName);
    }

    /**
     * Single-shot method to parse and build a source document, and
     * compile an execute an XPath 2.0 expression, against that document,
     * returning the result as a string
     * @param expr The XPath 2.0 expression to be compiled and executed
     * @param inputSource The source document: this will be parsed and built into a tree,
     * and the XPath expression will be executed with the root node of the tree as the
     * context node
     * @return the result of evaluating the expression, converted to a string as
     * if by calling the XPath string() function
     * @throws XPathExpressionException if any static or dynamic error occurs
     * in evaluating the expression.
     * @throws NullPointerException if either of the two arguments is null
     */

    public String evaluate(String expr, InputSource inputSource) throws XPathExpressionException {
        if (expr == null) {
            throw new NullPointerException("expr");
        }
        if (inputSource == null) {
            throw new NullPointerException("inputSource");
        }
        XPathExpression exp = compile(expr);
        return exp.evaluate(inputSource);
    }

    /**
    * Prepare and execute an XPath expression, supplied as a string, and returning the first
    * item in the result. This is useful where it is known that the expression will only return
    * a singleton value (for example, a single node, or a boolean).
    * @param expression The XPath expression to be evaluated, supplied as a string.
    * @return The first item in the sequence returned by the expression. If the expression
    * returns an empty sequence, this method returns null. Otherwise, it returns the first
    * item in the result sequence, represented as a Java object using the same mapping as for
    * the evaluate() method
     * @deprecated since Saxon 8.9 - use the methods defined in the JAXP interface
    */

    public Object evaluateSingle(String expression) throws org.orbeon.saxon.trans.XPathException {
        Expression exp = ExpressionTool.make(expression, staticContext,0,-1,1, false);
        ExpressionVisitor visitor = ExpressionVisitor.make(staticContext);
        visitor.setExecutable(getExecutable());
        exp = visitor.typeCheck(exp, Type.ITEM_TYPE);
        SlotManager map = staticContext.getConfiguration().makeSlotManager();
        ExpressionTool.allocateSlots(exp, 0, map);
        XPathContextMajor context = new XPathContextMajor(contextNode, staticContext.getExecutable());
        context.openStackFrame(map);
        SequenceIterator iterator = exp.iterate(context);
        Item item = iterator.next();
        if (item == null) {
            return null;
        } else {
            return Value.convertToJava(item);
        }
    }

    /**
     * A simple command-line interface for the XPathEvaluator (not documented).
     * @param args command line arguments.
     * First parameter is the filename containing the source document, second
     * parameter is the XPath expression.
     */

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("format: java XPathEvaluator source.xml \"expression\"");
            return;
        }
        XPathEvaluator xpe = new XPathEvaluator();
        List results = (List)xpe.evaluate(args[1], new StreamSource(new File(args[0])), XPathConstants.NODESET);
        for (int i = 0; i < results.size(); i++) {
            Object o = results.get(i);
            System.err.println(o);
        }


//        Configuration config = new Configuration();
//        config.setLineNumbering(true);
//        XPathEvaluator xpath = new XPathEvaluator(config);
//        xpath.setXPathVariableResolver(
//                new XPathVariableResolver() {
//                    public Object resolveVariable(QName variableName) {
//                        if (variableName.getLocalPart().equals("in")) {
//                            return "a/b/c/a";
//                        }
//                        return null;
//                    }
//                }
//        );
//        NodeInfo doca = xpath.setSource(new StreamSource((new File(args[0]))));
//        List list = xpath.evaluate("for $v in distinct-values(tokenize($in, '/')) return concat(' +', $v)");
//        for (int i=0; i<list.size(); i++) {
//            String s = (String)list.get(i);
//            System.out.println(s);
//        }

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
// The Initial Developer of the Original Code is Michael H. Kay
//
// Contributor(s):
//
