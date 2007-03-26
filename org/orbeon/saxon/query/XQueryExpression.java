package org.orbeon.saxon.query;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.Controller;
import org.orbeon.saxon.event.*;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.instruct.Bindery;
import org.orbeon.saxon.instruct.Executable;
import org.orbeon.saxon.instruct.SlotManager;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.pull.PullFromIterator;
import org.orbeon.saxon.pull.PullNamespaceReducer;
import org.orbeon.saxon.pull.PullProvider;
import org.orbeon.saxon.pull.PullPushCopier;
import org.orbeon.saxon.trace.TraceListener;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.UncheckedXPathException;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.DateTimeValue;
import org.orbeon.saxon.value.Value;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.Result;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import java.io.OutputStream;
import java.util.*;

/**
 * XQueryExpression represents a compiled query. This object is immutable and thread-safe,
 * the same compiled query may be executed many times in series or in parallel. The object
 * can be created only by using the compileQuery method of the QueryProcessor class.
 * <p/>
 * <p>Various methods are provided for evaluating the query, with different options for
 * delivery of the results.</p>
 */
public class XQueryExpression implements Container {

    private Expression expression;
    private SlotManager stackFrameMap;
    private Executable executable;
    private StaticQueryContext staticContext;

    /**
     * The constructor is protected, to ensure that instances can only be
     * created using the compileQuery() methods of StaticQueryContext
     */

    protected XQueryExpression(Expression exp, Executable exec, StaticQueryContext staticEnv, Configuration config)
            throws XPathException {
        stackFrameMap = staticEnv.getStackFrameMap();
        executable = exec;
        ComputedExpression.setParentExpression(exp, this);
        ExpressionTool.makeParentReferences(exp);
        try {
            exp = exp.simplify(staticEnv);
            exp = exp.typeCheck(staticEnv, staticEnv.getRequiredContextItemType());
            exp = exp.optimize(exec.getConfiguration().getOptimizer(), staticEnv, Type.ITEM_TYPE);
        } catch (XPathException err) {
            staticEnv.reportFatalError(err);
            throw err;
        }
        ExpressionTool.allocateSlots(exp, 0, staticEnv.getStackFrameMap());

        expression = exp;
        executable.setConfiguration(config);
        executable.setDefaultCollationName(staticEnv.getDefaultCollationName());
        executable.setCollationTable(staticEnv.getAllCollations());
        staticContext = staticEnv;

    }

    /**
     * Get the expression wrapped in this XQueryExpression object
     *
     * @return the underlying expression
     */

    public Expression getExpression() {
        return expression;
    }

    /**
      * Replace one subexpression by a replacement subexpression. For internal use only
      * @param original the original subexpression
      * @param replacement the replacement subexpression
      * @return true if the original subexpression is found
      */

    public boolean replaceSubExpression(Expression original, Expression replacement) {
         boolean found = false;
         if (expression == original) {
             expression = replacement;
             found = true;
         }
                 return found;
    }


    /**
     * Get the static context in which this expression was compiled. Note, this will be an internal
     * copy of the original user-created StaticQueryContext object. The user-created object is not modified
     * by Saxon, whereas the copy includes additional information found in the query prolog.
     *
     * @return the internal copy of the StaticQueryContext. This is available for inspection, but
     * should not be modified or reused by the application.
     */
    public StaticQueryContext getStaticContext() {
        return staticContext;
    }

    /**
     * Get a list containing the fingerprints of the names of the external variables in the query
     * @return an array of integers, being the integer fingerprints of external variables
     */

    public int[] getExternalVariableNames() {
        List list = stackFrameMap.getVariableMap();
        int[] names = new int[stackFrameMap.getNumberOfVariables()];
        for (int i=0; i<names.length; i++) {
            names[i] = ((Integer)list.get(i)).intValue() & NamePool.FP_MASK;
        }
        return names;
    }

    /**
     * Execute a the compiled Query, returning the results as a List.
     *
     * @param env Provides the dynamic query evaluation context
     * @return The results of the expression, as a List. The List represents the sequence
     *         of items returned by the expression. Each item in the list will either be an
     *         object representing a node, or an object representing an atomic value.
     *         For the types of Java object that may be returned, see the description of the
     *         {@link org.orbeon.saxon.xpath.XPathEvaluator#evaluate evaluate} method
     *         of class XPathProcessor
     */

    public List evaluate(DynamicQueryContext env) throws XPathException {
        SequenceIterator iterator = iterator(env);
        ArrayList list = new ArrayList(100);
        while (true) {
            Item item = iterator.next();
            if (item == null) {
                return list;
            }
            list.add(Value.convert(item));
        }
    }

