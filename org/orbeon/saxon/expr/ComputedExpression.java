package org.orbeon.saxon.expr;

import org.orbeon.saxon.instruct.Executable;
import org.orbeon.saxon.instruct.Instruction;
import org.orbeon.saxon.instruct.InstructionDetails;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.om.SingletonIterator;
import org.orbeon.saxon.trace.InstructionInfo;
import org.orbeon.saxon.trace.InstructionInfoProvider;
import org.orbeon.saxon.trace.Location;
import org.orbeon.saxon.type.SchemaType;
import org.orbeon.saxon.value.AtomicValue;
import org.orbeon.saxon.value.Cardinality;
import org.orbeon.saxon.value.StringValue;
import org.orbeon.saxon.xpath.DynamicError;
import org.orbeon.saxon.xpath.XPathException;

import javax.xml.transform.SourceLocator;
import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;

/**
 * <p>This class is an abstract superclass for different kinds of expression. This includes
 * XSLT instructions, which are treated just like XPath expressions.
 * Every expression is either a constant Value, or a ComputedExpression.</p>
 *
 * <p>There are three principal methods for evaluating an expression: iterate(), which
 * an iterator over the result of the expression as a sequence; evaluateItem(), which returns an
 * object that is an instance of org.orbeon.saxon.om.Item; and process(), which pushes the results of
 * the expression to a Receiver. All three methods take an
 * XPathContext object to supply the evaluation context; for an expression that is
 * a Value, this argument is ignored and may be null. This root class provides an implementation
 * of iterate() in terms of evaluateItem() that works only for singleton expressions, and an implementation
 * of evaluateItem() in terms of iterate() that works only for non-singleton expressions. Subclasses
 * of expression must therefore provide either iterate() or evaluateItem() or process(): they do not have to provide
 * all three.</p>
 *
 * <p>Note that the methods that take an XPathContext argument are run-time methods.
 * The methods without such an argument are compile-time methods. Run-time methods must not
 * modify the state of the Expression object.</p>
 */

