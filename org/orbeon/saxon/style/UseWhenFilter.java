package org.orbeon.saxon.style;

import org.orbeon.saxon.Controller;
import org.orbeon.saxon.event.ProxyReceiver;
import org.orbeon.saxon.event.StartTagBuffer;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.instruct.SlotManager;
import org.orbeon.saxon.om.NamespaceConstant;
import org.orbeon.saxon.trans.StaticError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.Type;

import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.Source;
import java.util.Stack;

/**
 * This is a filter inserted into the input pipeline for processing stylesheet modules, whose
 * task is to evaluate use-when expressions and discard those parts of the stylesheet module
 * for which the use-when attribute evaluates to false.
 */

public class UseWhenFilter extends ProxyReceiver {

    private StartTagBuffer startTag;
    private int useWhenCode;
    private int xslUseWhenCode;
    private int defaultNamespaceCode;
    private int depthOfHole = 0;
    private boolean emptyStylesheetElement = false;
    private Stack defaultNamespaceStack = new Stack();

    public UseWhenFilter(StartTagBuffer startTag) {
        this.startTag = startTag;
    }

    /**
     * Start of document
     */

    public void open() throws XPathException {
        useWhenCode = getNamePool().allocate("", "", "use-when") & 0xfffff;
        xslUseWhenCode = getNamePool().allocate("xsl", NamespaceConstant.XSLT, "use-when");
        defaultNamespaceCode = getNamePool().allocate("", "", "xpath-default-namespace");
        super.open();
    }

    /**
     * Notify the start of an element.
     *
     * @param nameCode    integer code identifying the name of the element within the name pool.
     * @param typeCode    integer code identifying the element's type within the name pool.
     * @param properties  bit-significant properties of the element node
     */

    public void startElement(int nameCode, int typeCode, int locationId, int properties) throws XPathException {
        defaultNamespaceStack.push(startTag.getAttribute(defaultNamespaceCode));
        if (emptyStylesheetElement) {
            depthOfHole = 1;
        }
        if (depthOfHole == 0) {
            String useWhen;
            if ((nameCode & 0xfffff) < 1024) {
                useWhen = startTag.getAttribute(useWhenCode);
            } else {
                useWhen = startTag.getAttribute(xslUseWhenCode);
            }
            if (useWhen != null) {
                try {
                    boolean b = evaluateUseWhen(useWhen, getDocumentLocator().getLineNumber(locationId));
                    if (!b) {
                        int fp = nameCode & 0xfffff;
                        if (fp == StandardNames.XSL_STYLESHEET || fp == StandardNames.XSL_TRANSFORM) {
                            emptyStylesheetElement = true;
                        } else {
                            depthOfHole = 1;
                            return;
                        }
                    }
                } catch (XPathException e) {
                    StaticError err = new StaticError("Error in use-when expression. " + e.getMessage());
                    ExpressionLocation loc = new ExpressionLocation();
                    loc.setSystemId(getDocumentLocator().getSystemId(locationId));
                    loc.setLineNumber(getDocumentLocator().getLineNumber(locationId));
                    err.setLocator(loc);
                    err.setErrorCode(e.getErrorCodeLocalPart());
                    try {
                        getPipelineConfiguration().getErrorListener().fatalError(err);
                    } catch (TransformerException tex) {
                        throw StaticError.makeStaticError(tex);
                    }
                    throw err;
                }
            }
        } else {
            depthOfHole++;
        }
        super.startElement(nameCode, typeCode, locationId, properties);
    }

    /**
     * Notify a namespace. Namespaces are notified <b>after</b> the startElement event, and before
     * any children for the element. The namespaces that are reported are only required
     * to include those that are different from the parent element; however, duplicates may be reported.
     * A namespace must not conflict with any namespaces already used for element or attribute names.
     *
     * @param namespaceCode an integer: the top half is a prefix code, the bottom half a URI code.
     *                      These may be translated into an actual prefix and URI using the name pool. A prefix code of
     *                      zero represents the empty prefix (that is, the default namespace). A URI code of zero represents
     *                      a URI of "", that is, a namespace undeclaration.
     * @throws IllegalStateException: attempt to output a namespace when there is no open element
     *                                start tag
     */

    public void namespace(int namespaceCode, int properties) throws XPathException {
        if (depthOfHole == 0) {
            super.namespace(namespaceCode, properties);
        }
    }

