package org.orbeon.saxon.expr;

import java.io.Serializable;

/**
 * This class doesn't actually do any optimization itself, despite the name. Rather, it is
 * intended to act as a factory for implementation classes that perform optimization, so that
 * the appropriate level of optimization can be selected.
 */
public class Optimizer implements Serializable {
      
    /**
     * Create a GeneralComparison expression
     */

    public GeneralComparison makeGeneralComparison(Expression p0, int op, Expression p1) {
        return new GeneralComparison(p0, op, p1);
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

