package net.sf.saxon.value;
import net.sf.saxon.expr.ExpressionTool;
import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.EmptyIterator;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.pattern.NoNodeTest;
import net.sf.saxon.type.ItemType;

import java.io.PrintStream;

/**
* An EmptySequence object represents a sequence containing no members.
*/


public final class EmptySequence extends Value {

    // This class has a single instance
    private static EmptySequence THE_INSTANCE = new EmptySequence();


    /**
    * Private constructor: only the predefined instances of this class can be used
    */

    private EmptySequence() {}

    /**
    * Get the implicit instance of this class
    */

    public static EmptySequence getInstance() {
        return THE_INSTANCE;
    }

    /**
     * An implementation of Expression must provide at least one of the methods evaluateItem(), iterate(), or process().
     * This method indicates which of these methods is prefered.
     */

    public int getImplementationMethod() {
        return EVALUATE_METHOD;
    }

    /**
    * Return an iteration over the sequence
    */

    public SequenceIterator iterate(XPathContext context) {
        return EmptyIterator.getInstance();
    }

    /**
    * Determine the item type
    */

    public ItemType getItemType() {
        return NoNodeTest.getInstance();
    }

    /**
    * Determine the static cardinality
    */

    public int getCardinality() {
        return StaticProperty.EMPTY;
    }

    /**
    * Get the static properties of this expression (other than its type). The result is
    * bit-signficant. These properties are used for optimizations. In general, if
    * property bit is set, it is true, but if it is unset, the value is unknown.
    */

    public int getSpecialProperties() {
        return  StaticProperty.ORDERED_NODESET |
                StaticProperty.SINGLE_DOCUMENT_NODESET |
                StaticProperty.CONTEXT_DOCUMENT_NODESET;
    }

    /**
     * Get the length of the sequence
     * @return always 0 for an empty sequence
     */

    public final int getLength() {
        return 0;
    }
    /**
    * Is this expression the same as another expression?
    * @throws ClassCastException if the values are not comparable
    */

    public boolean equals(Object other) {
        if (!(other instanceof EmptySequence)) {
            throw new ClassCastException("Cannot compare " + other.getClass() + " to empty sequence");
        }
        return true;
    }

    public int hashCode() {
        return 42;
    }

    /**
    * Evaluate as a string. Returns the string value of the first value in the sequence
    * @return "" always
    */

//    public String getStringValue() throws XPathException {
//        return "";
//    }

    /**
    * Get the effective boolean value - always false
    */

    public boolean effectiveBooleanValue(XPathContext context) {
        return false;
    }

    /**
    * Diagnostic print of expression structure
    */

    public void display(int level, NamePool pool, PrintStream out) {
        out.println(ExpressionTool.indent(level) + "()" );
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