    /**
     * Execute the compiled Query, returning the first item in the result.
     * This is useful where it is known that the expression will only return
     * a singleton value (for example, a single node, or a boolean).
     *
     * @param env Provides the dynamic query evaluation context
     * @return The first item in the sequence returned by the expression. If the expression
     *         returns an empty sequence, this method returns null. Otherwise, it returns the first
     *         item in the result sequence, represented as a Java object using the same mapping as for
     *         the {@link XQueryExpression#evaluate evaluate} method
     */

    public Object evaluateSingle(DynamicQueryContext env) throws XPathException {
        SequenceIterator iterator = iterator(env);
        Item item = iterator.next();
        if (item == null) {
            return null;
        }
        return Value.convert(item);
    }

    /**
     * Get an iterator over the results of the expression. This returns results without
     * any conversion of the returned items to "native" Java classes. The iterator will
     * deliver a sequence of Item objects, each item being either a NodeInfo (representing
     * a node) or an AtomicValue (representing an atomic value).
     * <p/>
     * <p>To get the results of the query in the form of an XML document in which each
     * item is wrapped by an element indicating its type, use:</p>
     * <p/>
     * <p><code>QueryResult.wrap(iterator(env))</code></p>
     * <p/>
     * <p>To serialize the results to a file, use the QueryResult.serialize() method.</p>
     *
     * @param env Provides the dynamic query evaluation context
     * @return an iterator over the results of the query. The class SequenceIterator
     *         is modeled on the standard Java Iterator class, but has extra functionality
     *         and can throw exceptions when errors occur.
     * @throws XPathException if a dynamic error occurs in evaluating the query. Some
     *                        dynamic errors will not be reported by this method, but will only be reported
     *                        when the individual items of the result are accessed using the returned iterator.
     */

    public SequenceIterator iterator(DynamicQueryContext env) throws XPathException {
        Controller controller = newController();
        initializeController(env, controller);

        try {
            Item contextItem = env.getContextItem();

            Bindery bindery = controller.getBindery();
            //bindery.openStackFrame();
            controller.defineGlobalParameters(bindery);
            XPathContextMajor context = controller.newXPathContext();

            if (contextItem != null) {
                if (!staticContext.getRequiredContextItemType().matchesItem(contextItem, false, env.getConfiguration())) {
                    throw new DynamicError("The supplied context item does not match the required context item type");
                }
                AxisIterator single = SingletonIterator.makeIterator(contextItem);
                single.next();
                context.setCurrentIterator(single);
                controller.setInitialContextItem(contextItem);
            }

            // In tracing/debugging mode, evaluate all the global variables first
            if (controller.getConfiguration().getTraceListener() != null) {
                controller.preEvaluateGlobals(context);
            }

            context.openStackFrame(stackFrameMap);

            SequenceIterator iterator = expression.iterate(context);
            return new ErrorReportingIterator(iterator, controller.getErrorListener());
        } catch (XPathException err) {
            TransformerException terr = err;
            while (terr.getException() instanceof TransformerException) {
                terr = (TransformerException)terr.getException();
            }
            DynamicError de = DynamicError.makeDynamicError(terr);
            controller.reportFatalError(de);
            throw de;
        }
    }

    private void initializeController(DynamicQueryContext env, Controller controller) {
        HashMap parameters = env.getParameters();
        if (parameters != null) {
            Iterator iter = parameters.keySet().iterator();
            while (iter.hasNext()) {
                String paramName = (String)iter.next();
                Object paramValue = parameters.get(paramName);
                controller.setParameter(paramName, paramValue);
            }
        }

        controller.setURIResolver(env.getURIResolver());
        controller.setErrorListener(env.getErrorListener());
        DateTimeValue currentDateTime = env.getCurrentDateTime();
        if (currentDateTime != null) {
            try {
                controller.setCurrentDateTime(currentDateTime);
            } catch (XPathException e) {
                throw new AssertionError(e);    // the value should already have been checked
            }
        }
    }

    /**
     * Run the query, sending the results directly to a JAXP Result object. This way of executing
     * the query is most efficient in the case of queries that produce a single document (or parentless
     * element) as their output, because it avoids constructing the result tree in memory: instead,
     * it is piped straight to the serializer.
     *
     * @param env              the dynamic query context
     * @param result           the destination for the results of the query. The query is effectively wrapped
     *                         in a document{} constructor, so that the items in the result are concatenated to form a single
     *                         document; this is then written to the requested Result destination, which may be (for example)
     *                         a DOMResult, a SAXResult, or a StreamResult
     * @param outputProperties Supplies serialization properties, in JAXP format, if the result is to
     *                         be serialized. This parameter can be defaulted to null.
     * @throws XPathException if the query fails.
     */

