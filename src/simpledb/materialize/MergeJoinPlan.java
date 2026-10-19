package simpledb.materialize;

import simpledb.tx.Transaction;
import simpledb.plan.Plan;
import simpledb.query.*;
import simpledb.record.*;

import java.util.*;

/**
 * The Plan class for the <i>mergejoin</i> operator.
 * @author Edward Sciore
 */
public class MergeJoinPlan implements Plan {
   private Transaction tx;
   private Plan p1, p2;
   private Plan unsorted1, unsorted2;
   private String fldname1, fldname2;
   private Schema sch = new Schema();
   
   /**
    * Creates a mergejoin plan for the two specified queries.
    * The RHS must be materialized after it is sorted, 
    * in order to deal with possible duplicates.
    * @param p1 the LHS query plan
    * @param p2 the RHS query plan
    * @param fldname1 the LHS join field
    * @param fldname2 the RHS join field
    * @param tx the calling transaction
    */
   public MergeJoinPlan(Transaction tx, Plan p1, Plan p2, String fldname1, String fldname2) {
      this.tx = tx;
      this.unsorted1 = p1;
      this.unsorted2 = p2;
      this.fldname1 = fldname1;
      List<String> sortlist1 = Arrays.asList(fldname1);
      this.p1 = SortPlan.onFields(tx, p1, sortlist1);
      
      this.fldname2 = fldname2;
      List<String> sortlist2 = Arrays.asList(fldname2);
      this.p2 = SortPlan.onFields(tx, p2, sortlist2);
      
      sch.addAll(p1.schema());
      sch.addAll(p2.schema());
   }
   
   /** The method first sorts its two underlying scans
     * on their join field. It then returns a mergejoin scan
     * of the two sorted table scans.
     * @see simpledb.plan.Plan#open()
     */
   public Scan open() {
      Scan s1 = p1.open();
      SortScan s2 = (SortScan) p2.open();
      return new MergeJoinScan(s1, s2, fldname1, fldname2);
   }
   
   /**
    * Return the number of block accesses required to
    * sort both inputs and then mergejoin them.
    * Cost = sortCost(M) + sortCost(N) + (M + N),
    * where M and N are the costs of the two inputs
    * (same as HashJoinPlan and NestedLoopsJoinPlan).
    * @see simpledb.plan.Plan#blocksAccessed()
    */
   public int blocksAccessed() {
      int M = unsorted1.blocksAccessed();
      int N = unsorted2.blocksAccessed();
      return sortCost(M) + sortCost(N) + M + N;
   }
   
   // External merge sort cost from lecture:
   // 2N * (1 + ceil(log_{B-1}(ceil(N/B))))
   private int sortCost(int N) {
      if (N == 0)
         return 0;
      int B = Math.max(3, tx.availableBuffs());
      double numRuns = Math.ceil((double) N / B);
      int numPasses = 1 + (int) Math.ceil(Math.log(numRuns) / Math.log(B - 1));
      return 2 * N * numPasses;
   }
   
   /**
    * Return the number of records in the join.
    * Assuming uniform distribution, the formula is:
    * <pre> R(join(p1,p2)) = R(p1)*R(p2)/max{V(p1,F1),V(p2,F2)}</pre>
    * @see simpledb.plan.Plan#recordsOutput()
    */
   public int recordsOutput() {
      int maxvals = Math.max(p1.distinctValues(fldname1),
                             p2.distinctValues(fldname2));
      return (p1.recordsOutput() * p2.recordsOutput()) / maxvals;
   }
   
   /**
    * Estimate the distinct number of field values in the join.
    * Since the join does not increase or decrease field values,
    * the estimate is the same as in the appropriate underlying query.
    * @see simpledb.plan.Plan#distinctValues(java.lang.String)
    */
   public int distinctValues(String fldname) {
      if (p1.schema().hasField(fldname))
         return p1.distinctValues(fldname);
      else
         return p2.distinctValues(fldname);
   }
   
   /**
    * Return the schema of the join,
    * which is the union of the schemas of the underlying queries.
    * @see simpledb.plan.Plan#schema()
    */
   public Schema schema() {
      return sch;
   }
}

