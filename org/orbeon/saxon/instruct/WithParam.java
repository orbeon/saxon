package org.orbeon.saxon.instruct;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.StandardNames;
import org.orbeon.saxon.om.ValueRepresentation;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.trace.ExpressionPresenter;

import java.util.List;

/**
* An instruction derived from a xsl:with-param element in the stylesheet. <br>
*/

public class WithParam extends GeneralVariable {

    int parameterId;

    public WithParam() {}

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
    

    public int getInstructionNameCode() {
        return StandardNames.XSL_WITH_PARAM;
    }

    public TailCall processLeavingTail(XPathContext context) throws XPathException {
        // not used
        return null;
    }

    public static void simplify(WithParam[] params, ExpressionVisitor visitor) throws XPathException {
         for (int i=0; i<params.length; i++) {
            Expression select = params[i].getSelectExpression();
            if (select != null) {
                params[i].setSelectExpression(visitor.simplify(select));
            }
        }
    }


    public static void typeCheck(WithParam[] params, ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
         for (int i=0; i<params.length; i++) {
            Expression select = params[i].getSelectExpression();
            if (select != null) {
                params[i].setSelectExpression(visitor.typeCheck(select, contextItemType));
            }
        }
    }

    public static void optimize(ExpressionVisitor visitor, WithParam[] params, ItemType contextItemType) throws XPathException {
         for (int i=0; i<params.length; i++) {
             visitor.optimize(params[i], contextItemType);
//            Expression select = params[i].getSelectExpression();
//            if (select != null) {
//                params[i].setSelectExpression(select.optimize(opt, env, contextItemType));
//            }
         }
    }

   /**
     * Promote the expressions in a set of with-param elements. This is a convenience
     * method for use by subclasses.
     */

    public static void promoteParams(WithParam[] params, PromotionOffer offer) throws XPathException {
        for (int i=0; i<params.length; i++) {
            Expression select = params[i].getSelectExpression();
            if (select != null) {
                params[i].setSelectExpression(select.promote(offer));
            }
        }
    }

    /**
     * Get the XPath expressions used in an array of WithParam parameters (add them to the supplied list)
     */

    public static void getXPathExpressions(WithParam[] params, List list) {
        if (params != null) {
            for (int i=0; i<params.length; i++) {
                list.add(params[i]);
            }
        }
    }

    /**
     * DIsplay the parameter expressions
     */

    public static void displayExpressions(WithParam[] params, ExpressionPresenter out) {
        if (params != null) {
            for (int i=0; i<params.length; i++) {
                out.startElement("withParam");
                out.emitAttribute("name", params[i].getVariableQName().getDisplayName());
                params[i].getSelectExpression().explain(out);
                out.endElement();
            }
        }
    }

    /**
     * Replace a subexpression
     */

    public static boolean replaceXPathExpression(WithParam[] params, Expression original, Expression replacement) {
        boolean found = false;
        for (int i=0; i<params.length; i++) {
            boolean f = params[i].replaceSubExpression(original, replacement);
            found |= f;
        }
        return found;
    }

    /**
     * Evaluate the variable (method exists only to satisfy the interface)
     */

    public ValueRepresentation evaluateVariable(XPathContext context) throws XPathException {
        throw new UnsupportedOperationException();
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
