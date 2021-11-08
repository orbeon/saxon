package org.orbeon.saxon.expr;

import org.orbeon.saxon.sort.AtomicComparer;

/**
 * Interface implemented by expressions that perform a comparison
 */
public interface ComparisonExpression {

    /**
     * Get the AtomicComparer used to compare atomic values. This encapsulates any collation that is used
     */

    public AtomicComparer getAtomicComparer();

    /**
     * Get the primitive (singleton) operator used: one of Token.FEQ, Token.FNE, Token.FLT, Token.FGT,
     * Token.FLE, Token.FGE
     */

    public int getSingletonOperator();

    /**
     * Get the two operands of the comparison
     */

    public Expression[] getOperands();

    /**
     * Determine whether untyped atomic values should be converted to the type of the other operand
     * @return true if untyped values should be converted to the type of the other operand, false if they
     * should be converted to strings.
     */

    public boolean convertsUntypedToOther();
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