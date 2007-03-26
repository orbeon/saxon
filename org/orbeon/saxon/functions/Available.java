package org.orbeon.saxon.functions;
import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.expr.StaticContext;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.style.StyleNodeFactory;
import org.orbeon.saxon.style.XSLTStaticContext;
import org.orbeon.saxon.style.ExpressionContext;
import org.orbeon.saxon.trans.StaticError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.value.*;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.type.SchemaType;
import org.orbeon.saxon.type.BuiltInAtomicType;
import org.orbeon.saxon.type.AtomicType;

/**
* This class supports the XSLT element-available and function-available functions.
*/

public class Available extends SystemFunction implements XSLTFunction {

    public static final int ELEMENT_AVAILABLE = 0;
    public static final int FUNCTION_AVAILABLE = 1;
    public static final int TYPE_AVAILABLE = 2;

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
    * if the argument is known (which is the normal case)
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
                    arity = ((NumericValue)argument[1].evaluateItem(env.makeEarlyEvaluationContext())).longValue();
                }
                try {
                    String[] parts = env.getConfiguration().getNameChecker().getQNameParts(qname);
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
                    StaticError err = new StaticError(e.getMessage());
                    err.setErrorCode("XTDE1400");
                    throw err;
                }
                break;
            case TYPE_AVAILABLE:
                try {
                    String[] parts = env.getConfiguration().getNameChecker().getQNameParts(qname);
                    String prefix = parts[0];
                    String uri;
                    if (prefix.equals("")) {
                        uri = env.getNamePool().getURIFromURICode(env.getDefaultElementNamespace());
                    } else {
                        uri = env.getURIForPrefix(prefix);
                    }
                    int fingerprint = env.getNamePool().allocate(prefix, uri, parts[1]) & 0xfffff;
                    SchemaType type = env.getConfiguration().getSchemaType(fingerprint);
                    if (type instanceof BuiltInAtomicType) {
                        b = ((ExpressionContext)env).isAllowedBuiltInType((AtomicType)type);
                    } else {
                        b = (type != null && env.isImportedSchema(uri));
                        // TODO: recognize extension types (in the java-type namespace)
                    }
                } catch (QNameException e) {
                    StaticError err = new StaticError(e.getMessage());
                    err.setErrorCode("XTDE1425");
                    throw err;
                }
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
            parts = context.getConfiguration().getNameChecker().getQNameParts(name);
        } catch (QNameException e) {
            String code = badQNameCode();
            dynamicError(e.getMessage(), code, context);
        }
        String prefix = parts[0];
        String lname = parts[1];
        String uri;
        if (prefix.equals("")) {
            if (operation == FUNCTION_AVAILABLE) {
        	    uri = NamespaceConstant.FN;
        	} else {
                // Use the default namespace for ELEMENT_AVAILABLE and TYPE_AVAILABLE
                uri = nsContext.getURIForPrefix(prefix, true);
            }
        } else {
        	uri = nsContext.getURIForPrefix(prefix, false);
        }
        if (uri==null) {
            String code = badQNameCode();
            dynamicError("Namespace prefix '" + prefix + "' has not been declared", code, context);
        }

        boolean b = false;
        switch(operation) {
            case ELEMENT_AVAILABLE:
                b = isElementAvailable(uri, lname, context);
                break;
            case FUNCTION_AVAILABLE:
                final int fingerprint = context.getNamePool().allocate(prefix, uri, lname) & 0xfffff;
                final FunctionLibrary lib = context.getController().getExecutable().getFunctionLibrary();
                b = lib.isAvailable(fingerprint, uri, lname, (int)arity);
                break;
            case TYPE_AVAILABLE:
                final int fp = context.getNamePool().allocate(prefix, uri, lname) & 0xfffff;
                SchemaType type = context.getConfiguration().getSchemaType(fp);
                b = (type != null);
                // TODO: tests whether the type exists in the Configuration, not necessarily in the static context

        }
        return BooleanValue.get(b);

    }

    private String badQNameCode() {
        switch (operation) {
            case FUNCTION_AVAILABLE:
                return "XTDE1400";
            case TYPE_AVAILABLE:
                return "XTDE1425";
            case ELEMENT_AVAILABLE:
                return "XTDE1440";
            default:
                return null;
        }
    }

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
                Configuration config = context.getConfiguration();
                styleNodeFactory = new StyleNodeFactory(config, context.getController().getErrorListener());
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
