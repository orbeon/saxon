package org.orbeon.saxon.query;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.event.*;
import org.orbeon.saxon.om.DocumentInfo;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.tinytree.TinyBuilder;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.QNameValue;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.Properties;

/**
 * This utility class takes the result sequence produced by a query, and wraps it as
 * an XML document. The class is never instantiated.
 */
public class QueryResult {

    public static String RESULT_NS = "http://saxon.sf.net/xquery-results";

    private QueryResult() {
    }

    /**
     * Take the results of a query (or any other SequenceIterator) and create
     * an XML document containing copies of all items in the sequence, suitably wrapped
     * @param iterator  The values to be wrapped
     * @param config The Saxon configuration used to evaluate the query
     * @return the document containing the wrapped results
     * @throws XPathException
     */

    public static DocumentInfo wrap(SequenceIterator iterator, Configuration config) throws XPathException {
        PipelineConfiguration pipe = config.makePipelineConfiguration();
        TinyBuilder builder = new TinyBuilder();
        builder.setPipelineConfiguration(pipe);
        NamespaceReducer reducer = new NamespaceReducer();
        reducer.setUnderlyingReceiver(builder);
        reducer.setPipelineConfiguration(pipe);
        ComplexContentOutputter outputter = new ComplexContentOutputter();
        outputter.setPipelineConfiguration(pipe);
        outputter.setReceiver(reducer);
        sendWrappedSequence(iterator, outputter);
        return (DocumentInfo)builder.getCurrentRoot();
    }

    /**
     * Take a sequence supplied in the form of an iterator and generate a wrapped represention of the
     * items in the sequence, the wrapped representation being a sequence of events sent to a supplied
     * Receiver.
     * @param iterator the input sequence
     * @param destination the Receiver to accept the wrapped output
     */

    public static void sendWrappedSequence(SequenceIterator iterator, Receiver destination) throws XPathException {
        SequenceCopier.copySequence(iterator, new SequenceWrapper(destination));
    }

    /**
     * Serialize a document containing wrapped query results (or any other document, in fact)
     * as XML.
     * @param node                      The document or element to be serialized
     * @param destination               The Result object to contain the serialized form
     * @param outputProperties          Serialization options
     * @param config                    The Configuration
     * @throws XPathException     If serialization fails
     */

    public static void serialize(NodeInfo node, Result destination, Properties outputProperties, Configuration config)
    throws XPathException {
        PipelineConfiguration pipe = config.makePipelineConfiguration();
        int type = node.getNodeKind();
        if (type==Type.DOCUMENT || type==Type.ELEMENT) {
            DocumentSender sender = new DocumentSender(node);
            SerializerFactory sf = config.getSerializerFactory();
            Receiver receiver = sf.getReceiver(destination,
                                              pipe,
                                              outputProperties);
            NamespaceReducer reducer = new NamespaceReducer();
            reducer.setUnderlyingReceiver(receiver);
            reducer.setPipelineConfiguration(pipe);
//            ComplexContentOutputter cco = new ComplexContentOutputter();
//            cco.setReceiver(reducer);
//            cco.setPipelineConfiguration(pipe);
            sender.send(reducer);
        } else {
            throw new DynamicError("Node to be serialized must be a Document or Element node");
        }
    }

    /**
     * Serialize an arbitrary sequence, without any special wrapping.
     * @param results the sequence to be serialized
     * @param config the configuration (gives access to information such as the NamePool)
     * @param destination the output stream to which the output is to be written
     * @param outputProps a set of serialization properties as defined in JAXP
     * @throws XPathException if any failure occurs
     */

    public static void serializeSequence(SequenceIterator results, Configuration config,
                                          OutputStream destination, Properties outputProps)
            throws XPathException {
            String encoding = outputProps.getProperty(OutputKeys.ENCODING);
        if (encoding==null) {
            encoding = "UTF-8";
        }
        PrintWriter writer;
        try {
            writer = new PrintWriter(
                new OutputStreamWriter(destination, encoding));
        } catch (UnsupportedEncodingException err) {
            throw new DynamicError(err);
        }
        serializeSequence(results, config, writer, outputProps);
    }

    /**
     * Serialize an arbitrary sequence, without any special wrapping.
     * @param results the sequence to be serialized
     * @param config the configuration (gives access to information such as the NamePool)
     * @param writer the writer to which the output is to be written
     * @param outputProps a set of serialization properties as defined in JAXP
     * @throws XPathException if any failure occurs
     */

    public static void serializeSequence(SequenceIterator results, Configuration config,
            PrintWriter writer, Properties outputProps)
            throws XPathException {

        while (true) {
            Item item = results.next();
            if (item == null) break;
            if (item instanceof NodeInfo) {
                switch (((NodeInfo)item).getNodeKind()) {
                case Type.DOCUMENT:
                case Type.ELEMENT:
                    serialize((NodeInfo)item,
                            new StreamResult(writer),
                            outputProps,
                            config);
                    writer.println("");
                    break;
                case Type.ATTRIBUTE:
                    writer.println(((NodeInfo)item).getLocalPart() +
                                   "=\"" +
                                   item.getStringValue() +
                                   '\"');
                    break;
                case Type.COMMENT:
                    writer.println("<!--" + item.getStringValue() + "-->");
                    break;
                case Type.PROCESSING_INSTRUCTION:
                    writer.println("<?" +
                                   ((NodeInfo)item).getLocalPart() +
                                   ' ' +
                                   item.getStringValue() +
                                   "?>");
                    break;
                default:
                    writer.println(item.getStringValue());
                }
            } else if (item instanceof QNameValue) {
                writer.println(((QNameValue)item).getClarkName());
            } else {
                writer.println(item.getStringValue());
            }
        }
        writer.flush();
    }

//    public static void main(String[] params) throws Exception {
//        StaticQueryContext env = new StaticQueryContext(new Configuration());
//        QueryProcessor qp = new QueryProcessor(env);
//        XQueryExpression exp = qp.compileQuery("<a><b/></a>");
//        SequenceIterator iter = exp.iterator(new DynamicQueryContext());
//        NodeInfo node = (NodeInfo)iter.next();
//        serialize(node, new SAXResult(new ExampleContentHandler()), new Properties());
//
//    }
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
// Contributor(s): none.
//
