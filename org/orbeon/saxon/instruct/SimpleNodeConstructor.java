package net.sf.saxon.instruct;
import net.sf.saxon.Controller;
import net.sf.saxon.event.SequenceOutputter;
import net.sf.saxon.expr.*;
import net.sf.saxon.om.*;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.StringValue;
import net.sf.saxon.xpath.DynamicError;
import net.sf.saxon.xpath.XPathException;
import net.sf.saxon.xpath.StaticError;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Common superclass for XSLT instructions whose content template produces a text
 * value: xsl:attribute, xsl:comment, xsl:processing-instruction, xsl:namespace,
 * and xsl:text
 */

public abstract class SimpleNodeConstructor extends InstructionWithChildren {

    protected Expression select = null;
    protected Expression separator = null;

    public void setSelect(Expression select) throws StaticError {
        this.select = select;
        adoptChildExpression(select);
    }

    public final void setSeparator(Expression separator) {
        this.separator = separator;
        adoptChildExpression(separator);
    }

    public Expression simplify(StaticContext env) throws XPathException {
        if (select != null) {
            select = select.simplify(env);
        }
        if (separator != null) {
            separator = separator.simplify(env);
        }
        return super.simplify(env);
    }

    public abstract void typeCheck(StaticContext env, ItemType contextItemType) throws XPathException;

    /**
     * The analyze() method is called in XQuery, where node constructors
     * are implemented as Expressions. In this case the required type for the
     * select expression is a single string.
     * @param env The static context for the query
     * @return the rewritten expression
     * @throws XPathException if any static errors are found in this expression
     * or any of its children
     */

    public Expression analyze(StaticContext env, ItemType contextItemType) throws XPathException {
        typeCheck(env, contextItemType);
        String instName = env.getNamePool().getDisplayName(getInstructionNameCode());
        if (separator != null) {
            separator = separator.analyze(env, contextItemType);
            RoleLocator role =
                new RoleLocator(RoleLocator.INSTRUCTION, instName + "/separator", 0);
            separator = TypeChecker.staticTypeCheck(separator, SequenceType.SINGLE_STRING, false, role, env);
        }
        if (select != null) {
            select = select.analyze(env, contextItemType);
            if (!Type.isSubType(select.getItemType(), Type.ANY_ATOMIC_TYPE)) {
                select = new Atomizer(select);
            }
            if (!Type.isSubType(select.getItemType(), Type.STRING_TYPE)) {
                select = new AtomicSequenceConverter(select, Type.STRING_TYPE);
            }
        } else {
            super.analyze(env, contextItemType);
        }
        return this;
    }

    public Iterator iterateSubExpressions() {
        ArrayList list = new ArrayList(10);
        if (children != null) {
            list.addAll(Arrays.asList(children));
        }
        if (select != null && !(select instanceof StringValue)) {
            list.add(select);
        }
        if (separator != null && !(separator instanceof StringValue)) {
            list.add(separator);
        }
        return list.iterator();
    }

    /**
    * Expand the stylesheet elements subordinate to this one, returning the result
    * as a string. The expansion must not generate any element or attribute nodes.
    * @param context The dynamic context for the transformation
    */

    public CharSequence expandChildren(XPathContext context) throws XPathException {

        String sep = " ";
        if (separator != null) {
            sep = separator.evaluateAsString(context);
        }

        if (select != null) {
            if (select instanceof StringValue) {
                return ((StringValue)select).getStringValue();
            } else {
                return flatten(select.iterate(context), sep);
            }

        } else {
            Controller controller = context.getController();
            XPathContext c2 = context.newMinorContext();
            SequenceOutputter seq = new SequenceOutputter();
            seq.setConfiguration(controller.getConfiguration());
            seq.setDocumentLocator(getExecutable().getLocationMap());
            c2.setTemporaryReceiver(seq);
            // process the child elements in the stylesheet
            processChildren(c2);
            seq.close();
            return flatten(seq.getSequence().iterate(context), sep);

        }
    }

    /**
     * Flatten the value of the sequence to a character string
     */

    private StringBuffer flatten(SequenceIterator content, String separator)
    throws XPathException {
        StringBuffer buffer = new StringBuffer(80);
        boolean first = true;
        while(true) {
            Item item = content.next();
            if (item == null) {
                break;
            }
            if (first) {
                first = false;
            } else {
                buffer.append(separator);
            }
            buffer.append(item.getStringValue());
        }
        return buffer;
    }

    /**
     * Evaluate as an expression. We rely on the fact that when these instructions
     * are generated by XQuery, there will always be a valueExpression to evaluate
     * the content
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        String content = (select==null ?
                    "" :
                    select.evaluateAsString(context));
        content = checkContent(content, context);
        try {
            Orphan o = new Orphan(context.getController().getNamePool());
            o.setNodeKind((short)getItemType().getPrimitiveType());
            o.setStringValue(content);
            o.setNameCode(evaluateNameCode(context));
            return o;
        } catch (SkipInstructionException skip) {
            // error recovery path
            return null;
        }
    }

    /**
     * Check the content of the node, and adjust it if necessary. The checks depend on the node kind.
     * @param data the supplied content
     * @param context the dynamic context
     * @return the original content, unless adjustments are needed
     * @throws DynamicError if the content is invalid
     */

    protected String checkContent(String data, XPathContext context) throws DynamicError {
        return data;
    }

    protected int evaluateNameCode(XPathContext context)
    throws XPathException, XPathException {
        return -1;
    }

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        return SingletonIterator.makeIterator(evaluateItem(context));
    }

    /**
     * Display this instruction as an expression, for diagnostics
     */

    public void display(int level, NamePool pool, PrintStream out) {
        if (select != null) {
            select.display(level, pool, out);
        } else if (children.length==0) {
            out.println(ExpressionTool.indent(level) + "empty content");
        } else {
            InstructionWithChildren.displayChildren(children, level+1, pool, out);
        }
    }

    /**
      * Offer promotion for subexpressions. The offer will be accepted if the subexpression
      * is not dependent on the factors (e.g. the context item) identified in the PromotionOffer.
      * By default the offer is not accepted - this is appropriate in the case of simple expressions
      * such as constant values and variable references where promotion would give no performance
      * advantage. This method is always called at compile time.
      *
      * @param offer details of the offer, for example the offer to move
      *     expressions that don't depend on the context to an outer level in
      *     the containing expression
      * @exception XPathException if any error is detected
      */

     protected void promoteInst(PromotionOffer offer) throws XPathException {
         if (select != null) {
             select = select.promote(offer);
         }
         super.promoteInst(offer);
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
