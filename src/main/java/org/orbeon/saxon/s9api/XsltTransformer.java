package org.orbeon.saxon.s9api;

import org.orbeon.saxon.Controller;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.event.Receiver;
import org.orbeon.saxon.event.TransformerReceiver;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.om.ValueRepresentation;
import org.orbeon.saxon.trans.XPathException;

import javax.xml.transform.TransformerException;
import javax.xml.transform.Source;

/**
 * An <code>XsltTransformer</code> represents a compiled and loaded stylesheet ready for execution.
 * The <code>XsltTransformer</code> holds details of the dynamic evaluation context for the stylesheet.
 *
 * <p>An <code>XsltTransformer</code> must not be used concurrently in multiple threads.
 * It is safe, however, to reuse the object within a single thread to run the same
 * stylesheet several times. Running the stylesheet does not change the context
 * that has been established.</p>
 *
 * <p>An <code>XsltTransformer</code> is always constructed by running the <code>Load</code>
 * method of an {@link XsltExecutable}.</p>
 *
 * <p>An <code>XsltTransformer</code> is itself a <code>Destination</code>. This means it is possible to use
 * one <code>XsltTransformer</code> as the destination to receive the results of another transformation,
 * this providing a simple way for transformations to be chained into a pipeline.</p>
 * @since 9.0
 */
public class XsltTransformer implements Destination {

    private Processor processor;
    private Controller controller;
    private NodeInfo initialContextNode;
    private Destination destination;

    /**
     * Protected constructor
     * @param processor the S9API processor
     * @param controller the Saxon controller object
     */

    protected XsltTransformer(Processor processor, Controller controller) {
        this.processor = processor;
        this.controller = controller;
    }

    /**
     * Set the initial named template for the transformation
     * @param templateName the name of the initial template, or null to indicate
     * that there should be no initial named template
     * @throws SaxonApiException if there is no named template with this name
     */

    public void setInitialTemplate(QName templateName) throws SaxonApiException {
        try {
            controller.setInitialTemplate(
                    templateName == null ? null : templateName.getClarkName());
        } catch (XPathException e) {
            throw new SaxonApiException(e);
        }
    }

    /**
     * Get the initial named template for the transformation, if one has been set
     * @return the name of the initial template, or null if none has been set
     */

    public QName getInitialTemplate() {
        String template = controller.getInitialTemplate();
        return (template == null ? null : QName.fromClarkName(template));
    }

    /**
     * Set the initial mode for the transformation
     * @param modeName the name of the initial mode, or null to indicate the default
     * (unnamed) mode
     */

    public void setInitialMode(QName modeName) {
        controller.setInitialMode(modeName == null ? null : modeName.getClarkName());
    }

    /**
     * Get the initial mode for the transformation, if one has been set.
     * @return the initial mode for the transformation. Returns null if no mode has been set,
     * or if the mode was set to null to represent the default (unnamed) mode
     */

    public QName getInitialMode() {
        String mode = controller.getInitialMode();
        if (mode == null) {
            return null;
        } else {
            return QName.fromClarkName(mode);
        }
    }

    /**
     * Set the source document for the transformation. This method is equivalent to building
     * a document from the supplied source object, and then supplying the document node of
     * the resulting document as the initial context node.
     * @param source the principal source document for the transformation
     */

    public void setSource(Source source) throws SaxonApiException {
        setInitialContextNode(processor.newDocumentBuilder().build(source));
    }

    /**
     * Set the initial context node for the transformation.
     * <p>This is ignored in the case where the {@link XsltTransformer} is used as the
     * {@link Destination} of another process. In that case the initial context node will always
     * be the document node of the document that is being streamed to this destination.</p>
     * @param node the initial context node, or null if there is to be no initial context node
     */

    public void setInitialContextNode(XdmNode node) {
        initialContextNode = (node == null ? null : node.getUnderlyingNode());
    }

    /**
     * Get the initial context node for the transformation, if one has been set
     * @return the initial context node, or null if none has been set. This will not necessarily
     * be the same {@link XdmNode} instance as was supplied, but it will be an XdmNode object that represents
     * the same underlying node.
     */

    public XdmNode getInitialContextNode() {
        return (XdmNode)XdmValue.wrap(initialContextNode);
    }

    /**
     * Set the value of a stylesheet parameter
     * @param name the name of the stylesheet parameter, as a QName
     * @param value the value of the stylesheet parameter, or null to clear a previously set value
     */

    public void setParameter(QName name, XdmValue value) {
        controller.setParameter(name.getStructuredQName(),
                (value == null ? null : value.getUnderlyingValue()));
    }

