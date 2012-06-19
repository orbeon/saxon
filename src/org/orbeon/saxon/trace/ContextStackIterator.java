package org.orbeon.saxon.trace;

import org.orbeon.saxon.expr.UserFunctionCall;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.instruct.*;
import org.orbeon.saxon.om.StandardNames;

import java.util.Iterator;

/**
 * This class provides a representation of the current runtime call stack, as represented by the stack
 * of XPathContext objects.
 */
public class ContextStackIterator implements Iterator {

    private boolean first = true;
    private XPathContext next;

    /**
     * Create an iterator over the stack of XPath dynamic context objects, starting with the top-most
     * stackframe and working down. The objects returned by this iterator will be of class {@link ContextStackFrame}
     * @param context the current context
     */

    public ContextStackIterator(XPathContext context) {
        next = context;
    }

    /**
     * Returns <tt>true</tt> if the iteration has more elements. (In other
     * words, returns <tt>true</tt> if <tt>next</tt> would return an element
     * rather than throwing an exception.)
     *
     * @return <tt>true</tt> if the iterator has more elements.
     */
    public boolean hasNext() {
        return next != null;
    }

    /**
     * Returns the next element in the iteration.  Calling this method
     * repeatedly until the {@link #hasNext()} method returns false will
     * return each element in the underlying collection exactly once.
     *
     * @return the next element in the iteration, which will always be an instance
     * of {@link ContextStackFrame}
     * @throws java.util.NoSuchElementException
     *          iteration has no more elements.
     */
    public Object next() {
        XPathContext context = next;
        if (context == null) {
            return null;
        }
        int construct = context.getOriginatingConstructType();
        Object origin = context.getOrigin();

        if (first) {
            // these constructs are only considered if they appear at the top of the stack
            if (construct == Location.FILTER_EXPRESSION ||
                    construct == Location.PATH_EXPRESSION ||
                    construct == Location.SORT_KEY ||
                    construct == Location.GROUPING_KEY) {

            } else {

            }
        }

        if (construct == Location.CONTROLLER) {
            next = context.getCaller();
            return new ContextStackFrame.CallingApplication();
        } else if (construct == Location.BUILT_IN_TEMPLATE) {
            next = context.getCaller();
            return new ContextStackFrame.BuiltInTemplateRule();
        }
//        InstructionInfo info;
//        if (origin instanceof Instruction) {
//            info = ((Instruction)origin);
//        } else {
//            next = context.getCaller();
//            return next();
//        }
        //System.err.println("Construct: " + construct);
        if (construct == Location.FUNCTION_CALL) {
            ContextStackFrame.FunctionCall sf = new ContextStackFrame.FunctionCall();
            UserFunctionCall ufc = (UserFunctionCall)origin;
            sf.setSystemId(ufc.getSystemId());
            sf.setLineNumber(ufc.getLineNumber());
            sf.setContainer(ufc.getContainer());
            sf.setFunctionName(ufc.getFunctionName());
            sf.setContextItem(context.getContextItem());
            next = context.getCaller();
            return sf;
        } else if (construct == StandardNames.XSL_APPLY_TEMPLATES) {
            ContextStackFrame.ApplyTemplates sf = new ContextStackFrame.ApplyTemplates();
            ApplyTemplates loc = (ApplyTemplates)origin;
            sf.setSystemId(loc.getSystemId());
            sf.setLineNumber(loc.getLineNumber());
            sf.setContainer(loc.getContainer());
            sf.setContextItem(context.getContextItem());
            next = context.getCaller();
            return sf;
        } else if (construct == StandardNames.XSL_CALL_TEMPLATE) {
            ContextStackFrame.CallTemplate sf = new ContextStackFrame.CallTemplate();
            CallTemplate loc = (CallTemplate)origin;
            sf.setSystemId(loc.getSystemId());
            sf.setLineNumber(loc.getLineNumber());
            sf.setContainer(loc.getContainer());
            sf.setTemplateName(loc.getObjectName());
            sf.setContextItem(context.getContextItem());
            next = context.getCaller();
            return sf;
        } else if (construct == StandardNames.XSL_VARIABLE) {
            ContextStackFrame.VariableEvaluation sf = new ContextStackFrame.VariableEvaluation();
            GeneralVariable var = ((GeneralVariable)origin);
            sf.setSystemId(var.getSystemId());
            sf.setLineNumber(var.getLineNumber());
            sf.setContainer(var.getContainer());
            sf.setContextItem(context.getContextItem());
            sf.setVariableName(var.getVariableQName());
            next = context.getCaller();
            return sf;
        } else if (construct == StandardNames.XSL_FOR_EACH) {
            ContextStackFrame.ForEach sf = new ContextStackFrame.ForEach();
            ForEach var = ((ForEach)origin);
            sf.setSystemId(var.getSystemId());
            sf.setLineNumber(var.getLineNumber());
            sf.setContainer(var.getContainer());
            sf.setContextItem(context.getContextItem());
            next = context.getCaller();
            return sf;
//        } else if (construct == Location.FILTER_EXPRESSION) {
//            out.println("    In predicate of filter expression");
//        } else if (construct == Location.PATH_EXPRESSION) {
//            out.println("    In step of path expression");
//        } else if (construct == Location.SORT_KEY) {
//            out.println("    In evaluation of sort key");
//        } else if (construct == Location.GROUPING_KEY) {
//            out.println("    In evaluation of grouping key");
        } else {
            //other context changes are not considered significant enough to report
            //out.println("    In unidentified location " + construct);
            next = context.getCaller();
            return next();
        }

    }

    /**
     * Removes from the underlying collection the last element returned by the
     * iterator (optional operation).
     *
     * @throws UnsupportedOperationException as the <tt>remove</tt>
     *                                       operation is not supported by this Iterator.
     */
    public void remove() {
        throw new UnsupportedOperationException();
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

