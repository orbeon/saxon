package net.sf.saxon.functions;
import net.sf.saxon.Controller;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.Name;
import net.sf.saxon.om.QNameException;
import net.sf.saxon.om.NamespaceResolver;
import net.sf.saxon.style.ExpressionContext;
import net.sf.saxon.trans.DecimalFormatManager;
import net.sf.saxon.value.NumericValue;
import net.sf.saxon.value.StringValue;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.xpath.XPathException;
import net.sf.saxon.xpath.DynamicError;
import net.sf.saxon.xpath.StaticError;

import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.io.Serializable;
import java.math.BigInteger;

/**
* XSLT 2.0 implementation of format-number() function - removes the dependence on the JDK.
*/

// (though we still use a Java DecimalFormatSymbols object to hold the special characters).

public class FormatNumber2 extends SystemFunction implements XSLTFunction {

    private NamespaceResolver nsContext = null;
        // held only if the third argument is present, and its value is not known statically

    private DecimalFormatSymbols decimalFormatSymbols = null;
        // held only if the decimal format to use can be determined statically

    private transient String picture = null;
        // held transiently at compile time if the picture is known statically

    private SubPicture[] subPictures = null;
        // held if the picture is known statically

    private boolean requireFixup = false;
        // used to detect when an unknown decimal-format name is used

    private transient boolean checked = false;
        // the second time checkArguments is called, it's a global check so the static context is inaccurate


    public void checkArguments(StaticContext env) throws XPathException {
        if (checked) return;
        checked = true;
        super.checkArguments(env);
        if (argument[1] instanceof StringValue) {
            // picture is known statically - optimize for this common case
            picture = ((StringValue)argument[1]).getStringValue();
        }
        if (argument.length==3) {
            if (argument[2] instanceof StringValue) {
                // common case, decimal format name is supplied as a string literal

                String qname = ((StringValue)argument[2]).getStringValue();
                String dfLocalName;
                String dfURI;
                try {
                    String[] parts = Name.getQNameParts(qname);
                    dfLocalName = parts[1];
                    dfURI = env.getURIForPrefix(parts[0]);
                } catch (QNameException e) {
                    throw new StaticError("Invalid decimal format name. " + e.getMessage());
                }

                DecimalFormatManager dfm = ((ExpressionContext)env).getXSLStylesheet().getDecimalFormatManager();
                requireFixup = true;
                dfm.registerUsage(dfURI, dfLocalName, this);
                    // this causes a callback to the fixup() method, either now, or later if it's a forwards reference
            } else {
                // we need to save the namespace context
                nsContext = env.getNamespaceResolver();
            }
        } else {
            // two arguments only: it uses the default decimal format
            if (env instanceof ExpressionContext) {
                // this is XSLT
                DecimalFormatManager dfm = ((ExpressionContext)env).getXSLStylesheet().getDecimalFormatManager();
                dfm.registerUsage("", "", this);
                // Note: if using the "default default", there will be no fixup call.
            } else {
                // using saxon:decimal-format in some other environment
            }
        }
    }

    /**
    * Fixup: this is a callback from the DecimalFormatManager used once the xsl:decimal-format
    * element is identified
    */

    public void fixup(DecimalFormatSymbols dfs) {
        // System.err.println("Fixed up format-number, picture=" + picture);
        requireFixup = false;
        decimalFormatSymbols = dfs;
        if (picture != null) {
            try {
                subPictures = getSubPictures(picture, dfs);
            } catch (XPathException err) {
                subPictures = null;
                    // we'll report the error at run-time
            }
        }
    }

    /**
    * Analyze a picture string into two sub-pictures.
    * @return an array of two sub-pictures, the positive and the negative sub-pictures respectively.
    * If there is only one sub-picture, the second one is null.
    */

