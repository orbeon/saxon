package net.sf.saxon.functions;
import net.sf.saxon.event.SequenceReceiver;
import net.sf.saxon.expr.StaticContext;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.Validation;
import net.sf.saxon.style.ExpressionContext;
import net.sf.saxon.trace.Location;
import net.sf.saxon.trans.DynamicError;
import net.sf.saxon.trans.StaticError;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.StringValue;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerConfigurationException;
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
        if (env instanceof ExpressionContext) {
            // We're in XSLT

            // We require the output format name to be known at compile time. If we allowed it
            // to be dynamic, we would not only need to save the namespace context, but also to
            // save details of all xsl:output declarations for use at run-time.

            if (!(argument[1] instanceof StringValue)) {
                throw new StaticError("Second argument of saxon:serialize must be known at compile time");
            }
            String format = ((StringValue)argument[1]).getStringValue();
            int fingerprint = -1;
            if (!format.equals("")) {
                fingerprint = ((ExpressionContext)env).getFingerprint(format, false);
                if (fingerprint==-1) {
                    throw new StaticError("Output format '" + format + "' has not been defined");
                }
            } try {
                outputProperties = ((ExpressionContext)env).getXSLStylesheet().gatherOutputProperties(fingerprint);
            } catch (TransformerConfigurationException err) {
                throw new StaticError(err);
            }
        } else {
            // we're not in XSLT: treat the second argument as the method property, default the rest
            outputProperties = new Properties();
            if (!(argument[1] instanceof StringValue)) {
                throw new StaticError("Second argument of saxon:serialize must be known at compile time");
            }
            outputProperties.setProperty(OutputKeys.METHOD, ((StringValue)argument[1]).getStringValue());
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

        try {
            StringWriter result = new StringWriter();
            XPathContext c2 = c.newMinorContext();
            c.setOriginatingConstructType(Location.SAXON_SERIALIZE);

            c2.changeOutputDestination(outputProperties,
                                               new StreamResult(result),
                                               false,
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
