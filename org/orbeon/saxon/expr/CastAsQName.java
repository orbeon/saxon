package org.orbeon.saxon.expr;

import org.orbeon.saxon.om.*;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.type.AtomicType;
import org.orbeon.saxon.value.*;
import org.orbeon.saxon.xpath.DynamicError;
import org.orbeon.saxon.xpath.XPathException;
import org.orbeon.saxon.style.StandardNames;

import java.util.Iterator;
import java.io.PrintStream;

/**
 * This class supports casting a string to a QName or a notation
 */

// TODO: the spec now permits this only as a compile-time constructor. This implementation is more liberal,
// it preserves the static namespace context and allows it to be used at run-time.

public class CastAsQName extends ComputedExpression {

    private Expression input;
    private NamespaceResolver nsContext;
    private AtomicType targetType;

    public CastAsQName(Expression s, AtomicType target) {
        input = s;
        targetType = target;
    }

    public Expression analyze(StaticContext env, ItemType contextItemType) throws XPathException {
        nsContext = env.getNamespaceResolver();
        return this;
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

    public int getIntrinsicDependencies() {
        return 0;
    }

    public Iterator iterateSubExpressions() {
        return new MonoIterator(input);
    }

    public Item evaluateItem(XPathContext context) throws XPathException {
        AtomicValue av = (AtomicValue)input.evaluateItem(context);
        if (av==null) return null;
        StringValue sv = (StringValue)av.getPrimitiveValue();

        try {
            String parts[] = Name.getQNameParts(sv.getStringValue());
            String uri = nsContext.getURIForPrefix(parts[0], true);
            if (uri==null) {
                DynamicError e = new DynamicError("Prefix '" + parts[0] + "' has not been declared");
                e.setXPathContext(context);
                throw e;
            }
            if (targetType.getFingerprint() == StandardNames.XS_QNAME) {
                return new QNameValue(parts[0], uri, parts[1]);
            } else if (Type.isSubType(targetType, Type.QNAME_TYPE)) {
                QNameValue q = new QNameValue(parts[0], uri, parts[1]);
                return DerivedAtomicValue.makeValue(q, sv.getStringValue(), targetType, true);
            } else {
                NotationValue n = new NotationValue(parts[0], uri, parts[1]);
                return DerivedAtomicValue.makeValue(n, sv.getStringValue(), targetType, true);
            }
        } catch (QNameException err) {
            DynamicError e = new DynamicError(err);
            e.setXPathContext(context);
            throw e;
        }
    }

    public void display(int level, NamePool pool, PrintStream out) {
        out.println(ExpressionTool.indent(level) + "cast as QName");
        input.display(level+1, pool, out);
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