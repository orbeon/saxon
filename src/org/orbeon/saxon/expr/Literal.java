package org.orbeon.saxon.expr;
import org.orbeon.saxon.trace.ExpressionPresenter;
import org.orbeon.saxon.event.SequenceReceiver;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.Navigator;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.value.*;

/**
 * A Literal is an expression whose value is constant: it is a class that implements the Expression
 * interface as a wrapper around a Value. This may derive from an actual literal in an XPath expression
 * or query, or it may be the result of evaluating a constant subexpression such as true() or xs:date('2007-01-16')
*/

public class Literal extends Expression {

    private Value value;

    /**
     * Create a literal as a wrapper around a Value
     * @param value the value of this literal
     */

    public Literal(Value value) {
        this.value = value;
    }

    /**
     * Create a literal as a wrapper around a Value (factory method)
     * @param value the value of this literal
     * @return the Literal
     */

    public static Literal makeLiteral(Value value) {
        if (value instanceof StringValue) {
            return new StringLiteral((StringValue)value);
        } else {
            return new Literal(value);
        }
    }

    /**
     * Make an empty-sequence literal
     * @return a literal whose value is the empty sequence
     */

    public static Literal makeEmptySequence() {
        return new Literal(EmptySequence.getInstance());
    }

    /**
     * Get the value represented by this Literal
     * @return the constant value
     */

    public Value getValue() {
        return value;
    }


    /**
     * Simplify an expression
     * @return for a Value, this always returns the value unchanged
     * @param visitor an expression visitor
     */

    public final Expression simplify(ExpressionVisitor visitor) {
        try {
            value = value.reduce();
        } catch (XPathException err) {
            throw new AssertionError();
        }
        return this;
    }

    /**
    * TypeCheck an expression
    * @return for a Value, this always returns the value unchanged
    */