    /**
     * Notify an attribute. Attributes are notified after the startElement event, and before any
     * children. Namespaces and attributes may be intermingled.
     *
     * @param nameCode   The name of the attribute, as held in the name pool
     * @param typeCode   The type of the attribute, as held in the name pool
     * @param properties Bit significant value. The following bits are defined:
     *                   <dd>DISABLE_ESCAPING</dd>    <dt>Disable escaping for this attribute</dt>
     *                   <dd>NO_SPECIAL_CHARACTERS</dd>      <dt>Attribute value contains no special characters</dt>
     * @throws IllegalStateException: attempt to output an attribute when there is no open element
     *                                start tag
     */

    public void attribute(int nameCode, int typeCode, CharSequence value, int locationId, int properties) throws XPathException {
        if (depthOfHole == 0) {
            super.attribute(nameCode, typeCode, value, locationId, properties);
        }
    }

    /**
     * Notify the start of the content, that is, the completion of all attributes and namespaces.
     * Note that the initial receiver of output from XSLT instructions will not receive this event,
     * it has to detect it itself. Note that this event is reported for every element even if it has
     * no attributes, no namespaces, and no content.
     */


    public void startContent() throws XPathException {
        if (depthOfHole == 0) {
            super.startContent();
        }
    }

    /**
     * End of element
     */

    public void endElement() throws XPathException {
        defaultNamespaceStack.pop();
        if (depthOfHole > 0) {
            depthOfHole--;
        } else {
            super.endElement();
        }
    }

    /**
     * Character data
     */

    public void characters(CharSequence chars, int locationId, int properties) throws XPathException {
        if (depthOfHole == 0) {
            super.characters(chars, locationId, properties);
        }
    }

    /**
     * Processing Instruction
     */

    public void processingInstruction(String target, CharSequence data, int locationId, int properties) {
        // these are ignored in a stylesheet
    }

    /**
     * Output a comment
     */

    public void comment(CharSequence chars, int locationId, int properties) throws XPathException {
        // these are ignored in a stylesheet
    }

    /**
     * Evaluate a use-when attribute
     */

    public boolean evaluateUseWhen(String expression, int locationId) throws XPathException {
        UseWhenStaticContext staticContext = new UseWhenStaticContext(getConfiguration(), startTag);
        // The following is an approximation: it doesn't take account of xml:base attributes
        staticContext.setBaseURI(getDocumentLocator().getSystemId(locationId));
        for (int i=defaultNamespaceStack.size()-1; i>=0; i--) {
            String uri = (String)defaultNamespaceStack.get(i);
            if (uri != null) {
                short code = getNamePool().getCodeForURI(uri);
                staticContext.setDefaultElementNamespace(code);
                break;
            }
        }
        Expression expr = ExpressionTool.make(expression, staticContext,
                0, Token.EOF, getDocumentLocator().getLineNumber(locationId));
        ItemType contextItemType = Type.ITEM_TYPE;
        expr = expr.analyze(staticContext, contextItemType);
        SlotManager stackFrameMap = getPipelineConfiguration().getConfiguration().makeSlotManager();
        ExpressionTool.allocateSlots(expr, stackFrameMap.getNumberOfVariables(), stackFrameMap);
        Controller controller = new Controller(getConfiguration());
        controller.setURIResolver(new URIPreventer());
        XPathContext dynamicContext = controller.newXPathContext();
        dynamicContext = dynamicContext.newCleanContext();
        ((XPathContextMajor)dynamicContext).openStackFrame(stackFrameMap.getNumberOfVariables());
        return ExpressionTool.effectiveBooleanValue(expr.iterate(dynamicContext));
    }

    /**
     * Define a URIResolver that disallows all URIs
     */

    private static class URIPreventer implements URIResolver {
        /**
         * Called by the processor when it encounters
         * an xsl:include, xsl:import, or document() function.
         *
         * @param href An href attribute, which may be relative or absolute.
         * @param base The base URI against which the first argument will be made
         *             absolute if the absolute URI is required.
         * @return A Source object, or null if the href cannot be resolved,
         *         and the processor should try to resolve the URI itself.
         * @throws javax.xml.transform.TransformerException
         *          if an error occurs when trying to
         *          resolve the URI.
         */
        public Source resolve(String href, String base) throws TransformerException {
            throw new TransformerException("No external documents are available within an [xsl]use-when expression");
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

