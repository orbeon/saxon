package org.orbeon.saxon.value;

import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.expr.RangeExpression;
import org.orbeon.saxon.expr.StaticProperty;
import org.orbeon.saxon.expr.ExpressionTool;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.Type;

import java.io.PrintStream;

/**
 * This class represents a sequence of consecutive ascending integers, for example 1 to 50.
 * The integers must be within the range of a Java long.
 */

public class IntegerRange extends Value {

    public long start;
    public long end;

    public IntegerRange(long start, long end) {
        this.start = start;
        this.end = end;
    }

    public long getStart() {
        return start;
    }

    public long getEnd() {
        return end;
    }

    /**
     * An implementation of Expression must provide at least one of the methods evaluateItem(), iterate(), or process().
     * This method indicates which of these methods is provided directly. The other methods will always be available
     * indirectly, using an implementation that relies on one of the other methods.
     */

    public int getImplementationMethod() {
        return ITERATE_METHOD;
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
     * @throws net.sf.saxon.trans.XPathException
     *          if any dynamic error occurs evaluating the
     *          expression
     */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        return new RangeExpression.RangeIterator(start, end);
    }

    /**
     * Determine the data type of the items in the expression, if possible
     *
     * @return AnyItemType (not known)
     */

    public ItemType getItemType() {
        return Type.INTEGER_TYPE;
    }

    /**
     * Determine the cardinality
     */

    public int getCardinality() {
        return StaticProperty.ALLOWS_MANY;
    }

    /**
     * Get the n'th item in the sequence (starting from 0). This is defined for all
     * Values, but its real benefits come for a sequence Value stored extensionally
     * (or for a MemoClosure, once all the values have been read)
     */

    public Item itemAt(int n) throws XPathException {
        if (n < 0 || n > (end-start)) {
            return null;
        }
        return new IntegerValue(start + n);
    }

    /**
     * Get the length of the sequence
     */

    public int getLength() throws XPathException {
        return (int)(end - start + 1);
    }

    /**
     * Diagnostic display of the expression
     */

    public void display(int level, NamePool pool, PrintStream out) {
        System.err.println(ExpressionTool.indent(level) + start + " to " + end);
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
