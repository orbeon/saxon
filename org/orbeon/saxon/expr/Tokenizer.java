package net.sf.saxon.expr;
import net.sf.saxon.functions.NormalizeSpace;
import net.sf.saxon.trans.StaticError;

import java.util.ArrayList;
import java.util.List;

/**
 * Tokenizer for expressions and inputs.
 *
 * This code was originally derived from James Clark's xt, though it has been greatly modified since.
 * See copyright notice at end of file.
 */


public final class Tokenizer {

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
        if (state==DEFAULT_STATE) {
            // force the followsOperator() test to return true
            precedingToken = Token.UNKNOWN;
            currentToken = Token.UNKNOWN;
        } else if (state==OPERATOR_STATE) {
            precedingToken = Token.RPAR;
            currentToken = Token.RPAR;
        }
    }

    private int state = DEFAULT_STATE;
        // we may need to make this a stack at some time

    /**
     * Initial default state of the Tokenizer
     */
    public static final int DEFAULT_STATE = 0;

    /**
     * State in which a name is NOT to be merged with what comes next, for example "("
     */
    public static final int BARE_NAME_STATE = 1;

    /**
     * State in which the next thing to be read is a SequenceType
     */
    public static final int SEQUENCE_TYPE_STATE = 2;
    /**
     * State in which the next thing to be read is an operator
     */

    public static final int OPERATOR_STATE = 3;

    /**
     * The starting line number (for XPath in XSLT, the line number in the stylesheet)
     */
    public int startLineNumber;
    /**
     * The number identifying the most recently read token
     */
    public int currentToken = Token.EOF;
    /**
     * The string value of the most recently read token
     */
    public String currentTokenValue = null;
    /**
     * The position in the input expression where the current token starts
     */
    public int currentTokenStartOffset = 0;
    /**
     * The number of the next token to be returned
     */
    private int nextToken = Token.EOF;
    /**
     * The string value of the next token to be returned
     */
    private String nextTokenValue = null;
    /**
     * The position in the expression of the start of the next token
     */
    private int nextTokenStartOffset = 0;
    /**
     * The string being parsed
     */
    public String input;
    /**
     * The current position within the input string
     */
    public int inputOffset = 0;
    /**
     * The length of the input string
     */
    private int inputLength;
    /**
     * The line number (within the expression) of the current token
     */
    private int lineNumber = 1;
    /**
     * The line number (within the expression) of the next token
     */
    private int nextLineNumber = 1;

    /**
     * List containing the positions (offsets in the input string) at which newline characters
     * occur
     */

    private List newlineOffsets = null;

    /**
     * The token number of the token that preceded the current token
     */
    private int precedingToken = Token.UNKNOWN;


    //public boolean recognizePragmas = false;
    //public String lastPragma = null;

    //
    // Lexical analyser for expressions, queries, and XSLT patterns
    //

    /**
     * Prepare a string for tokenization.
     * The actual tokens are obtained by calls on next()
     *
     * @param input the string to be tokenized
     * @param start start point within the string
     * @param end end point within the string (last character not read):
     * -1 means end of string
     * @exception net.sf.saxon.trans.StaticError if a lexical error occurs, e.g. unmatched
     *     string quotes
     */
    public void tokenize(String input, int start, int end, int lineNumber) throws StaticError {
        nextToken = Token.EOF;
        nextTokenValue = null;
        nextTokenStartOffset = 0;
        inputOffset = start;
        this.input = input;
        this.startLineNumber = lineNumber;
        this.lineNumber = lineNumber;
        this.nextLineNumber = lineNumber;
        if (end==-1) {
            this.inputLength = input.length();
        } else {
            this.inputLength = end;
        }

        // The tokenizer actually reads one token ahead. The raw lexical analysis performed by
        // the lookAhead() method does not (in general) distinguish names used as QNames from names
        // used for operators, axes, and functions. The next() routine further refines names into the
        // correct category, by looking at the following token. In addition, it combines compound tokens
        // such as "instance of" and "cast as".

        lookAhead();
        next();
    }

    //diagnostic version of next(): change real version to realnext()
    //
    //public void next() throws XPathException {
    //    realnext();
    //    System.err.println("Token: " + currentToken + "[" + tokens[currentToken] + "]");
    //}

    /**
     * Get the next token from the input expression. The type of token is returned in the
     * currentToken variable, the string value of the token in currentTokenValue.
     *
     * @exception net.sf.saxon.trans.StaticError if a lexical error is detected
     */

    public void next() throws StaticError {
        precedingToken = currentToken;
        currentToken = nextToken;
        currentTokenValue = nextTokenValue;
        if (currentTokenValue==null) {
            currentTokenValue="";
        }
        currentTokenStartOffset = nextTokenStartOffset;
        lineNumber = nextLineNumber;

        // disambiguate the current token based on the tokenizer state

        switch (currentToken) {
            case Token.NAME:
                int optype = getBinaryOp(currentTokenValue);
                if (optype!=Token.UNKNOWN && !followsOperator()) {
                    currentToken = optype;
                }
                break;
            case Token.LT:
                if (followsOperator()) {
                    currentToken = Token.TAG;
                }
                break;
            case Token.STAR:
                if (!followsOperator()) {
                    currentToken = Token.MULT;
                }
                break;
        }

        if (currentToken == Token.TAG || currentToken == Token.RCURLY) {
            // No lookahead after encountering "<" at the start of an XML-like tag.
            // After an RCURLY, the parser must do an explicit lookahead() to continue
            // tokenizing; otherwise it can continue with direct character reading
            return;
        }

        lookAhead();

        if (currentToken == Token.NAME) {
            if (state == BARE_NAME_STATE) {
                return;
            }
            switch (nextToken) {
                case Token.LPAR:
                    int op = getBinaryOp(currentTokenValue);
                    if (op == Token.UNKNOWN) {
	                    currentToken = getFunctionType(currentTokenValue);
	                    lookAhead();    // swallow the "("
                    } else {
                        currentToken = op;
                    }
                    break;

                case Token.LCURLY:
                    if (!(state == SEQUENCE_TYPE_STATE)) {
                        currentToken = Token.KEYWORD_CURLY;
                        lookAhead();        // swallow the "{"
                    }
                    break;

                case Token.COLONCOLON:
                    lookAhead();
                    currentToken = Token.AXIS;
                    break;

                case Token.COLONSTAR:
                    lookAhead();
                    currentToken = Token.PREFIX;
                    break;

                case Token.DOLLAR:
                    if (currentTokenValue=="for") {
                        currentToken = Token.FOR;
                    } else if (currentTokenValue=="some") {
                        currentToken = Token.SOME;
                    } else if (currentTokenValue=="every") {
                        currentToken = Token.EVERY;
                    } else if (currentTokenValue=="let") {
                        currentToken = Token.LET;
                    }
                    break;

                case Token.NAME:
                    int candidate = -1;
                    if (currentTokenValue.equals("element")) {
                        candidate = Token.ELEMENT_QNAME;
                    } else if (currentTokenValue.equals("attribute")) {
                        candidate = Token.ATTRIBUTE_QNAME;
                    } else if (currentTokenValue.equals("processing-instruction")) {
                        candidate = Token.PI_QNAME;
                    }
                    if (candidate != -1) {
                        // <'element' QName '{'> constructor
                        // <'attribute' QName '{'> constructor
                        // <'processing-instruction' QName '{'> constructor

                        String qname = nextTokenValue;
                        String saveTokenValue = currentTokenValue;
                        int savePosition = inputOffset;
                        lookAhead();
                        if (nextToken == Token.LCURLY) {
                            currentToken = candidate;
                            currentTokenValue = qname;
                            lookAhead();
                            return;
                        } else {
                            // backtrack (we don't have 2-token lookahead; this is the
                            // only case where it's needed. So we backtrack instead.)
                            currentToken = Token.NAME;
                            currentTokenValue = saveTokenValue;
                            inputOffset = savePosition;
                            nextToken = Token.NAME;
                            nextTokenValue = qname;
                        }

                    }
                    String composite = currentTokenValue + ' ' + nextTokenValue;
                    Integer val = (Integer)Token.doubleKeywords.get(composite);
                    if (val==null) {
                        break;
                    } else {
                        currentToken = val.intValue();
                        currentTokenValue = composite;
                        lookAhead();
                        return;
                    }
                default:
                    // no action needed
            }
        }
    }

    /**
     * Force the current token to be treated as an operator if possible
     */

    public void treatCurrentAsOperator() {
        switch (currentToken) {
            case Token.NAME:
                int optype = getBinaryOp(currentTokenValue);
                if (optype!=Token.UNKNOWN) {
                    currentToken = optype;
                }
                break;
            case Token.STAR:
                currentToken = Token.MULT;
                break;
        }
    }

    /**
     * Look ahead by one token. This method does the real tokenization work.
     * The method is normally called internally, but the XQuery parser also
     * calls it to resume normal tokenization after dealing with pseudo-XML
     * syntax.
     * @exception net.sf.saxon.trans.StaticError if a lexical error occurs
     */
    public void lookAhead() throws StaticError {
        precedingToken = nextToken;
        nextTokenValue = null;
        nextTokenStartOffset = inputOffset;
        for (;;) {
            if (inputOffset >= inputLength) {
	            nextToken = Token.EOF;
	            return;
            }
            char c = input.charAt(inputOffset++);
            switch (c) {
            case '/':
	            if (inputOffset < inputLength
	                    && input.charAt(inputOffset) == '/') {
	                inputOffset++;
	                nextToken = Token.SLSL;
	                return;
	            }
	            nextToken = Token.SLASH;
	            return;
            case ':':
	            if (inputOffset < inputLength) {
	                if (input.charAt(inputOffset) == ':') {
	                    inputOffset++;
	                    nextToken = Token.COLONCOLON;
	                    return;
	                } else if (input.charAt(inputOffset) == '=') {
                        nextToken = Token.ASSIGN;
                        inputOffset++;
                        return;
                    }
	            }
	            throw new StaticError("Unexpected colon at start of token");
            case '@':
	            nextToken = Token.AT;
	            return;
	        case '?':
	            nextToken = Token.QMARK;
	            return;
            case '[':
	            nextToken = Token.LSQB;
	            return;
            case ']':
	            nextToken = Token.RSQB;
	            return;
            case '{':
	            nextToken = Token.LCURLY;
	            return;
            case '}':
	            nextToken = Token.RCURLY;
	            return;
            case ';':
                nextToken = Token.SEMICOLON;
                state = DEFAULT_STATE;
                return;
            case '(':
                if (inputOffset < inputLength && input.charAt(inputOffset) == '#') {
	                inputOffset++;
                    int pragmaStart = inputOffset;
                    int nestingDepth = 1;
                    while (nestingDepth > 0 && inputOffset < (inputLength-1)) {
                        if (input.charAt(inputOffset) == '\n') {
                            incrementLineNumber();
                        } else if (input.charAt(inputOffset) == '#' &&
                               input.charAt(inputOffset+1) == ')') {
                            nestingDepth--;
                            inputOffset++;
                        } else if (input.charAt(inputOffset) == '(' &&
                               input.charAt(inputOffset+1) == '#') {
                            nestingDepth++;
                            inputOffset++;
                        }
                        inputOffset++;
                    }
                    if (nestingDepth > 0) {
                        throw new StaticError("Unclosed XPath comment");
                    }
	                nextToken = Token.PRAGMA;
                    nextTokenValue = input.substring(pragmaStart, inputOffset-2 );
	                return;
	            }
	            if (inputOffset < inputLength && input.charAt(inputOffset) == ':') {
                    // XPath comment syntax is (: .... :)
                    // Comments may be nested
                    inputOffset++;
                    int nestingDepth = 1;
                    while (nestingDepth > 0 && inputOffset < (inputLength-1)) {
                        if (input.charAt(inputOffset) == '\n') {
                            incrementLineNumber();
                        } else if (input.charAt(inputOffset) == ':' &&
                            input.charAt(inputOffset+1) == ')') {
                            nestingDepth--;
                            inputOffset++;
                        } else if (input.charAt(inputOffset) == '(' &&
                               input.charAt(inputOffset+1) == ':') {
                            nestingDepth++;
                            inputOffset++;
                        }
                        inputOffset++;
                    }
                    if (nestingDepth > 0) {
                        throw new StaticError("Unclosed XPath comment");
                    }
                    lookAhead();
                } else {
	                nextToken = Token.LPAR;
	            }
	            return;
            case ')':
	            nextToken = Token.RPAR;
	            return;
            case '+':
	            nextToken = Token.PLUS;
	            return;
            case '-':
	            nextToken = Token.MINUS;   // not detected if part of a name
	            return;
            case '=':
	            nextToken = Token.EQUALS;
	            return;
            case '!':
	            if (inputOffset < inputLength
	                    && input.charAt(inputOffset) == '=') {
	                inputOffset++;
	                nextToken = Token.NE;
	                return;
	            }
	            throw new StaticError("'!' without '='");
            case '*':
                // disambiguation of MULT and STAR is now done later
                //if (followsOperator()) {
                    if (inputOffset < inputLength
	                        && input.charAt(inputOffset) == ':') {
    	                inputOffset++;
    	                nextToken = Token.SUFFIX;
    	                // we leave the parser to get the following name as a separate
    	                // token, but first check there's no intervening white space
    	                if (inputOffset < inputLength) {
    	                    char ahead = input.charAt(inputOffset);
    	                    if (" \r\t\n".indexOf(ahead) >= 0) {
    	                        throw new StaticError("Whitespace is not allowed after '*:'");
    	                    }
    	                }
    	                return;
	                }
	                nextToken = Token.STAR;
                //} else {
                //    nextToken = MULT;
                //}
	            return;
            case ',':
	            nextToken = Token.COMMA;
	            return;
            case '$':
	            nextToken = Token.DOLLAR;
	            return;
            case '|':
	            nextToken = Token.UNION;
	            return;
            case '<':
	            if (inputOffset < inputLength
	                    && input.charAt(inputOffset) == '=') {
	                inputOffset++;
	                nextToken = Token.LE;
	                return;
	            }
	            if (inputOffset < inputLength
	                    && input.charAt(inputOffset) == '<') {
	                inputOffset++;
	                nextToken = Token.PRECEDES;
	                return;
	            }
	            nextToken = Token.LT;
	            return;
            case '>':
	            if (inputOffset < inputLength
	                    && input.charAt(inputOffset) == '=') {
	                inputOffset++;
	                nextToken = Token.GE;
	                return;
	            }
	            if (inputOffset < inputLength
	                    && input.charAt(inputOffset) == '>') {
	                inputOffset++;
	                nextToken = Token.FOLLOWS;
	                return;
	            }
	            nextToken = Token.GT;
	            return;
            case '.':
	            if (inputOffset < inputLength
	                    && input.charAt(inputOffset) == '.') {
	                inputOffset++;
	                nextToken = Token.DOTDOT;
	                return;
	            }
	            if (inputOffset == inputLength
	                    || input.charAt(inputOffset) < '0'
	                    || input.charAt(inputOffset) > '9') {
	                nextToken = Token.DOT;
	                return;
	            }
                // otherwise drop through: we have a number starting with a decimal point
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                // The logic here can return some tokens that are not legitimate numbers,
                // for example "23e" or "1.0e+". However, this will only happen if the XPath
                // expression as a whole is syntactically incorrect.
                // These errors will be caught by the numeric constructor.
                    // TODO: the current specification (Feb 2005) disallows "10div 3", though this
                    // was allowed in XPath 1.0. We are currently allowing this. But we don't allow
                    // "if (x) then 10else 3".
                boolean allowE = true;
                boolean allowSign = false;
                boolean allowDot = true;
                boolean endOfNum = false;
            numloop:
                while (!endOfNum) {
	                switch (c) {
                        case '0': case '1': case '2': case '3': case '4':
                        case '5': case '6': case '7': case '8': case '9':
                            allowSign = false;
                            break;
                        case '.':
                            if (allowDot) {
                                allowDot = false;
                                allowSign = false;
                            } else {
                                inputOffset--;
                                break numloop;
                            }
                            break;
                        case 'E': case 'e':
                            if (allowE) {
                                allowSign = true;
                                allowE = false;
                            } else {
                                inputOffset--;
                                break numloop;
                            }
                            break;
                        case '+': case '-':
                            if (allowSign) {
                                allowSign = false;
                            } else {
                                inputOffset--;
                                break numloop;
                            }
                            break;
                        default:
                            inputOffset--;
                            break numloop;
                    }
                    if (inputOffset >= inputLength) break;
                    c = input.charAt(inputOffset++);
	            }
	            nextTokenValue = input.substring(nextTokenStartOffset, inputOffset);
	            nextToken = Token.NUMBER;
	            return;
            case '"':
            case '\'':
                nextTokenValue = "";
                while (true) {
    	            inputOffset = input.indexOf(c, inputOffset);
    	            if (inputOffset < 0) {
    	                inputOffset = nextTokenStartOffset + 1;
    	                throw new StaticError("Unmatched quote in expression");
    	            }
    	            nextTokenValue += input.substring(nextTokenStartOffset + 1, inputOffset++);
    		        // look for doubled delimiters
    			    if (inputOffset < inputLength && input.charAt(inputOffset) == c) {
	                    nextTokenValue += c;
	                    nextTokenStartOffset = inputOffset;
	                    inputOffset++;
	                } else {
	                    break;
	                }
	            }

                // maintain line number if there are newlines in the string
                if (nextTokenValue.indexOf('\n') >= 0) {
                    for (int i = 0; i<nextTokenValue.length(); i++) {
                        if (nextTokenValue.charAt(i) == '\n') {
                            lineNumber++;
                            if (newlineOffsets==null) {
                                newlineOffsets = new ArrayList(20);
                            }
                            newlineOffsets.add(new Integer(nextTokenStartOffset+i));
                        }
                    }
                }
	            nextTokenValue = nextTokenValue.intern();
	            nextToken = Token.STRING_LITERAL;
	            return;
            case '\n':
                incrementLineNumber();
                // drop through
            case ' ':
            case '\t':
            case '\r':
	            nextTokenStartOffset = inputOffset;
	            break;
            default:
	            if (c < 0x80 && !Character.isLetter(c)) {
	                throw new StaticError("Invalid character '" + c + "' in expression");
                }
                /* fall through */
            case '_':
            loop:
	            for (;inputOffset < inputLength; inputOffset++) {
	                c = input.charAt(inputOffset);
	                switch (c) {
                    case ':':
        	            if (inputOffset+1 < inputLength) {
    	                    char nc = input.charAt(inputOffset+1);
                            if (nc == ':') {
                                nextTokenValue = input.substring(nextTokenStartOffset,
                                                                inputOffset).intern();
                                nextToken = Token.AXIS;
                                inputOffset+=2;
                                return;
        	                } else if (nc == '*') {
                                nextTokenValue = input.substring(nextTokenStartOffset,
                                                                inputOffset).intern();
                                nextToken = Token.PREFIX;
                                inputOffset+=2;
                                return;
                            } else if (nc == '=') {
                                // as in "let $x:=2"
                                nextTokenValue = input.substring(nextTokenStartOffset,
                                                                inputOffset).intern();
                                nextToken = Token.NAME;
                                return;
                            }
        	            }
                        break;
	                case '.':
	                case '-':
	                case '_':
	                    break;

	                default:
	                    if (c < 0x80 && !Character.isLetterOrDigit(c))
	                        break loop;
	                    break;
	                }
	            }
	            nextTokenValue = input.substring(nextTokenStartOffset,
					                                    inputOffset).intern();
                nextToken = Token.NAME;
	            return;
            }
        }
    }

    /**
     * Identify a binary operator
     *
     * @param s String representation of the operator - must be interned
     * @return the token number of the operator, or UNKNOWN if it is not a
     *     known operator
     */

    private static int getBinaryOp(String s) {
        switch(s.length()) {
            case 2:
                if (s=="or") return Token.OR;
                if (s=="is") return Token.IS;
                if (s=="to") return Token.TO;
                if (s=="in") return Token.IN;
                if (s=="eq") return Token.FEQ;
                if (s=="ne") return Token.FNE;
                if (s=="gt") return Token.FGT;
                if (s=="ge") return Token.FGE;
                if (s=="lt") return Token.FLT;
                if (s=="le") return Token.FLE;
                break;
            case 3:
                if (s=="and") return Token.AND;
                if (s=="div") return Token.DIV;
                if (s=="mod") return Token.MOD;
                break;
            case 4:
                if (s=="idiv") return Token.IDIV;
                if (s=="then") return Token.THEN;
                if (s=="else") return Token.ELSE;
                if (s=="case") return Token.CASE;
                break;
            case 5:
                if (s=="where") return Token.WHERE;
                if (s=="union") return Token.UNION;
                break;
            case 6:
                if (s=="except") return Token.EXCEPT;
                if (s=="return") return Token.RETURN;
                break;
            case 7:
                if (s=="default") return Token.DEFAULT;
            case 9:
                if (s=="intersect") return Token.INTERSECT;
                if (s=="satisfies") return Token.SATISFIES;
                break;
        }
        return Token.UNKNOWN;
    }

    /**
     * Distinguish nodekind names, "if", and function names, which are all
     * followed by a "("
     *
     * @param s the name - must be interned
     * @return the token number
     */

    private static int getFunctionType(String s) {
        switch(s.length()) {
            case 2:
                if (s=="if") return Token.IF;
                break;
            case 4:
                if (s=="node") return Token.NODEKIND;
                if (s=="item") return Token.NODEKIND;
                if (s=="text") return Token.NODEKIND;
                if (s=="void") return Token.NODEKIND;
                break;
            case 7:
                if (s=="element") return Token.NODEKIND;
                if (s=="comment") return Token.NODEKIND;
                break;
            case 9:
                if (s=="attribute") return Token.NODEKIND;
                if (s=="namespace") return Token.NODEKIND;
                break;
            case 10:
                if (s=="typeswitch") return Token.TYPESWITCH;
                break;
            default:
                if (s=="document-node") return Token.NODEKIND;
                if (s=="schema-element") return Token.NODEKIND;
                if (s=="schema-attribute") return Token.NODEKIND;
                if (s=="processing-instruction") return Token.NODEKIND;

                break;
        }
        return Token.FUNCTION;
    }

    /**
     * Test whether the previous token is an operator
     * @return true if the previous token is an operator token
     */

    private boolean followsOperator() {
        return precedingToken <= Token.LAST_OPERATOR;
    }

    /**
     * Read next character directly. Used by the XQuery parser when parsing pseudo-XML syntax
     * @return the next character from the input
     * @throws StringIndexOutOfBoundsException if an attempt is made to read beyond
     * the end of the string. This will only occur in the event of a syntax error in the
     * input.
     */

    public char nextChar() throws StringIndexOutOfBoundsException {
        char c = input.charAt(inputOffset++);
        c = normalizeLineEnding(c);
        if (c=='\n') {
            incrementLineNumber();
            lineNumber++;
        }
        return c;
    }

    /**
     * Normalize line endings according to the rules in XML 1.1.
     * @param c the most recently read character. The value of inputOffset must be the immediately following
     * character
     * @return c the current character after newline normalization
     */

    private char normalizeLineEnding(char c) throws StringIndexOutOfBoundsException {
        switch (c)  {
            case '\r':
                if (input.charAt(inputOffset) == '\n' || input.charAt(inputOffset) == 0x85) {
                    inputOffset++;
                    return '\n';
                } else {
                    return '\n';
                }
            case 0x85:
                return '\n';
            case 0x2028:
                return '\n';
            default:
                return c;
        }
    }

    /**
     * Increment the line number, making a record of where in the input string the newline character occurred.
     */

    private void incrementLineNumber() {
        nextLineNumber++;
        if (newlineOffsets==null) {
            newlineOffsets = new ArrayList(20);
        }
        newlineOffsets.add(new Integer(inputOffset-1));
    }

    /**
     * Step back one character. If this steps back to a previous line, adjust the line number.
     */

    public void unreadChar() {
        if (input.charAt(--inputOffset) == '\n') {
            nextLineNumber--;
            lineNumber--;
            if (newlineOffsets != null) {
                newlineOffsets.remove(newlineOffsets.size()-1);
            }
        }
    }

    /**
     * Get the most recently read text (for use in an error message)
     */

    public String recentText() {
        if (inputOffset > inputLength) {
            inputOffset = inputLength;
        }
        if (inputOffset < 34) {
            return input.substring(0, inputOffset);
        } else {
            return NormalizeSpace.normalize(
                    "..." + input.substring(inputOffset-30, inputOffset)).toString();
        }
    }

    /**
     * Get the line number of the current token
     */

    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * Get the column number of the current token
     */

    public int getColumnNumber() {
        return (int)(getLineAndColumn(currentTokenStartOffset)&0x7fffffff);
    }

