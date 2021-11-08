package org.orbeon.saxon.expr;
import org.orbeon.saxon.trans.Err;
import org.orbeon.saxon.event.SequenceReceiver;
import org.orbeon.saxon.event.TypeCheckingFilter;
import org.orbeon.saxon.om.FastStringBuffer;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.pattern.DocumentNodeTest;
import org.orbeon.saxon.trace.ExpressionPresenter;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.value.Cardinality;

/**
* A CardinalityChecker implements the cardinality checking of "treat as": that is,
* it returns the supplied sequence, checking that its cardinality is correct
*/

public final class CardinalityChecker extends UnaryExpression {

    private int requiredCardinality = -1;
    private RoleLocator role;

    /**
     * Private Constructor: use factory method
     * @param sequence the base sequence whose cardinality is to be checked
     * @param cardinality the required cardinality
     * @param role information to be used in error reporting
    */

    private CardinalityChecker(Expression sequence, int cardinality, RoleLocator role) {
        super(sequence);
        requiredCardinality = cardinality;
        this.role = role;
        computeStaticProperties();
        adoptChildExpression(sequence);
    }

    /**
     * Factory method to construct a CardinalityChecker. The method may create an expression that combines
     * the cardinality checking with the functionality of the underlying expression class
     * @param sequence the base sequence whose cardinality is to be checked
     * @param cardinality the required cardinality
     * @param role information to be used in error reporting
     * @return a new Expression that does the CardinalityChecking (not necessarily a CardinalityChecker)
     */

    public static Expression makeCardinalityChecker(Expression sequence, int cardinality, RoleLocator role) {
        Expression result;
        if (sequence instanceof Atomizer && !Cardinality.allowsMany(cardinality)) {
            Expression base = ((Atomizer)sequence).getBaseExpression();
            result = new SingletonAtomizer(base, role, Cardinality.allowsZero(cardinality));
        } else {
            result = new CardinalityChecker(sequence, cardinality, role);
        }
        ExpressionTool.copyLocationInfo(sequence, result);
        return result;
    }

    /**
     * Get the required cardinality
     * @return the cardinality required by this checker
     */

    public int getRequiredCardinality() {
        return requiredCardinality;
    }

    /**
    * Type-check the expression
    */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        operand = visitor.typeCheck(operand, contextItemType);
        if (requiredCardinality == StaticProperty.ALLOWS_ZERO_OR_MORE ||
                    Cardinality.subsumes(requiredCardinality, operand.getCardinality())) {
            return operand;
        }
        return this;
    }

    /**
     * Perform optimisation of an expression and its subexpressions.
     * <p/>
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable, and after all type checking has been done.</p>
     *
     * @param visitor an expression visitor
     * @param contextItemType the static type of "." at the point where this expression is invoked.
     *                        The parameter is set to null if it is known statically that the context item will be undefined.
     *                        If the type of the context item is not known statically, the argument is set to
     *                        {@link org.orbeon.saxon.type.Type#ITEM_TYPE}
     * @return the original expression, rewritten if appropriate to optimize execution
     * @throws XPathException if an error is discovered during this phase
     *                                        (typically a type error)
     */

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        operand = visitor.optimize(operand, contextItemType);
        if (requiredCardinality == StaticProperty.ALLOWS_ZERO_OR_MORE ||
                Cardinality.subsumes(requiredCardinality, operand.getCardinality())) {
            return operand;
        }
        return this;
    }


    /**
     * Set the error code to be returned (this is used when evaluating the functions such
     * as exactly-one() which have their own error codes)
     * @param code the error code to be used
     */

    public void setErrorCode(String code) {
        role.setErrorCode(code);
    }

    /**
     * Get the RoleLocator, which contains diagnostic information for use if the cardinality check fails
     * @return the diagnostic information
     */

    public RoleLocator getRoleLocator() {
        return role;
    }

    /**
     * An implementation of Expression must provide at least one of the methods evaluateItem(), iterate(), or process().
     * This method indicates which of these methods is provided. This implementation provides both iterate() and
     * process() methods natively.
     */

    public int getImplementationMethod() {
        int m = ITERATE_METHOD | PROCESS_METHOD;
        if (!Cardinality.allowsMany(requiredCardinality)) {
            m |= EVALUATE_METHOD;
        }
        return m;
    }


    /**
    * Iterate over the sequence of values
    */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        SequenceIterator base = operand.iterate(context);

        // If the base iterator knows how many items there are, then check it now rather than wasting time

        if ((base.getProperties() & SequenceIterator.LAST_POSITION_FINDER) != 0) {
            int count = ((LastPositionFinder)base).getLastPosition();
            if (count == 0 && !Cardinality.allowsZero(requiredCardinality)) {
                typeError("An empty sequence is not allowed as the " +
                             role.getMessage(), role.getErrorCode(), context);
            } else if (count == 1 && requiredCardinality == StaticProperty.EMPTY) {
                typeError("The only value allowed for the " +
                             role.getMessage() + " is an empty sequence", role.getErrorCode(), context);
            } else if (count > 1 && !Cardinality.allowsMany(requiredCardinality)) {
                typeError("A sequence of more than one item is not allowed as the " +
                                role.getMessage() + depictSequenceStart(base.getAnother(), 2),
                           role.getErrorCode(), context);
            }
            return base;
        }

        // Otherwise return an iterator that does the checking on the fly

        return new CardinalityCheckingIterator(base, requiredCardinality, role, this);

    }

    /**
     * Show the first couple of items in a sequence in an error message
     * @param seq iterator over the sequence
     * @param max maximum number of items to be shown
     * @return a message display of the contents of the sequence
     */

    public static String depictSequenceStart(SequenceIterator seq, int max) {
        try {
            FastStringBuffer sb = new FastStringBuffer(100);
            int count = 0;
            sb.append(" (");
            while (true) {
                Item next = seq.next();
                if (next == null) {
                    sb.append(") ");
                    return sb.toString();
                }
                if (count++ > 0) {
                    sb.append(", ");
                }
                if (count > max) {
                    sb.append("...) ");
                    return sb.toString();
                }

                sb.append(Err.depict(next));
            }
        } catch (XPathException e) {
            return "";
        }
    }

    /**
    * Mapping function used to check for sequences of length > 1 when this is not permitted
    */

