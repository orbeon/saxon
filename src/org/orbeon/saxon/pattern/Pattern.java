package org.orbeon.saxon.pattern;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.event.LocationProvider;
import org.orbeon.saxon.expr.*;
import org.orbeon.saxon.instruct.Block;
import org.orbeon.saxon.instruct.Executable;
import org.orbeon.saxon.instruct.SlotManager;
import org.orbeon.saxon.om.*;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.ItemType;
import org.orbeon.saxon.type.Type;

import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;

/**
* A Pattern represents the result of parsing an XSLT pattern string. <br>
* Patterns are created by calling the static method Pattern.make(string). <br>
* The pattern is used to test a particular node by calling match().
*/

public abstract class Pattern implements PatternFinder, Serializable, Container {

    private String originalText;
    private Executable executable;
    private String systemId;      // the module where the pattern occurred
    private int lineNumber;       // the line number where the pattern occurred

    /**
    * Static method to make a Pattern by parsing a String. <br>
    * @param pattern The pattern text as a String
    * @param env An object defining the compile-time context for the expression
    * @param exec The executable containing this pattern
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
        ExpressionVisitor visitor = ExpressionVisitor.make(env);
        visitor.setExecutable(exec);
        pat = pat.simplify(visitor);
        return pat;
    }

    /**
     * Get the executable containing this pattern
     * @return the executable
     */

    public Executable getExecutable() {
        return executable;
    }

    /**
     * Set the executable containing this pattern
     * @param executable the executable
     */

    public void setExecutable(Executable executable) {
        this.executable = executable;
    }

    /**
     * Get the LocationProvider allowing location identifiers to be resolved.
     */

    public LocationProvider getLocationProvider() {
        return executable.getLocationMap();
    }


	/**
	 * Set the original text of the pattern for use in diagnostics
     * @param text the original text of the pattern
	 */

	public void setOriginalText(String text) {
		originalText = text;
	}

    /**
     * Simplify the pattern by applying any context-independent optimisations.
     * Default implementation does nothing.
     * @return the optimised Pattern
     * @param visitor the expression visitor
     */

    public Pattern simplify(ExpressionVisitor visitor) throws XPathException {
        return this;
    }

    /**
     * Type-check the pattern.
     * Default implementation does nothing. This is only needed for patterns that contain
     * variable references or function calls.
     * @param visitor the expression visitor
     * @param contextItemType the type of the context item, if known, at the point where the pattern
     * is defined
     * @return the optimised Pattern
    */

    public Pattern analyze(ExpressionVisitor visitor, ItemType contextItemType) throws XPathException {
        return this;
    }

    /**
     * Get the dependencies of the pattern. The only possible dependency for a pattern is
     * on local variables. This is analyzed in those patterns where local variables may appear.
     * @return the dependencies, as a bit-significant mask
     */

    public int getDependencies() {
        return 0;
    }

    /**
     * Iterate over the subexpressions within this pattern
     * @return an iterator over the subexpressions. Default implementation returns an empty sequence
     */

    public Iterator iterateSubExpressions() {
        return Collections.EMPTY_LIST.iterator();
    }

   /**
     * Allocate slots to any variables used within the pattern
     * @param env the static context in the XSLT stylesheet
     * @param slotManager the slot manager representing the stack frame for local variables
     * @param nextFree the next slot that is free to be allocated @return the next slot that is free to be allocated
     */

