package org.orbeon.saxon.instruct;

import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.type.BuiltInAtomicType;
import org.orbeon.saxon.value.AtomicValue;
import org.orbeon.saxon.value.Cardinality;
import org.orbeon.saxon.value.StringValue;
import org.orbeon.saxon.value.Value;
import org.orbeon.saxon.trace.ExpressionPresenter;

import java.util.Iterator;

/**
 * This class implements the rules for an XSLT (or XQuery) simple content constructor, which are used in constructing
 * the string value of an attribute node, text node, comment node, etc, from the value of the select
 * expression or the contained sequence constructor.
 */

public class SimpleContentConstructor extends Expression {

    Expression select;
    Expression separator;
    boolean isSingleton = false;
    boolean isAtomic = false;

    /**
     * Create a SimpleContentConstructor
     * @param select the select expression (which computes a sequence of strings)
     * @param separator the separator expression (which computes a value to separate adjacent strings)
     */

    public SimpleContentConstructor(Expression select, Expression separator) {
        this.select = select;
        this.separator = separator;
        adoptChildExpression(select);
        adoptChildExpression(separator);
        select.setFlattened(true);
    }

    /**
     * Get the select expression
     * @return the select expression
     */

    public Expression getSelectExpression() {
        return select;
    }

    /**
     * Get the separator expression
     * @return the separator expression
     */

    public Expression getSeparatorExpression() {
        return separator;
    }

    /**
     * Determine whether the select expression is a singleton (an expression returning zero or one items)
     * @return true if the select expression will always be of length zero or one
     */

    public boolean isSingleton() {
        return isSingleton;
    }

    /**
     * Determine if the select expression is atomic
     * @return true if the select expression always returns atomic values
     */

    public boolean isAtomic() {
        return isAtomic;
    }

    /**
     * Compute the cardinality of the result of the expression.
     * @return the cardinality, @link {StaticProperty.EXACTLY_ONE}
     */

    protected int computeCardinality() {
        return StaticProperty.EXACTLY_ONE;

    }

    /**
     * Copy an expression. This makes a deep copy.
     * @return the copy of the original expression
     */

    public Expression copy() {
        return new SimpleContentConstructor(select.copy(), separator.copy());
    }

    /**
     * Simplify an expression. This performs any static optimization (by rewriting the expression
     * as a different expression). The default implementation does nothing.
     *
     * @return the simplified expression
     * @throws org.orbeon.saxon.trans.XPathException
     *          if an error is discovered during expression
     *          rewriting
     * @param visitor an expression visitor
     */

