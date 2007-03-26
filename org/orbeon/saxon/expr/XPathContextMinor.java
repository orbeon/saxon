package org.orbeon.saxon.expr;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.Controller;
import org.orbeon.saxon.event.*;
import org.orbeon.saxon.instruct.LocalParam;
import org.orbeon.saxon.instruct.ParameterSet;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.regex.RegexIterator;
import org.orbeon.saxon.sort.CodepointCollator;
import org.orbeon.saxon.sort.GroupIterator;
import org.orbeon.saxon.trace.InstructionInfoProvider;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.Mode;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.trans.Rule;
import org.orbeon.saxon.type.SchemaType;
import org.orbeon.saxon.value.Whitespace;

import javax.xml.transform.Result;
import java.util.Comparator;
import java.util.Properties;

/**
 * This class represents a minor change in the dynamic context in which an XPath expression is evaluated:
 * a "major context" object allows all aspects of the dynamic context to change, whereas
 * a "minor context" only allows changes to the focus and the destination for push output.
*/

public class XPathContextMinor implements XPathContext {

    Controller controller;
    SequenceIterator currentIterator;
    int last = -1;
    SequenceReceiver currentReceiver;
    boolean isTemporaryDestination = false;
    XPathContext caller = null;
    protected StackFrame stackFrame;
    Object origin = null;

    /**
    * Private Constructor
    */

    protected XPathContextMinor() {
    }

    /**
    * Construct a new context as a copy of another. The new context is effectively added
    * to the top of a stack, and contains a pointer to the previous context
    */

    public XPathContextMajor newContext() {
        return XPathContextMajor.newContext(this);
    }

    public XPathContextMinor newMinorContext() {
        XPathContextMinor c = new XPathContextMinor();
        c.controller = controller;
        c.caller = this;
        c.currentIterator = currentIterator;
        c.currentReceiver = currentReceiver;
        c.last = last;
        c.isTemporaryDestination = isTemporaryDestination;
        c.stackFrame = stackFrame;
        return c;
    }

    /**
     * Set the calling XPathContext
     */

    public void setCaller(XPathContext caller) {
        this.caller = caller;
    }

    /**
    * Construct a new context without copying (used for the context in a function call)
    */

    public XPathContextMajor newCleanContext() {
        XPathContextMajor c = new XPathContextMajor(this.getController());
        c.setCaller(this);
        return c;
    }

    /**
     * Get the XSLT-specific part of the context
     */

    public XPathContextMajor.XSLTContext getXSLTContext() {
        return getCaller().getXSLTContext();
    }

    /**
     * Get the local parameters for the current template call.
     * @return the supplied parameters
     */

    public ParameterSet getLocalParameters() {
        return getCaller().getLocalParameters();
    }

    /**
     * Get the tunnel parameters for the current template call.
     * @return the supplied tunnel parameters
     */

    public ParameterSet getTunnelParameters() {
        return getCaller().getTunnelParameters();
    }

    /**
     * Set the creating expression (for use in diagnostics). The origin is generally set to "this" by the
     * object that creates the new context. It's up to the debugger to determine whether this information
     * is useful. Where possible, the object will be an {@link InstructionInfoProvider}, allowing information
     * about the calling instruction to be obtained.
     */

    public void setOrigin(InstructionInfoProvider expr) {
        origin = expr;
    }

    /**
     * Set the type of creating expression (for use in diagnostics). When a new context is created, either
     * this method or {@link #setOrigin} should be called.
     * @param loc The originating location: the argument must be one of the integer constants in class
     * {@link org.orbeon.saxon.trace.Location}
     */

    public void setOriginatingConstructType(int loc) {
        origin = new Integer(loc);
    }

    /**
     * Get the type of location from which this context was created.
     */

    public int getOriginatingConstructType() {
        if (origin instanceof InstructionInfoProvider) {
            return ((InstructionInfoProvider)origin).getInstructionInfo().getConstructType();
        } else {
            return ((Integer)origin).intValue();
        }
    }

    /**
     * Get information about the creating expression or other construct.
     */

    public InstructionInfoProvider getOrigin() {
        if (origin instanceof InstructionInfoProvider) {
            return (InstructionInfoProvider)origin;
        } else {
            return null;
        }
    }

    /**
    * Get the Controller. May return null when running outside XSLT or XQuery
    */

    public final Controller getController() {
        return controller;
    }

    /**
     * Get the Configuration
     */

    public final Configuration getConfiguration() {
        return controller.getConfiguration();
    }

    /**
     * Get the Name Pool
     */

    public final NamePool getNamePool() {
        return controller.getNamePool();
    }

    /**
     * Get a NameChecker for checking names against the XML 1.0 or XML 1.1 specification as appropriate
     */

    public final NameChecker getNameChecker() {
        return controller.getConfiguration().getNameChecker();
    }

    /**
     * Get the calling XPathContext (the next one down the stack). This will be null if unknown, or
     * if the bottom of the stack has been reached.
     */

