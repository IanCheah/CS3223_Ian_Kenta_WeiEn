package simpledb.materialize;

import java.util.Arrays;
import java.util.List;

import simpledb.parse.SetOpr;
import simpledb.plan.Plan;
import simpledb.query.Scan;
import simpledb.record.Schema;
import simpledb.tx.Transaction;

/**
 * Plan for INTERSECT and EXCEPT operations using the
 * existing sort-merge strategy.
 */
public class SortedSetOprPlan implements Plan {
	private Transaction tx;
	
	private Plan leftPlan;
	private Plan sortedLeftPlan;
	
	private SetOpr operation;
	
	private Plan rightPlan;
	private Plan sortedRightPlan;
	
	private Schema schema;
	
	/**
	 * Creates a new sort-merge plan using the input left and right plan.
	 * 
	 * @param tx         the calling transaction
	 * @param operation  the set operation (INTERSECT or EXCEPT)
	 * @param leftPlan   the 1st query plan
	 * @param rightPlan  the 2nd query plan
	 * @param schema     the unifies output schema
	 */
	public SortedSetOprPlan(
			Transaction tx,
			SetOpr operation,
			Plan leftPlan,
			Plan rightPlan,
			Schema schema) {
		this.tx = tx;
		this.operation = operation;
		this.leftPlan = leftPlan;
		this.rightPlan = rightPlan;
		this.schema = schema;
		
		this.sortedLeftPlan = SortPlan.onFields(tx, leftPlan, leftPlan.schema().fields());
		this.sortedRightPlan = SortPlan.onFields(tx, rightPlan, rightPlan.schema().fields());
	}

	@Override
	public Scan open() {
		return new SortedSetOprScan(
	            operation, 
	            sortedLeftPlan.open(), 
	            sortedRightPlan.open(),
	            leftPlan.schema().fields(), 
	            rightPlan.schema().fields()
	        );
	}

	@Override
	public int blocksAccessed() {
		long cost = (long) sortedLeftPlan.blocksAccessed() + sortedRightPlan.blocksAccessed()
        		  + new MaterializePlan(tx, leftPlan).blocksAccessed()
        		  + new MaterializePlan(tx, rightPlan).blocksAccessed();
		return (int) Math.min(Integer.MAX_VALUE, cost);
	}

	@Override
	public int recordsOutput() {
		int leftRecords = leftPlan.recordsOutput();
        int rightRecords = rightPlan.recordsOutput();
        
        if (operation == SetOpr.INTERSECT) {
            return Math.min(leftRecords, rightRecords);
        } else {
            return Math.max(1, leftRecords - Math.min(leftRecords, rightRecords) / 2);
        }
	}

	@Override
	public int distinctValues(String fldname) {
		return Math.max(1, Math.min(recordsOutput(), leftPlan.distinctValues(fldname)));
	}

	@Override
	public Schema schema() {
		return schema;
	}
	
	public String describe(List<String> inputs) {
		if (operation == SetOpr.INTERSECT) {
            return "Intersect: keep rows of " + inputs.get(0) + " AND in " + inputs.get(1);
        } else {
            return "Except: keep rows of " + inputs.get(0) + " BUT not in " + inputs.get(1);
        }
	}
	
	public List<Plan> children() {
		return Arrays.asList(sortedLeftPlan, sortedRightPlan);
	}
}