    public void run(DynamicQueryContext env, Result result, Properties outputProperties) throws XPathException {

        Controller controller = newController();
        initializeController(env, controller);

        // Validate the serialization properties requested

        Properties baseProperties = controller.getOutputProperties();
        if (outputProperties != null) {
            Enumeration iter = outputProperties.propertyNames();
            while (iter.hasMoreElements()) {
                String key = (String)iter.nextElement();
                String value = outputProperties.getProperty(key);
                try {
                    SaxonOutputKeys.checkOutputProperty(key, value, controller.getConfiguration().getNameChecker());
                    baseProperties.setProperty(key, value);
                } catch (DynamicError dynamicError) {
                    try {
                        outputProperties.remove(key);
                        controller.getErrorListener().warning(dynamicError);
                    } catch (TransformerException err2) {
                        throw DynamicError.makeDynamicError(err2);
                    }
                }
            }
        }
        if (baseProperties.getProperty("method") == null) {
            // XQuery forces the default method to XML, unlike XSLT where it depends on the contents of the result tree
            baseProperties.setProperty("method", "xml");
        }

        Item contextItem = env.getContextItem();

        Bindery bindery = controller.getBindery();
        controller.defineGlobalParameters(bindery);

        XPathContextMajor context = controller.newXPathContext();

        if (contextItem != null) {
            AxisIterator single = SingletonIterator.makeIterator(contextItem);
            context.setCurrentIterator(single);
            single.next();
            controller.setInitialContextItem(contextItem);
        }

        // In tracing/debugging mode, evaluate all the global variables first
        TraceListener tracer = controller.getConfiguration().getTraceListener();
        if (tracer != null) {
            controller.preEvaluateGlobals(context);
            tracer.open();
        }

        context.openStackFrame(stackFrameMap);

        boolean mustClose = (result instanceof StreamResult &&
                ((StreamResult)result).getOutputStream() == null);
        context.changeOutputDestination(baseProperties, result, true,
                Configuration.XQUERY, Validation.PRESERVE, null);

        // Run the query
        try {
            expression.process(context);
        } catch (XPathException err) {
            controller.reportFatalError(err);
            throw err;
        }

        if (tracer != null) {
            tracer.close();
        }

        context.getReceiver().close();
        if (mustClose) {
            OutputStream os = ((StreamResult)result).getOutputStream();
            if (os != null) {
                try {
                    os.close();
                } catch (java.io.IOException err) {
                    throw new DynamicError(err);
                }
            }
        }
    }

    /**
     * Run the query in pull mode.
     * <p/>
     * For maximum effect this method should be used when lazyConstructionMode has been set in the Configuration.
     *
     * @see Configuration#setLazyConstructionMode(boolean)
     */

    public void pull(DynamicQueryContext dynamicEnv, Result destination, Properties outputProperties) throws XPathException {
        try {
            SequenceIterator iter = iterator(dynamicEnv);
            PullProvider pull = new PullFromIterator(iter);
            pull = new PullNamespaceReducer(pull);
            final Configuration config = executable.getConfiguration();
            pull.setPipelineConfiguration(config.makePipelineConfiguration());

            SerializerFactory sf = config.getSerializerFactory();
            Receiver receiver = sf.getReceiver(destination,
                            pull.getPipelineConfiguration(),
                            outputProperties);

            NamespaceReducer reducer = new NamespaceReducer();
            PipelineConfiguration pipe = pull.getPipelineConfiguration();
            reducer.setPipelineConfiguration(pipe);
            reducer.setUnderlyingReceiver(receiver);
            ComplexContentOutputter outputter = new ComplexContentOutputter();
            outputter.setReceiver(reducer);
            outputter.setPipelineConfiguration(pipe);
            if ("yes".equals(outputProperties.getProperty(SaxonOutputKeys.WRAP))) {
                receiver = new SequenceWrapper(outputter);
            } else {
                receiver = new TreeReceiver(outputter);
            }
            new PullPushCopier(pull, receiver).copy();
        } catch (UncheckedXPathException e) {
            throw e.getXPathException();
        }
    }

    /**
     * Get a controller that can be used to execute functions in this compiled query.
     * Functions in the query module can be found using {@link StaticQueryContext#getUserDefinedFunction}.
     * They can then be called directly from the Java application using {@link org.orbeon.saxon.instruct.UserFunction#call}
     * The same Controller can be used for a series of function calls.
     * <p/>
     * Note, this method is poorly named. It creates a new Controller each time it is called: which is useful
     * when a query is to be executed repeatedly.
     */

