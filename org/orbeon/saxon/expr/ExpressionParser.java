package org.orbeon.saxon.expr;
import org.orbeon.saxon.Err;
import org.orbeon.saxon.Configuration;
import org.orbeon.saxon.functions.RegexGroup;
import org.orbeon.saxon.functions.CurrentGroup;
import org.orbeon.saxon.event.LocationProvider;
import org.orbeon.saxon.instruct.Block;
import org.orbeon.saxon.instruct.Executable;
import org.orbeon.saxon.instruct.LocationMap;
import org.orbeon.saxon.instruct.TraceExpression;
import org.orbeon.saxon.om.Axis;
import org.orbeon.saxon.om.NameChecker;
import org.orbeon.saxon.om.NamespaceConstant;
import org.orbeon.saxon.om.QNameException;
import org.orbeon.saxon.pattern.*;
import org.orbeon.saxon.sort.Reverser;
import org.orbeon.saxon.style.StandardNames;
import org.orbeon.saxon.trace.Location;
import org.orbeon.saxon.trans.DynamicError;
import org.orbeon.saxon.trans.StaticError;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.*;
import org.orbeon.saxon.value.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Parser for XPath expressions and XSLT patterns.
 *
 * This code was originally inspired by James Clark's xt but has been totally rewritten (several times)
 *
 * @author Michael Kay
 */


public class ExpressionParser {

    protected Tokenizer t;
    protected StaticContext env;
    protected Stack rangeVariables = null;
        // The stack holds a list of range variables that are in scope.
        // Each entry on the stack is a VariableDeclaration object containing details
        // of the variable.

    protected NameChecker nameChecker;

    protected boolean scanOnly = false;
        // scanOnly is set to true while attributes in direct element constructors
        // are being processed. We need to parse enclosed expressions in the attribute
        // in order to find the end of the attribute value, but we don't yet know the
        // full namespace context at this stage.

    protected int language = XPATH;     // know which language we are parsing, for diagnostics
    protected static final int XPATH = 0;
    protected static final int XSLT_PATTERN = 1;
    protected static final int SEQUENCE_TYPE = 2;
    protected static final int XQUERY = 3;

    public ExpressionParser(){}

    public Tokenizer getTokenizer() {
        return t;
    }

    /**
     * Read the next token, catching any exception thrown by the tokenizer
     */

    protected void nextToken() throws StaticError {
        try {
            t.next();
        } catch (StaticError err) {
            grumble(err.getMessage());
        }
    }

    /**
     * Expect a given token; fail if the current token is different. Note that this method
     * does not read any tokens.
     *
     * @param token the expected token
     * @throws org.orbeon.saxon.trans.StaticError if the current token is not the expected
     *     token
     */

    protected void expect(int token) throws StaticError {
        if (t.currentToken != token)
            grumble("expected \"" + Token.tokens[token] +
                             "\", found " + currentTokenDisplay());
    }

    /**
     * Report a syntax error (a static error with error code XP0003)
     * @param message the error message
     * @exception org.orbeon.saxon.trans.StaticError always thrown: an exception containing the
     *     supplied message
     */

    protected void grumble(String message) throws StaticError {
        grumble(message, (language == XSLT_PATTERN ? "XTSE0340" : "XPST0003"));
    }

    /**
     * Report a static error
     *
     * @param message the error message
     * @param errorCode the error code
     * @throws org.orbeon.saxon.trans.StaticError always thrown: an exception containing the
     *     supplied message
     */

    protected void grumble(String message, String errorCode) throws StaticError {
        if (errorCode == null) {
            errorCode = "XPST0003";
        }
        String s = t.recentText();
        int line = t.getLineNumber();
        int column = t.getColumnNumber();
        String lineInfo = (line==1 ? "" : ("on line " + line + ' '));
        String columnInfo = "at char " + column + ' ';
        String prefix = getLanguage() + " syntax error " + columnInfo + lineInfo +
                    (message.startsWith("...") ? "near" : "in") +
                    ' ' + Err.wrap(s) + ":\n    ";
        StaticError err = new StaticError(prefix + message);
        err.setErrorCode(errorCode);
        throw err;
    }

    /**
     * Output a warning message
     */

    protected void warning(String message) throws StaticError {
        String s = t.recentText();
        int line = t.getLineNumber();
        String lineInfo = (line==1 ? "" : ("on line " + line + ' '));
        String prefix = "Warning " + lineInfo +
                    (message.startsWith("...") ? "near" : "in") +
                ' ' + Err.wrap(s) + ":\n    ";
        env.issueWarning(prefix + message, null);
    }

    /**
     * Get the current language (XPath or XQuery)
     */

    protected String getLanguage() {
        switch (language) {
            case XPATH:
                return "XPath";
            case XSLT_PATTERN:
                return "XSLT Pattern";
            case SEQUENCE_TYPE:
                return "SequenceType";
            case XQUERY:
                return "XQuery";
            default:
                return "XPath";
        }
    }

    /**
     * Display the current token in an error message
     *
     * @return the display representation of the token
     */
    protected String currentTokenDisplay() {
        if (t.currentToken==Token.NAME) {
            return "name \"" + t.currentTokenValue + '\"';
        } else if (t.currentToken==Token.UNKNOWN) {
            return "(unknown token)";
        } else {
            return '\"' + Token.tokens[t.currentToken] + '\"';
        }
    }

	/**
	 * Parse a string representing an expression
	 *
	 * @throws org.orbeon.saxon.trans.StaticError if the expression contains a syntax error
	 * @param expression the expression expressed as a String
     * @param start offset within the string where parsing is to start
     * @param terminator character to treat as terminating the expression
     * @param lineNumber location of the start of the expression, for diagnostics
	 * @param env the static context for the expression
	 * @return an Expression object representing the result of parsing
	 */

	public Expression parse(String expression, int start, int terminator, int lineNumber, StaticContext env) throws StaticError {
        // System.err.println("Parse expression: " + expression);
	    this.env = env;
        this.nameChecker = env.getConfiguration().getNameChecker();
        t = new Tokenizer();
        try {
	        t.tokenize(expression, start, -1, lineNumber);
        } catch (StaticError err) {
            grumble(err.getMessage());
        }
        Expression exp = parseExpression();
        if (t.currentToken != terminator) {
            if (t.currentToken == Token.EOF && terminator == Token.RCURLY) {
                grumble("Missing curly brace after expression in attribute value template", "XTSE0350");
            } else {
                grumble("Unexpected token " + currentTokenDisplay() + " beyond end of expression");
            }
        }
        return exp;
    }

    /**
     * Parse a string representing an XSLT pattern
     *
     * @throws org.orbeon.saxon.trans.StaticError if the pattern contains a syntax error
     * @param pattern the pattern expressed as a String
     * @param env the static context for the pattern
     * @return a Pattern object representing the result of parsing
     */

    public Pattern parsePattern(String pattern, StaticContext env) throws StaticError {
	    this.env = env;
        this.nameChecker = env.getConfiguration().getNameChecker();
        language = XSLT_PATTERN;
        t = new Tokenizer();
        try {
	        t.tokenize(pattern, 0, -1, env.getLineNumber());
        } catch (StaticError err) {
            grumble(err.getMessage());
        }
        Pattern pat = parseUnionPattern();
        if (t.currentToken != Token.EOF) {
            grumble("Unexpected token " + currentTokenDisplay() + " beyond end of pattern");
        }
        return pat;
    }

    /**
     * Parse a string representing a sequence type
     *
     * @param input the string, which should conform to the XPath SequenceType
     *      production
     * @param env the static context
     * @throws org.orbeon.saxon.trans.StaticError if any error is encountered
     * @return a SequenceType object representing the type
     */

    public SequenceType parseSequenceType(String input, StaticContext env) throws StaticError {
        this.env = env;
        this.nameChecker = env.getConfiguration().getNameChecker();
        language = SEQUENCE_TYPE;
        t = new Tokenizer();
        try {
            t.tokenize(input, 0, -1, 1);
        } catch (StaticError err) {
            grumble(err.getMessage());
        }
        SequenceType req = parseSequenceType();
        if (t.currentToken != Token.EOF) {
            grumble("Unexpected token " + currentTokenDisplay() + " beyond end of SequenceType");
        }
        return req;
    }


    //////////////////////////////////////////////////////////////////////////////////
    //                     EXPRESSIONS                                              //
    //////////////////////////////////////////////////////////////////////////////////

    /**
     * Parse a top-level Expression:
     * ExprSingle ( ',' ExprSingle )*
     *
     * @throws org.orbeon.saxon.trans.StaticError if the expression contains a syntax error
     * @return the Expression object that results from parsing
     */

    protected Expression parseExpression() throws StaticError {
        Expression exp = parseExprSingle();
        while (t.currentToken == Token.COMMA) {
            nextToken();
            exp = Block.makeBlock(exp, parseExpression());
            setLocation(exp);
        }
        return exp;
    }

    /**
     * Parse an ExprSingle
     *
     * @throws org.orbeon.saxon.trans.StaticError if any error is encountered
     * @return the resulting subexpression
     */

    protected Expression parseExprSingle() throws StaticError {
        switch (t.currentToken) {
            case Token.FOR:
            case Token.LET:             // XQuery only
                return parseForExpression();
            case Token.SOME:
            case Token.EVERY:
                return parseQuantifiedExpression();
            case Token.IF:
                return parseIfExpression();
            case Token.TYPESWITCH:
                return parseTypeswitchExpression();
            case Token.VALIDATE:
            case Token.VALIDATE_STRICT:
            case Token.VALIDATE_LAX:
                return parseValidateExpression();
            case Token.PRAGMA:
                return parseExtensionExpression();

            default:
                return parseOrExpression();
        }
    }

