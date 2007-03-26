package org.orbeon.saxon;

import org.orbeon.saxon.event.CommentStripper;
import org.orbeon.saxon.event.PipelineConfiguration;
import org.orbeon.saxon.event.Sender;
import org.orbeon.saxon.event.StartTagBuffer;
import org.orbeon.saxon.instruct.Executable;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.om.Validation;
import org.orbeon.saxon.style.*;
import org.orbeon.saxon.trans.StaticError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.trans.CompilerInfo;
import org.orbeon.saxon.tree.DocumentImpl;
import org.orbeon.saxon.tree.TreeBuilder;
import org.xml.sax.XMLReader;

import javax.xml.transform.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Properties;
import java.util.HashMap;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * This <B>PreparedStylesheet</B> class represents a Stylesheet that has been
 * prepared for execution (or "compiled").
 */

public class PreparedStylesheet implements Templates, Serializable {

    private Executable executable;
    private transient Configuration config;
    private NamePool targetNamePool;    // the namepool used when the stylesheet was compiled,
                                        // saved here so it can be used again when the stylesheet is run
    private transient StyleNodeFactory nodeFactory;
    private int errorCount = 0;
    private HashMap nextStylesheetCache;    // cache for stylesheets named as "saxon:next-in-chain"

    private ErrorListener errorListener;
    private URIResolver uriResolver;

    /**
     * Constructor: deliberately protected
     *
     * @param config The Configuration set up by the TransformerFactory
     */

    protected PreparedStylesheet(Configuration config, CompilerInfo info) {
        this.config = config;
        this.errorListener = info.getErrorListener();
        this.uriResolver = info.getURIResolver();
    }

    /**
     * Make a Transformer from this Templates object.
     *
     * @return the new Transformer (always a Controller)
     * @see org.orbeon.saxon.Controller
     */

    public Transformer newTransformer() {
        Controller c = new Controller(config, executable);
        c.setPreparedStylesheet(this);
        return c;
    }

    public void setConfiguration(Configuration config) {
        this.config = config;
    }

    public Configuration getConfiguration() {
        return config;
    }

    /**
     * Set the name pool
     */

    public void setTargetNamePool(NamePool pool) {
        targetNamePool = pool;
    }

	/**
	 * Get the name pool in use. This is the namepool used for names that need to be accessible
	 * at runtime, notably the names used in XPath expressions in the stylesheet.
	 *
	 * @return the name pool in use
	 */

	public NamePool getTargetNamePool() {
        if (targetNamePool==null) {
		    return config.getNamePool();
        } else {
            return targetNamePool;
        }
	}

	/**
	 * Get the StyleNodeFactory in use. The StyleNodeFactory determines which subclass of StyleElement
	 * to use for each element node in the stylesheet tree.
	 *
	 * @return the StyleNodeFactory
	 */

	public StyleNodeFactory getStyleNodeFactory() {
		return nodeFactory;
	}

    /**
     * Prepare a stylesheet from a Source document
     *
     * @param styleSource the source document containing the stylesheet
     * @exception TransformerConfigurationException if compilation of the
     *     stylesheet fails for any reason
     */

    protected void prepare(Source styleSource) throws TransformerConfigurationException {
        nodeFactory = new StyleNodeFactory(config, errorListener);
        DocumentImpl doc;
        try {
            doc = loadStylesheetModule(styleSource, config, config.getNamePool(), nodeFactory);
            setStylesheetDocument(doc, nodeFactory);
        } catch (XPathException e) {
            try {
                errorListener.fatalError(e);
            } catch (TransformerException e2) {
                // ignore an exception thrown by the error handler
            }
            if (errorCount==0) {
                errorCount++;
            }
        }

        if (errorCount > 0) {
            throw new TransformerConfigurationException(
                            "Failed to compile stylesheet. " +
                            errorCount +
                            (errorCount==1 ? " error " : " errors ") +
                            "detected.");
        }
    }

