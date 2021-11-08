package org.orbeon.saxon.xom;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.event.PipelineConfiguration;
import org.orbeon.saxon.event.Receiver;
import org.orbeon.saxon.expr.JPConverter;
import org.orbeon.saxon.expr.PJConverter;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.pattern.AnyNodeTest;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.value.SequenceExtent;
import org.orbeon.saxon.value.Value;
import nu.xom.Document;
import nu.xom.Node;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;



/**
 * This interface must be implemented by any third-party object model that can
 * be wrapped with a wrapper that implements the Saxon Object Model (the NodeInfo interface).
 * This implementation of the interface supports wrapping of JDOM Documents.
 */

public class XOMObjectModel implements ExternalObjectModel, Serializable {

    public XOMObjectModel() {}

    /**
     * Get the URI of the external object model as used in the JAXP factory interfaces for obtaining
     * an XPath implementation
     */

    public String getIdentifyingURI() {
        return NamespaceConstant.OBJECT_MODEL_XOM;
    }

    public PJConverter getPJConverter(Class targetClass) {
        if (isRecognizedNodeClass(targetClass)) {
            return new PJConverter() {
                public Object convert(ValueRepresentation value, Class targetClass, XPathContext context) throws XPathException {
                    return convertXPathValueToObject(Value.asValue(value), targetClass, context);
                }
            };
        } else {
            return null;
        }
    }

    public JPConverter getJPConverter(Class targetClass) {
        if (isRecognizedNodeClass(targetClass)) {
            return new JPConverter() {
                public ValueRepresentation convert(Object object, XPathContext context) throws XPathException {
                    return convertObjectToXPathValue(object, context.getConfiguration());
                }
                public ItemType getItemType() {
                    return AnyNodeTest.getInstance();
                }
            };
        } else {
            return null;
        }
    }

    /**
     * Get a converter that converts a sequence of XPath nodes to this model's representation
     * of a node list.
     * @param node an example of the kind of node used in this model
     * @return if the model does not recognize this node as one of its own, return null. Otherwise
     *         return a PJConverter that takes a list of XPath nodes (represented as NodeInfo objects) and
     *         returns a collection of nodes in this object model
     */

    public PJConverter getNodeListCreator(Object node) {
        return null;
    }

    /**
     * Test whether this object model recognizes a given node as one of its own
     */

    public boolean isRecognizedNode(Object object) {
        return (object instanceof nu.xom.Node);
    }

    /**
     * Test whether this object model recognizes a given class as representing a
     * node in that object model. This method will generally be called at compile time.
     *
     * @param nodeClass A class that possibly represents nodes
     * @return true if the class is used to represent nodes in this object model
     */

    public boolean isRecognizedNodeClass(Class nodeClass) {
        return nu.xom.Node.class.isAssignableFrom(nodeClass);
    }

    /**
     * Test whether this object model recognizes a given class as representing a
     * list of nodes in that object model. This method will generally be called at compile time.
     *
     * @param nodeClass A class that possibly represents nodes
     * @return true if the class is used to represent nodes in this object model
     */

    public boolean isRecognizedNodeListClass(Class nodeClass) {
        return false;
    }

    /**
     * Test whether this object model recognizes a particular kind of JAXP Result object,
     * and if it does, return a Receiver that builds an instance of this data model from
     * a sequence of events. If the Result is not recognised, return null.
     */

    public Receiver getDocumentBuilder(Result result) {
        return null;
    }

    /**
     * Test whether this object model recognizes a particular kind of JAXP Source object,
     * and if it does, send the contents of the document to a supplied Receiver, and return true.
     * Otherwise, return false.
     */

    public boolean sendSource(Source source, Receiver receiver, PipelineConfiguration pipe) throws XPathException {
        return false;
    }

    /**
     * Wrap or unwrap a node using this object model to return the corresponding Saxon node. If the supplied
     * source does not belong to this object model, return null
     */

    public NodeInfo unravel(Source source, Configuration config) {
        return null;
    }

    /**
     * Convert a Java object to an XPath value. If the supplied object is recognized as a representation
     * of a value using this object model, the object model should convert the value to an XPath value
     * and return this as the result. If not, it should return null. If the object is recognized but cannot
     * be converted, an exception should be thrown
     */