    public final XPathContext getCaller() {
        return caller;
    }

    /**
    * Set a new sequence iterator.
    */

    public void setCurrentIterator(SequenceIterator iter) {
        currentIterator = iter;
        last = 0;
    }

    /**
     * Get the current iterator.
     * This encapsulates the context item, context position, and context size.
     * @return the current iterator, or null if there is no current iterator
     * (which means the context item, position, and size are undefined).
    */

    public final SequenceIterator getCurrentIterator() {
        return currentIterator;
    }

    /**
     * Get the context position (the position of the context item)
     * @return the context position (starting at one)
     * @throws DynamicError if the context position is undefined
    */

    public final int getContextPosition() throws DynamicError {
        if (currentIterator==null) {
            DynamicError e = new DynamicError("The context position is currently undefined");
            e.setXPathContext(this);
            e.setErrorCode("FONC0001");
            throw e;
        }
        return currentIterator.position();
    }

    /**
    * Get the context item
     * @return the context item, or null if the context item is undefined
    */

    public final Item getContextItem() {
        if (currentIterator==null) {
            return null;
        }
        return currentIterator.current();
    }

    /**
     * Get the context size (the position of the last item in the current node list)
     * @return the context size
     * @throws XPathException if the context position is undefined
     */

    public final int getLast() throws XPathException {
        if (last > 0) {
            return last;
        }
        if (currentIterator == null) {
            DynamicError e = new DynamicError("The context size is currently undefined");
            e.setXPathContext(this);
            e.setErrorCode("FONC0001");
            throw e;
        }
        if ((currentIterator.getProperties() & SequenceIterator.LAST_POSITION_FINDER) == 0) {
            SequenceIterator another = currentIterator.getAnother();
            last = 0;
            while (another.next() != null) {
                last++;
            }
            return last;
        } else {
            last = ((LastPositionFinder)currentIterator).getLastPosition();
            return last;
        }
    }

    /**
    * Determine whether the context position is the same as the context size
    * that is, whether position()=last()
    */

    public final boolean isAtLast() throws XPathException {
        if ((currentIterator.getProperties() & SequenceIterator.LOOKAHEAD) != 0) {
            return !((LookaheadIterator)currentIterator).hasNext();
        }
        return getContextPosition() == getLast();
    }

    /**
    * Get a named collation
     * @throws XPathException if the collation is not recognized
    */

    public final Comparator getCollation(String name) throws XPathException {
        if (name.equals(NamespaceConstant.CODEPOINT_COLLATION_URI)) {
            return CodepointCollator.getInstance();
        }
        Comparator collation = null;
        if (controller != null) {
            collation = controller.getExecutable().getNamedCollation(name);

            if (collation == null) {
                Configuration config = controller.getConfiguration();
                collation = config.getCollationURIResolver().resolve(name, null, config);
                //collation = CollationFactory.makeCollationFromURI(name, getController().getConfiguration());
            }
        }
        if (collation==null) {
            DynamicError e = new DynamicError("Unknown collation " + name);
            e.setErrorCode("FOCH0002"); // Caller may have to change this
            e.setXPathContext(this);
            throw e;
        }
        return collation;
    }

    /**
    * Get the default collation
    */

    public final Comparator getDefaultCollation() {
        if (controller != null) {
            return controller.getExecutable().getDefaultCollation();
        } else {
            return CodepointCollator.getInstance();
        }
    }

    /**
     * Get a reference to the local stack frame for variables. Note that it's
     * the caller's job to make a local copy of this. This is used for creating
     * a Closure containing a retained copy of the variables for delayed evaluation.
     * @return array of variables.
     */

    public StackFrame getStackFrame() {
        return stackFrame;
    }


    /**
     * Get the value of a local variable, identified by its slot number
     */

    public ValueRepresentation evaluateLocalVariable(int slotnumber) {
        return stackFrame.slots[slotnumber];
    }

    /**
     * Set the value of a local variable, identified by its slot number
     */

    public void setLocalVariable(int slotnumber, ValueRepresentation value) {
        stackFrame.slots[slotnumber] = value;
    }

    /**
     * Set a new output destination, supplying the output format details. <BR>
     * This affects all further output until resetOutputDestination() is called. Note that
     * it is the caller's responsibility to close the Writer after use.
     *
     * @exception XPathException if any dynamic error occurs; and
     *     specifically, if an attempt is made to switch to a final output
     *     destination while writing a temporary tree or sequence
     * @param props properties defining the output format
     * @param result Details of the new output destination
     * @param isFinal true if the destination is a final result tree
     *     (either the principal output or a secondary result tree); false if
     * @param hostLanguage
     */

