package org.orbeon.saxon.instruct;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.Controller;
import org.orbeon.saxon.event.ReceiverOptions;
import org.orbeon.saxon.event.SequenceReceiver;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.NamespaceConstant;
import org.orbeon.saxon.pattern.NodeKindTest;
import org.orbeon.saxon.style.StandardNames;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.value.AnyURIValue;

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
        adoptChildExpression(name);
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

    public ItemType getItemType(TypeHierarchy th) {
        return NodeKindTest.NAMESPACE;
    }

    public int getCardinality() {
        return StaticProperty.EXACTLY_ONE;
    }

    protected void promoteInst(PromotionOffer offer) throws XPathException {
        if (select != null) {
            select = doPromotion(select, offer);
        }
        name = doPromotion(name, offer);
        super.promoteInst(offer);
    }

    public void localTypeCheck(StaticContext env, ItemType contextItemType) {}

    public Iterator iterateSubExpressions() {
        ArrayList list = new ArrayList(6);
        if (select != null) {
            list.add(select);
        }
        list.add(name);
        return list.iterator();
    }

    /**
     * Replace one subexpression by a replacement subexpression
     * @param original the original subexpression
     * @param replacement the replacement subexpression
     * @return true if the original subexpression is found
     */

    public boolean replaceSubExpression(Expression original, Expression replacement) {
        boolean found = false;
        if (select == original) {
            select = replacement;
            found = true;
        }
        if (name == original) {
            name = replacement;
            found = true;
        }
                return found;
    }


    private String evaluatePrefix(XPathContext context) throws XPathException {
        String prefix = name.evaluateAsString(context).trim();
        if (!(prefix.equals("") || context.getConfiguration().getNameChecker().isValidNCName(prefix))) {
            DynamicError err = new DynamicError("Namespace prefix is invalid: " + prefix, this);
            err.setErrorCode("XTDE0920");
            err.setXPathContext(context);
            throw dynamicError(this, err, context);
        }

        if (prefix.equals("xmlns")) {
            DynamicError err = new DynamicError("Namespace prefix 'xmlns' is not allowed", this);
            err.setErrorCode("XTDE0920");
            err.setXPathContext(context);
            throw dynamicError(this, err, context);
        }
        return prefix;
    }

    public int evaluateNameCode(XPathContext context) throws XPathException {
        String prefix = evaluatePrefix(context);
        return context.getNamePool().allocate("", "", prefix);
    }

    public TailCall processLeavingTail(XPathContext context) throws XPathException {
        Controller controller = context.getController();
        String prefix = evaluatePrefix(context);
        String uri = expandChildren(context).toString();

        // TODO: these checks are not being performed when evaluateItem() is used
        if (prefix.equals("xml") != uri.equals(NamespaceConstant.XML)) {
            DynamicError err = new DynamicError("Namespace prefix 'xml' and namespace uri " + NamespaceConstant.XML +
                    " must only be used together", this);
            err.setErrorCode("XTDE0925");
            err.setXPathContext(context);
            throw dynamicError(this, err, context);
        }

        if (uri.equals("")) {
            DynamicError err = new DynamicError("Namespace URI is an empty string", this);
            err.setErrorCode("XTDE0930");
            err.setXPathContext(context);
            //context.getController().recoverableError(err);
            throw dynamicError(this, err, context);
        }

        if (!AnyURIValue.isValidURI(uri)) {
            DynamicError de = new DynamicError(
                    "The string value of the constructed namespace node must be a valid URI");
            de.setErrorCode("XTDE0905");
            de.setXPathContext(context);
            de.setLocator(this);
            throw de;
        }

        int nscode = controller.getNamePool().allocateNamespaceCode(prefix, uri);
        SequenceReceiver out = context.getReceiver();
        out.namespace(nscode, ReceiverOptions.REJECT_DUPLICATES);
        return null;
    }

    /**
     * Display this instruction as an expression, for diagnostics
     */

    public void display(int level, PrintStream out, Configuration config) {
        out.println(ExpressionTool.indent(level) + "namespace");
        name.display(level+1, out, config);
        super.display(level+1, out, config);
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
