package net.sf.saxon.trans;

import net.sf.saxon.Configuration;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.expr.XPathContextMajor;
import net.sf.saxon.om.Navigator;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.pattern.NoNodeTest;
import net.sf.saxon.pattern.Pattern;
import net.sf.saxon.type.Type;

import java.io.Serializable;

/**
 * A Mode is a collection of rules; the selection of a rule to apply to a given element
 * is determined by a Pattern.
 *
 * @author Michael H. Kay
 */

public class Mode implements Serializable {

    public static final int DEFAULT_MODE = -1;
    public static final int ALL_MODES = -2;
    public static final int NAMED_MODE = -3;
    public static final int STRIPPER_MODE = -4;


    private Rule[] ruleDict = new Rule[101 + Type.MAX_NODE_TYPE];
    private int sequence = 0;   // sequence number for the rules in this Mode
    private boolean isDefault;
    private boolean isStripper;

    /**
     * Default constructor - creates a Mode containing no rules
     * @param usage one of {@link #DEFAULT_MODE}, {@link #NAMED_MODE}, {@link #STRIPPER_MODE}
     */

    public Mode(int usage) {
        this.isDefault = (usage == DEFAULT_MODE);
        this.isStripper = (usage == STRIPPER_MODE);
    }



    /**
     * Construct a new Mode, copying the contents of an existing Mode
     *
     * @param omniMode the existing mode. May be null, in which case it is not copied
     */

    public Mode(Mode omniMode) {
        isDefault = false;
        isStripper = false;
        if (omniMode != null) {
            for (int i = 0; i < this.ruleDict.length; i++) {
                if (omniMode.ruleDict[i] != null) {
                    this.ruleDict[i] = new Rule(omniMode.ruleDict[i]);
                }
            }
            this.sequence = omniMode.sequence;
        }
    }

    /**
     * Determine if this is the default mode
     */

    public boolean isDefaultMode() {
        return isDefault;
    }

    /**
     * Add a rule to the Mode. <br>
     * The rule effectively replaces any other rule for the same pattern/mode at the same or a lower
     * priority.
     *
     * @param p          a Pattern
     * @param obj        the Object to return from getRule() when the supplied node matches this Pattern
     * @param precedence the import precedence of the rule
     * @param priority   the explicit or implicit priority of the rule
     */

    public void addRule(Pattern p, Object obj, int precedence, double priority) {

        // System.err.println("Add rule, pattern = " + p.toString() + " class " + p.getClass() + ", priority=" + priority);

        // Ignore a pattern that will never match, e.g. "@comment"

        if (p.getNodeTest() instanceof NoNodeTest) {
            return;
        }

        // for fast lookup, we maintain one list for each element name for patterns that can only
        // match elements of a given name, one list for each node type for patterns that can only
        // match one kind of non-element node, and one generic list.
        // Each list is sorted in precedence/priority order so we find the highest-priority rule first

        int fingerprint = p.getFingerprint();
        int type = p.getNodeKind();

        int key = getList(fingerprint, type);
        // System.err.println("Fingerprint " + fingerprint + " key " + key + " type " + type);

        Rule newRule = new Rule(p, obj, precedence, priority, sequence++);

        Rule rule = ruleDict[key];
        if (rule == null) {
            ruleDict[key] = newRule;
            return;
        }

        // insert the new rule into this list before others of the same precedence/priority

        Rule prev = null;
        while (rule != null) {
            if ((rule.precedence < precedence) ||
                    (rule.precedence == precedence && rule.priority <= priority)) {
                newRule.next = rule;
                if (prev == null) {
                    ruleDict[key] = newRule;
                } else {
                    prev.next = newRule;
                }
                break;
            } else {
                prev = rule;
                rule = rule.next;
            }
        }
        if (rule == null) {
            prev.next = newRule;
            newRule.next = null;
        }
    }

    /**
     * Determine which list to use for a given pattern (we must also search the generic list)
     */

    public int getList(int fingerprint, int type) {

        if (type == Type.ELEMENT) {
            if (fingerprint == -1) {
                return Type.NODE;   // the generic list
            } else {
                return Type.MAX_NODE_TYPE +
                        (fingerprint % 101);
            }
        } else {
            return type;
        }
    }

    /**
     * Get the rule corresponding to a given Node, by finding the best Pattern match.
     *
     * @param node the NodeInfo referring to the node to be matched
     * @return the object (e.g. a NodeHandler) registered for that element, if any (otherwise null).
     */

