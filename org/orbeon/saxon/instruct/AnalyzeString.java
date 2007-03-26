package org.orbeon.saxon.instruct;

import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.Platform;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.SequenceIterator;
import org.orbeon.saxon.om.EmptyIterator;
import org.orbeon.saxon.pattern.NoNodeTest;
import org.orbeon.saxon.regex.RegexIterator;
import org.orbeon.saxon.regex.RegularExpression;
import org.orbeon.saxon.style.StandardNames;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.SchemaType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.type.TypeHierarchy;
import org.orbeon.saxon.value.SequenceType;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * An xsl:analyze-string elements in the stylesheet. New at XSLT 2.0<BR>
 */

public class AnalyzeString extends Instruction {

    private Expression select;
    private Expression regex;
    private Expression flags;
    private Expression matching;
    private Expression nonMatching;
    private RegularExpression pattern;

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
                         RegularExpression pattern) {
        this.select = select;
        this.regex = regex;
        this.flags = flags;
        this.matching = matching;
        this.nonMatching = nonMatching;
        this.pattern = pattern;

        Iterator kids = iterateSubExpressions();
        while (kids.hasNext()) {
            Expression child = (Expression)kids.next();
            adoptChildExpression(child);
        }

    }

    public int getInstructionNameCode() {
        return StandardNames.XSL_ANALYZE_STRING;
    }

    /**
     * An implementation of Expression must provide at least one of the methods evaluateItem(), iterate(), or process().
     * This method indicates which of these methods is prefered.
     */

    public int getImplementationMethod() {
        return Expression.PROCESS_METHOD | Expression.ITERATE_METHOD;
    }


    /**
     * Get the expression used to process matching substrings
     */

    public Expression getMatchingExpression() {
        return matching;
    }

    /**
     * Get the expression used to process non-matching substrings
     */

    public Expression getNonMatchingExpression() {
        return nonMatching;
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

    public Expression typeCheck(StaticContext env, ItemType contextItemType) throws XPathException {
        select = select.typeCheck(env, contextItemType);
        adoptChildExpression(select);
        regex = regex.typeCheck(env, contextItemType);
        adoptChildExpression(regex);
        flags = flags.typeCheck(env, contextItemType);
        adoptChildExpression(flags);
        if (matching != null) {
            matching = matching.typeCheck(env, Type.STRING_TYPE);
            adoptChildExpression(matching);
        }
        if (nonMatching != null) {
            nonMatching = nonMatching.typeCheck(env, Type.STRING_TYPE);
            adoptChildExpression(nonMatching);
        }
        // Following type checking has already been done in the case of XSLT xsl:analyze-string, but is
        // needed where the instruction is generated from saxon:analyze-string extension function
        RoleLocator role =
                new RoleLocator(RoleLocator.INSTRUCTION, "analyze-string/select", 0, null);
        role.setSourceLocator(this);
        select = TypeChecker.staticTypeCheck(select, SequenceType.SINGLE_STRING, false, role, env);

        role = new RoleLocator(RoleLocator.INSTRUCTION, "analyze-string/regex", 0, null);
        role.setSourceLocator(this);
        regex = TypeChecker.staticTypeCheck(regex, SequenceType.SINGLE_STRING, false, role, env);

        role = new RoleLocator(RoleLocator.INSTRUCTION, "analyze-string/flags", 0, null);
        role.setSourceLocator(this);
        flags = TypeChecker.staticTypeCheck(flags, SequenceType.SINGLE_STRING, false, role, env);

        return this;
    }


    public Expression optimize(Optimizer opt, StaticContext env, ItemType contextItemType) throws XPathException {
        select = select.optimize(opt, env, contextItemType);
        adoptChildExpression(select);
        regex = regex.optimize(opt, env, contextItemType);
        adoptChildExpression(regex);
        flags = flags.optimize(opt, env, contextItemType);
        adoptChildExpression(flags);
        if (matching != null) {
            matching = matching.optimize(opt, env, Type.STRING_TYPE);
            adoptChildExpression(matching);
        }
        if (nonMatching != null) {
            nonMatching = nonMatching.optimize(opt, env, Type.STRING_TYPE);
            adoptChildExpression(nonMatching);
        }
        return this;
    }

    /**
     * Check that any elements and attributes constructed or returned by this expression are acceptable
     * in the content model of a given complex type. It's always OK to say yes, since the check will be
     * repeated at run-time. The process of checking element and attribute constructors against the content
     * model of a complex type also registers the type of content expected of those constructors, so the
     * static validation can continue recursively.
     */

    public void checkPermittedContents(SchemaType parentType, StaticContext env, boolean whole) throws XPathException {
        if (matching != null) {
            matching.checkPermittedContents(parentType, env, false);
        }
        if (nonMatching != null) {
            nonMatching.checkPermittedContents(parentType, env, false);
        }
    }

    /**
     * Get the item type of the items returned by evaluating this instruction
     *
     * @return the static item type of the instruction
     * @param th
     */

    public ItemType getItemType(TypeHierarchy th) {
        if (matching != null) {
            if (nonMatching != null) {
                return Type.getCommonSuperType(matching.getItemType(th), nonMatching.getItemType(th), th);
            } else {
                return matching.getItemType(th);
            }
        } else {
            if (nonMatching != null) {
                return nonMatching.getItemType(th);
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
        select = doPromotion(select, offer);
        regex = doPromotion(regex, offer);
        flags = doPromotion(flags, offer);
        if (matching != null) {
            matching = doPromotion(matching, offer);
        }
        if (nonMatching != null) {
            nonMatching = doPromotion(nonMatching, offer);
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
        if (regex == original) {
            regex = replacement;
            found = true;
        }
        if (flags == original) {
            flags = replacement;
            found = true;
        }
        if (matching == original) {
            matching = replacement;
            found = true;
        }
        if (nonMatching == original) {
            nonMatching = replacement;
            found = true;
        }
                return found;
    }

    /**
    * ProcessLeavingTail: called to do the real work of this instruction. This method
    * must be implemented in each subclass. The results of the instruction are written
    * to the current Receiver, which can be obtained via the Controller.
    * @param context The dynamic context of the transformation, giving access to the current node,
    * the current variables, etc.
    * @return null if the instruction has completed execution; or a TailCall indicating
    * a function call or template call that is delegated to the caller, to be made after the stack has
    * been unwound so as to save stack space.
    */

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

        RegularExpression re = pattern;
        if (re == null) {
            String flagstr = flags.evaluateAsString(context);
            final Platform platform = context.getConfiguration().getPlatform();
            re = platform.compileRegularExpression(regex.evaluateAsString(context), true, flagstr);
            if (re.matches("")) {
                dynamicError("The regular expression must not be one that matches a zero-length string",
                        "XTDE1150", context);
            }
        }

        RegexIterator iter = re.analyze(input);
        return iter;
    }

    /**
     * Return an Iterator to iterate over the values of a sequence. The value of every
     * expression can be regarded as a sequence, so this method is supported for all
     * expressions. This default implementation handles iteration for expressions that
     * return singleton values: for non-singleton expressions, the subclass must
     * provide its own implementation.
     *
     * @param context supplies the context for evaluation
     * @return a SequenceIterator that can be used to iterate over the result
     *         of the expression
     * @throws org.orbeon.saxon.trans.XPathException
     *          if any dynamic error occurs evaluating the
     *          expression
     */

    public SequenceIterator iterate(XPathContext context) throws XPathException {
        RegexIterator iter = getRegexIterator(context);
        XPathContextMajor c2 = context.newContext();
        c2.setOrigin(this);
        c2.setCurrentIterator(iter);
        c2.setCurrentRegexIterator(iter);

        AnalyzeMappingFunction fn = new AnalyzeMappingFunction(iter, c2);
        return new ContextMappingIterator(fn, c2);
    }

    /**
     * Diagnostic print of expression structure. The expression is written to the System.err
     * output stream
     *
     * @param level indentation level for this expression
     @param out
     @param config
     */

    public void display(int level, PrintStream out, Configuration config) {
        out.println(ExpressionTool.indent(level) + "analyze-string");
        out.println(ExpressionTool.indent(level) + "select = ");
        select.display(level + 1, out, config);
        out.println(ExpressionTool.indent(level) + "regex = ");
        regex.display(level + 1, out, config);
        out.println(ExpressionTool.indent(level) + "flags = ");
        flags.display(level + 1, out, config);
        if (matching != null) {
            out.println(ExpressionTool.indent(level) + "matching = ");
            matching.display(level + 1, out, config);
        }
        if (nonMatching != null) {
            out.println(ExpressionTool.indent(level) + "non-matching = ");
            nonMatching.display(level + 1, out, config);
        }
    }

    /**
     * Mapping function that maps the sequence of matching/non-matching strings to the
     * sequence delivered by applying the matching-substring and non-matching-substring
     * expressions respectively to each such string
     */

    private class AnalyzeMappingFunction implements ContextMappingFunction {

        private RegexIterator base;
        private XPathContext c2;

        public AnalyzeMappingFunction(RegexIterator base, XPathContext c2) {
            this.base = base;
            this.c2 = c2;
        }

        /**
         * Map one item to a sequence.
         *
         * @param context The processing context. Some mapping functions use this because they require
         *                context information. Some mapping functions modify the context by maintaining the context item
         *                and position. In other cases, the context may be null.
         * @return either (a) a SequenceIterator over the sequence of items that the supplied input
         *         item maps to, or (b) an Item if it maps to a single item, or (c) null if it maps to an empty
         *         sequence.
         */

        public SequenceIterator map(XPathContext context) throws XPathException {
            if (base.isMatching()) {
                if (matching != null) {
                    return matching.iterate(c2);
                }
            } else {
                if (nonMatching != null) {
                    return nonMatching.iterate(c2);
                }
            }
            return EmptyIterator.getInstance();
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