    /**
     * Parse a Typeswitch Expression.
     * This construct is XQuery-only, so the XPath version of this
     * method throws an error unconditionally
     */

    protected Expression parseTypeswitchExpression() throws StaticError {
        grumble("typeswitch is not allowed in XPath");
        return null;
    }

    /**
     * Parse a Validate Expression.
     * This construct is XQuery-only, so the XPath version of this
     * method throws an error unconditionally
     */

    protected Expression parseValidateExpression() throws StaticError {
        grumble("validate{} expressions are not allowed in XPath");
        return null;
    }

    /**
     * Parse an Extension Expression
     * This construct is XQuery-only, so the XPath version of this
     * method throws an error unconditionally
     */

    protected Expression parseExtensionExpression() throws StaticError {
        grumble("extension expressions (#...#) are not allowed in XPath");
        return null;
    }

    /**
     * Parse an OrExpression:
     * AndExpr ( 'or' AndExpr )*
     *
     * @throws org.orbeon.saxon.trans.StaticError if any error is encountered
     * @return the resulting subexpression
     */

    private Expression parseOrExpression() throws StaticError {
        Expression exp = parseAndExpression();
        while (t.currentToken == Token.OR) {
            nextToken();
            exp = new BooleanExpression(exp, Token.OR, parseAndExpression());
            setLocation(exp);
        }
        return exp;
    }

    /**
     * Parse an AndExpr:
     * EqualityExpr ( 'and' EqualityExpr )*
     *
     * @throws org.orbeon.saxon.trans.StaticError if any error is encountered
     * @return the resulting subexpression
     */

    private Expression parseAndExpression() throws StaticError {
        Expression exp = parseComparisonExpression();
        while (t.currentToken == Token.AND) {
            nextToken();
            exp = new BooleanExpression(exp, Token.AND, parseComparisonExpression());
            setLocation(exp);
        }
        return exp;
    }

    /**
     * Parse a FOR expression:
     * for $x in expr (',' $y in expr)* 'return' expr
     *
     * @throws org.orbeon.saxon.trans.StaticError if any error is encountered
     * @return the resulting subexpression
     */

    protected Expression parseForExpression() throws StaticError {
        if (t.currentToken==Token.LET) {
            grumble("'let' is not supported in XPath");
        }
        return parseMappingExpression();
    }

    /**
     * Parse a quantified expression:
     * (some|every) $x in expr 'satisfies' expr
     *
     * @throws org.orbeon.saxon.trans.StaticError if any error is encountered
     * @return the resulting subexpression
     */

    private Expression parseQuantifiedExpression() throws StaticError {
        return parseMappingExpression();
    }

    /**
     * Parse a mapping expression. This is a common routine that handles
     * XPath 'for' expressions and quantified expressions.
     *
     * <p>Syntax: <br/>
     * (for|some|every) $x in expr (',' $y in expr)* (return|satisfies) expr
     * </p>
     *
     * <p>On entry, the current token indicates whether a for, some, or every
     * expression is expected.</p>
     *
     * @throws org.orbeon.saxon.trans.StaticError if any error is encountered
     * @return the resulting subexpression
     */

    protected Expression parseMappingExpression() throws StaticError {
        int offset = t.currentTokenStartOffset;
        int operator = t.currentToken;
        List clauseList = new ArrayList(3);
        do {
            ForClause clause = new ForClause();
            clause.offset = offset;
            clause.requiredType = SequenceType.SINGLE_ITEM;
            clauseList.add(clause);
            nextToken();
            expect(Token.DOLLAR);
            nextToken();
            expect(Token.NAME);
            String var = t.currentTokenValue;

            // declare the range variable
            RangeVariableDeclaration v = new RangeVariableDeclaration();
            v.setNameCode(makeNameCode(var, false));
            v.setRequiredType(SequenceType.SINGLE_ITEM);
            v.setVariableName(var);
            clause.rangeVariable = v;
            nextToken();

            if (isKeyword("as") && "XQuery".equals(getLanguage())) {
                nextToken();
                SequenceType type = parseSequenceType();
                clause.requiredType = type;
                if (type.getCardinality() != StaticProperty.EXACTLY_ONE) {
                    warning("Occurrence indicator on singleton range variable has no effect");
                    type = SequenceType.makeSequenceType(type.getPrimaryType(), StaticProperty.EXACTLY_ONE);
                }
                v.setRequiredType(type);
            }

            // "at" clauses are not recognized in XPath
            clause.positionVariable = null;

            // process the "in" clause
            expect(Token.IN);
            nextToken();
            clause.sequence = parseExprSingle();
            declareRangeVariable(clause.rangeVariable);

        } while (t.currentToken==Token.COMMA);

        // process the "return/satisfies" expression (called the "action")
        if (operator==Token.FOR) {
            expect(Token.RETURN);
        } else {
            expect(Token.SATISFIES);
        }
        nextToken();
        Expression action = parseExprSingle();

        // work back through the list of range variables, fixing up all references
        // to the variables in the inner expression

        final TypeHierarchy th = env.getConfiguration().getTypeHierarchy();
        for (int i = clauseList.size()-1; i>=0; i--) {
            ForClause fc = (ForClause)clauseList.get(i);
            Assignation exp;
            if (operator==Token.FOR) {
                exp = new ForExpression();
            } else {
                exp = new QuantifiedExpression();
                ((QuantifiedExpression)exp).setOperator(operator);
            }
            setLocation(exp, offset);
            exp.setVariableDeclaration(fc.rangeVariable);
            exp.setSequence(fc.sequence);

            // Attempt to give the range variable a more precise type, base on analysis of the
            // "action" expression. This will often be approximate, because variables and function
            // calls in the action expression have not yet been resolved. We rely on the ability
            // of all expressions to return some kind of type information even if this is
            // imprecise.

            if (fc.requiredType == SequenceType.SINGLE_ITEM) {
                SequenceType type = SequenceType.makeSequenceType(
                        fc.sequence.getItemType(th), StaticProperty.EXACTLY_ONE);
                fc.rangeVariable.setRequiredType(type);
            } else {
                fc.rangeVariable.setRequiredType(fc.requiredType);
            }
            exp.setAction(action);

            // for the next outermost "for" clause, the "action" is this ForExpression
            action = exp;
        }

        // undeclare all the range variables

        for (int i = clauseList.size()-1; i>=0; i--) {
            undeclareRangeVariable();
        }
        //action = makeTracer(offset, action, Location.FOR_EXPRESSION, -1);
        return action;
    }


    /**
     * Parse an IF expression:
     * if '(' expr ')' 'then' expr 'else' expr
     *
     * @throws org.orbeon.saxon.trans.StaticError if any error is encountered
     * @return the resulting subexpression
     */

    private Expression parseIfExpression() throws StaticError {
        // left paren already read
        int ifoffset = t.currentTokenStartOffset;
        nextToken();
        Expression condition = parseExpression();
        expect(Token.RPAR);
        nextToken();
        int thenoffset = t.currentTokenStartOffset;
        expect(Token.THEN);
        nextToken();
        Expression thenExp = makeTracer(thenoffset, parseExpression(), Location.THEN_EXPRESSION, -1);
        int elseoffset = t.currentTokenStartOffset;
        expect(Token.ELSE);
        nextToken();
        Expression elseExp = makeTracer(elseoffset, parseExprSingle(), Location.ELSE_EXPRESSION, -1);
        Expression ifExp = new IfExpression(condition, thenExp, elseExp);
        setLocation(ifExp, ifoffset);
        return makeTracer(ifoffset, ifExp, Location.IF_EXPRESSION, -1);
    }

    /**
     * Parse an "instance of"  expression
     * Expr ("instance" "of") SequenceType
     *
     * @throws org.orbeon.saxon.trans.StaticError if any error is encountered
     * @return the resulting subexpression
     */

    private Expression parseInstanceOfExpression() throws StaticError {
        Expression exp = parseTreatExpression();
        if (t.currentToken == Token.INSTANCE_OF) {
            nextToken();
            exp = new InstanceOfExpression(exp, parseSequenceType());
            setLocation(exp);
        }
        return exp;
    }

    /**
     * Parse a "treat as" expression
     * castable-expression ("treat" "as" SequenceType )?
     *
     * @throws org.orbeon.saxon.trans.StaticError if any error is encountered
     * @return the resulting subexpression
     */

    private Expression parseTreatExpression() throws StaticError {
        Expression exp = parseCastableExpression();
        if (t.currentToken == Token.TREAT_AS) {
            nextToken();
            SequenceType target = parseSequenceType();
            exp = TreatExpression.make(exp, target);
            setLocation(exp);
        }
        return exp;
    }

    /**
     * Parse a "castable as" expression
     * Expr "castable as" AtomicType "?"?
     *
     * @throws org.orbeon.saxon.trans.StaticError if any error is encountered
     * @return the resulting subexpression
     */

    private Expression parseCastableExpression() throws StaticError {
        Expression exp = parseCastExpression();
        if (t.currentToken == Token.CASTABLE_AS) {
            nextToken();
            expect(Token.NAME);
            AtomicType at = getAtomicType(t.currentTokenValue);
            if (at.getFingerprint() == StandardNames.XDT_ANY_ATOMIC_TYPE) {
                grumble("No value is castable to xdt:anyAtomicType", "XPST0080");
            }
            if (at.getFingerprint() == StandardNames.XS_NOTATION) {
                grumble("No value is castable to xs:NOTATION", "XPST0080");
            }
            nextToken();
            boolean allowEmpty = (t.currentToken == Token.QMARK);
            if (allowEmpty) {
                nextToken();
            }
            if (at.isNamespaceSensitive()) {
                if (exp instanceof StringValue) {
                    try {
                        String source = ((StringValue)exp).getStringValue();
                        makeNameCode(source, false);
                        exp = BooleanValue.TRUE;
                    } catch (Exception e) {
                        exp = BooleanValue.FALSE;
                    }
                } else {
                    exp = new CastableExpression(exp, at, allowEmpty);
                }
            } else {
                exp = new CastableExpression(exp, at, allowEmpty);
            }
            setLocation(exp);
        }
        return exp;
    }