    public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        select = visitor.simplify(select);
        if (select instanceof Literal && ((Literal)select).getValue() instanceof AtomicValue) {
            return select;
        }
        separator = visitor.simplify(separator);
        return this;
    }


    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        select = visitor.typeCheck(select, contextItemType);
        separator = visitor.typeCheck(separator, contextItemType);
        if (!Cardinality.allowsMany(select.getCardinality())) {
            isSingleton = true;
        }
        final TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();
        if (select.getItemType(th).isAtomicType()) {
            isAtomic = true;
        }
        select.setFlattened(true);
        if (select instanceof Literal && separator instanceof Literal) {
            XPathContext c = visitor.getStaticContext().makeEarlyEvaluationContext();
            return new Literal(Value.asValue(evaluateItem(c)));
        }
        return this;
    }

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        select = visitor.optimize(select, contextItemType);
        separator = visitor.optimize(separator, contextItemType);
        if (select instanceof Literal && separator instanceof Literal) {
            XPathContext c = visitor.getStaticContext().makeEarlyEvaluationContext();
            return Literal.makeLiteral(Value.asValue(evaluateItem(c)));
        }
        return this;
    }

    /**
     * Determine the data type of the expression, if possible. All expression return
     * sequences, in general; this method determines the type of the items within the
     * sequence, assuming that (a) this is known in advance, and (b) it is the same for
     * all items in the sequence.
     * <p/>
     * <p>This method should always return a result, though it may be the best approximation
     * that is available at the time.</p>
     *
     * @return a value such as Type.STRING, Type.BOOLEAN, Type.NUMBER,
     *         Type.NODE, or Type.ITEM (meaning not known at compile time)
     * @param th the type hierarchy cache
     */

    public ItemType getItemType(TypeHierarchy th) {
        return BuiltInAtomicType.STRING;
    }

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void explain(ExpressionPresenter out) {
        out.startElement("simpleContentConstructor");
        select.explain(out);
        separator.explain(out);
        out.endElement();
    }


     /**
     * Get the immediate sub-expressions of this expression. Default implementation
     * returns a zero-length array, appropriate for an expression that has no
     * sub-expressions.
     *
     * @return an iterator containing the sub-expressions of this expression
     */

    public Iterator iterateSubExpressions() {
        return new PairIterator(select, separator);
    }

    /**
      * Replace one subexpression by a replacement subexpression
      * @param original the original subexpression
      * @param replacement the replacement subexpression
      * @return true if the original subexpression is found
      */

     public boolean replaceSubExpression(Expression original, Expression replacement) {
         boolean found = false;
         if (select == original) {
             select = replacement;
             found = true;
         }
         if (separator == original) {
             separator = replacement;
             found = true;
         }
         return found;
     }


    /**
     * Offer promotion for this subexpression. The offer will be accepted if the subexpression
     * is not dependent on the factors (e.g. the context item) identified in the PromotionOffer.
     * By default the offer is not accepted - this is appropriate in the case of simple expressions
     * such as constant values and variable references where promotion would give no performance
     * advantage. This method is always called at compile time.
     *
     * @param offer details of the offer, for example the offer to move
     *              expressions that don't depend on the context to an outer level in
     *              the containing expression
     * @return if the offer is not accepted, return this expression unchanged.
     *         Otherwise return the result of rewriting the expression to promote
     *         this subexpression
     * @throws org.orbeon.saxon.trans.XPathException
     *          if any error is detected
     */

    public Expression promote(PromotionOffer offer) throws XPathException {
        Expression exp = offer.accept(this);
        if (exp!=null) {
            return exp;
        } else {
            select = doPromotion(select, offer);
            separator = doPromotion(separator, offer);
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
     * @return the node or atomic value that results from evaluating the
     *         expression; or null to indicate that the result is an empty
     *         sequence
     * @throws org.orbeon.saxon.trans.XPathException
     *          if any dynamic error occurs evaluating the
     *          expression
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        SequenceIterator iter;
        if (isSingleton) {
            // optimize for this case
            Item item = select.evaluateItem(context);
            if (item == null || item instanceof StringValue) {
                return item;
            } else if (item instanceof AtomicValue) {
                return ((AtomicValue)item).convert(BuiltInAtomicType.STRING, true, context).asAtomic();
            } else {
                iter = SingletonIterator.makeIterator(item);
            }
        } else {
            iter = select.iterate(context);
        }
        FastStringBuffer sb = new FastStringBuffer(1024);
        boolean prevText = false;
        boolean first = true;
        CharSequence sep = null;
        while (true) {
            Item item = iter.next();
            if (item==null) {
                break;
            }
            if (item instanceof NodeInfo) {
                if (((NodeInfo)item).getNodeKind() == Type.TEXT) {
                    CharSequence s = item.getStringValueCS();
                    if (s.length() > 0) {
                        if (!first && !prevText) {
                            if (sep == null) {
                                sep = separator.evaluateItem(context).getStringValueCS();
                            }
                            sb.append(sep);
                        }
                        first = false;
                        sb.append(s);
                        prevText = true;
                    }
                } else {
                    prevText = false;
                    SequenceIterator iter2 = item.getTypedValue();
                    while (true) {
                        Item item2 = iter2.next();
                        if (item2 == null) {
                            break;
                        }
                        if (!first) {
                            if (sep == null) {
                                sep = separator.evaluateItem(context).getStringValueCS();
                            }
                            sb.append(sep);
                        }
                        first = false;
                        sb.append(item2.getStringValueCS());
                    }
                }
            } else {
                if (!first) {
                    if (sep == null) {
                        sep = separator.evaluateItem(context).getStringValueCS();
                    }
                    sb.append(sep);
                }
                first = false;
                prevText = false;
                sb.append(item.getStringValueCS());
            }
        }
        return StringValue.makeStringValue(sb.condense());
    }

    /**
     * An implementation of Expression must provide at least one of the methods evaluateItem(), iterate(), or process().
     * This method indicates which of these methods is prefered.
     */

    public int getImplementationMethod() {
        return Expression.EVALUATE_METHOD;
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
