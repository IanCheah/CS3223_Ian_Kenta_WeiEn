package simpledb.metadata;

import java.util.HashMap;
import java.util.Map;

import simpledb.query.Constant;

/**
 * A StatInfo object holds statistical information about a table:
 * the number of blocks, the number of records,
 * and, for each field, the number of distinct values
 * and the lowest / highest value (as in lecture: V(R,A), low, high).
 * @author Edward Sciore
 */
public class StatInfo {
   private int numBlocks;
   private int numRecs;
   private Map<String,Integer> distinct;
   private Map<String,Constant> low, high;

   /**
    * Create a StatInfo object without per-field statistics.
    * The number of distinct values is then faked.
    * @param numblocks the number of blocks in the table
    * @param numrecs the number of records in the table
    */
   public StatInfo(int numblocks, int numrecs) {
      this(numblocks, numrecs, new HashMap<>(), new HashMap<>(), new HashMap<>());
   }

   /**
    * Create a StatInfo object with per-field statistics.
    * @param numblocks the number of blocks in the table
    * @param numrecs the number of records in the table
    * @param distinct the number of distinct values of each field
    * @param low the lowest value of each field
    * @param high the highest value of each field
    */
   public StatInfo(int numblocks, int numrecs, Map<String,Integer> distinct,
                   Map<String,Constant> low, Map<String,Constant> high) {
      this.numBlocks = numblocks;
      this.numRecs   = numrecs;
      this.distinct  = distinct;
      this.low       = low;
      this.high      = high;
   }

   /**
    * Return the estimated number of blocks in the table.
    * @return the estimated number of blocks in the table
    */
   public int blocksAccessed() {
      return numBlocks;
   }

   /**
    * Return the estimated number of records in the table.
    * @return the estimated number of records in the table
    */
   public int recordsOutput() {
      return numRecs;
   }

   /**
    * Return the number of distinct values for the specified field.
    * Falls back to a guess if the field was not counted.
    * Never returns 0, since callers divide by it.
    * @param fldname the name of the field
    * @return the number of distinct field values
    */
   public int distinctValues(String fldname) {
      Integer v = distinct.get(fldname);
      if (v == null)
         return 1 + (numRecs / 3);
      return Math.max(1, v);
   }

   /**
    * Return the lowest value of the field, or null if unknown.
    */
   public Constant minValue(String fldname) {
      return low.get(fldname);
   }

   /**
    * Return the highest value of the field, or null if unknown.
    */
   public Constant maxValue(String fldname) {
      return high.get(fldname);
   }
}
