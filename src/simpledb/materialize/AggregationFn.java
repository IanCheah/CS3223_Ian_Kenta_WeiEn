package simpledb.materialize;

import simpledb.query.*;
import simpledb.record.Schema;

/**
 * The interface implemented by aggregation functions.
 * Aggregation functions are used by the <i>groupby</i> operator.
 * @author Edward Sciore
 */
public interface AggregationFn {
	
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
   
   String inputField();
   
   /**
    * Return the computed aggregation value.
    * @return the computed aggregation value
    */
   Constant value();
   
   void addResultField(Schema input, Schema output);
   
   
   /**
    * Returns true if this function has a defined value for zero input rows (like COUNT returning 0)
    */
   boolean hasValueForEmptyInput();
}
