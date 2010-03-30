package org.orbeon.saxon;

import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.instruct.*;
import org.orbeon.saxon.om.Navigator;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.om.StandardNames;
import org.orbeon.saxon.om.StructuredQName;
import org.orbeon.saxon.trace.ContextStackFrame;
import org.orbeon.saxon.trace.ContextStackIterator;
import org.orbeon.saxon.trace.InstructionInfo;
import org.orbeon.saxon.trace.Location;
import org.orbeon.saxon.trans.KeyDefinition;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ValidationException;
import org.xml.sax.SAXException;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMLocator;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.Iterator;

/**
 * <B>StandardErrorListener</B> is the standard error handler for XSLT processing
 * errors, used if no other ErrorListener is nominated.
 *
 * @author Michael H. Kay
 */

public class StandardErrorListener implements ErrorListener, Serializable {

    private int recoveryPolicy = Configuration.RECOVER_WITH_WARNINGS;
    private int warningCount = 0;
    protected transient PrintStream errorOutput = System.err;
    private boolean doStackTrace = true;

    /**
     * Create a Standard Error Listener
     */

    public StandardErrorListener() {
    }

    /**
     * Make a clean copy of this ErrorListener. This is necessary because the
     * standard error listener is stateful (it remembers how many errors there have been)
     *
     * @param hostLanguage the host language (not used by this implementation)
     * @return a copy of this error listener
     */

    public StandardErrorListener makeAnother(int hostLanguage) {
        return new StandardErrorListener();
    }

    // Note, when the standard error listener is used, a new
    // one is created for each transformation, because it holds
    // the recovery policy and the warning count.

    /**
     * Set output destination for error messages (default is System.err)
     *
     * @param writer The PrintStream to use for error messages
     */

    public void setErrorOutput(PrintStream writer) {
        errorOutput = writer;
    }

    /**
     * Get the error output stream
     *
     * @return the error output stream
     */

    public PrintStream getErrorOutput() {
        return errorOutput;
    }

    /**
     * Set the recovery policy
     *
     * @param policy the recovery policy for XSLT recoverable errors. One of
     *               {@link Configuration#RECOVER_SILENTLY},
     *               {@link Configuration#RECOVER_WITH_WARNINGS},
     *               {@link Configuration#DO_NOT_RECOVER}.
     */

    public void setRecoveryPolicy(int policy) {
        recoveryPolicy = policy;
    }

    /**
     * Get the recovery policy
     *
     * @return the recovery policy for XSLT recoverable errors. One of
     *         {@link Configuration#RECOVER_SILENTLY},
     *         {@link Configuration#RECOVER_WITH_WARNINGS},
     *         {@link Configuration#DO_NOT_RECOVER}.
     */

    public int getRecoveryPolicy() {
        return recoveryPolicy;
    }

    /**
     * Receive notification of a warning.
     * <p/>
     * <p>Transformers can use this method to report conditions that
     * are not errors or fatal errors.  The default behaviour is to
     * take no action.</p>
     * <p/>
     * <p>After invoking this method, the Transformer must continue with
     * the transformation. It should still be possible for the
     * application to process the document through to the end.</p>
     *
     * @param exception The warning information encapsulated in a
     *                  transformer exception.
     * @throws javax.xml.transform.TransformerException
     *          if the application
     *          chooses to discontinue the transformation.
     * @see javax.xml.transform.TransformerException
     */

    public void warning(TransformerException exception)
            throws TransformerException {

        if (recoveryPolicy == Configuration.RECOVER_SILENTLY) {
            // do nothing
            return;
        }

        if (errorOutput == null) {
            // can happen after deserialization
            errorOutput = System.err;
        }
        String message = "";
        if (exception.getLocator() != null) {
            message = getLocationMessage(exception) + "\n  ";
        }
        message += wordWrap(getExpandedMessage(exception));

        if (exception instanceof ValidationException) {
            errorOutput.println("Validation error " + message);

        } else {
            errorOutput.println("Warning: " + message);
            warningCount++;
            if (warningCount > 25) {
                errorOutput.println("No more warnings will be displayed");
                recoveryPolicy = Configuration.RECOVER_SILENTLY;
                warningCount = 0;
            }
        }
    }

