package org.orbeon.saxon.expr;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.style.StandardNames;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.NameChecker;
import org.orbeon.saxon.om.QNameException;
import org.orbeon.saxon.sort.IntHashMap;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.StaticError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.*;
import org.orbeon.saxon.value.*;

/**
* Cast Expression: implements "cast as data-type ( expression )". It also allows an internal
* cast, which has the same semantics as a user-requested cast, but maps an empty sequence to
* an empty sequence.
*/

public final class CastExpression extends UnaryExpression  {

    static IntHashMap castingTable = new IntHashMap(25);

    static void addAllowedCasts(int source, int[] target) {
        castingTable.put(source, target);
    }

    /**
     * The following data represents all the "Y" and "M" entries in section 17.1 of the F+O spec.
     */

    static {
        final int uat = Type.UNTYPED_ATOMIC;
        final int str = Type.STRING;
        final int flt = Type.FLOAT;
        final int dbl = Type.DOUBLE;
        final int dec = Type.DECIMAL;
        final int ing = Type.INTEGER;
        final int dur = Type.DURATION;
        final int ymd = Type.YEAR_MONTH_DURATION;
        final int dtd = Type.DAY_TIME_DURATION;
        final int dtm = Type.DATE_TIME;
        final int tim = Type.TIME;
        final int dat = Type.DATE;
        final int gym = Type.G_YEAR_MONTH;
        final int gyr = Type.G_YEAR;
        final int gmd = Type.G_MONTH_DAY;
        final int gdy = Type.G_DAY;
        final int gmo = Type.G_MONTH;
        final int boo = Type.BOOLEAN;
        final int b64 = Type.BASE64_BINARY;
        final int hxb = Type.HEX_BINARY;
        final int uri = Type.ANY_URI;
        final int qnm = Type.QNAME;
        final int not = Type.NOTATION;

        final int[] t01 = {uat, str, flt, dbl, dec, ing, dur, ymd, dtd, dtm, tim, dat,
                          gym, gyr, gmd, gdy, gmo, boo, b64, hxb, uri};
        addAllowedCasts(uat, t01);
        final int[] t02 = {uat, str, flt, dbl, dec, ing, dur, ymd, dtd, dtm, tim, dat,
                          gym, gyr, gmd, gdy, gmo, boo, b64, hxb, uri, qnm, not};
        addAllowedCasts(str, t02);
        final int[] t03 = {uat, str, flt, dbl, dec, ing, boo};
        addAllowedCasts(flt, t03);
        addAllowedCasts(dbl, t03);
        addAllowedCasts(dec, t03);
        addAllowedCasts(ing, t03);
        final int[] t04 = {uat, str, dur, ymd, dtd};
        addAllowedCasts(dur, t04);
        addAllowedCasts(ymd, t04);
        addAllowedCasts(dtd, t04);
        final int[] t05 = {uat, str, dtm, tim, dat, gym, gyr, gmd, gdy, gmo};
        addAllowedCasts(dtm, t05);
        final int[] t06 = {uat, str, tim};
        addAllowedCasts(tim, t06);
        final int[] t07 = {uat, str, dtm, dat, gym, gyr, gmd, gdy, gmo};
        addAllowedCasts(dat, t07);
        final int[] t08 = {uat, str, gym};
        addAllowedCasts(gym, t08);
        final int[] t09 = {uat, str, gyr};
        addAllowedCasts(gyr, t09);
        final int[] t10 = {uat, str, gmd};
        addAllowedCasts(gmd, t10);
        final int[] t11 = {uat, str, gdy};
        addAllowedCasts(gdy, t11);
        final int[] t12 = {uat, str, gmo};
        addAllowedCasts(gmo, t12);
        final int[] t13 = {uat, str, flt, dbl, dec, ing, boo};
        addAllowedCasts(boo, t13);
        final int[] t14 = {uat, str, b64, hxb};
        addAllowedCasts(b64, t14);
        addAllowedCasts(hxb, t14);
        final int[] t15 = {uat, str, uri};
        addAllowedCasts(uri, t15);
        final int[] t16 = {uat, str, qnm};
        addAllowedCasts(qnm, t16);
        final int[] t17 = {uat, str, not};
        addAllowedCasts(not, t17);
    }

