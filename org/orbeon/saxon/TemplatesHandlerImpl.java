package org.orbeon.saxon;
import org.orbeon.saxon.event.CommentStripper;
import org.orbeon.saxon.event.ReceivingContentHandler;
import org.orbeon.saxon.style.StyleNodeFactory;
import org.orbeon.saxon.style.StylesheetStripper;
import org.orbeon.saxon.tree.DocumentImpl;
import org.orbeon.saxon.tree.TreeBuilder;

import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.TemplatesHandler;

import org.xml.sax.Locator;


/**
  * <b>TemplatesHandlerImpl</b> implements the javax.xml.transform.sax.TemplatesHandler
  * interface. It acts as a ContentHandler which receives a stream of
  * SAX events representing a stylesheet, and returns a Templates object that
  * represents the compiled form of this stylesheet.
  * @author Michael H. Kay
  */

public class TemplatesHandlerImpl extends ReceivingContentHandler implements TemplatesHandler {

    private TreeBuilder builder;
    private StyleNodeFactory nodeFactory;
    private Templates templates;
    private String systemId;

    /**
    * Create a TemplatesHandlerImpl and initialise variables. The constructor is protected, because
    * the Filter should be created using newTemplatesHandler() in the SAXTransformerFactory
    * class
    */

    protected TemplatesHandlerImpl(Configuration config) {

        setConfiguration(config);

        nodeFactory = new StyleNodeFactory(config.getNamePool(), config.isAllowExternalFunctions());

        StylesheetStripper styleStripper = new StylesheetStripper();
        styleStripper.setStylesheetRules(config.getNamePool());

        builder = new TreeBuilder();
        builder.setConfiguration(config);
        builder.setNodeFactory(nodeFactory);
        builder.setLineNumbering(true);

        styleStripper.setUnderlyingReceiver(builder);

        CommentStripper commentStripper = new CommentStripper();
        commentStripper.setUnderlyingReceiver(styleStripper);

        this.setReceiver(commentStripper);

    }

    /**
    * Get the Templates object to used for a transformation
    */

    public Templates getTemplates() {
        if (templates==null) {
            DocumentImpl doc = (DocumentImpl)builder.getCurrentDocument();
            if (doc==null) {
                return null;
            }
            PreparedStylesheet sheet = new PreparedStylesheet(getConfiguration());
            try {
                sheet.setStylesheetDocument(doc, nodeFactory);
                templates = sheet;
            } catch (TransformerConfigurationException tce) {
                // don't know why we aren't allowed to just throw it!
                throw new UnsupportedOperationException(tce.getMessage());
            }
        }

        return templates;
    }

    /**
    * Set the SystemId of the document
    */

    public void setSystemId(String url) {
        systemId = url;
        builder.setSystemId(url);
    }

    /**
    * Callback interface for SAX: not for application use
    */

    public void setDocumentLocator (Locator locator) {
    	super.setDocumentLocator(locator);
        setSystemId(locator.getSystemId());
    }

    /**
    * Get the systemId of the document
    */

    public String getSystemId() {
        return systemId;
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
