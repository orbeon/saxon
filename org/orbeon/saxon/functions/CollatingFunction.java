package net.sf.saxon.functions;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.sort.AtomicComparer;
import net.sf.saxon.sort.CodepointCollator;
import net.sf.saxon.trans.StaticError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.StringValue;
import net.sf.saxon.value.Value;

import java.util.Comparator;

/**
* Abstract superclass for all functions that take an optional collation argument
*/

// Supports string comparison using a collation

public abstract class CollatingFunction extends SystemFunction {

    // The collation, if known statically
    Comparator collation = null;

    /**
    * preEvaluate: if all arguments are known statically, evaluate early
    */

    public Expression preEvaluate(StaticContext env) throws XPathException {
        if (getNumberOfArguments() == getDetails().maxArguments) {
            // A collection was supplied explicitly (as a compile-time name, or we wouldn't be here)
            collation = env.getCollation(((Value)argument[getNumberOfArguments()-1]).getStringValue());
            if (collation == null) {
                StaticError err = new StaticError("Unknown collation {" +
                        ((Value)argument[getNumberOfArguments()-1]).getStringValue() + '}');
                err.setLocator(this);
                throw err;
            }
            return super.preEvaluate(env);
        } else {
            // Use the default collation
            String uri = env.getDefaultCollationName();
            collation = env.getCollation(uri);
            return super.preEvaluate(env);
        }
    }

    /**
    * Get a AtomicComparer that can be used to compare values
    * @param arg the position of the argument (starting at 0) containing the collation name.
    * If this argument was not supplied, the default collation is used
    * @param context The dynamic evaluation context.
    */

    protected AtomicComparer getAtomicComparer(int arg, XPathContext context) throws XPathException {
        return new AtomicComparer(getCollator(arg, context, true));
    }

    /**
    * Get a collator suitable for comparing strings. Returns the collator specified in the
    * given function argument if present, otherwise returns the default collator.
     * @param arg The argument position (counting from zero) that holds the collation
     * URI if present
     * @param context The dynamic context
     * @param useDefault true if, in the absence of a collation argument, the default
     * collation should be used; false if the codepoint collation should be used.
    * @return a Comparator, which will either be a java.text.Collator, or a CodepointCollator
    */

    protected Comparator getCollator(int arg, XPathContext context, boolean useDefault) throws XPathException {

        if (collation != null) {
            // the collation was determined statically
            return collation;
        } else {
            int numargs = argument.length;
            if (numargs > arg) {
                AtomicValue av = (AtomicValue)argument[arg].evaluateItem(context);
                StringValue collationValue = (StringValue)av.getPrimitiveValue();
                String collationName = collationValue.getStringValue();
                return context.getCollation(collationName);
            } else if (useDefault) {
                Comparator collator = context.getDefaultCollation();
                return (collator==null ? CodepointCollator.getInstance() : collator);
            } else {
                return CodepointCollator.getInstance();
            }
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