    /**
     * Receive notification of a recoverable error.
     * <p/>
     * <p>The transformer must continue to provide normal parsing events
     * after invoking this method.  It should still be possible for the
     * application to process the document through to the end.</p>
     * <p/>
     * <p>The action of the standard error listener depends on the
     * recovery policy that has been set, which may be one of RECOVER_SILENTLY,
     * RECOVER_WITH_WARNING, or DO_NOT_RECOVER
     *
     * @param exception The error information encapsulated in a
     *                  transformer exception.
     * @throws TransformerException if the application
     *                              chooses to discontinue the transformation.
     * @see TransformerException
     */

    public void error(TransformerException exception) throws TransformerException {
        if (recoveryPolicy == Configuration.RECOVER_SILENTLY) {
            // do nothing
            return;
        }
        if (errorOutput == null) {
            // can happen after deserialization
            errorOutput = System.err;
        }
        String message;
        if (exception instanceof ValidationException) {
            String explanation = getExpandedMessage(exception);
            String constraintReference = ((ValidationException)exception).getConstraintReferenceMessage();
            if (constraintReference != null) {
                explanation += " (" + constraintReference + ')';
            }
            message = "Validation error " +
                    getLocationMessage(exception) +
                    "\n  " +
                    wordWrap(explanation);
        } else {
            String prefix = (recoveryPolicy == Configuration.RECOVER_WITH_WARNINGS ?
                    "Recoverable error " : "Error ");
            message = prefix + getLocationMessage(exception) +
                    "\n  " +
                    wordWrap(getExpandedMessage(exception));
        }

        if (exception instanceof ValidationException) {
            errorOutput.println(message);

        } else if (recoveryPolicy == Configuration.RECOVER_WITH_WARNINGS) {
            errorOutput.println(message);
            warningCount++;
            if (warningCount > 25) {
                errorOutput.println("No more warnings will be displayed");
                recoveryPolicy = Configuration.RECOVER_SILENTLY;
                warningCount = 0;
            }
        } else {
            errorOutput.println(message);
            errorOutput.println("Processing terminated because error recovery is disabled");
            throw XPathException.makeXPathException(exception);
        }
    }

    /**
     * Receive notification of a non-recoverable error.
     * <p/>
     * <p>The application must assume that the transformation cannot
     * continue after the Transformer has invoked this method,
     * and should continue (if at all) only to collect
     * addition error messages. In fact, Transformers are free
     * to stop reporting events once this method has been invoked.</p>
     *
     * @param exception The error information encapsulated in a
     *                  transformer exception.
     * @throws TransformerException if the application
     *                              chooses to discontinue the transformation.
     * @see TransformerException
     */

    public void fatalError(TransformerException exception) throws TransformerException {
        if (exception instanceof XPathException && ((XPathException)exception).hasBeenReported()) {
            // don't report the same error twice
            return;
        }
        if (errorOutput == null) {
            // can happen after deserialization
            errorOutput = System.err;
        }
        String message;
        if (exception instanceof ValidationException) {
            String explanation = getExpandedMessage(exception);
            String constraintReference = ((ValidationException)exception).getConstraintReferenceMessage();
            if (constraintReference != null) {
                explanation += " (" + constraintReference + ')';
            }
            message = "Validation error " +
                    getLocationMessage(exception) +
                    "\n  " +
                    wordWrap(explanation);
        } else {
            message = "Error " +
                    getLocationMessage(exception) +
                    "\n  " +
                    wordWrap(getExpandedMessage(exception));

        }

        errorOutput.println(message);
        if (exception instanceof XPathException) {
            ((XPathException)exception).setHasBeenReported();
            // probably redundant. It's the caller's job to set this flag, because there might be
            // a non-standard error listener in use.
        }

        if (exception instanceof XPathException) {
            XPathContext context = ((XPathException)exception).getXPathContext();
            if (context != null && doStackTrace && getRecoveryPolicy() != Configuration.RECOVER_SILENTLY) {
                printStackTrace(errorOutput, context);
            }
        }
    }

    /**
     * Get a string identifying the location of an error.
     *
     * @param err the exception containing the location information
     * @return a message string describing the location
     */