    /**
     * Parse a "cast as" expression
     * castable-expression ("cast" "as" AtomicType "?"?)
     *
     * @throws org.orbeon.saxon.trans.StaticError if any error is encountered
     * @return the resulting subexpression
     */

    private Expression parseCastExpression() throws StaticError {
        Expression exp = parseUnaryExpression();
        if (t.currentToken == Token.CAST_AS) {
            nextToken();
            expect(Token.NAME);
            AtomicType at = getAtomicType(t.currentTokenValue);
            if (at.getFingerprint() == StandardNames.XDT_ANY_ATOMIC_TYPE) {
                grumble("Cannot cast to xdt:anyAtomicType", "XPST0080");
            }
            if (at.getFingerprint() == StandardNames.XS_NOTATION) {
                grumble("Cannot cast to xs:NOTATION", "XPST0080");
            }
            nextToken();
            boolean allowEmpty = (t.currentToken == Token.QMARK);
            if (allowEmpty) {
                nextToken();
            }
            if (at.isNamespaceSensitive() && exp instanceof StringValue) {
                try {
                    String source = ((StringValue)exp).getStringValue();
                    return CastExpression.castStringToQName(source, at, env);
                } catch (XPathException e) {
                    grumble(e.getMessage(), e.getErrorCodeLocalPart());
                }
            } else {
                exp = new CastExpression(exp, at, allowEmpty);
                try {
                    exp = exp.simplify(env);
                } catch (XPathException err) {
                    throw StaticError.makeStaticError(err);
                }
            }
            setLocation(exp);
        }
        return exp;
    }

    /**
     * Analyze a token whose expected value is the name of an atomic type,
     * and return the object representing the atomic type.
     * @param qname The lexical QName of the atomic type
     * @return The atomic type
     * @throws org.orbeon.saxon.trans.StaticError if the QName is invalid or if no atomic type of that
     * name exists as a built-in type or a type in an imported schema
     */
    private AtomicType getAtomicType(String qname) throws StaticError {
        if (scanOnly) {
            return Type.STRING_TYPE;
        }
        try {
            String[] parts = nameChecker.getQNameParts(qname);
            String uri;
            if ("".equals(parts[0])) {
                short uriCode = env.getDefaultElementNamespace();
                uri = env.getNamePool().getURIFromURICode(uriCode);
            } else {
                try {
                    uri = env.getURIForPrefix(parts[0]);
                } catch (XPathException err) {
                    grumble(err.getMessage(), err.getErrorCodeLocalPart());
                    uri = "";
                }
            }

            boolean builtInNamespace = uri.equals(NamespaceConstant.SCHEMA);

            if (!builtInNamespace && NamespaceConstant.isXDTNamespace(uri)) {
                uri = NamespaceConstant.XDT;
                builtInNamespace = true;
            }

            if (builtInNamespace) {
                ItemType t = Type.getBuiltInItemType(uri, parts[1]);
                if (t == null) {
                    grumble("Unknown atomic type " + qname, "XPST0051");
                }
                if (t instanceof BuiltInAtomicType) {
                    if (t instanceof BuiltInAtomicType && !env.isAllowedBuiltInType((BuiltInAtomicType)t)) {
                        grumble("The type " + qname + " is not recognized by a Basic XSLT Processor. ", "XPST0080");
                    }
                    return (AtomicType)t;
                } else {
                    grumble("The type " + qname + " is not atomic", "XPST0051");
                }
            } else if (uri.equals(NamespaceConstant.JAVA_TYPE)) {
                Class theClass = null;
                try {
                    String className = parts[1].replace('-', '$');
                    theClass = env.getConfiguration().getClass(className, false, null);
                } catch (XPathException err) {
                    grumble("Unknown Java class " + parts[1]);
                }
                return new ExternalObjectType(theClass, env.getConfiguration());
            } else if (uri.equals(NamespaceConstant.DOT_NET_TYPE)) {
                return (AtomicType)env.getConfiguration().getPlatform().getExternalObjectType(uri, parts[1]);
            } else {
                if (env.isImportedSchema(uri)) {
                    int fp = env.getNamePool().getFingerprint(uri, parts[1]);
                    if (fp == -1) {
                        grumble("Unknown type " + qname);
                    }
                    SchemaType st = env.getConfiguration().getSchemaType(fp);
                    if (st == null) {
                        grumble("Unknown atomic type " + qname);
                    } else if (st.isAtomicType()) {
                        return (AtomicType)st;
                    } else if (st.isComplexType()) {
                        grumble("Type (" + qname + ") is a complex type");
                        return null;
                    } else {
                        grumble("Type (" + qname + ") is a list or union type");
                        return null;
                    }

                } else {
                    if ("".equals(uri)) {
                        grumble("There is no imported schema for the null namespace");
                    } else {
                        grumble("There is no imported schema for namespace " + uri);
                    }
                    return null;
                }
            }
            grumble("Unknown atomic type " + qname);
        } catch (QNameException err) {
            grumble(err.getMessage());
        }
        return null;
    }
    /**
     * Parse a ComparisonExpr:<br>
     * RangeExpr ( op RangeExpr )*
     * where op is one of =, <, >, !=, <=, >=,
     * eq, lt, gt, ne, le, ge,
     * is, isnot, <<, >>
     *
     * @throws org.orbeon.saxon.trans.StaticError if any error is encountered
     * @return the resulting subexpression
     */

    private Expression parseComparisonExpression() throws StaticError {
        Expression exp = parseRangeExpression();
        switch (t.currentToken) {
            case Token.IS:
            case Token.PRECEDES:
            case Token.FOLLOWS:
                int op = t.currentToken;
                nextToken();
                exp = new IdentityComparison(exp, op, parseRangeExpression());
                setLocation(exp);
                return exp;

            case Token.EQUALS:
            case Token.NE:
            case Token.LT:
            case Token.GT:
            case Token.LE:
            case Token.GE:
                op = t.currentToken;
                nextToken();
                exp = env.getConfiguration().getOptimizer().makeGeneralComparison(
                        exp, op, parseRangeExpression(), env.isInBackwardsCompatibleMode());
                setLocation(exp);
                return exp;

            case Token.FEQ:
            case Token.FNE:
            case Token.FLT:
            case Token.FGT:
            case Token.FLE:
            case Token.FGE:
                op = t.currentToken;
                nextToken();
                exp = new ValueComparison(exp, op, parseRangeExpression());
                setLocation(exp);
                return exp;

            default:
                return exp;
        }
    }

    /**
     * Parse a RangeExpr:<br>
     * AdditiveExpr ('to' AdditiveExpr )?
     *
     * @throws org.orbeon.saxon.trans.StaticError if any error is encountered
     * @return the resulting subexpression
     */

    private Expression parseRangeExpression() throws StaticError {
        Expression exp = parseAdditiveExpression();
        if (t.currentToken == Token.TO ) {
            nextToken();
            exp = new RangeExpression(exp, Token.TO, parseAdditiveExpression());
            setLocation(exp);
        }
        return exp;
    }



    /**
     * Parse the sequence type production.
     * Provisionally, we use the syntax (QName | node-kind "()") ( "*" | "+" | "?" )?
     * We also allow "element of type QName" and "attribute of type QName"
     * The QName must be the name of a built-in schema-defined data type.
     *
     * @throws org.orbeon.saxon.trans.StaticError if any error is encountered
     * @return the resulting subexpression
     */

    protected SequenceType parseSequenceType() throws StaticError {
        ItemType primaryType;
        if (t.currentToken == Token.NAME) {
            primaryType = getAtomicType(t.currentTokenValue);
            nextToken();
        } else if (t.currentToken == Token.NODEKIND) {
            if (t.currentTokenValue == "item") {
                nextToken();
                expect(Token.RPAR);
                nextToken();
                primaryType = AnyItemType.getInstance();
            } else if (t.currentTokenValue == "empty-sequence") {
                nextToken();
                expect(Token.RPAR);
                nextToken();
                return SequenceType.makeSequenceType(NoNodeTest.getInstance(), StaticProperty.EMPTY);
                // return before trying to read an occurrence indicator
            } else {
                primaryType = parseKindTest();
            }
        } else {
            grumble("Expected type name in SequenceType, found " + Token.tokens[t.currentToken]);
            return null;
        }

        int occurrenceFlag;
        switch (t.currentToken) {
            case Token.STAR:
            case Token.MULT:
                // "*" will be tokenized different ways depending on what precedes it
                occurrenceFlag = StaticProperty.ALLOWS_ZERO_OR_MORE;
                // Make the tokenizer ignore the occurrence indicator when classifying the next token
                t.currentToken = Token.RPAR;
                nextToken();
                break;
            case Token.PLUS:
                occurrenceFlag = StaticProperty.ALLOWS_ONE_OR_MORE;
                // Make the tokenizer ignore the occurrence indicator when classifying the next token
                t.currentToken = Token.RPAR;
                nextToken();
                break;
            case Token.QMARK:
                occurrenceFlag = StaticProperty.ALLOWS_ZERO_OR_ONE;
                // Make the tokenizer ignore the occurrence indicator when classifying the next token
                t.currentToken = Token.RPAR;
                nextToken();
                break;
            default:
                occurrenceFlag = StaticProperty.EXACTLY_ONE;
        }
        return SequenceType.makeSequenceType(primaryType, occurrenceFlag);
    }


