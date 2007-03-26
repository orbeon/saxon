package org.orbeon.saxon.value;

import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.FastStringBuffer;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of the xs:gYear data type
 */

public class GMonthDayValue extends DateValue {

    private static Pattern regex =
            Pattern.compile("--([0-9][0-9]-[0-9][0-9])(Z|[+-][0-9][0-9]:[0-9][0-9])?");

    public GMonthDayValue(){};

    public GMonthDayValue(CharSequence value) throws XPathException {
        Matcher m = regex.matcher(Whitespace.trimWhitespace(value));
        if (!m.matches()) {
            throw new DynamicError("Cannot convert '" + value + "' to a gMonthDay");
        }
        String base = m.group(1);
        String tz = m.group(2);
        String date = "2000-" + base + (tz==null ? "" : tz);
        setLexicalValue(date);
    }

    public GMonthDayValue(byte month, byte day, int tz) {
        this.year = 2000;
        this.month = month;
        this.day = day;
        setTimezoneInMinutes(tz);
    }

    /**
    * Determine the data type of the expression
    * @return Type.G_MONTH_DAY_TYPE,
     * @param th
     */

    public ItemType getItemType(TypeHierarchy th) {
        return Type.G_MONTH_DAY_TYPE;
    }

    /**
     * Make a copy of this date, time, or dateTime value
     */

    public CalendarValue copy() {
        return new GMonthDayValue(month, day, getTimezoneInMinutes());
    }

    /**
    * Convert to target data type
    * @param requiredType an integer identifying the required atomic type
    * @param context
     * @return an AtomicValue, a value of the required type; or an ErrorValue
    */

    public AtomicValue convertPrimitive(BuiltInAtomicType requiredType, boolean validate, XPathContext context)  {
        switch(requiredType.getPrimitiveType()) {
        case Type.G_MONTH_DAY:
        case Type.ANY_ATOMIC:
        case Type.ITEM:
            return this;

        case Type.STRING:
            return new StringValue(getStringValueCS());
        case Type.UNTYPED_ATOMIC:
            return new UntypedAtomicValue(getStringValueCS());
        default:
            ValidationException err = new ValidationException("Cannot convert gMonthDay to " +
                                     requiredType.getDisplayName());
            err.setErrorCode("XPTY0004");
            err.setIsTypeError(true);
            return new ValidationErrorValue(err);
        }
    }

    public CharSequence getStringValueCS() {

        FastStringBuffer sb = new FastStringBuffer(16);

        sb.append("--");
        appendTwoDigits(sb, month);
        sb.append('-');
        appendTwoDigits(sb, day);

        if (hasTimezone()) {
            appendTimezone(sb);
        }

        return sb;

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
// The Initial Developer of the Original Code is Saxonica Limited
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none
//