    private SubPicture[] getSubPictures(String picture, DecimalFormatSymbols dfs) throws XPathException {
        SubPicture[] pics = new SubPicture[2];
        if (picture.length()==0) {
            DynamicError err = new DynamicError("format-number() picture is zero-length");
            err.setErrorCode("XT1310");
            throw err;
        }
        int sep = picture.indexOf(dfs.getPatternSeparator());

        if (sep<0) {
            pics[0] = new SubPicture(picture, dfs);
            pics[1] = null;
        } else {
            if (sep==picture.length()-1) {
                grumble("second subpicture is zero-length");
            }
            if (picture.indexOf(dfs.getPatternSeparator(), sep+1) >= 0) {
                grumble("more than one pattern separator");
            }
            if (sep==0) {
                grumble("first subpicture is zero-length");
            }
            pics[0] = new SubPicture(picture.substring(0, sep), dfs);
            pics[1] = new SubPicture(picture.substring(sep+1), dfs);
        }
        return pics;
    }

    /**
    * preEvaluate: this method suppresses compile-time evaluation by doing nothing.
    * We can't evaluate early because we don't have access to the DecimalFormatManager.
    */

    // TODO: we could evaluate early when there are two arguments

    public Expression preEvaluate(StaticContext env) {
        return this;
    }

    /**
    * Evaluate in a context where a string is wanted
    */

    public String evaluateAsString(XPathContext context) throws XPathException {
        int numArgs = argument.length;
        Controller ctrl = context.getController();

        DecimalFormatSymbols dfs = decimalFormatSymbols;

        AtomicValue av0 = (AtomicValue)argument[0].evaluateItem(context);
        NumericValue number = (NumericValue)av0.getPrimitiveValue();

        if (dfs == null) {
            // the decimal-format name was not resolved statically
            if (requireFixup) {
                // we registered for a fixup, but none came
                dynamicError("Unknown decimal format name", "XT1280", context);
                return null;
            }
            DecimalFormatManager dfm = ctrl.getDecimalFormatManager();
            if (numArgs==2) {
                dfs = dfm.getDefaultDecimalFormat();
            } else {
                // the decimal-format name was given as a run-time expression
                String qname = argument[2].evaluateItem(context).getStringValue();
                try {
                    String[] parts = Name.getQNameParts(qname);
                    String localName = parts[1];
                    String uri = nsContext.getURIForPrefix(parts[0], false);
                    if (uri==null) {
                        dynamicError("Namespace prefix '" + parts[0] + "' has not been defined", "XT1280", context);
                        return null;
                    }
                    dfs = dfm.getNamedDecimalFormat(uri, localName);
                    if (dfs==null) {
                        dynamicError(
                            "format-number function: decimal-format '" + localName + "' is not defined", "XT1280", context);
                        return null;
                    }
                } catch (QNameException e) {
                    dynamicError("Invalid decimal format name. " + e.getMessage(), "XT1280", context);
                }
            }
        }
        SubPicture[] pics = subPictures;
        if (pics == null) {
            String format = argument[1].evaluateItem(context).getStringValue();
            pics = getSubPictures(format, dfs);
        }
        return formatNumber(number.getDoubleValue(), pics, dfs).toString();
    }

    /**
    * Evaluate in a general context
    */

    public Item evaluateItem(XPathContext c) throws XPathException {
        return new StringValue(evaluateAsString(c));
    }

    /**
    * Format a number, given the two subpictures and the decimal format symbols
    */

    private CharSequence formatNumber(double n,
                                      SubPicture[] subPictures,
                                      DecimalFormatSymbols dfs) {

        SubPicture pic;
        String minusSign = "";
        if (n < 0) {
            if (subPictures[1]==null) {
                pic = subPictures[0];
                minusSign = "" + dfs.getMinusSign();
            } else {
                pic = subPictures[1];
            }
        } else {
            pic = subPictures[0];
        }

        return pic.format(Math.abs(n), dfs, minusSign);
    }

    private void grumble(String s) throws XPathException {
        dynamicError("format-number picture: " + s, "XT1310", null);
    }

    /**
    * Inner class to represent one sub-picture (the negative or positive subpicture)
    */

    private class SubPicture implements Serializable {

        int minWholePartSize = 0;
        int maxWholePartSize = 0;
        int minFractionPartSize = 0;
        int maxFractionPartSize = 0;
        boolean isPercent = false;
        boolean isPerMille = false;
        String prefix = "";
        String suffix = "";
        int[] wholePartGroupingPositions = null;
        int[] fractionalPartGroupingPositions = null;