    /**
     * Parse an AdditiveExpr:
     * MultiplicativeExpr ( (+|-) MultiplicativeExpr )*
     *
     * @throws org.orbeon.saxon.trans.StaticError if any error is encountered
     * @return the resulting subexpression
     */

    private Expression parseAdditiveExpression() throws StaticError {
        Expression exp = parseMultiplicativeExpression();
        while (t.currentToken == Token.PLUS ||
                t.currentToken == Token.MINUS ) {
            int op = t.currentToken;
            nextToken();
            exp = new ArithmeticExpression(exp, op, parseMultiplicativeExpression());
            setLocation(exp);
        }
        return exp;
    }

    /**
     * Parse a MultiplicativeExpr:<br>
     * UnionExpr ( (*|div|idiv|mod) UnionExpr )*
     *
     * @throws org.orbeon.saxon.trans.StaticError if any error is encountered
     * @return the resulting subexpression
     */

    private Expression parseMultiplicativeExpression() throws StaticError {
        Expression exp = parseUnionExpression();
        while (t.currentToken == Token.MULT ||
                t.currentToken == Token.DIV ||
                t.currentToken == Token.IDIV ||
                t.currentToken == Token.MOD ) {
            int op = t.currentToken;
            nextToken();
            exp = new ArithmeticExpression(exp, op, parseUnionExpression());
            setLocation(exp);
        }
        return exp;
    }

    /**
     * Parse a UnaryExpr:<br>
     * ('+'|'-')* ValueExpr
     * parsed as ('+'|'-')? UnaryExpr
     *
     * @throws org.orbeon.saxon.trans.StaticError if any error is encountered
     * @return the resulting subexpression
     */

    private Expression parseUnaryExpression() throws StaticError {
        Expression exp;
        switch (t.currentToken) {
        case Token.MINUS:
            nextToken();
            exp = new ArithmeticExpression(new IntegerValue(0),
                                          Token.NEGATE,
                                          parseUnaryExpression());
            break;
        case Token.PLUS:
            nextToken();
            // Unary plus: can't ignore it completely, it might be a type error, or it might
            // force conversion to a number which would affect operations such as "=".
            exp = new ArithmeticExpression(new IntegerValue(0),
                                          Token.PLUS,
                                          parseUnaryExpression());
            break;
        case Token.VALIDATE:
        case Token.VALIDATE_STRICT:
        case Token.VALIDATE_LAX:
            exp = parseValidateExpression();
            break;
        case Token.PRAGMA:
            exp = parseExtensionExpression();
            break;
        default:
            exp = parsePathExpression();
        }
        setLocation(exp);
        return exp;
    }

    /**
     * Parse a UnionExpr:<br>
     * IntersectExceptExpr ( "|" | "union" IntersectExceptExpr )*
     *
     * @throws org.orbeon.saxon.trans.StaticError if any error is encountered
     * @return the resulting subexpression
     */

    private Expression parseUnionExpression() throws StaticError {
        Expression exp = parseIntersectExpression();
        while (t.currentToken == Token.UNION ) {
            nextToken();
            exp = new VennExpression(exp, Token.UNION, parseIntersectExpression());
            setLocation(exp);
        }
        return exp;
    }

    /**
     * Parse an IntersectExceptExpr:<br>
     * PathExpr ( ( 'intersect' | 'except') PathExpr )*
     *
     * @throws org.orbeon.saxon.trans.StaticError if any error is encountered
     * @return the resulting subexpression
     */

    private Expression parseIntersectExpression() throws StaticError {
        Expression exp = parseInstanceOfExpression();
        while (t.currentToken == Token.INTERSECT ||
                t.currentToken == Token.EXCEPT ) {
            int op = t.currentToken;
            nextToken();
            exp = new VennExpression(exp, op, parseInstanceOfExpression());
            setLocation(exp);
        }
        return exp;
    }



    /**
     * Test whether the current token is one that can start a RelativePathExpression
     *
     * @return the resulting subexpression
     */

    private boolean atStartOfRelativePath() {
        switch(t.currentToken) {
            case Token.AXIS:
            case Token.AT:
            case Token.NAME:
            case Token.PREFIX:
            case Token.SUFFIX:
            case Token.STAR:
            case Token.NODEKIND:
            case Token.DOT:
            case Token.DOTDOT:
            case Token.FUNCTION:
            case Token.STRING_LITERAL:
            case Token.NUMBER:
            case Token.LPAR:
                return true;
            default:
                return false;
        }
    }

    /**
     * Parse a PathExpresssion. This includes "true" path expressions such as A/B/C, and also
     * constructs that may start a path expression such as a variable reference $name or a
     * parenthesed expression (A|B). Numeric and string literals also come under this heading.
     *
     * @throws org.orbeon.saxon.trans.StaticError if any error is encountered
     * @return the resulting subexpression
     */

    private Expression parsePathExpression() throws StaticError {
        switch (t.currentToken) {
        case Token.SLASH:
            nextToken();
            final RootExpression start = new RootExpression();
            setLocation(start);
            if (atStartOfRelativePath()) {
                //final Expression path = new PathExpression(start, parseRelativePath(null));
                final Expression path = parseRemainingPath(start);
                setLocation(path);
                return path;
            } else {
                return start;
            }

        case Token.SLSL:
            // The logic for absolute path expressions changed in 8.4 so that //A/B/C parses to
            // (((root()/descendant-or-self::node())/A)/B)/C rather than
            // (root()/descendant-or-self::node())/(((A)/B)/C) as previously. This is to allow
            // the subsequent //A optimization to kick in.
            nextToken();
            // add in the implicit descendant-or-self::node() step
//            final RootExpression start2 = new RootExpression();
//            setLocation(start2);
//            final AxisExpression axisExp = new AxisExpression(Axis.DESCENDANT_OR_SELF, null);
//            setLocation(axisExp);
//            final PathExpression pathExp = new PathExpression(axisExp, parseRelativePath(null));
//            setLocation(pathExp);
//            final Expression exp = new PathExpression(start2, pathExp);
//            setLocation(exp);
//            return exp;
            final RootExpression start2 = new RootExpression();
            setLocation(start2);
            final AxisExpression axisExp = new AxisExpression(Axis.DESCENDANT_OR_SELF, null);
            setLocation(axisExp);
            final Expression exp = parseRemainingPath(new PathExpression(start2, axisExp));
            setLocation(exp);
            return exp;
        default:
            return parseRelativePath();
        }

    }


    /**
     * Parse a relative path (a sequence of steps). Called when the current token immediately
     * follows a separator (/ or //), or an implicit separator (XYZ is equivalent to ./XYZ)
     * @throws org.orbeon.saxon.trans.StaticError if any error is encountered
     * @return the resulting subexpression
     */

    protected Expression parseRelativePath() throws StaticError {
        Expression exp = parseStepExpression();
        while (t.currentToken == Token.SLASH ||
                t.currentToken == Token.SLSL ) {
            int op = t.currentToken;
            nextToken();
            Expression next = parseStepExpression();
            if (op == Token.SLASH) {
                exp = new PathExpression(exp, next);
            } else {
                // add implicit descendant-or-self::node() step
                exp = new PathExpression(exp,
                        new PathExpression(new AxisExpression(Axis.DESCENDANT_OR_SELF, null),
                            next));
            }
            setLocation(exp);
        }
        return exp;
    }

    /**
     * Parse the remaining steps of an absolute path expression (one starting in "/" or "//"). Note that the
     * token immediately after the "/" or "//" has already been read, and in the case of "/", it has been confirmed
     * that we have a path expression starting with "/" rather than a standalone "/" expression.
     * @param start the initial implicit expression: root() in the case of "/", root()/descendant-or-self::node in
     * the case of "//"
     * @return the completed path expression
     * @throws StaticError
     */
    protected Expression parseRemainingPath(Expression start) throws StaticError {
            Expression exp = start;
            int op = Token.SLASH;
            while (true) {
                Expression next = parseStepExpression();
                if (op == Token.SLASH) {
                    exp = new PathExpression(exp, next);
                } else {
                    // add implicit descendant-or-self::node() step
                    exp = new PathExpression(exp,
                            new PathExpression(new AxisExpression(Axis.DESCENDANT_OR_SELF, null),
                                next));
                }
                setLocation(exp);
                op = t.currentToken;
                if (op != Token.SLASH && op != Token.SLSL) {
                    break;
                }
                nextToken();
            }
            return exp;
        }


    /**
     * Parse a step (including an optional sequence of predicates)
     *
     * @throws org.orbeon.saxon.trans.StaticError if any error is encountered
     * @return the resulting subexpression
     */

    protected Expression parseStepExpression() throws StaticError {
        Expression step = parseBasicStep();

        // When the filter is applied to an Axis step, the nodes are considered in
        // axis order. In all other cases they are considered in document order
        boolean reverse = (step instanceof AxisExpression) &&
                          Axis.isReverse[((AxisExpression)step).getAxis()] &&
                          ((AxisExpression)step).getAxis() != Axis.SELF;

        while (t.currentToken == Token.LSQB) {
            nextToken();
            Expression predicate = parseExpression();
            expect(Token.RSQB);
            nextToken();
            step = new FilterExpression(step, predicate);
            setLocation(step);
        }
        if (reverse) {
            return new Reverser(step);
        } else {
            return step;
        }
    }

    /**
     * Parse a basic step expression (without the predicates)
     *
     * @throws org.orbeon.saxon.trans.StaticError if any error is encountered
     * @return the resulting subexpression
     */

