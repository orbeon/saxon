package org.orbeon.saxon.instruct;

import org.orbeon.saxon.Loader;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.functions.NumberFn;
import org.orbeon.saxon.number.NumberFormatter;
import org.orbeon.saxon.number.Numberer;
import org.orbeon.saxon.number.Numberer_en;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.pattern.Pattern;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.AtomicValue;
import org.orbeon.saxon.value.IntegerValue;
import org.orbeon.saxon.value.NumericValue;
import org.orbeon.saxon.value.StringValue;
import org.orbeon.saxon.xpath.XPathException;
import org.orbeon.saxon.xpath.DynamicError;

import java.util.*;
import java.io.PrintStream;

/**
 * An xsl:number element in the stylesheet. Although this is an XSLT instruction, it is compiled
 * into an expression, evaluated using xsl:value-of to create the resulting text node.<br>
 */

public class NumberInstruction extends ComputedExpression {

    private static final int SINGLE = 0;
    private static final int MULTI = 1;
    private static final int ANY = 2;
    private static final int SIMPLE = 3;

    private int level;
    private Pattern count = null;
    private Pattern from = null;
    private Expression select = null;
    private Expression value = null;
    private Expression format = null;
    private Expression groupSize = null;
    private Expression groupSeparator = null;
    private Expression letterValue = null;
    private Expression ordinal = null;
    private Expression lang = null;
    private NumberFormatter formatter = null;
    private Numberer numberer = null;
    private HashMap nationalNumberers = null;
    private boolean hasVariablesInPatterns;

    private static Numberer defaultNumberer = new Numberer_en();

    public NumberInstruction(Expression select,
                             int level,
                             Pattern count,
                             Pattern from,
                             Expression value,
                             Expression format,
                             Expression groupSize,
                             Expression groupSeparator,
                             Expression letterValue,
                             Expression ordinal,
                             Expression lang,
                             NumberFormatter formatter,
                             Numberer numberer,
                             boolean hasVariablesInPatterns) {
        this.select = select;
        this.level = level;
        this.count = count;
        this.from = from;
        this.value = value;
        this.format = format;
        this.groupSize = groupSize;
        this.groupSeparator = groupSeparator;
        this.letterValue = letterValue;
        this.ordinal = ordinal;
        this.lang = lang;
        this.formatter = formatter;
        this.numberer = numberer;
        this.hasVariablesInPatterns = hasVariablesInPatterns;

        if (this.value != null && !Type.isSubType(this.value.getItemType(), Type.ANY_ATOMIC_TYPE)) {
            this.value = new Atomizer(this.value);
        }

        if (value != null) {
            adoptChildExpression(value);
        }
        if (format != null) {
            adoptChildExpression(format);
        }
        if (groupSize != null) {
            adoptChildExpression(groupSize);
        }
        if (groupSeparator != null) {
            adoptChildExpression(groupSeparator);
        }
        if (letterValue != null) {
            adoptChildExpression(letterValue);
        }
        if (ordinal != null) {
            adoptChildExpression(ordinal);
        }
        if (lang != null) {
            adoptChildExpression(lang);
        }
        // TODO: treat the patterns as regular subexpressions
    }

    public Expression simplify(StaticContext env) throws XPathException {
        if (select != null) {
            select = select.simplify(env);
        }
        if (value != null) {
            value = value.simplify(env);
        }
        if (format != null) {
            format = format.simplify(env);
        }
        if (groupSize != null) {
            groupSize = groupSize.simplify(env);
        }
        if (groupSeparator != null) {
            groupSeparator = groupSeparator.simplify(env);
        }
        if (letterValue != null) {
            letterValue = letterValue.simplify(env);
        }
        if (ordinal != null) {
            ordinal = ordinal.simplify(env);
        }
        if (lang != null) {
            lang = lang.simplify(env);
        }
        return this;
    }

