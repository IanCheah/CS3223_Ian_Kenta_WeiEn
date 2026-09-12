package simpledb.query;

/**
 * Encapsulates a field and the direction - asc or desc
 */
public class SortField {
	// Naming it ascending because that is the default behaviour
	private String field;
	private boolean isAsc;
	
	public SortField(String field, boolean isAsc) {
		this.field = field;
		this.isAsc = isAsc;
	}
	
	public String getFieldName() {
		return this.field;
	}
	
	public boolean isAscending() {
		return this.isAsc;
	}
	
	public String toString() {
		String direction = this.isAsc ? "asc" : "desc";
		return this.field + " " + direction; 
	}
}
