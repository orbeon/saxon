package org.orbeon.saxon.sort;

import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.trace.ExpressionPresenter;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.TypeHierarchy;

import java.util.Iterator;

/**
 * An expression that sorts an underlying sequence into document order if some condition is true, or that
 * returns the sequence "as is" (knowing that it doesn't need sorting) if the condition is false.
 */
public class ConditionalSorter extends Expression {

    private Expression condition;
    private DocumentSorter documentSorter;

    /**
     * Create a conditional document sorter
     * @param condition the conditional expression
     * @param sorter the sorting expression
     */

    public ConditionalSorter(Expression condition, DocumentSorter sorter) {
        this.condition = condition;
        documentSorter = sorter;
    }

    /**
     * Get the condition under which the nodes need to be sorted
     * @return the condition (an expression)
     */

    public Expression getCondition() {
        return condition;
    }

    /**
     * Get the document sorter, which sorts the nodes if the condition is true
     * @return  the document sorter
     */

    public DocumentSorter getDocumentSorter() {
        return documentSorter;
    }


    /**
     * Determine the static cardinality of the expression. This establishes how many items
     * there will be in the result of the expression, at compile time (i.e., without
     * actually evaluating the result.
     *
     * @return one of the values Cardinality.ONE_OR_MORE,
     *         Cardinality.ZERO_OR_MORE, Cardinality.EXACTLY_ONE,
     *         Cardinality.ZERO_OR_ONE, Cardinality.EMPTY. This default
     *         implementation returns ZERO_OR_MORE (which effectively gives no
     *         information).
     */

    public int getCardinality() {
        return documentSorter.getCardinality();
    }


    /**
     * Compute the special properties of this expression. These properties are denoted by a bit-significant
     * integer, possible values are in class {@link org.orbeon.saxon.expr.StaticProperty}. The "special" properties are properties
     * other than cardinality and dependencies, and most of them relate to properties of node sequences, for
     * example whether the nodes are in document order.
     *
     * @return the special properties, as a bit-significant integer
     */

    protected int computeSpecialProperties() {
        return condition.getSpecialProperties()
                | StaticProperty.ORDERED_NODESET
                & ~StaticProperty.REVERSE_DOCUMENT_ORDER;
    }

    /**
     * An implementation of Expression must provide at least one of the methods evaluateItem(), iterate(), or process().
     * This method indicates which of these methods is provided directly. The other methods will always be available
     * indirectly, using an implementation that relies on one of the other methods.
     *
     * @return the implementation method, for example {@link #ITERATE_METHOD} or {@link #EVALUATE_METHOD} or
     *         {@link #PROCESS_METHOD}
     */

    public int getImplementationMethod() {
        return ITERATE_METHOD;
    }


    /**
     * Get the immediate sub-expressions of this expression. Default implementation
     * returns a zero-length array, appropriate for an expression that has no
     * sub-expressions.
     *
     * @return an iterator containing the sub-expressions of this expression
     */

    public Iterator iterateSubExpressions() {
        return new PairIterator(condition, documentSorter);
    }

    /**
      * Replace one subexpression by a replacement subexpression
      * @param original the original subexpression
      * @param replacement the replacement subexpression
      * @return true if the original subexpression is found
      */

    public boolean replaceSubExpression(Expression original, Expression replacement) {
                boolean found = false;
        if (condition == original) {
            condition = replacement;
            found = true;
        }
        if (documentSorter == original) {
            documentSorter = (DocumentSorter)replacement;
            found = true;
        }
        return found;
    }

    /**
     * Compute the static cardinality of this expression
     *
     * @return the computed cardinality, as one of the values {@link org.orbeon.saxon.expr.StaticProperty#ALLOWS_ZERO_OR_ONE},
     *         {@link org.orbeon.saxon.expr.StaticProperty#EXACTLY_ONE}, {@link org.orbeon.saxon.expr.StaticProperty#ALLOWS_ONE_OR_MORE},
     *         {@link org.orbeon.saxon.expr.StaticProperty#ALLOWS_ZERO_OR_MORE}
     */

    protected int computeCardinality() {
        return StaticProperty.ALLOWS_ZERO_OR_MORE;
    }

    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    public Expression copy() {
        return new ConditionalSorter(condition.copy(), (DocumentSorter)documentSorter.copy());
    }

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     *
     * @param out the expression presenter used to display the structure
     */

    public void explain(ExpressionPresenter out) {
        out.startElement("conditionalSort");
        condition.explain(out);
        documentSorter.explain(out);
        out.endElement();
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
     * @param th the type hierarchy cache
     * @return a value such as Type.STRING, Type.BOOLEAN, Type.NUMBER,
     *         Type.NODE, or Type.ITEM (meaning not known at compile time)
     */

    public ItemType getItemType(TypeHierarchy th) {
        return documentSorter.getItemType(th);
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
        if (exp != null) {
            return exp;
        } else {
            condition = doPromotion(condition, offer);
            Expression e = doPromotion(documentSorter, offer);
            if (e instanceof DocumentSorter) {
                return this;
            } else {
                return e;
            }
        }
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
        boolean b = condition.effectiveBooleanValue(context);
        if (b) {
            return documentSorter.iterate(context);
        } else {
            return documentSorter.getBaseExpression().iterate(context);
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
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//

