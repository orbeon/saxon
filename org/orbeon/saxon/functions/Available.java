package net.sf.saxon.functions;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.*;
import net.sf.saxon.style.StyleNodeFactory;
import net.sf.saxon.style.XSLTStaticContext;
import net.sf.saxon.trans.StaticError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.*;

/**
* This class supports the XSLT element-available and function-available functions.
*/

public class Available extends SystemFunction implements XSLTFunction {

    public static final int ELEMENT_AVAILABLE = 0;
    public static final int FUNCTION_AVAILABLE = 1;

    private transient NamespaceResolver nsContext;
    private transient StyleNodeFactory styleNodeFactory;
    private transient boolean checked = false;
        // the second time checkArguments is called, it's a global check so the static context is inaccurate

    public void checkArguments(StaticContext env) throws XPathException {
        if (checked) return;
        checked = true;
        super.checkArguments(env);
        if (!(argument[0] instanceof Value &&
                (argument.length==1 || argument[1] instanceof Value))) {
            // we need to save the namespace context
            nsContext = env.getNamespaceResolver();
        }
    }

    /**
    * preEvaluate: this method uses the static context to do early evaluation of the function
    * if the argument is known
    */

    public Expression preEvaluate(StaticContext env) throws XPathException {
        String qname = ((StringValue)argument[0]).getStringValue();

        boolean b = false;
        switch(operation) {
            case ELEMENT_AVAILABLE:
                b = ((XSLTStaticContext)env).isElementAvailable(qname);
                break;
            case FUNCTION_AVAILABLE:
                long arity = -1;
                if (argument.length == 2) {
                    arity = ((NumericValue)argument[1].evaluateItem(null)).longValue();
                }
                //b = ((ExpressionContext)env).isFunctionAvailable(qname, arity);
                try {
                    String[] parts = Name.getQNameParts(qname);
                    String prefix = parts[0];
                    String uri;
                    if (prefix.equals("")) {
                        uri = env.getDefaultFunctionNamespace();
                    } else {
                        uri = env.getURIForPrefix(prefix);
                    }
                    int fingerprint = env.getNamePool().allocate(prefix, uri, parts[1]) & 0xfffff;
                    b = env.getFunctionLibrary().isAvailable(fingerprint, uri, parts[1], (int)arity);
                } catch (QNameException e) {
                    throw new StaticError(e.getMessage());
                }
                break;
        }
        return BooleanValue.get(b);
    }

    /**
    * Run-time evaluation. This is the only thing in the spec that requires information
     * about in-scope functions to be available at run-time. However, we keep it because
     * it's handy for some other things such as saxon:evaluate().
    */

    public Item evaluateItem(XPathContext context) throws XPathException {
        AtomicValue av1 = (AtomicValue)argument[0].evaluateItem(context);
        long arity = -1;
        if (argument.length == 2) {
            arity = ((NumericValue)argument[1].evaluateItem(context)).longValue();
        }
        StringValue nameValue = (StringValue)av1.getPrimitiveValue();
        String name = nameValue.getStringValue();
        String[] parts = null;
        try {
            parts = Name.getQNameParts(name);
        } catch (QNameException e) {
            String code = (operation == FUNCTION_AVAILABLE ? "XT1400" : "XT1440");
            dynamicError(e.getMessage(), code, context);
        }
        String prefix = parts[0];
        String lname = parts[1];
        String uri;
        if (prefix.equals("")) {
            if (operation==ELEMENT_AVAILABLE) {
                // Use the default namespace for ELEMENT_AVAILABLE only
                uri = nsContext.getURIForPrefix(prefix, true);
            } else {
        	    uri = NamespaceConstant.FN;
        	}
        } else {
        	uri = nsContext.getURIForPrefix(prefix, false);
        }
        if (uri==null) {
            dynamicError("Namespace prefix '" + prefix + "' has not been declared", context);
        }

        boolean b = false;
        switch(operation) {
            case ELEMENT_AVAILABLE:
                b = isElementAvailable(uri, lname, context);
                break;
            case FUNCTION_AVAILABLE:
                final int fingerprint = context.getController().getNamePool().allocate(prefix, uri, lname) & 0xfffff;
                final FunctionLibrary lib = context.getController().getExecutable().getFunctionLibrary();
                b = lib.isAvailable(fingerprint, uri, lname, (int)arity);
                break;
        }
        return BooleanValue.get(b);

    }

    /**
    * Determine at run-time whether a function is available. Returns true only in the case
    * of standard functions, EXSLT and Saxon functions, and Java extension functions found
    * in a class that's implicit from the URI. Currently returns false in the case of
    * stylesheet functions (xsl:function) and Java classes identified using saxon:script
    */

//    private static boolean isFunctionAvailable(String uri, String localname, int arity, XPathContext context) {
//        try {
//            if (uri.equals("") || uri.equals(NamespaceConstant.FN)) {
//                StandardFunction.Entry entry =
//                        StandardFunction.getFunction(localname);
//                if (entry == null) {
//                    return false;
//                }
//                return (arity == -1 ||
//                        (arity >= entry.minArguments && arity <= entry.maxArguments));
//            } else {
//                FunctionLibrary proxy = context.getController().getConfiguration().getExtensionBinder();
//                return proxy.isAvailable(uri, localname, arity);
//            }
//        } catch (Exception err) {
//            return false;
//        }
//    }

    /**
    * Determine at run-time whether a particular instruction is available. Returns true
    * only in the case of XSLT instructions and Saxon extension instructions; returns false
    * for user-defined extension instructions
    */

    private boolean isElementAvailable(String uri, String localname, XPathContext context) {

        // This is horribly inefficient. But hopefully it's hardly ever executed, because there
        // is very little point calling element-available() with a dynamically-constructed argument.
        // And the inefficiency is only incurred once, on the first call.

        // Note: this requires the compile-time classes to be available at run-time; it will need
        // changing if we ever want to build a run-time JAR file.

        try {
            if (styleNodeFactory==null) {
                NamePool instructionNamePool = new NamePool();
                styleNodeFactory =
                    new StyleNodeFactory (
                            instructionNamePool,
                            context.getController().getConfiguration().isAllowExternalFunctions());
            }
            return styleNodeFactory.isElementAvailable(uri, localname);
        } catch (Exception err) {
            //err.printStackTrace();
            return false;
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
