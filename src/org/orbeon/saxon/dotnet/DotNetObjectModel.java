package org.orbeon.saxon.dotnet;

import cli.System.Xml.XmlDocument;
import cli.System.Xml.XmlNode;
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
import org.orbeon.saxon.value.SingletonNode;
import org.orbeon.saxon.value.Value;

import javax.xml.transform.Result;
import javax.xml.transform.Source;

/**
 * The DotNetObjectModel is an ExternalObjectModel that recognizes nodes as defined in the .Net
 * System.Xml namespace, and also recognizes the wrapper objects defined in the Saxon.Api interface. This
 * allows the Saxon.Api objects to be used withing extension functions. This is an abstract class because
 * it is designed to have no references to the classes in Saxon.Api; instead there is a concrete subclass in
 * the Saxon.Api package that has this knowledge.
 */
public abstract class DotNetObjectModel implements ExternalObjectModel {

    /**
     * Get the URI of the external object model as used in the JAXP factory interfaces for obtaining
     * an XPath implementation
     */

    public String getIdentifyingURI() {
        return NamespaceConstant.OBJECT_MODEL_DOT_NET_DOM;
    }

    public PJConverter getPJConverter(Class targetClass) {
        if (isRecognizedNodeClass(targetClass)) {
            return new PJConverter() {
                public Object convert(ValueRepresentation value, Class targetClass, XPathContext context) throws XPathException {
                    return null; //TODO:FIXME!
                    //return convertXPathValueToObject(Value.asValue(value), targetClass, context);
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
                    return unwrapXdmValue(object);
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
     * Test whether the supplied object is an XDM value as defined in Saxon.Api
     * (implemented this way to avoid a reference to the Saxon.Api package)
     * @param object the object under test
     * @return true if it is an instance of XdmValue
     */

    public abstract boolean isXdmValue(Object object);

   /**
     * Test whether the supplied type is an XDM value type as defined in Saxon.Api
     * (implemented this way to avoid a reference to the Saxon.Api package)
     * @param type the type under test
     * @return true if the type is a subtype of XdmValue
     */

    public abstract boolean isXdmValueType(cli.System.Type type);

    /**
     * Unwrap an XdmValue
     * @param object the supplied XdmValue
     * @return the underlying Value
     */

    public abstract ValueRepresentation unwrapXdmValue(Object object);

    /**
     * Wrap a Value as an XdmValue
     * @param value the value to be wrapped
     * @return the resulting XdmValue
     */

    public abstract Object wrapAsXdmValue(Value value);

    /**
     * Test whether the supplied type is a subtype of System.Xml.XmlNode
     * @param type the supplied type
     * @return true if the supplied type is System.Xml.XmlNode or a subtype thereof
     */

    public abstract boolean isXmlNodeType(cli.System.Type type);

    /**
     * Convert a Java object to an XPath value. If the supplied object is recognized as a representation
     * of a value using this object model, the object model should convert the value to an XPath value
     * and return this as the result. If not, it should return null. If the object is recognized but cannot
     * be converted, an exception should be thrown
     *
     * @param object the object to be converted
     * @param config the Saxon configuration
     * @return the result of the conversion if the object can be converted, or null otherwise
     */

    public Value convertObjectToXPathValue(Object object, Configuration config) throws XPathException {
        if (object instanceof XmlNode) {
            XmlNode node = (XmlNode)object;
            DocumentWrapper dw = new DocumentWrapper(
                    node.get_OwnerDocument(),  node.get_BaseURI(), config);
            NodeWrapper nw = dw.wrap(node);
            return new SingletonNode(nw);         
        } else if (isXdmValue(object)) {
            return Value.asValue(unwrapXdmValue(object));
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
     *
     * @param value       the value to be converted
     * @param targetClass the required class of the result of the conversion
     * @param context     the XPath dynamic evaluation context
     * @return the result of the conversion; always an instance of targetClass, or null if the value
     *         cannot be converted.
     * @throws org.orbeon.saxon.trans.XPathException
     *          if the target class is explicitly associated with this object model, but the
     *          supplied value cannot be converted to the appropriate class
     */

    public Object convertXPathValueToObject(Value value, cli.System.Type targetClass, XPathContext context) throws XPathException {
        //System.err.println("CONVERT TO: " + targetClass.toString());
        if (isXmlNodeType(targetClass) &&
                value instanceof SingletonNode &&
                ((SingletonNode)value).getNode() instanceof NodeWrapper) {
            return ((NodeWrapper)((SingletonNode)value).getNode()).getUnderlyingNode();
        } else {
            if (isXdmValueType(targetClass)) {
                return wrapAsXdmValue(value);
            }
        }
        return null;
    }

    /**
     * Test whether this object model recognizes a particular kind of JAXP Result object,
     * and if it does, return a Receiver that builds an instance of this data model from
     * a sequence of events. If the Result is not recognised, return null.
     *
     * @param result a JAXP result object
     * @return a Receiver that writes to that result, if available; or null otherwise
     */

    public Receiver getDocumentBuilder(Result result) throws XPathException {
        return null;
    }

    /**
     * Test whether this object model recognizes a given node as one of its own. This method
     * will generally be called at run time.
     *
     * @param object An object that possibly represents a node
     * @return true if the object is a representation of a node in this object model
     */

    public boolean isRecognizedNode(Object object) {
        return object instanceof XmlNode;
    }

    /**
     * Test whether this object model recognizes a given class as representing a
     * node in that object model. This method will generally be called at compile time.
     *
     * @param nodeClass A class that possibly represents nodes
     * @return true if the class is used to represent nodes in this object model
     */

    private boolean isRecognizedNodeClass(Class nodeClass) {
        return XmlNode.class.isAssignableFrom(nodeClass);
    }


    /**
     * Test whether this object model recognizes a particular kind of JAXP Source object,
     * and if it does, send the contents of the document to a supplied Receiver, and return true.
     * Otherwise, return false.
     *
     * @param source   a JAXP Source object
     * @param receiver the Receiver that is to receive the data from the Source
     * @param pipe     configuration information
     * @return true if the data from the Source has been sent to the Receiver, false otherwise
     */

    public boolean sendSource(Source source, Receiver receiver, PipelineConfiguration pipe) throws XPathException {
        return false;
    }

    /**
     * Wrap or unwrap a node using this object model to return the corresponding Saxon node. If the supplied
     * source does not belong to this object model, return null
     *
     * @param source a JAXP Source object
     * @param config the Saxon configuration
     * @return a NodeInfo corresponding to the Source, if this can be constructed; otherwise null
     */

    public NodeInfo unravel(Source source, Configuration config) {
        return null; 
    }

    /**
     * Wrap a document node in the external object model in a document wrapper that implements
     * the Saxon DocumentInfo interface
     *
     * @param node    a node (any node) in the third party document
     * @param baseURI the base URI of the node (supply "" if unknown)
     * @param config  the Saxon configuration (which among other things provides access to the NamePool)
     * @return the wrapper, which must implement DocumentInfo
     */

    public DocumentInfo wrapDocument(Object node, String baseURI, Configuration config) {
        if (node instanceof XmlDocument) {
            return new DocumentWrapper((XmlDocument)node, baseURI, config);
        } else {
            return null;
        }
    }

    /**
     * Wrap a node within the external object model in a node wrapper that implements the Saxon
     * VirtualNode interface (which is an extension of NodeInfo)
     *
     * @param document the document wrapper, as a DocumentInfo object
     * @param node     the node to be wrapped. This must be a node within the document wrapped by the
     *                 DocumentInfo provided in the first argument
     * @return the wrapper for the node, as an instance of VirtualNode
     */

    public NodeInfo wrapNode(DocumentInfo document, Object node) {
        if (document instanceof DocumentWrapper && node instanceof XmlNode) {
            return ((DocumentWrapper)document).wrap((XmlNode)node);
        } else {
            return null;
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
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//

