package org.orbeon.saxon.expr;

import org.orbeon.saxon.instruct.*;
import org.orbeon.saxon.om.NamespaceConstant;
import org.orbeon.saxon.om.StructuredQName;
import org.orbeon.saxon.om.ValueRepresentation;
import org.orbeon.saxon.trace.ExpressionPresenter;
import org.orbeon.saxon.trans.XPathException;

import java.util.Iterator;
import java.util.ArrayList;

/**
 *
 */
public class ContinueInstr extends Instruction {

    private WithParam[] actualParams = null;
    private IterateInstr iterateInstr;
    private UserFunction continueFunction;
    static ValueRepresentation[] emptyArgs = new ValueRepresentation[0];
    public static StructuredQName SAXON_CONTINUE =
            new StructuredQName("saxon", NamespaceConstant.SAXON, "continue");

    public ContinueInstr(IterateInstr iterateInstr) {
        this.iterateInstr = iterateInstr;
        continueFunction = new UserFunction();
        continueFunction.setFunctionName(SAXON_CONTINUE);
    }

    public void setParameters(WithParam[] actualParams) {
        this.actualParams = actualParams;
    }

    public Iterator iterateSubExpressions() {
        ArrayList list = new ArrayList(10);
        WithParam.getXPathExpressions(actualParams, list);
        return list.iterator();
    }

    public TailCall processLeavingTail(XPathContext context) throws XPathException {
        XPathContextMajor cm = (XPathContextMajor)context;
        ParameterSet params = assembleParams(context, actualParams);
        cm.setLocalParameters(params);
        cm.requestTailCall(continueFunction, emptyArgs);
        return null;
    }

    public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        WithParam.simplify(actualParams, visitor);
        return this;
    }

    public Expression copy() {
        return this;
    }

    public void explain(ExpressionPresenter out) {
        out.startElement("saxonContinue");
        if (actualParams != null && actualParams.length > 0) {
            out.startSubsidiaryElement("withParams");
            WithParam.displayExpressions(actualParams, out);
            out.endSubsidiaryElement();
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
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//

