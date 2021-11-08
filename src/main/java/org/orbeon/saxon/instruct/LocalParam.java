package org.orbeon.saxon.instruct;

import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.StandardNames;
import org.orbeon.saxon.om.ValueRepresentation;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.trace.ExpressionPresenter;

import java.util.Collections;
import java.util.Iterator;

/**
 * The compiled form of an xsl:param element within a template in an XSLT stylesheet.
 *
 * <p>The xsl:param element in XSLT has mandatory attribute name and optional attribute select. It can also
 * be specified as required="yes" or required="no".</p>
 *
 * <p>This is used only for parameters to XSLT templates. For function calls, the caller of the function
 * places supplied arguments onto the callee's stackframe and the callee does not need to do anything.
 * Global parameters (XQuery external variables) are handled using {@link GlobalParam}.
 */

public final class LocalParam extends GeneralVariable {

    private int parameterId;
    private Expression conversion = null;
    private int conversionEvaluationMode = ExpressionTool.UNDECIDED;

    /**
     * Allocate a number which is essentially an alias for the parameter name,
     * unique within a stylesheet
     * @param id the parameter id
     */

    public void setParameterId(int id) {
        parameterId = id;
    }

    /**
     * Get the parameter id, which is essentially an alias for the parameter name,
     * unique within a stylesheet
     * @return the parameter id
     */

    public int getParameterId() {
        return parameterId;
    }
    
    /**
     * Define a conversion that is to be applied to the supplied parameter value.
     * @param convertor The expression to be applied. This performs type checking,
     * and the basic conversions implied by function calling rules, for example
     * numeric promotion, atomization, and conversion of untyped atomic values to
     * a required type. The conversion uses the actual parameter value as input,
     * referencing it using a VariableReference.
     */
    public void setConversion(Expression convertor) {
        conversion = convertor;
        if (convertor != null) {
            conversionEvaluationMode = ExpressionTool.eagerEvaluationMode(conversion);
        }
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
     * Replace one subexpression by a replacement subexpression
     * @param original the original subexpression
     * @param replacement the replacement subexpression
     * @return true if the original subexpression is found
     */

    public boolean replaceSubExpression(Expression original, Expression replacement) {
        boolean found = false;
        if (select == original) {
            select = replacement;
            found = true;
        }
        if (conversion == original) {
            conversion = replacement;
            found = true;
        }
        return found;
    }


    /**
    * Process the local parameter declaration
    */

    public TailCall processLeavingTail(XPathContext context) throws XPathException {
        boolean wasSupplied = context.useLocalParameter(getVariableQName(), this, isTunnelParam());
        if (wasSupplied) {
            // if a parameter was supplied by the caller, we may need to convert it to the type required
            if (conversion != null) {
                context.setLocalVariable(getSlotNumber(),
                        ExpressionTool.evaluate(conversion, conversionEvaluationMode, context, 10));
                // We do an eager evaluation here for safety, because the result of the
                // type conversion overwrites the slot where the actual supplied parameter
                // is contained.
            }

            // don't evaluate the default if a value has been supplied or if it has already been
            // evaluated by virtue of a forwards reference

        } else {
            if (isImplicitlyRequiredParam()) {
                XPathException e = new XPathException("A value must be supplied for the parameter because " +
                        "the default value is not a valid instance of the required type");
                e.setXPathContext(context);
                e.setErrorCode("XTDE0610");
                throw e;
            } else if (isRequiredParam()) {
                XPathException e = new XPathException("No value supplied for required parameter");
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
        return c.evaluateLocalVariable(slotNumber);
    }

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void explain(ExpressionPresenter out) {
        out.startElement("param");
        out.emitAttribute("name", variableQName.getDisplayName());
        if (select != null) {
            select.explain(out);
        }
        out.endElement();
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
