package net.sf.saxon.expr;

import net.sf.saxon.functions.NumberFn;
import net.sf.saxon.functions.SystemFunction;
import net.sf.saxon.pattern.AnyNodeTest;
import net.sf.saxon.type.AnyItemType;
import net.sf.saxon.type.AtomicType;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;
import net.sf.saxon.value.Cardinality;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.Value;
import net.sf.saxon.xpath.StaticError;
import net.sf.saxon.xpath.XPathException;

/**
 * This class provides Saxon's type checking capability. It contains a static method,
 * staticTypeCheck, which is called at compile time to perform type checking of
 * an expression. This class is never instantiated.
 */

public final class TypeChecker {

    // Class is not instantiated
    private TypeChecker() {}

    /**
     * Check an expression against a required type, modifying it if necessary.
     *
     * <p>This method takes the supplied expression and checks to see whether it is
     * known statically to conform to the specified type. There are three possible
     * outcomes. If the static type of the expression is a subtype of the required
     * type, the method returns the expression unchanged. If the static type of
     * the expression is incompatible with the required type (for example, if the
     * supplied type is integer and the required type is string) the method throws
     * an exception (this results in a compile-time type error being reported). If
     * the static type is a supertype of the required type, then a new expression
     * is constructed that evaluates the original expression and checks the dynamic
     * type of the result; this new expression is returned as the result of the
     * method.</p>
     *
     * <p>The rules applied are those for function calling in XPath, that is, the rules
     * that the argument of a function call must obey in relation to the signature of
     * the function. Some contexts require slightly different rules (for example,
     * operands of polymorphic operators such as "+"). In such cases this method cannot
     * be used.</p>
     *
     * <p>Note that this method does <b>not</b> do recursive type-checking of the
     * sub-expressions.</p>
     *
     * @param supplied      The expression to be type-checked
     * @param req           The required type for the context in which the expression is used
     * @param backwardsCompatible
     *                      True if XPath 1.0 backwards compatibility mode is applicable
     * @param role          Information about the role of the subexpression within the
     *                      containing expression, used to provide useful error messages
     * @param env           The static context containing the types being checked. At present
     *                      this is used only to locate a NamePool
     * @return              The original expression if it is type-safe, or the expression
     *                      wrapped in a run-time type checking expression if not.
     * @throws net.sf.saxon.xpath.StaticError if the supplied type is statically inconsistent with the
     *                      required type (that is, if they have no common subtype)
     */