public abstract class ComputedExpression
        implements Serializable, Expression, InstructionInfoProvider, Container {

    protected int staticProperties = -1;
    protected int locationId = -1;
    private Container parentExpression;

    /**
     * Get the expression that immediately contains this expression. This method
     * returns null for an outermost expression; it also return null in the case
     * of literal values. For an XPath expression occurring within an XSLT stylesheet,
     * this method returns the XSLT instruction containing the XPath expression.
     * @return the expression that contains this expression, if known; return null
     * if there is no containing expression or if the containing expression is unknown.
     */

    public Container getParentExpression() {
        return parentExpression;
    }

    public void setParentExpression(Container parent) {
        if (this == parent) {
            throw new AssertionError("Incestuous relationship!");
        }
        parentExpression = parent;
    }

    protected void adoptChildExpression(Expression child) {
        if (this == child) {
            throw new AssertionError("Incestuous relationship!");
        }
        if (child instanceof ComputedExpression) {
            if (parentExpression == null && child.getParentExpression() != this) {
                parentExpression = child.getParentExpression();
            }
            ((ComputedExpression)child).setParentExpression(this);
            if (this.locationId == -1) {
                ExpressionTool.copyLocationInfo(child, this);
            } else if (((ComputedExpression)child).locationId == -1) {
                ExpressionTool.copyLocationInfo(this, child);
            }
        }
    }

    /**
     * An implementation of Expression must provide at least one of the methods evaluateItem(), iterate(), or process().
     * This method indicates which of these methods is prefered.
     */

    public int getImplementationMethod() {
        if (Cardinality.allowsMany(getCardinality())) {
            return ITERATE_METHOD;
        } else {
            return EVALUATE_METHOD;
        }
    }

    /**
     * Set the location ID on an expression.
     */

    public void setLocationId(int id) {
        locationId = id;
    }

    /**
     * Get the location ID of the expression
     */

    public final int getLocationId() {
        return locationId;
    }

    /**
     * Get the line number of the expression
     */

    public int getLineNumber() {
        if (locationId == -1) {
            if (parentExpression != null) {
                return parentExpression.getLineNumber();
            } else {
                return -1;
            }
        }
        return locationId & 0xfffff;
    }

    /**
     * Get the column number of the expression
     */

    public int getColumnNumber() {
        return -1;
    }

    /**
     * Get the systemId of the module containing the expression
     */

    public String getSystemId() {
        Executable exec = getExecutable();
        if (exec == null) {
            return null;
        }
        return exec.getLocationMap().getSystemId(locationId);
    }

    /**
     * Get the publicId of the module containing the expression (to satisfy the SourceLocator interface)
     */

    public final String getPublicId() {
        return null;
    }

    /**
     * Get the executable containing this expression
     */

    public Executable getExecutable() {
        Container container = getParentExpression();
        if (container == null) {
            return null;
        } if (container == this) {
            throw new IllegalStateException("Expression cannot contain itself");
        } else {
            return container.getExecutable();
        }
    }

    /**
     * Simplify an expression. This performs any static optimization (by rewriting the expression
     * as a different expression). The default implementation does nothing.
     *
     * @exception XPathException if an error is discovered during expression
     *     rewriting
     * @return the simplified expression
     */

    public Expression simplify(StaticContext env) throws XPathException {
        return this;
    }

    /**
     * Offer promotion for this subexpression. The offer will be accepted if the subexpression
     * is not dependent on the factors (e.g. the context item) identified in the PromotionOffer.
     * By default the offer is not accepted - this is appropriate in the case of simple expressions
     * such as constant values and variable references where promotion would give no performance
     * advantage. This method is always called at compile time.
     *
     * @param offer details of the offer, for example the offer to move
     *     expressions that don't depend on the context to an outer level in
     *     the containing expression
     * @exception XPathException if any error is detected
     * @return if the offer is not accepted, return this expression unchanged.
     *      Otherwise return the result of rewriting the expression to promote
     *      this subexpression
     */

    public Expression promote(PromotionOffer offer) throws XPathException {
        // The following temporary code checks that this method is implemented for all expressions
        // that have subexpressions
//        if (getSubExpressions().length > 0) {
//            throw new UnsupportedOperationException("promote is not implemented for " + this.getClass());
//        }
        return this;
    }

    /**
     * Get the static properties of this expression (other than its type). The result is
     * bit-signficant. These properties are used for optimizations. In general, if
     * property bit is set, it is true, but if it is unset, the value is unknown.
     *
     * @return a set of flags indicating static properties of this expression
     */

    public final int getSpecialProperties() {
        if (staticProperties == -1) {
            computeStaticProperties();
        }
        return staticProperties & StaticProperty.SPECIAL_PROPERTY_MASK;
    }

    /**
     * Compute the static properties. This should only be done once for each
     * expression.
     */

    public final void computeStaticProperties() {
        staticProperties =
                computeDependencies() |
                computeCardinality() |
                computeSpecialProperties();
    }

    /**
     * Reset the static properties. This should be done whenever the expression is changed in a way that might
     * affect the properties. It causes the properties to be recomputed next time they are needed.
     */

    public final void resetStaticProperties() {
        staticProperties = -1;
    }

    protected abstract int computeCardinality();

    public int computeSpecialProperties() {
        return 0;
    }

    /**
     * Determine the static cardinality of the expression. This establishes how many items
     * there will be in the result of the expression, at compile time (i.e., without
     * actually evaluating the result.
     *
     * @return one of the values Cardinality.ONE_OR_MORE,
     *     Cardinality.ZERO_OR_MORE, Cardinality.EXACTLY_ONE,
     *     Cardinality.ZERO_OR_ONE, Cardinality.EMPTY. This default
     *     implementation returns ZERO_OR_MORE (which effectively gives no
     *     information).
     */

    public int getCardinality() {
        if (staticProperties == -1) {
            computeStaticProperties();
        }
        return staticProperties & StaticProperty.CARDINALITY_MASK;
    }


    /**
     * Determine which aspects of the context the expression depends on. The result is
     * a bitwise-or'ed value composed from constants such as XPathContext.VARIABLES and
     * XPathContext.CURRENT_NODE. The default implementation combines the intrinsic
     * dependencies of this expression with the dependencies of the subexpressions,
     * computed recursively. This is overridden for expressions such as FilterExpression
     * where a subexpression's dependencies are not necessarily inherited by the parent
     * expression.
     *
     * @return a set of bit-significant flags identifying the dependencies of
     *     the expression
     */

    public int getDependencies() {
        // Implemented as a memo function: we only compute the dependencies
        // for each expression once
        if (staticProperties == -1) {
            computeStaticProperties();
        }
        return staticProperties & StaticProperty.DEPENDENCY_MASK;
    }

    /**
     * Compute the dependencies of an expression, as the union of the
     * dependencies of its subexpressions. (This is overridden for path expressions
     * and filter expressions, where the dependencies of a subexpression are not all
     * propogated). This method should be called only once, to compute the dependencies;
     * after that, getDependencies should be used.
     * @return the depencies, as a bit-mask
     */

    public int computeDependencies() {
        int dependencies = (short) getIntrinsicDependencies();
        for (Iterator children = iterateSubExpressions(); children.hasNext();) {
            dependencies |= (short) ((Expression)children.next()).getDependencies();
        }
        return dependencies;
    }

    /**
     * Determine the intrinsic dependencies of an expression, that is, those which are not derived
     * from the dependencies of its subexpressions. For example, position() has an intrinsic dependency
     * on the context position, while (position()+1) does not. The default implementation
     * of the method returns 0, indicating "no dependencies".
     *
     * @return a set of bit-significant flags identifying the "intrinsic"
     *     dependencies. The flags are documented in class org.orbeon.saxon.value.StaticProperty
     */

    public int getIntrinsicDependencies() {
        return 0;
    }


    /**
     * Get the immediate sub-expressions of this expression. Default implementation
     * returns a zero-length array, appropriate for an expression that has no
     * sub-expressions.
     * @return an iterator containing the sub-expressions of this expression
     */

    public Iterator iterateSubExpressions() {
        return Collections.EMPTY_LIST.iterator();
    }

    /**
     * Check that any elements and attributes constructed or returned by this expression are acceptable
     * in the content model of a given complex type. It's always OK to say yes, since the check will be
     * repeated at run-time. The process of checking element and attribute constructors against the content
     * model of a complex type also registers the type of content expected of those constructors, so the
     * static validation can continue recursively.
     */

    public void checkPermittedContents(SchemaType parentType, StaticContext env, boolean whole) throws XPathException {
        return;
    }

    /**
     * Mark tail-recursive calls on stylesheet functions. For most expressions, this does nothing.
     *
     * @return true if a tail recursive call was found and if this call
     *     accounts for the whole of the value.
     */

    public boolean markTailFunctionCalls() {
        return false;
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
        return iterate(context).next();
    }

    /**
     * Evaluate an expression as a String. This function must only be called in contexts
     * where it is known that the expression will return a single string (or where an empty sequence
     * is to be treated as a zero-length string). Implementations should not attempt to convert
     * the result to a string, other than converting () to "". This method is used mainly to
     * evaluate expressions produced by compiling an attribute value template.
     *
     * @exception XPathException if any dynamic error occurs evaluating the
     *     expression
     * @exception ClassCastException if the result type of the
     *     expression is not xs:string?
     * @param context The context in which the expression is to be evaluated
     * @return the value of the expression, evaluated in the current context.
     *     The expression must return a string or (); if the value of the
     *     expression is (), this method returns "".
     */

    public String evaluateAsString(XPathContext context) throws XPathException {
        Item o = evaluateItem(context);
        if (o instanceof AtomicValue && !((AtomicValue)o).hasBuiltInType()) {
            o = ((AtomicValue) o).getPrimitiveValue();
        }
        StringValue value = (StringValue) o;  // the ClassCastException is deliberate
        if (value == null) return "";
        return value.getStringValue();
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
        if ((!Cardinality.allowsMany(getCardinality()))) {
            // The above line is defensive coding. We should only be here if it's a singleton.
            Item value = evaluateItem(context);
            return SingletonIterator.makeIterator(value);
        } else {
            throw new UnsupportedOperationException("Non-singleton expression " + getClass() + " must supply iterate() method");
        }
    }


    /**
     * Get the effective boolean value of the expression. This returns false if the value
     * is the empty sequence, a zero-length string, a number equal to zero, or the boolean
     * false. Otherwise it returns true.
     *
     * @param context The context in which the expression is to be evaluated
     * @exception XPathException if any dynamic error occurs evaluating the
     *     expression
     * @return the effective boolean value
     */

    public boolean effectiveBooleanValue(XPathContext context) throws XPathException {
        return ExpressionTool.effectiveBooleanValue(iterate(context));
    }

    /**
     * Process the instruction, without returning any tail calls
     * @param context The dynamic context, giving access to the current node,
     * the current variables, etc.
     */

    public void process(XPathContext context) throws XPathException {
        int m = getImplementationMethod();

        if ((m & EVALUATE_METHOD) != 0) {
            Item item = evaluateItem(context);
            // Need to cater for it being a tailcall returned from a function
            Instruction.appendItem(item, context.getReceiver(), locationId);

        } else if ((m & ITERATE_METHOD) != 0) {

            SequenceIterator iter = iterate(context);
            while (true) {
                Item it = iter.next();
                if (it == null) break;
                // Need to cater for it being a tailcall returned from a function
                Instruction.appendItem(it, context.getReceiver(), locationId);
            }

        } else {
            dynamicError("process() is not implemented in the subclass " + this.getClass(), context);
        }
    }


    /**
     * Method used in subclasses to signal a dynamic error
     */

    protected void dynamicError(String message, XPathContext context) throws DynamicError {
        DynamicError err = new DynamicError(message, getSourceLocator());
        err.setXPathContext(context);
        throw err;
    }

    /**
     * Method used in subclasses to signal a dynamic error
     */

    protected void dynamicError(String message, String code, XPathContext context) throws DynamicError {
        DynamicError err = new DynamicError(message, getSourceLocator());
        err.setXPathContext(context);
        err.setErrorCode(code);
        throw err;
    }

    /**
     * Method used in subclasses to signal a runtime type error
     */

    protected void typeError(String message, XPathContext context) throws DynamicError {
        DynamicError e = new DynamicError(message, getSourceLocator());
        e.setIsTypeError(true);
        e.setXPathContext(context);
        throw e;
    }

    /**
     * Method used in subclasses to signal a runtime type error
     */

    protected void typeError(String message, String errorCode, XPathContext context) throws DynamicError {
        DynamicError e = new DynamicError(message, getSourceLocator());
        e.setIsTypeError(true);
        e.setErrorCode(errorCode);
        e.setXPathContext(context);
        throw e;
    }

    /**
     * Get a SourceLocator for this expression
     */

    private SourceLocator getSourceLocator() {
        return ExpressionTool.getLocator(this);
    }

    /**
     * Get InstructionInfo for this expression
     */

    public InstructionInfo getInstructionInfo() {
        InstructionDetails details = new InstructionDetails();
        details.setConstructType(getConstructType());
        details.setProperty("expression", this);
        details.setSystemId(getSystemId());
        details.setLineNumber(getLineNumber());
        details.setColumnNumber(getColumnNumber());
        if (this instanceof Assignation) {
            details.setObjectNameCode(((Assignation)this).getVariableNameCode());
        }
        return details;
    }

    /**
     * Get the type of this expression for use in tracing and diagnostics
     * @return the type of expression, as enumerated in class {@link Location}
     */

    protected int getConstructType() {
        return Location.XPATH_EXPRESSION;
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
// Contributor(s): Michael Kay
//
