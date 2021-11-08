package org.orbeon.saxon.instruct;
import org.orbeon.saxon.event.SequenceReceiver;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.StandardNames;
import org.orbeon.saxon.pattern.NodeKindTest;
import org.orbeon.saxon.trace.ExpressionPresenter;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.TypeHierarchy;


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

    public ItemType getItemType(TypeHierarchy th) {
        return NodeKindTest.COMMENT;
    }

    public int getCardinality() {
        return StaticProperty.EXACTLY_ONE;
    }


    /**
     * Copy an expression. This makes a deep copy.
     *
     * @return the copy of the original expression
     */

    public Expression copy() {
        Comment exp = new Comment();
        try {
            exp.setSelect(select.copy(), getExecutable().getConfiguration());
        } catch (XPathException err) {
            throw new UnsupportedOperationException(err.getMessage());
        }
        return exp;
    }

    public void localTypeCheck(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        // Do early checking of content if known statically

        if (select instanceof Literal) {
            String s = ((Literal)select).getValue().getStringValue();
            String s2 = checkContent(s, visitor.getStaticContext().makeEarlyEvaluationContext());
            if (!s2.equals(s)) {
                setSelect(new StringLiteral(s2), visitor.getConfiguration());
            }
        }
    }


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
     * @throws XPathException if the content is invalid
     */

    protected String checkContent(String comment, XPathContext context) throws XPathException {
        while(true) {
            int hh = comment.indexOf("--");
            if (hh < 0) break;
            if (isXSLT()) {
                comment = comment.substring(0, hh+1) + ' ' + comment.substring(hh+1);
            } else {
                XPathException err = new XPathException("Invalid characters (--) in comment", this);
                err.setErrorCode("XQDY0072");
                err.setXPathContext(context);
                throw dynamicError(this, err, context);
            }
        }
        if (comment.length()>0 && comment.charAt(comment.length()-1)=='-') {
            if (isXSLT()) {
                comment = comment + ' ';
            } else {
                XPathException err = new XPathException("Comment cannot end in '-'", this);
                err.setErrorCode("XQDY0072");
                err.setXPathContext(context);
                throw dynamicError(this, err, context);
            }
        }
        return comment;
    }

    /**
     * Diagnostic print of expression structure. The abstract expression tree
     * is written to the supplied output destination.
     */

    public void explain(ExpressionPresenter out) {
        out.startElement("comment");
        getSelect().explain(out);
        out.endElement();
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
