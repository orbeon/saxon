package net.sf.saxon;
import net.sf.saxon.event.PipelineConfiguration;
import net.sf.saxon.event.ReceivingContentHandler;
import net.sf.saxon.event.ResultWrapper;
import net.sf.saxon.trans.XPathException;
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
            setReceiver(ResultWrapper.getReceiver(result, pipe, props));
            setPipelineConfiguration(pipe);
        } catch (XPathException err) {
            throw new SAXException(err);
        }
        super.startDocument();
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
// Contributor(s): None
//
