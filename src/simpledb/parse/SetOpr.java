package simpledb.parse;

/** The set operations supported between 2 select blocks */
public enum SetOpr {
	UNION("union"),
	UNION_ALL("union all"),
	INTERSECT("intersect"),
	EXCEPT("except");
	
	private final String label;
	
	SetOpr(String label) {
		this.label = label;
	}
	
	@Override
	public String toString() {
		return this.label;
	}
}
