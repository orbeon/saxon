package org.orbeon.saxon.sxpath;

import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.expr.XPathContextMajor;
import org.orbeon.saxon.expr.TypeChecker;
import org.orbeon.saxon.instruct.SlotManager;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.trans.SaxonErrorCode;
import org.orbeon.saxon.value.SequenceType;
import org.orbeon.saxon.value.Value;

import javax.xml.transform.Source;

/**
 * This object represents the dynamic XPath execution context for use in the free-standing Saxon XPath API.
 * The dynamic context holds the context item and the values of external variables used by the XPath expression.
 *
 * <p>This object is always created via the method
 * {@link org.orbeon.saxon.sxpath.XPathExpression#createDynamicContext(org.orbeon.saxon.om.Item)}</p>
 */
public class XPathDynamicContext {

    private XPathContextMajor contextObject;
    private SlotManager stackFrameMap;

    // ORBEON: made public
    public XPathDynamicContext(XPathContextMajor contextObject, SlotManager stackFrameMap) {
        this.contextObject = contextObject;
        this.stackFrameMap = stackFrameMap;
    }

    /**
     * Set the context item to a node derived from a supplied Source object. This may be
     * any implementation of the Source interface recognized by Saxon. Note that the
     * Saxon {@link NodeInfo} interface, representing a node in a tree, is one such
     * implementation; others include {@link javax.xml.transform.stream.StreamSource},
     * {@link javax.xml.transform.sax.SAXSource}, and {@link javax.xml.transform.dom.DOMSource}
     *
     * @param source The source object representing the node that will be used as the context item
     * @throws XPathException if a failure occurs reading or parsing a Source object to build an input tree,
     * or if the source is a document that was built under the wrong configuration
     */

    public void setContextNode(Source source) throws XPathException {
        NodeInfo origin;
        if (source instanceof NodeInfo) {
            origin = (NodeInfo)source;
            if (!origin.getConfiguration().isCompatible(contextObject.getConfiguration())) {
                throw new XPathException(
                        "Supplied node must be built using the same or a compatible Configuration",
                        SaxonErrorCode.SXXP0004);
            }
        } else {
            origin = contextObject.getConfiguration().buildDocument(source);
        }
        setContextItem(origin);
    }

    /**
     * Set the context item for evaluation of the XPath Expression
     * @param item the context item
     * @throws XPathException if the node is in a document that was built under the wrong configuration
     */

    public void setContextItem(Item item) throws XPathException {
        if (item instanceof NodeInfo) {
            if (!((NodeInfo)item).getConfiguration().isCompatible(contextObject.getConfiguration())) {
                throw new XPathException(
                        "Supplied node must be built using the same or a compatible Configuration",
                        SaxonErrorCode.SXXP0004);
            }
        }
        UnfailingIterator iter = SingletonIterator.makeIterator(item);
        iter.next();
        contextObject.setCurrentIterator(iter);
    }

    /**
     * Get the context item
     * @return the context item if there is one, or null otherwise
     */

    public Item getContextItem() {
        return contextObject.getContextItem();
    }

    /**
     * Set the value of an external variable used within the XPath expression
     * @param variable the object representing the variable, as returned by the
     * {@link org.orbeon.saxon.sxpath.XPathEvaluator#declareVariable(String, String)} method.
     * Note that setting the value of a variable does not modify the {@link XPathVariable}
     * object itself, which means that this method is thread-safe.
     * @param value The value of the variable.
     * @throws XPathException if the supplied value does not conform to the required type of the
     * variable; or if the supplied value contains a node that does not belong to this Configuration
     * (or another Configuration that shares the same namePool)
     */

    public void setVariable(XPathVariable variable, ValueRepresentation value) throws XPathException {
        SequenceType requiredType = variable.getRequiredType();
        if (requiredType != SequenceType.ANY_SEQUENCE) {
            XPathException err = TypeChecker.testConformance(value, requiredType, contextObject);
            if (err != null) {
                throw err;
            }
        }
        SequenceIterator iter = Value.asIterator(value);
        while (true) {
            Item item = iter.next();
            if (item == null) {
                break;
            }
            if (item instanceof NodeInfo) {
                if (!((NodeInfo)item).getConfiguration().isCompatible(contextObject.getConfiguration())) {
                    throw new XPathException(
                            "Supplied node must be built using the same or a compatible Configuration",
                            SaxonErrorCode.SXXP0004);
                }
            }
        }
        int slot = variable.getLocalSlotNumber();
        StructuredQName expectedName = (slot >= stackFrameMap.getNumberOfVariables() ? null :
                (StructuredQName)stackFrameMap.getVariableMap().get(slot));
        if (!variable.getVariableQName().equals(expectedName)) {
            throw new XPathException(
                    "Supplied XPathVariable is bound to the wrong slot: perhaps it was created using a different static context"); 
        }
        contextObject.setLocalVariable(slot, value);
    }

    /**
     * For system use: get the wrapped XPathContext object
     * @return the underlying XPathContext object
     */

    public XPathContext getXPathContextObject() {
        return contextObject;
    }

    /**
     * Check that all external variables have been given a value
     * @param stackFrameMap describes the stack frame
     * @param numberOfExternals the number of variables that need to be supplied
     * @throws XPathException if required variables have not been given a value
     */

    protected void checkExternalVariables(SlotManager stackFrameMap, int numberOfExternals) throws XPathException {
        ValueRepresentation[] stack = contextObject.getStackFrame().getStackFrameValues();
        for (int i=0; i<numberOfExternals; i++) {
            if (stack[i] == null) {
                StructuredQName qname = (StructuredQName)stackFrameMap.getVariableMap().get(i);
                throw new XPathException("No value has been supplied for variable $" + qname.getDisplayName());
            }
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
// The Original Code is: all this file
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Contributor(s):
//

