package net.sf.saxon.instruct;
import net.sf.saxon.event.SequenceReceiver;
import net.sf.saxon.expr.ExpressionTool;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.expr.StaticProperty;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.pattern.NodeKindTest;
import net.sf.saxon.style.StandardNames;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;

import java.io.PrintStream;


/**
* An instruction representing an xsl:comment element in the stylesheet.
*/

public final class Comment extends SimpleNodeConstructor {

    /**
    * Construct the instruction
    */

    public Comment() {}

    /**
    * Get the instruction name, for diagnostics and tracing
    * return the string "xsl:comment"
    */

    public int getInstructionNameCode() {
        return StandardNames.XSL_COMMENT;
    }

    public ItemType getItemType() {
        return NodeKindTest.COMMENT;
    }

    public int getCardinality() {
        return StaticProperty.EXACTLY_ONE;
    }

    public void typeCheck(StaticContext env, ItemType contextItemType) {}


    /**
    * Process this instruction, to output a Comment Node
    * @param context the dynamic context for this transformation
    * @return a TailCall representing a call delegated to the caller. Always
    * returns null in this implementation
    */

    public TailCall processLeavingTail(XPathContext context) throws XPathException {
        String comment = expandChildren(context).toString();
        comment = checkContent(comment, context);
        SequenceReceiver out = context.getReceiver();
        out.comment(comment, locationId, 0);
        return null;
    }

    /**
     * Check the content of the node, and adjust it if necessary
     *
     * @param comment    the supplied content
     * @param context the dynamic context
     * @return the original content, unless adjustments are needed
     * @throws net.sf.saxon.trans.DynamicError
     *          if the content is invalid
     */

    protected String checkContent(String comment, XPathContext context) throws DynamicError {
        while(true) {
            int hh = comment.indexOf("--");
            if (hh < 0) break;
            DynamicError err = new DynamicError("Invalid characters (--) in comment", this);
            err.setErrorCode((isXSLT(context) ? "XT0950" : "XQ0072"));
            err.setXPathContext(context);
            context.getController().recoverableError(err);
            comment = comment.substring(0, hh+1) + ' ' + comment.substring(hh+1);
        }
        if (comment.length()>0 && comment.charAt(comment.length()-1)=='-') {
            DynamicError err = new DynamicError("Invalid character (-) at end of comment", this);
            err.setErrorCode((isXSLT(context) ? "XT0950" : "XQ0072"));
            err.setXPathContext(context);
            context.getController().recoverableError(err);
            comment = comment + ' ';
        }        
        return comment;
    }

    public void display(int level, NamePool pool, PrintStream out) {
        out.println(ExpressionTool.indent(level) + "comment");
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
