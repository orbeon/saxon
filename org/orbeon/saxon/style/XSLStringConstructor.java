package net.sf.saxon.style;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.instruct.SimpleNodeConstructor;
import net.sf.saxon.instruct.Executable;
import net.sf.saxon.om.Axis;
import net.sf.saxon.om.AxisIterator;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.StringValue;
import net.sf.saxon.xpath.StaticError;

import javax.xml.transform.TransformerConfigurationException;

/**
* Common superclass for XSLT elements whose content template produces a text
* value: xsl:attribute, xsl:comment, and xsl:processing-instruction
*/

public abstract class XSLStringConstructor extends StyleElement {

    //protected String stringValue = null;
    protected Expression select = null;

    /**
    * Determine whether this node is an instruction.
    * @return true - it is an instruction
    */

    public boolean isInstruction() {
        return true;
    }

    /**
    * Determine whether this type of element is allowed to contain a template-body
    * @return true: yes, it may contain a template-body
    */

    public boolean mayContainSequenceConstructor() {
        return true;
    }

    public void validate() throws TransformerConfigurationException {
        if (select!=null && hasChildNodes()) {
            compileError("An " + getDisplayName() + " element with a select attribute must be empty");
        }
        AxisIterator kids = iterateAxis(Axis.CHILD);
        NodeInfo first = (NodeInfo)kids.next();
        if (select==null) {
            if (first == null) {
                // there are no child nodes and no select attribute
                //stringValue = "";
                select = StringValue.EMPTY_STRING;
            } else {
                if (kids.next() == null) {
                    // there is exactly one child node
                    if (first.getNodeKind() == Type.TEXT) {
                        // it is a text node: optimize for this case
                        select = new StringValue(first.getStringValue());
                    //} else if (first instanceof XSLValueOf) {
                    //    // it is an xsl:value-of instruction: optimize this case
                    //    // *** optimization removed: it doesn't handle value-of with separator, type conversion,
                          // etc.
                    //    XSLValueOf v = (XSLValueOf)first;
                    //    valueExpression = v.getSelectExpression();
                    //    if (v.getDisableOutputEscaping() && !(this instanceof XSLText)) {
                    //        v.compileError("disable-output-escaping is not allowed for a non-text node");
                    //    }
                    }
                }
            }
        } else {
//            try {
//                select = makeStringJoin(select);
//            } catch (XPathException e) {
//                compileError(e);
//            }
        }
    }

    protected void compileContent(Executable exec, SimpleNodeConstructor inst) throws TransformerConfigurationException {
        if (select != null) {
            try {
                inst.setSelect(select);
            } catch (StaticError err) {
                compileError(err);
            }
        } else {
            compileChildren(exec, inst, true);
        }
    }

    /**
    * Make a string-join expression that concatenates the string-values of items in
    * a sequence with intervening spaces. This may be simplified later as a result
    * of type-checking.
    */

//    private static Expression makeStringJoin(Expression exp) throws XPathException {
//        if (!Type.isSubType(exp.getItemType(), Type.ANY_ATOMIC_TYPE)) {
//            exp = new Atomizer(exp);
//        }
//        if (!Type.isSubType(exp.getItemType(), Type.STRING_TYPE)) {
//            exp = new AtomicSequenceConverter(exp, Type.STRING_TYPE);
//        }
//        if (Cardinality.allowsMany(exp.getCardinality())) {
//            StringJoin fn = (StringJoin)SystemFunction.makeSystemFunction("string-join");
//            Expression[] args = new Expression[2];
//            args[0] = exp;
//            args[1] = new StringValue(" ");
//            fn.setArguments(args);
//            exp = fn;
//        }
//        return exp;
//    }
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
