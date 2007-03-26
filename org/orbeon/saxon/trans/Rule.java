package org.orbeon.saxon.trans;

import org.orbeon.saxon.pattern.Pattern;

import java.io.Serializable;

/**
 * Rule: a template rule, or a strip-space rule used to support the implementation
 */

public final class Rule implements Serializable {
    private Pattern pattern;     // The pattern that fires this rule
    private Object action;       // The action associated with this rule (usually a Template)
    private int precedence;      // The import precedence
    private double priority;     // The priority of the rule
    private Rule next;           // The next rule after this one in the chain of rules
    private int sequence;        // The relative position of this rule, its position in declaration order    

    public int getSequence() {
        return sequence;
    }



    public Object getAction() {
        return action;
    }

    public Rule getNext() {
        return next;
    }

    public void setNext(Rule next) {
        this.next = next;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public int getPrecedence() {
        return precedence;
    }

    public double getPriority() {
        return priority;
    }

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
        action = o;
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
        this.action = r.action;
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
