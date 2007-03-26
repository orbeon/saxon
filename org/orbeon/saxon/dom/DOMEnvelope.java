package org.orbeon.saxon.dom;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.event.PipelineConfiguration;
import org.orbeon.saxon.event.Receiver;
import org.orbeon.saxon.event.Sender;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.SequenceExtent;
import org.orbeon.saxon.value.Value;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * DOMEnvelope is an object model representation in which DOM interfaces are wrapped around
 * Saxon NodeInfo nodes: that is, it implements the DOM on top of a Saxon tree implementation
 * such as the tiny tree or linked tree.
 */

public class DOMEnvelope implements ExternalObjectModel, Serializable {

    public DOMEnvelope() {}

     /**
     * Test whether this object model recognizes a given node as one of its own
     */

    public boolean isRecognizedNode(Object object) {
         return object instanceof NodeOverNodeInfo;
    }

    /**
     * Test whether this object model recognizes a given class as representing a
     * node in that object model. This method will generally be called at compile time.
     *
     * @param nodeClass A class that possibly represents nodes
     * @return true if the class is used to represent nodes in this object model
     */

    public boolean isRecognizedNodeClass(Class nodeClass) {
        return NodeOverNodeInfo.class.isAssignableFrom(nodeClass);
    }

    /**
     * Test whether this object model recognizes a given class as representing a
     * list of nodes in that object model. This method will generally be called at compile time.
     *
     * @param nodeClass A class that possibly represents nodes
     * @return true if the class is used to represent nodes in this object model
     */

    public boolean isRecognizedNodeListClass(Class nodeClass) {
        return NodeList.class.isAssignableFrom(nodeClass);
    }

    /**
     * Test whether this object model recognizes a particular kind of JAXP Result object,
     * and if it does, return a Receiver that builds an instance of this data model from
     * a sequence of events. If the Result is not recognised, return null.
     * <p>
     * This implementation always returns null: it is not possible to construct an instance
     * of this object model implementation directly as the result of a JAXP transformation.
     */

    public Receiver getDocumentBuilder(Result result) throws XPathException {
        return null;
    }

    /**
     * Test whether this object model recognizes a particular kind of JAXP Source object,
     * and if it does, send the contents of the document to a supplied Receiver, and return true.
     * Otherwise, return false.
     * <p>
     * This implementation returns true only if the source is a DOMSource whose contained node is a
     * a "NodeOverNodeInfo".
     */

    public boolean sendSource(Source source, Receiver receiver, PipelineConfiguration pipe) throws XPathException {
        if (source instanceof DOMSource) {
            Node startNode = ((DOMSource)source).getNode();
            if (startNode instanceof NodeOverNodeInfo) {
                NodeInfo base = ((NodeOverNodeInfo)startNode).getUnderlyingNodeInfo();
                Sender driver = new Sender(pipe);
                driver.send(base, receiver);
                return true;
            }
        }
        return false;
    }

    /**
     * Wrap or unwrap a node using this object model to return the corresponding Saxon node. If the supplied
     * source does not belong to this object model, return null
     */

    public NodeInfo unravel(Source source, Configuration config) {

        if (source instanceof DOMSource) {
            Node dsnode = ((DOMSource)source).getNode();
            if (dsnode instanceof NodeOverNodeInfo) {
                // Supplied source is a DOM Node wrapping a Saxon node: unwrap it
                return ((NodeOverNodeInfo)dsnode).getUnderlyingNodeInfo();
            }
        }
        return null;
    }

    /**
     * Convert a Java object to an XPath value. If the supplied object is recognized as a representation
     * of a value using this object model, the object model should convert the value to an XPath value
     * and return this as the result. If not, it should return null. If the object is recognized but cannot
     * be converted, an exception should be thrown
     */

    public Value convertObjectToXPathValue(Object object, Configuration config) throws XPathException {
        if (object instanceof NodeList) {
            NodeList list = ((NodeList)object);
            NodeInfo[] nodes = new NodeInfo[list.getLength()];
            for (int i=0; i<list.getLength(); i++) {
                if (list.item(i) instanceof NodeOverNodeInfo) {
                    nodes[i] = ((NodeOverNodeInfo)list.item(i)).getUnderlyingNodeInfo();
                } else {
                    return null;
                }
            }
            return new SequenceExtent(nodes);

            // Note, we accept the nodes in the order returned by the function; there
            // is no requirement that this should be document order.
        } else if (object instanceof NodeOverNodeInfo) {
            return Value.asValue(((NodeOverNodeInfo)object).getUnderlyingNodeInfo());
        } else {
            return null;
        }
    }

    /**
     * Convert an XPath value to an object in this object model. If the supplied value can be converted
     * to an object in this model, of the specified class, then the conversion should be done and the
     * resulting object returned. If the value cannot be converted, the method should return null. Note
     * that the supplied class might be a List, in which case the method should inspect the contents of the
     * Value to see whether they belong to this object model.
     * @throws org.orbeon.saxon.trans.XPathException if the target class is explicitly associated with this object model, but the
     * supplied value cannot be converted to the appropriate class
     */

