package net.sf.saxon.trans;

import net.sf.saxon.functions.FormatNumber2;

import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import javax.xml.transform.TransformerConfigurationException;
import java.io.Serializable;

/**
  * DecimalFormatManager manages the collection of named and unnamed decimal formats
  * @author Michael H. Kay
  */

public class DecimalFormatManager implements Serializable {

    private DecimalFormatSymbols defaultDFS;
    private HashMap formatTable;            // table for named decimal formats
    private boolean usingOriginalDefault = true;

    /**
    * create a DecimalFormatManager and initialise variables
    */

    public DecimalFormatManager() {
        formatTable = new HashMap();
        DecimalFormatSymbols d = new DecimalFormatSymbols();
        setDefaults(d);
        defaultDFS = d;
    }

    /**
    * Set up the XSLT-defined default attributes in a DecimalFormatSymbols
    */

    public static void setDefaults(DecimalFormatSymbols d) {
        d.setDecimalSeparator('.');
        d.setGroupingSeparator(',');
        d.setInfinity("Infinity");
        d.setMinusSign('-');
        d.setNaN("NaN");
        d.setPercent('%');
        d.setPerMill('\u2030');
        d.setZeroDigit('0');
        d.setDigit('#');
        d.setPatternSeparator(';');
    }

    /**
    * Register the default decimal-format.
    * Note that it is an error to register the same decimal-format twice, even with different
    * precedence
    */

    public void setDefaultDecimalFormat(DecimalFormatSymbols dfs)
    throws TransformerConfigurationException {
        if (!usingOriginalDefault) {
            if (!dfs.equals(defaultDFS)) {
                throw new TransformerConfigurationException(
                    "There are two conflicting definitions of the default decimal format");
            }
        }
        defaultDFS = dfs;
        usingOriginalDefault = false;
        setNamedDecimalFormat("", "", dfs);
            // this is to trigger fixup of calls
    }

    /**
    * Method called at the end of stylesheet compilation to fix up any format-number() calls
    * to the "default default" decimal format
    */

    public void fixupDefaultDefault() throws TransformerConfigurationException {
        if (usingOriginalDefault) {
            setNamedDecimalFormat("", "", defaultDFS);
        }
    }

    /**
    * Get the default decimal-format.
    */

    public DecimalFormatSymbols getDefaultDecimalFormat() {
        return defaultDFS;
    }

    /**
    * Set a named decimal format.
    * Note that it is an error to register the same decimal-format twice, even with different
    * precedence, unless the values are equal
    * @param uri The URI of the name of the decimal format
    * @param localName The local part of the name of the decimal format
    */

    public void setNamedDecimalFormat(String uri, String localName, DecimalFormatSymbols dfs)
    throws TransformerConfigurationException {
		String dfskey = localName + '#' + uri;
		Object o = formatTable.get(dfskey);
		if (o != null) {
    		if (o instanceof List) {
    		    // this indicates there are forwards references to this decimal format that need to be fixed up
    		    for (Iterator iter = ((List)o).iterator(); iter.hasNext(); ) {
    		        FormatNumber2 call = (FormatNumber2)iter.next();
    		        call.fixup(dfs);
    		    }
    		} else {
            	DecimalFormatSymbols old = (DecimalFormatSymbols)o;
                if (!dfs.equals(old)) {
                    throw new TransformerConfigurationException("Duplicate declaration of decimal-format");
                }
            }
        }
        formatTable.put(dfskey, dfs);
    }

    /**
    * Register a format-number() function call that uses a particular decimal format. This
    * allows early compile time resolution to a DecimalFormatSymbols object where possible,
    * even in the case of a forwards reference
    */

    public void registerUsage(String uri, String localName, FormatNumber2 call) {
        String dfskey = localName + '#' + uri;
        Object o = formatTable.get(dfskey);
        if (o == null) {
            // it's a forwards reference
            List list = new ArrayList();
            list.add(call);
            formatTable.put(dfskey, list);
        } else if (o instanceof List) {
            // it's another forwards reference
            List list = (List)o;
            list.add(call);
        } else {
            // it's a backwards reference
            call.fixup((DecimalFormatSymbols)o);
        }
    }

    /**
    * Get a named decimal-format registered using setNamedDecimalFormat
    * @param uri The URI of the name of the decimal format
    * @param localName The local part of the name of the decimal format
    * @return the DecimalFormatSymbols object corresponding to the named locale, if any
    * or null if not set.
    */

    public DecimalFormatSymbols getNamedDecimalFormat(String uri, String localName) {
		String dfskey = localName + '#' + uri;
        return (DecimalFormatSymbols)formatTable.get(dfskey);
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
