package net.sf.saxon.expr;
import net.sf.saxon.Controller;
import net.sf.saxon.event.SequenceReceiver;
import net.sf.saxon.instruct.LocalParam;
import net.sf.saxon.instruct.ParameterSet;
import net.sf.saxon.instruct.RegexIterator;
import net.sf.saxon.instruct.Template;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.om.ValueRepresentation;
import net.sf.saxon.sort.GroupIterator;
import net.sf.saxon.trace.InstructionInfoProvider;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.Mode;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.SchemaType;

import javax.xml.transform.Result;
import java.util.Comparator;
import java.util.Properties;

/**
* This class represents a context in which an XPath expression is evaluated.
*/

public interface XPathContext {


    /**
    * Construct a new context as a copy of another. The new context is effectively added
    * to the top of a stack, and contains a pointer to the previous context
    */

    public XPathContextMajor newContext();

    /**
    * Construct a new context without copying (used for the context in a function call)
    */

    public XPathContextMajor newCleanContext();

    /**
     * Construct a new minor context. A minor context can only hold new values of the focus
     * (currentIterator) and current output destination.
     */

    public XPathContextMinor newMinorContext();

    /**
     * Get the XSLT-specific part of the context
     */

    public XPathContextMajor.XSLTContext getXSLTContext();

    /**
     * Get the local (non-tunnel) parameters that were passed to the current function or template
     * @return a ParameterSet containing the local parameters
     */

    public ParameterSet getLocalParameters();

    /**
     * Get the tunnel parameters that were passed to the current function or template. This includes all
     * active tunnel parameters whether the current template uses them or not.
     * @return a ParameterSet containing the tunnel parameters
     */

    public ParameterSet getTunnelParameters();

    /**
     * Set the creating expression (for use in diagnostics). The origin is generally set to "this" by the
     * object that creates the new context. It's up to the debugger to determine whether this information
     * is useful. Where possible, the object will be an {@link InstructionInfoProvider}, allowing information
     * about the calling instruction to be obtained.
     */

    public void setOrigin(InstructionInfoProvider expr);

    /**
     * Set the type of creating expression (for use in diagnostics). When a new context is created, either
     * this method or {@link #setOrigin} should be called.
     * @param loc The originating location: the argument must be one of the integer constants in class
     * {@link net.sf.saxon.trace.Location}
     */

    public void setOriginatingConstructType(int loc);

    /**
     * Get information about the creating expression or other construct.
     */

    public InstructionInfoProvider getOrigin();

    /**
     * Get the type of location from which this context was created.
     */

    public int getOriginatingConstructType();

    /**
    * Get the Controller. May return null when running outside XSLT or XQuery
    */

    public Controller getController();

    /**
     * Set the calling XPathContext
     */

    public void setCaller(XPathContext caller);

    /**
     * Get the calling XPathContext (the next one down the stack). This will be null if unknown, or
     * if the bottom of the stack has been reached.
     */

    public XPathContext getCaller();

    /**
    * Set a new sequence iterator.
    */

    public void setCurrentIterator(SequenceIterator iter);

     /**
     * Get the current iterator.
     * This encapsulates the context item, context position, and context size.
     * @return the current iterator, or null if there is no current iterator
     * (which means the context item, position, and size are undefined).
    */

    public SequenceIterator getCurrentIterator();

    /**
     * Get the context position (the position of the context item)
     * @return the context position (starting at one)
     * @throws DynamicError if the context position is undefined
    */

    public int getContextPosition() throws DynamicError;

    /**
    * Get the context item
     * @return the context item, or null if the context item is undefined
    */

    public Item getContextItem();
    /**
     * Get the context size (the position of the last item in the current node list)
     * @return the context size
     * @throws XPathException if the context position is undefined
     */

    public int getLast() throws XPathException;
    /**
    * Determine whether the context position is the same as the context size
    * that is, whether position()=last()
    */

    public boolean isAtLast() throws XPathException;

    /**
    * Get a named collation
    */

    public Comparator getCollation(String name) throws XPathException;

    /**
    * Get the default collation
    */

    public Comparator getDefaultCollation();

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
                                     boolean isTunnel) throws XPathException;

    /**
     * Get a reference to the local stack frame for variables. Note that it's
     * the caller's job to make a local copy of this. This is used for creating
     * a Closure containing a retained copy of the variables for delayed evaluation.
     * @return array of variables.
     */

    public StackFrame getStackFrame();

     /**
     * Get the value of a local variable, identified by its slot number
     */

    public ValueRepresentation evaluateLocalVariable(int slotnumber);

    /**
     * Set the value of a local variable, identified by its slot number
     */

    public void setLocalVariable(int slotnumber, ValueRepresentation value);

    /**
     * Set a new output destination, supplying the output format details. <BR>
     * Note that it is the caller's responsibility to close the Writer after use.
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
                                        SchemaType schemaType) throws XPathException;

    /**
     * Set the receiver to which output is to be written, marking it as a temporary (non-final)
     * output destination.
     * @param out The SequenceOutputter to be used
     */

    public void setTemporaryReceiver(SequenceReceiver out);

    /**
     * Change the Receiver to which output is written
     */

    public void setReceiver(SequenceReceiver receiver);

    /**
     * Get the Receiver to which output is currently being written.
     *
     * @return the current Receiver
     */
    public SequenceReceiver getReceiver();

    /**
     * Get the current mode.
     * @return the current mode
     */

    public Mode getCurrentMode();

    /**
     * Get the current template. This is used to support xsl:apply-imports
     *
     * @return the current template
     */

    public Template getCurrentTemplate();

    /**
     * Get the current group iterator. This supports the current-group() and
     * current-grouping-key() functions in XSLT 2.0
     * @return the current grouped collection
     */

    public GroupIterator getCurrentGroupIterator();

    /**
     * Get the current regex iterator. This supports the functionality of the regex-group()
     * function in XSLT 2.0.
     * @return the current regular expressions iterator
     */

    public RegexIterator getCurrentRegexIterator();

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
