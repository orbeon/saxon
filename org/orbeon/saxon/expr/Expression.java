package net.sf.saxon.expr;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.SchemaType;

import java.io.PrintStream;
import java.util.Iterator;

/**
 * Interface supported by an XPath expression. This includes both compile-time
 * and run-time methods.
 */

public interface Expression {

    public static final int EVALUATE_METHOD = 1;
    public static final int ITERATE_METHOD = 2;
    public static final int PROCESS_METHOD = 4;

    /**
     * An implementation of Expression must provide at least one of the methods evaluateItem(), iterate(), or process().
     * This method indicates which of these methods is provided directly. The other methods will always be available
     * indirectly, using an implementation that relies on one of the other methods.
     */

    public int getImplementationMethod();

    /**
     * Simplify an expression. This performs any static optimization (by rewriting the expression
     * as a different expression). The default implementation does nothing.
     *
     * @exception net.sf.saxon.trans.StaticError if an error is discovered during expression
     *     rewriting
     * @return the simplified expression
     */

    Expression simplify(StaticContext env) throws XPathException;

    /**
     * Perform static analysis and optimisation of an expression and its subexpressions.
     *
     * <p>This checks statically that the operands of the expression have
     * the correct type; if necessary it generates code to do run-time type checking or type
     * conversion. A static type error is reported only if execution cannot possibly succeed, that
     * is, if a run-time type error is inevitable. The call may return a modified form of the expression.</p>
     *
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable. However, the types of such functions and
     * variables may not be accurately known if they have not been explicitly declared.</p>
     *
     * @param env the static context of the expression
     * @param contextItemType the static type of "." at the point where this expression is invoked.
     * The parameter is set to null if it is known statically that the context item will be undefined.
     * If the type of the context item is not known statically, the argument is set to
     * {@link net.sf.saxon.type.Type#ITEM_TYPE}
     * @exception net.sf.saxon.trans.StaticError if an error is discovered during this phase
     *     (typically a type error)
     * @return the original expression, rewritten to perform necessary
     *     run-time type checks, and to perform other type-related
     *     optimizations
     */

    Expression analyze(StaticContext env, ItemType contextItemType) throws XPathException;

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
     * @exception net.sf.saxon.trans.XPathException if any error is detected
     * @return if the offer is not accepted, return this expression unchanged.
     *      Otherwise return the result of rewriting the expression to promote
     *      this subexpression
     */

    Expression promote(PromotionOffer offer) throws XPathException;

    /**
     * Get the static properties of this expression (other than its type). The result is
     * bit-signficant. These properties are used for optimizations. In general, if
     * property bit is set, it is true, but if it is unset, the value is unknown.
     *
     * @return a set of flags indicating static properties of this expression
     */

    int getSpecialProperties();

    /**
     * <p>Determine the static cardinality of the expression. This establishes how many items
     * there will be in the result of the expression, at compile time (i.e., without
     * actually evaluating the result.</p>
     *
     * <p>This method should always return a result, though it may be the best approximation
     * that is available at the time.</p>
     *
     * @return one of the values {@link StaticProperty#ALLOWS_ONE},
     *     {@link StaticProperty#ALLOWS_ZERO_OR_MORE}, {@link StaticProperty#ALLOWS_ZERO_OR_ONE},
     *     {@link StaticProperty#ALLOWS_ONE_OR_MORE}, {@link StaticProperty#EMPTY}. This default
     *     implementation returns ZERO_OR_MORE (which effectively gives no
     *     information).
     */

    int getCardinality();

    /**
	 * Determine the data type of the expression, if possible. All expression return
	 * sequences, in general; this method determines the type of the items within the
	 * sequence, assuming that (a) this is known in advance, and (b) it is the same for
	 * all items in the sequence.
     *
     * <p>This method should always return a result, though it may be the best approximation
     * that is available at the time.</p>
	 *
	 * @return a value such as Type.STRING, Type.BOOLEAN, Type.NUMBER,
	 *     Type.NODE, or Type.ITEM (meaning not known at compile time)
	 */

