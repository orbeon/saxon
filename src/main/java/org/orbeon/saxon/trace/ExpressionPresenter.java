package org.orbeon.saxon.trace;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.event.Receiver;
import org.orbeon.saxon.event.SaxonOutputKeys;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.om.StandardNames;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.TypeHierarchy;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.stream.StreamResult;
import java.io.OutputStream;
import java.util.Properties;

/**
 * This class handles the display of an abstract expression tree in an XML format
 * with some slight resemblence to XQueryX
 */
public class ExpressionPresenter {

    private Configuration config;
    private Receiver receiver;
    int depth = 0;

    /**
     * Make an ExpressionPresenter that writes indented output to System.err
     * @param config the Saxon configuration
     */

    public ExpressionPresenter(Configuration config) {
        this(config, System.err);
    }

    /**
     * Make an ExpressionPresenter that writes indented output to a specified output stream
     * @param config the Saxon configuration
     * @param out the output stream
     */

    public ExpressionPresenter(Configuration config, OutputStream out) {
        Properties props = makeDefaultProperties();
        try {
            receiver = config.getSerializerFactory().getReceiver(
                            new StreamResult(out),
                            config.makePipelineConfiguration(),
                            props);
        } catch (XPathException err) {
            err.printStackTrace();
            throw new InternalError(err.getMessage());
        }
        this.config = config;
        try {
            receiver.open();
            receiver.startDocument(0);
        } catch (XPathException err) {
            err.printStackTrace();
            throw new InternalError(err.getMessage());
        }
    }

    /**
     * Make an ExpressionPresenter for a given Configuration using a user-supplied Receiver
     * to accept the output
     * @param config the Configuration
     * @param receiver the user-supplied Receiver
     */

    public ExpressionPresenter(Configuration config, Receiver receiver) {
        this.config = config;
        this.receiver = receiver;
        try {
            receiver.open();
            receiver.startDocument(0);
        } catch (XPathException err) {
            err.printStackTrace();
            throw new InternalError(err.getMessage());
        }
    }

    /**
     * Make a receiver, using default output properties, with serialized output going
     * to a specified OutputStream
     * @param config the Configuration
     * @param out the OutputStream
     * @return a Receiver that directs serialized output to this output stream
     * @throws XPathException
     */

    public static Receiver defaultDestination(Configuration config, OutputStream out) throws XPathException {
        Properties props = makeDefaultProperties();
        return config.getSerializerFactory().getReceiver(
                        new StreamResult(out),
                        config.makePipelineConfiguration(),
                        props);
    }


    /**
     * Make a Properties object containing defaulted serialization attributes for the expression tree
     * @return a default set of properties
     */

    private static Properties makeDefaultProperties() {
        Properties props = new Properties();
        props.setProperty(OutputKeys.METHOD, "xml");
        props.setProperty(OutputKeys.INDENT, "yes");
        props.setProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        props.setProperty(SaxonOutputKeys.INDENT_SPACES, "2");
        return props;
    }

    /**
     * Start an element
     * @param name the name of the element
     * @return the depth of the tree before this element: for diagnostics, this can be compared
     * with the value returned by endElement
     */

    public int startElement(String name) {
        try {
            receiver.startElement(config.getNamePool().allocate("", "", name), StandardNames.XS_UNTYPED, 0, 0);
        } catch (XPathException err) {
            err.printStackTrace();
            throw new InternalError(err.getMessage());
        }
        return depth++;
    }

    /**
     * Output an attribute node
     * @param name the name of the attribute
     * @param value the value of the attribute
     */

    public void emitAttribute(String name, String value) {
        try {
            receiver.attribute(config.getNamePool().allocate("", "", name), StandardNames.XS_UNTYPED, value, 0, 0);
        } catch (XPathException err) {
            err.printStackTrace();
            throw new InternalError(err.getMessage());
        }
    }

    /**
     * End an element in the expression tree
     * @return the depth of the tree after ending this element. For diagnostics, this can be compared with the
     * value returned by startElement()
     */

    public int endElement() {
        try {
            receiver.endElement();
        } catch (XPathException err) {
            err.printStackTrace();
            throw new InternalError(err.getMessage());
        }
        return --depth;
    }

    /**
     * Start a child element in the output
     * @param name the name of the child element
     */

    public void startSubsidiaryElement(String name) {
        startElement(name);
    }

    /**
     * End a child element in the output
     */

    public void endSubsidiaryElement() {
        endElement();
    }

    /**
     * Close the output
     */

    public void close() {
        try {
            receiver.endDocument();
            receiver.close();
        } catch (XPathException err) {
            err.printStackTrace();
            throw new InternalError(err.getMessage());
        }
    }

    /**
     * Get the Saxon configuration
     * @return the Saxon configuration
     */

    public Configuration getConfiguration() {
        return config;
    }

    /**
     * Get the name pool
     * @return the name pool
     */

    public NamePool getNamePool() {
        return config.getNamePool();
    }

    /**
     * Get the type hierarchy cache
     * @return the type hierarchy cache
     */

    public TypeHierarchy getTypeHierarchy() {
        return config.getTypeHierarchy();
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

