package org.orbeon.saxon.functions;

import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.expr.ExpressionVisitor;
import org.orbeon.saxon.expr.StaticProperty;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.StandardNames;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.BuiltInAtomicType;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.value.DateTimeValue;

/**
* This class implements the XPath 2.0 functions
 * current-date(), current-time(), and current-dateTime(), as
 * well as the function implicit-timezone(). The value that is required
 * is inferred from the type of result required.
*/


public class CurrentDateTime extends SystemFunction {

    /**
     * preEvaluate: this method suppresses compile-time evaluation by doing nothing
     * (because the value of the expression depends on the runtime context)
     * @param visitor an expression visitor
     */

    public Expression preEvaluate(ExpressionVisitor visitor) {
        return this;
    }

    /**
    * Determine the dependencies
    */

    public int getIntrinsicDependencies() {
        // current date/time is part of the context, but it is fixed for a transformation, so
        // we don't need to manage it as a dependency: expressions using it can be freely
        // rearranged
       return StaticProperty.DEPENDS_ON_RUNTIME_ENVIRONMENT;
    }

    /**
    * Evaluate in a general context
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        final DateTimeValue dt = DateTimeValue.getCurrentDateTime(context);
        final TypeHierarchy th = context.getConfiguration().getTypeHierarchy();
        final int targetType = getItemType(th).getPrimitiveType();
        switch (targetType) {
            case StandardNames.XS_DATE_TIME:
                return dt;
            case StandardNames.XS_DATE:
                return dt.convert(BuiltInAtomicType.DATE, true, context).asAtomic();
            case StandardNames.XS_TIME:
                return dt.convert(BuiltInAtomicType.TIME, true, context).asAtomic();
            case StandardNames.XS_DAY_TIME_DURATION:
            case StandardNames.XS_DURATION:
                return dt.getComponent(Component.TIMEZONE);
            default:
                throw new IllegalArgumentException("Wrong target type for current date/time");
        }
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
