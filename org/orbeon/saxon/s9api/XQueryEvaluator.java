package org.orbeon.saxon.s9api;

import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.om.ValueRepresentation;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.query.DynamicQueryContext;
import org.orbeon.saxon.query.XQueryExpression;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.SequenceExtent;
import org.orbeon.saxon.value.AtomicValue;
import org.orbeon.saxon.value.EmptySequence;
import org.orbeon.saxon.trace.TraceListener;
import org.orbeon.saxon.event.Receiver;

import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.Source;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import java.io.PrintStream;

/**
 * An <code>XQueryEvaluator</code> represents a compiled and loaded stylesheet ready for execution.
 * The <code>XQueryEvaluator</code> holds details of the dynamic evaluation context for the stylesheet.
 * <p/>
 * <p>An <code>XQueryEvaluator</code> must not be used concurrently in multiple threads.
 * It is safe, however, to reuse the object within a single thread to run the same
 * stylesheet several times. Running the stylesheet does not change the context
 * that has been established.</p>
 * <p/>
 * <p>An <code>XQueryEvaluator</code> is always constructed by running the <code>Load</code>
 * method of an {@link org.orbeon.saxon.s9api.XQueryExecutable}.</p>
 * <p/>
 * <p>An <code>XQueryEvaluator</code> is itself a <code>Iterable</code>. This makes it possible to
 * evaluate the results in a for-each expression.</p>
 */
public class XQueryEvaluator implements Iterable<XdmItem> {

    private Processor processor;
    private XQueryExpression expression;
    private DynamicQueryContext context;
    private Destination destination;
    private Set<XdmNode> updatedDocuments;

    /**
     * Protected constructor
     * @param processor  the Saxon processor
     * @param expression the XQuery expression
     */

    protected XQueryEvaluator(Processor processor, XQueryExpression expression) {
        this.processor = processor;
        this.expression = expression;
        this.context = new DynamicQueryContext(expression.getExecutable().getConfiguration());
    }

    /**
     * Set the source document for the query. This method is equivalent to building
     * a document from the supplied source object, and then supplying the document node of
     * the resulting document as the initial context node.
     * @param source the principal source document for the transformation
     */

    public void setSource(Source source) throws SaxonApiException {
        setContextItem(processor.newDocumentBuilder().build(source));
    }

    /**
     * Set the initial context item for the query
     * @param item the initial context item, or null if there is to be no initial context item
     */

    public void setContextItem(XdmItem item) {
        context.setContextItem(item == null ? null : (Item)item.getUnderlyingValue());
    }

    /**
     * Get the initial context item for the query, if one has been set
     * @return the initial context item, or null if none has been set. This will not necessarily
     *         be the same object as was supplied, but it will be an XdmItem object that represents
     *         the same underlying node or atomic value.
     */

    public XdmItem getContextItem() {
        return (XdmItem)XdmValue.wrap(context.getContextItem());
    }

    /**
     * Set the value of external variable defined in the query
     * @param name  the name of the external variable, as a QName
     * @param value the value of the external variable, or null to clear a previously set value
     */

    public void setExternalVariable(QName name, XdmValue value) {
        context.setParameter(name.getClarkName(),
                (value == null ? null : value.getUnderlyingValue()));
    }

    /**
     * Get the value that has been set for an external variable
     * @param name the name of the external variable whose value is required
     * @return the value that has been set for the external variable, or null if no value has been set
     */

    public XdmValue getExternalVariable(QName name) {
        Object oval = context.getParameter(name.getClarkName());
        if (oval == null) {
            return null;
        }
        if (oval instanceof ValueRepresentation) {
            return XdmValue.wrap((ValueRepresentation)oval);
        }
        throw new IllegalStateException(oval.getClass().getName());
    }

    /**
     * Set an object that will be used to resolve URIs used in
     * fn:doc() and related functions.
     * @param resolver An object that implements the URIResolver interface, or
     *                 null.
     */

    public void setURIResolver(URIResolver resolver) {
        context.setURIResolver(resolver);
    }

    /**
     * Get the URI resolver.
     * @return the user-supplied URI resolver if there is one, or the
     *         system-defined one otherwise
     */

    public URIResolver getURIResolver() {
        return context.getURIResolver();
    }

    /**
     * Set the error listener. The error listener receives reports of all run-time
     * errors and can decide how to report them.
     * @param listener the ErrorListener to be used
     */

    public void setErrorListener(ErrorListener listener) {
        context.setErrorListener(listener);
    }

    /**
     * Get the error listener.
     * @return the ErrorListener in use
     */

    public ErrorListener getErrorListener() {
        return context.getErrorListener();
    }

    /**
     * Set a TraceListener which will receive messages relating to the evaluation of all expressions.
     * This option has no effect unless the query was compiled to enable tracing.
     * @param listener the TraceListener to use
     */

    public void setTraceListener(TraceListener listener) {
        context.setTraceListener(listener);
    }

    /**
     * Get the registered TraceListener, if any
     * @return listener the TraceListener in use, or null if none has been set
     */

    public TraceListener getTraceListener() {
        return context.getTraceListener();
    }

    /**
     * Set the destination for output from the fn:trace() function.
     * By default, the destination is System.err. If a TraceListener is in use,
     * this is ignored, and the trace() output is sent to the TraceListener.
     * @param stream the PrintStream to which trace output will be sent. If set to
     *               null, trace output is suppressed entirely. It is the caller's responsibility
     *               to close the stream after use.
     * @since 9.1
     */

    public void setTraceFunctionDestination(PrintStream stream) {
        context.setTraceFunctionDestination(stream);
    }