    public final Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) {
        return this;
    }

    /**
    * Optimize an expression
    * @return for a Value, this always returns the value unchanged
    */

    public final Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) {
        return this;
    }

    /**
     * Determine the data type of the items in the expression, if possible
     * @return for the default implementation: AnyItemType (not known)
     * @param th The TypeHierarchy. Can be null if the target is an AtomicValue.
     */

    public ItemType getItemType(TypeHierarchy th) {
        return value.getItemType(th);
    }

    /**
     * Determine the cardinality
     */

    public int computeCardinality() {
        if (value instanceof EmptySequence) {
            return StaticProperty.EMPTY;
        } else if (value instanceof AtomicValue) {
            return StaticProperty.EXACTLY_ONE;
        }
        try {
            SequenceIterator iter = value.iterate();
            Item next = iter.next();
            if (next == null) {
                return StaticProperty.EMPTY;
            } else {
                if (iter.next() != null) {
                    return StaticProperty.ALLOWS_ONE_OR_MORE;
                } else {
                    return StaticProperty.EXACTLY_ONE;
                }
            }
        } catch (XPathException err) {
            // can't actually happen
            return StaticProperty.ALLOWS_ZERO_OR_MORE;
        }
    }

    /**
     * Compute the static properties of this expression (other than its type). For a
     * Value, the only special property is {@link StaticProperty#NON_CREATIVE}.
     * @return the value {@link StaticProperty#NON_CREATIVE}
     */


    public int computeSpecialProperties() {
        if (getValue() instanceof EmptySequence) {
            // An empty sequence has all special properties except "has side effects".
            return StaticProperty.SPECIAL_PROPERTY_MASK &~ StaticProperty.HAS_SIDE_EFFECTS;
        }
        return StaticProperty.NON_CREATIVE;
    }

    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    public Expression copy() {
        return new Literal(value);
    }

    /**
     * Add a representation of this expression to a PathMap. The PathMap captures a map of the nodes visited
     * by an expression in a source tree.
     *
     * @param pathMap     the PathMap to which the expression should be added
     * @param pathMapNodeSet
     * @return the pathMapNode representing the focus established by this expression, in the case where this
     *         expression is the first operand of a path expression or filter expression
     */

    public PathMap.PathMapNodeSet addToPathMap(PathMap pathMap, PathMap.PathMapNodeSet pathMapNodeSet) {
        return pathMapNodeSet;
    }

    /**
    * Determine which aspects of the context the expression depends on. The result is
    * a bitwise-or'ed value composed from constants such as StaticProperty.VARIABLES and
    * StaticProperty.CURRENT_NODE
     * @return for a Value, this always returns zero.
    */

    public final int getDependencies() {
        return 0;
    }

    /**
     * Return an Iterator to iterate over the values of a sequence. The value of every
     * expression can be regarded as a sequence, so this method is supported for all
     * expressions. This default implementation handles iteration for expressions that
     * return singleton values: for non-singleton expressions, the subclass must
     * provide its own implementation.
     *
     * @param context supplies the context for evaluation
     * @return a SequenceIterator that can be used to iterate over the result
     *         of the expression
     * @throws org.orbeon.saxon.trans.XPathException
     *          if any dynamic error occurs evaluating the
     *          expression
     */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        return value.iterate();
    }

    /**
     * Evaluate as a singleton item (or empty sequence). Note: this implementation returns
     * the first item in the sequence. The method should not be used unless appropriate type-checking
     * has been done to ensure that the value will be a singleton.
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        if (value instanceof AtomicValue) {
            return (AtomicValue)value;
        }
        return value.iterate().next();
    }


    /**
      * Process the value as an instruction, without returning any tail calls
      * @param context The dynamic context, giving access to the current node,
      * the current variables, etc.
      */

    public void process(XPathContext context) throws XPathException {
        SequenceIterator iter = value.iterate();
        SequenceReceiver out = context.getReceiver();
        while (true) {
            Item it = iter.next();
            if (it==null) break;
            out.append(it, 0, NodeInfo.ALL_NAMESPACES);
        }
    }

    /*
     * Evaluate an expression as a String. This function must only be called in contexts
     * where it is known that the expression will return a single string (or where an empty sequence
     * is to be treated as a zero-length string). Implementations should not attempt to convert
     * the result to a string, other than converting () to "". This method is used mainly to
     * evaluate expressions produced by compiling an attribute value template.
     *
     * @exception org.orbeon.saxon.trans.XPathException if any dynamic error occurs evaluating the
     *     expression
     * @exception ClassCastException if the result type of the
     *     expression is not xs:string?
     * @param context The context in which the expression is to be evaluated
     * @return the value of the expression, evaluated in the current context.
     *     The expression must return a string or (); if the value of the
     *     expression is (), this method returns "".
     */

    public CharSequence evaluateAsString(XPathContext context) throws XPathException {
        AtomicValue value = (AtomicValue) evaluateItem(context);
        if (value == null) return "";
        return value.getStringValueCS();
    }


    /**
     * Get the effective boolean value of the expression. This returns false if the value
     * is the empty sequence, a zero-length string, a number equal to zero, or the boolean
     * false. Otherwise it returns true.
     *
     * @param context The context in which the expression is to be evaluated
     * @exception org.orbeon.saxon.trans.XPathException if any dynamic error occurs evaluating the
     *     expression
     * @return the effective boolean value
     */

    public boolean effectiveBooleanValue(XPathContext context) throws XPathException {
        return value.effectiveBooleanValue();
    }


    /**
     * Evaluate an updating expression, adding the results to a Pending Update List.
     * The default implementation of this method, which is used for non-updating expressions,
     * throws an UnsupportedOperationException. The implementation for a literal representing
     * an empty sequence, however, is a no-op.
     *
     * @param context the XPath dynamic evaluation context
     * @param pul     the pending update list to which the results should be written
     */

    public void evaluatePendingUpdates(XPathContext context, PendingUpdateList pul) throws XPathException {
        if (value instanceof EmptySequence) {
            // do nothing
        } else {
            super.evaluatePendingUpdates(context, pul);
        }
    }

    /**
     * Determine whether two literals are equal, when considered as expressions.
     * @param obj the other expression
     * @return true if the two literals are equal
     */

    public boolean equals(Object obj) {
        if (!(obj instanceof Literal)) {
            return false;
        }
        Value v = ((Literal)obj).value;
        return value.getSchemaComparable().equals(v.getSchemaComparable());
    }

    /**
     * Return a hash code to support the equals() function
     */

    public int hashCode() {
        return value.getSchemaComparable().hashCode();
    }

    /**
     * The toString() method for an expression attempts to give a representation of the expression
     * in an XPath-like form, but there is no guarantee that the syntax will actually be true XPath.
     * In the case of XSLT instructions, the toString() method gives an abstracted view of the syntax
     */

    public String toString() {
        return value.toString();
    }

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void explain(ExpressionPresenter out) {
        out.startElement("literal");
        if (value instanceof EmptySequence) {
            out.emitAttribute("value", "()");
        } else if (value instanceof AtomicValue) {
            //noinspection RedundantCast
            out.emitAttribute("value", ((AtomicValue)value).getStringValue());
                    // cast is needed to tell the compiler there's no exception possible
            out.emitAttribute("type", ((AtomicValue)value).getTypeLabel().getDisplayName());
        } else {
            try {
                out.emitAttribute("count", value.getLength()+"");
                if (value.getLength() < 20) {
                    SequenceIterator iter = iterate(null);
                    while (true) {
                        Item it = iter.next();
                        if (it == null) {
                            break;
                        }
                        if (it instanceof NodeInfo) {
                            out.startElement("node");
                            out.emitAttribute("path", Navigator.getPath(((NodeInfo)it)));
                            out.emitAttribute("uri", ((NodeInfo)it).getSystemId());
                            out.endElement();
                        } else {
                            out.startElement("atomicValue");
                            out.emitAttribute("value", it.getStringValue());
                            out.emitAttribute("type", ((AtomicValue)it).getTypeLabel().getDisplayName());
                            out.endElement();
                        }
                    }
                }
            } catch (XPathException err) {
                //
            }
        }
        out.endElement();
    }

    /**
     * Test whether the literal wraps an atomic value. (Note, if this method returns false,
     * this still leaves the possibility that the literal wraps a sequence that happens to contain
     * a single atomic value).
     * @param exp an expression
     * @return if the expression is a literal and the literal wraps an AtomicValue
     */

    public static boolean isAtomic(Expression exp) {
        return exp instanceof Literal && ((Literal)exp).getValue() instanceof AtomicValue;
    }

    /**
     * Test whether the literal explicitly wraps an empty sequence. (Note, if this method returns false,
     * this still leaves the possibility that the literal wraps a sequence that happens to be empty).
     * @param exp an expression
     * @return if the expression is a literal and the literal wraps an AtomicValue
     */

    public static boolean isEmptySequence(Expression exp) {
        return exp instanceof Literal && ((Literal)exp).getValue() instanceof EmptySequence;
    }

    /**
     * Test if a literal represents the boolean value true
     * @param exp an expression
     * @param value true or false
     * @return if the expression is a literal and the literal represents the boolean value given in the
     * second argument
     */

    public static boolean isConstantBoolean(Expression exp, boolean value) {
        if (exp instanceof Literal) {
            Value b = ((Literal)exp).getValue();
            return (b instanceof BooleanValue && ((BooleanValue)b).getBooleanValue() == value);
        }
        return false;
    }

    /**
     * Test if a literal represents the integer value 1
     * @param exp an expression
     * @return if the expression is a literal and the literal represents the integer value 1
     */

    public static boolean isConstantOne(Expression exp) {
        if (exp instanceof Literal) {
            Value v = ((Literal)exp).getValue();
            return (v instanceof Int64Value && ((Int64Value)v).longValue() == 1);
        }
        return false;
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
