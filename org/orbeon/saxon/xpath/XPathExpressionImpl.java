package org.orbeon.saxon.xpath;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.expr.Atomizer;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.expr.XPathContextMajor;
import org.orbeon.saxon.functions.NumberFn;
import org.orbeon.saxon.instruct.SlotManager;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.pattern.NodeTest;
import org.orbeon.saxon.sort.SortKeyDefinition;
import org.orbeon.saxon.sort.SortedIterator;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.*;
import org.xml.sax.InputSource;

import javax.xml.namespace.QName;
import javax.xml.transform.sax.SAXSource;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import java.util.Comparator;
import java.util.List;

/**
  * <p>The JAXP XPathExpression interface represents a compiled XPath expression that can be repeatedly
  * evaluated. This class is Saxon's implementation of that interface.</p>
  *
  * <p>The class also includes some methods retained from Saxon's original XPath API. When these methods
  * are used, the object contains the context node and other state, so it is not thread-safe.</p>
 *
  * @author Michael H. Kay
  */


public class XPathExpressionImpl implements XPathExpression {

    private Configuration config;
    private Expression expression;
    private NodeInfo contextNode;
    private SlotManager stackFrameMap;
    private XPathExpressionImpl sortKey = null;

    /**
    * The constructor is protected, to ensure that instances can only be
    * created using the createExpression() method of XPathEvaluator
    */

    protected XPathExpressionImpl(Expression exp, Configuration config) {
        expression = exp;
        this.config = config;
    }

    /**
     * Define the number of slots needed for local variables within the expression.
     * This method is for internal use only.
     */

    protected void setStackFrameMap(SlotManager map) {
        stackFrameMap = map;
    }

    /**
     * Get the stack frame map. This holds information about the allocation of slots to variables.
     * This is needed by applications using low-level interfaces for evaluating the expression
     */

    public SlotManager getStackFrameMap() {
        return stackFrameMap;
    }

    /**
     * Get the Configuration under which this XPath expression was compiled
     */

    public Configuration getConfiguration() {
        return config;
    }

    /**
    * Define the sort order for the results of the expression. If this method is called, then
    * the list returned by a subsequent call on the evaluate() method will first be sorted.
    * @param sortKey an XPathExpression, which will be applied to each item in the sequence;
    * the result of this expression determines the ordering of the list returned by the evaluate()
    * method. The sortKey can be null, to clear a previous sort key.
    */

    public void setSortKey(XPathExpressionImpl sortKey) {
        this.sortKey = sortKey;
    }

    /**
    * Set the context node for evaluating the expression. If this method is not called,
    * the context node will be the root of the document to which the prepared expression is
    * bound.
    */

    public void setContextNode(NodeInfo node) {
        if (node==null) {
            throw new NullPointerException("Context node cannot be null");
        }
        if (node.getNamePool() != config.getNamePool()) {
            throw new IllegalArgumentException("Supplied node uses the wrong NamePool");
        }
        contextNode = node;
    }


    /**
    * Execute a prepared XPath expression, returning the results as a List. The context
     * node must have been set previously using {@link #setContextNode(org.orbeon.saxon.om.NodeInfo)}.
    * @return The results of the expression, as a List. The List represents the sequence
    * of items returned by the expression. Each item in the list will either be an instance
    * of org.orbeon.saxon.om.NodeInfo, representing a node, or a Java object representing an atomic value.
    * For the types of Java object that may be returned, see {@link #evaluate(Object, javax.xml.namespace.QName)}
     * with the second argument set to NODESET.
    */

    public List evaluate() throws XPathException {
        XPathContextMajor context = new XPathContextMajor(contextNode, config);
        context.openStackFrame(stackFrameMap);
        SequenceIterator iter = expression.iterate(context);
        SequenceExtent extent = new SequenceExtent(iter);
        return (List)extent.convertToJava(Object.class, context);
    }

    /**
    * Execute a prepared XPath expression, returning the first item in the result.
    * This is useful where it is known that the expression will only return
    * a singleton value (for example, a single node, or a boolean). The context node
     * must be set previously using {@link #setContextNode(org.orbeon.saxon.om.NodeInfo)}.
    * @return The first item in the sequence returned by the expression. If the expression
    * returns an empty sequence, this method returns null. Otherwise, it returns the first
    * item in the result sequence, represented as a Java object using the same mapping as for
    * the evaluate() method
    */

