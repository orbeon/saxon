package org.orbeon.saxon.expr;

import org.orbeon.saxon.functions.NumberFn;
import org.orbeon.saxon.functions.StringFn;
import org.orbeon.saxon.functions.SystemFunction;
import org.orbeon.saxon.style.StandardNames;
import org.orbeon.saxon.trans.StaticError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.type.AnyItemType;
import org.orbeon.saxon.type.AtomicType;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.*;
import org.orbeon.saxon.pattern.NoNodeTest;

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
     * @throws org.orbeon.saxon.trans.StaticError if the supplied type is statically inconsistent with the
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
            cardOK = Cardinality.subsumes(reqCard, suppliedCard);   // TODO: is this right? Atomization may change the cardinality later
        }

        boolean itemTypeOK = reqItemType instanceof AnyItemType;
        // Unless the required item type and content type are ITEM (no constraints)
        // check the static item type against the supplied expression.
        // NOTE: we don't currently do any static inference regarding the content type
        if (!itemTypeOK) {
            suppliedItemType = exp.getItemType();
            if (suppliedItemType instanceof NoNodeTest) {
                // supplied type is empty(): this can violate a cardinality constraint but not an item type constraint
                itemTypeOK = true;
            } else {
                int relation = Type.relationship(reqItemType, suppliedItemType);
                itemTypeOK = relation == Type.SAME_TYPE || relation == Type.SUBSUMES;
            }
        }


        // Handle the special rules for 1.0 compatibility mode
        if (backwardsCompatible && !allowsMany) {
            // rule 1
            if (Cardinality.allowsMany(suppliedCard)) {
                ComputedExpression cexp = new FirstItemExpression(exp);
                cexp.adoptChildExpression(exp);
                exp = cexp;
                suppliedCard = StaticProperty.ALLOWS_ZERO_OR_ONE;
                cardOK = Cardinality.subsumes(reqCard, suppliedCard);
            }
            if (!itemTypeOK) {
                // rule 2
                if (reqItemType == Type.STRING_TYPE) {
                    StringFn fn = (StringFn)SystemFunction.makeSystemFunction("string", 1, env.getNamePool());
                    Expression[] args = {exp};
                    fn.setArguments(args);
                    try {
                        exp = fn.simplify(env).analyze(env, AnyItemType.getInstance());
                    } catch (XPathException err) {
                        throw err.makeStatic();
                    }
                    suppliedItemType = Type.STRING_TYPE;
                    suppliedCard = StaticProperty.EXACTLY_ONE;
                    cardOK = Cardinality.subsumes(reqCard, suppliedCard);
                    itemTypeOK = true;
                }
                // rule 3
                if (reqItemType == Type.NUMBER_TYPE || reqItemType == Type.DOUBLE_TYPE) {
                    NumberFn fn = (NumberFn)SystemFunction.makeSystemFunction("number", 1, env.getNamePool());
                    Expression[] args = {exp};
                    fn.setArguments(args);
                    try {
                        exp = fn.simplify(env).analyze(env, AnyItemType.getInstance());
                    } catch (XPathException err) {
                        throw err.makeStatic();
                    }
                    suppliedItemType = Type.DOUBLE_TYPE;
                    suppliedCard = StaticProperty.EXACTLY_ONE;
                    cardOK = Cardinality.subsumes(reqCard, suppliedCard);
                    itemTypeOK = true;
                }
            }
        }

        if (!itemTypeOK) {
            // Now apply the conversions needed in 2.0 mode

            if (reqItemType instanceof AtomicType) {

                // rule 1: Atomize
                if (!(suppliedItemType instanceof AtomicType) &&
                        !(suppliedCard == StaticProperty.EMPTY)) {
                    ComputedExpression cexp = new Atomizer(exp, env.getConfiguration());
                    exp = cexp;
                    suppliedItemType = exp.getItemType();
                    suppliedCard = exp.getCardinality();
                    cardOK = Cardinality.subsumes(reqCard, suppliedCard);
                }

                // rule 2: convert untypedAtomic to the required type

                //   2a: all supplied values are untyped atomic. Convert if necessary, and we're finished.

                if ((suppliedItemType == Type.UNTYPED_ATOMIC_TYPE)
                        && !(reqItemType == Type.UNTYPED_ATOMIC_TYPE || reqItemType == Type.ANY_ATOMIC_TYPE)) {

                    ComputedExpression cexp = new UntypedAtomicConverter(exp, (AtomicType)reqItemType, role);
                    try {
                        if (exp instanceof Value) {
                            exp = new SequenceExtent(cexp.iterate(null)).simplify();
                        } else {
                            exp = cexp;
                        }
                    } catch (XPathException err) {
                        throw err.makeStatic();
                    }
                    itemTypeOK = true;
                    suppliedItemType = reqItemType;
                }

                //   2b: some supplied values are untyped atomic. Convert these to the required type; but
                //   there may be other values in the sequence that won't convert and still need to be checked

                if ((suppliedItemType == Type.ANY_ATOMIC_TYPE)
                    && !(reqItemType == Type.UNTYPED_ATOMIC_TYPE || reqItemType == Type.ANY_ATOMIC_TYPE)) {

                    ComputedExpression cexp = new UntypedAtomicConverter(exp, (AtomicType)reqItemType, role);
                    try {
                        if (exp instanceof Value) {
                            exp = new SequenceExtent(cexp.iterate(null)).simplify();
                        } else {
                            exp = cexp;
                        }
                    } catch (XPathException err) {
                        throw err.makeStatic();
                    }
                }

                // Rule 3a: numeric promotion decimal -> float -> double

                int rt = ((AtomicType)reqItemType).getFingerprint();
                if (rt == StandardNames.XS_DOUBLE || rt == StandardNames.XS_FLOAT) {
                    if (Type.relationship(suppliedItemType, Type.NUMBER_TYPE) != Type.DISJOINT) {
                        exp = new NumericPromoter(exp, rt);
                        try {
                            exp = exp.simplify(env).analyze(env, AnyItemType.getInstance());
                        } catch (XPathException err) {
                            throw err.makeStatic();
                        }
                        suppliedItemType = (rt == StandardNames.XS_DOUBLE ? Type.DOUBLE_TYPE : Type.FLOAT_TYPE);
                    }
                }

                // Rule 3b: promotion from anyURI -> string

                if (rt == Type.STRING &&
                        Type.isSubType(suppliedItemType, Type.ANY_URI_TYPE)) {
                    suppliedItemType = Type.STRING_TYPE;
                    itemTypeOK = true;
                        // we don't generate code to do a run-time type conversion; rather, we rely on
                        // operators and functions that accept a string to also accept an xs:anyURI. This
                        // is straightforward, because anyURIValue is a subclass of StringValue
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

        // If the supplied value is () and () isn't allowed, fail now
        if (suppliedCard==StaticProperty.EMPTY && ((reqCard & StaticProperty.ALLOWS_ZERO) == 0) ) {
            StaticError err = new StaticError(
                        "An empty sequence is not allowed as the " + role.getMessage(),
                        ExpressionTool.getLocator(supplied));
            err.setErrorCode(role.getErrorCode());
            err.setIsTypeError(true);
            throw err;
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
                    env.issueWarning(msg, ExpressionTool.getLocator(supplied));
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

        // Unless the type is guaranteed to match, add a dynamic type check,
        // unless the value is already known in which case we might as well report
        // the error now.

        if (!(relation == Type.SAME_TYPE || relation == Type.SUBSUMED_BY)) {
                ComputedExpression cexp = new ItemChecker(exp, reqItemType, role);
                cexp.adoptChildExpression(exp);
                exp = cexp;
        }

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

     /**
     * Check an expression against a required type, modifying it if necessary. This
     * is a variant of the method {@link #staticTypeCheck} used for expressions that
     * declare variables in XQuery. In these contexts, conversions such as numeric
     * type promotion and atomization are not allowed.
     *
     * @param supplied      The expression to be type-checked
     * @param req           The required type for the context in which the expression is used
     * @param role          Information about the role of the subexpression within the
     *                      containing expression, used to provide useful error messages
     * @param env           The static context containing the types being checked. At present
     *                      this is used only to locate a NamePool
     * @return              The original expression if it is type-safe, or the expression
     *                      wrapped in a run-time type checking expression if not.
     * @throws org.orbeon.saxon.trans.StaticError if the supplied type is statically inconsistent with the
     *                      required type (that is, if they have no common subtype)
     */

    public static Expression strictTypeCheck(Expression supplied,
                                             SequenceType req,
                                             RoleLocator role,
                                             StaticContext env)
    throws StaticError {

        // System.err.println("Strict Type Check on expression (requiredType = " + req + "):"); supplied.display(10);

        Expression exp = supplied;

        ItemType reqItemType = req.getPrimaryType();
        int reqCard = req.getCardinality();

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
        }

        // If both the cardinality and item type are statically OK, return now.
        if (itemTypeOK && cardOK) {
            return exp;
        }

        // If we haven't evaluated the cardinality of the supplied expression, do it now
        if (suppliedCard == -1) {
            if (suppliedItemType instanceof NoNodeTest) {
                suppliedCard = StaticProperty.EMPTY;
            } else {
                suppliedCard = exp.getCardinality();
            }
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
                    env.issueWarning(msg, ExpressionTool.getLocator(supplied));
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

        // Unless the type is guaranteed to match, add a dynamic type check,
        // unless the value is already known in which case we might as well report
        // the error now.

        if (!(relation == Type.SAME_TYPE || relation == Type.SUBSUMED_BY)) {
                ComputedExpression cexp = new ItemChecker(exp, reqItemType, role);
                cexp.adoptChildExpression(exp);
                exp = cexp;
        }

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

    /**
     * Test whether a given value conforms to a given type
     * @param val the value
     * @param requiredType the required type
     * @return a DynamicError describing the error condition if the value doesn't conform;
     * or null if it does.
     */

    public static DynamicError testConformance(Value val, SequenceType requiredType) {
        ItemType reqItemType = requiredType.getPrimaryType();
        if (!Type.isSubType(val.getItemType(), reqItemType)) {
            DynamicError err = new DynamicError (
                    "Global parameter requires type " + reqItemType +
                    "; supplied value has type " + val.getItemType());
            err.setIsTypeError(true);
            return err;
        }
        int reqCardinality = requiredType.getCardinality();
        if (!Cardinality.subsumes(reqCardinality, val.getCardinality())) {
            DynamicError err = new DynamicError (
                    "Supplied value of external parameter does not match the required cardinality");
            err.setIsTypeError(true);
            return err;
        }
        return null;
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
// Contributor(s): none.
//