    public void changeOutputDestination(Properties props,
                                        Result result,
                                        boolean isFinal,
                                        int hostLanguage,
                                        int validation,
                                        SchemaType schemaType)
    throws XPathException {
        if (isFinal && isTemporaryDestination) {
            DynamicError err = new DynamicError("Cannot switch to a final result destination while writing a temporary tree");
            err.setErrorCode("XTDE1480");
            throw err;
        }
        if (isFinal) {
            validation |= Validation.VALIDATE_OUTPUT;
        } else {
            isTemporaryDestination = true;
        }
        PipelineConfiguration pipe = null;
        if (result instanceof Receiver) {
            pipe = ((Receiver)result).getPipelineConfiguration();
        }
        if (pipe == null) {
            pipe = controller.makePipelineConfiguration();
            pipe.setSerializing(isFinal);
            pipe.setHostLanguage(hostLanguage);
        }
        ComplexContentOutputter out = new ComplexContentOutputter();
        out.setHostLanguage(hostLanguage);
        out.setPipelineConfiguration(pipe);

        if (props == null) {
            props = new Properties();
        }

        SerializerFactory sf = getConfiguration().getSerializerFactory();
        Receiver receiver = sf.getReceiver(result, pipe, props);

        // if this is the implicit XSLT result document, add a filter to check the first write

        // TODO: this is necessary only for a stylesheet that contains an xsl:result-document instruction

        if ("yes".equals(props.getProperty(SaxonOutputKeys.IMPLICIT_RESULT_DOCUMENT))) {
            receiver = new ImplicitResultChecker(receiver, controller);
        }

        // add a validator to the pipeline if required

        receiver = controller.getConfiguration().getDocumentValidator(
                receiver, receiver.getSystemId(), validation,
                Whitespace.NONE, schemaType);

		// add a filter to remove duplicate namespaces

		NamespaceReducer ne = new NamespaceReducer();
		ne.setUnderlyingReceiver(receiver);
		ne.setPipelineConfiguration(pipe);
		out.setReceiver(ne);

        //out.open();
        currentReceiver = out;
    }

    /**
     * Set the output destination to write to a sequence. <BR>
     * This affects all further output until resetOutputDestination() is called.
     *
     * @param out The SequenceReceiver to be used
     */

    public void setTemporaryReceiver(SequenceReceiver out) {
        isTemporaryDestination = true;
        currentReceiver = out;
    }

    /**
     * Change the Receiver to which output is written
     */

    public void setReceiver(SequenceReceiver receiver) {
        currentReceiver = receiver;
    }

    /**
     * Get the Receiver to which output is currently being written.
     *
     * @return the current Receiver
     */
    public final SequenceReceiver getReceiver() {
        return currentReceiver;
    }

    /**
    * Use local parameter. This is called when a local xsl:param element is processed.
    * If a parameter of the relevant name was supplied, it is bound to the xsl:param element.
    * Otherwise the method returns false, so the xsl:param default will be evaluated
    * @param fingerprint    The fingerprint of the parameter name
    * @param binding        The XSLParam element to bind its value to
    * @param isTunnel      True if a tunnel parameter is required, else false
    * @return true if a parameter of this name was supplied, false if not
    */

    public boolean useLocalParameter(int fingerprint,
                                     LocalParam binding,
                                     boolean isTunnel) throws XPathException {
        return getCaller().useLocalParameter(fingerprint, binding, isTunnel);
    }

    /**
     * Get the current mode.
     * @return the current mode
     */

    public Mode getCurrentMode() {
        return getCaller().getCurrentMode();
    }

    /**
     * Get the current template. This is used to support xsl:apply-imports
     *
     * @return the current template
     */

    public Rule getCurrentTemplateRule() {
        return getCaller().getCurrentTemplateRule();
    }

    /**
     * Get the current group iterator. This supports the current-group() and
     * current-grouping-key() functions in XSLT 2.0
     * @return the current grouped collection
     */

    public GroupIterator getCurrentGroupIterator() {
        return getCaller().getCurrentGroupIterator();
    }

    /**
     * Get the current regex iterator. This supports the functionality of the regex-group()
     * function in XSLT 2.0.
     * @return the current regular expressions iterator
     */

    public RegexIterator getCurrentRegexIterator() {
        return getCaller().getCurrentRegexIterator();
    }

    /**
     * Get the implicit timezone, as a positive or negative offset from UTC in minutes.
     * The range is -14hours to +14hours
     */

    public final int getImplicitTimezone() {
        return getConfiguration().getImplicitTimezone();
    }

    // TODO: eliminate this class. A new XPathContextMinor is created under two circumstances,
    // (a) when the focus changes (i.e., a new current iterator), and (b) when the current
    // receiver changes. We could handle these by maintaining a stack of iterators and a stack of
    // receivers in the XPathContextMajor object. Adding a new iterator or receiver to the stack would
    // generally be cheaper than creating the new XPathContextMinor object. The main difficulty (in the
    // case of iterators) is knowing when to pop the stack: currently we rely on the garbage collector.
    // We can only really do this when the iterator comes to its end, which is difficult to detect.
    // Perhaps we should try to do static allocation, so that fixed slots are allocated for different
    // minor-contexts within a Procedure, and a compiled expression that uses the focus knows which
    // slot to look in.
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