    ItemType getItemType();

    /**
     * Determine which aspects of the context the expression depends on. The result is
     * a bitwise-or'ed value composed from constants such as {@link StaticProperty#DEPENDS_ON_CONTEXT_ITEM} and
     * {@link StaticProperty#DEPENDS_ON_CURRENT_ITEM}. The default implementation combines the intrinsic
     * dependencies of this expression with the dependencies of the subexpressions,
     * computed recursively. This is overridden for expressions such as FilterExpression
     * where a subexpression's dependencies are not necessarily inherited by the parent
     * expression.
     *
     * @return a set of bit-significant flags identifying the dependencies of
     *     the expression
     */

    int getDependencies();

    /**
     * Get the immediate sub-expressions of this expression. Default implementation
     * returns a zero-length array, appropriate for an expression that has no
     * sub-expressions.
     * @return an iterator containing the sub-expressions of this expression
     */

    Iterator iterateSubExpressions();

    /**
     * Get the container that immediately contains this expression. This method
     * returns null for an outermost expression; it also return null in the case
     * of literal values. For an XPath expression occurring within an XSLT stylesheet,
     * this method returns the XSLT instruction containing the XPath expression.
     * @return the expression that contains this expression, if known; return null
     * if there is no containing expression or if the containing expression is unknown.
     */

    Container getParentExpression();

    /**
     * Test if this expression is the same as another expression.
     * (Note, returns false to indicate "don't know": our tests are rather limited)
     *
     * @param other the expression to be compared with this one
     * @return true if this expression is known to be equivalent to the other
     *     expression (that is, to deliver the same result in all
     *     circumstances, assuming the context is the same)
     */

    //boolean equals(Object other);


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

    Item evaluateItem(XPathContext context) throws XPathException;

    /**
     * Return an Iterator to iterate over the values of a sequence. The value of every
     * expression can be regarded as a sequence, so this method is supported for all
     * expressions. This default implementation handles iteration for expressions that
     * return singleton values: for non-singleton expressions, the subclass must
     * provide its own implementation.
     *
     * @exception net.sf.saxon.trans.XPathException if any dynamic error occurs evaluating the
     *     expression
     * @param context supplies the context for evaluation
     * @return a SequenceIterator that can be used to iterate over the result
     *     of the expression
     */

    SequenceIterator iterate(XPathContext context) throws XPathException;

    /**
     * Get the effective boolean value of the expression. This returns false if the value
     * is the empty sequence, a zero-length string, a number equal to zero, or the boolean
     * false. Otherwise it returns true.
     *
     * @param context The context in which the expression is to be evaluated
     * @exception net.sf.saxon.trans.XPathException if any dynamic error occurs evaluating the
     *     expression
     * @return the effective boolean value
     */

    boolean effectiveBooleanValue(XPathContext context) throws XPathException;

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

    public String evaluateAsString(XPathContext context) throws XPathException;

    /**
    * Process the instruction, without returning any tail calls
    * @param context The dynamic context, giving access to the current node,
    * the current variables, etc.
    */

    public void process(XPathContext context) throws XPathException;

    /**
     * Diagnostic print of expression structure. The expression is written to the System.err
     * output stream
     *
     * @param level indentation level for this expression
     * @param pool NamePool used to expand any names appearing in the expression
     * @param out Output destination
     */

    public void display(int level, NamePool pool, PrintStream out);

    /**
     * Check statically that the results of the expression are capable of constructing the content
     * of a given schema type.
     * @param parentType The schema type
     * @param env the static context
     * @param whole true if this expression is expected to make the whole content of the type, false
     * if it is expected to make up only a part
     * @throws XPathException if the expression doesn't match the required content type
     */

    public void checkPermittedContents(SchemaType parentType, StaticContext env, boolean whole) throws XPathException;
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