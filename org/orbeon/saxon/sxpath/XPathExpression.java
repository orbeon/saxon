package net.sf.saxon.sxpath;

import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.XPathContextMajor;
import net.sf.saxon.instruct.SlotManager;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.SequenceExtent;
import net.sf.saxon.value.Value;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.Item;

import javax.xml.transform.Source;
import java.util.Collections;
import java.util.List;

/**
 * This class is a representation of an XPath Expression for use with the XPathEvaluator class.
 * It is modelled on the XPath API defined in JAXP 1.3, but is cut down to remove any dependencies on JAXP 1.3,
 * making it suitable for use on vanilla JDK 1.4 installations.
 *
 * @author Michael H. Kay
 */


public class XPathExpression {

    private XPathEvaluator evaluator;
    private Expression expression;
    private SlotManager stackFrameMap;

    /**
     * The constructor is protected, to ensure that instances can only be
     * created using the createExpression() method of XPathEvaluator
     */

    protected XPathExpression(XPathEvaluator evaluator, Expression exp) {
        expression = exp;
        this.evaluator = evaluator;
    }

    /**
     * Define the number of slots needed for local variables within the expression
     */

    protected void setStackFrameMap(SlotManager map) {
        stackFrameMap = map;
    }

    /**
     * Execute a prepared XPath expression, returning the results as a List.
     *
     * @param source the document or other node against which the XPath expression
     *                    will be evaluated. This may be a Saxon NodeInfo object, representing a node in an
     *                    existing tree, or it may be any kind of JAXP Source object such as a StreamSource
     *                    SAXSource or DOMSource.
     * @return The results of the expression, as a List. The List represents the sequence
     *         of items returned by the expression. Each item in the list will either be an instance
     *         of net.sf.saxon.om.NodeInfo, representing a node, or a Java object representing an atomic value.
     */

    public List evaluate(Source source) throws XPathException {
        NodeInfo origin;
        if (source instanceof NodeInfo) {
            origin = (NodeInfo)source;
        } else {
            origin = evaluator.build(source);
        }
        XPathContextMajor context = new XPathContextMajor(origin, evaluator.getConfiguration());
        context.openStackFrame(stackFrameMap);
        SequenceIterator iter = expression.iterate(context);
        SequenceExtent extent = new SequenceExtent(iter);
        List result = (List)extent.convertToJava(List.class, context);
        if (result == null) {
            result = Collections.EMPTY_LIST;
        }
        return result;
    }

    /**
     * Execute a prepared XPath expression, returning the first item in the result.
     * This is useful where it is known that the expression will only return
     * a singleton value (for example, a single node, or a boolean).
     * @param source the document or other node against which the XPath expression
     *                    will be evaluated. This may be a Saxon NodeInfo object, representing a node in an
     *                    existing tree, or it may be any kind of JAXP Source object such as a StreamSource
     *                    SAXSource or DOMSource.
     *
     * @return The first item in the sequence returned by the expression. If the expression
     *         returns an empty sequence, this method returns null. Otherwise, it returns the first
     *         item in the result sequence, represented as a Java object using the same mapping as for
     *         the evaluate() method
     */

    public Object evaluateSingle(Source source) throws XPathException {
        NodeInfo origin;
        if (source instanceof NodeInfo) {
            origin = (NodeInfo)source;
        } else {
            origin = evaluator.build(source);
        }
        XPathContextMajor context = new XPathContextMajor(origin, evaluator.getConfiguration());
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
     * @param source the document or other node against which the XPath expression
     *                    will be evaluated. This may be a Saxon NodeInfo object, representing a node in an
     *                    existing tree, or it may be any kind of JAXP Source object such as a StreamSource
     *                    SAXSource or DOMSource.
     */

    public SequenceIterator rawIterator(Source source) throws XPathException {
        NodeInfo origin;
        if (source instanceof NodeInfo) {
            origin = (NodeInfo)source;
        } else {
            origin = evaluator.build(source);
        }
        XPathContextMajor context = new XPathContextMajor(origin, evaluator.getConfiguration());
        context.openStackFrame(stackFrameMap);
        SequenceIterator iterator = expression.iterate(context);
        return iterator;
    }

    /**
     * Low-level method to get the internal Saxon expression object. This exposes a wide range of
     * internal methods that may be needed by specialized applications, and allows greater control
     * over the dynamic context for evaluating the expression.
     *
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