    /**
     * Determine whether casting from a source type to a target type is possible
     * @param source a primitive type (one that has an entry in the casting table)
     * @param target another primitive type
     * @return true if the entry in the casting table is either "Y" (casting always succeeds)
     * or "M" (casting allowed but may fail for some values)
     */

    public static boolean isPossibleCast(int source, int target) {
        if (source == Type.ANY_ATOMIC || source == Type.EMPTY) {
            return true;
        }
        if (source == Type.NUMBER) {
            source = Type.DOUBLE;
        }
        int[] targets = (int[])castingTable.get(source);
        if (targets == null) {
            return false;
        }
        for (int i=0; i<targets.length; i++) {
            if (targets[i] == target) {
                return true;
            }
        }
        return false;
    }


    private AtomicType targetType;
    private AtomicType targetPrimitiveType;
    private boolean allowEmpty = false;
    private boolean derived = false;
    private boolean upcast = false;



    public CastExpression(Expression source, AtomicType target, boolean allowEmpty) {
        super(source);
        this.allowEmpty = allowEmpty;
        targetType = target;
        targetPrimitiveType = (AtomicType)target.getPrimitiveItemType();
        derived = (targetType.getFingerprint() != targetPrimitiveType.getFingerprint());
        adoptChildExpression(source);
    }

    /**
    * Simplify the expression
    * @return the simplified expression
    */

     public Expression simplify(StaticContext env) throws XPathException {
        if ((targetType instanceof BuiltInAtomicType) && !env.isAllowedBuiltInType(targetType)) {
            // this is checked here because the ConstructorFunctionLibrary doesn't have access to the static
            // context at bind time
            StaticError err = new StaticError("The type " + targetType.getDisplayName() +
                    " is not recognized by a Basic XSLT Processor", this);
            err.setErrorCode("XPST0080");
            throw err;
        }
        operand = operand.simplify(env);
        if (operand instanceof AtomicValue) {
            return typeCheck(env, Type.ITEM_TYPE);
        }
        return this;
    }

    /**
    * Type-check the expression
    */

    public Expression typeCheck(StaticContext env, ItemType contextItemType) throws XPathException {
        operand = operand.typeCheck(env, contextItemType);
        SequenceType atomicType = SequenceType.makeSequenceType(Type.ANY_ATOMIC_TYPE, getCardinality());

        RoleLocator role = new RoleLocator(RoleLocator.TYPE_OP, "cast as", 0, null);
        role.setSourceLocator(this);
        operand = TypeChecker.staticTypeCheck(operand, atomicType, false, role, env);

        final TypeHierarchy th = env.getConfiguration().getTypeHierarchy();
        ItemType sourceType = operand.getItemType(th);
        if (th.isSubType(sourceType, targetType)) {
            // It's generally true that any expression defined to return an X is allowed to return a subtype of X.
            // However, people seem to get upset if we treat the cast as a no-op.
            upcast = true;
            return this;
        }
//            if (operand instanceof ComputedExpression) {
//                ((ComputedExpression)operand).setParentExpression(getParentExpression());
//            }
//            return operand;
//        }
//        if (targetType.isNamespaceSensitive() && operand instanceof StringValue
//                && !(operand instanceof UntypedAtomicValue)) {
//            return castStringToQName(env);
//        }
        if (operand instanceof AtomicValue) {
            return (AtomicValue)evaluateItem(env.makeEarlyEvaluationContext());
        }
        if (operand instanceof EmptySequence) {
            if (allowEmpty) {
                return operand;
            } else {
                StaticError err = new StaticError("Cast can never succeed: the operand must not be an empty sequence");
                err.setErrorCode("XPTY0004");
                err.setLocator(this);
                err.setIsTypeError(true);
                throw err;
            }
        }
        int p = sourceType.getPrimitiveType();
        if (!isPossibleCast(p, targetType.getPrimitiveType())) {
            StaticError err = new StaticError("Casting from " + sourceType + " to " + targetType +
                    " can never succeed");
            err.setErrorCode("XPTY0004");
            err.setLocator(this);
            err.setIsTypeError(true);
            throw err;
        }

        return this;
    }

    /**
    * Get the static cardinality of the expression
    */

    public int computeCardinality() {
        return (allowEmpty & Cardinality.allowsZero(operand.getCardinality())
                ? StaticProperty.ALLOWS_ZERO_OR_ONE : StaticProperty.EXACTLY_ONE);
    }

    /**
    * Get the static type of the expression
     * @param th
     */