    public ValueRepresentation convertObjectToXPathValue(Object object, Configuration config) throws XPathException {
        if (object instanceof Node) {
            return wrapNode((Node)object, config);
        } else if (object instanceof Node[]) {
            NodeInfo[] nodes = new NodeInfo[((Node[])object).length];
            for (int i=0; i<nodes.length; i++) {
                nodes[i] = wrapNode(((Node[])object)[i], config);
            }
            return new SequenceExtent(nodes);
        } else {
            return null;
        }
    }

     private synchronized NodeInfo wrapNode(Node node, Configuration config) {
    	    return new DocumentWrapper(node.getDocument(), "", config).wrap(node);
     }

    /**
     * Convert an XPath value to an object in this object model. If the supplied value can be converted
     * to an object in this model, of the specified class, then the conversion should be done and the
     * resulting object returned. If the value cannot be converted, the method should return null. Note
     * that the supplied class might be a List, in which case the method should inspect the contents of the
     * Value to see whether they belong to this object model.
     */

    public Object convertXPathValueToObject(Value value, Object targetClass, XPathContext context)
    throws XPathException {
        // We accept the object if (a) the target class is Node or Node[],
        // or (b) the supplied object is a node, or sequence of nodes, that wrap XOM nodes,
        // provided that the target class is Object or a collection class
        Class target = (Class)targetClass;
        boolean requireXOM =
                (Node.class.isAssignableFrom(target) ||
                (target.isArray() && Node.class.isAssignableFrom(target.getComponentType())));

        // Note: we allow the declared type of the method argument to be a subclass of Node. If the actual
        // node supplied is the wrong kind of node, this will result in a Java exception.

        boolean allowXOM =
                (target == Object.class || target.isAssignableFrom(ArrayList.class) ||
                target.isAssignableFrom(HashSet.class) ||
                (target.isArray() && target.getComponentType() == Object.class));
        if (!(requireXOM || allowXOM)) {
            return null;
        }
        List nodes = new ArrayList(20);

        SequenceIterator iter = value.iterate();
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
                    if (requireXOM) {
                        XPathException err = new XPathException("Extension function required class " + target.getName() +
                                "; supplied value of class " + item.getClass().getName() +
                                " could not be converted");
                        throw err;
                    }
                }
            } else if (requireXOM) {
                XPathException err = new XPathException("Extension function required class " + target.getName() +
                        "; supplied value of class " + item.getClass().getName() +
                        " could not be converted");
                throw err;
            } else {
                return null;   
            }
        }

        if (nodes.isEmpty() && !requireXOM) {
            return null;  // empty sequence supplied - try a different mapping
        }
        if (Node.class.isAssignableFrom(target)) {
            if (nodes.size() != 1) {
                XPathException err = new XPathException("Extension function requires a single XOM Node" +
                        "; supplied value contains " + nodes.size() + " nodes");
                throw err;
            }
            return nodes.get(0);
        } else if (target.isArray() && Node.class.isAssignableFrom(target.getComponentType())) {
            Node[] array = (Node[])Array.newInstance(target.getComponentType(), nodes.size());
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
     * the Saxon DocumentInfo interface
     * @param node    a node (any node) in the third party document
     * @param baseURI the base URI of the node (supply "" if unknown)
     * @param config the Saxon configuration (which among other things provides access to the NamePool)
     * @return the wrapper, which must implement DocumentInfo
     */

    public DocumentInfo wrapDocument(Object node, String baseURI, Configuration config) {
        Document documentNode = ((Node)node).getDocument();
        return new DocumentWrapper(documentNode, baseURI, config);
    }

    /**
     * Wrap a node within the external object model in a node wrapper that implements the Saxon
     * VirtualNode interface (which is an extension of NodeInfo)
     * @param document the document wrapper, as a DocumentInfo object
     * @param node the node to be wrapped. This must be a node within the document wrapped by the
     * DocumentInfo provided in the first argument
     * @return the wrapper for the node, as an instance of VirtualNode
     */

    public NodeInfo wrapNode(DocumentInfo document, Object node) {
        if (!(node instanceof Node)) {
            throw new IllegalArgumentException("Object to be wrapped is not a XOM Node: " + node.getClass());
        }
        return ((DocumentWrapper)document).wrap((Node)node);
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
