package org.orbeon.saxon.functions;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.style.StyleNodeFactory;
import org.orbeon.saxon.style.XSLTStaticContext;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.BuiltInAtomicType;
import org.orbeon.saxon.type.SchemaType;
import org.orbeon.saxon.type.BuiltInListType;
import org.orbeon.saxon.value.AtomicValue;
import org.orbeon.saxon.value.BooleanValue;
import org.orbeon.saxon.value.NumericValue;
import org.orbeon.saxon.value.StringValue;

import java.util.Set;

/**
* This class supports the XSLT element-available and function-available functions.
*/

public class Available extends SystemFunction implements XSLTFunction {

    public static final int ELEMENT_AVAILABLE = 0;
    public static final int FUNCTION_AVAILABLE = 1;
    public static final int TYPE_AVAILABLE = 2;

    private NamespaceResolver nsContext;
    private transient StyleNodeFactory styleNodeFactory;
    private transient boolean checked = false;
    private Set importedSchemaNamespaces;



    public void checkArguments(ExpressionVisitor visitor) throws XPathException {
        // the second time checkArguments is called, it's a global check so the static context is inaccurate
        if (checked) {
            return;
        }
        checked = true;
        super.checkArguments(visitor);
        if (!(argument[0] instanceof Literal &&
                (argument.length==1 || argument[1] instanceof Literal))) {
            // we need to save the namespace context
            nsContext = visitor.getStaticContext().getNamespaceResolver();
            // for type-available, we need to save the set of imported namespaces
            if (operation == TYPE_AVAILABLE) {
                importedSchemaNamespaces = visitor.getStaticContext().getImportedSchemaNamespaces();
            }
        }
    }

    /**
    * preEvaluate: this method uses the static context to do early evaluation of the function
    * if the argument is known (which is the normal case)
     * @param visitor the expression visitor
     */

    public Expression preEvaluate(ExpressionVisitor visitor) throws XPathException {
        String lexicalQName = ((Literal)argument[0]).getValue().getStringValue();
        StaticContext env = visitor.getStaticContext();
        boolean b = false;
        Configuration config = visitor.getConfiguration();
        switch(operation) {
            case ELEMENT_AVAILABLE:
                b = ((XSLTStaticContext)env).isElementAvailable(lexicalQName);
                break;
            case FUNCTION_AVAILABLE:
                long arity = -1;
                if (argument.length == 2) {
                    arity = ((NumericValue)argument[1].evaluateItem(env.makeEarlyEvaluationContext())).longValue();
                }
                try {
                    String[] parts = config.getNameChecker().getQNameParts(lexicalQName);
                    String prefix = parts[0];
                    String uri;
                    if (prefix.length() == 0) {
                        uri = env.getDefaultFunctionNamespace();
                    } else {
                        uri = env.getURIForPrefix(prefix);
                    }
                    StructuredQName functionName = new StructuredQName(prefix, uri, parts[1]);
                    b = env.getFunctionLibrary().isAvailable(functionName, (int)arity);
                } catch (QNameException e) {
                    XPathException err = new XPathException(e.getMessage());
                    err.setErrorCode("XTDE1400");
                    throw err;
                }
                break;
            case TYPE_AVAILABLE:
                try {
                    String[] parts = config.getNameChecker().getQNameParts(lexicalQName);
                    String prefix = parts[0];
                    String uri;
                    if (prefix.length() == 0) {
                        uri = env.getDefaultElementNamespace();
                    } else {
                        uri = env.getURIForPrefix(prefix);
                    }
                    if (uri.equals(NamespaceConstant.JAVA_TYPE)) {
                        try {
                            config.getClass(parts[1], false, null);
                            b = true;
                        } catch (XPathException err) {
                            b = false;
                        }
                    } else {
                        int fingerprint = config.getNamePool().allocate(prefix, uri, parts[1]) & 0xfffff;
                        SchemaType type = config.getSchemaType(fingerprint);
                        if (type instanceof BuiltInAtomicType) {
                            b = env.isAllowedBuiltInType((BuiltInAtomicType)type);
                        } else if (type instanceof BuiltInListType) {
                            b = config.isSchemaAware(Configuration.XSLT);
                        } else {
                            b = (type != null && (uri.equals(NamespaceConstant.SCHEMA) || env.isImportedSchema(uri)));
                        }
                    }
                } catch (QNameException e) {
                    XPathException err = new XPathException(e.getMessage());
                    err.setErrorCode("XTDE1425");
                    throw err;
                }
        }
        return Literal.makeLiteral(BooleanValue.get(b));
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
        StringValue nameValue = (StringValue)av1;
        String lexicalName = nameValue.getStringValue();
        StructuredQName qName;
        try {
            if (lexicalName.indexOf(':') < 0) {
                // we're in XSLT, where the default namespace for functions can't be changed
                String uri = (operation == FUNCTION_AVAILABLE
                        ? NamespaceConstant.FN
                        : nsContext.getURIForPrefix("", true));
                qName = new StructuredQName("", uri, lexicalName);
            } else {
                qName = StructuredQName.fromLexicalQName(lexicalName,
                    false,
                    context.getConfiguration().getNameChecker(),
                    nsContext);
            }
        } catch (XPathException e) {
            dynamicError(e.getMessage(), badQNameCode(), context);
            return null;
        }

        boolean b = false;
        switch(operation) {
            case ELEMENT_AVAILABLE:
                b = isElementAvailable(qName.getNamespaceURI(), qName.getLocalName(), context);
                break;
            case FUNCTION_AVAILABLE:
                final FunctionLibrary lib = context.getController().getExecutable().getFunctionLibrary();
                b = lib.isAvailable(qName, (int)arity);
                break;
            case TYPE_AVAILABLE:
                String uri = qName.getNamespaceURI();
                if (uri.equals(NamespaceConstant.SCHEMA) || importedSchemaNamespaces.contains(uri)) {
                    final int fp = context.getNamePool().allocate(
                            qName.getPrefix(), uri, qName.getLocalName()) & 0xfffff;
                    SchemaType type = context.getConfiguration().getSchemaType(fp);
                    b = (type != null);
                } else {
                    return BooleanValue.FALSE;
                }

        }
        return BooleanValue.get(b);

    }

    private String badQNameCode() {
        switch (operation) {
            case FUNCTION_AVAILABLE:
                return "XTDE1400";
            case TYPE_AVAILABLE:
                return "XTDE1428";
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
     * @param uri the namespace URI of the element
     * @param localname the local part of the element name
     * @param context the XPath evaluation context
     * @return true if the instruction is available, in the sense of the XSLT element-available() function
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