    /**
     * Build the tree representation of a stylesheet module
     *
     * @param styleSource the source of the module
     * @param config the Configuration of the transformation factory
     * @param localNamePool the namepool used during compilation
     * @param nodeFactory the StyleNodeFactory used for creating
     *     element nodes in the tree
     * @exception XPathException if XML parsing or tree
     *     construction fails
     * @return the root Document node of the tree containing the stylesheet
     *     module
     */
    public static DocumentImpl loadStylesheetModule(
                                    Source styleSource,
                                    Configuration config,
                                    NamePool localNamePool,
                                    StyleNodeFactory nodeFactory)
    throws XPathException {

        TreeBuilder styleBuilder = new TreeBuilder();
        PipelineConfiguration pipe = config.makePipelineConfiguration();
        styleBuilder.setPipelineConfiguration(pipe);
        styleBuilder.setSystemId(styleSource.getSystemId());
        styleBuilder.setNodeFactory(nodeFactory);
        styleBuilder.setLineNumbering(true);

        StartTagBuffer startTagBuffer = new StartTagBuffer();

        UseWhenFilter useWhenFilter = new UseWhenFilter(startTagBuffer);
        useWhenFilter.setUnderlyingReceiver(styleBuilder);

        startTagBuffer.setUnderlyingReceiver(useWhenFilter);

        StylesheetStripper styleStripper = new StylesheetStripper();
        styleStripper.setStylesheetRules(localNamePool);
        styleStripper.setUnderlyingReceiver(startTagBuffer);

        CommentStripper commentStripper = new CommentStripper();
        commentStripper.setUnderlyingReceiver(styleStripper);

        // build the stylesheet document

        DocumentImpl doc;

        Sender sender = new Sender(pipe);
        AugmentedSource aug = AugmentedSource.makeAugmentedSource(styleSource);
        aug.setSchemaValidationMode(Validation.STRIP);
        aug.setDTDValidationMode(Validation.STRIP);
        aug.setLineNumbering(true);
        if (aug.getXMLReader() == null && config.getPlatform() instanceof JavaPlatform) {
            XMLReader styleParser = config.getStyleParser();
            aug.setXMLReader(styleParser);
            sender.send(aug, commentStripper);
            config.reuseStyleParser(styleParser);
        } else {
            sender.send(aug, commentStripper);
        }
        doc = (DocumentImpl)styleBuilder.getCurrentRoot();

        if (aug.isPleaseCloseAfterUse()) {
            aug.close();
        }

        return doc;

    }

    /**
     * Load a PreparedStylesheet from a compiled stylesheet stored in a file.
     * @param config The Configuration. <b>This method changes the NamePool used by this configuration
     * to be the NamePool that was stored with the compiled stylesheet. The method must therefore not
     * be used in a multi-threaded environment where the Configuration (and NamePool) are shared between
     * multiple concurrent transformations.</b>
     * @param fileName The name of the file containing the compiled stylesheet (which is just the Java serialization
     * of a PreparedStylesheet object).
     * @return the PreparedStylesheet, which can be used in JAXP interfaces as the Templates object
     */

