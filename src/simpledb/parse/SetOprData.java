package simpledb.parse;

import java.util.List;
import simpledb.query.SortField;

/**
 * Encapsulates the data of several select statements combined with 
 * set operation, e.g, "q1 union q2 except q3"
 * 
 * The order by clause applied to the whole result.
 */
public class SetOprData {
	private final List<QueryData> queries;
	private final List<SetOpr> oprs;
	private final List<SortField> sortFields;
	
	/**
	 * Stores SELECT blocks, their connecting operations, and the final result ordering.
	 * The lists are retained directly; callers supply one fewer operation than SELECT blocks.
	 *
	 * @param queries the SELECT blocks in textual order
	 * @param oprs the operations between adjacent blocks, evaluated left to right
	 * @param sortFields the ordering applied to the combined result
	 */
	public SetOprData(List<QueryData> queries, List<SetOpr> oprs, List<SortField> sortFields) {
		this.queries = queries;
		this.oprs = oprs;
		this.sortFields = sortFields;
	}
	
	/**
	 * Returns the queries that describes each select block
	 * 
	 * @return the List of QueryData
	 */
	public List<QueryData> queries() {
		return this.queries;
	}
	
	/**
	 * Returns the operations joining 2 select blocks
	 * 
	 * @return the List of SetOpr
	 */
	public List<SetOpr> oprs() {
		return this.oprs;
	}
	
	/**
	 * Returns the sortFields specified after ORDER BY
	 * 
	 * @return the List of SortField
	 */
	public List<SortField> sortFields() {
		return this.sortFields;
	}
	
	@Override 
	public String toString() {
		if (this.queries == null || queries.isEmpty()) {
			return "";
		}
		
		StringBuilder sb = new StringBuilder(queries.get(0).toString());
		
		for (int i = 0; i < this.oprs.size(); i++) {
			sb.append(" ")
			  .append(this.oprs.get(i))
			  .append(" ")
			  .append(this.queries.get(i + 1));
		}
		
		if (this.sortFields == null || this.sortFields.isEmpty()) {
			return sb.toString();
		}
		
		sb.append(" order by ");
		
		int numSortFields = this.sortFields.size();
		for (int i = 0; i < numSortFields; i++) {
			sb.append(this.sortFields.get(i));
			if (i < numSortFields - 1) {
				sb.append(", ");
			}
		}
		
		return sb.toString();
	}
}
