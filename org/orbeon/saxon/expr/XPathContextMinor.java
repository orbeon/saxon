package net.sf.saxon.expr;
import net.sf.saxon.Controller;
import net.sf.saxon.event.*;
import net.sf.saxon.instruct.LocalParam;
import net.sf.saxon.instruct.ParameterSet;
import net.sf.saxon.instruct.RegexIterator;
import net.sf.saxon.instruct.Template;
import net.sf.saxon.om.*;
import net.sf.saxon.sort.CodepointCollator;
import net.sf.saxon.sort.CollationFactory;
import net.sf.saxon.sort.GroupIterator;
import net.sf.saxon.trace.InstructionInfoProvider;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.Mode;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.SchemaType;

import javax.xml.transform.Result;
import javax.xml.transform.TransformerException;
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
     * {@link net.sf.saxon.trace.Location}
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

    public Controller getController() {
        return controller;
    }

    /**
     * Get the calling XPathContext (the next one down the stack). This will be null if unknown, or
     * if the bottom of the stack has been reached.
     */

    public XPathContext getCaller() {
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

    public SequenceIterator getCurrentIterator() {
        return currentIterator;
    }

    /**
     * Get the context position (the position of the context item)
     * @return the context position (starting at one)
     * @throws DynamicError if the context position is undefined
    */

    public int getContextPosition() throws DynamicError {
        if (currentIterator==null) {
            DynamicError e = new DynamicError("The context position is currently undefined");
            e.setXPathContext(this);
            e.setErrorCode("XP0002");
            throw e;
        }
        return currentIterator.position();
    }

    /**
    * Get the context item
     * @return the context item, or null if the context item is undefined
    */

    public Item getContextItem() {
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

    public int getLast() throws XPathException {
        if (last>0) return last;
        if (currentIterator==null) {
            DynamicError e = new DynamicError("The context size is currently undefined");
            e.setXPathContext(this);
            e.setErrorCode("XP0002");
            throw e;
        }
        if (currentIterator instanceof LastPositionFinder) {
            last = ((LastPositionFinder)currentIterator).getLastPosition();
            return last;
        } else {
            SequenceIterator another = currentIterator.getAnother();
            last = 0;
            while (another.next() != null) {
                last++;
            }
            return last;
        }
    }

    /**
    * Determine whether the context position is the same as the context size
    * that is, whether position()=last()
    */

    public boolean isAtLast() throws XPathException {
        if (currentIterator instanceof LookaheadIterator) {
            return !((LookaheadIterator)currentIterator).hasNext();
            // TODO: reinstate this optimization for more types of iterator!
        }
        return getContextPosition() == getLast();
    }

    /**
    * Get a named collation
    */

    public Comparator getCollation(String name) throws XPathException {
        if (name.equals(NamespaceConstant.CodepointCollationURI)) {
            return CodepointCollator.getInstance();
        }
        Comparator collation = null;
        if (controller != null) {
            collation = controller.getExecutable().getNamedCollation(name);
        }
        if (collation == null) {
            collation = CollationFactory.makeCollationFromURI(name);
            if (collation==null) {
                DynamicError e = new DynamicError("Unknown collation " + name);
                e.setXPathContext(this);
                throw e;
            }
        }
        return collation;
    }

    /**
    * Get the default collation
    */

    public Comparator getDefaultCollation() {
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
        return getCaller().getStackFrame();
    }


    /**
     * Get the value of a local variable, identified by its slot number
     */

    public ValueRepresentation evaluateLocalVariable(int slotnumber) {
        return getCaller().evaluateLocalVariable(slotnumber);
    }

    /**
     * Set the value of a local variable, identified by its slot number
     */

    public void setLocalVariable(int slotnumber, ValueRepresentation value) {
        getCaller().setLocalVariable(slotnumber, value);
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
     *     it is a temporary tree, xsl:attribute, etc.
     */

    public void changeOutputDestination(Properties props,
                                        Result result,
                                        boolean isFinal,
                                        int validation,
                                        SchemaType schemaType)
    throws XPathException {
        if (isFinal && isTemporaryDestination) {
            DynamicError err = new DynamicError("Cannot switch to a final result destination while writing a temporary tree");
            err.setErrorCode("XT1480");
            throw err;
        }
        if (isFinal) {
            validation |= Validation.VALIDATE_OUTPUT;
        } else {
            isTemporaryDestination = true;
        }
        PipelineConfiguration pipe = controller.makePipelineConfiguration();
        ComplexContentOutputter out = new ComplexContentOutputter();
        out.setPipelineConfiguration(pipe);

        if (props == null) {
            props = new Properties();
        }


        Receiver receiver = ResultWrapper.getReceiver(
                                            result,
                                            pipe,
                                            props);

        // add a validator to the pipeline if required

        if (schemaType != null) {
            try {
                getController().getErrorListener().warning(
                        new DynamicError("Type attribute for result document is currently ignored"));
                // TODO: implement this
            } catch (TransformerException err) {}
        }

        receiver = controller.getConfiguration().getDocumentValidator(
                receiver, receiver.getSystemId(), controller.getNamePool(), validation
        );
        //receiver.getPipelineConfiguration().setLocationProvider(locationMap);

		// add a filter to remove duplicate namespaces

		NamespaceReducer ne = new NamespaceReducer();
		ne.setUnderlyingReceiver(receiver);
		ne.setPipelineConfiguration(pipe);
		out.setReceiver(ne);

        out.open();
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
    public SequenceReceiver getReceiver() {
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

    public Template getCurrentTemplate() {
        return getCaller().getCurrentTemplate();
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
