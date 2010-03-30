package org.orbeon.saxon;

import org.orbeon.saxon.event.*;
import org.orbeon.saxon.om.AllElementStripper;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.Whitespace;
import org.xml.sax.SAXException;

import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import java.util.Properties;

/**
  * <b>IdentityTransformerHandler</b> implements the javax.xml.transform.sax.TransformerHandler
  * interface. It acts as a ContentHandler and LexicalHandler which receives a stream of
  * SAX events representing an input document, and performs an identity transformation passing
  * these events to a Result
  * @author Michael H. Kay
  */

public class IdentityTransformerHandler extends ReceivingContentHandler implements TransformerHandler {

    private Result result;
    private String systemId;
    private Controller controller;

    /**
     * Create a IdentityTransformerHandler and initialise variables. The constructor is protected, because
     * the Filter should be created using newTransformerHandler() in the SAXTransformerFactory
     * class
     * @param controller the Controller for this transformation
    */

    protected IdentityTransformerHandler(Controller controller) {
        this.controller = controller;
        setPipelineConfiguration(controller.makePipelineConfiguration());

    }

    /**
    * Get the Transformer used for this transformation
    */

    public Transformer getTransformer() {
        return controller;
    }

    /**
    * Set the SystemId of the document
    */

    public void setSystemId(String url) {
        systemId = url;
    }

    /**
    * Get the systemId of the document
    */

    public String getSystemId() {
        return systemId;
    }

    /**
    * Set the output destination of the transformation
    */

    public void setResult(Result result) {
        if (result==null) {
            throw new IllegalArgumentException("Result must not be null");
        }
        this.result = result;
    }

    /**
     * Get the output destination of the transformation
     * @return the output destination
    */

    public Result getResult() {
        return result;
    }

    /**
    * Override the behaviour of startDocument() in ReceivingContentHandler
    */

    public void startDocument() throws SAXException {
        if (result==null) {
            result = new StreamResult(System.out);
        }
        try {
            Properties props = controller.getOutputProperties();
            PipelineConfiguration pipe = controller.makePipelineConfiguration();
            Configuration config = getConfiguration();
            SerializerFactory sf = config.getSerializerFactory();
            Receiver out = sf.getReceiver(result, pipe, props);
            setPipelineConfiguration(pipe);
            int stripSpace = config.getStripsWhiteSpace();
            if (stripSpace == Whitespace.ALL) {
                Stripper s = new AllElementStripper();
                s.setStripAll();
                s.setPipelineConfiguration(pipe);
                s.setUnderlyingReceiver(out);
                out = s;
            }
            setReceiver(out);
        } catch (XPathException err) {
            throw new SAXException(err);
        }
        super.startDocument();
    }

//    public static void main(String[] args) throws Exception {
//		String in = "<rows>" + "   " +
//		"<r>\t<c>1</c>\n<c>2000</c><c>inq1</c> \t\r\t\n <c>inquiry1</c></r> " +
//		"</rows>";
//		String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
//				"<rows>" +
//		"<r><c>1</c><c>2000</c><c>inq1</c><c>inquiry1</c></r>" +
//		"</rows>" ;
//		//System.setProperty(TRANSFORMERFACTORY_PROPERTY,TRANSFORMERFACTORY_IMPL);
//		TransformerFactory transFactory = new TransformerFactoryImpl();
//		transFactory.setAttribute(
//				org.orbeon.saxon.FeatureKeys.STRIP_WHITESPACE, "all");
//		if (transFactory.getFeature(SAXTransformerFactory.FEATURE)) {
//			// We can use an intermediate SAXResult and feed it to a
//			// TransformerHandler
//			SAXTransformerFactory saxTransFactory = (SAXTransformerFactory) transFactory;
//			TransformerHandler identityTransformerHandler = saxTransFactory
//					.newTransformerHandler();
//
//			SAXParserFactory factory = SAXParserFactory.newInstance();
//	        factory.setNamespaceAware(true) ;
//	        SAXParser saxParser = factory.newSAXParser();
//	        XMLReader reader = saxParser.getXMLReader();
//			DOMResult result = new DOMResult() ;
//			identityTransformerHandler.setResult(result);
//	        reader.setContentHandler(identityTransformerHandler);
//	        reader.parse(new InputSource(new StringReader(in)));
//
//	        //When we further transform the result in a String using a transformer, the
//	        //whitespace nodes are not there
//	        Transformer transf = transFactory.newTransformer();
//	        DOMSource inDOM = new DOMSource(result.getNode());
//	        StringWriter resWriter = new StringWriter() ;
//	        StreamResult resStream = new StreamResult(resWriter) ;
//	        transf.transform(inDOM, resStream);
//	        String res = resWriter.toString();
//	        System.err.println(expected);
//            System.err.println(res);
//	        // But the whitespace nodes are available in the DOM represenatation
//	        Node docNode = result.getNode(); // The document node
//			Node rowsNode = docNode.getFirstChild();
//			String name = rowsNode.getNodeName() ;
//			System.err.println(name);
//			Node rNode = rowsNode.getFirstChild();
//			System.err.println(Node.ELEMENT_NODE == rNode.getNodeType());
//		}
//		else {
//			throw new UnsupportedOperationException() ;
//		}
//
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
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): None
//
