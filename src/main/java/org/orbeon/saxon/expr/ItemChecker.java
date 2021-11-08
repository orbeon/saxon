package org.orbeon.saxon.expr;
import org.orbeon.saxon.event.SequenceReceiver;
import org.orbeon.saxon.event.TypeCheckingFilter;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.pattern.DocumentNodeTest;
import org.orbeon.saxon.pattern.NodeTest;
import org.orbeon.saxon.pattern.CombinedNodeTest;
import org.orbeon.saxon.trace.ExpressionPresenter;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.*;
import org.orbeon.saxon.value.Cardinality;
import org.orbeon.saxon.value.Value;

/**
* A ItemChecker implements the item type checking of "treat as": that is,
* it returns the supplied sequence, checking that all its items are of the correct type
*/

public final class ItemChecker extends UnaryExpression {

    private ItemType requiredItemType;
    private RoleLocator role;

    /**
     * Constructor
     * @param sequence the expression whose value we are checking
     * @param itemType the required type of the items in the sequence
     * @param role information used in constructing an error message
    */

    public ItemChecker(Expression sequence, ItemType itemType, RoleLocator role) {
        super(sequence);
        requiredItemType = itemType;
        this.role = role;
        adoptChildExpression(sequence);
    }

    /**
     * Get the required type
     * @return the required type of the items in the sequence
     */

    public ItemType getRequiredType() {
        return requiredItemType;
    }

    /**
     * Get the RoleLocator (used to construct error messages)
     * @return the RoleLocator
     */

    public RoleLocator getRoleLocator() {
        return role;
    }

    /**
    * Simplify an expression
     * @param visitor an expression visitor
     */

     public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        operand = visitor.simplify(operand);
        if (requiredItemType instanceof AnyItemType) {
            return operand;
        }
        return this;
    }

    /**
    * Type-check the expression
    */

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        operand = visitor.typeCheck(operand, contextItemType);
        // When typeCheck is called a second time, we might have more information...

        final TypeHierarchy th = visitor.getConfiguration().getTypeHierarchy();
        int card = operand.getCardinality();
        if (card == StaticProperty.EMPTY) {
            //value is always empty, so no item checking needed
            return operand;
        }
        ItemType supplied = operand.getItemType(th);
        int relation = th.relationship(requiredItemType, supplied);
        if (relation == TypeHierarchy.SAME_TYPE || relation == TypeHierarchy.SUBSUMES) {
            return operand;
        } else if (relation == TypeHierarchy.DISJOINT) {
            final NamePool namePool = visitor.getConfiguration().getNamePool();
            if (Cardinality.allowsZero(card)) {

                String message = role.composeErrorMessage(
                        requiredItemType, operand.getItemType(th), namePool);
                visitor.getStaticContext().issueWarning("The only value that can pass type-checking is an empty sequence. " +
                        message, this);
            } else if (requiredItemType.equals(BuiltInAtomicType.STRING) && th.isSubType(supplied, BuiltInAtomicType.ANY_URI)) {
                // URI promotion will take care of this at run-time
                return operand;
            } else {
                String message = role.composeErrorMessage(requiredItemType, operand.getItemType(th), namePool);
                XPathException err = new XPathException(message);
                err.setErrorCode(role.getErrorCode());
                err.setLocator(this);
                err.setIsTypeError(true);
                throw err;
            }
        }
        return this;
    }

    /**
     * An implementation of Expression must provide at least one of the methods evaluateItem(), iterate(), or process().
     * This method indicates which of these methods is provided. This implementation provides both iterate() and
     * process() methods natively.
     */

    public int getImplementationMethod() {
        int m = ITERATE_METHOD | PROCESS_METHOD;
        if (!Cardinality.allowsMany(getCardinality())) {
            m |= EVALUATE_METHOD;
        }
        return m;
    }


    /**
    * Iterate over the sequence of values
    */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        SequenceIterator base = operand.iterate(context);
        ItemCheckMappingFunction map = new ItemCheckMappingFunction();
        map.externalContext = context;
        return new ItemMappingIterator(base, map);
    }

    /**
    * Mapping function: this is used only if the expression does not allow a sequence of more than
    * one item.
    */
    private class ItemCheckMappingFunction implements ItemMappingFunction {
        public XPathContext externalContext;
        public Item map(Item item) throws XPathException {
            testConformance(item, externalContext);
            return item;
        }
    }

    /**
    * Evaluate as an Item.
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        Item item = operand.evaluateItem(context);
        if (item==null) return null;
        testConformance(item, context);
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
        int card = StaticProperty.ALLOWS_ZERO_OR_MORE;
        if (next instanceof CardinalityChecker) {
            card = ((CardinalityChecker)next).getRequiredCardinality();
            next = ((CardinalityChecker)next).getBaseExpression();
        }
        if ((next.getImplementationMethod() & PROCESS_METHOD) != 0 && !(requiredItemType instanceof DocumentNodeTest)) {
            SequenceReceiver out = context.getReceiver();
            TypeCheckingFilter filter = new TypeCheckingFilter();
            filter.setUnderlyingReceiver(out);
            filter.setPipelineConfiguration(out.getPipelineConfiguration());
            filter.setRequiredType(requiredItemType, card, role);
            context.setReceiver(filter);
            next.process(context);
            filter.close();
            context.setReceiver(out);
        } else {
            super.process(context);
        }
    }

    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    public Expression copy() {
        return new ItemChecker(getBaseExpression().copy(), requiredItemType, role);
    }


    private void testConformance(Item item, XPathContext context) throws XPathException {
        if (!requiredItemType.matchesItem(item, true, (context == null ? null : context.getConfiguration()))) {
            String message;
            if (context == null) {
                // no name pool available
                message = "Supplied value of type " + Type.displayTypeName(item) +
                        " does not match the required type of " + role.getMessage();
            } else {
                final NamePool pool = context.getNamePool();
                final TypeHierarchy th = context.getConfiguration().getTypeHierarchy();
                message = role.composeErrorMessage(requiredItemType, Value.asValue(item).getItemType(th), pool);
            }
            String errorCode = role.getErrorCode();
            if ("XPDY0050".equals(errorCode)) {
                // error in "treat as" assertion
                dynamicError(message, errorCode, context);
            } else {
                typeError(message, errorCode, context);
            }
        }
    }

    /**
     * Determine the data type of the items returned by the expression
     * @param th the type hierarchy cache
     */

	public ItemType getItemType(TypeHierarchy th) {
        ItemType operandType = operand.getItemType(th);
        int relationship = th.relationship(requiredItemType, operandType);
        switch (relationship) {
            case TypeHierarchy.OVERLAPS:
                if (requiredItemType instanceof NodeTest && operandType instanceof NodeTest) {
                    return new CombinedNodeTest((NodeTest)requiredItemType, Token.INTERSECT, (NodeTest)operandType);
                } else {
                    // we don't know how to intersect atomic types, it doesn't actually happen
                    return requiredItemType;
                }

            case TypeHierarchy.SUBSUMES:
            case TypeHierarchy.SAME_TYPE:
                // shouldn't happen, but it doesn't matter
                return operandType;
            case TypeHierarchy.SUBSUMED_BY:
            default:
                return requiredItemType;
        }
    }

    /**
     * Is this expression the same as another expression?
     */

    public boolean equals(Object other) {
        return super.equals(other) &&
                requiredItemType == ((ItemChecker)other).requiredItemType;
    }

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void explain(ExpressionPresenter out) {
        out.startElement("treat");
        out.emitAttribute("as", requiredItemType.toString(out.getConfiguration().getNamePool()));
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
