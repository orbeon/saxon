package net.sf.saxon.expr;
import net.sf.saxon.value.SequenceType;

/**
* Treat Expression: implements "treat as data-type ( expression )". This is a factory class only.
*/

public abstract class TreatExpression {

    /**
    * Make a treat expression
    * @return the expression
    */

    public static Expression make(Expression sequence, SequenceType type) {
        RoleLocator role = new RoleLocator(RoleLocator.TYPE_OP, "treat as", 0);
        Expression e = new CardinalityChecker(sequence, type.getCardinality(), role);
        ItemChecker checker = new ItemChecker(e, type.getPrimaryType(), role);
        checker.setErrorCode("XP0050");
        return checker;
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