    public Object evaluateSingle() throws XPathException {
        XPathContextMajor context = new XPathContextMajor(contextNode, config);
        context.openStackFrame(stackFrameMap);
        SequenceIterator iterator = expression.iterate(context);
        Item item = iterator.next();
        if (item == null) {
            return null;
        } else {
            return Value.convert(item);
        }
    }

    /**
    * Get a raw iterator over the results of the expression. This returns results without
    * any conversion of the returned items to "native" Java classes. This method is intended
    * for use by applications that need to process the results of the expression using
    * internal Saxon interfaces.
    */

    public SequenceIterator rawIterator() throws XPathException {
        XPathContextMajor context = new XPathContextMajor(contextNode, config);
        context.openStackFrame(stackFrameMap);
        SequenceIterator iterator = expression.iterate(context);
        if (sortKey != null) {
            Expression key = sortKey.expression;
            if (key.getItemType(config.getTypeHierarchy()) instanceof NodeTest) {
                key = new Atomizer(key, config);
            }

            SortKeyDefinition[] sk = new SortKeyDefinition[1];

            sk[0] = new SortKeyDefinition();
            sk[0].setSortKey(key);
            Comparator comp = sk[0].makeComparator(context);
            sk[0].setComparer(comp);
            Comparator[] comps = {comp};

            iterator = new SortedIterator(context, iterator, sk, comps);
            ((SortedIterator)iterator).setHostLanguage(Configuration.XPATH);
        }
        return iterator;
    }

    /**
     * JAXP 1.3 evaluate() method
     * @param node The context node. This must use a representation of nodes that this implementation understands.
     * This may be a Saxon NodeInfo, or a node in one of the external object models supported, for example
     * DOM, JDOM, or XOM, provided the support module for that object model is loaded.
     * @param qName Indicates the type of result required. This must be one of the constants defined in
     * the JAXP {@link XPathConstants} class.
     * Saxon will attempt to convert the actual result of the expression to the required type using the
     * XPath 1.0 conversion rules.
     * @return the result of the evaluation, as a Java object of the appropriate type. Saxon interprets the
     * rules as follows:
     * <table>
     * <thead><tr><td>QName</td><td>Return Value</td></thead>
     * <tbody>
     *   <tr><td>BOOLEAN</td>
     *       <td>The effective boolean value of the actual result,
     *           as a Java Boolean object</td></tr>
     *   <tr><td>STRING</td>
     *       <td>The result of applying the string() function to the actual result,
     *           as a Java String object</td></tr>
     *   <tr><td>NUMBER</td>
     *       <td>The result of applying the number() function to the actual result,
     *           as a Java Double object</td></tr>
     *   <tr><td>NODE</td>
     *       <td>A single node, in the native data model supplied as input. If the
     *           expression returns more than one node, the first is returned. If
     *           the expression returns an empty sequence, null is returned. If the
     *           expression returns an atomic value, or if the first item in the
     *           result sequence is an atomic value, an exception is thrown.</td></tr>
     *   <tr><td>NODESET</td>
     *       <td>This is interpreted as allowing any sequence, of nodes or atomic values.
     *           The result is returned as a Java List object, unless it is empty, in which
     *           case null is returned. The contents of the list may be node objects (in the
     *           native data model supplied as input, or Java objects representing the XPath
     *           atomic values in the actual result: String for an xs:string, Double for a xs:double,
     *           Long for an xs:integer, and so on. (For safety, cast the values to a type
     *           such as xs:string within the XPath expression). Note that the result is never
     *           returned as a DOM NodeList.</td></tr></table>
     *
     * @throws XPathExpressionException if evaluation of the expression fails or if the
     * result cannot be converted to the requested type.
     */
    public Object evaluate(Object node, QName qName) throws XPathExpressionException {
        ExternalObjectModel model = null;
        if (node instanceof NodeInfo) {
            setContextNode((NodeInfo)node);
        } else {
            model = config.findExternalObjectModel(node);
            if (model == null) {
                throw new XPathExpressionException(
                        "Cannot locate an object model implementation for nodes of class "
                        + node.getClass().getName());
            }
            DocumentInfo doc = model.wrapDocument(node, "", config);
            NodeInfo startNode = model.wrapNode(doc, node);
            setContextNode(startNode);
        }
        XPathContextMajor context = new XPathContextMajor(contextNode, config);
        context.openStackFrame(stackFrameMap);
        try {
            if (qName.equals(XPathConstants.BOOLEAN)) {
                return Boolean.valueOf(expression.effectiveBooleanValue(context));
            } else if (qName.equals(XPathConstants.STRING)) {
                SequenceIterator iter = expression.iterate(context);

                Item first = iter.next();
                if (first == null) {
                    return "";
                }
                return first.getStringValue();

            } else if (qName.equals(XPathConstants.NUMBER)) {
                SequenceIterator iter = new Atomizer(expression, config).iterate(context);

                Item first = iter.next();
                if (first == null) {
                    return new Double(Double.NaN);
                }
                if (first instanceof NumericValue) {
                    return new Double(((NumericValue)first).getDoubleValue());
                } else {
                    DoubleValue v = NumberFn.convert((AtomicValue)first);
                    return new Double(v.getDoubleValue());
                }

            } else if (qName.equals(XPathConstants.NODE)) {
                SequenceIterator iter = expression.iterate(context);
                Item first = iter.next();
                if (first instanceof VirtualNode) {
                    return ((VirtualNode)first).getUnderlyingNode();
                }
                if (first == null || first instanceof NodeInfo) {
                    return first;
                }
                throw new XPathExpressionException("Expression result is not a node");
            } else if (qName.equals(XPathConstants.NODESET)) {
                SequenceIterator iter = expression.iterate(context);
                SequenceExtent extent = new SequenceExtent(iter);
                if (model != null) {
                    Object result = model.convertToNodeList(extent);
                    if (result != null) {
                        return result;
                    }
                }
                return extent.convertToJava(List.class, context);
            } else {
                throw new IllegalArgumentException("Unknown type for expected result");
            }
        } catch (XPathException e) {
            throw new XPathExpressionException(e);
        }
    }

