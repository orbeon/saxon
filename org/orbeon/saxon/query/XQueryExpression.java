package net.sf.saxon.query;

import net.sf.saxon.Configuration;
import net.sf.saxon.Controller;
import net.sf.saxon.event.SaxonOutputKeys;
import net.sf.saxon.expr.*;
import net.sf.saxon.instruct.*;
import net.sf.saxon.om.*;
import net.sf.saxon.trace.TraceListener;
import net.sf.saxon.type.Type;
import net.sf.saxon.xpath.DynamicError;
import net.sf.saxon.xpath.XPathEvaluator;
import net.sf.saxon.xpath.XPathException;

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
 *
 * <p>Various methods are provided for evaluating the query, with different options for
 * delivery of the results.</p>
 */
public class XQueryExpression implements Container {

    private Expression expression;
    private SlotManager stackFrameMap;
    private Executable executable;

    // The documentInstruction is a document{...} wrapper around the expression as written by the user
    private DocumentInstr documentInstruction;

    /**
    * The constructor is protected, to ensure that instances can only be
    * created using the compileQuery() methods of QueryProcessor
    */

    protected XQueryExpression(Expression exp, Executable exec, StaticQueryContext staticEnv, Configuration config)
    throws XPathException {
        stackFrameMap = staticEnv.getStackFrameMap();
        executable = exec;
        if (exp instanceof ComputedExpression) {
            ((ComputedExpression)exp).setParentExpression(this);
        }
        ExpressionTool.makeParentReferences(exp);
        try {
            exp = exp.simplify(staticEnv);
            exp = exp.analyze(staticEnv, Type.ITEM_TYPE);
        } catch (XPathException err) {
            try {
                config.getErrorListener().fatalError(err);
            } catch (TransformerException e2) {
                //
            }
            throw err;
        }
        ExpressionTool.allocateSlots(exp, 0, staticEnv.getStackFrameMap());

        expression = exp;
        executable.setConfiguration(config);
        executable.setDefaultCollationName(staticEnv.getDefaultCollationName());
        executable.setCollationTable(staticEnv.getAllCollations());

    }

    /**
     * Get the expression wrapped in this XQueryExpression object
     * @return the underlying expression
     */

    public Expression getExpression() {
        return expression;
    }

    protected void setDocumentInstruction(DocumentInstr doc) {
        documentInstruction = doc;
        doc.setParentExpression(this);
    }

    /**
     * Execute a the compiled Query, returning the results as a List.
     * @param env Provides the dynamic query evaluation context
     * @return The results of the expression, as a List. The List represents the sequence
     * of items returned by the expression. Each item in the list will either be an
     * object representing a node, or an object representing an atomic value.
     * For the types of Java object that may be returned, see the description of the
     * {@link net.sf.saxon.xpath.XPathEvaluator#evaluate evaluate} method
     * of class XPathProcessor
     */

    public List evaluate(DynamicQueryContext env) throws XPathException {
        SequenceIterator iterator = iterator(env);
        ArrayList list = new ArrayList(100);
        while (true) {
            Item item = iterator.next();
            if (item == null) {
                return list;
            }
            list.add(XPathEvaluator.convert(item));
        }
    }

    /**
     * Execute the compiled Query, returning the first item in the result.
     * This is useful where it is known that the expression will only return
     * a singleton value (for example, a single node, or a boolean).
     * @param env Provides the dynamic query evaluation context
     * @return The first item in the sequence returned by the expression. If the expression
     * returns an empty sequence, this method returns null. Otherwise, it returns the first
     * item in the result sequence, represented as a Java object using the same mapping as for
     * the {@link XQueryExpression#evaluate evaluate} method
     */

    public Object evaluateSingle(DynamicQueryContext env) throws XPathException {
        SequenceIterator iterator = iterator(env);
        Item item = iterator.next();
        if (item == null) {
            return null;
        }
        return XPathEvaluator.convert(item);
    }

    /**
     * Get an iterator over the results of the expression. This returns results without
     * any conversion of the returned items to "native" Java classes. The iterator will
     * deliver a sequence of Item objects, each item being either a NodeInfo (representing
     * a node) or an AtomicValue (representing an atomic value).
     *
     * <p>To get the results of the query in the form of an XML document in which each
     * item is wrapped by an element indicating its type, use:</p>
     *
     * <p><code>QueryResult.wrap(iterator(env))</code></p>
     *
     * <p>To serialize the results to a file, use the QueryResult.serialize() method.</p>
     *
     * @param env Provides the dynamic query evaluation context
     * @return an iterator over the results of the query. The class SequenceIterator
     * is modeled on the standard Java Iterator class, but has extra functionality
     * and can throw exceptions when errors occur.
     * @throws XPathException if a dynamic error occurs in evaluating the query. Some
     * dynamic errors will not be reported by this method, but will only be reported
     * when the individual items of the result are accessed using the returned iterator.
     */

