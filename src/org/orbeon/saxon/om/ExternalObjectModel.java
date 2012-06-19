package org.orbeon.saxon.om;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.event.PipelineConfiguration;
import org.orbeon.saxon.event.Receiver;
import org.orbeon.saxon.expr.JPConverter;
import org.orbeon.saxon.expr.PJConverter;
import org.orbeon.saxon.trans.XPathException;

import javax.xml.transform.Result;
import javax.xml.transform.Source;

/**
 * This interface must be implemented by any third-party object model that can
 * be wrapped with a wrapper that implements the Saxon Object Model (the NodeInfo interface).
 * <p>
 * This interface is designed to enable advanced applications to implement and register
 * new object model implementations that Saxon can then use without change. Although it is intended
 * for external use, it cannot at this stage be considered part of the stable Saxon Public API.
 * In particular, it is likely that the interface will grow by the addition of new methods.
 */

public interface ExternalObjectModel {

    /**
     * Get the URI of the external object model as used in the JAXP factory interfaces for obtaining
     * an XPath implementation
     */

    public String getIdentifyingURI();

    /**
     * Get a converter from XPath values to values in the external object model
     * @param targetClass the required class of the result of the conversion. If this class represents
     * a node or list of nodes in the external object model, the method should return a converter that takes
     * a native node or sequence of nodes as input and returns a node or sequence of nodes in the
     * external object model representation. Otherwise, it should return null.
     * @return a converter, if the targetClass is recognized as belonging to this object model;
     * otherwise null
     */

    public PJConverter getPJConverter(Class targetClass);

    /**
     * Get a converter from a sequence of nodes to the object used in this object model to represent
     * a sequence of nodes.
     * @param node: an example of an object used to represent a node. If this external object model
     * does not recognize this object 
     */

    /**
     * Get a converter from XPath values to values in the external object model
     * @param targetClass the required class of the result of the conversion. If this class represents
     * a node or list of nodes in the external object model, the method should return a converter that takes
     * an object of this class as input and returns a node or sequence of nodes in the
     * native Saxon representation. Otherwise, it should return null.
     * @return a converter, if the targetClass is recognized as belonging to this object model;
     * otherwise null
     */

    public JPConverter getJPConverter(Class targetClass);

    /**
     * Get a converter that converts a sequence of XPath nodes to this model's representation
     * of a node list.
     * @param node an example of the kind of node used in this model
     * @return if the model does not recognize this node as one of its own, return null. Otherwise
     * return a PJConverter that takes a list of XPath nodes (represented as NodeInfo objects) and
     * returns a collection of nodes in this object model
     */

    public PJConverter getNodeListCreator(Object node);

    /**
     * Test whether this object model recognizes a given node as one of its own. This method
     * will generally be called at run time.
     * @param object An object that possibly represents a node
     * @return true if the object is a representation of a node in this object model
     */

    //public boolean isRecognizedNode(Object object);

    /**
     * Test whether this object model recognizes a given class as representing a
     * node in that object model. This method will generally be called at compile time.
     * @param nodeClass A class that possibly represents nodes
     * @return true if the class is used to represent nodes in this object model
     */

    //public boolean isRecognizedNodeClass(Class nodeClass);

    /**
     * Test whether this object model recognizes a given class as representing a
     * list of nodes in that object model. This method will generally be called at compile time.
     * @param nodeClass A class that possibly represents nodes
     * @return true if the class is used to represent nodes in this object model
     */

    //public boolean isRecognizedNodeListClass(Class nodeClass);

    /**
     * Test whether this object model recognizes a particular kind of JAXP Result object,
     * and if it does, return a Receiver that builds an instance of this data model from
     * a sequence of events. If the Result is not recognised, return null.
     * @param result a JAXP result object
     * @return a Receiver that writes to that result, if available; or null otherwise
     */

    public Receiver getDocumentBuilder(Result result) throws XPathException;

    /**
     * Test whether this object model recognizes a particular kind of JAXP Source object,
     * and if it does, send the contents of the document to a supplied Receiver, and return true.
     * Otherwise, return false.
     * @param source a JAXP Source object
     * @param receiver the Receiver that is to receive the data from the Source
     * @param pipe configuration information
     * @return true if the data from the Source has been sent to the Receiver, false otherwise
     */

    public boolean sendSource(Source source, Receiver receiver, PipelineConfiguration pipe) throws XPathException;

    /**
     * Wrap or unwrap a node using this object model to return the corresponding Saxon node. If the supplied
     * source does not belong to this object model, return null
     * @param source a JAXP Source object
     * @param config the Saxon configuration
     * @return a NodeInfo corresponding to the Source, if this can be constructed; otherwise null
     */

    public NodeInfo unravel(Source source, Configuration config);

    /**
     * Convert a Java object to an XPath value. If the supplied object is recognized as a representation
     * of a value using this object model, the object model should convert the value to an XPath value
     * and return this as the result. If not, it should return null. If the object is recognized but cannot
     * be converted, an exception should be thrown
     * @param object the object to be converted
     * @param config the Saxon configuration
     * @return the result of the conversion if the object can be converted, or null otherwise
     */

    //public Value convertObjectToXPathValue(Object object, Configuration config) throws XPathException;

    /**
     * Convert an XPath value to an object in this object model. If the supplied value can be converted
     * to an object in this model, of the specified class, then the conversion should be done and the
     * resulting object returned. If the value cannot be converted, the method should return null. Note
     * that the supplied class might be a List, in which case the method should inspect the contents of the
     * Value to see whether they belong to this object model.
     * @param value the value to be converted
     * @param targetClass the required class of the result of the conversion. (This is a Class object when calling
     * a Java method, a Type object when calling .NET)
     * @param context the XPath dynamic evaluation context
     * @return the result of the conversion; always an instance of targetClass, or null if the value
     * cannot be converted.
     * @throws XPathException if the target class is explicitly associated with this object model, but the
     * supplied value cannot be converted to the appropriate class
     */

//    public Object convertXPathValueToObject(Value value, Object targetClass, XPathContext context) throws XPathException;

    /**
     * Wrap a document node in the external object model in a document wrapper that implements
     * the Saxon DocumentInfo interface
     * @param node    a node (any node) in the third party document
     * @param baseURI the base URI of the node (supply "" if unknown)
     * @param config  the Saxon configuration (which among other things provides access to the NamePool)
     * @return the wrapper, which must implement DocumentInfo
     */

    //public DocumentInfo wrapDocument(Object node, String baseURI, Configuration config);

    /**
     * Wrap a node within the external object model in a node wrapper that implements the Saxon
     * VirtualNode interface (which is an extension of NodeInfo)
     * @param document the document wrapper, as a DocumentInfo object
     * @param node the node to be wrapped. This must be a node within the document wrapped by the
     * DocumentInfo provided in the first argument
     * @return the wrapper for the node, as an instance of VirtualNode
     */

//    public NodeInfo wrapNode(DocumentInfo document, Object node);

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
