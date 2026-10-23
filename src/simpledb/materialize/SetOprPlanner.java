package simpledb.materialize;

import java.util.ArrayList;
import java.util.List;
import static java.sql.Types.VARCHAR;

import simpledb.parse.SetOpr;
import simpledb.plan.Plan;
import simpledb.record.Schema;
import simpledb.tx.Transaction;

/**
 * Builds plans for UNION, UNION ALL, INTERSECT, and EXCEPT.
 * Matches result columns by position, requires matching types, and retains the
 * left input's field names. String result lengths accommodate either input.
 */
public class SetOprPlanner {
	
	/**
	 * Builds execution plan for 2 plans and a set operation
	 * 
	 * @param tx    the calling transaction
	 * @param left  the left input plan
	 * @param opr   the set operation
	 * @param right the right input plan
	 * @return the appropriate execution plan, UnionPlan or SortedSetOprPlan
	 */
	public static Plan combine(Transaction tx, Plan leftPlan, SetOpr opr, Plan rightPlan) {
		Schema unifiedSchema = unifySchema(leftPlan.schema(), rightPlan.schema());
		
		switch (opr) {
		case UNION_ALL:
			return new UnionPlan(leftPlan, rightPlan, unifiedSchema);
		
		case UNION:
			return new GroupByPlan(
					tx,
					new UnionPlan(leftPlan, rightPlan, unifiedSchema),
					unifiedSchema.fields(),
					new ArrayList<>()
				);
		
		case INTERSECT:
			return new SortedSetOprPlan(tx, opr, leftPlan, rightPlan, unifiedSchema);
		
		case EXCEPT:
			return new SortedSetOprPlan(tx, opr, leftPlan, rightPlan, unifiedSchema);
			
		default:
			throw new IllegalArgumentException("Unknown set operation: " + opr);
		}
	}
	
	
	/**
	 * Creates and validates a unified schema
	 */
	static Schema unifySchema(Schema leftSchema, Schema rightSchema) {
        List<String> leftFields = leftSchema.fields();
        List<String> rightFields = rightSchema.fields();
        
        if (leftFields.size() != rightFields.size()) {
            throw new IllegalArgumentException(
                "Schema mismatch: left query has " + leftFields.size() + 
                " columns, right query has " + rightFields.size()
            );
        }
        
        Schema out = new Schema();
        
        for (int i = 0; i < leftFields.size(); i++) {
            String leftField = leftFields.get(i);
            String rightField = rightFields.get(i);
            
            if (leftSchema.type(leftField) != rightSchema.type(rightField)) {
                throw new IllegalArgumentException(
                    "Type mismatch at column " + (i + 1) + 
                    ": '" + leftField + "' vs '" + rightField + "'"
                );
            }
            
            int length = leftSchema.length(leftField);
            if (leftSchema.type(leftField) == VARCHAR) {
                length = Math.max(length, rightSchema.length(rightField));
            }
            
            out.addField(leftField, leftSchema.type(leftField), length);
        }
        
        return out;
    }
}
