package net.sf.saxon.value;

import net.sf.saxon.om.FastStringBuffer;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.BuiltInAtomicType;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;
import net.sf.saxon.type.ValidationException;
import net.sf.saxon.ConversionContext;

import java.util.Calendar;
import java.util.GregorianCalendar;
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
        Matcher m = regex.matcher(value);
        if (!m.matches()) {
            throw new DynamicError("Cannot convert '" + value + "' to a gMonthDay");
        }
        String base = m.group(1);
        String tz = m.group(2);
        String date = "2000-" + base + (tz==null ? "" : tz);
        setLexicalValue(date);
    }

    public GMonthDayValue(GregorianCalendar calendar, boolean timezoneSpecified, int tzoffset) {
        super(calendar, timezoneSpecified, tzoffset);
    }

    /**
    * Determine the data type of the expression
    * @return Type.G_MONTH_DAY_TYPE,
    */

    public ItemType getItemType() {
        return Type.G_MONTH_DAY_TYPE;
    }

    /**
    * Convert to target data type
    * @param requiredType an integer identifying the required atomic type
    * @param conversion
     * @return an AtomicValue, a value of the required type; or an ErrorValue
    */

    public AtomicValue convertPrimitive(BuiltInAtomicType requiredType, boolean validate, ConversionContext conversion)  {
        switch(requiredType.getPrimitiveType()) {
        case Type.G_MONTH_DAY:
        case Type.ATOMIC:
        case Type.ITEM:
            return this;

        case Type.STRING:
            return new StringValue(getStringValueCS());
        case Type.UNTYPED_ATOMIC:
            return new UntypedAtomicValue(getStringValueCS());
        default:
            ValidationException err = new ValidationException("Cannot convert gMonthDay to " +
                                     requiredType.getDisplayName());
            err.setErrorCode("FORG0001");
            return new ValidationErrorValue(err);
        }
    }

    public String getStringValue() {

        FastStringBuffer sb = new FastStringBuffer(16);

        sb.append("--");
        DateTimeValue.appendString(sb, calendar.get(Calendar.MONTH)+1, 2);
        sb.append('-');
        DateTimeValue.appendString(sb, calendar.get(Calendar.DATE), 2);

        if (zoneSpecified) {
            DateTimeValue.appendTimezone(tzOffset, sb);
        }

        return sb.toString();

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