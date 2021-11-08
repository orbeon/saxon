package org.orbeon.saxon.trace;

import org.orbeon.saxon.trans.Err;
import org.orbeon.saxon.expr.Container;
import org.orbeon.saxon.instruct.*;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.trans.KeyDefinition;

import java.io.PrintStream;

/**
 * An entry on the context stack. A new entry is created every time the context changes. This is a
 * representation of the stack created on request; it does not hold live data.
 */
public abstract class ContextStackFrame {

    private String moduleUri;
    private int lineNumber;
    private Container container;
    private Item contextItem;

    /**
     * Set the system ID representing the location of the instruction that caused this new context
     * to be created
     * @param uri the system ID (base URI/module URI) of the module containing the instruction
     */

    public void setSystemId(String uri) {
        this.moduleUri = uri;
    }

    /**
     * Get the system ID representing the location of the instruction that caused this new context
     * to be created
     * @return the system ID (base URI/module URI) of the module containing the instruction
     */

    public String getSystemId() {
        return moduleUri;
    }

    /**
     * Set the line number of the location of the instruction that caused this new context
     * to be created
     * @param lineNumber the line number of the instruction within its containing module
     */

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    /**
     * Get the line number of the location of the instruction that caused this new context
     * to be created
     * @return the line number of the instruction within its containing module
     */

    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * Set the container of the instruction that caused this new context to be created. This will
     * generally be an object such as an XSLT Template or a user-defined function
     * @param container the container of the instruction
     */

    public void setContainer(Container container) {
        this.container = container;
    }

   /**
     * Get the container of the instruction that caused this new context to be created. This will
     * generally be an object such as an XSLT Template or a user-defined function
     * @return the container of the instruction in the expression tree
     */

    public Container getContainer() {
        return container;
    }

    /**
     * Set the value of the context item at this level of the context stack
     * @param contextItem the context item as it was when this new context was created
     */

    public void setContextItem(Item contextItem) {
        this.contextItem = contextItem;
    }

    /**
     * Get the value of the context item at this level of the context stack
     * @return the context item as it was when this new context was created
     */

    public Item getContextItem() {
        return contextItem;
    }

    /**
     * Display a representation of the stack frame on the specified output stream
     * @param out the output stream
     */

    public abstract void print(PrintStream out);

    /**
     * Subclass of ContextStackFrame representing the outermost stack frame,
     * for the calling application
     */

    public static class CallingApplication extends ContextStackFrame {
        public void print(PrintStream out) {
            //out.println("  (called from external application)");
        }
    }

    /**
     * Subclass of ContextStackFrame representing a built-in template rule in XSLT
     */

    public static class BuiltInTemplateRule extends ContextStackFrame {
        public void print(PrintStream out) {
            out.println("  in built-in template rule");
        }
    }

    /**
     * Subclass of ContextStackFrame representing a call to a user-defined function
     * either in XSLT or XQuery
     */

    public static class FunctionCall extends ContextStackFrame {

        StructuredQName functionName;

        /**
         * Get the name of the function being called
         * @return the name of the function being called
         */
        public StructuredQName getFunctionName() {
            return functionName;
        }

        /**
         * Set the name of the function being called
         * @param functionName the name of the function being called
         */
        public void setFunctionName(StructuredQName functionName) {
            this.functionName = functionName;
        }

        /**
         * Display a representation of the stack frame on the specified output stream
         * @param out the output stream
         */

        public void print(PrintStream out) {
            out.println("  at " + functionName.getDisplayName() + "() " +
                        "(" + getSystemId() + "#" + getLineNumber() + ")");
        }
    }

    /**
     * Subclass of ContextStackFrame representing an xsl:apply-templates call in XSLT
     */

    public static class ApplyTemplates extends ContextStackFrame {

        /**
         * Display a representation of the stack frame on the specified output stream
         * @param out the output stream
         */

        public void print(PrintStream out) {
            out.println("  at xsl:apply-templates (" + getSystemId() + "#" + getLineNumber() + ")");
            Item node = getContextItem();
            if (node instanceof NodeInfo) {
                out.println("     processing " + Navigator.getPath((NodeInfo)node));
            }
        }
    }

   /**
     * Subclass of ContextStackFrame representing an xsl:for-each instruction in XSLT
     */

    public static class ForEach extends ContextStackFrame {
       
        /**
         * Display a representation of the stack frame on the specified output stream
         * @param out the output stream
         */

        public void print(PrintStream out) {
            out.println("  at xsl:for-each (" + getSystemId() + "#" + getLineNumber() + ")");
            Item item = getContextItem();
            if (item instanceof NodeInfo) {
                out.println("     processing " + Navigator.getPath((NodeInfo)item));
            } else {
                out.println("     processing " + Err.wrap(item.getStringValueCS(), Err.VALUE));
            }
        }
    }

   /**
     * Subclass of ContextStackFrame representing an xsl:call-template instruction in XSLT
     */

    public static class CallTemplate extends ContextStackFrame {

       /**
        * Get the name of the template being called
        * @return the name of the template being called. Note this may be null in the case of the
        * extension instruction saxon:call-template
        */
        public StructuredQName getTemplateName() {
            return templateName;
        }

       /**
        * Set the name of the template being called
        * @param templateName the name of the template being called.
        */
        public void setTemplateName(StructuredQName templateName) {
            this.templateName = templateName;
        }

        StructuredQName templateName;

        /**
         * Display a representation of the stack frame on the specified output stream
         * @param out the output stream
         */

        public void print(PrintStream out) {
            String name = templateName == null ? "??" : templateName.getDisplayName();
            out.println("  at xsl:call-template name=\"" + name + "\" ("  +
                        getSystemId() + "#" + getLineNumber() + ")");
        }
    }

   /**
     * Subclass of ContextStackFrame representing the evaluation of a variable (typically a global variable)
     */

    public static class VariableEvaluation extends ContextStackFrame {

       /**
        * Get the name of the variable
        * @return the name of the variable
        */
        public StructuredQName getVariableName() {
            return variableName;
        }

       /**
        * Set the name of the variable
        * @param variableName the name of the variable
        */
        public void setVariableName(StructuredQName variableName) {
            this.variableName = variableName;
        }

        StructuredQName variableName;

        /**
         * Display a representation of the stack frame on the specified output stream
         * @param out the output stream
         */

        public void print(PrintStream out) {
            out.println("  in " + displayContainer(getContainer()) +
                        " (" + getSystemId() + "#" + getLineNumber() + ")");
        }              
    }

    private static String displayContainer(Container container) {
        if (container instanceof Procedure) {
            StructuredQName name = ((Procedure)container).getObjectName();
            String objectName = (name==null ? "" : name.getDisplayName());
            if (container instanceof Template) {
                if (name == null) {
                    //NamePool pool = container.getExecutable().getConfiguration().getNamePool();
                    return "template match=\"" + ((Template)container).getMatchPattern().toString() + "\"";
                } else {
                    return "template name=\"" + objectName + "\"";
                }
            } else if (container instanceof UserFunction) {
                return "function " + objectName + "()";
            } else if (container instanceof AttributeSet) {
                return "attribute-set " + objectName;
            } else if (container instanceof KeyDefinition) {
                return "key " + objectName;
            }
        } else if (container instanceof GlobalVariable) {
            StructuredQName qName = ((GlobalVariable)container).getVariableQName();
            if (NamespaceConstant.SAXON.equals(qName.getNamespaceURI()) && "gg".equals(qName.getPrefix())) {
                return "optimizer-created global variable";
            } else {
                return "variable " + qName.getDisplayName();
            }
        } else {
            return "";
        }
        return "";
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

