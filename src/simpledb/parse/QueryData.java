package simpledb.parse;

import java.util.*;


import simpledb.query.*;
import simpledb.materialize.AggregationFn;

/**
 * Data for the SQL <i>select</i> statement.
 * 
 * @author Edward Sciore
 */
public class QueryData {
	private List<String> fields;
	private Collection<String> tables;
	private Predicate pred;
	private List<SortField> sortFields;
	
	// Fields for aggregate and Group By
	private boolean isDistinct;
	private List<String> groupFields;
	private List<AggregationFn> aggFns;

	/**
	 * Saves the field and table list and predicate.
	 */
	public QueryData(
			List<String> fields,
			Collection<String> tables,
			Predicate pred,
			List<SortField> sortfields,
			boolean isDistinct,
			List<String> groupFields,
			List<AggregationFn> aggFns) {
		this.fields = fields;
		this.tables = tables;
		this.pred = pred;
		this.sortFields = sortfields;
		
		// Populate new fields to support aggregate / group by
		this.isDistinct = isDistinct;
		this.groupFields = groupFields;
		this.aggFns = aggFns;
	}

	/**
	 * Returns the fields mentioned in the select clause.
	 * 
	 * @return a list of field names
	 */
	public List<String> fields() {
		return fields;
	}

	/**
	 * Returns the tables mentioned in the from clause.
	 * 
	 * @return a collection of table names
	 */
	public Collection<String> tables() {
		return tables;
	}

	/**
	 * Returns the predicate that describes which records should be in the output
	 * table.
	 * 
	 * @return the query predicate
	 */
	public Predicate pred() {
		return pred;
	}

	public List<SortField> sortFields() {
		return this.sortFields;
	}
	
	public boolean isDistinct() {
		return this.isDistinct;
	}

	public List<String> groupFields() {
		return this.groupFields;
	}
	
	public List<AggregationFn> aggFns() {
		return this.aggFns;
	}
	
	public String toString() {
		String result = "select ";
		for (String fldname : fields)
			result += fldname + ", ";
		
		if (this.isDistinct) {
			result += "distinct ";
		}
		
		result = result.substring(0, result.length() - 2); // remove final comma
		result += " from ";
		for (String tblname : tables)
			result += tblname + ", ";
		result = result.substring(0, result.length() - 2); // remove final comma
		String predstring = pred.toString();
		if (!predstring.equals(""))
			result += " where " + predstring;
		
		if (!groupFields.isEmpty()) {
			result += " group by ";
			for (String field : groupFields) {
				result += field + ", ";
			}
			result = result.substring(0, result.length() - 2);
		}
		
		if (!(this.sortFields.size() == 0)) {
			result += " order by ";

			for (int i = 0; i < this.sortFields.size(); i++) {
				result += sortFields.get(i).toString();

				if (i < sortFields.size() - 1) {
					result += ", ";
				}
			}
		}
		return result;
	}
}
