package org.orbeon.saxon.expr;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.Controller;
import org.orbeon.saxon.instruct.*;
import org.orbeon.saxon.om.AxisIterator;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.SingletonIterator;
import org.orbeon.saxon.om.ValueRepresentation;
import org.orbeon.saxon.regex.RegexIterator;
import org.orbeon.saxon.sort.GroupIterator;
import org.orbeon.saxon.trace.InstructionInfoProvider;
import org.orbeon.saxon.trans.Mode;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.trans.Rule;

import java.util.Arrays;

/**
 * This class represents a "major context" in which an XPath expression is evaluated:
 * a "major context" object allows all aspects of the dynamic context to change, whereas
 * a "minor context" only allows changes to the focus and the destination for push output.
*/

public class XPathContextMajor extends XPathContextMinor {

    private ParameterSet localParameters = null;
    private XSLTContext xsltContext = null;
    private UserFunction tailCallFunction = null;

    /**
    * Constructor should only be called by the Controller,
    * which acts as a XPathContext factory.
    */

    public XPathContextMajor(Controller c) {
        controller = c;
        stackFrame = StackFrame.EMPTY;
        origin = controller;
    }

    /**
    * Private Constructor
    */

    private XPathContextMajor() {
    }

    /**
    * Constructor for use in free-standing Java applications.
    */

    public XPathContextMajor(Item item, Configuration config) {
        Executable exec = new Executable();
        exec.setConfiguration(config);
        exec.setHostLanguage(Configuration.JAVA_APPLICATION);
        controller = new Controller(config, exec);
        AxisIterator iter = SingletonIterator.makeIterator(item);
        iter.next();
        currentIterator = iter;
        origin = controller;
    }

    /**
    * Construct a new context as a copy of another. The new context is effectively added
    * to the top of a stack, and contains a pointer to the previous context
    */

    public XPathContextMajor newContext() {
        XPathContextMajor c = new XPathContextMajor();
        c.controller = controller;
        c.currentIterator = currentIterator;
        c.stackFrame = stackFrame;
        c.localParameters = localParameters;
        c.last = last;
        c.currentReceiver = currentReceiver;
        c.isTemporaryDestination = isTemporaryDestination;
        c.xsltContext = xsltContext;
        c.caller = this;
        c.tailCallFunction = null;
        return c;
    }

    public static XPathContextMajor newContext(XPathContextMinor p) {
        XPathContextMajor c = new XPathContextMajor();
        c.controller = p.getController();
        c.currentIterator = p.getCurrentIterator();
        c.stackFrame = p.getStackFrame();
        c.localParameters = p.getLocalParameters();

        c.last = p.last;
        c.currentReceiver = p.currentReceiver;
        c.isTemporaryDestination = p.isTemporaryDestination;
        c.xsltContext = p.getXSLTContext();
        c.caller = p;
        c.tailCallFunction = null;
        return c;
    }

    /**
     * Get the XSLT-specific part of the context
     */

    public XSLTContext getXSLTContext() {
        return xsltContext;
    }

    /**
     * Get the local parameters for the current template call.
     * @return the supplied parameters
     */

    public ParameterSet getLocalParameters() {
        return localParameters;
    }

    /**
     * Set the local parameters for the current template call.
     * @param localParameters the supplied parameters
     */

    public void setLocalParameters(ParameterSet localParameters) {
        this.localParameters = localParameters;
    }

    /**
     * Get the tunnel parameters for the current template call.
     * @return the supplied tunnel parameters
     */

    public ParameterSet getTunnelParameters() {
        if (xsltContext != null) {
            return xsltContext.tunnelParameters;
        } else {
            return null;
        }
    }

    /**
     * Set the tunnel parameters for the current template call.
     * @param tunnelParameters the supplied tunnel parameters
     */

    public void setTunnelParameters(ParameterSet tunnelParameters) {
        xsltContext = new XSLTContext(xsltContext);
        xsltContext.tunnelParameters = tunnelParameters;
    }

