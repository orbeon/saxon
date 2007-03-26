package org.orbeon.saxon.functions;
import org.orbeon.saxon.event.SequenceReceiver;
import org.orbeon.saxon.expr.StaticContext;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.instruct.ResultDocument;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.style.ExpressionContext;
import org.orbeon.saxon.style.StandardNames;
import org.orbeon.saxon.trace.Location;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.StaticError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.StringValue;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.util.Properties;


/**
* This class implements the saxon:serialize() extension function,
* which is specially-recognized by the system because it needs access
* to parts of the static context
*/

public class Serialize extends SystemFunction implements XSLTFunction {

    Properties outputProperties;
    private transient boolean checked = false;
        // the second time checkArguments is called, it's a global check so the static context is inaccurate

    /**
    * Method supplied by each class of function to check arguments during parsing, when all
    * the argument expressions have been read
    */

    public void checkArguments(StaticContext env) throws XPathException {
        if (checked) return;
        checked = true;
        super.checkArguments(env);
        if (argument[1] instanceof StringValue) {
            if (env instanceof ExpressionContext) {
                // We're in XSLT

                String format = ((StringValue)argument[1]).getStringValue();
                int fingerprint = -1;
                if (!format.equals("")) {
                    fingerprint = ((ExpressionContext)env).getFingerprint(format, false);
                    if (fingerprint==-1) {
                        throw new StaticError("Output format '" + format + "' has not been defined");
                    }
                }
                outputProperties = ((ExpressionContext)env).getXSLStylesheet().gatherOutputProperties(fingerprint);
            } else {
                // we're not in XSLT: treat the second argument as the method property, default the rest
                // See https://sourceforge.net/forum/message.php?msg_id=3780729
                outputProperties = new Properties();
                outputProperties.setProperty(OutputKeys.METHOD, ((StringValue)argument[1]).getStringValue());
            }
        }
    }

    /**
    * Evaluate the function
    */

    public Item evaluateItem(XPathContext c) throws XPathException {
        NodeInfo node = (NodeInfo)argument[0].evaluateItem(c);
        if (node==null) {
            return StringValue.EMPTY_STRING;
        }
        Properties props = outputProperties;
        if (props == null) {
            // the second argument was not a literal string: in this case it must be an xsl:output element
            Item secondArg = argument[1].evaluateItem(c);
            if (!(secondArg instanceof NodeInfo &&
                    ((NodeInfo)secondArg).getNodeKind() == Type.ELEMENT &&
                    ((NodeInfo)secondArg).getFingerprint() == StandardNames.XSL_OUTPUT)) {
                DynamicError err = new DynamicError("The second argument of saxon:serialize must either be " +
                        "a string literal, or an xsl:output element");
                err.setXPathContext(c);
                throw err;
            }
            props = processXslOutputElement((NodeInfo)secondArg, c);
        }

        try {
            StringWriter result = new StringWriter();
            XPathContext c2 = c.newMinorContext();
            c.setOriginatingConstructType(Location.SAXON_SERIALIZE);

            c2.changeOutputDestination(props,
                                               new StreamResult(result),
                                               false,
                                               getHostLanguage(),
                                               Validation.PRESERVE,
                                               null);
            SequenceReceiver out = c2.getReceiver();
            out.open();
            node.copy(out, NodeInfo.ALL_NAMESPACES, true, locationId);
            out.close();
            return new StringValue(result.toString());
        } catch (XPathException err) {
            throw new DynamicError(err);
        }
    }

    /**
     * Construct a set of output properties from an xsl:output element supplied at run-time
     * @param element an xsl:output element
     */

    private Properties processXslOutputElement(NodeInfo element, XPathContext c) throws XPathException {
        Properties props = new Properties();
		SequenceIterator iter = element.iterateAxis(Axis.ATTRIBUTE);
        NameChecker nc = c.getConfiguration().getNameChecker();
        NamespaceResolver resolver = new InscopeNamespaceResolver(element);
        while (true) {
            NodeInfo att = (NodeInfo)iter.next();
            if (att == null) {
                break;
            }
            String uri = att.getURI();
            String local = att.getLocalPart();
            String val = att.getStringValue().trim();
            ResultDocument.setSerializationProperty(props, uri, local, val, resolver, false, nc);
            // TODO: unrecognized attributes are currently ignored
        }
        return props;
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