    private Expression parseBasicStep() throws StaticError {
        switch(t.currentToken) {
        case Token.DOLLAR:
            nextToken();
            expect(Token.NAME);
            String var = t.currentTokenValue;
            nextToken();

            if (scanOnly) {
                return new ContextItemExpression();
                // don't do any semantic checks during a prescan
            }

            int vtest = makeNameCode(var, false) & 0xfffff;

            // See if it's a range variable or a variable in the context
            VariableDeclaration b = findRangeVariable(vtest);
            VariableReference ref;
            if (b != null) {
                ref = new VariableReference(b);
            } else {
                try {
                    ref = env.bindVariable(vtest);
                } catch (StaticError err) {
                    if ("XPST0008".equals(err.getErrorCodeLocalPart())) {
                        // Improve the error message
                        grumble("Variable $" + var + " has not been declared", "XPST0008");
                        ref = null;     // humour the compiler
                    } else {
                        throw err;
                    }
                }
            }
            setLocation(ref);
            return ref;

        case Token.LPAR:
            nextToken();
            if (t.currentToken==Token.RPAR) {
                nextToken();
                return EmptySequence.getInstance();
            }
            Expression seq = parseExpression();
            expect(Token.RPAR);
            nextToken();
            return seq;

        case Token.STRING_LITERAL:
            StringValue literal = makeStringLiteral(t.currentTokenValue);
            nextToken();
            return literal;

        case Token.NUMBER:
            NumericValue number = NumericValue.parseNumber(t.currentTokenValue);
            if (number.isNaN()) {
                grumble("Invalid numeric literal " + Err.wrap(t.currentTokenValue, Err.VALUE));
            }
            nextToken();
            return number;

        case Token.FUNCTION:
            return parseFunctionCall();

        case Token.DOT:
            nextToken();
            Expression cie = new ContextItemExpression();
            setLocation(cie);
            return cie;

        case Token.DOTDOT:
            nextToken();
            Expression pne = new ParentNodeExpression();
            setLocation(pne);
            return pne;

        case Token.NAME:
        case Token.PREFIX:
        case Token.SUFFIX:
        case Token.STAR:
        case Token.NODEKIND:
            byte defaultAxis = Axis.CHILD;
            if (t.currentToken == Token.NODEKIND &&
                    (t.currentTokenValue == "attribute" || t.currentTokenValue == "schema-attribute")) {
                defaultAxis = Axis.ATTRIBUTE;
            }
            AxisExpression ae = new AxisExpression(defaultAxis, parseNodeTest(Type.ELEMENT));
            setLocation(ae);
            return ae;

        case Token.AT:
            nextToken();
            switch(t.currentToken) {

            case Token.NAME:
            case Token.PREFIX:
            case Token.SUFFIX:
            case Token.STAR:
            case Token.NODEKIND:
                AxisExpression ae2 = new AxisExpression(Axis.ATTRIBUTE, parseNodeTest(Type.ATTRIBUTE));
                setLocation(ae2);
                return ae2;

            default:
                grumble("@ must be followed by a NodeTest");
            }
            break;

        case Token.AXIS:
                byte axis;
                try {
                    axis = Axis.getAxisNumber(t.currentTokenValue);
                } catch (StaticError err) {
                    grumble(err.getMessage());
                    axis = Axis.CHILD; // error recovery
                }
                if (axis == Axis.NAMESPACE && language == XQUERY) {
                grumble("The namespace axis is not available in XQuery");
            }
            short principalNodeType = Axis.principalNodeType[axis];
            nextToken();
            switch (t.currentToken) {

            case Token.NAME:
            case Token.PREFIX:
            case Token.SUFFIX:
            case Token.STAR:
            case Token.NODEKIND:
                Expression ax = new AxisExpression(axis, parseNodeTest(principalNodeType));
                setLocation(ax);
                return ax;

            default:
                grumble("Unexpected token " + currentTokenDisplay() + " after axis name");
            }
            break;

        case Token.KEYWORD_CURLY:
        case Token.ELEMENT_QNAME:
        case Token.ATTRIBUTE_QNAME:
        case Token.PI_QNAME:
        case Token.TAG:
            return parseConstructor();

        default:
            grumble("Unexpected token " + currentTokenDisplay() + " in path expression");
            //break;
        }
        return null;
    }

    /**
     * Method to make a string literal from a token identified as a string
     * literal. This is trivial in XPath, but in XQuery the method is overridden
     * to identify pseudo-XML character and entity references. Note that the job of handling
     * doubled string delimiters is done by the tokenizer.
     * @param currentTokenValue
     * @return The string value of the string literal
     */

    protected StringValue makeStringLiteral(String currentTokenValue) throws StaticError {
        return StringValue.makeStringValue(currentTokenValue);
    }

    /**
     * Parse a node constructor. This is allowed only in XQuery, so the method throws
     * an error for XPath.
     */

    protected Expression parseConstructor() throws StaticError {
        grumble("Node constructor expressions are allowed only in XQuery, not in XPath");
        return null;
    }

    /**
     * Parse a NodeTest.
     * One of QName, prefix:*, *:suffix, *, text(), node(), comment(), or
     * processing-instruction(literal?), or element(~,~), attribute(~,~), etc.
     *
     * @throws org.orbeon.saxon.trans.StaticError if any error is encountered
     * @param nodeType the node type being sought if one is specified
     * @return the resulting NodeTest object
     */

    protected NodeTest parseNodeTest(short nodeType) throws StaticError {
        int tok = t.currentToken;
        String tokv = t.currentTokenValue;
        switch (tok) {
        case Token.NAME:
            nextToken();
            return makeNameTest(nodeType, tokv, nodeType==Type.ELEMENT);

        case Token.PREFIX:
            nextToken();
        	return makeNamespaceTest(nodeType, tokv);

        case Token.SUFFIX:
            nextToken();
            tokv = t.currentTokenValue;
            expect(Token.NAME);
            nextToken();
        	return makeLocalNameTest(nodeType, tokv);

        case Token.STAR:
            nextToken();
            return NodeKindTest.makeNodeKindTest(nodeType);

        case Token.NODEKIND:
            return parseKindTest();

        default:
            grumble("Unrecognized node test");
            return null;
        }
    }

    /**
     * Parse a KindTest
     */

