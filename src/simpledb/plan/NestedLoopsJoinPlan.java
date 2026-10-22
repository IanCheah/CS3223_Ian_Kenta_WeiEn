package simpledb.plan;

import java.util.Arrays;
import java.util.List;
import simpledb.query.ProductScan;
import simpledb.query.Scan;
import simpledb.record.Schema;
import simpledb.query.NestedLoopsJoinScan;
import simpledb.query.Predicate;

public class NestedLoopsJoinPlan implements Plan {
   private Plan p1, p2;
   private Schema schema = new Schema();
   private Predicate joinpred;
   
   public NestedLoopsJoinPlan(Plan p1, Plan p2, Predicate joinpred) {
      this.p1 = p1;
      this.p2 = p2;
      this.joinpred = joinpred;
      schema.addAll(p1.schema());
      schema.addAll(p2.schema());
   }
	   
   public Scan open() {
      Scan s1 = this.p1.open();
      Scan s2 = p2.open();
      return new NestedLoopsJoinScan(s1, s2, this.joinpred);
   }
   
   public int blocksAccessed() {
	  // the scan is tuple-at-a-time: the inner is rescanned for every outer record
	  long cost = p1.blocksAccessed() + ((long) p1.recordsOutput() * p2.blocksAccessed());
	  return (int) Math.min(Integer.MAX_VALUE, cost);
   }

   // This is just an estimate.
   // The selectivity uses the inputs' V values (1/max(V1, V2)),
   // not this plan's own, since those depend on recordsOutput.
   public int recordsOutput() {
      double recs = (double) p1.recordsOutput() * p2.recordsOutput() * joinpred.selectivity(new ProductPlan(p1, p2));
      return (int) Math.min(Integer.MAX_VALUE, Math.round(recs));
   }
	   
   public int distinctValues(String fldname) {
      // V(U,A) = min(V(R1,A), V(R2,A)) for the
      // join column A, and V(U,B) = min(||U||, V(R1,B)) for any other column B
      String other = joinpred.equatesWithField(fldname);
      if (other != null)
         return Math.max(1, Math.min(inputDistinctValues(fldname), inputDistinctValues(other)));
      return Math.max(1, Math.min(recordsOutput(), inputDistinctValues(fldname)));
   }

   private int inputDistinctValues(String fldname) {
      if (p1.schema().hasField(fldname))
         return p1.distinctValues(fldname);
      else
         return p2.distinctValues(fldname);
   }
   
   public Schema schema() {
      return schema;
   }

   public String describe(List<String> inputs) {
      return "Nested-Loops Join: join " + inputs.get(0) + " and " + inputs.get(1) + " on " + joinpred;
   }

   public List<Plan> children() {
      return Arrays.asList(p1, p2);
   }
}