// --Commented out by Inspection START (16/12/04 14:40):
//    /**
//     * Get the line and column number of the current token,
//     * as a long value with the line number in the top half
//     * and the column number in the lower half
//     * @return the line and column number, packed together
//     */
//
//    public long getLineAndColumn() {
//        return ((long)getLineNumber()) << 32 | ((long)getColumnNumber());
//    }
// --Commented out by Inspection STOP (16/12/04 14:40)


    /**
     * Get the line and column number corresponding to a given offset in the input expression,
     * as a long value with the line number in the top half
     * and the column number in the lower half
     * @return the line and column number, packed together
     */

    public long getLineAndColumn(int offset) {
        if (newlineOffsets==null) {
            return ((long)startLineNumber) << 32 | (long)offset;
        }
        for (int line=newlineOffsets.size()-1; line>=0; line--) {
            int nloffset = ((Integer)newlineOffsets.get(line)).intValue();
            if (offset > nloffset) {
                return ((long)(line+startLineNumber+1)<<32) | ((long)(offset - nloffset));
            }
        }
        return ((long)startLineNumber) << 32 | (long)(offset+1);
    }

    public int getLineNumber(int offset) {
        return (int)((getLineAndColumn(offset))>>32);
    }

    public int getColumnNumber(int offset) {
        return (int)((getLineAndColumn(offset))&0x7fffffff);
    }

}