    public int allocateSlots(StaticContext env, SlotManager slotManager, int nextFree) {
        return nextFree;
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
     * @throws org.orbeon.saxon.trans.XPathException
     *          if any error is detected
     */

    public void promote(PromotionOffer offer) throws XPathException {
        // default implementation does nothing
    }

    /**
     * Set the system ID where the pattern occurred
     * @param systemId the URI of the module containing the pattern
    */

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    /**
     * Set the line number where the pattern occurred
     * @param lineNumber the line number of the pattern in the source module
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
     * Select nodes in a document using this PatternFinder.
     * @param doc the document node at the root of a tree
     * @param context the dynamic evaluation context
     * @return an iterator over the selected nodes in the document.
     */

    public SequenceIterator selectNodes(DocumentInfo doc, final XPathContext context) throws XPathException {
       final int kind = getNodeKind();
       switch (kind) {
            case Type.DOCUMENT:
                if (matches(doc, context)) {
                    return SingletonIterator.makeIterator(doc);
                } else {
                    return EmptyIterator.getInstance();
                }
            case Type.ATTRIBUTE: {
                AxisIterator allElements = doc.iterateAxis(Axis.DESCENDANT, NodeKindTest.ELEMENT);
                MappingFunction atts = new MappingFunction() {
                    public SequenceIterator map(Item item) {
                        return ((NodeInfo)item).iterateAxis(Axis.ATTRIBUTE);
                    }
                };
                SequenceIterator allAttributes = new MappingIterator(allElements, atts);
                ItemMappingFunction test = new ItemMappingFunction() {
                    public Item map(Item item) throws XPathException {
                        if ((matches((NodeInfo)item, context))) {
                            return item;
                        } else {
                            return null;
                        }
                    }
                };
                return new ItemMappingIterator(allAttributes, test);
            }
            case Type.ELEMENT:
            case Type.COMMENT:
            case Type.TEXT:
            case Type.PROCESSING_INSTRUCTION: {
                AxisIterator allChildren = doc.iterateAxis(Axis.DESCENDANT, NodeKindTest.makeNodeKindTest(kind));
                ItemMappingFunction test = new ItemMappingFunction() {
                    public Item map(Item item) throws XPathException {
                        if ((matches((NodeInfo)item, context))) {
                            return item;
                        } else {
                            return null;
                        }
                    }
                };
                return new ItemMappingIterator(allChildren, test);
            }
            case Type.NODE: {
                AxisIterator allChildren = doc.iterateAxis(Axis.DESCENDANT);
                MappingFunction attsOrSelf = new MappingFunction() {
                    public SequenceIterator map(Item item) {
                        return new PrependIterator((NodeInfo)item, ((NodeInfo)item).iterateAxis(Axis.ATTRIBUTE));
                    }
                };
                SequenceIterator attributesOrSelf = new MappingIterator(allChildren, attsOrSelf);
                ItemMappingFunction test = new ItemMappingFunction() {
                    public Item map(Item item) throws XPathException {
                        if ((matches((NodeInfo)item, context))) {
                            return item;
                        } else {
                            return null;
                        }
                    }
                };
                return new ItemMappingIterator(attributesOrSelf, test);
            }
            case Type.NAMESPACE:
               throw new UnsupportedOperationException("Patterns can't match namespace nodes");
            default:
               throw new UnsupportedOperationException("Unknown node kind");
        }
    }

    /**
     * Make an expression whose effect is to select all the nodes that match this pattern
     * in a given document. This expression takes the root of the tree (always a document node)
     * as the context node; it takes into account all the constraints expressed by the pattern
     * including the parent and ancestor nodes and the filters
     */

    public Expression makeSearchExpression() {
        // TODO:PERF could improve the logic for LocationPathPatterns
        final int kind = getNodeKind();
        Expression base;
        switch (kind) {
            case Type.DOCUMENT:
                base = new ContextItemExpression();
                break;
            case Type.ATTRIBUTE:
                Expression allElements = new AxisExpression(Axis.DESCENDANT, NodeKindTest.ELEMENT);
                base = new PathExpression(allElements,
                        new AxisExpression(Axis.ATTRIBUTE, AnyNodeTest.getInstance()));
                break;

            case Type.ELEMENT:
            case Type.COMMENT:
            case Type.TEXT:
            case Type.PROCESSING_INSTRUCTION:
                base = new AxisExpression(Axis.DESCENDANT, NodeKindTest.makeNodeKindTest(kind));
                break;

            case Type.NODE:
                Expression allChildren = new AxisExpression(Axis.DESCENDANT, NodeKindTest.ELEMENT);
                Block block = new Block();
                Expression[] union = {new ContextItemExpression(),
                                      new AxisExpression(Axis.ATTRIBUTE, AnyNodeTest.getInstance())};
                block.setChildren(union);
                base = new PathExpression(allChildren, block);
                break;

            case Type.NAMESPACE:
               throw new UnsupportedOperationException("Patterns can't match namespace nodes");
            default:
               throw new UnsupportedOperationException("Unknown node kind");
        }

        return new FilterExpression(base, new PatternMatchExpression(this));

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
     * @return a NodeTest, as specific as possible, which all the matching nodes satisfy
    */

    public abstract NodeTest getNodeTest();

    /**
     * Determine the default priority to use if this pattern appears as a match pattern
     * for a template with no explicit priority attribute.
     * @return the default priority for the pattern
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
        if (originalText != null) {
    	    return originalText;
        } else {
            return "pattern matching " + getNodeTest().toString();
        }
    }

    /**
     * Get the host language (XSLT, XQuery, XPath) used to implement the code in this container
     * @return typically {@link org.orbeon.saxon.Configuration#XSLT} or {@link org.orbeon.saxon.Configuration#XQUERY}
     */

    public int getHostLanguage() {
        return Configuration.XSLT;
    }

    public boolean replaceSubExpression(Expression original, Expression replacement) {
        throw new IllegalArgumentException("Invalid replacement");
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
