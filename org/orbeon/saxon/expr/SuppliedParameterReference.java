package org.orbeon.saxon.expr;
import org.orbeon.saxon.trace.ExpressionPresenter;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.om.ValueRepresentation;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.AnyItemType;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.value.Value;

/**
 * Supplied parameter reference: this is an internal expression used to refer to
 * the value of the n'th parameter supplied on a template call (apply-templates).
 * It is used within a type-checking expression designed to check the consistency
 * of the supplied value with the required type. This type checking is all done
 * at run-time, because the binding of apply-templates to actual template rules
 * is entirely dynamic.
 */

public class SuppliedParameterReference extends Expression {

    int slotNumber;

    /**
    * Constructor
    * @param slot identifies this parameter
    */

    public SuppliedParameterReference(int slot) {
        slotNumber = slot;
    }

    public Expression typeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        return this;
    }

    public Expression optimize(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        return this;
    }

    /**
    * Determine the data type of the expression, if possible.
    * @return Type.ITEM, because we don't know the type of the supplied value
    * in advance.
     * @param th the type hierarchy cache
     */

    public ItemType getItemType(TypeHierarchy th) {
        return AnyItemType.getInstance();
    }

    /**
    * Get the static cardinality
     * @return ZERO_OR_MORE, because we don't know the type of the supplied value
     * in advance.
    */

    public int computeCardinality() {
        return StaticProperty.ALLOWS_ZERO_OR_MORE;
    }

    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    public Expression copy() {
        return new SuppliedParameterReference(slotNumber);
    }

    /**
    * Test if this expression is the same as another expression.
    * (Note, we only compare expressions that
    * have the same static and dynamic context).
    */

    public boolean equals(Object other) {
        return this==other;
    }

    /**
    * Get the value of this expression in a given context.
    * @param c the XPathContext which contains the relevant variable bindings
    * @return the value of the variable, if it is defined
    * @throws XPathException if the variable is undefined
    */

    public SequenceIterator iterate(XPathContext c) throws XPathException {
        return Value.getIterator(c.evaluateLocalVariable(slotNumber));
    }

    public Item evaluateItem(XPathContext c) throws XPathException {
        ValueRepresentation actual = c.evaluateLocalVariable(slotNumber);
        return Value.asItem(actual);
    }

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void explain(ExpressionPresenter destination) {
        destination.startElement("suppliedParameter");
        destination.emitAttribute("slot", slotNumber+"");
        destination.endElement();
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
// Contributor(s):
//