    /**
     * Set the creating expression (for use in diagnostics). The origin is generally set to "this" by the
     * object that creates the new context. It's up to the debugger to determine whether this information
     * is useful. The object will either be an {@link InstructionInfoProvider}, allowing information
     * about the calling instruction to be obtained, or null.
     */

    public void setOrigin(InstructionInfoProvider expr) {
        origin = expr;
    }

    /**
     * Set the local stack frame. This method is used when creating a Closure to support
     * delayed evaluation of expressions. The "stack frame" is actually on the Java heap, which
     * means it can survive function returns and the like.
     */

    public void setStackFrame(SlotManager map, ValueRepresentation[] variables) {
        stackFrame = new StackFrame(map, variables);
        if (map != null && variables.length != map.getNumberOfVariables()) {
            if (variables.length > map.getNumberOfVariables()) {
                throw new IllegalStateException("Attempting to set more local variables than the stackframe can accommodate");
            }
            stackFrame.slots = new ValueRepresentation[map.getNumberOfVariables()];
            System.arraycopy(variables, 0, stackFrame.slots, 0, variables.length);
        }
    }

    /**
     * Reset the stack frame variable map, while reusing the StackFrame object itself. This
     * is done on a tail call to a different function
     */

    public void resetStackFrameMap(SlotManager map, int numberOfParams) {
        stackFrame.map = map;
        if (stackFrame.slots.length != map.getNumberOfVariables()) {
            ValueRepresentation[] v2 = new ValueRepresentation[map.getNumberOfVariables()];
            System.arraycopy(stackFrame.slots, 0, v2, 0, numberOfParams);
            stackFrame.slots = v2;
        } else {
            // not strictly necessary
            Arrays.fill(stackFrame.slots, numberOfParams, stackFrame.slots.length, null);
        }
    }

    /**
     * Reset the local stack frame. This method is used when processing a tail-recursive function.
     * Instead of the function being called recursively, the parameters are set to new values and the
     * function body is evaluated repeatedly
     */

    public void requestTailCall(UserFunction fn, ValueRepresentation[] variables) {
        if (variables.length > stackFrame.slots.length) {
            ValueRepresentation[] v2 = new ValueRepresentation[fn.getStackFrameMap().getNumberOfVariables()];
            System.arraycopy(variables, 0, v2, 0, variables.length);
            stackFrame.slots = v2;
        } else {
            System.arraycopy(variables, 0, stackFrame.slots, 0, variables.length);
        }
        tailCallFunction = fn;
    }

    /**
     * Determine whether the body of a function is to be repeated, due to tail-recursive function calls
     */

    public UserFunction getTailCallFunction() {
        UserFunction fn = tailCallFunction;
        tailCallFunction = null;
        return fn;
    }

    /**
     * Create a new stack frame for local variables, using the supplied SlotManager to
     * define the allocation of slots to individual variables
     * @param map the SlotManager for the new stack frame
     */
    public void openStackFrame(SlotManager map) {
        int numberOfSlots = map.getNumberOfVariables();
        if (numberOfSlots == 0) {
            stackFrame = StackFrame.EMPTY;
        } else {
            stackFrame = new StackFrame(map, new ValueRepresentation[numberOfSlots]);
        }
    }

    /**
     * Create a new stack frame large enough to hold a given number of local variables,
     * for which no stack frame map is available. This is used in particular when evaluating
     * match patterns of template rules.
     * @param numberOfVariables The number of local variables to be accommodated.
     */

    public void openStackFrame(int numberOfVariables) {
        stackFrame = new StackFrame(null, new ValueRepresentation[numberOfVariables]);
    }

    /**
     * Get the value of a local variable, identified by its slot number
     */

//    public ValueRepresentation evaluateLocalVariable(int slotnumber) {
//        return slots[slotnumber];
//    }

    /**
     * Set the value of a local variable, identified by its slot number
     */

//    public void setLocalVariable(int slotnumber, ValueRepresentation value) {
//        slots[slotnumber] = value;
//    }


