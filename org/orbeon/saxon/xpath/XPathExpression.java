package net.sf.saxon.xpath;
import net.sf.saxon.Configuration;
import net.sf.saxon.instruct.SlotManager;
import net.sf.saxon.expr.Atomizer;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.XPathContextMajor;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.pattern.NodeTest;
import net.sf.saxon.sort.FixedSortKeyDefinition;
import net.sf.saxon.sort.SortedIterator;

import java.util.ArrayList;
import java.util.List;

/**
  * <p>XPathExpression represents a compiled XPath expression that can be repeatedly
  * evaluated.</p>
  *
  * <p>Note that the object contains the context node, so it is not thread-safe.
  *
  * @author Michael H. Kay
  */


public final class XPathExpression {

    private Configuration configuration;
    private Expression expression;
    private NodeInfo contextNode;
    private SlotManager stackFrameMap;
    private XPathExpression sortKey = null;

    /**
    * The constructor is protected, to ensure that instances can only be
    * created using the createExpression() method of XPathEvaluator
    */

    protected XPathExpression(Expression exp, Configuration config) {
        expression = exp;
        this.configuration = config;
    }

    /**
     * Define the number of slots needed for local variables within the expression
     */

    protected void setStackFrameMap(SlotManager map) {
        stackFrameMap = map;
    }

    /**
    * Define the sort order for the results of the expression. If this method is called, then
    * the list returned by a subsequent call on the evaluate() method will first be sorted.
    * @param sortKey an XPathExpression, which will be applied to each item in the sequence;
    * the result of this expression determines the ordering of the list returned by the evaluate()
    * method. The sortKey can be null, to clear a previous sort key.
    */

    public void setSortKey(XPathExpression sortKey) {
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
        if (node.getNamePool() != configuration.getNamePool()) {
            throw new IllegalArgumentException("Supplied node uses the wrong NamePool");
        }
        contextNode = node;
    }


    /**
    * Execute a prepared XPath expression, returning the results as a List.
    * @return The results of the expression, as a List. The List represents the sequence
    * of items returned by the expression. Each item in the list will either be an instance
    * of net.sf.saxon.om.NodeInfo, representing a node, or a Java object representing an atomic value.
    * For the types of Java object that may be returned, see the description of the evaluate method
    * of class XPathProcessor
    */

    public List evaluate() throws XPathException {
        SequenceIterator iterator = rawIterator();
        ArrayList list = new ArrayList();
        while (true) {
            Item item = iterator.next();
            if (item == null) {
                return list;
            }
            list.add(XPathEvaluator.convert(item));
        }
    }

    /**
    * Execute a prepared XPath expression, returning the first item in the result.
    * This is useful where it is known that the expression will only return
    * a singleton value (for example, a single node, or a boolean).
    * @return The first item in the sequence returned by the expression. If the expression
    * returns an empty sequence, this method returns null. Otherwise, it returns the first
    * item in the result sequence, represented as a Java object using the same mapping as for
    * the evaluate() method
    */

    public Object evaluateSingle() throws XPathException {
        XPathContextMajor context = new XPathContextMajor(contextNode, configuration);
        context.openStackFrame(stackFrameMap);
        SequenceIterator iterator = expression.iterate(context);
        Item item = iterator.next();
        if (item == null) {
            return null;
        } else {
            return XPathEvaluator.convert(item);
        }
    }

    /**
    * Get a raw iterator over the results of the expression. This returns results without
    * any conversion of the returned items to "native" Java classes. This method is intended
    * for use by applications that need to process the results of the expression using
    * internal Saxon interfaces.
    */

    public SequenceIterator rawIterator() throws XPathException {
        XPathContextMajor context = new XPathContextMajor(contextNode, configuration);
        context.openStackFrame(stackFrameMap);
        SequenceIterator iterator = expression.iterate(context);
        if (sortKey != null) {
            Expression key = sortKey.expression;
            if (key.getItemType() instanceof NodeTest) {
                key = new Atomizer(key);
            }

            FixedSortKeyDefinition[] sk = new FixedSortKeyDefinition[1];

            sk[0] = new FixedSortKeyDefinition();
            sk[0].setSortKey(key);
            sk[0].bindComparer();

            iterator = new SortedIterator(context, iterator, sk);
        }
        return iterator;
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
// Contributor(s): Michael Kay, Peter Bryant, David Megginson
//
