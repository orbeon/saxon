package org.orbeon.saxon.instruct;
import org.orbeon.saxon.Controller;
import org.orbeon.saxon.event.ReceiverOptions;
import org.orbeon.saxon.event.SequenceReceiver;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.om.XMLChar;
import org.orbeon.saxon.pattern.NodeKindTest;
import org.orbeon.saxon.style.StandardNames;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.xpath.DynamicError;
import org.orbeon.saxon.xpath.XPathException;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;

/**
* An xsl:namespace element in the stylesheet. (XSLT 2.0)
*/

public class Namespace extends SimpleNodeConstructor {

    private Expression name;

    public Namespace (Expression name) {
        this.name = name;
    }

    /**
    * Set the name of this instruction for diagnostic and tracing purposes
    */

    public int getInstructionNameCode() {
        return StandardNames.XSL_NAMESPACE;
    }

    public Expression simplify(StaticContext env) throws XPathException {
        name = name.simplify(env);
        return super.simplify(env);
    }

    public ItemType getItemType() {
        return NodeKindTest.NAMESPACE;
    }

    public int getCardinality() {
        return StaticProperty.EXACTLY_ONE;
    }

    protected void promoteInst(PromotionOffer offer) {
        // do nothing
        //throw new UnsupportedOperationException("Namespace instruction cannot be used as an expression");
    }

    public void typeCheck(StaticContext env, ItemType contextItemType) {}

    public Iterator iterateSubExpressions() {
        ArrayList list = new ArrayList(6);
        if (select != null) {
            list.add(select);
        }
//        if (separator != null && !(separator instanceof StringValue)) {
//            list.add(separator);
//        }
        list.add(name);
        return list.iterator();
    }


    public TailCall processLeavingTail(XPathContext context) throws XPathException {
        Controller controller = context.getController();
        String prefix = name.evaluateAsString(context);

        if (!(prefix.equals("") || XMLChar.isValidNCName(prefix))) {
            DynamicError err = new DynamicError("Namespace prefix is invalid: " + prefix, this);
            err.setErrorCode("XT0920");
            err.setXPathContext(context);
            context.getController().recoverableError(err);
            return null;
        }

        if (prefix.equals("xml") || prefix.equals("xmlns")) {
            DynamicError err = new DynamicError("Namespace prefix '" + prefix + "' is not allowed", this);
            err.setErrorCode("XT0920");
            err.setXPathContext(context);
            context.getController().recoverableError(err);
            return null;
        }

        String uri = expandChildren(context).toString();

        if (uri.equals("")) {
            DynamicError err = new DynamicError("Namespace URI is an empty string", this);
            err.setErrorCode("XT0930");
            err.setXPathContext(context);
            context.getController().recoverableError(err);
            return null;
        }

        int nscode = controller.getNamePool().allocateNamespaceCode(prefix, uri);
        SequenceReceiver out = context.getReceiver();
        out.namespace(nscode, ReceiverOptions.REJECT_DUPLICATES);
        return null;
    }

    /**
     * Display this instruction as an expression, for diagnostics
     */

    public void display(int level, NamePool pool, PrintStream out) {
        out.println(ExpressionTool.indent(level) + "namespace");
        name.display(level+1, pool, out);
        super.display(level+1, pool, out);
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