    public Controller newController() {
        Controller controller = new Controller(executable.getConfiguration(), executable);
        executable.initialiseBindery(controller.getBindery());
        return controller;
    }

    /**
     * Deprecated synonym for {@link #newController}
     * @deprecated since 8.5.1 - use newController()
     */

    public Controller getController() {
        return newController();
    }

    /**
     * Diagnostic method: display a representation of the compiled query on the
     * System.err output stream.
     */

    public void explain(Configuration config) {
        System.err.println("============ Compiled Expression ============");
        expression.display(10, System.err, config);
        System.err.println("=============================================");
    }

    /**
     * Get the Executable (representing a complete stylesheet or query) of which this Container forms part
     */

    public Executable getExecutable() {
        return executable;
    }

    /**
     * Get the LocationProvider allowing location identifiers to be resolved.
     */

    public LocationProvider getLocationProvider() {
        return executable.getLocationMap();
    }

    /**
     * Return the public identifier for the current document event.
     * <p/>
     * <p>The return value is the public identifier of the document
     * entity or of the external parsed entity in which the markup that
     * triggered the event appears.</p>
     *
     * @return A string containing the public identifier, or
     *         null if none is available.
     * @see #getSystemId
     */
    public String getPublicId() {
        return null;
    }

    /**
     * Return the system identifier for the current document event.
     * <p/>
     * <p>The return value is the system identifier of the document
     * entity or of the external parsed entity in which the markup that
     * triggered the event appears.</p>
     * <p/>
     * <p>If the system identifier is a URL, the parser must resolve it
     * fully before passing it to the application.</p>
     *
     * @return A string containing the system identifier, or null
     *         if none is available.
     * @see #getPublicId
     */
    public String getSystemId() {
        return null;
    }

    /**
     * Return the line number where the current document event ends.
     * <p/>
     * <p><strong>Warning:</strong> The return value from the method
     * is intended only as an approximation for the sake of error
     * reporting; it is not intended to provide sufficient information
     * to edit the character content of the original XML document.</p>
     * <p/>
     * <p>The return value is an approximation of the line number
     * in the document entity or external parsed entity where the
     * markup that triggered the event appears.</p>
     *
     * @return The line number, or -1 if none is available.
     * @see #getColumnNumber
     */
    public int getLineNumber() {
        return -1;
    }

    /**
     * Return the character position where the current document event ends.
     * <p/>
     * <p><strong>Warning:</strong> The return value from the method
     * is intended only as an approximation for the sake of error
     * reporting; it is not intended to provide sufficient information
     * to edit the character content of the original XML document.</p>
     * <p/>
     * <p>The return value is an approximation of the column number
     * in the document entity or external parsed entity where the
     * markup that triggered the event appears.</p>
     *
     * @return The column number, or -1 if none is available.
     * @see #getLineNumber
     */
    public int getColumnNumber() {
        return -1;
    }

    /**
     * Get the host language (XSLT, XQuery, XPath) used to implement the code in this container
     * @return typically {@link org.orbeon.saxon.Configuration#XSLT} or {@link org.orbeon.saxon.Configuration#XQUERY}
     */

    public int getHostLanguage() {
        return Configuration.XQUERY;
    }

    /**
     * ErrorReportingIterator is an iterator that wraps a base iterator and reports
     * any exceptions that are raised to the ErrorListener
     */

    private class ErrorReportingIterator implements SequenceIterator {
        private SequenceIterator base;
        private ErrorListener listener;

        public ErrorReportingIterator(SequenceIterator base, ErrorListener listener) {
            this.base = base;
            this.listener = listener;
        }

        public Item next() throws XPathException {
            try {
                return base.next();
            } catch (XPathException e1) {
                if (e1.getLocator() == null) {
                    e1.setLocator(ExpressionTool.getLocator(expression));
                }
                try {
                    listener.fatalError(e1);
                } catch (TransformerException e2) {
                    //
                }
                e1.setHasBeenReported();
                throw e1;
            }
        }

        public Item current() {
            return base.current();
        }

        public int position() {
            return base.position();
        }

        public SequenceIterator getAnother() throws XPathException {
            return new ErrorReportingIterator(base.getAnother(), listener);
        }

        /**
         * Get properties of this iterator, as a bit-significant integer.
         *
         * @return the properties of this iterator. This will be some combination of
         *         properties such as {@link GROUNDED}, {@link LAST_POSITION_FINDER},
         *         and {@link LOOKAHEAD}. It is always
         *         acceptable to return the value zero, indicating that there are no known special properties.
         *         It is acceptable for the properties of the iterator to change depending on its state.
         */

        public int getProperties() {
            return 0;
        }
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

