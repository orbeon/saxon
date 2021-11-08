package org.orbeon.saxon.expr;

import java.util.HashMap;

/**
 * This class holds static constants and methods defining the lexical tokens used in
 * XPath and XQuery, and associated keywords.
 */

public abstract class Token {

    /**
     * Token numbers. Those in the range 0 to 100 are tokens that can be followed
     * by a name or expression; those in the range 101 to 200 are tokens that can be
     * followed by an binary operator.
     */

    /**
     * Pseudo-token representing the end of the expression
     */
    public static final int EOF = 0;
    /**
     * "union" or "|" token
     */
    public static final int UNION = 1;
    /**
     * Forwards "/"
     */
    public static final int SLASH = 2;
    /**
     * At token, "@"
     */
    public static final int AT = 3;
    /**
     * Left square bracket
     */
    public static final int LSQB = 4;
    /**
     * Left parenthesis
     */
    public static final int LPAR = 5;
    /**
     * Equals token ("=")
     */
    public static final int EQUALS = 6;
    /**
     * Comma token
     */
    public static final int COMMA = 7;
    /**
     * Double forwards slash, "//"
     */
    public static final int SLSL = 8;
    /**
     * Operator "or"
     */
    public static final int OR = 9;
    /**
     * Operator "and"
     */
    public static final int AND = 10;
    /**
     * Operator ">"
     */
    public static final int GT = 11;
    /**
     * Operator "<"
     */
    public static final int LT = 12;
    /**
     * Operator ">="
     */
    public static final int GE = 13;
    /**
     * Operator "<="
     */
    public static final int LE = 14;
    /**
     * Operator "+"
     */
    public static final int PLUS = 15;
    /**
     * Binary minus operator
     */
    public static final int MINUS = 16;
    /**
     * Multiply operator, "*" when used in an operator context
     */
    public static final int MULT = 17;
    /**
     * Operator "div"
     */
    public static final int DIV = 18;
    /**
     * Operator "mod"
     */
    public static final int MOD = 19;
    /**
     * Operator "is"
     */
    public static final int IS = 20;
    /**
     * "$" symbol
     */
    public static final int DOLLAR = 21;
    /**
     * Operator not-equals. That is, "!="
     */
    public static final int NE = 22;
    /**
     * Operator "intersect"
     */
    public static final int INTERSECT = 23;
    /**
     * Operator "except"
     */
    public static final int EXCEPT = 24;
    /**
     * Keyword "return"
     */
    public static final int RETURN = 25;
    /**
     * Ketword "then"
     */
    public static final int THEN = 26;
    /**
     * Keyword "else"
     */
    public static final int ELSE = 27;
    /**
     * Keyword "where"
     */
    public static final int WHERE = 28;
    /**
     * Operator "to"
     */
    public static final int TO = 29;
    /**
     * Keyword "in"
     */
    public static final int IN = 30;
    /**
     * Keyword "some"
     */
    public static final int SOME = 31;
    /**
     * Keyword "every"
     */
    public static final int EVERY = 32;
    /**
     * Keyword "satisfies"
     */
    public static final int SATISFIES = 33;
    /**
     * Token representing the name of a function and the following "(" symbol
     */
    public static final int FUNCTION = 34;
    /**
     * Token representing the name of an axis and the following "::" symbol
     */
    public static final int AXIS = 35;
    /**
     * Keyword "if"
     */
    public static final int IF = 36;
    /**
     * Operator "<<"
     */
    public static final int PRECEDES = 37;
    /**
     * Operator ">>"
     */
    public static final int FOLLOWS = 38;
    /**
     * "::" symbol
     */
    public static final int COLONCOLON = 39;
    /**
     * ":*" symbol
     */
    public static final int COLONSTAR = 40;
    /**
     * operator "instance of"
     */
    public static final int INSTANCE_OF = 41;
    /**
     * operator "cast as"
     */
    public static final int CAST_AS = 42;
    /**
     * operator "treat as"
     */
    public static final int TREAT_AS = 43;
    /**
     * operator "eq"
     */
    public static final int FEQ = 44;       // "Fortran" style comparison operators eq, ne, etc
    /**
     * operator "ne"
     */
    public static final int FNE = 45;
    /**
     * operator "gt"
     */
    public static final int FGT = 46;
    /**
     * operator "lt"
     */
    public static final int FLT = 47;
    /**
     * operator "ge"
     */
    public static final int FGE = 48;
    /**
     * opeartor "le"
     */
    public static final int FLE = 49;
    /**
     * operator "idiv"
     */
    public static final int IDIV = 50;
    /**
     * operator "castable as"
     */
    public static final int CASTABLE_AS = 51;
    /**
      * ":=" symbol (XQuery only)
      */
    public static final int ASSIGN = 52;
    /**
     * "{" symbol (XQuery only)
     */
    public static final int LCURLY = 53;
    /**
     * composite token: <keyword "{"> (XQuery only)
     */
    public static final int KEYWORD_CURLY = 54;
    /**
     * composite token <'element' QNAME> (XQuery only)
     */
    public static final int ELEMENT_QNAME = 55;
    /**
     * composite token <'attribute' QNAME> (XQuery only)
     */
    public static final int ATTRIBUTE_QNAME = 56;
    /**
     * composite token <'pi' QNAME> (XQuery only)
     */
    public static final int PI_QNAME = 57;
    /**
     * Keyword "typeswitch"
     */
    public static final int TYPESWITCH = 58;
    /**
     * Keyword "case"
     */
    public static final int CASE = 59;
    /**
     * Keyword "modify"
     */
    public static final int MODIFY = 60;

