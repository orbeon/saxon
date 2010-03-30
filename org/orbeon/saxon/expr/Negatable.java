package org.orbeon.saxon.expr;

/**
 * This interface is implemented by expressions that returns a boolean value, and returns an expression
 * whose result is the negated boolean value
 */
public interface Negatable {

    /**
     * Check whether this specific instance of the expression is negatable
     * @return true if it is
     */

    public boolean isNegatable(ExpressionVisitor visitor);

    /**
     * Create an expression that returns the negation of this expression
     * @return the negated expression
     * @throws IllegalOperationException if isNegatable() returns false
     */

    public Expression negate();
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

