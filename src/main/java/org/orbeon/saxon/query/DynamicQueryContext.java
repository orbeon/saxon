package org.orbeon.saxon.query;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.StandardErrorListener;
import org.orbeon.saxon.trace.TraceListener;
import org.orbeon.saxon.functions.Component;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.om.ValueRepresentation;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.DateTimeValue;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.URIResolver;
import java.util.HashMap;
import java.io.PrintStream;

/**
 * This object represents a dynamic context for query execution. This class is used
 * by the application writer to set up aspects of the dynamic context; it is not used
 * operationally (or modified) by the XQuery processor itself, which copies all required
 * information into its own internal representation.
 */

public class DynamicQueryContext {

    private Item contextItem;
    private HashMap parameters;
    private Configuration config;
    private URIResolver uriResolver;
    private ErrorListener errorListener;
    private TraceListener traceListener;
    private DateTimeValue currentDateTime;
    private PrintStream traceFunctionDestination = System.err;

    /**
     * Create the dynamic context for a query
     * @param config the Saxon configuration

     * @since 8.4.
     */

    public DynamicQueryContext(Configuration config) {
        this.config = config;
        uriResolver = config.getURIResolver();
        errorListener = config.getErrorListener();
        try {
            traceListener = config.makeTraceListener();
        } catch (XPathException err) {
            throw new IllegalStateException(err.getMessage());
        }
        if (errorListener instanceof StandardErrorListener) {
            errorListener = ((StandardErrorListener)errorListener).makeAnother(Configuration.XQUERY);
            ((StandardErrorListener)errorListener).setRecoveryPolicy(Configuration.DO_NOT_RECOVER);
        }
    }

    /**
     * Set the context item for evaluating the expression to be a node. If this method is not called,
     * the context node will be undefined. The context node is available as the value of
     * the expression ".".
     * To obtain a NodeInfo by parsing a source document, see the method
     * {@link org.orbeon.saxon.Configuration#buildDocument buildDocument}
     * in class QueryProcessor.
     *
     * @param node      The node that is to be the context node for the query
     * @since 8.4
     * @deprecated From Saxon 8.7, the method {@link #setContextItem(Item)} is preferred
     */

    public void setContextNode(NodeInfo node) {
        if (node==null) {
            throw new NullPointerException("Context node cannot be null");
        }
        setContextItem(node);
    }

    /**
     * Set the context item for evaluating the expression. If this method is not called,
     * the context node will be undefined. The context item is available as the value of
     * the expression ".",.
     * To obtain a node by parsing a source document, see the method
     * {@link org.orbeon.saxon.Configuration#buildDocument buildDocument}
     * in class QueryProcessor.
     * @param item The item that is to be the context item for the query
     * @throws IllegalArgumentException if the supplied item is a node that was built under the wrong
     * Saxon Configuration
     * @since 8.4
     */

    public void setContextItem(Item item) {
        if (item==null) {
            throw new NullPointerException("Context item cannot be null");
        }
        if (item instanceof NodeInfo) {
            if (!((NodeInfo)item).getConfiguration().isCompatible(config)) {
                throw new IllegalArgumentException(
                            "Supplied node must be built using the same or a compatible Configuration");
            }
        }
        contextItem = item;
    }

     /**
     * Get the context item for the query, as set using setContextItem() or setContextNode().
     * @return the context item if set, or null otherwise.
     * @since 8.4
     */

    public Item getContextItem() {
        return contextItem;
    }

    /**
     * Set a parameter for the query.
     *
     * @param expandedName The name of the parameter in "{uri}local-name" format.
     *     It is not an error to supply a value for a parameter that has not been
     *     declared, the parameter will simply be ignored. If the parameter has
     *     been declared in the query (as an external global variable) then it
     *     will be initialized with the value supplied.
     * @param value The value of the parameter.  This can be any valid Java
     *     object.  It follows the same conversion rules as a value returned
     *     from a Saxon extension function. An error will occur at query
     *     execution time if the supplied value cannot be converted to the required
     *     type as declared in the query. For precise control of the type of the
     *     value, instantiate one of the classes in the org.orbeon.saxon.value package,
     *     for example org.orbeon.saxon.value.DayTimeDuration.
     * @since 8.4
     */

    public void setParameter(String expandedName, Object value) {
        if (parameters==null) {
            parameters = new HashMap(10);
        }
        parameters.put(expandedName, value);
    }

    /**
     * Set a parameter for the query.
     *
     * @param expandedName The name of the parameter in "{uri}local-name" format.
     *     It is not an error to supply a value for a parameter that has not been
     *     declared, the parameter will simply be ignored. If the parameter has
     *     been declared in the query (as an external global variable) then it
     *     will be initialized with the value supplied.
     * @param value The value of the parameter.  This must be an XPath value in its Saxon
     *     representation: no conversion occurs.
     * @since 8.8
     */

