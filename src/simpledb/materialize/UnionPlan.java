package simpledb.materialize;

import java.util.Arrays;
import java.util.List;

import simpledb.plan.Plan;
import simpledb.query.Scan;
import simpledb.record.Schema;

/**
 * Plan for the UNION ALL operation.
 * Concatenates records from underlying queries
 */
public class UnionPlan implements Plan {
	private Plan left;
	private Plan right;
	private Schema schema;
	
	/**
	 * Creates a union plan.
	 * 
	 * @param left the 1st query plan
	 * @param right the 2nd query plan
	 * @param schema the unified output schema
	 */
	public UnionPlan(Plan left, Plan right, Schema schema) {
		this.left = left;
        this.right = right;
        this.schema = schema;
	}
	
	/**
	 * Creates the execution scan for this plan
	 */
	public Scan open() {
		return new UnionScan(
			left.open(),
			right.open(),
			left.schema().fields(),
			right.schema().fields()
		);
	}

    public int blocksAccessed() {
        return left.blocksAccessed() + right.blocksAccessed();
    }

    public int recordsOutput() {
        return left.recordsOutput() + right.recordsOutput();
    }
    
    public int distinctValues(String fldname) {
        int i = schema.fields().indexOf(fldname);
        String rf = right.schema().fields().get(i);
        return Math.min(recordsOutput(), left.distinctValues(fldname) + right.distinctValues(rf));
    }

    /**
     * Returns the unified output schema.
     */
    public Schema schema() {
    	return schema;
    }

    /**
     * Returns a string description of the plan.
     */
    public String describe(List<String> inputs) {
    	return "UNION ALL: append " + inputs.get(1) + " to " + inputs.get(0);
    }
    
    /**
     * Returns the child nodes of this plan.
     */
    public List<Plan> children() {
        return Arrays.asList(left, right);
    }
}
