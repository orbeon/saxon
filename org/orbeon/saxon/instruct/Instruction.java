package org.orbeon.saxon.instruct;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.Controller;
import org.orbeon.saxon.event.PipelineConfiguration;
import org.orbeon.saxon.event.SequenceOutputter;
import org.orbeon.saxon.event.SequenceReceiver;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.trace.InstructionInfo;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.type.TypeHierarchy;

import javax.xml.transform.SourceLocator;

/**
* Abstract superclass for all instructions in the compiled stylesheet.
* This represents a compiled instruction, and as such, the minimum information is
* retained from the original stylesheet. <br>
* Note: this class implements SourceLocator: that is, it can identify where in the stylesheet
* the source instruction was located.
*/

public abstract class Instruction extends ComputedExpression implements SourceLocator, TailCallReturner {

    /**
    * Constructor
    */

    public Instruction() {}

    /**
     * An implementation of Expression must provide at least one of the methods evaluateItem(), iterate(), or process().
     * This method indicates which of these methods is prefered. For instructions this is the process() method.
     */

    public int getImplementationMethod() {
        return Expression.PROCESS_METHOD;
    }

    /**
    * Get the namecode of the instruction for use in diagnostics
    */

    public int getInstructionNameCode() {
        return -1;
    };

    /**
     * Get the item type of the items returned by evaluating this instruction
     * @return the static item type of the instruction
     * @param th
     */

    public ItemType getItemType(TypeHierarchy th) {
        return Type.ITEM_TYPE;
    }

    /**
     * Get the cardinality of the sequence returned by evaluating this instruction
     * @return the static cardinality
     */

    public int computeCardinality() {
        return StaticProperty.ALLOWS_ZERO_OR_MORE;
    }

    /**
    * ProcessLeavingTail: called to do the real work of this instruction. This method
    * must be implemented in each subclass. The results of the instruction are written
    * to the current Receiver, which can be obtained via the Controller.
    * @param context The dynamic context of the transformation, giving access to the current node,
    * the current variables, etc.
    * @return null if the instruction has completed execution; or a TailCall indicating
    * a function call or template call that is delegated to the caller, to be made after the stack has
    * been unwound so as to save stack space.
    */

    public abstract TailCall processLeavingTail(XPathContext context) throws XPathException;

    /**
    * Process the instruction, without returning any tail calls
    * @param context The dynamic context, giving access to the current node,
    * the current variables, etc.
    */

    public void process(XPathContext context) throws XPathException {
        try {
            TailCall tc = processLeavingTail(context);
            while (tc != null) {
                tc = tc.processLeavingTail();
            }
        } catch (XPathException err) {
            if (err.getLocator() == null || err.getLocator().getLineNumber() == -1) {
                err.setLocator(this);
            }
            throw err;
        }
    }

    /**
     * Get a SourceLocator identifying the location of this instruction
     */

    public SourceLocator getSourceLocator() {
        return this;
    }

    /**
    * Construct an exception with diagnostic information. Note that this method
    * returns the exception, it does not throw it: that is up to the caller.
    * @param error The exception containing information about the error
    * @param context The controller of the transformation
    * @return an exception based on the supplied exception, but with location information
    * added relating to this instruction
    */

    protected static XPathException dynamicError(SourceLocator loc, XPathException error, XPathContext context) {
        if (error instanceof TerminationException) return error;
        if (error.getLocator()==null ||
                (error.getLocator() instanceof ExpressionLocation &&
                        context.getController().getExecutable().getHostLanguage() != Configuration.XQUERY) ||
                error.getLocator().getLineNumber()==-1) {
            // If the exception has no location information, construct a new
            // exception containing the required information
            try {
                DynamicError de = new DynamicError(error.getMessage(),
                                                loc,
                                                error.getException());
                if (error instanceof DynamicError) {
                    de.setErrorCode(error.getErrorCodeLocalPart());
                    if (((DynamicError)error).getXPathContext()==null) {
                        de.setXPathContext(context);
                    } else {
                        de.setXPathContext(((DynamicError)error).getXPathContext());
                    }
                }
                return de;
            } catch (Exception secondaryError) {
                // currently happens when running XQuery
                return error;
            }
        }
        return error;
    }