     /**
     * Node kind, e.g. "node()" or "comment()"
     */
    public static final int NODEKIND = 61;
    /**
     * "*:" token
     */
    public static final int SUFFIX = 62;    // e.g. *:suffix - the suffix is actually a separate token
    /**
     * "as" (in XQuery Update rename expression)
     */
    public static final int AS = 63;


    // The following tokens are used only in the query prolog. They are categorized
    // as operators on the basis that a following name is treated as a name rather than
    // an operator.


    /**
     * "xquery version"
     */
    public static final int XQUERY_VERSION = 70;
    /**
     * "declare namespace"
     */
    public static final int DECLARE_NAMESPACE = 71;
    /**
     * "declare default"
     */
    public static final int DECLARE_DEFAULT = 72;
    /**
     * "declare construction"
     */
    public static final int DECLARE_CONSTRUCTION = 73;
    /**
     * "declare base-uri"
     */
    public static final int DECLARE_BASEURI = 74;
    /**
     * "declare boundary-space"
     */
    public static final int DECLARE_BOUNDARY_SPACE = 75;
    /**
     * "import schema"
     */
    public static final int IMPORT_SCHEMA = 76;
    /**
     * "import module"
     */
    public static final int IMPORT_MODULE = 77;
    /**
     * "define variable"
     */
    public static final int DECLARE_VARIABLE = 78;
    /**
     * "declare function"
     */
    public static final int DECLARE_FUNCTION = 79;
    /**
     * "module namespace"
     */
    public static final int MODULE_NAMESPACE = 80;
    /**
     * Various compound symbols supporting XQuery validation expression
     */
    public static final int VALIDATE = 81;
    public static final int VALIDATE_STRICT = 82;
    public static final int VALIDATE_LAX = 83;

    /**
     * "declare xmlspace"
     */
    public static final int DECLARE_ORDERING = 84;

    /**
     * "declare copy-namespaces"
     */
    public static final int DECLARE_COPY_NAMESPACES = 85;
    /**
     * "declare option"
     */
    public static final int DECLARE_OPTION = 86;
    /**
     * "declare revalidation"
     */
    public static final int DECLARE_REVALIDATION = 87;
    /**
     * "insert node/nodes"
     */
    public static final int INSERT_NODE = 90;
    /**
     * "delete node/nodes"
     */
    public static final int DELETE_NODE = 91;
    /**
     * "replace node/nodes"
     */
    public static final int REPLACE_NODE = 92;
    /**
     * "replace value"
     */
    public static final int REPLACE_VALUE = 93;
    /**
     * "rename node"
     */
    public static final int RENAME_NODE = 94;
    /**
     * "as first"
     */
    //public static final int AS_FIRST = 95;
    /**
     * "as last"
     */
    //public static final int AS_LAST = 96;
    /**
     * "first into"
     */
    public static final int FIRST_INTO = 95;
    /**
     * "last into"
     */
    public static final int LAST_INTO = 96;
    /**
     * "after"
     */
    public static final int AFTER = 97;
    /**
     * "before"
     */
    public static final int BEFORE = 98;
   /**
     * "into"
     */
    public static final int INTO = 99;
   /**
     * "with"
     */
    public static final int WITH = 100;
    /**
     * "declare updating [function]"
     */
    public static final int DECLARE_UPDATING = 101;
    /**
     * semicolon separator
     */
    public static final int SEMICOLON = 149;


    /**
     * Constant identifying the token number of the last token to be classified as an operator
     */
    static int LAST_OPERATOR = 150;

    // Tokens that set "operator" context, so an immediately following "div" is recognized
    // as an operator, not as an element name