    private NodeTest parseKindTest() throws StaticError {
        String typeName = t.currentTokenValue;
        boolean schemaDeclaration = (typeName.startsWith("schema-"));
        int primaryType = getSystemType(typeName);
        int nameCode = -1;
        int contentType;
        boolean empty = false;
        nextToken();
        if (t.currentToken == Token.RPAR) {
            if (schemaDeclaration) {
                grumble("schema-element() and schema-attribute() require a name to be supplied");
                return null;
            }
            empty = true;
            nextToken();
        }
        switch (primaryType) {
            case Type.ITEM:
                grumble("item() is not allowed in a path expression");
                return null;
            case Type.NODE:
                if (empty) {
                    return AnyNodeTest.getInstance();
                } else {
                    grumble("No arguments are allowed in node()");
                    return null;
                }
            case Type.TEXT:
                if (empty) {
                    return NodeKindTest.TEXT;
                } else {
                    grumble("No arguments are allowed in text()");
                    return null;
                }
            case Type.COMMENT:
                if (empty) {
                    return NodeKindTest.COMMENT;
                } else {
                    grumble("No arguments are allowed in comment()");
                    return null;
                }
            case Type.NAMESPACE:
                grumble("No node test is defined for namespace nodes");
                return null;
            case Type.DOCUMENT:
                if (empty) {
                    return NodeKindTest.DOCUMENT;
                } else {
                    int innerType;
                    try {
                        innerType = getSystemType(t.currentTokenValue);
                    } catch (XPathException err) {
                        innerType = Type.ITEM;
                    }
                    if (innerType != Type.ELEMENT) {
                        grumble("Argument to document-node() must be an element type descriptor");
                        return null;
                    }
                    NodeTest inner = parseKindTest();
                    expect(Token.RPAR);
                    nextToken();
                    return new DocumentNodeTest(inner);
                }
            case Type.PROCESSING_INSTRUCTION:
                if (empty) {
                    return NodeKindTest.PROCESSING_INSTRUCTION;
                } else if (t.currentToken == Token.STRING_LITERAL) {
                    try {
                        String[] parts = nameChecker.getQNameParts(t.currentTokenValue);
                        if ("".equals(parts[0])) {
                            nameCode = makeNameCode(parts[1], false);
                        } else {
                            warning("No processing instruction name will ever contain a colon");
                            nameCode = env.getNamePool().allocate("prefix", "http://saxon.sf.net/ nonexistent namespace", "___invalid-name");
                        }
                    } catch (QNameException e) {
                        warning("No processing instruction will ever be named '" +
                                t.currentTokenValue + "'. " + e.getMessage());
                        nameCode = env.getNamePool().allocate("prefix", "http://saxon.sf.net/ nonexistent namespace", "___invalid-name");
                    }
                } else if (t.currentToken == Token.NAME) {
                    try {
                        String[] parts = nameChecker.getQNameParts(t.currentTokenValue);
                        if ("".equals(parts[0])) {
                            nameCode = makeNameCode(parts[1], false);
                        } else {
                            grumble("Processing instruction name must not contain a colon");
                        }
                    } catch (QNameException e) {
                        grumble("Invalid processing instruction name. " + e.getMessage());
                    }
                } else {
                    grumble("Processing instruction name must be a QName or a string literal");
                }
                nextToken();
                expect(Token.RPAR);
                nextToken();
                return new NameTest(Type.PROCESSING_INSTRUCTION, nameCode, env.getNamePool());

            case Type.ATTRIBUTE:
                // drop through

            case Type.ELEMENT:
                String nodeName = "";
                if (empty) {
                    return NodeKindTest.makeNodeKindTest(primaryType);
                } else if (t.currentToken == Token.STAR || t.currentToken == Token.MULT) {
                    // allow for both representations of "*" to be safe
                    if (schemaDeclaration) {
                        grumble("schema-element() and schema-attribute() must specify an actual name, not '*'");
                        return null;
                    }
                    nameCode = -1;
                } else if (t.currentToken == Token.NAME) {
                    nodeName = t.currentTokenValue;
                    nameCode = makeNameCode(t.currentTokenValue, primaryType == Type.ELEMENT); // & 0xfffff;
                } else {
                    grumble("Unexpected " + Token.tokens[t.currentToken] + " after '(' in SequenceType");
                }
                String suri = null;
                if (nameCode != -1) {
                    suri = env.getNamePool().getURI(nameCode);
                }
                nextToken();
                if (t.currentToken == Token.RPAR) {
                    nextToken();
                    if (nameCode == -1) {
                        // element(*) or attribute(*)
                        return NodeKindTest.makeNodeKindTest(primaryType);
                    } else {
                        NodeTest nameTest = null;
                        SchemaType schemaType = null;
                        if (primaryType == Type.ATTRIBUTE) {
                            // attribute(N) or schema-attribute(N)
                            if (schemaDeclaration) {
                                // schema-attribute(N)
                                SchemaDeclaration attributeDecl =
                                        env.getConfiguration().getAttributeDeclaration(nameCode & 0xfffff);
                                if (!env.isImportedSchema(suri)) {
                                    grumble("No schema has been imported for namespace '" + suri + '\'');
                                }
                                if (attributeDecl == null) {
                                    grumble("There is no declaration for attribute @" + nodeName + " in an imported schema");
                                } else {
                                    schemaType = attributeDecl.getType();
                                    nameTest = new NameTest(Type.ATTRIBUTE, nameCode, env.getNamePool());
                                }
                            } else {
                                nameTest = new NameTest(Type.ATTRIBUTE, nameCode, env.getNamePool());
                                return nameTest;
                            }
                        } else {
                            // element(N) or schema-element(N)
                            if (schemaDeclaration) {
                                // schema-element(N)
                                if (!env.isImportedSchema(suri)) {
                                    grumble("No schema has been imported for namespace '" + suri + '\'');
                                }
                                SchemaDeclaration elementDecl =
                                        env.getConfiguration().getElementDeclaration(nameCode & 0xfffff);
                                if (elementDecl == null) {
                                    grumble("There is no declaration for element <" + nodeName + "> in an imported schema");
                                } else {
                                    schemaType = elementDecl.getType();
                                    nameTest = elementDecl.makeSchemaNodeTest();

                                }
                            } else {
                                nameTest = new NameTest(Type.ELEMENT, nameCode, env.getNamePool());
                                return nameTest;
                            }
                        }
                        ContentTypeTest contentTest = null;
                        if (schemaType != null) {
                            contentTest = new ContentTypeTest(primaryType, schemaType, env.getConfiguration());
                        }

                        if (contentTest == null) {
                            return nameTest;
                        } else {
                            CombinedNodeTest combo = new CombinedNodeTest(nameTest, Token.INTERSECT, contentTest);
                            combo.setGlobalComponentTest(schemaDeclaration);
                            return combo;
                        }
                    }
                } else if (t.currentToken == Token.COMMA) {
                    if (schemaDeclaration) {
                        grumble("schema-element() and schema-attribute() must have one argument only");
                        return null;
                    }
                    nextToken();
                    NodeTest result;
                    if (t.currentToken == Token.STAR) {
                        grumble("'*' is no longer permitted as the second argument of element() and attribute()");
                        return null;
                    } else if (t.currentToken == Token.NAME) {
                        SchemaType schemaType;
                        contentType = makeNameCode(t.currentTokenValue, true) & 0xfffff;
                        String uri = env.getNamePool().getURI(contentType);
                        String lname = env.getNamePool().getLocalName(contentType);
                        if (NamespaceConstant.isXDTNamespace(uri)) {
                            // TODO: delete temporary code to handle old XDT namespaces
                            uri = NamespaceConstant.SCHEMA;
                            contentType = env.getNamePool().getFingerprint(uri, lname);
                        }

                        if (uri.equals(NamespaceConstant.SCHEMA)) {
                            schemaType = env.getConfiguration().getSchemaType(contentType);
                        } else {
                            if (!env.isImportedSchema(uri)) {
                                grumble("No schema has been imported for namespace '" + uri + '\'');
                            }
                            schemaType = env.getConfiguration().getSchemaType(contentType);
                        }
                        if (schemaType == null) {
                            grumble("Unknown type name " + lname);
                        }
                        if (primaryType == Type.ATTRIBUTE && schemaType.isComplexType()) {
                            grumble("An attribute cannot have a complex type");
                        }
                        ContentTypeTest typeTest = new ContentTypeTest(primaryType, schemaType, env.getConfiguration());
                        if (nameCode == -1) {
                            // this represents element(*,T) or attribute(*,T)
                            result = typeTest;
                            if (primaryType == Type.ATTRIBUTE) {
                                nextToken();
                            } else {
                                // assert (primaryType == Type.ELEMENT);
                                nextToken();
                                if (t.currentToken == Token.QMARK) {
                                     typeTest.setNillable(true);
                                     nextToken();
                                }
                            }
                        } else {
                            if (primaryType == Type.ATTRIBUTE) {
                                NodeTest nameTest = new NameTest(Type.ATTRIBUTE, nameCode, env.getNamePool());
                                result = new CombinedNodeTest(nameTest, Token.INTERSECT, typeTest);
                                nextToken();
                            } else {
                                // assert (primaryType == Type.ELEMENT);
                                NodeTest nameTest = new NameTest(Type.ELEMENT, nameCode, env.getNamePool());
                                result = new CombinedNodeTest(nameTest, Token.INTERSECT, typeTest);
                                nextToken();
                                if (t.currentToken == Token.QMARK) {
                                     typeTest.setNillable(true);
                                     nextToken();
                                }
                            }
                        }
                    } else {
                        grumble("Unexpected " + Token.tokens[t.currentToken] + " after ',' in SequenceType");
                        return null;
                    }

                    expect(Token.RPAR);
                    nextToken();
                    return result;
                } else {
                    grumble("Expected ')' or ',' in SequenceType");
                }
                return null;
            default:
                // can't happen!
                grumble("Unknown node kind");
                return null;
        }
    }

    /**
     * Get a system type - that is, one whose name is a keyword rather than a QName. This includes the node
     * kinds such as element and attribute, the generic types node() and item(), and the pseudo-type empty-sequence()
     *
     * @param name
     * @exception org.orbeon.saxon.trans.StaticError
     */
    private static int getSystemType(String name) throws StaticError {
        if ("item".equals(name))                   return Type.ITEM;
        else if ("document-node".equals(name))     return Type.DOCUMENT;
        else if ("element".equals(name))           return Type.ELEMENT;
        else if ("schema-element".equals(name))    return Type.ELEMENT;
        else if ("attribute".equals(name))         return Type.ATTRIBUTE;
        else if ("schema-attribute".equals(name))  return Type.ATTRIBUTE;
        else if ("text".equals(name))              return Type.TEXT;
        else if ("comment".equals(name))           return Type.COMMENT;
        else if ("processing-instruction".equals(name))
                                                   return Type.PROCESSING_INSTRUCTION;
        else if ("namespace".equals(name))         return Type.NAMESPACE;
        else if ("node".equals(name))              return Type.NODE;
        else throw new StaticError("Unknown type " + name);
    }

    /**
     * Parse a function call.
     * function-name '(' ( Expression (',' Expression )* )? ')'
     *
     * @throws org.orbeon.saxon.trans.StaticError if any error is encountered
     * @return the resulting subexpression
     */