    /**
     * Evaluate the expression to return a string value
     * @param node the initial context node. This must be an instance of NodeInfo
     * @return the results of the expression, converted to a String
     * @throws XPathExpressionException if evaluation fails
     */

    public String evaluate(Object node) throws XPathExpressionException {
        return (String)evaluate(node, XPathConstants.STRING);
    }

    /**
     * Evaluate the XPath expression against an input source to obtain a result of a specified type
     * @param inputSource The input source document against which the expression is evaluated.
     * (Note that there is no caching. This will be parsed, and the parsed result will be discarded.)
     * @param qName The type required, identified by a constant in {@link XPathConstants}
     * @return the result of the evaluation, as a Java object of the appropriate type:
     * see {@link #evaluate(Object, javax.xml.namespace.QName)}
     * @throws XPathExpressionException
     */
    public Object evaluate(InputSource inputSource, QName qName) throws XPathExpressionException {
        try {
            NodeInfo doc = new XPathEvaluator().setSource(new SAXSource(inputSource));
            return evaluate(doc, qName);
        } catch (XPathException e) {
            throw new XPathExpressionException(e);
        }
    }

    /**
     * Evaluate the XPath expression against an input source to obtain a string result
     * @param inputSource The input source document against which the expression is evaluated.
     * (Note that there is no caching. This will be parsed, and the parsed result will be discarded.)
     * @return the result of the evaluation, converted to a String
     * @throws XPathExpressionException
     */

    public String evaluate(InputSource inputSource) throws XPathExpressionException {
        try {
            NodeInfo doc = new XPathEvaluator().setSource(new SAXSource(inputSource));
            return (String)evaluate(doc, XPathConstants.STRING);
        } catch (XPathException e) {
            throw new XPathExpressionException(e);
        }
    }

    /**
     * Low-level method to get the internal Saxon expression object. This exposes a wide range of
     * internal methods that may be needed by specialized applications, and allows greater control
     * over the dynamic context for evaluating the expression.
     * @return the underlying Saxon expression object.
     */

    public Expression getInternalExpression() {
        return expression;
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

// The Initial Developer of the Original Code is
// Michael H. Kay.
//
// Contributor(s):
//
