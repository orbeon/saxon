package net.sf.saxon.instruct;

import net.sf.saxon.expr.*;
import net.sf.saxon.functions.Matches;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NamePool;
import net.sf.saxon.type.RegexTranslator;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;
import net.sf.saxon.xpath.XPathException;
import net.sf.saxon.xpath.DynamicError;
import net.sf.saxon.style.StandardNames;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.pattern.NoNodeTest;

import java.util.Iterator;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.io.PrintStream;

/**
 * An xsl:analyze-string elements in the stylesheet. New at XSLT 2.0<BR>
 */

public class AnalyzeString extends Instruction {

    private Expression select;
    private Expression regex;
    private Expression flags;
    private Expression matching;
    private Expression nonMatching;
    private Pattern pattern;    // a regex pattern, not an XSLT pattern!

    /**
     * Construct an AnalyzeString instruction
     *
     * @param select      the expression containing the input string
     * @param regex       the regular expression
     * @param flags       the flags parameter
     * @param matching    actions to be applied to a matching substring
     * @param nonMatching actions to be applied to a non-matching substring
     * @param pattern     the compiled regular expression, if it was known statically
     */
    public AnalyzeString(Expression select,
                         Expression regex,
                         Expression flags,
                         Expression matching,
                         Expression nonMatching,
                         Pattern pattern) {
        this.select = select;
        this.regex = regex;
        this.flags = flags;
        this.matching = matching;
        this.nonMatching = nonMatching;
        this.pattern = pattern;

    }

    public int getInstructionNameCode() {
        return StandardNames.XSL_ANALYZE_STRING;
    }

    /**
     * Simplify an expression. This performs any static optimization (by rewriting the expression
     * as a different expression).
     *
     * @return the simplified expression
     * @throws XPathException if an error is discovered during expression
     *                        rewriting
     */

    public Expression simplify(StaticContext env) throws XPathException {
        select = select.simplify(env);
        regex = regex.simplify(env);
        flags = flags.simplify(env);
        if (matching != null) {
            matching = matching.simplify(env);
        }
        if (nonMatching != null) {
            nonMatching = nonMatching.simplify(env);
        }
        return this;
    }

    /**
     * Perform static analysis of an expression and its subexpressions.
     * <p/>
     * <p>This checks statically that the operands of the expression have
     * the correct type; if necessary it generates code to do run-time type checking or type
     * conversion. A static type error is reported only if execution cannot possibly succeed, that
     * is, if a run-time type error is inevitable. The call may return a modified form of the expression.</p>
     * <p/>
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable. However, the types of such functions and
     * variables will only be accurately known if they have been explicitly declared.</p>
     *
     * @param env the static context of the expression
     * @return the original expression, rewritten to perform necessary
     *         run-time type checks, and to perform other type-related
     *         optimizations
     * @throws XPathException if an error is discovered during this phase
     *                        (typically a type error)
     */

    public Expression analyze(StaticContext env, ItemType contextItemType) throws XPathException {
        select = select.analyze(env, contextItemType);
        regex = regex.analyze(env, contextItemType);
        flags = flags.analyze(env, contextItemType);
        if (matching != null) {
            matching = matching.analyze(env, Type.STRING_TYPE);
        }
        if (nonMatching != null) {
            nonMatching = nonMatching.analyze(env, Type.STRING_TYPE);
        }
        // Following type checking has already been done in the case of XSLT xsl:analyze-string, but is
        // needed where the instruction is generated from saxon:analyze-string extension function
        RoleLocator role =
                new RoleLocator(RoleLocator.INSTRUCTION, "analyze-string/select", 0);
        select = TypeChecker.staticTypeCheck(select, SequenceType.SINGLE_STRING, false, role, env);

        role =
                new RoleLocator(RoleLocator.INSTRUCTION, "analyze-string/regex", 0);
        regex = TypeChecker.staticTypeCheck(regex, SequenceType.SINGLE_STRING, false, role, env);

        role =
                new RoleLocator(RoleLocator.INSTRUCTION, "analyze-string/flags", 0);
        flags = TypeChecker.staticTypeCheck(flags, SequenceType.SINGLE_STRING, false, role, env);

        return this;
    }

    /**
     * Get the item type of the items returned by evaluating this instruction
     *
     * @return the static item type of the instruction
     */

    public ItemType getItemType() {
        if (matching != null) {
            if (nonMatching != null) {
                return Type.getCommonSuperType(matching.getItemType(), nonMatching.getItemType());
            } else {
                return matching.getItemType();
            }
        } else {
            if (nonMatching != null) {
                return nonMatching.getItemType();
            } else {
                return NoNodeTest.getInstance();
            }
        }
    }

