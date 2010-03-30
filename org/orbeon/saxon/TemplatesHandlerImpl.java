package org.orbeon.saxon;
import org.orbeon.saxon.event.CommentStripper;
import org.orbeon.saxon.event.ReceivingContentHandler;
import org.orbeon.saxon.event.StartTagBuffer;
import org.orbeon.saxon.style.StyleNodeFactory;
import org.orbeon.saxon.style.StylesheetStripper;
import org.orbeon.saxon.style.UseWhenFilter;
import org.orbeon.saxon.trans.CompilerInfo;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.tree.DocumentImpl;
import org.orbeon.saxon.tree.TreeBuilder;
import org.xml.sax.Locator;

import javax.xml.transform.Templates;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.TemplatesHandler;


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
     * @param config the Saxon configuration
    */

    protected TemplatesHandlerImpl(Configuration config) {

        setPipelineConfiguration(config.makePipelineConfiguration());

        nodeFactory = new StyleNodeFactory(config, getPipelineConfiguration().getErrorListener());

        builder = new TreeBuilder();
        builder.setPipelineConfiguration(getPipelineConfiguration());
        builder.setNodeFactory(nodeFactory);
        builder.setLineNumbering(true);

        StartTagBuffer startTagBuffer = new StartTagBuffer();

        UseWhenFilter useWhenFilter = new UseWhenFilter(startTagBuffer);
        useWhenFilter.setUnderlyingReceiver(builder);
        useWhenFilter.setPipelineConfiguration(getPipelineConfiguration());

        startTagBuffer.setUnderlyingReceiver(useWhenFilter);
        startTagBuffer.setPipelineConfiguration(getPipelineConfiguration());

        StylesheetStripper styleStripper = new StylesheetStripper();
        styleStripper.setUnderlyingReceiver(startTagBuffer);
        styleStripper.setPipelineConfiguration(getPipelineConfiguration());

        CommentStripper commentStripper = new CommentStripper();
        commentStripper.setUnderlyingReceiver(styleStripper);
        commentStripper.setPipelineConfiguration(getPipelineConfiguration());

        setReceiver(commentStripper);

    }

    /**
    * Get the Templates object to be used for a transformation
    */

    public Templates getTemplates() {
        if (templates==null) {
            DocumentImpl doc = (DocumentImpl)builder.getCurrentRoot();
            builder.reset();
            if (doc==null) {
                return null;
            }

            final Configuration config = getConfiguration();
            CompilerInfo info = new CompilerInfo();
            info.setURIResolver(config.getURIResolver());
            info.setErrorListener(config.getErrorListener());
            info.setCompileWithTracing(config.isCompileWithTracing());
            PreparedStylesheet sheet = new PreparedStylesheet(config, info);

            try {
                sheet.setStylesheetDocument(doc, nodeFactory);
                templates = sheet;
            } catch (XPathException tce) {
                if (!tce.hasBeenReported()) {
                    try {
                        info.getErrorListener().fatalError(tce);
                    } catch (TransformerException e2) {
                        //
                    }
                }
                // don't know why we aren't allowed to just throw it!
                throw new IllegalStateException(tce.getMessage());
            }
        }

        return templates;
    }

    /**
     * Set the SystemId of the document. Note that if this method is called, any locator supplied
     * to the setDocumentLocator() method is ignored. This also means that no line number information
     * will be available.
     * @param url the system ID (base URI) of the stylesheet document, which will be used in any error
     * reporting and also for resolving relative URIs in xsl:include and xsl:import. It will also form
     * the static base URI in the static context of XPath expressions.
    */

    public void setSystemId(String url) {
        systemId = url;
        builder.setSystemId(url);
        super.setDocumentLocator(new Locator() {
            public int getColumnNumber() {
                return -1;
            }

            public int getLineNumber() {
                return -1;
            }

            public String getPublicId() {
                return null;
            }

            public String getSystemId() {
                return systemId;
            }
        });
    }

    /**
    * Callback interface for SAX: not for application use
    */

    public void setDocumentLocator (final Locator locator) {
        // If the user has called setSystemId(), we use that system ID in preference to this one,
        // which probably comes from the XML parser possibly via some chain of SAX filters
        if (systemId == null) {
            super.setDocumentLocator(locator);
        }
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
