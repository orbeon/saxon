package net.sf.saxon.instruct;

import net.sf.saxon.expr.*;
import net.sf.saxon.om.ValueRepresentation;
import net.sf.saxon.style.StandardNames;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;

import java.util.Collections;
import java.util.Iterator;

/**
 * The compiled form of an xsl:param element in the stylesheet or an
 * external variable in a Query. <br>
 * The xsl:param element in XSLT has mandatory attribute name and optional attribute select. It can also
 * be specified as required="yes" or required="no". In standard XQuery external variables are always required,
 * and no default value can be specified; but Saxon provides an extension pragma that allows a query
 * to specify a default.
 */

public final class LocalParam extends GeneralVariable {

    private Expression conversion = null;

    /**
     * Define a conversion that is to be applied to the supplied parameter value.
     * @param convertor
     */
    public void setConversion(Expression convertor) {
        conversion = convertor;
    }

    /**
     * Get the name of this instruction for diagnostic and tracing purposes
     */

    public int getInstructionNameCode() {
        return StandardNames.XSL_PARAM;
    }

    /**
     * Get all the XPath expressions associated with this instruction
     * (in XSLT terms, the expression present on attributes of the instruction,
     * as distinct from the child instructions in a sequence construction)
     */

    public Iterator iterateSubExpressions() {
        if (select != null && conversion != null) {
            return new PairIterator(select, conversion);
        } else if (select != null) {
            return new MonoIterator(select);
        } else if (conversion != null) {
            return new MonoIterator(conversion);
        } else {
            return Collections.EMPTY_LIST.iterator();
        }
    }

    /**
    * Process the local parameter declaration
    */

    public TailCall processLeavingTail(XPathContext context) throws XPathException {
        boolean wasSupplied = context.useLocalParameter(getVariableFingerprint(), this, isTunnelParam());
        if (wasSupplied) {
            // if a parameter was supplied by the caller, we may need to convert it to the type required
            if (conversion != null) {
                context.setLocalVariable(getSlotNumber(),
                        ExpressionTool.eagerEvaluate(conversion, context));
                // We do an eager evaluation here for safety, because the result of the
                // type conversion overwrites the slot where the actual supplied parameter
                // is contained.
            }

            // don't evaluate the default if a value has been supplied or if it has already been
            // evaluated by virtue of a forwards reference

        } else {
            if (isRequiredParam()) {
                DynamicError e = new DynamicError("No value supplied for required parameter");
                e.setXPathContext(context);
                e.setErrorCode("XTDE0700");
                throw e;
            }
            context.setLocalVariable(getSlotNumber(), getSelectValue(context));
        }
        return null;
    }

    /**
     * Evaluate the variable
     */

    public ValueRepresentation evaluateVariable(XPathContext c) {
        return c.evaluateLocalVariable(getSlotNumber());
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