    /**
     * Perform static analysis of an expression and its subexpressions.
     *
     * <p>This checks statically that the operands of the expression have
     * the correct type; if necessary it generates code to do run-time type checking or type
     * conversion. A static type error is reported only if execution cannot possibly succeed, that
     * is, if a run-time type error is inevitable. The call may return a modified form of the expression.</p>
     *
     * <p>This method is called after all references to functions and variables have been resolved
     * to the declaration of the function or variable. However, the types of such functions and
     * variables will only be accurately known if they have been explicitly declared.</p>
     *
     * @param env the static context of the expression
     * @exception org.orbeon.saxon.xpath.StaticError if an error is discovered during this phase
     *     (typically a type error)
     * @return the original expression, rewritten to perform necessary
     *     run-time type checks, and to perform other type-related
     *     optimizations
     */

    public Expression analyze(StaticContext env, ItemType contextItemType) throws XPathException {
        if (select != null) {
            select = select.analyze(env, contextItemType);
        }
        if (value != null) {
            value = value.analyze(env, contextItemType);
        }
        if (format != null) {
            format = format.analyze(env, contextItemType);
        }
        if (groupSize != null) {
            groupSize = groupSize.analyze(env, contextItemType);
        }
        if (groupSeparator != null) {
            groupSeparator = groupSeparator.analyze(env, contextItemType);
        }
        if (letterValue != null) {
            letterValue = letterValue.analyze(env, contextItemType);
        }
        if (ordinal != null) {
            ordinal = ordinal.analyze(env, contextItemType);
        }
        if (lang != null) {
            lang = lang.analyze(env, contextItemType);
        }
        return this;
    }

   /**
     * Get the immediate sub-expressions of this expression. Default implementation
     * returns a zero-length array, appropriate for an expression that has no
     * sub-expressions.
     * @return an iterator containing the sub-expressions of this expression
     */

    public Iterator iterateSubExpressions() {
        List sub = new ArrayList(8);
        if (value != null) {
            sub.add(value);
        }
        if (format != null) {
            sub.add(format);
        }
        if (groupSize != null) {
            sub.add(groupSize);
        }
        if (groupSeparator != null) {
            sub.add(groupSeparator);
        }
        if (letterValue != null) {
            sub.add(letterValue);
        }
        if (ordinal != null) {
            sub.add(ordinal);
        }
        if (lang != null) {
            sub.add(lang);
        }
       return sub.iterator();
    }

    public ItemType getItemType() {
        return Type.STRING_TYPE;
    }

    public int computeCardinality() {
        return StaticProperty.EXACTLY_ONE;
    }