    public Object getRule(NodeInfo node, XPathContext context) throws XPathException {
        // System.err.println("Get rule for " + Navigator.getPath(node));
        int fingerprint = node.getFingerprint();
        int type = node.getNodeKind();
        int key = getList(fingerprint, type);
        int policy = context.getController().getRecoveryPolicy();

        // If there are match patterns in the stylesheet that use local variables, we need to allocate
        // a new stack frame for evaluating the match patterns. We base this on the match pattern with
        // the highest number of range variables, so we can reuse the same stack frame for all rules
        // that we test against. If no patterns use range variables, we don't bother allocating a new
        // stack frame.

        context = perhapsMakeNewContext(context);

        Rule specificRule = null;
        Rule generalRule = null;
        int specificPrecedence = -1;
        double specificPriority = Double.NEGATIVE_INFINITY;

        // search the specific list for this node type / node name

        // System.err.println("Hash key = " + key);

        if (key != Type.NODE) {
            Rule r = ruleDict[key];
            while (r != null) {
                // if we already have a match, and the precedence or priority of this
                // rule is lower, quit the search for a second match
                if (specificRule != null) {
                    if (r.precedence < specificPrecedence ||
                            (r.precedence == specificPrecedence && r.priority < specificPriority)) {
                        break;
                    }
                }
                //System.err.println("Testing " + Navigator.getPath(node) + " against " + r.pattern);
                if (r.pattern.matches(node, context)) {
                    //System.err.println("Matches");

                    // is this a second match?
                    if (specificRule != null) {
                        if (r.precedence == specificPrecedence && r.priority == specificPriority) {
                            reportAmbiguity(node, specificRule, r, context);
                        }
                        break;
                    }
                    specificRule = r;
                    specificPrecedence = r.precedence;
                    specificPriority = r.priority;
                    if (policy == Configuration.RECOVER_SILENTLY) {
                        break;                      // find the first; they are in priority order
                    }
                }
                r = r.next;
            }
        }

        // search the general list

        Rule r2 = ruleDict[Type.NODE];
        while (r2 != null) {
            if (r2.precedence < specificPrecedence ||
                    (r2.precedence == specificPrecedence && r2.priority < specificPriority)) {
                break;      // no point in looking at a lower priority rule than the one we've got
            }
            if (r2.pattern.matches(node, context)) {
                // is it a second match?
                if (generalRule != null) {
                    if (r2.precedence == generalRule.precedence && r2.priority == generalRule.priority) {
                        reportAmbiguity(node, r2, generalRule, context);
                    }
                    break;
                } else {
                    generalRule = r2;
                    if (policy == Configuration.RECOVER_SILENTLY) {
                        break;                      // find only the first; they are in priority order
                    }
                }
            }
            r2 = r2.next;
        }

        if (specificRule != null && generalRule == null) {
            return specificRule.object;
        }
        if (specificRule == null && generalRule != null) {
            return generalRule.object;
        }
        if (specificRule != null && generalRule != null) {
            if (specificRule.precedence == generalRule.precedence &&
                    specificRule.priority == generalRule.priority) {
                // This situation is exceptional: we have a "specific" pattern and
                // a "general" pattern with the same priority. We have to select
                // the one that was added last
                // (Bug reported by Norman Walsh Jan 2002)
                Object result = (specificRule.sequence > generalRule.sequence ?
                        specificRule.object :
                        generalRule.object);

                if (policy != Configuration.RECOVER_SILENTLY) {
                    reportAmbiguity(node, specificRule, generalRule, context);
                }
                return result;
            }
            if (specificRule.precedence > generalRule.precedence ||
                    (specificRule.precedence == generalRule.precedence &&
                    specificRule.priority >= generalRule.priority)) {
                return specificRule.object;
            } else {
                return generalRule.object;
            }
        }
        return null;
    }

    /**
     * Make a new XPath context for evaluating patterns if there is any possibility that the
     * pattern uses local variables
     *
     * @param context The existing XPath context
     * @return a new XPath context (or the existing context if no new context was created)
     */

    private XPathContext perhapsMakeNewContext(XPathContext context) {
        int patternLocals = context.getController().getExecutable().getLargestPatternStackFrame();
        if (patternLocals > 0) {
            context = context.newContext();
            context.setOrigin(context.getController());
            ((XPathContextMajor)context).openStackFrame(patternLocals);
        }
        return context;
    }

    /**
     * Get the rule corresponding to a given Node, by finding the best Pattern match, subject to a minimum
     * and maximum precedence. (This supports xsl:apply-imports)
     *
     * @param node the NodeInfo referring to the node to be matched
     * @return the object (e.g. a NodeHandler) registered for that element, if any (otherwise null).
     */

