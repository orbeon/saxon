package net.sf.saxon.instruct;
import net.sf.saxon.expr.Expression;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.XPathContextMajor;
import net.sf.saxon.style.StandardNames;
import net.sf.saxon.trace.InstructionInfo;
import net.sf.saxon.trace.InstructionInfoProvider;
import net.sf.saxon.trans.XPathException;

/**
* An xsl:template element in the style sheet.
*/

public class Template extends Procedure implements InstructionInfoProvider {

    // The body of the template is represented by an expression,
    // which is responsible for any type checking that's needed.

    private int precedence;
    private int minImportPrecedence;
    private int templateFingerprint;
    private transient InstructionDetails details;

    public Template () { }

    public void init (  int templateFingerprint,
                        int precedence,
                        int minImportPrecedence) {
        this.templateFingerprint = templateFingerprint;
        this.precedence = precedence;
        this.minImportPrecedence = minImportPrecedence;
    }

    /**
     * Get the namepool fingerprint of the name of the template (if it is named)
     * @return the fingerprint of the template name, or -1 if unnamed
     */

    public int getFingerprint() {
        return templateFingerprint;
    }

    public int getPrecedence() {
        return precedence;
    }

    public int getMinImportPrecedence() {
        return minImportPrecedence;
    }

    public boolean needsStackFrame() {
        return getStackFrameMap().getNumberOfVariables() > 0;
    }

    /**
    * Process the template, without returning any tail calls
    * @param context The dynamic context, giving access to the current node,
    * the current variables, etc.
    */

    public void process(XPathContext context) throws XPathException {
        TailCall tc = processLeavingTail(context);
        while (tc != null) {
            tc = tc.processLeavingTail(context);
        }
    }

    /**
    * Process this template, with the possibility of returning a tail call package if the template
     * contains any tail calls that are to be performed by the caller.
    */

    public TailCall processLeavingTail(XPathContext context) throws XPathException {
        if (getBody()==null) {
            // fast path for an empty template
            return null;
        }
        XPathContextMajor c2 = context.newContext();
        c2.setOrigin(this);
        c2.setCurrentTemplate(this);

        TailCall tc = expand(c2);
        return tc;
    }

    /**
    * Expand the template. Called when the template is invoked using xsl:call-template.
    * Invoking a template by this method does not change the current template.
    */

    public TailCall expand(XPathContext context) throws XPathException {
        Expression body = getBody();
        if (body==null) {
            // fast path for an empty template
            return null;
        }
        if (body instanceof Instruction) {
            return ((Instruction)body).processLeavingTail(context);
        } else {
            body.process(context);
            return null;
        }
    }

    /**
     * Get the InstructionInfo details about the construct. This information isn't used for tracing,
     * but it is available when inspecting the context stack.
     */

    public InstructionInfo getInstructionInfo() {
        if (details==null) {
            details = new InstructionDetails();
            details.setSystemId(getSystemId());
            details.setLineNumber(getLineNumber());
            details.setConstructType(StandardNames.XSL_TEMPLATE);
            details.setProperty("template", this);
        }
        return details;
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
// Contributor(s):
// Portions marked "e.g." are from Edwin Glaser (edwin@pannenleiter.de)
//