    /**
     * Get the value that has been set for a stylesheet parameter
     * @param name the parameter whose name is required
     * @return the value that has been set for the parameter, or null if no value has been set
     */

    public XdmValue getParameter(QName name) {
        Object oval = controller.getParameter(name.getClarkName());
        if (oval == null) {
            return null;
        }
        if (oval instanceof ValueRepresentation) {
            return XdmValue.wrap((ValueRepresentation)oval);
        }
        throw new IllegalStateException(oval.getClass().getName());
    }

    /**
     * Set the destination to be used for the transformation.
     * <p>This method can be used to chain transformations into a pipeline, by using one
     * {@link XsltTransformer} as the destination of another</p>
     * @param destination the destination to be used
     */

    public void setDestination(Destination destination) {
        this.destination = destination;
    }

    /**
     * Get the destination that was specified in a previous call of {@link #setDestination}
     * @return the destination, or null if none has been supplied
     */

    public Destination getDestination() {
        return destination;
    }

    /**
     * Set the MessageListener to be notified whenever the stylesheet evaluates an
     * <code>xsl:message</code> instruction.  If no MessageListener is nominated,
     * the output of <code>xsl:message</code> instructions will be serialized and sent
     * to the standard error stream.
     * @param listener the MessageListener to be used
     * @since 9.1
     */

    public void setMessageListener(MessageListener listener) {
        controller.setMessageEmitter(new MessageListenerProxy(listener));
    }

    /**
     * Get the MessageListener to be notified whenever the stylesheet evaluates an
     * <code>xsl:message</code> instruction. If no MessageListener has been nominated,
     * return null
     * @return the user-supplied MessageListener, or null if none has been supplied
     * @since 9.1
     */

    public MessageListener getMessageListener() {
        Receiver r = controller.getMessageEmitter();
        if (r instanceof MessageListenerProxy) {
            return ((MessageListenerProxy)r).getMessageListener();
        } else {
            return null;
        }
    }

    /**
     * Perform the transformation. If this method is used, a destination must have been supplied
     * previously
     * @throws SaxonApiException if any dynamic error occurs during the transformation
     * @throws IllegalStateException if no destination has been supplied
     */

    public void transform() throws SaxonApiException {
        if (destination == null) {
            throw new IllegalStateException("No destination has been supplied");
        }
        try {
            Receiver receiver;
            if (destination instanceof Serializer) {
                receiver = ((Serializer)destination).getReceiver(
                        controller.getConfiguration(), controller, controller.getOutputProperties());
            } else {
                receiver = destination.getReceiver(controller.getConfiguration());
            }
            controller.transform(initialContextNode, receiver);
        } catch (TransformerException e) {
            throw new SaxonApiException(e);
        }
    }

   /**
     * Return a Receiver which can be used to supply the principal source document for the transformation.
     * This method is intended primarily for internal use, though it can also
     * be called by a user application that wishes to feed events into the transformation engine.
     *
     * <p>Saxon calls this method to obtain a Receiver, to which it then sends
     * a sequence of events representing the content of an XML document. This method is provided so that
     * <code>XsltTransformer</code> implements <code>Destination</code>, allowing one transformation
     * to receive the results of another in a pipeline.</p>
     *
     * <p>Before calling this method, the {@link #setDestination} method must be called to supply a destination
     * for the transformation.</p>
     *
     * <p>Note that when an <code>XsltTransformer</code> is used as a <code>Destination</code>, the initial
     * context node set on that <code>XsltTransformer</code> is ignored.</p>
     *
     * @param config The Saxon configuration. This is supplied so that the destination can
     *               use information from the configuration (for example, a reference to the name pool)
     *               to construct or configure the returned Receiver.
     * @return the Receiver to which events are to be sent.
     * @throws SaxonApiException if the Receiver cannot be created
     * @throws IllegalStateException if no Destination has been supplied
     */

    public Receiver getReceiver(Configuration config) throws SaxonApiException {
        if (destination == null) {
            throw new IllegalStateException("No destination has been supplied");
        }
        TransformerReceiver tr = new TransformerReceiver(controller);
        tr.setResult(destination.getReceiver(config));
        tr.setPipelineConfiguration(controller.makePipelineConfiguration());
        return tr;
    }

    /**
     * Get the underlying Controller used to implement this XsltTransformer. This provides access
     * to lower-level methods not otherwise available in the s9api interface. Note that classes
     * and methods obtained by this route cannot be guaranteed stable from release to release.
     * @since 9.0.0.4
     */

    public Controller getUnderlyingController() {
        return controller;
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

