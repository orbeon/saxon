package org.orbeon.saxon.expr;

import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.Name;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.om.QNameException;
import org.orbeon.saxon.style.StandardNames;
import org.orbeon.saxon.trans.StaticError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.AtomicType;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.*;

import java.io.PrintStream;
import java.util.Iterator;

/**
 * This class supports casting a string to a QName or a notation.
 */

public class CastAsQName extends ComputedExpression {

    private Expression operand;
    private AtomicType targetType;

    public CastAsQName(Expression s, AtomicType target) {
        operand = s;
        targetType = target;
    }

    public Expression analyze(StaticContext env, ItemType contextItemType) throws XPathException {
        if (operand instanceof StringValue) {
            try {
                CharSequence arg = ((StringValue)operand).getStringValueCS();
                String parts[] = Name.getQNameParts(arg);
                String uri;
                if ("".equals(parts[0])) {
                    uri = "";
                } else {
                    uri = env.getURIForPrefix(parts[0]);
                    if (uri==null) {
                        StaticError e = new StaticError("Prefix '" + parts[0] + "' has not been declared");
                        throw e;
                    }
                }
                if (targetType.getFingerprint() == StandardNames.XS_QNAME) {
                    return new QNameValue(parts[0], uri, parts[1]);
                } else if (Type.isSubType(targetType, Type.QNAME_TYPE)) {
                    QNameValue q = new QNameValue(parts[0], uri, parts[1]);
                    AtomicValue av = targetType.makeDerivedValue(q, arg, true);
                    if (av instanceof ErrorValue) {
                        throw ((ErrorValue)av).getException();
                    }
                    return av;
                } else {
                    NotationValue n = new NotationValue(parts[0], uri, parts[1]);
                    AtomicValue av =  targetType.makeDerivedValue(n, arg, true);
                    if (av instanceof ErrorValue) {
                        throw ((ErrorValue)av).getException();
                    }
                    return av;
                }
            } catch (QNameException err) {
                StaticError e = new StaticError(err);
                throw e;
            }
        } else {
            StaticError err = new StaticError("The argument of a QName constructor must be a string literal");
            throw err;
        }
    }

    public Expression promote(PromotionOffer offer) throws XPathException {
        return super.promote(offer);
    }

    public int computeCardinality() {
        return StaticProperty.EXACTLY_ONE;
    }

    public ItemType getItemType() {
        return targetType;
    }

    /**
     * Determine the special properties of this expression
     * @return {@link StaticProperty#NON_CREATIVE}.
     */

    public int computeSpecialProperties() {
        int p = super.computeSpecialProperties();
        return p | StaticProperty.NON_CREATIVE;
    }

    public int getIntrinsicDependencies() {
        return 0;
    }

    public Iterator iterateSubExpressions() {
        return new MonoIterator(operand);
    }

    public Item evaluateItem(XPathContext context) throws XPathException {
        throw new UnsupportedOperationException("A QName constructor cannot be evaluated at run-time");
    }

    public void display(int level, NamePool pool, PrintStream out) {
        out.println(ExpressionTool.indent(level) + "cast as QName");
        operand.display(level+1, pool, out);
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