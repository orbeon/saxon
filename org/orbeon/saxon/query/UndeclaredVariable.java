package org.orbeon.saxon.query;

import org.orbeon.saxon.expr.BindingReference;
import org.orbeon.saxon.expr.VariableDeclaration;
import org.orbeon.saxon.instruct.GlobalVariable;
import org.orbeon.saxon.trans.XPathException;

import java.util.Collections;
import java.util.Iterator;

/**
 *  An UndeclaredVariable object is created when a reference is encountered to a variable
 *  that has not yet been declared. This can happen as a result of recursive module imports.
 *  These references are resolved at the end of query parsing.
 */

public class UndeclaredVariable extends GlobalVariableDefinition {

    public UndeclaredVariable(){}

    public void transferReferences(VariableDeclaration var) {
        Iterator iter = references.iterator();
        while (iter.hasNext()) {
            BindingReference ref = (BindingReference)iter.next();
            var.registerReference(ref);
        }
        references = Collections.EMPTY_LIST;
    }

    public GlobalVariable compile(StaticQueryContext env, int slot) throws XPathException {
        throw new UnsupportedOperationException("Attempt to compile a place-holder for an undeclared variable");
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
// Contributor(s): none
//
