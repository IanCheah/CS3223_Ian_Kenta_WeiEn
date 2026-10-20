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
    * Lecture: sort(M) + sort(N) + (M + N), where sort(M) = 2M * passes.
    * The inputs may be selections or joins that are pipelined into the
    * sort, so pass 0 reads them at their own cost C instead of M,
    * and every later pass works on the M pages of their output:
    * Cost = C1 + C2 + 2M * passes(M) + 2N * passes(N)
    * (for base tables C = M, which gives the lecture formula).
    * @see simpledb.plan.Plan#blocksAccessed()
    */
   public int blocksAccessed() {
      long C1 = unsorted1.blocksAccessed();
      long C2 = unsorted2.blocksAccessed();
      int M = new MaterializePlan(tx, unsorted1).blocksAccessed();
      int N = new MaterializePlan(tx, unsorted2).blocksAccessed();
      return (int) Math.min(Integer.MAX_VALUE,
            C1 + C2 + SortPlan.sortCost(tx, M) + SortPlan.sortCost(tx, N));
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
      long recs = (long) p1.recordsOutput() * p2.recordsOutput() / maxvals;
      return (int) Math.min(Integer.MAX_VALUE, recs);
   }
   
   /**
    * Estimate the distinct number of field values in the join:
    * the smaller of the two inputs' values for the join field,
    * otherwise the input's value capped by the number of output records.
    * @see simpledb.plan.Plan#distinctValues(java.lang.String)
    */
   public int distinctValues(String fldname) {
      // lecture (opt slides 65-66): V(U,A) = min(V(R1,A), V(R2,A)) for the
      // join column A, and V(U,B) = min(||U||, V(R1,B)) for any other column B
      if (fldname.equals(fldname1) || fldname.equals(fldname2))
         return Math.max(1, Math.min(p1.distinctValues(fldname1), p2.distinctValues(fldname2)));
      return Math.max(1, Math.min(recordsOutput(), p1.schema().hasField(fldname) ? p1.distinctValues(fldname) : p2.distinctValues(fldname)));
   }
   
   /**
    * Return the schema of the join,
    * which is the union of the schemas of the underlying queries.
    * @see simpledb.plan.Plan#schema()
    */
   public Schema schema() {
      return sch;
   }

   public String describe(List<String> inputs) {
      return "Sort-Merge Join: sort " + inputs.get(0) + " and " + inputs.get(1)
            + ", then merge them on " + fldname1 + " = " + fldname2;
   }

   public List<Plan> children() {
      return Arrays.asList(unsorted1, unsorted2);
   }
}
