package org.orbeon.saxon.instruct;

import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.NamePool;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.StringValue;
import org.orbeon.saxon.xpath.XPathException;

import java.io.PrintStream;
import java.util.Iterator;

/**
 * This class implements the rules for an XSLT simple content constructor, which are used in constructing
 * the string value of an attribute node, text node, comment node, etc, from the value of the select
 * expression or the contained sequence constructor.
 */

public class SimpleContentConstructor extends ComputedExpression {

    Expression select;
    Expression separator;

    public SimpleContentConstructor(Expression select, Expression separator) {
        this.select = select;
        this.separator = separator;
        adoptChildExpression(select);
        adoptChildExpression(separator);
    }

    /**
     * Compute the cardinality of the result of the expression.
     * @return the cardinality, @link {StaticProperty.EXACTLY_ONE}
     */

    protected int computeCardinality() {
        return StaticProperty.EXACTLY_ONE;

    }

    /**
     * Perform static analysis and optimisation of an expression and its subexpressions.
     * <p/>
     * <p>This checks statically that the operands of the expression have
     * the correct type; if necessary it generates code to do run-time type checking or type
     * conversion. A static type error is reported only if execution cannot possibly succeed, that
     * is, if a run-time type error is inevitable. The call may return a modified form of the expression.</p>
     * <p/>
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable. However, the types of such functions and
     * variables may not be accurately known if they have not been explicitly declared.</p>
     *
     * @param env             the static context of the expression
     * @param contextItemType the static type of "." at the point where this expression is invoked.
     *                        The parameter is set to null if it is known statically that the context item will be undefined.
     *                        If the type of the context item is not known statically, the argument is set to
     *                        {@link org.orbeon.saxon.type.Type#ITEM_TYPE}
     * @return the original expression, rewritten to perform necessary
     *         run-time type checks, and to perform other type-related
     *         optimizations
     * @throws org.orbeon.saxon.xpath.StaticError if an error is discovered during this phase
     *                                        (typically a type error)
     */

    public Expression analyze(StaticContext env, ItemType contextItemType) throws XPathException {
        select = select.analyze(env, contextItemType);
        separator = separator.analyze(env, contextItemType);
        return this;
    }

    /**
     * Determine the data type of the expression, if possible. All expression return
     * sequences, in general; this method determines the type of the items within the
     * sequence, assuming that (a) this is known in advance, and (b) it is the same for
     * all items in the sequence.
     * <p/>
     * <p>This method should always return a result, though it may be the best approximation
     * that is available at the time.</p>
     *
     * @return a value such as Type.STRING, Type.BOOLEAN, Type.NUMBER,
     *         Type.NODE, or Type.ITEM (meaning not known at compile time)
     */

    public ItemType getItemType() {
        return Type.STRING_TYPE;
    }

    /**
     * Diagnostic print of expression structure. The expression is written to the System.err
     * output stream
     *
     * @param level indentation level for this expression
     * @param pool  NamePool used to expand any names appearing in the expression
     * @param out   Output destination
     */

    public void display(int level, NamePool pool, PrintStream out) {
        out.println(ExpressionTool.indent(level) + "construct simple content");
        select.display(level+1, pool, out);
        separator.display(level+1, pool, out);
    }

    /**
     * Simplify an expression. This performs any static optimization (by rewriting the expression
     * as a different expression). The default implementation does nothing.
     *
     * @return the simplified expression
     * @throws org.orbeon.saxon.xpath.XPathException
     *          if an error is discovered during expression
     *          rewriting
     */

    public Expression simplify(StaticContext env) throws XPathException {
        select = select.simplify(env);
        separator = separator.simplify(env);
        return this;
    }

     /**
     * Get the immediate sub-expressions of this expression. Default implementation
     * returns a zero-length array, appropriate for an expression that has no
     * sub-expressions.
     *
     * @return an iterator containing the sub-expressions of this expression
     */

    public Iterator iterateSubExpressions() {
        return new PairIterator(select, separator);
    }

    /**
     * Evaluate an expression as a single item. This always returns either a single Item or
     * null (denoting the empty sequence). No conversion is done. This method should not be
     * used unless the static type of the expression is a subtype of "item" or "item?": that is,
     * it should not be called if the expression may return a sequence. There is no guarantee that
     * this condition will be detected.
     *
     * @param context The context in which the expression is to be evaluated
     * @return the node or atomic value that results from evaluating the
     *         expression; or null to indicate that the result is an empty
     *         sequence
     * @throws org.orbeon.saxon.xpath.XPathException
     *          if any dynamic error occurs evaluating the
     *          expression
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        SequenceIterator iter = select.iterate(context);
        StringBuffer sb = new StringBuffer(200);
        boolean prevText = false;
        boolean first = true;
        String sep = null;
        while (true) {
            Item item = iter.next();
            if (item==null) {
                break;
            }
            if (item instanceof NodeInfo) {
                // TODO: this special handling of text nodes is incorrect for XQuery
                // TODO: this anticipates changes to XSLT to align attribute value templates with xsl:attribute
                if (((NodeInfo)item).getNodeKind() == Type.TEXT) {
                    String s = item.getStringValue();
                    if (s.length() > 0) {
                        if (!first && !prevText) {
                            if (sep == null) {
                                sep = separator.evaluateItem(context).getStringValue();
                            }
                            sb.append(sep);
                        }
                        first = false;
                        sb.append(s);
                        prevText = true;
                    }
                } else {
                    prevText = false;
                    SequenceIterator iter2 = item.getTypedValue();
                    while (true) {
                        Item item2 = iter2.next();
                        if (item2 == null) {
                            break;
                        }
                        if (!first) {
                            if (sep == null) {
                                sep = separator.evaluateItem(context).getStringValue();
                            }
                            sb.append(sep);
                        }
                        first = false;
                        sb.append(item2.getStringValue());
                    }
                }
            } else {
                if (!first) {
                    if (sep == null) {
                        sep = separator.evaluateItem(context).getStringValue();
                    }
                    sb.append(sep);
                }
                first = false;
                String s = item.getStringValue();
                sb.append(s);
            }
        }
        return new StringValue(sb);
    }

    /**
     * An implementation of Expression must provide at least one of the methods evaluateItem(), iterate(), or process().
     * This method indicates which of these methods is prefered.
     */

    public int getImplementationMethod() {
        return Expression.EVALUATE_METHOD;
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
