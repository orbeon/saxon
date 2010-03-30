package org.orbeon.saxon.sql;

import org.orbeon.saxon.expr.Expression;
import org.orbeon.saxon.expr.SimpleExpression;
import org.orbeon.saxon.expr.XPathContext;
import org.orbeon.saxon.instruct.Executable;
import org.orbeon.saxon.om.Axis;
import org.orbeon.saxon.om.AxisIterator;
import org.orbeon.saxon.om.Item;
import org.orbeon.saxon.om.NodeInfo;
import org.orbeon.saxon.style.ExtensionInstruction;
import org.orbeon.saxon.trans.SaxonErrorCode;
import org.orbeon.saxon.trans.XPathException;
import org.orbeon.saxon.type.Type;
import org.orbeon.saxon.value.AtomicValue;
import org.orbeon.saxon.value.ObjectValue;
import org.orbeon.saxon.value.Whitespace;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
* An sql:insert element in the stylesheet.
*/

public class SQLInsert extends ExtensionInstruction {

    Expression connection;
    String table;

    public void prepareAttributes() throws XPathException {

		table = getAttributeList().getValue("", "table");
		if (table==null) {
            reportAbsence("table");
        }
        table = SQLConnect.quoteSqlName(table);
        
        String connectAtt = getAttributeList().getValue("", "connection");
        if (connectAtt==null) {
            reportAbsence("connection");
        } else {
            connection = makeExpression(connectAtt);
        }
    }

    public void validate() throws XPathException {
        super.validate();
        connection = typeCheck("connection", connection);
        AxisIterator kids = iterateAxis(Axis.CHILD);
        while(true) {
            NodeInfo curr = (NodeInfo)kids.next();
            if (curr == null) {
                break;
            }
            if (curr instanceof SQLColumn) {
                // OK
            } else if (curr.getNodeKind() == Type.TEXT && Whitespace.isWhite(curr.getStringValueCS())) {
                // OK
            } else {
                compileError("Only sql:column is allowed as a child of sql:insert", "XTSE0010");
            }
        }

    }

    public Expression compile(Executable exec) throws XPathException {

        // Collect names of columns to be added

        StringBuffer statement = new StringBuffer(120);
        statement.append("INSERT INTO " + table + " (");

        AxisIterator kids = iterateAxis(Axis.CHILD);
        NodeInfo child;
		int cols = 0;
		while (true) {
            child = (NodeInfo)kids.next();
            if (child == null) {
                break;
            }
		    if (child instanceof SQLColumn) {
    			if (cols++ > 0)	statement.append(',');
    			String colname = ((SQLColumn)child).getColumnName();
    			statement.append(colname);
		    }
		}
        statement.append(") VALUES (");

        // Add "?" marks for the variable parameters

		for(int i=0; i<cols; i++) {
			if (i!=0) {
			    statement.append(',');
            }
			statement.append('?');
		};

		statement.append(')');

        return new InsertInstruction(connection, statement.toString(), getColumnInstructions(exec));
    }

    public List getColumnInstructions(Executable exec) throws XPathException {
        List list = new ArrayList(10);

        AxisIterator kids = iterateAxis(Axis.CHILD);
        NodeInfo child;
		while (true) {
            child = (NodeInfo)kids.next();
            if (child == null) {
                break;
            }
		    if (child instanceof SQLColumn) {
    			list.add(((SQLColumn)child).compile(exec));
		    }
		}

		return list;
    }

    private static class InsertInstruction extends SimpleExpression {

        public static final int CONNECTION = 0;
        public static final int FIRST_COLUMN = 1;
        String statement;

        public InsertInstruction(Expression connection, String statement, List columnInstructions) {
            Expression[] sub = new Expression[columnInstructions.size() + 1];
            sub[CONNECTION] = connection;
            for (int i=0; i<columnInstructions.size(); i++) {
                sub[i+FIRST_COLUMN] = (Expression)columnInstructions.get(i);
            }
            this.statement = statement;
            setArguments(sub);
        }

        /**
         * A subclass must provide one of the methods evaluateItem(), iterate(), or process().
         * This method indicates which of the three is provided.
         */

        public int getImplementationMethod() {
            return Expression.EVALUATE_METHOD;
        }

        public String getExpressionType() {
            return "sql:insert";
        }

//        public Expression promote(PromotionOffer offer) throws XPathException {
//            if (offer.action != PromotionOffer.FOCUS_INDEPENDENT && offer.action != PromotionOffer.EXTRACT_GLOBAL_VARIABLES) {
//                // TODO: See bug 1971291, test case sql002. Suppressing the promotion of sql:column instructions is a rather
//                // crude solution to the bug; a better approach would be to allow them to be promoted, and avoid the assumption
//                // that the arguments at run-time will always be SQLColumn instructions. Also, it would be nice to get
//                // rid of the type hierarchy abuse: SQLColumn should not really extend GeneralVariable. See also similar
//                // logic in SQLUpdate.
//                return super.promote(offer);
//            } else {
//                return this;
//            }
//        }

        public Item evaluateItem(XPathContext context) throws XPathException {

            // Prepare the SQL statement (only do this once)

            Item conn = arguments[CONNECTION].evaluateItem(context);
            if (!(conn instanceof ObjectValue && ((ObjectValue)conn).getObject() instanceof Connection) ) {
                dynamicError("Value of connection expression is not a JDBC Connection", SaxonErrorCode.SXSQ0001, context);
            }
            Connection connection = (Connection)((ObjectValue)conn).getObject();
            PreparedStatement ps = null;

            try {
              	ps=connection.prepareStatement(statement);

                // Add the actual column values to be inserted

                int i = 1;
                for (int c=FIRST_COLUMN; c<arguments.length; c++) {
                    AtomicValue v = (AtomicValue)arguments[c].evaluateItem(context);

         			// TODO: the values are all strings. There is no way of adding to a numeric column
           		    String val = v.getStringValue();

           		    // another hack: setString() doesn't seem to like single-character string values
           		    if (val.length()==1) val += " ";
           		    //System.err.println("Set statement parameter " + i + " to " + val);
           			ps.setObject(i++, val);

    	        }

    			ps.executeUpdate();
    			if (!connection.getAutoCommit()) {
                    connection.commit();
                }

    	    } catch (SQLException ex) {
    			dynamicError("SQL INSERT failed: " + ex.getMessage(), SaxonErrorCode.SXSQ0004, context);
            } finally {
               if (ps != null) {
                   try {
                       ps.close();
                   } catch (SQLException ignore) {}
               }
            }

            return null;
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
