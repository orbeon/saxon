package org.orbeon.saxon.value;

import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.style.StandardNames;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.xpath.DynamicError;
import org.orbeon.saxon.xpath.XPathException;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of the xs:gYear data type
 */

public class GYearValue extends DateValue {

    private static Pattern regex =
            Pattern.compile("(-?[0-9]+)(Z|[+-][0-9][0-9]:[0-9][0-9])?");

    public GYearValue(){};

    public GYearValue(CharSequence value) throws XPathException {
        Matcher m = regex.matcher(value);
        if (!m.matches()) {
            throw new DynamicError("Cannot convert '" + value + "' to a gYear");
        }
        String base = m.group(1);
        String tz = m.group(2);
        String date = base + "-01-01" + (tz==null ? "" : tz);
        setLexicalValue(date);
    }

    /**
    * Determine the data type of the expression
    * @return Type.G_YEAR_TYPE,
    */

    public ItemType getItemType() {
        return Type.G_YEAR_TYPE;
    }


    /**
    * Convert to target data type
    * @param requiredType an integer identifying the required atomic type
    * @return an AtomicValue, a value of the required type
    * @throws XPathException if the conversion is not possible
    */

    public AtomicValue convert(int requiredType, XPathContext context) throws XPathException {
        switch(requiredType) {
        case Type.G_YEAR:
        case Type.ATOMIC:
        case Type.ITEM:
            return this;

        case Type.STRING:
            return new StringValue(getStringValue());
        case Type.UNTYPED_ATOMIC:
            return new UntypedAtomicValue(getStringValue());
        default:
            DynamicError err = new DynamicError("Cannot convert gYear to " +
                                     StandardNames.getDisplayName(requiredType));
            err.setErrorCode("FORG0001");
            throw err;
        }
    }

    public String getStringValue() {

        StringBuffer sb = new StringBuffer();
        int era = calendar.get(GregorianCalendar.ERA);
        int year = calendar.get(Calendar.YEAR);
        if (era == GregorianCalendar.BC) {
            sb.append('-');
        }
        DateTimeValue.appendString(sb, year, (year>9999 ? (calendar.get(Calendar.YEAR)+"").length() : 4));

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