    public Object getRule(NodeInfo node, int min, int max, XPathContext context) throws XPathException {
        int fing = node.getFingerprint();
        int type = node.getNodeKind();
        int key = getList(fing, type);

        Rule specificRule = null;
        Rule generalRule = null;

        context = perhapsMakeNewContext(context);

        // search the the specific list for this node type / name

        if (key != Type.NODE) {
            Rule r = ruleDict[key];
            while (r != null) {
                if (r.precedence >= min && r.precedence <= max &&
                        r.pattern.matches(node, context)) {
                    specificRule = r;
                    break;                      // find the first; they are in priority order
                }
                r = r.next;
            }
        }

        // search the generic list

        Rule r2 = ruleDict[Type.NODE];
        while (r2 != null) {
            if (r2.precedence >= min && r2.precedence <= max && r2.pattern.matches(node, context)) {
                generalRule = r2;
                break;                      // find only the first; they are in priority order
            }
            r2 = r2.next;
        }
        if (specificRule != null && generalRule == null) {
            return specificRule.object;
        }
        if (specificRule == null && generalRule != null) {
            return generalRule.object;
        }
        if (specificRule != null && generalRule != null) {
            if (specificRule.precedence > generalRule.precedence ||
                    (specificRule.precedence == generalRule.precedence &&
                    specificRule.priority >= generalRule.priority)) {
                return specificRule.object;
            } else {
                return generalRule.object;
            }
        }
        return null;
    }

    /**
     * Get the rule corresponding to a given Node, by finding the next-best Pattern match
     * after the specified object.
     *
     * @param node the NodeInfo referring to the node to be matched
     * @return the object (e.g. a NodeHandler) registered for that element, if any (otherwise null).
     */

    public Object getNextMatchRule(NodeInfo node, Object currentHandler, XPathContext context) throws XPathException {
        int fingerprint = node.getFingerprint();
        int type = node.getNodeKind();
        int key = getList(fingerprint, type);
        int policy = context.getController().getRecoveryPolicy();

        int currentPrecedence = -1;
        double currentPriority = -1.0;
        int currentSequence = -1;

        context = perhapsMakeNewContext(context);

        // First find the Rule object corresponding to the current handler

        Rule r = ruleDict[key];
        while (r != null) {
            if (r.object == currentHandler) {
                currentPrecedence = r.precedence;
                currentPriority = r.priority;
                currentSequence = r.sequence;
                break;
            } else {
                r = r.next;
            }
        }
        if (r == null) {
            r = ruleDict[Type.NODE];
            while (r != null) {
                if (r.object == currentHandler) {
                    currentPrecedence = r.precedence;
                    currentPriority = r.priority;
                    currentSequence = r.sequence;
                    break;
                } else {
                    r = r.next;
                }
            }
            if (r == null) {
                DynamicError de = new DynamicError("Internal error: current template doesn't match current node");
                de.setXPathContext(context);
                de.setErrorCode("SAXON:0000");
                throw de;
            }
        }

        Rule specificRule = null;
        Rule generalRule = null;
        int specificPrecedence = -1;
        double specificPriority = Double.NEGATIVE_INFINITY;

        // search the specific list for this node type / node name

        // System.err.println("Hash key = " + key);

        if (key != Type.NODE) {
            r = ruleDict[key];
            while (r != null) {
                // skip this rule if its template is the current template. (There can be more than
                // one rule for the same template in the case of a union pattern.)
                if (r.object == currentHandler) {
                    // skip this rule
                } else
                // skip this rule unless it's "below" the current rule in search order
                    if ((r.precedence > currentPrecedence) ||
                            (r.precedence == currentPrecedence &&
                            (r.priority > currentPriority ||
                            (r.priority == currentPriority && r.sequence >= currentSequence)))) {
                        // skip this rule
                    } else {
                        // quit the search on finding the second (recoverable error) match
                        if (specificRule != null) {
                            if (r.precedence < specificPrecedence ||
                                    (r.precedence == specificPrecedence && r.priority < specificPriority)) {
                                break;
                            }
                        }
                        //System.err.println("Testing " + Navigator.getPath(node) + " against " + r.pattern);
                        if (r.pattern.matches(node, context)) {
                            //System.err.println("Matches");

                            // is this a second match?
                            if (specificRule != null) {
                                if (r.precedence == specificPrecedence && r.priority == specificPriority) {
                                    reportAmbiguity(node, specificRule, r, context);
                                }
                                break;
                            }
                            specificRule = r;
                            specificPrecedence = r.precedence;
                            specificPriority = r.priority;
                            if (policy == Configuration.RECOVER_SILENTLY) {
                                break;                      // find the first; they are in priority order
                            }
                        }
                    }
                r = r.next;
            }
        }

        // search the general list

        Rule r2 = ruleDict[Type.NODE];
        while (r2 != null) {
            // skip this rule if the template is the current template
            if (r2.object == currentHandler) {
                // skip this rule
            } else
            // skip this rule unless it's "after" the current rule in search order
                if ((r2.precedence > currentPrecedence) ||
                        (r2.precedence == currentPrecedence &&
                        (r2.priority > currentPriority ||
                        (r2.priority == currentPriority && r2.sequence >= currentSequence)))) {
                    // skip this rule
                } else {
                    if (r2.precedence < specificPrecedence ||
                            (r2.precedence == specificPrecedence && r2.priority < specificPriority)) {
                        break;      // no point in looking at a lower priority rule than the one we've got
                    }
                    if (r2.pattern.matches(node, context)) {
                        // is it a second match?
                        if (generalRule != null) {
                            if (r2.precedence == generalRule.precedence && r2.priority == generalRule.priority) {
                                reportAmbiguity(node, r2, generalRule, context);
                            }
                            break;
                        } else {
                            generalRule = r2;
                            if (policy == Configuration.RECOVER_SILENTLY) {
                                break;                      // find only the first; they are in priority order
                            }
                        }
                    }
                }
            r2 = r2.next;
        }

        if (specificRule != null && generalRule == null) {
            return specificRule.object;
        }
        if (specificRule == null && generalRule != null) {
            return generalRule.object;
        }
        if (specificRule != null && generalRule != null) {
            if (specificRule.precedence == generalRule.precedence &&
                    specificRule.priority == generalRule.priority) {
                // This situation is exceptional: we have a "specific" pattern and
                // a "general" pattern with the same priority. We have to select
                // the one that was added last
                // (Bug reported by Norman Walsh Jan 2002)
                Object result = (specificRule.sequence > generalRule.sequence ?
                        specificRule.object :
                        generalRule.object);

                if (policy != Configuration.RECOVER_SILENTLY) {
                    reportAmbiguity(node, specificRule, generalRule, context);
                }
                return result;
            }
            if (specificRule.precedence > generalRule.precedence ||
                    (specificRule.precedence == generalRule.precedence &&
                    specificRule.priority >= generalRule.priority)) {
                return specificRule.object;
            } else {
                return generalRule.object;
            }
        }
        return null;
    }

