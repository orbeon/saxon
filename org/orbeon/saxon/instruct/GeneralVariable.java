package net.sf.saxon.instruct;
import net.sf.saxon.expr.*;
import net.sf.saxon.om.*;
import net.sf.saxon.pattern.NoNodeTest;
import net.sf.saxon.style.StandardNames;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.value.SequenceType;

import java.io.PrintStream;
import java.util.Collections;
import java.util.Iterator;

/**
* This class defines common behaviour across xsl:variable, xsl:param, and xsl:with-param;
* also saxon:assign
*/

public abstract class GeneralVariable extends Instruction implements Binding {


    private static final int ASSIGNABLE = 1;
    private static final int REQUIRED = 4;
    private static final int TUNNEL = 8;

    private byte properties = 0;
    Expression select = null;
    protected int nameCode = -1;
    SequenceType requiredType;
    private int slotNumber;
    private String variableName;


    public GeneralVariable() {};

    public void init(   Expression select,
                        int nameCode) {
        this.select = select;
        this.nameCode = nameCode;
    }

    public void setSelectExpression(Expression select) {
        this.select = select;
    }

    public Expression getSelectExpression() {
        return select;
    }

    public void setRequiredType(SequenceType required) {
        requiredType = required;
    }

    public SequenceType getRequiredType() {
        return requiredType;
    }

    public void setNameCode(int nameCode) {
        this.nameCode = nameCode;
    }

    public int getNameCode() {
        return nameCode;
    }

    public void setAssignable(boolean assignable) {
        if (assignable) {
            properties |= ASSIGNABLE;
        } else {
            properties &= ~ASSIGNABLE;
        }
    }

    public void setRequiredParam(boolean requiredParam) {
        if (requiredParam) {
            properties |= REQUIRED;
        } else {
            properties &= ~REQUIRED;
        }
    }



    public void setTunnel(boolean tunnel) {
        if (tunnel) {
            properties |= TUNNEL;
        } else {
            properties &= ~TUNNEL;
        }
    }

    /**
    * Test whether it is permitted to assign to the variable using the saxon:assign
    * extension element. This will only be true if the extra attribute saxon:assignable="yes"
    * is present.
    */

    public final boolean isAssignable() {
        return (properties & ASSIGNABLE) != 0;
    }

    /**
     * Get the name of the variable (as a NamePool fingerprint)
     * @return the NamePool fingerprint of the variable's expanded name.
     */

    public int getVariableFingerprint() {
        return nameCode & 0xfffff;
    }

    /**
     * Get the type of the result of this instruction. An xsl:variable instruction returns nothing, so the
     * type is empty.
     * @return the empty type.
     */

    public ItemType getItemType() {
        return NoNodeTest.getInstance();
    }

    /**
     * Get the cardinality of the result of this instruction. An xsl:variable instruction returns nothing, so the
     * type is empty.
     * @return the empty cardinality.
     */

    public int getCardinality() {
        return StaticProperty.EMPTY;
    }

    public boolean isGlobal() {
        return false;
    }



    public final boolean isRequiredParam() {
        return (properties & REQUIRED) != 0;
    }

    public final boolean isTunnelParam() {
        return (properties & TUNNEL) != 0;
    }

    public int getInstructionNameCode() {
        return StandardNames.XSL_VARIABLE;
    }

    public Expression simplify(StaticContext env) throws XPathException {
        if (select != null) {
            select = select.simplify(env);
        }
        return this;
    }

    public Expression analyze(StaticContext env, ItemType contextItemType) throws XPathException {
        if (select != null) {
            select = select.analyze(env, contextItemType);
        }
        checkAgainstRequiredType(env);
        return this;
    }

    /**
     * Check the select expression against the required type.
     * @param env
     * @throws XPathException
     */

    private void checkAgainstRequiredType(StaticContext env)
    throws XPathException {
        // Note, in some cases we are doing this twice.
        RoleLocator role = new RoleLocator(RoleLocator.VARIABLE, variableName, 0, null);
        role.setSourceLocator(this);
        SequenceType r = requiredType;
        if (r != null && select != null) {
            // check that the expression is consistent with the required type
            select = TypeChecker.staticTypeCheck(select, requiredType, false, role, env);
        }
    }

    /**
     * Evaluate an expression as a single item. This always returns either a single Item or
     * null (denoting the empty sequence). No conversion is done. This method should not be
     * used unless the static type of the expression is a subtype of "item" or "item?": that is,
     * it should not be called if the expression may return a sequence. There is no guarantee that
     * this condition will be detected.
     *
     * @param context The context in which the expression is to be evaluated
     * @exception XPathException if any dynamic error occurs evaluating the
     *     expression
     * @return the node or atomic value that results from evaluating the
     *     expression; or null to indicate that the result is an empty
     *     sequence
     */

    public Item evaluateItem(XPathContext context) throws XPathException {
        process(context);
        return null;
    }

    /**
     * Return an Iterator to iterate over the values of a sequence. The value of every
     * expression can be regarded as a sequence, so this method is supported for all
     * expressions. This default implementation relies on the process() method: it
     * "pushes" the results of the instruction to a sequence in memory, and then
     * iterates over this in-memory sequence.
     *
     * In principle instructions should implement a pipelined iterate() method that
     * avoids the overhead of intermediate storage.
     *
     * @exception XPathException if any dynamic error occurs evaluating the
     *     expression
     * @param context supplies the context for evaluation
     * @return a SequenceIterator that can be used to iterate over the result
     *     of the expression
     */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        evaluateItem(context);
        return EmptyIterator.getInstance();
    }

    /**
     * Evaluate the variable. That is,
     * get the value of the select expression if present or the content
     * of the element otherwise, either as a tree or as a sequence
    */

    public ValueRepresentation getSelectValue(XPathContext context) throws XPathException {
        if (select==null) {
            throw new AssertionError("*** No select expression!!");
            // The value of the variable is a sequence of nodes and/or atomic values

        } else {
            // There is a select attribute: do a lazy evaluation of the expression,
            // which will already contain any code to force conversion to the required type.

            if (isAssignable()) {
                return ExpressionTool.eagerEvaluate(select, context);
            } else {
                return ExpressionTool.lazyEvaluate(select, context, true);
            }

        }
    }

    /**
     * Handle promotion offers, that is, non-local tree rewrites.
     * @param offer The type of rewrite being offered
     * @throws XPathException
     */

    protected void promoteInst(PromotionOffer offer) throws XPathException {
        if (select != null) {
            select.promote(offer);
        }
    }

    /**
     * Get all the XPath expressions associated with this instruction
     * (in XSLT terms, the expression present on attributes of the instruction,
     * as distinct from the child instructions in a sequence construction)
     */


    public Iterator iterateSubExpressions() {
        if (select != null) {
            return new MonoIterator(select);
        } else {
            return Collections.EMPTY_LIST.iterator();
        }
    }

    /**
     * Diagnostic print of expression structure. The expression is written to the System.err
     * output stream
     *
     * @param level indentation level for this expression
     * @param out
     */

    public void display(int level, NamePool pool, PrintStream out) {
        out.println(ExpressionTool.indent(level) + "variable " +
                pool.getDisplayName(nameCode));
        if (select != null) {
            select.display(level+1, pool, out);
        }
    }

    public int getSlotNumber() {
        return slotNumber;
    }

    public void setSlotNumber(int s) {
        slotNumber = s;
    }

    public void setVariableName(String s) {
        variableName = s;
    }

    public String getVariableName() {
        return variableName;
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