    /**
     * Name token (a QName, in general)
     */
    public static final int NAME = 201;
    /**
     * String literal
     */
    public static final int STRING_LITERAL = 202;
    /**
     * Right square bracket
     */
    public static final int RSQB = 203;
    /**
     * Right parenthesis
     */
    public static final int RPAR = 204;
    /**
     * "." symbol
     */
    public static final int DOT = 205;
    /**
     * ".." symbol
     */
    public static final int DOTDOT = 206;
    /**
     * "*" symbol when used as a wildcard
     */
    public static final int STAR = 207;
    /**
     * "prefix:*" token
     */
    public static final int PREFIX = 208;    // e.g. prefix:*
    /**
     * Numeric literal
     */
    public static final int NUMBER = 209;

    /**
     * "for" keyword
     */
    public static final int FOR = 211;

    /**
     * Keyword "default"
     */
    public static final int DEFAULT = 212;
    /**
     * Question mark symbol. That is, "?"
     */
    public static final int QMARK = 213;
    /**
     * "}" symbol (XQuery only)
     */
    public static final int RCURLY = 215;
    /**
     * "let" keyword (XQuery only)
     */
    public static final int LET = 216;
    /**
     * "<" at the start of a tag (XQuery only). The pseudo-XML syntax that
     * follows is read character-by-character by the XQuery parser
     */
    public static final int TAG = 217;
    /**
     * A token representing an XQuery pragma.
     * This construct "(# .... #)" is regarded as a single token, for the QueryParser to sort out.
     */
    public static final int PRAGMA = 218;
    /**
     * "copy" keyword
     */
    public static final int COPY = 219;

    /**
     * Unary minus sign
     */
    public static final int NEGATE = 299;    // unary minus: not actually a token, but we
                                             // use token numbers to identify operators.


    /**
     * The following strings are used to represent tokens in error messages
     */

    public static String[] tokens = new String[300];
    static {
        tokens [ EOF ] = "<eof>";
        tokens [ UNION ] = "|";
        tokens [ SLASH ] = "/";
        tokens [ AT ] = "@";
        tokens [ LSQB ] = "[";
        tokens [ LPAR ] = "(";
        tokens [ EQUALS ] = "=";
        tokens [ COMMA ] = ",";
        tokens [ SLSL ] = "//";
        tokens [ OR ] = "or";
        tokens [ AND ] = "and";
        tokens [ GT ] = ">";
        tokens [ LT ] = "<";
        tokens [ GE ] = ">=";
        tokens [ LE ] = "<=";
        tokens [ PLUS ] = "+";
        tokens [ MINUS ] = "-";
        tokens [ MULT ] = "*";
        tokens [ DIV ] = "div";
        tokens [ MOD ] = "mod";
        tokens [ IS ] = "is";
        tokens [ DOLLAR ] = "$";
        tokens [ NE ] = "!=";
        tokens [ INTERSECT ] = "intersect";
        tokens [ EXCEPT ] = "except";
        tokens [ RETURN ] = "return";
        tokens [ THEN ] = "then";
        tokens [ ELSE ] = "else";
        //tokens [ ISNOT ] = "isnot";
        tokens [ TO ] = "to";
        tokens [ IN ] = "in";
        tokens [ SOME ] = "some";
        tokens [ EVERY ] = "every";
        tokens [ SATISFIES ] = "satisfies";
        tokens [ FUNCTION ] = "<function>(";
        tokens [ AXIS ] = "<axis>";
        tokens [ IF ] = "if(";
        tokens [ PRECEDES ] = "<<";
        tokens [ FOLLOWS ] = ">>";
        tokens [ COLONCOLON ] = "::";
        tokens [ COLONSTAR ] = ":*";
        tokens [ INSTANCE_OF ] = "instance of";
        tokens [ CAST_AS ] = "cast as";
        tokens [ TREAT_AS ] = "treat as";
        tokens [ FEQ ] = "eq";
        tokens [ FNE ] = "ne";
        tokens [ FGT ] = "gt";
        tokens [ FGE ] = "ge";
        tokens [ FLT ] = "lt";
        tokens [ FLE ] = "le";
        tokens [ IDIV ] = "idiv";
        tokens [ CASTABLE_AS ] = "castable as";
        tokens [ ASSIGN ] = ":=";
        tokens [ TYPESWITCH ] = "typeswitch";
        tokens [ CASE ] = "case";
        tokens [ DEFAULT ] = "default";
        //tokens [ AS_FIRST ] = "as first";
        //tokens [ AS_LAST ] = "as last";
        tokens [ AFTER ] = "after";
        tokens [ BEFORE ] = "before";
        tokens [ INTO ] = "into";
        tokens [ WITH ] = "with";
        tokens [ MODIFY ] = "modify";
        tokens [ AS ] = "as";


        tokens [ NAME ] = "<name>";
        tokens [ STRING_LITERAL ] = "<string-literal>";
        tokens [ RSQB ] = "]";
        tokens [ RPAR ] = ")";
        tokens [ DOT ] = ".";
        tokens [ DOTDOT ] = "..";
        tokens [ STAR ] = "*";
        tokens [ PREFIX ] = "<prefix:*>";
        tokens [ NUMBER ] = "<numeric-literal>";
        tokens [ NODEKIND ] = "<node-type>()";
        tokens [ FOR ] = "for";
        tokens [ SUFFIX ] = "<*:local-name>";
        tokens [ QMARK ] = "?";
        tokens [ LCURLY ] = "{";
        tokens [ KEYWORD_CURLY ] = "<keyword> {";
        tokens [ RCURLY ] = "}";
        tokens [ LET ] = "let";
        tokens [ VALIDATE ] = "validate {";
        tokens [ TAG ] = "<element>";
        tokens [ PRAGMA ] = "(# ... #)";
        tokens [ SEMICOLON ] = ";";
        tokens [ COPY ] = "copy";
        tokens [ NEGATE ] = "-";
    }

