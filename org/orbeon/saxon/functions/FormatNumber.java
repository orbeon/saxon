package net.sf.saxon.functions;
import net.sf.saxon.Controller;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.Name;
import net.sf.saxon.om.NamespaceResolver;
import net.sf.saxon.om.QNameException;
import net.sf.saxon.trans.DecimalFormatManager;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.StaticError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.AtomicValue;
import net.sf.saxon.value.NumericValue;
import net.sf.saxon.value.StringValue;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

/**
 * This is the original XSLT 1.0 implementation of format-number using the JDK
 * directly. It is still available for the time being under the name
 * format-number-1.0, in case the new version has problems.
 */

public class FormatNumber extends SystemFunction implements XSLTFunction {

    private DecimalFormat decimalFormat = new DecimalFormat();
    private String previousFormat = "[null]";
    private DecimalFormatSymbols previousDFS = null;

    private NamespaceResolver nsContext = null;
    private String dfURI = null;
    private String dfLocalName = null;
    private transient boolean checked = false;
        // the second time checkArguments is called, it's a global check so the static context is inaccurate


    public void checkArguments(StaticContext env) throws XPathException {
        if (checked) return;
        checked = true;
        super.checkArguments(env);
        if (argument.length==3 && argument[2] instanceof StringValue) {
            // common case, decimal format name is supplied as a string literal

            String qname = ((StringValue)argument[2]).getStringValue();
            if (!Name.isQName(qname)) {
                throw new StaticError("Decimal format name '" + qname + "' is not a valid QName");
            }
            try {
                String[] parts = Name.getQNameParts(qname);
                dfLocalName = parts[1];
                dfURI = env.getURIForPrefix(parts[0]);
            } catch (QNameException e) {
                throw new StaticError(e.getMessage());
            }
        } else {
            // we need to save the namespace context
            nsContext = env.getNamespaceResolver();
        }
    }

    /**
    * preEvaluate: this method suppresses compile-time evaluation by doing nothing.
    * We can't evaluate early because we don't have access to the DecimalFormatManager.
    */

    public Expression preEvaluate(StaticContext env) {return this;}

    /**
    * Evaluate in a context where a string is wanted
    */

    public String evaluateAsString(XPathContext context) throws XPathException {
        int numArgs = argument.length;
        Controller ctrl = context.getController();
        DecimalFormatManager dfm = ctrl.getDecimalFormatManager();
        DecimalFormatSymbols dfs;

        AtomicValue av0 = (AtomicValue)argument[0].evaluateItem(context);
        NumericValue number = (NumericValue)av0.getPrimitiveValue();
        String format = argument[1].evaluateItem(context).getStringValue();

        if (numArgs==2) {
            dfs = dfm.getDefaultDecimalFormat();
        } else {
            String uri = dfURI;
            String localName = dfLocalName;
            if (localName==null) {
                // the decimal-format name was given as a run-time expression
                String qname = argument[2].evaluateItem(context).getStringValue();
                if (!Name.isQName(qname)) {
                    DynamicError e = new DynamicError("Decimal format name '" + qname + "' is not a valid QName");
                    e.setXPathContext(context);
                    e.setErrorCode("XT1280");
                    throw e;
                }
                try {
                    String[] parts = Name.getQNameParts(qname);
                    localName = parts[1];
                    uri = nsContext.getURIForPrefix(parts[0], false);
                    if (uri==null) {
                        DynamicError e = new DynamicError("Namespace prefix '" + parts[0] + "' has not been defined");
                        e.setXPathContext(context);
                        e.setErrorCode("XT1280");
                        throw e;
                    }
                } catch (QNameException e) {
                    dynamicError("Invalid decimal format name. " + e.getMessage(), "XT1280", context);
                }
            }
            dfs = dfm.getNamedDecimalFormat(uri, localName);
            if (dfs==null) {
                DynamicError e = new DynamicError(
                    "format-number function: decimal-format '" + localName + "' is not defined");
                e.setXPathContext(context);
                e.setErrorCode("XT1280");
                throw e;
            }
        }
        return formatNumber(number.getDoubleValue(), format, dfs, context);
    }

    /**
    * Evaluate in a general context
    */

    public Item evaluateItem(XPathContext c) throws XPathException {
        return new StringValue(evaluateAsString(c));
    }

    /**
    * Here is the method that does the work. It needs to be synchronized because
    * it remembers information from one invocation to the next; it doesn't matter
    * if these are in different threads but it can't be interrupted. The reason for
    * remembering information is that getting a new DecimalFormatSymbols each time
    * is incredibly expensive, especially with the Microsoft Java VM.
    */

    public synchronized String
            formatNumber(double n, String format, DecimalFormatSymbols dfs, XPathContext context)
            throws XPathException {
        try {
            DecimalFormat df = decimalFormat;
            if (!(dfs==previousDFS && format.equals(previousFormat))) {
                df.setDecimalFormatSymbols(dfs);
                df.applyLocalizedPattern(format);
                previousDFS = dfs;
                previousFormat = format;
            }
            return df.format(n);
        } catch (Exception err) {
            DynamicError e = new DynamicError("Unable to interpret format pattern " + format + "(" + err + ")");
            e.setXPathContext(context);
            e.setErrorCode("XT1310");
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
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
