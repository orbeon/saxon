package net.sf.saxon.instruct;
import net.sf.saxon.Controller;
import net.sf.saxon.ParameterSet;
import net.sf.saxon.style.StandardNames;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.trans.Mode;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.XPathContextMajor;

import net.sf.saxon.xpath.XPathException;
import net.sf.saxon.xpath.DynamicError;

/**
* An xsl:next-match element in the stylesheet
*/

public class NextMatch extends ApplyImports {

    public NextMatch() {
    }

    /**
    * Get the name of this instruction for diagnostic and tracing purposes
    */

    public int getInstructionNameCode() {
        return StandardNames.XSL_NEXT_MATCH;
    }

    public TailCall processLeavingTail(XPathContext context) throws XPathException {

        Controller controller = context.getController();

        // handle parameters if any

        ParameterSet params = assembleParams(context, actualParams);
        ParameterSet tunnels = assembleTunnelParams(context, tunnelParams);

        Template currentTemplate = context.getCurrentTemplate();
        if (currentTemplate==null) {
            DynamicError e = new DynamicError("There is no current template rule");
            e.setXPathContext(context);
            e.setErrorCode("XT0560");
            throw e;
        }
        Mode mode = context.getCurrentMode();
        if (context.getCurrentIterator()==null) {
            DynamicError e = new DynamicError("There is no context item");
            e.setXPathContext(context);
            e.setErrorCode("XT0565");
            throw e;
        }
        Item currentItem = context.getCurrentIterator().current();
        if (!(currentItem instanceof NodeInfo)) {
            DynamicError e = new DynamicError("Cannot call xsl:next-match when context item is not a node");
            e.setXPathContext(context);
            e.setErrorCode("XT0565");
            throw e;
        }
        NodeInfo node = (NodeInfo)currentItem;
        Template nh = controller.getRuleManager().getNextMatchHandler(node, mode, currentTemplate, context);

		if (nh==null) {             // use the default action for the node
            ApplyTemplates.defaultAction(node, params, tunnels, context);
        } else {
            XPathContextMajor c2 = context.newContext();
            c2.setOrigin(this);
            c2.openStackFrame(nh.getStackFrameMap());
            c2.setLocalParameters(params);
            c2.setTunnelParameters(tunnels);
            nh.process(c2);
        }
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
