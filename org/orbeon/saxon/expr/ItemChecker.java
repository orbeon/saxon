package net.sf.saxon.expr;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.StaticError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.AnyItemType;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;

/**
* A ItemChecker implements the item type checking of "treat as": that is,
* it returns the supplied sequence, checking that all its items are of the correct type
*/

public final class ItemChecker extends UnaryExpression implements MappingFunction {

    // TODO: implement item checking within the push pipeline by providing a process() method
    // (currently an xsl:template that declares a result type materializes its results as a sequence)

    private ItemType requiredItemType;
    private RoleLocator role;

    /**
    * Constructor
    */

    public ItemChecker(Expression sequence, ItemType itemType, RoleLocator role) {
        super(sequence);
        this.requiredItemType = itemType;
        this.role = role;
        adoptChildExpression(sequence);
    }

    /**
    * Simplify an expression
    */

     public Expression simplify(StaticContext env) throws XPathException {
        operand = operand.simplify(env);
        if (requiredItemType instanceof AnyItemType) {
            return operand;
        }
        return this;
    }

    /**
    * Type-check the expression
    */

    public Expression analyze(StaticContext env, ItemType contextItemType) throws XPathException {
        operand = operand.analyze(env, contextItemType);
        // When analyze is called a second time, we might have more information...

        ItemType supplied = operand.getItemType();
        int relation = Type.relationship(requiredItemType, supplied);
        if (relation == Type.SAME_TYPE || relation == Type.SUBSUMES) {
            return operand;
        }
        //ItemType supplied = operand.getItemType();
        //if (!(requiredItemType instanceof NodeTest && supplied instanceof NodeTest)) {
            // we can't yet analyze NodeTests well enough
            if (relation == Type.DISJOINT) {
                String message = "Required type of " + role.getMessage() +
                                                 " is " + requiredItemType.toString(env.getNamePool()) +
                                                 "; supplied value has type " +
                                                 operand.getItemType().toString(env.getNamePool());
                StaticError err = new StaticError(message);
                err.setErrorCode(role.getErrorCode());
                err.setLocator(this);
                err.setIsTypeError(true);
                throw err;
            }
        //}
        return this;
    }

    /**
    * Iterate over the sequence of values
    */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        SequenceIterator base = operand.iterate(context);
        return new MappingIterator(base, this, null, context);
    }

    /**
    * Mapping function: this is used only if the expression does not allow a sequence of more than
    * one item.
    */

    public Object map(Item item, XPathContext nullcontext, Object info) throws XPathException {
        testConformance(item, (XPathContext)info);
        return item;
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

    private void testConformance(Item item, XPathContext context) throws XPathException {
        if (!requiredItemType.matchesItem(item)) {
            NamePool pool = context.getController().getNamePool();
            String message = "Required type of " + role.getMessage() +
                                             " is " + requiredItemType.toString(pool) +
                                             "; supplied value has type " + Type.displayTypeName(item);
            String errorCode = role.getErrorCode();
            if ("XP0050".equals(errorCode)) {
                // error in "treat as" assertion
                dynamicError(message, errorCode, context);
            } else {
                typeError(message, errorCode, context);
            }
        }
    }

    /**
    * Determine the data type of the items returned by the expression
    */

	public ItemType getItemType() {
        // TODO: take the intersection of the required type with the static type of the operand
	    return requiredItemType;
	}

    /**
     * Is this expression the same as another expression?
     */

    public boolean equals(Object other) {
        return super.equals(other) &&
                requiredItemType == ((ItemChecker)other).requiredItemType;
    }

    /**
     * Give a string representation of the operator for use in diagnostics
     * @return the operator, as a string
     */

    protected String displayOperator(NamePool pool) {
        return "treat as " + requiredItemType.toString(pool);
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
