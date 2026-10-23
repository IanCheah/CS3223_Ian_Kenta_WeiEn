package simpledb.materialize;

import simpledb.query.*;
import simpledb.record.Schema;

/**
 * The interface implemented by aggregation functions.
 * Aggregation functions are used by the <i>groupby</i> operator.
 * @author Edward Sciore
 */
public interface AggregationFn {
	
	/**
	 * Clear the accumulated state before evaluating a new group
	 */
	void reset();
   
   /**
    * Use the current record of the specified scan
    * to be the first record in the group.
    * @param s the scan to aggregate over.
    */
   void processFirst(Scan s);
   
   /**
    * Use the current record of the specified scan
    * to be the next record in the group.
    * @param s the scan to aggregate over.
    */
   void processNext(Scan s);
   
   /**
    * Return the name of the new aggregation field.
    * @return the name of the new aggregation field
    */
   String fieldName();
   
   /**
    * Return the source field consumed by this aggregate.
    * @return the input field name
    */
   String inputField();
   
   /**
    * Return the computed aggregation value.
    * @return the computed aggregation value
    */
   Constant value();
   
   /**
    * Validates the input field and adds the aggregate output field to the result schema.
    * Implementations may also use the input type to configure evaluation. Call this
    * method before accumulating values.
    *
    * @param input the schema of the rows being aggregated
    * @param output the schema to receive the aggregate result field
    * @throws IllegalArgumentException if the input field is missing or its type is unsupported
    */
   void addResultField(Schema input, Schema output);
   
   
   /**
    * Returns true if this function has a defined value for zero input rows (like COUNT returning 0)
    */
   boolean hasValueForEmptyInput();
}
