package org.orbeon.saxon.functions;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.expr.ExpressionVisitor;
import org.orbeon.saxon.expr.Literal;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.NameChecker;
import org.orbeon.saxon.om.QNameException;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.BuiltInAtomicType;
import org.orbeon.saxon.value.AtomicValue;
import org.orbeon.saxon.value.QNameValue;


/**
* This class supports the fn:QName() function (previously named fn:expanded-QName())
*/

public class QNameFn extends SystemFunction {

    /**
     * Pre-evaluate a function at compile time. Functions that do not allow
     * pre-evaluation, or that need access to context information, can override this method.
     * @param visitor an expression visitor
     */

    public Expression preEvaluate(ExpressionVisitor visitor) throws XPathException {
        try {
            XPathContext early = visitor.getStaticContext().makeEarlyEvaluationContext();
            final Item item1 = argument[1].evaluateItem(early);
            final String lex = item1.getStringValue();
            final Item item0 = argument[0].evaluateItem(early);
            String uri;
            if (item0 == null) {
                uri = "";
            } else {
                uri = item0.getStringValue();
            }
            final NameChecker checker = visitor.getConfiguration().getNameChecker();
            final String[] parts = checker.getQNameParts(lex);
            // The QNameValue constructor does not check the prefix
            if (parts[0].length() != 0 && !checker.isValidNCName(parts[0])) {
                XPathException err = new XPathException("Malformed prefix in QName: '" + parts[0] + '\'');
                err.setErrorCode("FOCA0002");
                throw err;
            }
            return Literal.makeLiteral(
                    new QNameValue(parts[0], uri, parts[1], BuiltInAtomicType.QNAME, checker));
        } catch (QNameException e) {
            XPathException err = new XPathException(e.getMessage(), this);
            err.setErrorCode("FOCA0002");
            err.setLocator(this);
            throw err;
        } catch (XPathException err) {
            err.maybeSetLocation(this);
            throw err;
        }
    }

    /**
    * Evaluate the expression
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        AtomicValue arg0 = (AtomicValue)argument[0].evaluateItem(context);

        String uri;
        if (arg0 == null) {
            uri = null;
        } else {
            uri = arg0.getStringValue();
        }

        try {
            final String lex = argument[1].evaluateItem(context).getStringValue();
            final NameChecker checker = context.getConfiguration().getNameChecker();
            final String[] parts = checker.getQNameParts(lex);
            // The QNameValue constructor does not check the prefix
            if (parts[0].length() != 0 && !checker.isValidNCName(parts[0])) {
                XPathException err = new XPathException("Malformed prefix in QName: '" + parts[0] + '\'');
                err.setErrorCode("FORG0001");
                throw err;
            }
            return new QNameValue(parts[0], uri, parts[1], BuiltInAtomicType.QNAME, checker);
        } catch (QNameException e) {
            dynamicError(e.getMessage(), "FOCA0002", context);
            return null;
        } catch (XPathException err) {
            err.maybeSetLocation(this);
            throw err;
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
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//