    /**
     * Assemble a ParameterSet. Method used by instructions that have xsl:with-param
     * children. This method is used for the non-tunnel parameters.
     */

    protected static ParameterSet assembleParams(XPathContext context,
                                                 WithParam[] actualParams)
    throws XPathException {
        if (actualParams == null || actualParams.length == 0) {
            return null;
        }
        ParameterSet params = new ParameterSet(actualParams.length);
        for (int i=0; i<actualParams.length; i++) {
            params.put(actualParams[i].getVariableFingerprint(),
                       actualParams[i].getSelectValue(context));
        }
        return params;
    }

    /**
     * Assemble a ParameterSet. Method used by instructions that have xsl:with-param
     * children. This method is used for the tunnel parameters.
     */

    protected static ParameterSet assembleTunnelParams(XPathContext context,
                                                       WithParam[] actualParams)
    throws XPathException {
        ParameterSet existingParams = context.getTunnelParameters();
        if (existingParams == null) {
            return assembleParams(context, actualParams);
        }
        ParameterSet newParams = new ParameterSet(existingParams, (actualParams==null ? 0 : actualParams.length));
        if (actualParams == null || actualParams.length == 0) {
            return newParams;
        }
        for (int i=0; i<actualParams.length; i++) {
            newParams.put(actualParams[i].getVariableFingerprint(),
                          actualParams[i].getSelectValue(context));
        }
        return newParams;
    }

    /**
     * Simplify an expression. This performs any static optimization (by rewriting the expression
     * as a different expression). The default implementation does nothing.
     *
     * @exception org.orbeon.saxon.trans.XPathException if an error is discovered during expression
     *     rewriting
     * @return the simplified expression
     */

     public abstract Expression simplify(StaticContext env) throws XPathException;

    /**
     * Get the static properties of this expression (other than its type). The result is
     * bit-signficant. These properties are used for optimizations. In general, if
     * property bit is set, it is true, but if it is unset, the value is unknown.
     *
     * @return a set of flags indicating static properties of this expression
     */

    public int computeSpecialProperties() {
        int p = super.computeSpecialProperties();
        if (createsNewNodes()) {
            return p;
        } else {
            return p | StaticProperty.NON_CREATIVE;
        }
    }

    /**
     * Determine whether this instruction creates new nodes.
     * This implementation returns a default value of false
     */

    public boolean createsNewNodes() {
        return false;
    }


    /**
     * Handle promotion offers, that is, non-local tree rewrites.
     * @param offer The type of rewrite being offered
     * @throws XPathException
     */

    protected void promoteInst(PromotionOffer offer) throws XPathException {
    }

    /**
     * Offer promotion for this subexpression. The offer will be accepted if the subexpression
     * is not dependent on the factors (e.g. the context item) identified in the PromotionOffer.
     * This method is always called at compile time.
     *
     * @param offer details of the offer, for example the offer to move
     *     expressions that don't depend on the context to an outer level in
     *     the containing expression
     * @exception org.orbeon.saxon.trans.XPathException if any error is detected
     * @return if the offer is not accepted, return this expression unchanged.
     *      Otherwise return the result of rewriting the expression to promote
     *      this subexpression
     */

    public Expression promote(PromotionOffer offer) throws XPathException {
        Expression exp = offer.accept(this);
        if (exp!=null) {
            return exp;
        } else {
            promoteInst(offer);
            return this;
        }
    }