//    private class CardinalityCheckingFunction implements ItemMappingFunction {
//
//        public SequenceIterator iterator;
//        public XPathContext context;
//
//        public CardinalityCheckingFunction(XPathContext context) {
//            this.context = context;
//        }
//
//        public Item map(Item item) throws XPathException {
//            if (iterator.position()==2) {
//                typeError(
//                        "A sequence of more than one item is not allowed as the " +
//                        role.getMessage() + depictSequenceStart(iterator.getAnother(), 2),
//                        role.getErrorCode(), context);
//                return null;
//            }
//            return item;
//        }
//    }

    /**
     * Mapping function used to check that a sequence is empty
     */

//    private class EmptyCheckingFunction implements ItemMappingFunction {
//
//        public SequenceIterator iterator;
//        public XPathContext context;
//
//        public EmptyCheckingFunction(XPathContext context) {
//            this.context = context;
//        }
//
//        public Item map(Item item) throws XPathException {
//            typeError("An empty sequence is required as the " +
//                    role.getMessage(), role.getErrorCode(), context);
//            return null;
//        }
//    }


    /**
    * Evaluate as an Item.
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        SequenceIterator iter = operand.iterate(context);
        Item item = null;
        while (true) {
            Item nextItem = iter.next();
            if (nextItem == null) break;
            if (requiredCardinality == StaticProperty.EMPTY) {
                typeError("An empty sequence is required as the " +
                    role.getMessage(), role.getErrorCode(), context);
                return null;
            }
            if (item != null) {
                typeError("A sequence of more than one item is not allowed as the " +
                    role.getMessage() + depictSequenceStart(iter.getAnother(), 2), role.getErrorCode(), context);
                return null;
            }
            item = nextItem;
        }
        if (item == null && !Cardinality.allowsZero(requiredCardinality)) {
            typeError("An empty sequence is not allowed as the " +
                    role.getMessage(), role.getErrorCode(), context);
            return null;
        }
        return item;
    }

    /**
     * Process the instruction, without returning any tail calls
     *
     * @param context The dynamic context, giving access to the current node,
     *                the current variables, etc.
     */

    public void process(XPathContext context) throws XPathException {
        Expression next = operand;
        ItemType type = Type.ITEM_TYPE;
        if (next instanceof ItemChecker) {
            type = ((ItemChecker)next).getRequiredType();
            next = ((ItemChecker)next).getBaseExpression();
        }
        if ((next.getImplementationMethod() & PROCESS_METHOD) != 0 && !(type instanceof DocumentNodeTest)) {
            SequenceReceiver out = context.getReceiver();
            TypeCheckingFilter filter = new TypeCheckingFilter();
            filter.setUnderlyingReceiver(out);
            filter.setPipelineConfiguration(out.getPipelineConfiguration());
            filter.setRequiredType(type, requiredCardinality, role);
            context.setReceiver(filter);
            next.process(context);
            filter.close();
            context.setReceiver(out);
        } else {
            super.process(context);
        }
    }

    /**
    * Determine the data type of the items returned by the expression, if possible
    * @return a value such as Type.STRING, Type.BOOLEAN, Type.NUMBER, Type.NODE,
    * or Type.ITEM (meaning not known in advance)
     * @param th the type hierarchy cache
     */

	public ItemType getItemType(TypeHierarchy th) {
	    return operand.getItemType(th);
	}

	/**
	* Determine the static cardinality of the expression
	*/

	public int computeCardinality() {
        return requiredCardinality;
	}

    /**
    * Get the static properties of this expression (other than its type). The result is
    * bit-signficant. These properties are used for optimizations. In general, if
    * property bit is set, it is true, but if it is unset, the value is unknown.
    */

    public int computeSpecialProperties() {
        return operand.getSpecialProperties();
    }

    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    public Expression copy() {
        return new CardinalityChecker(getBaseExpression().copy(), requiredCardinality, role);
    }

    /**
    * Is this expression the same as another expression?
    */

    public boolean equals(Object other) {
        return super.equals(other) &&
                requiredCardinality == ((CardinalityChecker)other).requiredCardinality;
    }

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void explain(ExpressionPresenter out) {
        out.startElement("checkCardinality");
        out.emitAttribute("occurs", Cardinality.toString(requiredCardinality));
        operand.explain(out);
        out.endElement();
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
// Contributor(s): none.
//