        public SubPicture(String pic, DecimalFormatSymbols dfs) throws XPathException {

            final char percentSign = dfs.getPercent();
            final char perMilleSign = dfs.getPerMill();
            final char decimalSeparator = dfs.getDecimalSeparator();
            final char groupingSeparator = dfs.getGroupingSeparator();
            final char digitSign = dfs.getDigit();
            final char zeroDigit = dfs.getZeroDigit();

            List wholePartPositions = null;
            List fractionalPartPositions = null;

            // TODO: check sub-pic contains at least one digit or zero-digit sign
            // System.err.println("Creating sub-picture " + pic);

            int phase = 0;
                // phase = 0: passive characters at start
                // phase = 1: digit signs in whole part
                // phase = 2: zero-digit signs in whole part
                // phase = 3: zero-digit signs in fractional part
                // phase = 4: digit signs in fractional part
                // phase = 5: passive characters at end

            for (int i=0; i<pic.length(); i++) {
                char c = pic.charAt(i);

                if (c == percentSign || c == perMilleSign) {
                    if (isPercent || isPerMille) {
                        grumble("Cannot have more than one percent or per-mille character in a sub-picture");
                    }
                    isPercent = (c==percentSign);
                    isPerMille = (c==perMilleSign);
                    switch (phase) {
                        case 0:
                            prefix += c;
                            break;
                        case 1:
                        case 2:
                        case 3:
                        case 4:
                        case 5:
                            phase = 5;
                            suffix += c;
                            break;
                    }
                } else if (c == digitSign) {
                    switch (phase) {
                        case 0:
                        case 1:
                            phase = 1;
                            maxWholePartSize++;
                            break;
                        case 2:
                            grumble("Digit sign must not appear after a zero-digit sign in the integer part of a sub-picture");
                            break;
                        case 3:
                        case 4:
                            phase = 4;
                            maxFractionPartSize++;
                            break;
                        case 5:
                            grumble("Passive character must not appear between active characters in a sub-picture");
                            break;
                    }
                } else if (c == zeroDigit) {
                    switch (phase) {
                        case 0:
                        case 1:
                        case 2:
                            phase = 2;
                            minWholePartSize++;
                            maxWholePartSize++;
                            break;
                        case 3:
                            minFractionPartSize++;
                            maxFractionPartSize++;
                            break;
                        case 4:
                            grumble("Zero digit sign must not appear after a digit sign in the fractional part of a sub-picture");
                            break;
                        case 5:
                            grumble("Passive character must not appear between active characters in a sub-picture");
                            break;
                    }
                } else if (c == decimalSeparator) {
                    switch (phase) {
                        case 0:
                        case 1:
                        case 2:
                            phase = 3;
                            break;
                        case 3:
                        case 4:
                        case 5:
                            grumble("There must only be one decimal separator in a sub-picture");
                            break;
                    }
                } else if (c == groupingSeparator) {
                    switch (phase) {
                        case 0:
                        case 1:
                        case 2:
                            if (wholePartPositions == null) {
                                wholePartPositions = new ArrayList(3);
                            }
                            wholePartPositions.add(new Integer(maxWholePartSize));
                                // note these are positions from a false offset, they will be corrected later
                            break;
                        case 3:
                        case 4:
                            if (maxFractionPartSize == 0) {
                                grumble("Grouping separator cannot be adjacent to decimal separator");
                            }
                            if (fractionalPartPositions == null) {
                                fractionalPartPositions = new ArrayList(3);
                            }
                            fractionalPartPositions.add(new Integer(maxFractionPartSize));
                            break;
                        case 5:
                            grumble("Grouping separator found in suffix of sub-picture");
                            break;
                    }
                } else {    // passive character found
                    switch (phase) {
                        case 0:
                            prefix += c;
                            break;
                        case 1:
                        case 2:
                        case 3:
                        case 4:
                        case 5:
                            phase = 5;
                            suffix += c;
                            break;
                    }
                }
            }

            // System.err.println("minWholePartSize = " + minWholePartSize);
            // System.err.println("maxWholePartSize = " + maxWholePartSize);
            // System.err.println("minFractionPartSize = " + minFractionPartSize);
            // System.err.println("maxFractionPartSize = " + maxFractionPartSize);

            // Sort out the grouping positions

            if (wholePartPositions != null) {
                // convert to positions relative to the decimal separator
                int n = wholePartPositions.size();
                wholePartGroupingPositions = new int[n];
                for (int i=0; i<n; i++) {
                    wholePartGroupingPositions[i] =
                        maxWholePartSize - ((Integer)wholePartPositions.get(n - i - 1)).intValue();
                }
                if (n > 1) {
                    boolean regular = true;
                    int first = wholePartGroupingPositions[0];
                    for (int i=1; i<n; i++) {
                        if (wholePartGroupingPositions[i] != i * first) {
                            regular = false;
                            break;
                        }
                    }
                    if (regular) {
                        wholePartGroupingPositions = new int[1];
                        wholePartGroupingPositions[0] = first;
                    }
                }
                if (wholePartGroupingPositions[0] == 0) {
                    grumble("Cannot have a grouping separator adjacent to the decimal separator");
                }
            }

            if (fractionalPartPositions != null) {
                int n = fractionalPartPositions.size();
                fractionalPartGroupingPositions = new int[n];
                for (int i=0; i<n; i++) {
                    fractionalPartGroupingPositions[i] =
                        ((Integer)fractionalPartPositions.get(i)).intValue();
                }
            }
        }