     /**
     * Evaluate an expression as a single item. This always returns either a single Item or
     * null (denoting the empty sequence). No conversion is done. This method should not be
     * used unless the static type of the expression is a subtype of "item" or "item?": that is,
     * it should not be called if the expression may return a sequence. There is no guarantee that
     * this condition will be detected.
     *
     * @param context The context in which the expression is to be evaluated
     * @exception XPathException if any dynamic error occurs evaluating the
     *     expression
     * @return the node or atomic value that results from evaluating the
     *     expression; or null to indicate that the result is an empty
     *     sequence
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        int m = getImplementationMethod();
        if ((m & EVALUATE_METHOD) != 0) {
                throw new AssertionError(
                        "evaluateItem() is not implemented in the subclass " + this.getClass());
        } else if ((m & ITERATE_METHOD) != 0) {
            return iterate(context).next();
        } else {
            Controller controller = context.getController();
            XPathContext c2 = context.newMinorContext();
            c2.setOrigin(this);
            SequenceOutputter seq = controller.allocateSequenceOutputter(1);
            final PipelineConfiguration pipe = controller.makePipelineConfiguration();
            pipe.setHostLanguage(getHostLanguage());
            seq.setPipelineConfiguration(pipe);
            c2.setTemporaryReceiver(seq);
            process(c2);
            seq.close();
            Item result = seq.getFirstItem();
            seq.reset();
            return result;
        }
    }

    /**
     * Return an Iterator to iterate over the values of a sequence. The value of every
     * expression can be regarded as a sequence, so this method is supported for all
     * expressions. This default implementation handles iteration for expressions that
     * return singleton values: for non-singleton expressions, the subclass must
     * provide its own implementation.
     *
     * @exception XPathException if any dynamic error occurs evaluating the
     *     expression
     * @param context supplies the context for evaluation
     * @return a SequenceIterator that can be used to iterate over the result
     *     of the expression
     */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        int m = getImplementationMethod();
        if ((m & EVALUATE_METHOD) != 0) {
            Item item = evaluateItem(context);
            if (item==null) {
                return EmptyIterator.getInstance();
            } else {
                return SingletonIterator.makeIterator(item);
            }
        } else if ((m & ITERATE_METHOD) != 0) {
            throw new AssertionError("iterate() is not implemented in the subclass " + this.getClass());
        } else {
            Controller controller = context.getController();
            XPathContext c2 = context.newMinorContext();
            c2.setOrigin(this);
            SequenceOutputter seq = controller.allocateSequenceOutputter(20);
            PipelineConfiguration pipe = controller.makePipelineConfiguration();
            pipe.setHostLanguage(getHostLanguage());
            seq.setPipelineConfiguration(pipe);
            c2.setTemporaryReceiver(seq);
            process(c2);
            seq.close();
            return seq.iterate();
        }
    }

    /**
     * Evaluate an expression as a String. This function must only be called in contexts
     * where it is known that the expression will return a single string (or where an empty sequence
     * is to be treated as a zero-length string). Implementations should not attempt to convert
     * the result to a string, other than converting () to "". This method is used mainly to
     * evaluate expressions produced by compiling an attribute value template.
     *
     * @exception org.orbeon.saxon.trans.XPathException if any dynamic error occurs evaluating the
     *     expression
     * @exception java.lang.ClassCastException if the result type of the
     *     expression is not xs:string?
     * @param context The context in which the expression is to be evaluated
     * @return the value of the expression, evaluated in the current context.
     *     The expression must return a string or (); if the value of the
     *     expression is (), this method returns "".
     */

    public final String evaluateAsString(XPathContext context) throws XPathException {
        Item item = evaluateItem(context);
        if (item==null) {
            return "";
        } else {
            return item.getStringValue();
        }
    }

    public InstructionInfo getInstructionInfo() {
        InstructionDetails details = new InstructionDetails();
        details.setSystemId(getSystemId());
        details.setLineNumber(getLineNumber());
        details.setConstructType(getInstructionNameCode());
        return details;
    }

    /**
     * Static method to append an item that results from evaluating an expression to the current
     * result receiver. The method checks to see whether the item returned from the expression is
     * actually a function-call-package representing the result of a tail call optimization; if so,
     * the tail function calls are complete and the final result is passed on.
     * @param it the item: possibly a FunctionCallPackage
     * @param out the Receiver
     * @throws XPathException
     */

    public static void appendItem(Item it, SequenceReceiver out, int locationId) throws XPathException {
        //// If this call to xsl:sequence is in a template (rather than a function) it may
        //// be marked as a tail call; in this situation we need to evaluate the returned
        //// function call package. Doing so may return further function call packages, which also need
        //// to be processed. This has to be iterative rather than recursive to avoid consuming stack space.
       // while (true) {
            if (it == null) {
                return;
            } else {
                out.append(it, locationId, NodeInfo.ALL_NAMESPACES);
                return;
            }
        //}
    }

    /**
     * Establish whether this is an XSLT instruction or an XQuery instruction
     * (used to produce appropriate diagnostics)
     * @return true for XSLT, false for XQuery
     */
    public boolean isXSLT() {
        return getHostLanguage() == Configuration.XSLT;
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
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none
//