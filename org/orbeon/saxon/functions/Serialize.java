package org.orbeon.saxon.functions;
import org.orbeon.saxon.event.SequenceReceiver;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.instruct.ResultDocument;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.style.ExpressionContext;
import org.orbeon.saxon.trace.Location;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.StringValue;
import org.orbeon.saxon.value.Whitespace;

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

    public void checkArguments(ExpressionVisitor visitor) throws XPathException {
        if (checked) return;
        checked = true;
        super.checkArguments(visitor);
        if (argument[1] instanceof StringLiteral) {
            StaticContext env = visitor.getStaticContext();
            if (env instanceof ExpressionContext) {
                // We're in XSLT

                String formatString = ((StringLiteral)argument[1]).getStringValue();
                StructuredQName formatQName = null;
                if (formatString.length() != 0) {
                    formatQName = ((ExpressionContext)env).getStructuredQName(formatString, false);
//                    if (fingerprint==-1) {
//                        throw new XPathException("Output format '" + format + "' has not been defined");
//                    }
                }
                outputProperties = ((ExpressionContext)env).getXSLStylesheet().gatherOutputProperties(formatQName);
            } else {
                // we're not in XSLT: treat the second argument as the method property, default the rest
                // See https://sourceforge.net/forum/message.php?msg_id=3780729
                outputProperties = new Properties();
                outputProperties.setProperty(OutputKeys.METHOD, ((StringLiteral)argument[1]).getStringValue());
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
                XPathException err = new XPathException("The second argument of saxon:serialize must either be " +
                        "a string literal, or an xsl:output element");
                err.setXPathContext(c);
                throw err;
            }
            props = new Properties();
            processXslOutputElement((NodeInfo)secondArg, props, c);
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
            throw new XPathException(err);
        }
    }

    /**
     * Construct a set of output properties from an xsl:output element supplied at run-time
     * @param element an xsl:output element
     * @param props Properties object to which will be added the values of those serialization properties
     * that were specified
     * @param c the XPath dynamic context
     */

    public static void processXslOutputElement(NodeInfo element, Properties props, XPathContext c) throws XPathException {
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
            String val = Whitespace.trim(att.getStringValueCS());
            ResultDocument.setSerializationProperty(props, uri, local, val, resolver, false, nc);
        }
    }

    @Override
    public PathMap.PathMapNodeSet addToPathMap(PathMap pathMap, PathMap.PathMapNodeSet pathMapNodeSet) {

        // Mark the result as atomized to indicate that this expression depends on the string value of the result
        final PathMap.PathMapNodeSet result0 = argument[0].addToPathMap(pathMap, pathMapNodeSet);
        if (result0 != null)
            result0.setAtomized();

        // Don't forget the second argument
        // Also atomize in case the 2nd argument is interpreted as a string
        // NOTE: Could test if type is known statically and not Type.NODE, and atomize only in this case
        final PathMap.PathMapNodeSet result1 = argument[1].addToPathMap(pathMap, pathMapNodeSet);
        if (result1 != null)
            result1.setAtomized();

        // We are an atomic type
        return null;
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