    public static PreparedStylesheet loadCompiledStylesheet(Configuration config, String fileName)
            throws IOException, ClassNotFoundException {
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fileName));
        return loadCompiledStylesheet(config, ois);
    }

    /**
     * Load a PreparedStylesheet from a compiled stylesheet stored in a file.
     * @param config The Configuration. <b>This method changes the NamePool used by this configuration
     * to be the NamePool that was stored with the compiled stylesheet. The method must therefore not
     * be used in a multi-threaded environment where the Configuration (and NamePool) are shared between
     * multiple concurrent transformations.</b>
     * @param ois The ObjectInputStream containing the compiled stylesheet (which is just the Java serialization
     * of a PreparedStylesheet object).
     * @return the PreparedStylesheet, which can be used in JAXP interfaces as the Templates object
     */

    public static PreparedStylesheet loadCompiledStylesheet(Configuration config, ObjectInputStream ois)
            throws IOException, ClassNotFoundException {
        PreparedStylesheet sheet = (PreparedStylesheet)ois.readObject();
        ois.close();
        NamePool compiledNamePool = sheet.getTargetNamePool();
        sheet.setConfiguration(config);
        sheet.getExecutable().setConfiguration(config);
        config.setNamePool(compiledNamePool);
        NamePool.setDefaultNamePool(compiledNamePool);
        return sheet;
    }

    /**
     * Create a PreparedStylesheet from a supplied DocumentInfo
     * Note: the document must have been built using the StyleNodeFactory
     *
     * @param doc the document containing the stylesheet module
     * @param snFactory the StyleNodeFactory used to build the tree
     * @exception XPathException if the document supplied
     *     is not a stylesheet
     */

    protected void setStylesheetDocument(DocumentImpl doc, StyleNodeFactory snFactory)
    throws XPathException {

        DocumentImpl styleDoc = doc;
		nodeFactory = snFactory;

        // If top-level node is a literal result element, stitch it into a skeleton stylesheet

        StyleElement topnode = (StyleElement)styleDoc.getDocumentElement();
        if (topnode instanceof LiteralResultElement) {
            styleDoc = ((LiteralResultElement)topnode).makeStylesheet(this, snFactory);
        }

        if (!(styleDoc.getDocumentElement() instanceof XSLStylesheet)) {
            throw new StaticError(
                        "Outermost element of stylesheet is not xsl:stylesheet or xsl:transform or literal result element");
        }

        XSLStylesheet top = (XSLStylesheet)styleDoc.getDocumentElement();
        if (config.isVersionWarning() && top.getVersion().equals(BigDecimal.valueOf(1))) {
            try {
                TransformerException w = new TransformerException(
                        "Running an XSLT 1.0 stylesheet with an XSLT 2.0 processor");
                w.setLocator(topnode);
                config.getErrorListener().warning(w);
            } catch (TransformerException e) {
                throw StaticError.makeStaticError(e);
            }
        }

        // Preprocess the stylesheet, performing validation and preparing template definitions

        top.setPreparedStylesheet(this);
        try {
            top.preprocess();
        } catch (XPathException e) {
            Throwable e2 = e.getException();
            if (e2 instanceof XPathException) {
                try {
                    errorListener.fatalError((XPathException)e2);
                } catch (TransformerException e3) {
                    // ignore an error thrown by the ErrorListener
                }
            }
            throw e;
        }

        // Compile the stylesheet, retaining the resulting executable

        executable = top.compileStylesheet();
    }

    /**
     * Get the associated executable
     *
     * @return the Executable for this stylesheet
     */

    public Executable getExecutable() {
        return executable;
    }

    /**
     * Get the properties for xsl:output.  JAXP method. The object returned will
     * be a clone of the internal values, and thus it can be mutated
     * without mutating the Templates object, and then handed in to
     * the process method.
     * <p>In Saxon, the properties object is a new, empty, Properties object that is
     * backed by the live properties to supply default values for missing properties.
     * This means that the property values must be read using the getProperty() method.
     * Calling the get() method on the underlying Hashtable will return null.</p>
     * <p>In Saxon 8.x, this method gets the output properties for the unnamed output
     * format in the stylesheet.</p>
     *
     * @see javax.xml.transform.Transformer#setOutputProperties
     * @return A Properties object reflecting the output properties defined
     *     for the default (unnamed) output format in the stylesheet. It may
     *     be mutated and supplied to the setOutputProperties() method of the
     *     Transformer, without affecting other transformations that use the
     *     same stylesheet.
     */


    public Properties getOutputProperties() {
        Properties details = executable.getDefaultOutputProperties();
        return new Properties(details);
    }

    /**
     * Report a compile time error. This calls the errorListener to output details
     * of the error, and increments an error count.
     *
     * @param err the exception containing details of the error
     * @exception TransformerException if the ErrorListener decides that the
     *     error should be reported
     */

    public void reportError(TransformerException err) throws TransformerException {
        errorCount++;
        if (err instanceof XPathException) {
            if (!((XPathException)err).hasBeenReported()) {
                try {
                    errorListener.fatalError(err);
                } catch (Exception err2) {
                    // ignore secondary error
                }
                ((XPathException)err).setHasBeenReported();
            }
        } else {
            errorListener.fatalError(err);
        }
    }

    /**
     * Get the number of errors reported so far
     *
     * @return the number of errors reported
     */

    public int getErrorCount() {
        return errorCount;
    }

    /**
     * Report a compile time warning. This calls the errorListener to output details
     * of the warning.
     *
     * @param err an exception holding details of the warning condition to be
     *     reported
     */

    public void reportWarning(TransformerException err) {
        try {
            errorListener.warning(err);
        } catch (TransformerException err2) {}
    }

    /**
     * Get a "next in chain" stylesheet
     */

    public PreparedStylesheet getCachedStylesheet(String href, String baseURI) {
        URI abs = null;
        try {
            abs = new URI(baseURI).resolve(href);
        } catch (URISyntaxException err) {
            //
        }
        PreparedStylesheet result = null;
        if (abs != null && nextStylesheetCache != null) {
            result = (PreparedStylesheet)nextStylesheetCache.get(abs);
        }
        return result;
    }

    public void putCachedStylesheet(String href, String baseURI, PreparedStylesheet pss) {
        URI abs = null;
        try {
            abs = new URI(baseURI).resolve(href);
        } catch (URISyntaxException err) {
            //
        }
        if (abs != null) {
            if (nextStylesheetCache == null) {
                nextStylesheetCache = new HashMap(4);
            }
            nextStylesheetCache.put(abs, pss);
        }
    }
    public URIResolver getURIResolver() {
        return uriResolver;
    }

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
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
