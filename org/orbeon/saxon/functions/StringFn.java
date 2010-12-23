package org.orbeon.saxon.functions;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.expr.ExpressionVisitor;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.expr.PathMap;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.StringValue;

/**
* Implement XPath function string()
*/

public class StringFn extends SystemFunction {

    /**
    * Simplify and validate.
    * This is a pure function so it can be simplified in advance if the arguments are known
     * @param visitor an expression visitor
     */

     public Expression simplify(ExpressionVisitor visitor) throws XPathException {
        useContextItemAsDefault();
        argument[0].setFlattened(true);
        return simplifyArguments(visitor);
    }


    /**
     * Add a representation of a doc() call or similar function to a PathMap.
     * This is a convenience method called by the addToPathMap() methods for doc(), document(), collection()
     * and similar functions. These all create a new root expression in the path map.
     *
     * @param pathMap      the PathMap to which the expression should be added
     * @param pathMapNodes the node in the PathMap representing the focus at the point where this expression
     *                     is called. Set to null if this expression appears at the top level.
     * @return the pathMapNode representing the focus established by this expression, in the case where this
     *         expression is the first operand of a path expression or filter expression
     */

    public PathMap.PathMapNodeSet addToPathMap(PathMap pathMap, PathMap.PathMapNodeSet pathMapNodes) {
        PathMap.PathMapNodeSet result = argument[0].addToPathMap(pathMap, pathMapNodes);
        if (result != null) {
            result.setAtomized();
        }
        return null;
    }

    /**
    * Evaluate the function
    */

    public Item evaluateItem(XPathContext c) throws XPathException {
        Item arg = argument[0].evaluateItem(c);
        if (arg==null) {
            return StringValue.EMPTY_STRING;
        } else {
            return StringValue.makeStringValue(arg.getStringValueCS());
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