/*

The following copyright notice is copied from the licence for xt, from which the
original version of this module was derived:
--------------------------------------------------------------------------------
Copyright (c) 1998, 1999 James Clark

Permission is hereby granted, free of charge, to any person obtaining
a copy of this software and associated documentation files (the
"Software"), to deal in the Software without restriction, including
without limitation the rights to use, copy, modify, merge, publish,
distribute, sublicense, and/or sell copies of the Software, and to
permit persons to whom the Software is furnished to do so, subject to
the following conditions:

The above copyright notice and this permission notice shall be included
in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED ``AS IS'', WITHOUT WARRANTY OF ANY KIND, EXPRESS
OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
IN NO EVENT SHALL JAMES CLARK BE LIABLE FOR ANY CLAIM, DAMAGES OR
OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
OTHER DEALINGS IN THE SOFTWARE.

Except as contained in this notice, the name of James Clark shall
not be used in advertising or otherwise to promote the sale, use or
other dealings in this Software without prior written authorization
from James Clark.
---------------------------------------------------------------------------
*/

//
// The contents of this file are subject to the Mozilla Public License Version 1.0 (the "License");
// you may not use this file except in compliance with the License. You may obtain a copy of the
// License at http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS" basis,
// WITHOUT WARRANTY OF ANY KIND, either express or implied.
// See the License for the specific language governing rights and limitations under the License.
//
// The Original Code is: all this file, other than the parts developed by James Clark as part of xt.
//
// The Initial Developer of the Original Code is Michael H. Kay.
//
// Portions created by (your name) are Copyright (C) (your legal entity). All Rights Reserved.
//
// Contributor(s): none.
//