    public ItemType getItemType(TypeHierarchy th) {
        return targetType;
    }

    /**
     * Determine the special properties of this expression
     * @return {@link StaticProperty#NON_CREATIVE}.
     */

    public int computeSpecialProperties() {
        int p = super.computeSpecialProperties();
        return p | StaticProperty.NON_CREATIVE;
    }

    /**
    * Evaluate the expression
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        AtomicValue value = (AtomicValue)operand.evaluateItem(context);
        if (value==null) {
            if (allowEmpty) {
                return null;
            } else {
                DynamicError e = new DynamicError("Cast does not allow an empty sequence");
                e.setXPathContext(context);
                e.setLocator(this);
                e.setErrorCode("XPTY0004");
                throw e;
            }
        }
        if (upcast) {
            // When casting to a supertype of the original type, we can bypass validation
            AtomicValue result = value.convert(targetPrimitiveType, context, false);
            if (derived) {
                result = result.convert(targetType, context, false);
            }
            return result;
        }
        AtomicValue result = value.convert(targetPrimitiveType, context, true);
        if (result instanceof ValidationErrorValue) {
            XPathException err = ((ValidationErrorValue)result).getException();
            String code = err.getErrorCodeLocalPart();
            dynamicError(err.getMessage(), code, context);
        }
        if (derived) {
            result = result.convert(targetType, context, true);
            if (result instanceof ValidationErrorValue) {
                XPathException err = ((ValidationErrorValue)result).getException();
                String code = err.getErrorCodeLocalPart();
                dynamicError(err.getMessage(), code, context);
            }
        }
        return result;
    }

    /**
     * Is this expression the same as another expression?
     */

    public boolean equals(Object other) {
        return super.equals(other) &&
                targetType == ((CastExpression)other).targetType &&
                allowEmpty == ((CastExpression)other).allowEmpty;
    }

    /**
     * Give a string representation of the operator for use in diagnostics
     * @return the operator, as a string
     * @param config
     */

    protected String displayOperator(Configuration config) {
        return "cast as " + targetType.toString(config.getNamePool());
    }

    /**
     * Evaluate the "pseudo-cast" of a string literal to a QName or NOTATION value. This can only happen
     * at compile time
     * @return the QName or NOTATION value that results from casting the string to a QName.
     * This will either be a QNameValue or a DerivedAtomicValue derived from QName or NOTATION
     */

    public static AtomicValue castStringToQName(
            CharSequence operand, AtomicType targetType, StaticContext env) throws XPathException {
        try {
            CharSequence arg = Whitespace.trimWhitespace(operand);
            String parts[] = env.getConfiguration().getNameChecker().getQNameParts(arg);
            String uri;
            if ("".equals(parts[0])) {
                uri = env.getNamePool().getURIFromURICode(env.getDefaultElementNamespace());
            } else {
                uri = env.getURIForPrefix(parts[0]);
                if (uri==null) {
                    StaticError e = new StaticError("Prefix '" + parts[0] + "' has not been declared");
                    e.setErrorCode("FONS0004");
                    throw e;
                }
            }
            final NameChecker checker = env.getConfiguration().getNameChecker();
            final TypeHierarchy th = env.getConfiguration().getTypeHierarchy();
            if (targetType.getFingerprint() == StandardNames.XS_QNAME) {
                return new QNameValue(parts[0], uri, parts[1], checker);
            } else if (th.isSubType(targetType, Type.QNAME_TYPE)) {
                QNameValue q = new QNameValue(parts[0], uri, parts[1], checker);
                AtomicValue av = targetType.makeDerivedValue(q, arg, true);
                if (av instanceof ValidationErrorValue) {
                    throw ((ValidationErrorValue)av).getException();
                }
                return av;
            } else {
                NotationValue n = new NotationValue(parts[0], uri, parts[1], checker);
                AtomicValue av =  targetType.makeDerivedValue(n, arg, true);
                if (av instanceof ValidationErrorValue) {
                    throw ((ValidationErrorValue)av).getException();
                }
                return av;
            }
        } catch (XPathException err) {
            StaticError e = new StaticError(err);
            e.setErrorCode("FONS0004");
            throw e;
        } catch (QNameException err) {
            StaticError e = new StaticError(err);
            e.setErrorCode("FONS0004");
            throw e;
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
// Contributor(s): none.
//
