package net.sf.saxon.pattern;
import net.sf.saxon.expr.*;
import net.sf.saxon.instruct.Executable;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.ItemType;
import net.sf.saxon.type.Type;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Collections;

/**
* A Pattern represents the result of parsing an XSLT pattern string. <br>
* Patterns are created by calling the static method Pattern.make(string). <br>
* The pattern is used to test a particular node by calling match().
*/

public abstract class Pattern implements Serializable, Container {

    private String originalText;
    private Executable executable;
    private String systemId;      // the module where the pattern occurred
    private int lineNumber;       // the line number where the pattern occurred

    /**
    * Static method to make a Pattern by parsing a String. <br>
    * @param pattern The pattern text as a String
    * @param env An object defining the compile-time context for the expression
    * @return The pattern object
    */

    public static Pattern make(String pattern, StaticContext env, Executable exec) throws XPathException {

        Pattern pat = (new ExpressionParser()).parsePattern(pattern, env);        
        pat.setSystemId(env.getSystemId());
        pat.setLineNumber(env.getLineNumber());
        // System.err.println("Simplified [" + pattern + "] to " + pat.getClass() + " default prio = " + pat.getDefaultPriority());
        // set the pattern text for use in diagnostics
        pat.setOriginalText(pattern);
        pat.setExecutable(exec);
        pat = pat.simplify(env);
        return pat;
    }


    public Executable getExecutable() {
        return executable;
    }

    public void setExecutable(Executable executable) {
        this.executable = executable;
    }


	/**
	* Set the original text of the pattern for use in diagnostics
	*/

	public void setOriginalText(String text) {
		originalText = text;
	}

    /**
    * Simplify the pattern by applying any context-independent optimisations.
    * Default implementation does nothing.
    * @return the optimised Pattern
    */

    public Pattern simplify(StaticContext env) throws XPathException {
        return this;
    }

    /**
    * Type-check the pattern.
    * Default implementation does nothing. This is only needed for patterns that contain
    * variable references or function calls.
    * @return the optimised Pattern
    */

    public Pattern analyze(StaticContext env, ItemType contextItemType) throws XPathException {
        return this;
    }

    /**
     * Get the dependencies of the pattern. The only possible dependency for a pattern is
     * on local variables. This is analyzed in those patterns where local variables may appear.
     */

    public int getDependencies() {
        return 0;
    }

    /**
     * Iterate over the subexpressions within this pattern
     */

    public Iterator iterateSubExpressions() {
        return Collections.EMPTY_LIST.iterator();
    }

    /**
     * Offer promotion for subexpressions within this pattern. The offer will be accepted if the subexpression
     * is not dependent on the factors (e.g. the context item) identified in the PromotionOffer.
     * By default the offer is not accepted - this is appropriate in the case of simple expressions
     * such as constant values and variable references where promotion would give no performance
     * advantage. This method is always called at compile time.
     *
     * <p>Unlike the corresponding method on {@link Expression}, this method does not return anything:
     * it can make internal changes to the pattern, but cannot return a different pattern. Only certain
     * kinds of promotion are applicable within a pattern: specifically, promotions affecting local
     * variable references within the pattern.
     *
     * @param offer details of the offer, for example the offer to move
     *              expressions that don't depend on the context to an outer level in
     *              the containing expression
     * @throws net.sf.saxon.trans.XPathException
     *          if any error is detected
     */

    public void promote(PromotionOffer offer) throws XPathException {
        // default implementation does nothing
    }

    /**
    * Set the system ID where the pattern occurred
    */

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    /**
    * Set the line number where the pattern occurred
    */

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    /**
    * Determine whether this Pattern matches the given Node. This is the main external interface
    * for matching patterns: it sets current() to the node being tested
    * @param node The NodeInfo representing the Element or other node to be tested against the Pattern
    * @param context The dynamic context. Only relevant if the pattern
    * uses variables, or contains calls on functions such as document() or key().
    * @return true if the node matches the Pattern, false otherwise
    */

    public abstract boolean matches(NodeInfo node, XPathContext context) throws XPathException;

    /**
    * Determine whether this Pattern matches the given Node. This is an internal interface used
    * for matching sub-patterns; it does not alter the value of current(). The default implementation
    * is identical to matches().
    * @param node The NodeInfo representing the Element or other node to be tested against the Pattern
    * @param context The dynamic context. Only relevant if the pattern
    * uses variables, or contains calls on functions such as document() or key().
    * @return true if the node matches the Pattern, false otherwise
    */

    protected boolean internalMatches(NodeInfo node, XPathContext context) throws XPathException {
        return matches(node, context);
    }

    /**
    * Determine the types of nodes to which this pattern applies. Used for optimisation.
    * For patterns that match nodes of several types, return Type.NODE
    * @return the type of node matched by this pattern. e.g. Type.ELEMENT or Type.TEXT
    */

    public int getNodeKind() {
        return Type.NODE;
    }

    /**
    * Determine the name fingerprint of nodes to which this pattern applies. Used for
    * optimisation.
    * @return A fingerprint that the nodes must match, or -1 if it can match multiple fingerprints
    */

    public int getFingerprint() {
        return -1;
    }

    /**
    * Get a NodeTest that all the nodes matching this pattern must satisfy
    */

    public abstract NodeTest getNodeTest();

    /**
    * Determine the default priority to use if this pattern appears as a match pattern
    * for a template with no explicit priority attribute.
    */

    public double getDefaultPriority() {
        return 0.5;
    }

    /**
    * Get the system id of the entity in which the pattern occurred
    */

    public String getSystemId() {
		return systemId;
    }

    /**
    * Get the line number on which the pattern was defined
    */

    public int getLineNumber() {
		return lineNumber;
    }

    /**
     * Get the column number (always -1)
     */

    public int getColumnNumber() {
        return -1;
    }

    /**
     * Get the public ID (always null)
     */

    public String getPublicId() {
        return null;
    }
    /**
    * Get the original pattern text
    */

    public String toString() {
    	return originalText;
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
// Contributor(s): Michael Kay
//