    public static Expression staticTypeCheck(Expression supplied,
                                             SequenceType req,
                                             boolean backwardsCompatible,
                                             RoleLocator role,
                                             StaticContext env)
    throws StaticError {

        // System.err.println("Static Type Check on expression (requiredType = " + req + "):"); supplied.display(10);

        Expression exp = supplied;

        ItemType reqItemType = req.getPrimaryType();
        int reqCard = req.getCardinality();
        boolean allowsMany = Cardinality.allowsMany(reqCard);

        ItemType suppliedItemType = null;
            // item type of the supplied expression: null means not yet calculated
        int suppliedCard = -1;
            // cardinality of the supplied expression: -1 means not yet calculated

        boolean cardOK = (reqCard == StaticProperty.ALLOWS_ZERO_OR_MORE);
        // Unless the required cardinality is zero-or-more (no constraints).
        // check the static cardinality of the supplied expression
        if (!cardOK) {
            suppliedCard = exp.getCardinality();
            cardOK = Cardinality.subsumes(reqCard, suppliedCard);
        }

        boolean itemTypeOK = req.getPrimaryType() instanceof AnyItemType;
        // Unless the required item type and content type are ITEM (no constraints)
        // check the static item type against the supplied expression.
        // NOTE: we don't currently do any static inference regarding the content type
        if (!itemTypeOK) {
            suppliedItemType = exp.getItemType();
            int relation = Type.relationship(reqItemType, suppliedItemType);
            itemTypeOK = relation == Type.SAME_TYPE || relation == Type.SUBSUMES;
            if (!itemTypeOK) {
                // Handle numeric promotion decimal -> float -> double
                if (Type.isPromotable(suppliedItemType, reqItemType)) {
                    itemTypeOK = true;
                    ComputedExpression cexp;
                    if (allowsMany) {
                        cexp = new AtomicSequenceConverter(exp, (AtomicType)reqItemType);
                    } else {
                        cexp = new CastExpression(exp, (AtomicType)reqItemType, true);
                    }
                    if (exp instanceof Value) {
                        try {
                            exp = ExpressionTool.eagerEvaluate(cexp, null);
                        } catch (XPathException err) {
                            throw new StaticError(err);
                        }
                    } else {
                        cexp.adoptChildExpression(exp);
                        exp = cexp;
                    }
                    suppliedItemType = reqItemType;
                }
                // Handle conversion of untypedAtomic to the required type
                if (suppliedItemType.getPrimitiveType() == Type.UNTYPED_ATOMIC) {
                    itemTypeOK = true;
                    ComputedExpression cexp;
                    if (allowsMany) {
                        cexp = new AtomicSequenceConverter(exp, (AtomicType)reqItemType);
                    } else {
                        cexp = new CastExpression(exp, (AtomicType)reqItemType, true);
                    }
                    if (exp instanceof Value) {
                        try {
                            exp = ExpressionTool.eagerEvaluate(cexp, null);
                        } catch (XPathException err) {
                            throw new StaticError(err);
                        }
                    } else {
                        cexp.adoptChildExpression(exp);
                        exp = cexp;
                    }
                    suppliedItemType = reqItemType;
                }
            }
        }

        // If both the cardinality and item type are statically OK, return now.
        if (itemTypeOK && cardOK) {
            return exp;
        }

        // If we haven't evaluated the cardinality of the supplied expression, do it now
        if (suppliedCard == -1) {
            suppliedCard = exp.getCardinality();
            if (!cardOK) {
                cardOK = Cardinality.subsumes(reqCard, suppliedCard);
            }
        }

        // If an empty sequence was explicitly supplied, and empty sequence is allowed,
        // then the item type doesn't matter
        if (cardOK && suppliedCard==StaticProperty.EMPTY) {
            return exp;
        }

        // If we haven't evaluated the item type of the supplied expression, do it now
        if (suppliedItemType == null) {
            suppliedItemType = exp.getItemType();
        }

        if (suppliedCard==StaticProperty.EMPTY && ((reqCard & StaticProperty.ALLOWS_ZERO) == 0) ) {
            StaticError err = new StaticError(
                        "An empty sequence is not allowed as the " + role.getMessage(),
                        ExpressionTool.getLocator(supplied));
            err.setErrorCode(role.getErrorCode());
            err.setIsTypeError(true);
            throw err;
        }

        // Handle the special rules for 1.0 compatibility mode
        if (backwardsCompatible && !allowsMany) {
            if (Type.isSubType(reqItemType, Type.STRING_TYPE)) {
                if (!Type.isSubType(suppliedItemType, Type.ANY_ATOMIC_TYPE)) {
                    ComputedExpression cexp = new Atomizer(exp);
                    cexp.adoptChildExpression(exp);
                    exp = cexp;
                }
                ComputedExpression cexp2 = new ConvertToString(exp);
                if (exp instanceof Value) {
                    try {
                        exp = ExpressionTool.eagerEvaluate(cexp2, null);
                    } catch (XPathException err) {
                        throw new StaticError(err);
                    }
                } else {
                    cexp2.adoptChildExpression(exp);
                    exp = cexp2;
                }
                suppliedItemType = Type.STRING_TYPE;
                suppliedCard = StaticProperty.EXACTLY_ONE;
                cardOK = Cardinality.subsumes(reqCard, suppliedCard);
            } else if (reqItemType == Type.NUMBER_TYPE || Type.isSubType(reqItemType, Type.DOUBLE_TYPE)) {
                // TODO: in the Nov 2003 draft, the rules have changed so that number() is called
                // only if the supplied value doesn't match the expected type. We're currently
                // returning different results for round(()) depending on whether the arg value
                // is known statically or not.
                NumberFn fn = (NumberFn)SystemFunction.makeSystemFunction("number", env.getNamePool());
                Expression[] args = new Expression[1];
                args[0] = exp;
                fn.setArguments(args);
                if (exp instanceof Value) {
                    try {
                        exp = ExpressionTool.eagerEvaluate(fn, null);
                    } catch (XPathException err) {
                        throw new StaticError(err);
                    }
                } else {
                    fn.adoptChildExpression(exp);
                    exp = fn;
                }
                suppliedItemType = Type.DOUBLE_TYPE;
                suppliedCard = StaticProperty.EXACTLY_ONE;
                cardOK = Cardinality.subsumes(reqCard, suppliedCard);
            } else if (reqItemType instanceof AnyNodeTest ||
                    reqItemType instanceof AnyItemType
                        || reqItemType == Type.ANY_ATOMIC_TYPE ) {
                                // TODO: this last condition isn't in the rules for function calls,
                                // but is needed for arithmetic expressions
                if (Cardinality.allowsMany(suppliedCard)) {
                    ComputedExpression cexp = new FirstItemExpression(exp);
                    cexp.adoptChildExpression(exp);
                    exp = cexp;
                }
                suppliedCard = StaticProperty.ALLOWS_ZERO_OR_ONE;
                cardOK = Cardinality.subsumes(reqCard, suppliedCard);
            }
        }

        // If the required type is atomic, and the supplied type is not statically
        // guaranteed to be atomic, add an Atomizer

        if ((reqItemType instanceof AtomicType) &&
                !(suppliedItemType instanceof AtomicType) &&
                !(suppliedCard == StaticProperty.EMPTY)) {
            ComputedExpression cexp = new Atomizer(exp);
            exp = cexp;
            suppliedItemType = exp.getItemType();
            suppliedCard = exp.getCardinality();
            cardOK = Cardinality.subsumes(reqCard, suppliedCard);
        }

        // If the required type is a subtype of ATOMIC, and the supplied type is
        // capable of producing untyped atomic values, add an Untyped Atomic Converter

        if (reqItemType != Type.ANY_ATOMIC_TYPE &&
                (reqItemType instanceof AtomicType) &&
                (suppliedItemType instanceof AtomicType) &&
                (suppliedItemType == Type.ANY_ATOMIC_TYPE ||
                    suppliedItemType == Type.UNTYPED_ATOMIC_TYPE) &&
                !(suppliedCard == StaticProperty.EMPTY)) {
            ComputedExpression cexp = new UntypedAtomicConverter(exp, (AtomicType)reqItemType);
            cexp.adoptChildExpression(exp);
            exp = cexp;
            suppliedItemType = exp.getItemType();
            suppliedCard = exp.getCardinality();
            cardOK = Cardinality.subsumes(reqCard, suppliedCard);
        }

        // Try a static type check. We only throw it out if the call cannot possibly succeed.

        int relation = Type.relationship(suppliedItemType, reqItemType);
        if (relation == Type.DISJOINT) {
            // The item types may be disjoint, but if both the supplied and required types permit
            // an empty sequence, we can't raise a static error. Raise a warning instead.
            if (Cardinality.allowsZero(suppliedCard) &&
                    Cardinality.allowsZero(reqCard)) {
                if (suppliedCard != StaticProperty.EMPTY) {
                    String msg = "Required item type of " + role.getMessage() +
                            " is " + reqItemType.toString(env.getNamePool()) +
                            "; supplied value has item type " +
                            suppliedItemType.toString(env.getNamePool()) +
                            ". The expression can succeed only if the supplied value is an empty sequence.";
                    env.issueWarning(msg);
                }
            } else {
                    StaticError err = new StaticError(
                        "Required item type of " + role.getMessage() +
                            " is " + reqItemType.toString(env.getNamePool()) +
                            "; supplied value has item type " +
                            suppliedItemType.toString(env.getNamePool()),
                        ExpressionTool.getLocator(supplied));
                    err.setErrorCode(role.getErrorCode());
                    err.setIsTypeError(true);
                    throw err;
            }
        }
        //    }
        //}

        // Unless the type is guaranteed to match, add a dynamic type check,
        // unless the value is already known in which case we might as well report
        // the error now.

        //if (!itemTypeOK) {
//            if (exp instanceof Value) {
//                 StaticError err = new StaticError(
//                    "Required type of " + role.getMessage() +
//                        " is " + reqItemType.toString(env.getNamePool()) +
//                        "; supplied value has type " +
//                        suppliedItemType.toString(env.getNamePool()),
//                    ExpressionTool.getLocator(supplied));
//                err.setIsTypeError(true);
//                throw err;
//            } else {
        if (!(relation == Type.SAME_TYPE || relation == Type.SUBSUMED_BY)) {
                ComputedExpression cexp = new ItemChecker(exp, reqItemType, role);
                cexp.adoptChildExpression(exp);
                exp = cexp;
        }
//            }
//        }

        if (!cardOK) {
            if (exp instanceof Value) {
                 StaticError err = new StaticError (
                    "Required cardinality of " + role.getMessage() +
                        " is " + Cardinality.toString(reqCard) +
                        "; supplied value has cardinality " +
                        Cardinality.toString(suppliedCard),
                    ExpressionTool.getLocator(supplied));
                err.setIsTypeError(true);
                err.setErrorCode(role.getErrorCode());
                throw err;
            } else {
                ComputedExpression cexp = new CardinalityChecker(exp, reqCard, role);
                cexp.adoptChildExpression(exp);
                exp = cexp;
            }
        }

        return exp;
    }

    // TODO: when we check a sequence (string, int) against a required type of int, we are not
    // generating a static error, because we treat the supplied item type as anyAtomicType which
    // in other circumstances might be OK. Fixing this can only be done by improving the granularity
    // of the information that expressions return about their static type. Alternatively we could
    // special-case it: for an AppendExpression or Block or conditional expression, we could check
    // each subexpression against the required type.
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
// Contributor(s): none.
//