    public Item evaluateItem(XPathContext context) throws XPathException {
        long value = -1;
        List vec = null;

        if (this.value != null) {

            SequenceIterator iter = this.value.iterate(context);
            vec = new ArrayList(4);
            while (true) {
                AtomicValue val = (AtomicValue) iter.next();
                if (val == null) {
                    break;
                }
                try {
                    NumericValue num;
                    if (val instanceof NumericValue) {
                        num = (NumericValue) val;
                    } else {
                        num = NumberFn.convert(val);
                    }
                    num = num.round();
                    if (num.compareTo(IntegerValue.MAX_LONG) > 0) {
                        DynamicError e = new DynamicError("A number is too large to be formatted");
                        e.setXPathContext(context);
                        e.setErrorCode("SAXON:0000");
                        throw e;
                    }
                    if (num.compareTo(IntegerValue.MIN_LONG) < 0) {
                        DynamicError e = new DynamicError("The numbers to be formatted must not be negative");
                        e.setXPathContext(context);
                        e.setErrorCode("XT0980");
                        throw e;
                    }
                    long i = ((NumericValue) num.convert(Type.INTEGER, context)).longValue();
                    if (i < 0) {
                        DynamicError e = new DynamicError("The numbers to be formatted must not be negative");
                        e.setXPathContext(context);
                        e.setErrorCode("XT0980");
                        throw e;
                    }
                    vec.add(new Long(i));
                } catch (DynamicError err) {
                    vec.add(val.getStringValue());
                    recoverableError(err, context);
                }
            }

        } else {
            NodeInfo source;
            if (select != null) {
                source = (NodeInfo) select.evaluateItem(context);
            } else {
                Item item = context.getContextItem();
                if (!(item instanceof NodeInfo)) {
                    DynamicError err = new DynamicError("context item for xsl:number must be a node");
                    err.setErrorCode("XT0990");
                    err.setXPathContext(context);
                    recoverableError(err, context);
                    return null;     // error recovery action is to output nothing
                }
                source = (NodeInfo) item;
            }

            if (level == SIMPLE) {
                value = Navigator.getNumberSimple(source, context);
            } else if (level == SINGLE) {
                value = Navigator.getNumberSingle(source, count, from, context);
                if (value == 0) {
                    vec = Collections.EMPTY_LIST; 	// an empty list
                }
            } else if (level == ANY) {
                value = Navigator.getNumberAny(this, source, count, from, context, hasVariablesInPatterns);
                if (value == 0) {
                    vec = Collections.EMPTY_LIST; 	// an empty list
                }
            } else if (level == MULTI) {
                vec = Navigator.getNumberMulti(source, count, from, context);
            }
        }

        int gpsize = 0;
        String gpseparator = "";
        String letterVal;
        String ordinalVal = null;

        if (groupSize != null) {
            String g = groupSize.evaluateAsString(context);
            try {
                gpsize = Integer.parseInt(g);
            } catch (NumberFormatException err) {
                DynamicError e = new DynamicError("grouping-size must be numeric");
                e.setXPathContext(context);
                e.setErrorCode("XT0030");
                throw e;
            }
        }

        if (groupSeparator != null) {
            gpseparator = groupSeparator.evaluateAsString(context);
        }

        if (ordinal != null) {
            ordinalVal = ordinal.evaluateAsString(context);
        }

        // fast path for the simple case

        if (vec == null && format == null && gpsize == 0 && lang == null) {
            return new StringValue("" + value);
        }

        // Use the numberer decided at compile time if possible; otherwise try to get it from
        // a table of numberers indexed by language; if not there, load the relevant class and
        // add it to the table.
        Numberer numb = numberer;
        if (numb == null) {
            String language = lang.evaluateAsString(context);
            if (nationalNumberers == null) {
                nationalNumberers = new HashMap(4);
            }
            numb = (Numberer)nationalNumberers.get(language);
            if (numb == null) {
                numb = makeNumberer(language);
                nationalNumberers.put(language, numb);
            }
        }

        if (letterValue == null) {
            letterVal = "";
        } else {
            letterVal = letterValue.evaluateAsString(context);
            if (!("alphabetic".equals(letterVal) || "traditional".equals(letterVal))) {
                DynamicError e = new DynamicError("letter-value must be \"traditional\" or \"alphabetic\"");
                e.setXPathContext(context);
                e.setErrorCode("XT0030");
                throw e;
            }
        }

        if (vec == null) {
            vec = new ArrayList(1);
            vec.add(new Long(value));
        }

        NumberFormatter nf;
        if (formatter == null) {              // format not known until run-time
            nf = new NumberFormatter();
            nf.prepare(format.evaluateAsString(context));
        } else {
            nf = formatter;
        }

        String s = nf.format(vec, gpsize, gpseparator, letterVal, ordinalVal, numb);
        return new StringValue(s);
    }

    private void recoverableError(DynamicError error, XPathContext context) throws XPathException {
        error.setLocator(ExpressionTool.getLocator(this));
        context.getController().recoverableError(error);
    }

    /**
     * Load a Numberer class for a given language and check it is OK.
     * @param language the language for which a Numberer is required
     * @return a suitable numberer. If no specific numberer is available
     * for the language, the default (English) numberer is used.
     */

    public static Numberer makeNumberer(String language) {
        Numberer numberer;
        if ("en".equals(language)) {
            numberer = defaultNumberer;
        } else {
            String langClassName = "org.orbeon.saxon.number.Numberer_";
            for (int i = 0; i < language.length(); i++) {
                if (Character.isLetter(language.charAt(i))) {
                    langClassName += language.charAt(i);
                }
            }
            try {
                numberer = (Numberer) (Loader.getInstance(langClassName));
            } catch (XPathException err) {
                numberer = defaultNumberer;
            }
        }

        return numberer;
    }

    /**
     * Diagnostic print of expression structure. The expression is written to the System.err
     * output stream
     *
     * @param level indentation level for this expression
     * @param out
     */

    public void display(int level, NamePool pool, PrintStream out) {
        out.println(ExpressionTool.indent(level) + "xsl:number");
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