    public Object convertXPathValueToObject(Value value, Class target, XPathContext context) throws XPathException {
        // We accept the object if (a) the target class is Node, Node[], or NodeList,
        // or (b) the supplied object is a node, or sequence of nodes, that wrap DOM nodes,
        // provided that the target class is Object or a collection class
        boolean requireDOM =
                (Node.class.isAssignableFrom(target) || (target == NodeList.class) ||
                (target.isArray() && Node.class.isAssignableFrom(target.getComponentType())));

        // Note: we allow the declared type of the method argument to be a subclass of Node. If the actual
        // node supplied is the wrong kind of node, this will result in a Java exception.

        boolean allowDOM =
                (target == Object.class || target.isAssignableFrom(ArrayList.class) ||
                target.isAssignableFrom(HashSet.class) ||
                (target.isArray() && target.getComponentType() == Object.class));
        if (!(requireDOM || allowDOM)) {
            return null;
        }
        List nodes = new ArrayList(20);

        SequenceIterator iter = value.iterate(context);
        while (true) {
            Item item = iter.next();
            if (item == null) {
                break;
            }
            if (item instanceof VirtualNode) {
                Object o = ((VirtualNode)item).getUnderlyingNode();
                if (o instanceof Node) {
                    nodes.add(o);
                } else {
                    if (requireDOM) {
                        DynamicError err = new DynamicError("Extension function required class " + target.getName() +
                                "; supplied value of class " + item.getClass().getName() +
                                " could not be converted");
                        throw err;
                    };
                }
            } else if (requireDOM) {
                if (item instanceof NodeInfo) {
                    nodes.add(NodeOverNodeInfo.wrap((NodeInfo)item));
                } else {
                    DynamicError err = new DynamicError("Extension function required class " + target.getName() +
                                "; supplied value of class " + item.getClass().getName() +
                                " could not be converted");
                    throw err;
                }
            } else {
                return null;    // DOM Nodes are not actually required; let someone else try the conversion
            }
        }

        if (nodes.size() == 0 && !requireDOM) {
            return null;  // empty sequence supplied - try a different mapping
        }
        if (Node.class.isAssignableFrom(target)) {
            if (nodes.size() != 1) {
                DynamicError err = new DynamicError("Extension function requires a single DOM Node" +
                                "; supplied value contains " + nodes.size() + " nodes");
                throw err;
            }
            return nodes.get(0);
            // could fail if the node is of the wrong kind
        } else if (target == NodeList.class) {
            return new DOMNodeList(nodes);
        } else if (target.isArray() && target.getComponentType() == Node.class) {
            Node[] array = new Node[nodes.size()];
            nodes.toArray(array);
            return array;
        } else if (target.isAssignableFrom(ArrayList.class)) {
            return nodes;
        } else if (target.isAssignableFrom(HashSet.class)) {
            return new HashSet(nodes);
        } else {
            // after all this work, give up
            return null;
        }
    }

    /**
     * Wrap a document node in the external object model in a document wrapper that implements
     * the Saxon DocumentInfo interface. (However, if the supplied object is a wrapper for a Saxon
     * NodeInfo object, which is the case for this object model, then we <i>unwrap</i> it).
     * @param node    a node (any node) in the third party document
     * @param baseURI the base URI of the node (supply "" if unknown)
     * @param config the Saxon configuration (which among other things provides access to the NamePool)
     * @return the wrapper, which must implement DocumentInfo
     */

    public DocumentInfo wrapDocument(Object node, String baseURI, Configuration config) {
        DocumentInfo doc = null;
        if (node instanceof DocumentOverNodeInfo) {
            doc = (DocumentInfo)((DocumentOverNodeInfo)node).getUnderlyingNodeInfo();
        } else if (node instanceof NodeOverNodeInfo) {
            doc = ((NodeOverNodeInfo)node).getUnderlyingNodeInfo().getDocumentRoot();
        }
        if (doc == null) {
            throw new IllegalArgumentException("Unknown node class " + node.getClass());
        } else if (doc.getConfiguration() != config) {
            throw new IllegalArgumentException("Externally-supplied DOM envelope belong to the wrong Configuration");
        } else {
            return doc;
        }
    }

    /**
     * Wrap a node in the external object model in a wrapper that implements
     * the Saxon NodeInfo interface. (However, if the supplied object is a wrapper for a Saxon
     * NodeInfo object, which is the case for this object model, then we <i>unwrap</i> it).
     * @param document the document wrapper, as a DocumentInfo object
     * @param node the node to be wrapped. This must be a node within the document wrapped by the
     * DocumentInfo provided in the first argument
     * @return the wrapper for the node, as an instance of VirtualNode
     */

    public NodeInfo wrapNode(DocumentInfo document, Object node) {
        if (node instanceof NodeOverNodeInfo) {
            return ((NodeOverNodeInfo)node).getUnderlyingNodeInfo();
        }
        throw new IllegalArgumentException("Unknown node class " + node.getClass());
    }

    /**
     * Convert a sequence of values to a NODELIST, as defined in the JAXP XPath API spec. This method
     * is used when the evaluate() request specifies the return type as NODELIST, regardless of the
     * actual results of the expression. If the sequence contains things other than nodes, the fallback
     * is to return the sequence as a Java List object. The method can return null to invoke fallback
     * behaviour.
     */

    public Object convertToNodeList(SequenceExtent extent) {
        return null;
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
// Contributor(s): Gunther Schadow (changes to allow access to public fields; also wrapping
// of extensions and mapping of null to empty sequence).
//