    /**
     * Get the destination for output from the fn:trace() function.
     * @return the PrintStream to which trace output will be sent. If no explicitly
     *         destination has been set, returns System.err. If the destination has been set
     *         to null to suppress trace output, returns null.
     * @since 9.1
     */

    public PrintStream getTraceFunctionDestination() {
        return context.getTraceFunctionDestination();
    }

    /**
     * Set the destination to be used for the query results
     * @param destination the destination to which the results of the query will be sent
     */

    public void setDestination(Destination destination) {
        this.destination = destination;
    }

    /**
     * Perform the query.
     * <p/>
     * <ul><li>In the case of a non-updating query, the results are sent to the
     * registered Destination.</li>
     * <li>In the case of an updating query, all updated documents will be available after query
     * execution as the result of the {@link #getUpdatedDocuments} method.</li>
     * </ul>
     * @throws org.orbeon.saxon.s9api.SaxonApiException
     *                               if any dynamic error occurs during the query
     * @throws IllegalStateException if this is a non-updating query and no Destination has been
     *                               supplied for the query results
     */

    public void run() throws SaxonApiException {
        try {
            if (expression.isUpdateQuery()) {
                Set docs = expression.runUpdate(context);
                updatedDocuments = new HashSet();
                for (Iterator iter = docs.iterator(); iter.hasNext();) {
                    NodeInfo root = (NodeInfo)iter.next();
                    updatedDocuments.add((XdmNode)XdmNode.wrapItem(root));
                }
            } else {
                if (destination == null) {
                    throw new IllegalStateException("No destination supplied");
                }
                Receiver receiver;
                if (destination instanceof Serializer) {
                    receiver = ((Serializer)destination).getReceiver(
                            expression.getExecutable().getConfiguration(),
                            null,
                            expression.getExecutable().getDefaultOutputProperties());
                } else {
                    receiver = destination.getReceiver(expression.getExecutable().getConfiguration());
                }
                expression.run(context, receiver, null);
            }
        } catch (TransformerException e) {
            throw new SaxonApiException(e);
        }
    }

    /**
     * Perform the query, sending the results to a specified destination. This method
     * must not be used with an updating query
     * @param destination The destination where the result document will be sent
     * @throws org.orbeon.saxon.s9api.SaxonApiException
     *                               if any dynamic error occurs during the query
     * @throws IllegalStateException if this is an updating query
     */

    public void run(Destination destination) throws SaxonApiException {
        if (expression.isUpdateQuery()) {
            throw new IllegalStateException("Query is updating");
        }
        try {
            Receiver receiver;
            if (destination instanceof Serializer) {
                receiver = ((Serializer)destination).getReceiver(
                        expression.getExecutable().getConfiguration(),
                        null,
                        expression.getExecutable().getDefaultOutputProperties());
            } else {
                receiver = destination.getReceiver(expression.getExecutable().getConfiguration());
            }
            expression.run(context, receiver, null);
        } catch (TransformerException e) {
            throw new SaxonApiException(e);
        }
    }

    /**
     * Perform the query, returning the results as an XdmValue. This method
     * must not be used with an updating query
     * @return an XdmValue representing the results of the query
     * @throws SaxonApiException     if the query fails with a dynamic error
     * @throws IllegalStateException if this is an updating query
     */

    public XdmValue evaluate() throws SaxonApiException {
        if (expression.isUpdateQuery()) {
            throw new IllegalStateException("Query is updating");
        }
        try {
            SequenceIterator iter = expression.iterator(context);
            ValueRepresentation result = SequenceExtent.makeSequenceExtent(iter);
            if (result instanceof NodeInfo) {
                return new XdmNode((NodeInfo)result);
            } else if (result instanceof AtomicValue) {
                return new XdmAtomicValue((AtomicValue)result);
            } else if (result instanceof EmptySequence) {
                return XdmEmptySequence.getInstance();
            } else {
                return new XdmValue(result);
            }
        } catch (XPathException e) {
            throw new SaxonApiException(e);
        }
    }

    /**
     * Get an iterator over the results of the query. This method
     * must not be used with an updating query
     * @throws SaxonApiUncheckedException if a dynamic error is detected while constructing the iterator.
     *                                    It is also possible for an SaxonApiUncheckedException to be thrown by the hasNext() method of the
     *                                    returned iterator if a dynamic error occurs while evaluating the result sequence.
     * @throws IllegalStateException      if this is an updating query
     */

    public Iterator<XdmItem> iterator() throws SaxonApiUncheckedException {
        if (expression.isUpdateQuery()) {
            throw new IllegalStateException("Query is updating");
        }
        try {
            return new XdmSequenceIterator(expression.iterator(context));
        } catch (XPathException e) {
            throw new SaxonApiUncheckedException(e);
        }
    }

    /**
     * After executing an updating query using the {@link #run()} method, iterate over the root
     * nodes of the documents updated by the query.
     * <p/>
     * <p>The results remain available until a new query is executed. This method returns the results
     * of the most recently executed query. It does not consume the results.</p>
     * @return an iterator over the root nodes of documents (or other trees) that were updated by the query
     * @since 9.1
     */

    public Iterator<XdmNode> getUpdatedDocuments() {
        return updatedDocuments.iterator();
    }

    /**
     * Get the underlying dynamic context object. This provides an escape hatch to the underlying
     * implementation classes, which contain methods that may change from one release to another.
     * @return the underlying object representing the dynamic context for query execution
     */

    public DynamicQueryContext getUnderlyingQueryContext() {
        return context;
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
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//