    protected Expression parseFunctionCall() throws StaticError {

        String fname = t.currentTokenValue;
        int offset = t.currentTokenStartOffset;
        ArrayList args = new ArrayList(10);

        // the "(" has already been read by the Tokenizer: now parse the arguments

        nextToken();
        if (t.currentToken!=Token.RPAR) {
            Expression arg = parseExprSingle();
            args.add(arg);
            while(t.currentToken==Token.COMMA) {
                nextToken();
                arg = parseExprSingle();
                args.add(arg);
            }
            expect(Token.RPAR);
        }
        nextToken();

        if (scanOnly) {
            return StringValue.EMPTY_STRING;
        }

        Expression[] arguments = new Expression[args.size()];
        args.toArray(arguments);

        String[] parts;
        try {
            parts = nameChecker.getQNameParts(fname);
        } catch (QNameException e) {
            grumble("Unknown prefix in function name " + fname + "()");
            return null;
        }
        String uri;
        if ("".equals(parts[0])) {
            uri = env.getDefaultFunctionNamespace();
        } else {
            try {
                uri = env.getURIForPrefix(parts[0]);
            } catch (XPathException err) {
                grumble(err.getMessage());
                return null;
            }
        }
        int nameCode = env.getNamePool().allocate(parts[0], uri, parts[1]);
        if (uri.equals(NamespaceConstant.SCHEMA)) {
            ItemType t = Type.getBuiltInItemType(uri, parts[1]);
            if (t instanceof BuiltInAtomicType && !env.isAllowedBuiltInType((BuiltInAtomicType)t)) {
                grumble("The type " + fname + " is not recognized by a Basic XSLT Processor. ", "XPST0080");
            }
        }
        Expression fcall;
        try {
            fcall = env.getFunctionLibrary().bind(nameCode, uri, parts[1], arguments);
        } catch (XPathException err) {
            if (err.getErrorCodeLocalPart() == null) {
                err.setErrorCode("XPST0017");
            }
            grumble(err.getMessage(), err.getErrorCodeLocalPart());
            return null;
        }
        if (fcall == null) {
            String msg = "Cannot find a matching " + arguments.length +
                    "-argument function named " + env.getNamePool().getClarkName(nameCode) + "()";
            if (!env.getConfiguration().isAllowExternalFunctions()) {
                msg += ". Note: external function calls have been disabled";
            }
            if (env.isInBackwardsCompatibleMode()) {
                // treat this as a dynamic error to be reported only if the function call is executed
                DynamicError err = new DynamicError(msg);
                ErrorExpression exp = new ErrorExpression(err);
                setLocation(exp);
                return exp;
            }
            grumble(msg);
        }
        //  A QName or NOTATION constructor function must be evaluated now, while we know the namespace context
        if (fcall instanceof CastExpression &&
                ((AtomicType)fcall.getItemType(env.getConfiguration().getTypeHierarchy())).isNamespaceSensitive() &&
                arguments[0] instanceof StringValue) {
            try {
                return CastExpression.castStringToQName(((StringValue)arguments[0]).getStringValue(), 
                        (AtomicType)((CastExpression)fcall).getItemType(env.getConfiguration().getTypeHierarchy()),
                        env);
            } catch (XPathException e) {
                grumble(e.getMessage(), e.getErrorCodeLocalPart());
            }
        }
        // There are special rules for certain functions appearing in a pattern
        if (language == XSLT_PATTERN) {
            if (fcall instanceof RegexGroup) {
                return EmptySequence.getInstance();
            } else if (fcall instanceof CurrentGroup) {
                String errorCode = "XTSE1060";
                String function = ((CurrentGroup)fcall).getDisplayName(env.getNamePool());
                if (function.equals("current-grouping-key")) {
                    errorCode = "XTSE1070";
                }
                grumble("The " + Err.wrap(function, Err.FUNCTION) + " function cannot be used in a pattern",
                        errorCode);
            }
        }
        setLocation(fcall, offset);
        for (int a=0; a<arguments.length; a++) {
            ((ComputedExpression)fcall).adoptChildExpression(arguments[a]);
        }
        return makeTracer(offset, fcall, Location.FUNCTION_CALL, nameCode);

    }


    //////////////////////////////////////////////////////////////////////////////////
    // Routines for handling range variables
    //////////////////////////////////////////////////////////////////////////////////

    /**
     * Declare a range variable (record its existence within the parser).
     * A range variable is a variable declared within an expression, as distinct
     * from a variable declared in the context.
     *
     * @param declaration the VariableDeclaration to be added to the stack
     * @throws org.orbeon.saxon.trans.StaticError if any error is encountered
     */

    protected void declareRangeVariable(VariableDeclaration declaration) throws StaticError {
        if (rangeVariables == null) {
            rangeVariables = new Stack();
        }
        rangeVariables.push(declaration);
    }

    /**
     * Note when the most recently declared range variable has gone out of scope
     */

    protected void undeclareRangeVariable() {
        rangeVariables.pop();
    }

    /**
     * Locate a range variable with a given name. (By "range variable", we mean a
     * variable declared within the expression where it is used.)
     *
     * @param fingerprint identifies the name of the range variable within the
     *      name pool
     * @return null if not found (this means the variable is probably a
     *     context variable); otherwise the relevant VariableDeclaration
     */

    private VariableDeclaration findRangeVariable(int fingerprint) {
        if (rangeVariables==null) {
            return null;
        }
        for (int v=rangeVariables.size()-1; v>=0; v--) {
            VariableDeclaration b = (VariableDeclaration)rangeVariables.elementAt(v);
            if ((b.getNameCode() & 0xfffff) == fingerprint) {
                return b;
            }
        }
        return null;  // not an in-scope range variable
    }

    /**
     * Get the range variable stack. Used when parsing a nested subexpression
     * inside an attribute constructor
     */

    public Stack getRangeVariableStack() {
        return rangeVariables;
    }

    /**
     * Set the range variable stack. Used when parsing a nested subexpression
     * inside an attribute constructor.
     */

    public void setRangeVariableStack(Stack stack) {
        rangeVariables = stack;
    }

    //////////////////////////////////////////////////////////////////////////////////
    //                     PATTERNS                                                 //
    //////////////////////////////////////////////////////////////////////////////////


    /**
     * Parse a Union Pattern:<br>
     * pathPattern ( | pathPattern )*
     *
     * @throws org.orbeon.saxon.trans.StaticError if any error is encountered
     * @return the pattern that results from parsing
     */

    private Pattern parseUnionPattern() throws StaticError {
        Pattern exp1 = parsePathPattern();

        while (t.currentToken == Token.UNION ) {
            if (t.currentTokenValue == "union") {
                grumble("Union operator in a pattern must be written as '|'");
            }
            nextToken();
            Pattern exp2 = parsePathPattern();
            exp1 = new UnionPattern(exp1, exp2);
        }

        return exp1;
    }

    /**
     * Parse a Location Path Pattern:
     *
     * @throws org.orbeon.saxon.trans.StaticError if any error is encountered
     * @return the pattern that results from parsing
     */

    private Pattern parsePathPattern() throws StaticError {

        Pattern prev = null;
        int connector = -1;
        boolean rootonly = false;

        // special handling of stuff before the first component

        switch(t.currentToken) {
            case Token.SLASH:
                connector = t.currentToken;
                nextToken();
                prev = new NodeTestPattern(NodeKindTest.makeNodeKindTest(Type.DOCUMENT));
                rootonly = true;
                break;
            case Token.SLSL:            // leading double slash can't be ignored
                                            // because it changes the default priority
                connector = t.currentToken;
                nextToken();
                prev = new NodeTestPattern(NodeKindTest.makeNodeKindTest(Type.DOCUMENT));
                rootonly = false;
                break;
            default:
                break;
        }

        while(true) {
            Pattern pat = null;
            switch(t.currentToken) {
                case Token.AXIS:
                    if ("child".equals(t.currentTokenValue)) {
                        nextToken();
                        pat = parsePatternStep(Type.ELEMENT);
                    } else if ("attribute".equals(t.currentTokenValue)) {
                        nextToken();
                        pat = parsePatternStep(Type.ATTRIBUTE);
                    } else {
                        grumble("Axis in pattern must be child or attribute");
                    }
                    break;

                case Token.STAR:
                case Token.NAME:
                case Token.PREFIX:
                case Token.SUFFIX:
                    pat = parsePatternStep(Type.ELEMENT);
                    break;

                case Token.NODEKIND:
                    pat = parsePatternStep((t.currentTokenValue=="attribute" || t.currentTokenValue=="schema-attribute") 
                                                ? Type.ATTRIBUTE : Type.ELEMENT);
                    break;

                case Token.AT:
                    nextToken();
                    pat = parsePatternStep(Type.ATTRIBUTE);
                    break;

                case Token.FUNCTION:        // must be id(literal) or key(literal,literal)
                    if (prev!=null) {
                        grumble("Function call may appear only at the start of a pattern");
                    }
                    if ("id".equals(t.currentTokenValue)) {
                        nextToken();
                        Expression idValue = null;
                        if (t.currentToken == Token.STRING_LITERAL) {
                            idValue = new StringValue(t.currentTokenValue);
                        } else if (t.currentToken == Token.DOLLAR) {
                            nextToken();
                            expect(Token.NAME);
                            int varNameCode = makeNameCode(t.currentTokenValue, false) & 0xfffff;
                            idValue = env.bindVariable(varNameCode);
                        } else {
                            grumble("id value in pattern must be either a literal or a variable reference");
                        }
                        pat = new IDPattern(idValue);
                        nextToken();
                        expect(Token.RPAR);
                        nextToken();
                    } else if ("key".equals(t.currentTokenValue)) {
                        nextToken();
                        expect(Token.STRING_LITERAL);
                        String keyname = t.currentTokenValue;
                        nextToken();
                        expect(Token.COMMA);
                        nextToken();
                        Expression idValue = null;
                        if (t.currentToken == Token.STRING_LITERAL) {
                            idValue = new StringValue(t.currentTokenValue);
                        } else if (t.currentToken == Token.DOLLAR) {
                            nextToken();
                            expect(Token.NAME);
                            int varNameCode = makeNameCode(t.currentTokenValue, false) & 0xfffff;
                            idValue = env.bindVariable(varNameCode);
                        } else {
                            grumble("key value must be either a literal or a variable reference");
                        }
                        pat = new KeyPattern(makeNameCode(keyname, false),
                        						idValue);
                        nextToken();
                        expect(Token.RPAR);
                        nextToken();
                    } else {
                        grumble("The only functions allowed in a pattern are id() and key()");
                    }
                    break;

                default:
                    if (rootonly) {     // the pattern was plain '/'
                        return prev;
                    }
                    grumble("Unexpected token in pattern, found " + currentTokenDisplay());
            }

            if (prev != null) {
                if (connector==Token.SLASH) {
                     ((LocationPathPattern)pat).parentPattern = prev;
                } else {                        // connector == SLSL
                     ((LocationPathPattern)pat).ancestorPattern = prev;
                }
            }
            connector = t.currentToken;
            rootonly = false;
            if (connector == Token.SLASH || connector == Token.SLSL) {
                prev = pat;
                nextToken();
            } else {
                return pat;
            }
        }
    }

