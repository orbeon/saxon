package net.sf.saxon.exslt;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.EmptySequence;
import net.sf.saxon.value.SequenceExtent;
import net.sf.saxon.value.Value;

import java.util.ArrayList;

/**
* This class implements extension functions in the
* http://exslt.org/math namespace. <p>
*/

public abstract class Math  {

    /**
    * Get the maximum numeric value of the string-value of each of a set of nodes
    */

    public static double max (SequenceIterator nsv) throws XPathException {
        double max = Double.NEGATIVE_INFINITY;
        try {
            while (true) {
                Item it = nsv.next();
                if (it == null) break;
                double x = Value.stringToNumber(it.getStringValueCS());
                if (Double.isNaN(x)) return x;
                if (x>max) max = x;
            }
            return max;
        } catch (NumberFormatException err) {
            return Double.NaN;
        }
    }


    /**
    * Get the minimum numeric value of the string-value of each of a set of nodes
    */

    public static double min (SequenceIterator nsv) throws XPathException {
        try {
            double min = Double.POSITIVE_INFINITY;
            while (true) {
                Item it = nsv.next();
                if (it == null) break;
                double x = Value.stringToNumber(it.getStringValueCS());
                if (Double.isNaN(x)) return x;
                if (x<min) min = x;
            }
            return min;
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }


    /**
    * Get the items with maximum numeric value of the string-value of each of a sequence of items.
    * The items are returned in the order of the original sequence.
    */

    public static Value highest (SequenceIterator nsv) throws XPathException {
        try {
            double max = Double.NEGATIVE_INFINITY;
            ArrayList highest = new ArrayList();
            while (true) {
                Item it = nsv.next();
                if (it == null) break;
                double x = Value.stringToNumber(it.getStringValueCS());
                if (Double.isNaN(x)) return EmptySequence.getInstance();
                if (x==max) {
                    highest.add(it);
                } else if (x>max) {
                    max = x;
                    highest.clear();
                    highest.add(it);
                }
            }
            return new SequenceExtent(highest);
        } catch (NumberFormatException e) {
            return EmptySequence.getInstance();
        }
    }



    /**
    * Get the items with minimum numeric value of the string-value of each of a sequence of items
    * The items are returned in the order of the original sequence.
    */

    public static Value lowest (SequenceIterator nsv) throws XPathException {
        try {
            double min = Double.POSITIVE_INFINITY;
            ArrayList lowest = new ArrayList();
            while (true) {
               Item it = nsv.next();
               if (it == null) break;
               double x = Value.stringToNumber(it.getStringValueCS());
               if (Double.isNaN(x)) return EmptySequence.getInstance();
               if (x==min) {
                   lowest.add(it);
               } else if (x<min) {
                   min = x;
                   lowest.clear();
                   lowest.add(it);
               }
           }
            return new SequenceExtent(lowest);
        } catch (NumberFormatException e) {
            return EmptySequence.getInstance();
        }
    }

    /**
    * Get the absolute value of a numeric value (SStL)
    */

    public static double abs (double x) {
        return java.lang.Math.abs(x);
    }

    /**
    * Get the square root of a numeric value (SStL)
    */

    public static double sqrt (double x) {
        return java.lang.Math.sqrt(x);
    }

    /**
    * Get the power of two numeric values  (SStL)
    */

    public static double power (double x, double y) {
        return java.lang.Math.pow(x,y);
    }

    /**
    * Get a named constant to a given precision  (SStL)
    */

    public static double constant (XPathContext context, String name, double precision) throws XPathException {
        //PI, E, SQRRT2, LN2, LN10, LOG2E, SQRT1_2

        String con = "";

        if (name.equals("PI")) {
            con="3.1415926535897932384626433832795028841971693993751";
        } else if (name.equals("E")) {
            con="2.71828182845904523536028747135266249775724709369996";
        } else if (name.equals("SQRRT2")) {
            con="1.41421356237309504880168872420969807856967187537694";
        } else if (name.equals("LN2")) {
            con="0.69314718055994530941723212145817656807550013436025";
        } else if (name.equals("LN10")) {
            con="2.302585092994046";
        } else if (name.equals("LOG2E")) {
            con="1.4426950408889633";
        } else if (name.equals("SQRT1_2")) {
            con="0.7071067811865476";
        } else {
            DynamicError e = new DynamicError("Unknown math constant " + name);
            e.setXPathContext(context);
            throw e;
        }

        int x = (int) precision;
        String returnVal=con.substring(0,x+2);
        double rV=new Double(returnVal).doubleValue();
        return rV;
    }

    /**
    * Get the logarithm of a numeric value (SStL)
    */

    public static double log (double x) {
        return java.lang.Math.log(x);
    }

    /**
    * Get a random numeric value (SStL)
    */

    public static double random() {
        return java.lang.Math.random();
    }

    /**
    * Get the sine of a numeric value (SStL)
    */

    public static double sin (double x) {
        return java.lang.Math.sin(x);
    }

    /**
    * Get the cosine of a numeric value (SStL)
    */

    public static double cos (double x) {
        return java.lang.Math.cos(x);
    }

    /**
    * Get the tangent of a numeric value  (SStL)
    */

    public static double tan (double x) {
        return java.lang.Math.tan(x);
    }

    /**
    * Get the arcsine of a numeric value  (SStL)
    */

    public static double asin (double x) {
        return java.lang.Math.asin(x);
    }

    /**
    * Get the arccosine of a numeric value  (SStL)
    */

    public static double acos (double x) {
        return java.lang.Math.acos(x);
    }

    /**
    * Get the arctangent of a numeric value  (SStL)
    */

    public static double atan (double x) {
        return java.lang.Math.atan(x);
    }

    /**
    * Converts rectangular coordinates to polar  (SStL)
    */

    public static double atan2 (double x, double y) {
        return java.lang.Math.atan2(x,y);
    }

    /**
    * Get the exponential of a numeric value  (SStL)
    */

    public static double exp (double x) {
        return java.lang.Math.exp(x);
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
// Portions marked SStL were provided by Simon St.Laurent [simonstl@simonstl.com]. All Rights Reserved.
//
// Contributor(s): none.
//