    /**
     * Set the current mode.
     * @param mode the new current mode
     */

    public void setCurrentMode(Mode mode) {
        if ((mode != null && !mode.isDefaultMode()) || (getCurrentMode() != null)) {
            xsltContext = new XSLTContext(xsltContext);
            xsltContext.currentMode = mode;
        }
    }

    /**
     * Get the current mode.
     * @return the current mode. May return null if the current mode is the default mode.
     */

    public Mode getCurrentMode() {
        if (xsltContext != null) {
            return xsltContext.currentMode;
        } else {
            return null;
        }
    }

    /**
     * Set the current template. This is used to support xsl:apply-imports. The caller
     * is responsible for remembering the previous current template and resetting it
     * after use.
     *
     * @param rule the current template rule
     */

    public void setCurrentTemplateRule(Rule rule) {
        xsltContext = new XSLTContext(xsltContext);
        xsltContext.currentTemplate = rule;
    }

    /**
     * Get the current template. This is used to support xsl:apply-imports
     *
     * @return the current template
     */

    public Rule getCurrentTemplateRule() {
        if (xsltContext != null) {
            return xsltContext.currentTemplate;
        } else {
            return null;
        }
    }

    /**
     * Set the current grouping iterator. This supports the current-group() and
     * current-grouping-key() functions in XSLT 2.0
     * @param collection the new current GroupIterator
     */

    public void setCurrentGroupIterator(GroupIterator collection) {
        xsltContext = new XSLTContext(xsltContext);
        xsltContext.currentGroupIterator = collection;
    }

    /**
     * Get the current group iterator. This supports the current-group() and
     * current-grouping-key() functions in XSLT 2.0
     * @return the current grouped collection
     */

    public GroupIterator getCurrentGroupIterator() {
        if (xsltContext != null) {
            return xsltContext.currentGroupIterator;
        } else {
            return null;
        }
    }

    /**
     * Set the current regex iterator. This supports the functionality of the regex-group()
     * function in XSLT 2.0.
     * @param currentJRegexIterator the current regex iterator
     */

    public void setCurrentRegexIterator(RegexIterator currentJRegexIterator) {
        xsltContext = new XSLTContext(xsltContext);
        xsltContext.currentJRegexIterator = currentJRegexIterator;
    }

    /**
     * Get the current regex iterator. This supports the functionality of the regex-group()
     * function in XSLT 2.0.
     * @return the current regular expressions iterator
     */

    public RegexIterator getCurrentRegexIterator() {
        if (xsltContext != null) {
            return xsltContext.currentJRegexIterator;
        } else {
            return null;
        }
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

        ParameterSet params = (isTunnel ? getTunnelParameters() : localParameters);
    	if (params==null) return false;
    	ValueRepresentation val = params.get(fingerprint);
        stackFrame.slots[binding.getSlotNumber()] = val;
        return (val != null);
    }

    /**
     * An XSLTContext object holds all the additional dynamic context items used in XSLT.
     * These are held in a separate object for two reasons: firstly, they don't change often,
     * so it's costly to copy them every time a new context object is created, and secondly,
     * they aren't used at all in XQuery, they just add overhead.
     */

    protected static class XSLTContext {
        public ParameterSet tunnelParameters = null;
        public Mode currentMode = null;
        public Rule currentTemplate = null;
        public GroupIterator currentGroupIterator = null;
        public RegexIterator currentJRegexIterator = null;

        /**
         * Create a new XSLTContext optionally by copying an existing XSLTContext
         * @param original the existing XSLTContext. May be null, in which case a new XSLTContext is
         * created from scratch.
         */

        public XSLTContext(XSLTContext original) {
            if (original != null) {
                this.tunnelParameters = original.tunnelParameters;
                this.currentMode = original.currentMode;
                this.currentTemplate = original.currentTemplate;
                this.currentGroupIterator = original.currentGroupIterator;
                this.currentJRegexIterator = original.currentJRegexIterator;
            }
        }
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