        /**
        * Format a number using this sub-picture
        * @param value the absolute value of the number to be formatted
        */

        public CharSequence format(double value, DecimalFormatSymbols dfs, String minusSign) {

            // System.err.println("Formatting " + value);

            if (Double.isNaN(value)) {
                return prefix + dfs.getNaN() + suffix;
            }

            if (Double.isInfinite(value)) {
                return prefix + minusSign + dfs.getInfinity() + suffix;
            }

            if (isPercent) {
                value = 100 * value;
            } else if (isPerMille) {
                value = 1000 * value;
            }

            // following (commented-out) lines work, but are slow. We may need to use this approach
            // for very large or small numbers, however.

            // BigDecimal dec = new BigDecimal(value).setScale(maxFractionPartSize, BigDecimal.ROUND_HALF_EVEN);
            // StringBuffer sb = new StringBuffer(dec.toString());

            StringBuffer sb = new StringBuffer(20);

            // Convert to a scaled integer, by multiplying by 10^d where d is the maximum fraction size

            double d = value;
            if (maxFractionPartSize != 0) {
                d *= Math.pow(10, maxFractionPartSize);
            }
            int point;


            if (Math.abs(d) > Long.MAX_VALUE) {
                // If this exceeds the size of a long, construct a BigInteger
                long bits = Double.doubleToLongBits(value);
                boolean negative = (bits & 0x8000000000000000L) != 0;
                int exponent = (int)((bits & 0x7ff0000000000000L)>>52) - 1023 - 52;
                long mantissa = bits & 0x000fffffffffffffL | 0x0010000000000000L;
                BigInteger big = BigInteger.valueOf(mantissa);
                big = big.multiply(BigInteger.valueOf(2).pow(exponent));
                // TODO: round the value to about 18 significant digits.
                if (negative) {
                    sb.append('-');
                }
                sb.append(big.toString());
                // add a decimal point, it will be removed later if not needed
                point = sb.length();
                sb.append('.');

                // TODO: Java DecimalFormat gives nicer results for large
                // values, e.g. 1e19 comes out as 10,000,000,000,000,000,000
                // whereas we are producing 10,000,018,432,000,000,000.

//                int power = 0;
//                while (Math.abs(d) > Long.MAX_VALUE) {
//                    // there must be an easier way than this...
//                    d = d / 10;
//                    power++;
//                }
//                // this is likely to have accumulated rounding errors, so do it again
//                d = value * Math.pow(10, -power);
//                sb.append((long)d);
//                for (int i=0; i<power; i++) {
//                    sb.append('0');
//                }
//                // add a decimal point, it will be removed later...
//                point = sb.length();
//                sb.append('.');

            } else {
                long ld = (long)d;

                // Now apply any rounding needed, using the "round half to even" rule

                double rem = d - ld;
                if (rem > 0.5) {
                    ld++;
                } else if (rem == 0.5) {
                    // round half to even - check the last bit
                    if ((ld & 1) == 1) {
                        ld++;
                    }
                }

                // TODO. We currently use the built-in Java integer-to-string conversion. We could do
                // it ourselves, which would enable addition of leading zeros and mapping to the selected
                // Unicode digits to be done on-the-fly rather than by post-processing.

                String sd = "" + ld;
                int wholeSize = sd.length() - maxFractionPartSize;
                if (wholeSize > 0) {
                    sb.append(sd.substring(0, wholeSize));
                }

                point = sb.length();
                sb.append('.');

                while (wholeSize < 0) {
                    sb.append('0');
                    wholeSize++;
                }
                sb.append(sd.substring(wholeSize));

                // System.err.println("Rounded number: " + sb.toString());

                while (point < minWholePartSize) {
                    sb.insert(0, '0');
                    point++;
                }

                while (point > maxWholePartSize) {
                    if (sb.charAt(0)=='0') {
                        sb.deleteCharAt(0);
                        point--;
                    } else {
                        break;
                    }
                                // TODO: check here whether the number exceeds the overflow threshold
                }
                int actualFractionSize = sb.length()-point-1;
                while (actualFractionSize > minFractionPartSize) {
                    if (sb.charAt(sb.length()-1) == '0') {
                        sb.deleteCharAt(sb.length()-1);
                        actualFractionSize--;
                    } else {
                        break;
                    }
                }
            }

            // System.err.println("Justified number: " + sb.toString());

            // Map the digits and decimal point to use the selected characters

                        // TODO: can a mapped digit be a surrogate pair? Perhaps we should
                        // call the translate() function. Java DecimalFormatSymbols doesn't allow
                        // it - it must be a Java char.

            if (dfs.getDecimalSeparator() != '.') {
                sb.setCharAt(point, dfs.getDecimalSeparator());
            }

            // If there is no fractional part, delete the decimal point

            if (maxFractionPartSize == 0) {
                sb.deleteCharAt(point);
            }

            if (dfs.getZeroDigit() != '0') {
                char newZero = dfs.getZeroDigit();
                for (int i=0; i<sb.length(); i++) {
                    char c = sb.charAt(i);
                    if (c>='0' && c<='9') {
                        sb.setCharAt(i, (char)(c-'0'+newZero));
                    }
                }
            }

            // System.err.println("Mapped number: " + sb.toString());

            // Add the whole-part grouping separators

            if (wholePartGroupingPositions != null) {
                if (wholePartGroupingPositions.length == 1) {
                    // grouping separators are at regular positions
                    int g = wholePartGroupingPositions[0];
                    int p = point - g;
                    while (p > 0) {
                        sb.insert(p, dfs.getGroupingSeparator());
                        p -= g;
                    }
                } else {
                    // grouping separators are at irregular positions
                    for (int i=0; i<wholePartGroupingPositions.length; i++) {
                        int p = point - wholePartGroupingPositions[i];
                        if (p > 0) {
                            sb.insert(p, dfs.getGroupingSeparator());
                        }
                    }
                }
            }



            // Add the fractional-part grouping separators

            if (fractionalPartGroupingPositions != null) {
                    // grouping separators are at irregular positions.
                for (int i=0; i<fractionalPartGroupingPositions.length; i++) {
                    int p = point + 1 + fractionalPartGroupingPositions[i] + i;
                    if (p < sb.length()-1) {
                        sb.insert(p, dfs.getGroupingSeparator());
                    }
                }
            }

            // System.err.println("Grouped number: " + sb.toString());

            //sb.insert(0, prefix + minusSign);
                    // spec has changed
            sb.insert(0, prefix);
            sb.insert(0, minusSign);
            sb.append(suffix);

            return sb;
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
