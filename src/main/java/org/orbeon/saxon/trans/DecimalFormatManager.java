package org.orbeon.saxon.trans;

import org.orbeon.saxon.functions.FormatNumber;
import org.orbeon.saxon.om.StructuredQName;
import org.orbeon.saxon.om.NamespaceConstant;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
  * DecimalFormatManager manages the collection of named and unnamed decimal formats
  * @author Michael H. Kay
  */

public class DecimalFormatManager implements Serializable {

    private DecimalSymbols defaultDFS;
    private HashMap formatTable;            // table for named decimal formats
    private boolean usingOriginalDefault = true;

    /**
    * create a DecimalFormatManager and initialise variables
    */

    public DecimalFormatManager() {
        formatTable = new HashMap(10);
        DecimalSymbols d = new DecimalSymbols();
        setDefaults(d);
        defaultDFS = d;
    }

    /**
    * Set up the XSLT-defined default attributes in a DecimalFormatSymbols
    */

    public static void setDefaults(DecimalSymbols d) {
        d.decimalSeparator = ('.');
        d.groupingSeparator = (',');
        d.infinity = ("Infinity");
        d.minusSign = ('-');
        d.NaN = ("NaN");
        d.percent = ('%');
        d.permill = ('\u2030');
        d.zeroDigit = ('0');
        d.digit = ('#');
        d.patternSeparator = (';');
    }

    /**
    * Register the default decimal-format.
    * Note that it is an error to register the same decimal-format twice, even with different
    * precedence
    */

    public void setDefaultDecimalFormat(DecimalSymbols dfs, int precedence)
    throws XPathException {
        if (!usingOriginalDefault) {
            if (!dfs.equals(defaultDFS)) {
                XPathException err = new XPathException("There are two conflicting definitions of the default decimal format");
                err.setErrorCode("XTSE1290");
                err.setIsStaticError(true);
                throw err;
            }
        }
        defaultDFS = dfs;
        usingOriginalDefault = false;
        setNamedDecimalFormat(DEFAULT_NAME, dfs, precedence);
            // this is to trigger fixup of calls
    }

    final public static StructuredQName DEFAULT_NAME = 
            new StructuredQName("saxon", NamespaceConstant.SAXON, "default-decimal-format");

    /**
    * Method called at the end of stylesheet compilation to fix up any format-number() calls
    * to the "default default" decimal format
    */

    public void fixupDefaultDefault() throws XPathException {
        if (usingOriginalDefault) {
            setNamedDecimalFormat(DEFAULT_NAME, defaultDFS, -1000);
        }
    }

    /**
    * Get the default decimal-format.
    */

    public DecimalSymbols getDefaultDecimalFormat() {
        return defaultDFS;
    }

    /**
    * Set a named decimal format.
    * Note that it is an error to register the same decimal-format twice, unless hte values are
     * equal, or unless there is another of higher precedence. This method assumes that decimal-formats
     * are registered in order of decreasing precedence
    * @param qName the name of the decimal format
    */

    public void setNamedDecimalFormat(StructuredQName qName, DecimalSymbols dfs, int precedence)
    throws XPathException {
		Object o = formatTable.get(qName);
		if (o != null) {
    		if (o instanceof List) {
    		    // this indicates there are forwards references to this decimal format that need to be fixed up
    		    for (Iterator iter = ((List)o).iterator(); iter.hasNext(); ) {
    		        FormatNumber call = (FormatNumber)iter.next();
    		        call.fixup(dfs);
    		    }
    		} else {
                DecimalFormatInfo info = (DecimalFormatInfo)o;
            	DecimalSymbols old = info.dfs;
                int oldPrecedence = info.precedence;
                if (precedence < oldPrecedence) {
                    return;
                }
                if (precedence==oldPrecedence && !dfs.equals(old)) {
                    XPathException err = new XPathException("There are two conflicting definitions of the named decimal-format");
                    err.setErrorCode("XTSE1290");
                    err.setIsStaticError(true);
                    throw err;
                }
            }
        }
        DecimalFormatInfo dfi = new DecimalFormatInfo();
        dfi.dfs = dfs;
        dfi.precedence = precedence;
        formatTable.put(qName, dfi);
    }

    /**
    * Register a format-number() function call that uses a particular decimal format. This
    * allows early compile time resolution to a DecimalFormatSymbols object where possible,
    * even in the case of a forwards reference
    */

    public void registerUsage(StructuredQName qName, FormatNumber call) {
        Object o = formatTable.get(qName);
        if (o == null) {
            // it's a forwards reference
            List list = new ArrayList(10);
            list.add(call);
            formatTable.put(qName, list);
        } else if (o instanceof List) {
            // it's another forwards reference
            List list = (List)o;
            list.add(call);
        } else {
            // it's a backwards reference
            DecimalFormatInfo dfi = (DecimalFormatInfo)o;
            call.fixup(dfi.dfs);
        }
    }

    /**
    * Get a named decimal-format registered using setNamedDecimalFormat
    * @param qName The  name of the decimal format
    * @return the DecimalFormatSymbols object corresponding to the named locale, if any
    * or null if not set.
    */

    public DecimalSymbols getNamedDecimalFormat(StructuredQName qName) {
        DecimalFormatInfo dfi = ((DecimalFormatInfo)formatTable.get(qName));
        if (dfi == null) {
            return null;
        }
        return dfi.dfs;
    }

    private static class DecimalFormatInfo implements Serializable {
        public DecimalSymbols dfs;
        public int precedence;
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