    public String getLocationMessage(TransformerException err) {
        SourceLocator loc = err.getLocator();
        while (loc == null) {
            if (err.getException() instanceof TransformerException) {
                err = (TransformerException)err.getException();
                loc = err.getLocator();
            } else if (err.getCause() instanceof TransformerException) {
                err = (TransformerException)err.getCause();
                loc = err.getLocator();
            } else {
                return "";
            }
        }
        XPathContext context = null;
        if (err instanceof XPathException) {
            context = ((XPathException)err).getXPathContext();
        }
        return getLocationMessageText(loc, context);
    }

    private static String getLocationMessageText(SourceLocator loc, XPathContext context) {
        String locMessage = "";
        String systemId = null;
        NodeInfo node = null;
        String nodeMessage = null;
        int lineNumber = -1;
        if (loc instanceof DOMLocator) {
            nodeMessage = "at " + ((DOMLocator)loc).getOriginatingNode().getNodeName() + ' ';
        } else if (loc instanceof NodeInfo) {
            node = (NodeInfo)loc;
            nodeMessage = "at " + node.getDisplayName() + ' ';
        } else if (loc instanceof ValidationException && ((ValidationException)loc).getNode() != null) {
            node = ((ValidationException)loc).getNode();
            nodeMessage = "at " + node.getDisplayName() + ' ';
        } else if (loc instanceof Instruction) {
            String instructionName = getInstructionName(((Instruction)loc), context);
            if (!"".equals(instructionName)) {
                nodeMessage = "at " + instructionName + ' ';
            }
            systemId = loc.getSystemId();
            lineNumber = loc.getLineNumber();
        } else if (loc instanceof Procedure) {
            String kind = "procedure";
            if (loc instanceof UserFunction) {
                kind = "function";
            } else if (loc instanceof Template) {
                kind = "template";
            } else if (loc instanceof AttributeSet) {
                kind = "attribute-set";
            } else if (loc instanceof KeyDefinition) {
                kind = "key";
            }
            systemId = loc.getSystemId();
            lineNumber = loc.getLineNumber();
            nodeMessage = "at " + kind + " " +
                    ((InstructionInfo)loc).getObjectName();
        }
        if (lineNumber == -1) {
            lineNumber = loc.getLineNumber();
        }
        boolean containsLineNumber = lineNumber != -1;
        if (node != null && !containsLineNumber) {
            nodeMessage = "at " + Navigator.getPath(node) + ' ';
        }
        if (nodeMessage != null) {
            locMessage += nodeMessage;
        }
        if (containsLineNumber) {
            locMessage += "on line " + lineNumber + ' ';
            if (loc.getColumnNumber() != -1) {
                locMessage += "column " + loc.getColumnNumber() + ' ';
            }
        }
        
        if (systemId != null && systemId.length() == 0) {
            systemId = null;
        }
        if (systemId == null) {
            systemId = loc.getSystemId();
        }
        if (systemId != null && systemId.length() != 0) {
            locMessage += (containsLineNumber ? "of " : "in ") + abbreviatePath(systemId) + ':';
        }
        return locMessage;
    }

    /**
     * Abbreviate a URI (if requested)
     * @param uri the URI to be abbreviated
     * @return the abbreviated URI, unless full path names were requested, in which case
     * the URI as supplied
     */

    public static String abbreviatePath(String uri) {
        int slash = uri.lastIndexOf('/');
        if (slash >= 0 && slash < uri.length()-1) {
            return uri.substring(slash+1);
        } else {
            return uri;
        }
    }

    /**
     * Get a string containing the message for this exception and all contained exceptions
     *
     * @param err the exception containing the required information
     * @return a message that concatenates the message of this exception with its contained exceptions,
     *         also including information about the error code and location.
     */

    public static String getExpandedMessage(TransformerException err) {

        String code = null;
        if (err instanceof XPathException) {
            code = ((XPathException)err).getErrorCodeLocalPart();
        }
        if (code == null && err.getException() instanceof XPathException) {
            code = ((XPathException)err.getException()).getErrorCodeLocalPart();
        }
        String message = "";
        if (code != null) {
            message = code;
        }

        Throwable e = err;
        while (true) {
            if (e == null) {
                break;
            }
            String next = e.getMessage();
            if (next == null) {
                next = "";
            }
            if (next.startsWith("org.orbeon.saxon.trans.XPathException: ")) {
                next = next.substring(next.indexOf(": ") + 2);
            }
            if (!("TRaX Transform Exception".equals(next) || message.endsWith(next))) {
                if (!"".equals(message) && !message.trim().endsWith(":")) {
                    message += ": ";
                }
                message += next;
            }
            if (e instanceof TransformerException) {
                e = ((TransformerException)e).getException();
            } else if (e instanceof SAXException) {
                e = ((SAXException)e).getException();
            } else {
                // e.printStackTrace();
                break;
            }
        }

        return message;
    }