    /**
     * Lookup table for composite (two-keyword) tokens
     */
    public static HashMap doubleKeywords = new HashMap(30);
    /**
     * Pseudo-token representing the start of the expression
     */
    public static final int UNKNOWN = -1;

    private Token() {
    }

    static {
        mapDouble("instance of", INSTANCE_OF);
        mapDouble("cast as", CAST_AS);
        mapDouble("treat as", TREAT_AS);
        mapDouble("castable as", CASTABLE_AS);
        mapDouble("xquery version", XQUERY_VERSION);
        mapDouble("declare namespace", DECLARE_NAMESPACE);
        mapDouble("declare default", DECLARE_DEFAULT);
        mapDouble("declare construction", DECLARE_CONSTRUCTION);
        mapDouble("declare base-uri", DECLARE_BASEURI);
        mapDouble("declare boundary-space", DECLARE_BOUNDARY_SPACE);
        mapDouble("declare ordering", DECLARE_ORDERING);
        mapDouble("declare copy-namespaces", DECLARE_COPY_NAMESPACES);
        mapDouble("declare option", DECLARE_OPTION);
        mapDouble("declare revalidation", DECLARE_REVALIDATION);
        mapDouble("import schema", IMPORT_SCHEMA);
        mapDouble("import module", IMPORT_MODULE);
        mapDouble("declare variable", DECLARE_VARIABLE);
        mapDouble("declare function", DECLARE_FUNCTION);
        mapDouble("declare updating", DECLARE_UPDATING);
        mapDouble("module namespace", MODULE_NAMESPACE);
        mapDouble("validate strict", VALIDATE_STRICT);
        mapDouble("validate lax", VALIDATE_LAX);
        mapDouble("insert node", INSERT_NODE);
        mapDouble("insert nodes", INSERT_NODE);
        mapDouble("delete node", DELETE_NODE);
        mapDouble("delete nodes", DELETE_NODE);
        mapDouble("replace node", REPLACE_NODE);
        mapDouble("replace value", REPLACE_VALUE);
        mapDouble("rename node", RENAME_NODE);
        mapDouble("rename nodes", RENAME_NODE);
        mapDouble("first into", FIRST_INTO);
        mapDouble("last into", LAST_INTO);
        //mapDouble("as first", AS_FIRST);
        //mapDouble("as last", AS_LAST);

    }

    private static void mapDouble(String doubleKeyword, int token) {
        doubleKeywords.put(doubleKeyword, new Integer(token));
        tokens[token] = doubleKeyword;
    }

    /**
	* Return the inverse of a relational operator, so that "a op b" can be
	* rewritten as "b inverse(op) a"
	*/

    public static int inverse(int operator) {
        switch(operator) {
            case LT:
                return GT;
            case LE:
                return GE;
            case GT:
                return LT;
            case GE:
                return LE;
            case FLT:
                return FGT;
            case FLE:
                return FGE;
            case FGT:
                return FLT;
            case FGE:
                return FLE;
            default:
                return operator;
        }
    }

    /**
	* Return the negation of a relational operator, so that "a op b" can be
	* rewritten as not(b op' a)
	*/

    public static int negate(int operator) {
        switch(operator) {
            case FEQ:
                return FNE;
            case FNE:
                return FEQ;
            case FLT:
                return FGE;
            case FLE:
                return FGT;
            case FGT:
                return FLE;
            case FGE:
                return FLT;
            default:
                throw new IllegalArgumentException("Invalid operator for negate()");
        }
    }

    public static boolean isOrderedOperator(int operator) {
        return operator != FEQ && operator != FNE;
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