    public void setParameterValue(String expandedName, ValueRepresentation value) {
        if (parameters==null) {
            parameters = new HashMap(10);
        }
        parameters.put(expandedName, value);
    }

    /**
     * Reset the parameters to an empty list.
     */

    public void clearParameters() {
        parameters = null;
    }

    /**
     * Get the actual value of a parameter to the query.
     *
     * @param expandedName the name of the required parameter, in
     *     "{uri}local-name" format
     * @return the value of the parameter, if it exists, or null otherwise
     */

    public Object getParameter(String expandedName) {
        if (parameters==null) return null;
        return parameters.get(expandedName);
    }

    /**
     * Get all the supplied parameters as a HashMap. The key is the expanded QName in Clark notation,
     * the value is the value as supplied to setParameterValue
     * @return a HashMap containing all the parameters
     */

    public HashMap getParameters() {
        if (parameters == null) {
            return new HashMap(4);
        }
        return parameters;
    }

    /**
     * Set an object that will be used to resolve URIs used in
     * fn:document() and related functions.
     *
     * @param resolver An object that implements the URIResolver interface, or
     *      null.
     * @since 8.4
     */

    public void setURIResolver(URIResolver resolver) {
        // System.err.println("Setting uriresolver to " + resolver + " on " + this);
        uriResolver = resolver;
    }

    /**
     * Get the URI resolver.
     *
     * @return the user-supplied URI resolver if there is one, or the
     *     system-defined one otherwise
     * @since 8.4
     */

    public URIResolver getURIResolver() {
        return uriResolver;
    }

	/**
	 * Set the error listener. The error listener receives reports of all run-time
     * errors and can decide how to report them.
	 *
	 * @param listener the ErrorListener to be used
     * @since 8.4
	 */

	public void setErrorListener(ErrorListener listener) {
		errorListener = listener;
	}

	/**
	 * Get the error listener.
	 *
	 * @return the ErrorListener in use
     * @since 8.4
	 */

	public ErrorListener getErrorListener() {
		return errorListener;
	}

    /**
     * Set the trace listener. The trace listener receives reports of all run-time
     * expression evaluation.
     *
     * @param listener the TraceListener to be used
     * @since 9.0
     */

    public void setTraceListener(TraceListener listener) {
        traceListener = listener;
    }

    /**
     * Get the trace listener.
     *
     * @return the TraceListener in use, or null if none is in use
     * @since 9.0
     */

    public TraceListener getTraceListener() {
        return traceListener;
    }

    /**
     * Set the destination for output from the fn:trace() function.
     * By default, the destination is System.err. If a TraceListener is in use,
     * this is ignored, and the trace() output is sent to the TraceListener.
     * @param stream the PrintStream to which trace output will be sent. If set to
     * null, trace output is suppressed entirely. It is the caller's responsibility
     * to close the stream after use.
     * @since 9.1
     */

    public void setTraceFunctionDestination(PrintStream stream) {
        traceFunctionDestination = stream;
    }

    /**
     * Get the destination for output from the fn:trace() function.
     * @return the PrintStream to which trace output will be sent. If no explicitly
     * destination has been set, returns System.err. If the destination has been set
     * to null to suppress trace output, returns null.
     * @since 9.1
     */

    public PrintStream getTraceFunctionDestination() {
        return traceFunctionDestination;
    }    


    /**
     * Get the date and time set previously using {@link #setCurrentDateTime(org.orbeon.saxon.value.DateTimeValue)}
     * or null if none has been set.
     * @return the current date and time, if it has been set.
     * @since 8.5
     */

    public DateTimeValue getCurrentDateTime() {
        return currentDateTime;
    }

    /**
     * Set a value to be used as the current date and time for the query. By default, the "real" current date and
     * time are used. The main purpose of this method is that it allows repeatable results to be achieved when
     * testing queries.
     * <p>This method also has the effect of setting the implicit timezone.</p>
     * @param dateTime The value to be used as the current date and time. This must include a timezone. The timezone
     * from this value will also be used as the implicit timezone
     * @since 8.5
     */

    public void setCurrentDateTime(DateTimeValue dateTime) throws XPathException {
        currentDateTime = dateTime;
        if (dateTime.getComponent(Component.TIMEZONE) == null) {
            throw new XPathException("Supplied date/time must include a timezone");
        }
    }

    /**
     * Get the Configuration associated with this dynamic query context
     * @return the Configuration
     * @since 8.8
     */ 

    public Configuration getConfiguration() {
        return config;
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
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none
//