package org.orbeon.saxon;
import org.orbeon.saxon.event.CommentStripper;
import org.orbeon.saxon.event.Sender;
import org.orbeon.saxon.instruct.Executable;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.style.LiteralResultElement;
import org.orbeon.saxon.style.StyleElement;
import org.orbeon.saxon.style.StyleNodeFactory;
import org.orbeon.saxon.style.StylesheetStripper;
import org.orbeon.saxon.style.XSLStylesheet;
import org.orbeon.saxon.tree.DocumentImpl;
import org.orbeon.saxon.tree.TreeBuilder;
import org.xml.sax.SAXParseException;

import javax.xml.transform.*;

import org.orbeon.saxon.xpath.XPathException;
import java.io.Serializable;
import java.util.Properties;

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

    /**
     * Constructor: deliberately protected
     *
     * @param config The Configuration set up by the TransformerFactory
     */

    protected PreparedStylesheet(Configuration config) {
        this.config = config;
    }

    /**
     * Make a Transformer from this Templates object.
     *
     * @return the new Transformer (always a Controller)
     * @see org.orbeon.saxon.Controller
     */

    public Transformer newTransformer() {
        Controller c = new Controller(config);
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
        nodeFactory = new StyleNodeFactory(config.getNamePool(), config.isAllowExternalFunctions());
        DocumentImpl doc = loadStylesheetModule(styleSource, config, config.getNamePool(), nodeFactory);
        setStylesheetDocument(doc, nodeFactory);

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
     * @exception TransformerConfigurationException if XML parsing or tree
     *     construction fails
     * @return the root Document node of the tree containing the stylesheet
     *     module
     */
    public static DocumentImpl loadStylesheetModule(
                                    Source styleSource,
                                    Configuration config,
                                    NamePool localNamePool,
                                    StyleNodeFactory nodeFactory)
    throws TransformerConfigurationException {

        StylesheetStripper styleStripper = new StylesheetStripper();
        styleStripper.setStylesheetRules(localNamePool);

        TreeBuilder styleBuilder = new TreeBuilder();
        styleBuilder.setConfiguration(config);
        styleBuilder.setSystemId(styleSource.getSystemId());
        styleBuilder.setNodeFactory(nodeFactory);
        styleBuilder.setLineNumbering(true);

        styleStripper.setUnderlyingReceiver(styleBuilder);

        CommentStripper commentStripper = new CommentStripper();
        commentStripper.setUnderlyingReceiver(styleStripper);

        // build the stylesheet document

        DocumentImpl doc;
        try {
            Sender sender = new Sender(config);
            AugmentedSource aug = AugmentedSource.makeAugmentedSource(styleSource);
            aug.setSchemaValidation(Boolean.FALSE);
            if (aug.getXMLReader() == null) {
                aug.setXMLReader(config.getStyleParser());
            }
            sender.send(aug, commentStripper);
            doc = (DocumentImpl)styleBuilder.getCurrentDocument();
        } catch (XPathException err) {
            Throwable cause = err.getException();
            if (cause != null) {
                if (cause instanceof SAXParseException) {
                    // This normally means there was an XML parsing error, in which
                    // case it has already been reported. But in the case of Crimson,
                    // the SAXParseException might wrap an exception that happened
                    // within callback code in Saxon.
                    SAXParseException spe = (SAXParseException)cause;
                    cause = spe.getException();
                    if (cause != null)  {
                        if (cause instanceof TransformerConfigurationException) {
                            throw (TransformerConfigurationException)cause;
                        } else {
                            if (cause instanceof RuntimeException) {
                                // A RunTimeException during stylesheet compilation is bad news...
                                cause.printStackTrace();
                            }
                            throw new TransformerConfigurationException(cause);
                        }
                    }
                    // details already reported, don't repeat them
                    throw new TransformerConfigurationException("Failed to parse stylesheet");
                } else if (cause instanceof TransformerConfigurationException) {
                    throw (TransformerConfigurationException)cause;
                } else {
                    throw new TransformerConfigurationException(cause);
                }
            }
            throw new TransformerConfigurationException(err);
        }

        if (doc.getDocumentElement()==null) {
            throw new TransformerConfigurationException("Stylesheet is empty or absent");
        }

        return doc;

    }


    /**
     * Create a PreparedStylesheet from a supplied DocumentInfo
     * Note: the document must have been built using the StyleNodeFactory
     *
     * @param doc the document containing the stylesheet module
     * @param snFactory the StyleNodeFactory used to build the tree
     * @exception TransformerConfigurationException if the document supplied
     *     is not a stylesheet
     */

    protected void setStylesheetDocument(DocumentImpl doc, StyleNodeFactory snFactory)
    throws TransformerConfigurationException {

        DocumentImpl styleDoc = doc;
		nodeFactory = snFactory;

//        if (targetNamePool==null) {
//        	targetNamePool = NamePool.getDefaultNamePool();
//        }

        // If top-level node is a literal result element, stitch it into a skeleton stylesheet

        StyleElement topnode = (StyleElement)styleDoc.getDocumentElement();
        if (topnode instanceof LiteralResultElement) {
            styleDoc = ((LiteralResultElement)topnode).makeStylesheet(this, snFactory);
        }

        if (!(styleDoc.getDocumentElement() instanceof XSLStylesheet)) {
            throw new TransformerConfigurationException(
                        "Outermost element of stylesheet is not xsl:stylesheet or xsl:transform or literal result element");
        }

        XSLStylesheet top = (XSLStylesheet)styleDoc.getDocumentElement();

        // Preprocess the stylesheet, performing validation and preparing template definitions

        top.setPreparedStylesheet(this);
        top.preprocess();

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
     * <p>In Saxon 7.x, this method gets the output properties for the unnamed output
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
        config.getErrorListener().fatalError(err);
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
            config.getErrorListener().warning(err);
        } catch (TransformerException err2) {}
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