    /**
     * Parse a pattern step (after any axis name or @)
     *
     * @throws org.orbeon.saxon.trans.StaticError if any error is encountered
     * @param principalNodeType is ELEMENT if we're on the child axis, ATTRIBUTE for
     *     the attribute axis
     * @return the pattern that results from parsing
     */

    private Pattern parsePatternStep(short principalNodeType) throws StaticError {
        LocationPathPattern step = new LocationPathPattern();
        NodeTest test = parseNodeTest(principalNodeType);
        if (test instanceof AnyNodeTest) {
            // handle node() and @node() specially
            if (principalNodeType == Type.ELEMENT) {
                // this means we're on the CHILD axis
                test = AnyChildNodePattern.getInstance();
            } else {
                // we're on the attribute axis
                test = NodeKindTest.makeNodeKindTest(principalNodeType);
            }
        }

        // Deal with nonsense patterns such as @comment() or child::attribute(). These
        // are legal, but will never match anything.

        int kind = test.getPrimitiveType();
        if (principalNodeType == Type.ELEMENT &&
                (kind == Type.ATTRIBUTE || kind == Type.NAMESPACE)) {
            test = NoNodeTest.getInstance();
        } else if (principalNodeType == Type.ATTRIBUTE &&
                (kind == Type.COMMENT || kind == Type.TEXT ||
                kind == Type.PROCESSING_INSTRUCTION || kind == Type.ELEMENT ||
                kind == Type.DOCUMENT)) {
            test = NoNodeTest.getInstance();
        }

        step.nodeTest = test;
        parseFilters(step);
        return step;
    }

    /**
     * Test to see if there are filters for a Pattern, if so, parse them
     *
     * @param path the LocationPathPattern to which the filters are to be
     *     added
     * @throws org.orbeon.saxon.trans.StaticError if any error is encountered
     */

    private void parseFilters(LocationPathPattern path) throws StaticError {
        while (t.currentToken == Token.LSQB) {
            nextToken();
            Expression qual = parseExpression();
            expect(Token.RSQB);
            nextToken();
            path.addFilter(qual);
        }
    }

    // Helper methods to access the static context

    /**
     * Make a NameCode, using this Element as the context for namespace resolution
     *
     * @throws org.orbeon.saxon.trans.StaticError if the name is invalid, or the prefix
     *     undeclared
     * @param qname The name as written, in the form "[prefix:]localname"
     * @param useDefault Defines the action when there is no prefix. If
     *      true, use the default namespace URI for element names. If false,
     *     use no namespace URI (as for attribute names).
     * @return the namecode, which can be used to identify this name in the
     *     name pool
     */

    public final int makeNameCode(String qname, boolean useDefault) throws StaticError {
        if (scanOnly) {
            return -1;
        }
        try {
            String[] parts = nameChecker.getQNameParts(qname);
            String prefix = parts[0];
            if ("".equals(prefix)) {
                short uricode = 0;
                if (useDefault) {
                    uricode = env.getDefaultElementNamespace();
                }
                return env.getNamePool().allocate(prefix, uricode, qname);
            } else {
                try {
                    String uri = env.getURIForPrefix(prefix);
                    return env.getNamePool().allocate(prefix, uri, parts[1]);
                } catch (XPathException err) {
                    grumble(err.getMessage(), err.getErrorCodeLocalPart());
                    return -1;
                }
            }
        } catch (QNameException e) {
            //throw new XPathException.Static(e.getMessage());
            grumble(e.getMessage());
            return -1;
        }
    }

	/**
	 * Make a NameTest, using the static context for namespace resolution
	 *
	 * @param nodeType the type of node required (identified by a constant in
	 *     class Type)
	 * @param qname the lexical QName of the required node
	 * @param useDefault true if the default namespace should be used when
	 *     the QName is unprefixed
	 * @throws org.orbeon.saxon.trans.StaticError if the QName is invalid
	 * @return a NameTest, representing a pattern that tests for a node of a
	 *     given node kind and a given name
	 */

	public NameTest makeNameTest(short nodeType, String qname, boolean useDefault)
		    throws StaticError {
        int nameCode = makeNameCode(qname, useDefault);
		NameTest nt = new NameTest(nodeType, nameCode, env.getNamePool());
		//nt.setOriginalText(qname);
		return nt;
	}

	/**
	 * Make a NamespaceTest (name:*)
	 *
	 * @param nodeType integer code identifying the type of node required
	 * @param prefix the namespace prefix
	 * @throws org.orbeon.saxon.trans.StaticError if the namespace prefix is not declared
	 * @return the NamespaceTest, a pattern that matches all nodes in this
	 *     namespace
	 */

	public NamespaceTest makeNamespaceTest(short nodeType, String prefix)
			throws StaticError {
        if (scanOnly) {
            // return an arbitrary namespace if we're only doing a syntax check
            return new NamespaceTest(env.getNamePool(), nodeType, NamespaceConstant.SAXON);
        }

        try {
            NamespaceTest nt = new NamespaceTest(env.getNamePool(), nodeType, env.getURIForPrefix(prefix));
            //nt.setOriginalText(prefix + ":*");
            return nt;
        } catch (XPathException e) {
            // env.getURIForPrefix can return a dynamic error
            grumble(e.getMessage(), "XPST0081");
            return null;
        }
    }

	/**
	 * Make a LocalNameTest (*:name)
	 *
	 * @param nodeType the kind of node to be matched
	 * @param localName the requred local name
	 * @throws org.orbeon.saxon.trans.StaticError if the local name is invalid
	 * @return a LocalNameTest, a pattern which matches all nodes of a given
	 *     local name, regardless of namespace
	 */

	public LocalNameTest makeLocalNameTest(short nodeType, String localName)
			throws StaticError {
        if (!nameChecker.isValidNCName(localName)) {
            grumble("Local name [" + localName + "] contains invalid characters");
        }
		return new LocalNameTest(env.getNamePool(), nodeType, localName);
    }

    /**
     * Set location information on an expression. At present this consists of a simple
     * line number. Needed mainly for XQuery.
     */

    protected void setLocation(Expression exp) {
        if (exp instanceof ComputedExpression) {
            setLocation(exp, t.currentTokenStartOffset);
        }
    }

    /**
     * Set location information on an expression. At present only the line number
     * is retained. Needed mainly for XQuery. This version of the method supplies an
     * explicit offset (character position within the expression or query), which the tokenizer
     * can convert to a line number and column number.
     */

    protected void setLocation(Expression exp, int offset) {
        // TODO: we are losing the column position and retaining only the line number
        int line = t.getLineNumber(offset);
        if (exp instanceof ComputedExpression && ((ComputedExpression)exp).getLocationId()==-1) {
            int loc = env.getLocationMap().allocateLocationId(env.getSystemId(), line);
            ComputedExpression cexp = (ComputedExpression)exp;
            cexp.setLocationId(loc);
            // add a temporary container to provide location information
            if (cexp.getParentExpression() == null) {
                TemporaryContainer container = new TemporaryContainer(env.getLocationMap(), loc);
                cexp.setParentExpression(container);
            }
        }
    }

    /**
     * If tracing, wrap an instruction in a trace instruction
     */

    protected Expression makeTracer(int startOffset, Expression exp, int construct, int objectNameCode) {
        if (env.getConfiguration().isCompileWithTracing()) {
            TraceExpression trace = new TraceExpression(exp);
            long lc = t.getLineAndColumn(startOffset);
            trace.setLineNumber((int)(lc>>32));
            trace.setColumnNumber((int)(lc&0x7fffffff));
            trace.setSystemId(env.getSystemId());
            trace.setNamespaceResolver(env.getNamespaceResolver());
            trace.setConstructType(construct);
            trace.setObjectNameCode(objectNameCode);
            return trace;
        } else {
            return exp;
        }
    }

    /**
     * Test whether the current token is a given keyword.
     * @param s     The string to be compared with the current token
     * @return true if they are the same
     */

    protected boolean isKeyword(String s) {
        return (t.currentToken == Token.NAME && t.currentTokenValue.equals(s));
    }

    public void setScanOnly(boolean scanOnly) {
        this.scanOnly = scanOnly;
    }

    public static class ForClause {

        public RangeVariableDeclaration rangeVariable;
        public RangeVariableDeclaration positionVariable;
        public Expression sequence;
        public SequenceType requiredType;
        public int offset;
    }

    protected static class TemporaryContainer implements Container, LocationProvider, Serializable {
        private LocationMap map;
        private int locationId;

        public TemporaryContainer(LocationMap map, int locationId) {
            this.map = map;
            this.locationId = locationId;
        }

        public Executable getExecutable() {
            return null;
        }

        public LocationProvider getLocationProvider() {
            return map;
        }

        public String getPublicId() {
            return null;
        }

        public String getSystemId() {
            return map.getSystemId(locationId);
        }

        public int getLineNumber() {
            return map.getLineNumber(locationId);
        }

        public int getColumnNumber() {
            return -1;
        }

        public String getSystemId(int locationId) {
            return getSystemId();
        }

        public int getLineNumber(int locationId) {
            return getLineNumber();
        }

        /**
         * Get the host language (XSLT, XQuery, XPath) used to implement the code in this container
         * @return typically {@link org.orbeon.saxon.Configuration#XSLT} or {@link org.orbeon.saxon.Configuration#XQUERY}
         */

        public int getHostLanguage() {
            return Configuration.XPATH;
        }

        /**
          * Replace one subexpression by a replacement subexpression
          * @param original the original subexpression
          * @param replacement the replacement subexpression
          * @return true if the original subexpression is found
          */

        public boolean replaceSubExpression(Expression original, Expression replacement) {
            // overridden in subclasses
            throw new IllegalArgumentException("Invalid replacement");
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
