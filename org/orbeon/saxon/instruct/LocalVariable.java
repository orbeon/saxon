package org.orbeon.saxon.instruct;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.expr.ExpressionTool;
import org.orbeon.saxon.xpath.XPathException;
import org.orbeon.saxon.value.Value;

/**
* Handler for local xsl:variable elements in stylesheet. Not used in XQuery. <br>
*/

public class LocalVariable extends GeneralVariable {

    /**
    * Process the local variable declaration
    */

    public TailCall processLeavingTail(XPathContext context) throws XPathException {
        context.setLocalVariable(getSlotNumber(),
                ExpressionTool.lazyEvaluate(getSelectExpression(), context));
        return null;
    }

   /**
    * Evaluate the variable
    */

   public Value evaluateVariable(XPathContext c) throws XPathException {
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
