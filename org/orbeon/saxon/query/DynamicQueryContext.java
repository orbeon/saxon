package net.sf.saxon.query;

import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.Item;
import net.sf.saxon.StandardURIResolver;
import net.sf.saxon.StandardErrorListener;
import net.sf.saxon.Configuration;

import javax.xml.transform.URIResolver;
import javax.xml.transform.ErrorListener;
import java.util.HashMap;

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

    public DynamicQueryContext(Configuration config) {
        this.config = config;
        uriResolver = config.getURIResolver();
        errorListener = config.getErrorListener();
        if (errorListener instanceof StandardErrorListener) {
            ((StandardErrorListener)errorListener).setRecoveryPolicy(Configuration.DO_NOT_RECOVER);
        }
    }

    /**
     * Set the context item for evaluating the expression to be a node. If this method is not called,
     * the context node will be undefined. The context node is available as the value of
     * the expression ".".
     * To obtain a NodeInfo by parsing a source document, see the method
     * {@link net.sf.saxon.query.QueryProcessor#buildDocument buildDocument}
     * in class QueryProcessor.
     * @see
     * @param node      The node that is to be the context node for the query
     */

    public void setContextNode(NodeInfo node) {
        if (node==null) {
            throw new NullPointerException("Context node cannot be null");
        }
        contextItem = node;
    }

    /**
     * Set the context item for evaluating the expression. If this method is not called,
     * the context node will be undefined. The context item is available as the value of
     * the expression ".",.
     * To obtain a node by parsing a source document, see the method
     * {@link net.sf.saxon.query.QueryProcessor#buildDocument buildDocument}
     * in class QueryProcessor.
     * @param item The item that is to be the context item for the query
     */

    public void setContextItem(Item item) {
        if (item==null) {
            throw new NullPointerException("Context item cannot be null");
        }
        contextItem = item;
    }

    /**
     * Get the context node for the query, as set using setContextNode()
     * or getContextItem() (provided the item is a node).
     * @return the context node if set, or null otherwise.
     */

    public NodeInfo getContextNode() {
        return (contextItem instanceof NodeInfo ?
                (NodeInfo)contextItem :
                null);
    }

     /**
     * Get the context item for the query, as set using setContextItem() or setContextNode().
     * @return the context item if set, or null otherwise.
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
     *     value, instantiate one of the classes in the net.sf.saxon.value package,
     *     for example net.sf.saxon.value.DayTimeDuration.
     */

    public void setParameter(String expandedName, Object value) {
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
     * Get the supplied parameters as a HashMap
     */

    protected HashMap getParameters() {
        return parameters;
    }

    /**
     * Set an object that will be used to resolve URIs used in
     * fn:document() and related functions.
     *
     * @param resolver An object that implements the URIResolver interface, or
     *      null.
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
     */

    public URIResolver getURIResolver() {
        return uriResolver;
    }

	/**
	 * Set the error listener. The error listener receives reports of all run-time
     * errors and can decide how to report them.
	 *
	 * @param listener the ErrorListener to be used
	 */

	public void setErrorListener(ErrorListener listener) {
		errorListener = listener;
	}

	/**
	 * Get the error listener.
	 *
	 * @return the ErrorListener in use
	 */

	public ErrorListener getErrorListener() {
		return errorListener;
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