    /**
     * Extract a name identifying the instruction at which an error occurred
     *
     * @param inst    the provider of information
     * @param context the dynamic evaluation context
     * @return the name of the containing instruction or expression, in user-meaningful terms
     */

    private static String getInstructionName(Instruction inst, XPathContext context) {
        // TODO: subclass this for XSLT and XQuery
        if (context == null) {
            return "";
        }
        try {
            //InstructionInfo info = inst.getInstructionInfo();
            int construct = inst.getInstructionNameCode();
            if (construct < 0) {
                return "";
            }
            if (construct < 1024 &&
                    construct != StandardNames.XSL_FUNCTION &&
                    construct != StandardNames.XSL_TEMPLATE) {
                // it's a standard name
                if (context.getController().getExecutable().getHostLanguage() == Configuration.XSLT) {
                    return StandardNames.getDisplayName(construct);
                } else {
                    String s = StandardNames.getDisplayName(construct);
                    int colon = s.indexOf(':');
                    if (colon > 0) {
                        String local = s.substring(colon + 1);
                        if (local.equals("document")) {
                            return "document node constructor";
                        } else if (local.equals("text") || s.equals("value-of")) {
                            return "text node constructor";
                        } else if (local.equals("element")) {
                            return "computed element constructor";
                        } else if (local.equals("attribute")) {
                            return "computed attribute constructor";
                        } else if (local.equals("variable")) {
                            return "variable declaration";
                        } else if (local.equals("param")) {
                            return "external variable declaration";
                        } else if (local.equals("comment")) {
                            return "comment constructor";
                        } else if (local.equals("processing-instruction")) {
                            return "processing-instruction constructor";
                        }
                    }
                    return s;
                }
            }
            switch (construct) {
            case Location.LITERAL_RESULT_ELEMENT: {
                StructuredQName qName = inst.getObjectName();
                String name = "element constructor";
                if (context != null) {
                    name += " <" + qName.getDisplayName() + '>';
                }
                return name;
            }
            case Location.LITERAL_RESULT_ATTRIBUTE: {
                StructuredQName qName = inst.getObjectName();
                String name = "attribute constructor";
                if (context != null) {
                    name += ' ' + qName.getDisplayName() + "=\"{...}\"";
                }
                return name;
            }

            default:
                return "";
            }

        } catch (Exception err) {
            return "";
        }
    }

    /**
     * Wordwrap an error message into lines of 72 characters or less (if possible)
     *
     * @param message the message to be word-wrapped
     * @return the message after applying word-wrapping
     */

    private static String wordWrap(String message) {
        int nl = message.indexOf('\n');
        if (nl < 0) {
            nl = message.length();
        }
        if (nl > 100) {
            int i = 90;
            while (message.charAt(i) != ' ' && i > 0) {
                i--;
            }
            if (i > 10) {
                return message.substring(0, i) + "\n  " + wordWrap(message.substring(i + 1));
            } else {
                return message;
            }
        } else if (nl < message.length()) {
            return message.substring(0, nl) + '\n' + wordWrap(message.substring(nl + 1));
        } else {
            return message;
        }
    }

    /**
     * Print a stack trace to a specified output destination
     * @param out the print stream to which the stack trace will be output
     * @param context the XPath dynamic execution context (which holds the head of a linked
     * list of context objects, representing the execution stack)
     */

    public static void printStackTrace(PrintStream out, XPathContext context) {
        Iterator iterator = new ContextStackIterator(context);
        while (iterator.hasNext()) {
            ContextStackFrame frame = (ContextStackFrame)iterator.next();
            if (frame == null) {
                break;
            }
            frame.print(out);
        }
    }

}

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
//