    /**
     * Compute the dependencies of an expression, as the union of the
     * dependencies of its subexpressions. (This is overridden for path expressions
     * and filter expressions, where the dependencies of a subexpression are not all
     * propogated). This method should be called only once, to compute the dependencies;
     * after that, getDependencies should be used.
     *
     * @return the depencies, as a bit-mask
     */

    public int computeDependencies() {
        // some of the dependencies in the "action" part and in the grouping and sort keys aren't relevant,
        // because they don't depend on values set outside the for-each-group expression
        int dependencies = 0;
        dependencies |= select.getDependencies();
        dependencies |= regex.getDependencies();
        dependencies |= flags.getDependencies();
        if (matching != null) {
            dependencies |= (matching.getDependencies() &~
                    (StaticProperty.DEPENDS_ON_FOCUS | StaticProperty.DEPENDS_ON_REGEX_GROUP));
        }
        if (nonMatching != null) {
            dependencies |= (nonMatching.getDependencies() &~
                    (StaticProperty.DEPENDS_ON_FOCUS | StaticProperty.DEPENDS_ON_REGEX_GROUP));
        }
        return dependencies;
    }

    /**
     * Handle promotion offers, that is, non-local tree rewrites.
     *
     * @param offer The type of rewrite being offered
     * @throws XPathException
     */

    protected void promoteInst(PromotionOffer offer) throws XPathException {
        select = select.promote(offer);
        regex = regex.promote(offer);
        flags = flags.promote(offer);
        if (matching != null) {
            matching = matching.promote(offer);
        }
        if (nonMatching != null) {
            nonMatching = nonMatching.promote(offer);
        }
    }

    /**
     * Get all the XPath expressions associated with this instruction
     * (in XSLT terms, the expression present on attributes of the instruction,
     * as distinct from the child instructions in a sequence construction)
     */

    public Iterator iterateSubExpressions() {
        ArrayList list = new ArrayList(5);
        list.add(select);
        list.add(regex);
        list.add(flags);
        if (matching != null) {
            list.add(matching);
        }
        if (nonMatching != null) {
            list.add(nonMatching);
        }
        return list.iterator();
    }

    public TailCall processLeavingTail(XPathContext context) throws XPathException {
        RegexIterator iter = getRegexIterator(context);
        XPathContextMajor c2 = context.newContext();
        c2.setOrigin(this);
        c2.setCurrentIterator(iter);
        c2.setCurrentRegexIterator(iter);

        while (true) {
            Item it = iter.next();
            if (it == null) {
                break;
            }
            if (iter.isMatching()) {
                if (matching != null) {
                    matching.process(c2);
                }
            } else {
                if (nonMatching != null) {
                    nonMatching.process(c2);
                }
            }
        }

        return null;

    }

    /**
     * Get an iterator over the substrings defined by the regular expression
     *
     * @param context the evaluation context
     * @return an iterator that returns matching and nonmatching substrings
     * @throws XPathException
     */

    private RegexIterator getRegexIterator(XPathContext context) throws XPathException {
        String input = select.evaluateAsString(context);

        Pattern re = pattern;
        if (re == null) {
            int jflags = Matches.setFlags(flags.evaluateAsString(context));
            try {
                String javaRegex = RegexTranslator.translate(regex.evaluateAsString(context), true);
                re = Pattern.compile(javaRegex, jflags);
            } catch (RegexTranslator.RegexSyntaxException err) {
                throw new DynamicError(err);
            } catch (PatternSyntaxException err) {
                throw new DynamicError(err);
            }
        }

        RegexIterator iter = new RegexIterator(input, re);
        return iter;
    }

    // TODO: implement an iterate() method (as a MappingIterator).

    /**
     * Diagnostic print of expression structure. The expression is written to the System.err
     * output stream
     *
     * @param level indentation level for this expression
     * @param out
     */

    public void display(int level, NamePool pool, PrintStream out) {
        out.println(ExpressionTool.indent(level) + "analyze-string");
        out.println(ExpressionTool.indent(level) + "select = ");
        select.display(level + 1, pool, out);
        out.println(ExpressionTool.indent(level) + "regex = ");
        regex.display(level + 1, pool, out);
        out.println(ExpressionTool.indent(level) + "flags = ");
        flags.display(level + 1, pool, out);
        if (matching != null) {
            out.println(ExpressionTool.indent(level) + "matching = ");
            matching.display(level + 1, pool, out);
        }
        if (nonMatching != null) {
            out.println(ExpressionTool.indent(level) + "non-matching = ");
            nonMatching.display(level + 1, pool, out);
        }
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
// The Initial Developer of the Original Code is Michael H. Kay
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s):
// Portions marked "e.g." are from Edwin Glaser (edwin@pannenleiter.de)
//