    public SequenceIterator iterator(DynamicQueryContext env) throws XPathException {
        Controller controller = getController();
        initializeController(env, controller);

        try {
            NodeInfo node = env.getContextNode();
 
            Bindery bindery = controller.getBindery();
            //bindery.openStackFrame();
            controller.defineGlobalParameters(bindery);
            XPathContextMajor context = controller.newXPathContext();

            // In tracing/debugging mode, evaluate all the global variables first
            if (controller.getConfiguration().getTraceListener() != null) {
                controller.preEvaluateGlobals(context);
            }

            context.openStackFrame(stackFrameMap);
            if (node != null) {
                context.setCurrentIterator(SingletonIterator.makeIterator(node));
                controller.setPrincipalSourceDocument(node.getDocumentRoot());
            }            
            SequenceIterator iterator = expression.iterate(context);
            return new ErrorReportingIterator(iterator, controller.getErrorListener());
        } catch (XPathException err) {
            TransformerException terr = err;
            while (terr.getException() instanceof TransformerException) {
                terr = (TransformerException)terr.getException();
            }
            try {
                controller.getErrorListener().fatalError(terr);
            } catch (TransformerException e) {
                //
            }
            throw DynamicError.makeDynamicError(terr);
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
    }

    /**
     * Run the query, sending the results directly to a JAXP Result object. This way of executing
     * the query is most efficient in the case of queries that produce a single document (or parentless
     * element) as their output, because it avoids constructing the result tree in memory: instead,
     * it is piped straight to the serializer.
     * @param env the dynamic query context
     * @param result the destination for the results of the query. The query is effectively wrapped
     * in a document{} constructor, so that the items in the result are concatenated to form a single
     * document; this is then written to the requested Result destination, which may be (for example)
     * a DOMResult, a SAXResult, or a StreamResult
     * @param outputProperties Supplies serialization properties, in JAXP format, if the result is to
     * be serialized. This parameter can be defaulted to null.
     * @throws XPathException if the query fails.
     */

    public void run(DynamicQueryContext env, Result result, Properties outputProperties) throws XPathException {

        Controller controller = getController();
        initializeController(env, controller);

        // Validate the serialization properties requested

        if (outputProperties == null) {
            outputProperties = new Properties();
        } else {
            Enumeration iter = outputProperties.propertyNames();
            while (iter.hasMoreElements()) {
                String key = (String)iter.nextElement();
                String value = outputProperties.getProperty(key);
                try {
                    SaxonOutputKeys.checkOutputProperty(key, value);
                } catch (DynamicError dynamicError) {
                    try {
                        controller.getErrorListener().fatalError(dynamicError);
                        throw dynamicError;
                        // TODO: could be a warning, but currently all warnings are fatal.
                        //outputProperties.remove(key);
                    } catch (TransformerException err2) {
                        throw DynamicError.makeDynamicError(err2);
                    }
                }
            }
        }

        NodeInfo node = env.getContextNode();

        Bindery bindery = controller.getBindery();
        controller.defineGlobalParameters(bindery);
        
        XPathContextMajor context = controller.newXPathContext();

        // In tracing/debugging mode, evaluate all the global variables first
        TraceListener tracer = controller.getConfiguration().getTraceListener();
        if (tracer != null) {
            controller.preEvaluateGlobals(context);
            tracer.open();
        }

        context.openStackFrame(stackFrameMap);
        if (node != null) {
            context.setCurrentIterator(SingletonIterator.makeIterator(node));
            controller.setPrincipalSourceDocument(node.getDocumentRoot());
        }        
        
        boolean mustClose = (result instanceof StreamResult &&
            ((StreamResult)result).getOutputStream() == null);
        context.changeOutputDestination(outputProperties, result, true, Validation.PRESERVE, null);

        // Run the query
        try {
            // TODO: this saves building a temporary tree for queries that construct a single element,
            // it also makes line number information available to a query debugger. But it's rather ad-hoc.
            if (expression instanceof ElementCreator) {
                expression.process(context);
            } else {
                documentInstruction.process(context);
            }
        } catch (XPathException err) {
            try {
                controller.getErrorListener().fatalError(err);
            } catch (TransformerException e) {
                //
            }
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
     * Get a controller that can be used to execute functions in this compiled query.
     * Functions in the query module can be found using {@link StaticQueryContext#getUserDefinedFunction}.
     * They can then be called directly from the Java application using {@link net.sf.saxon.instruct.UserFunction#call}
     * The same Controller can be used for a series of function calls.
     */

    public Controller getController() {
        Controller controller = new Controller(executable.getConfiguration());
        controller.setExecutable(executable);
        executable.initialiseBindery(controller.getBindery());
        return controller;
    }

    /**
     * Diagnostic method: display a representation of the compiled query on the
     * System.err output stream.
     */

    public void explain(NamePool pool) {
        System.err.println("============ Compiled Expression ============");
        expression.display(10, pool, System.err);
        System.err.println("=============================================");
    }

    /**
     * Get the Executable (representing a complete stylesheet or query) of which this Container forms part
     */

    public Executable getExecutable() {
        return executable;
    }

    /**
     * Return the public identifier for the current document event.
     *
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
     *
     * <p>The return value is the system identifier of the document
     * entity or of the external parsed entity in which the markup that
     * triggered the event appears.</p>
     *
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
     *
     * <p><strong>Warning:</strong> The return value from the method
     * is intended only as an approximation for the sake of error
     * reporting; it is not intended to provide sufficient information
     * to edit the character content of the original XML document.</p>
     *
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
     *
     * <p><strong>Warning:</strong> The return value from the method
     * is intended only as an approximation for the sake of error
     * reporting; it is not intended to provide sufficient information
     * to edit the character content of the original XML document.</p>
     *
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
     * ErrorReportingIterator is an iterator that wraps a base iterator and reports
     * any exceptions that are raised to the ErrorListener
     */

    private static class ErrorReportingIterator implements SequenceIterator {
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
                try {
                    listener.fatalError(e1);
                } catch (TransformerException e2) {}
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

