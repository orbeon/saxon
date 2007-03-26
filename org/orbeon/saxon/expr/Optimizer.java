package org.orbeon.saxon.expr;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.om.ValueRepresentation;
import org.orbeon.saxon.trans.StaticError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.value.Closure;
import org.orbeon.saxon.value.MemoClosure;

import java.io.Serializable;

/**
 * This class doesn't actually do any optimization itself, despite the name. Rather, it is
 * intended to act as a factory for implementation classes that perform optimization, so that
 * the appropriate level of optimization can be selected.
 */
public class Optimizer implements Serializable {

    protected Configuration config;

    public Optimizer(Configuration config) {
        this.config = config;
    }

    public Configuration getConfiguration() {
        return config;
    }

    /**
     * Create a GeneralComparison expression
     */

    public BinaryExpression makeGeneralComparison(Expression p0, int op, Expression p1, boolean backwardsCompatible) {
        if (backwardsCompatible) {
            return new GeneralComparison10(p0, op, p1);
        } else {
            return new GeneralComparison(p0, op, p1);
        }
    }

    /**
     * Attempt to optimize a copy operation. Return null if no optimization is possible.
     * @param select the expression that selects the items to be copied
     * @return null if no optimization is possible, or an expression that does an optimized
     * copy of these items otherwise
     */

    public Expression optimizeCopy(Expression select) throws XPathException {
        final TypeHierarchy th = config.getTypeHierarchy();
        if (select.getItemType(th).isAtomicType()) {
            return select;
        }
        return null;
    }

    /**
     * Make a Closure, given the expected reference count
     */

    public Closure makeClosure(Expression expression, int ref) {
        if (ref == 1) {
            return new Closure();
        } else {
            return new MemoClosure();
        }
    }

    /**
     * Examine a path expression to see whether it can be replaced by a call on the key() function;
     * if so, generate an appropriate key definition and return the call on key(). If not, return null.
     * @param pathExp The path expression to be converted.
     */

    public Expression convertPathExpressionToKey(PathExpression pathExp, StaticContext env)
    throws XPathException {
        return null;
    }

    /**
     * Try converting a filter expression to a call on the key function. Return null if not possible
     */

    public ComputedExpression tryToConvertFilterExpressionToKey(FilterExpression f, StaticContext env)
    throws XPathException {
        return null;
    }

    /**
     * Convert a path expression such as a/b/c[predicate] into a filter expression
     * of the form (a/b/c)[predicate]. This is possible whenever the predicate is non-positional.
     * The conversion is useful in the case where the path expression appears inside a loop,
     * where the predicate depends on the loop variable but a/b/c does not.
     * @param pathExp the path expression to be converted
     * @return the resulting filterexpression if conversion is possible, or null if not
     */

    public FilterExpression convertToFilterExpression(PathExpression pathExp, TypeHierarchy th)
    throws StaticError {
        return null;
    }

    public SequenceIterator tryIndexedFilter(ValueRepresentation startValue, Expression filter,
                                             int isIndexable, XPathContext context)
    throws XPathException {
        return null;
    }

    /**
     * Test whether a filter predicate is indexable.
     * @param filter the predicate expression
     * @return 0 if not indexable; +1 if the predicate is in the form expression=value; -1 if it is in
     * the form value=expression
     */

    public int isIndexableFilter(Expression filter) {
        return 0;
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