    /**
     * Report an ambiguity, that is, the situation where two rules of the same
     * precedence and priority match the same node
     *
     * @param node The node that matches two or more rules
     * @param r1   The first rule that the node matches
     * @param r2   The second rule that the node matches
     * @param c    The controller for the transformation
     */

    private void reportAmbiguity(NodeInfo node, Rule r1, Rule r2, XPathContext c)
            throws XPathException {
        // don't report an error if the conflict is between two branches of the same
        // Union pattern
        if (r1.object == r2.object) {
            return;
        }
        Pattern pat1 = r1.pattern;
        Pattern pat2 = r2.pattern;

        String path;
        if (isStripper) {
            path = "xsl:strip-space";
        } else {
            path = Navigator.getPath(node);
        }

        DynamicError err = new DynamicError("Ambiguous rule match for " + path + '\n' +
                "Matches both \"" + pat1 + "\" on line " + pat1.getLineNumber() + " of " + pat1.getSystemId() +
                "\nand \"" + pat2 + "\" on line " + pat2.getLineNumber() + " of " + pat2.getSystemId());
        err.setErrorCode((isStripper ? "XT0270" : "XT0540"));
        err.setLocator(c.getOrigin().getInstructionInfo());
        c.getController().recoverableError(err);
    }


    /**
     * Inner class Rule used to support the implementation
     */

    private static class Rule implements Serializable {
        public Pattern pattern;
        public Object object;
        public int precedence;
        public double priority;
        public int sequence;
        public Rule next;

        /**
         * Create a Rule
         *
         * @param p    the pattern that this rule matches
         * @param o    the object invoked by this rule (usually a Template)
         * @param prec the precedence of the rule
         * @param prio the priority of the rule
         * @param seq  a sequence number for ordering of rules
         */

        public Rule(Pattern p, Object o, int prec, double prio, int seq) {
            pattern = p;
            object = o;
            precedence = prec;
            priority = prio;
            next = null;
            sequence = seq;
        }

        /**
         * Copy a rule, including the chain of rules linked to it
         *
         * @param r
         */

        public Rule(Rule r) {
            this.pattern = r.pattern;
            this.object = r.object;
            this.precedence = r.precedence;
            this.priority = r.priority;
            this.sequence = r.sequence;
            if (r.next == null) {
                this.next = null;
            } else {
                this.next = new Rule(r.next);
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
// The Original Code is: all this file.
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//
