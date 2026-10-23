package simpledb.materialize;

import java.util.*;
import simpledb.tx.Transaction;
import simpledb.record.*;
import simpledb.plan.Plan;
import simpledb.plan.PlanPrinter;
import simpledb.query.*;

/**
 * The Plan class for the <i>sort</i> operator.
 * @author Edward Sciore
 */
public class SortPlan implements Plan {
   private Transaction tx;
   private Plan p;
   private Schema sch;
   private RecordComparator comp;
   private List<SortField> sortFields;
   
   /**
    * Create a sort plan for the specified query.
    * @param p the plan for the underlying query
    * @param sortfields the fields to sort by
    * @param tx the calling transaction
    */
   public SortPlan(Transaction tx, Plan p, List<SortField> sortFields) {
      this.tx = tx;
      this.p = p;
      sch = p.schema();
      comp = new RecordComparator(sortFields);
      this.sortFields = sortFields;
   }
   
   /**
    * A factory method for backward compatibility.
    * All fields are sorted in the default ascending order.
    * @param p the plan for the underlying query
    * @param fldnames the fields to be sorted in asc order
    * @param tx the calling transaction
    */
   public static SortPlan onFields(Transaction tx, Plan p, List<String> fldnames) {
	   List<SortField> sortFields = new ArrayList<SortField>();
	   for (String fldname : fldnames) {
		   sortFields.add(new SortField(fldname, true));
	   }
	   return new SortPlan(tx, p, sortFields);
   }
   
   /**
    * This method is where most of the action is.
    * Up to 2 sorted temporary tables are created,
    * and are passed into SortScan for final merging.
    * @see simpledb.plan.Plan#open()
    */
   public Scan open() {
      Scan src = p.open();
      List<TempTable> runs = splitIntoRuns(src);
      src.close();
      while (runs.size() > 2)
         runs = doAMergeIteration(runs);
      return new SortScan(runs, comp);
   }
   
   /**
    * Return the number of block accesses required to sort the input:
    * the cost of the input itself, plus the external merge sort
    * cost of the pages it outputs (see sortCost).
    * @see simpledb.plan.Plan#blocksAccessed()
    */
   public int blocksAccessed() {
      // reading the input (at its own cost, since it is pipelined into
      // the first pass), plus sorting the M pages it outputs
      long C = p.blocksAccessed();
      int M = new MaterializePlan(tx, p).blocksAccessed(); // not opened; just for analysis
      return (int) Math.min(Integer.MAX_VALUE, C + sortCost(tx, M));
   }

   /**
    * External merge sort cost for N pages:
    * 2N * (1 + ceil(log_{B-1}(ceil(N/B))))
    * where B is the number of available buffers.
    */
   public static long sortCost(Transaction tx, int N) {
      if (N == 0)
         return 0;
      int B = Math.max(3, tx.availableBuffs());
      double numRuns = Math.ceil((double) N / B);
      int numPasses = 1 + (int) Math.ceil(Math.log(numRuns) / Math.log(B - 1));
      return 2L * N * numPasses;
   }
   
   /**
    * Return the number of records in the sorted table,
    * which is the same as in the underlying query.
    * @see simpledb.plan.Plan#recordsOutput()
    */
   public int recordsOutput() {
      return p.recordsOutput();
   }
   
   /**
    * Return the number of distinct field values in
    * the sorted table, which is the same as in
    * the underlying query.
    * @see simpledb.plan.Plan#distinctValues(java.lang.String)
    */
   public int distinctValues(String fldname) {
      return p.distinctValues(fldname);
   }
   
   /**
    * Return the schema of the sorted table, which
    * is the same as in the underlying query.
    * @see simpledb.plan.Plan#schema()
    */
   public Schema schema() {
      return sch;
   }
   
   private List<TempTable> splitIntoRuns(Scan src) {
      List<TempTable> temps = new ArrayList<>();
      src.beforeFirst();
      if (!src.next()) {
    	 temps.add(new TempTable(tx, sch));
         return temps;
      }
      TempTable currenttemp = new TempTable(tx, sch);
      temps.add(currenttemp);
      UpdateScan currentscan = currenttemp.open();
      while (copy(src, currentscan))
         if (comp.compare(src, currentscan) < 0) {
         // start a new run
         currentscan.close();
         currenttemp = new TempTable(tx, sch);
         temps.add(currenttemp);
         currentscan = (UpdateScan) currenttemp.open();
      }
      currentscan.close();
      return temps;
   }
   
   private List<TempTable> doAMergeIteration(List<TempTable> runs) {
      List<TempTable> result = new ArrayList<>();
      while (runs.size() > 1) {
         TempTable p1 = runs.remove(0);
         TempTable p2 = runs.remove(0);
         result.add(mergeTwoRuns(p1, p2));
      }
      if (runs.size() == 1)
         result.add(runs.get(0));
      return result;
   }
   
   private TempTable mergeTwoRuns(TempTable p1, TempTable p2) {
      Scan src1 = p1.open();
      Scan src2 = p2.open();
      TempTable result = new TempTable(tx, sch);
      UpdateScan dest = result.open();
      
      boolean hasmore1 = src1.next();
      boolean hasmore2 = src2.next();
      while (hasmore1 && hasmore2)
         if (comp.compare(src1, src2) < 0)
         hasmore1 = copy(src1, dest);
      else
         hasmore2 = copy(src2, dest);
      
      if (hasmore1)
         while (hasmore1)
         hasmore1 = copy(src1, dest);
      else
         while (hasmore2)
         hasmore2 = copy(src2, dest);
      src1.close();
      src2.close();
      dest.close();
      return result;
   }
   
   private boolean copy(Scan src, UpdateScan dest) {
      dest.insert();
      for (String fldname : sch.fields())
         dest.setVal(fldname, src.getVal(fldname));
      return src.next();
   }

   /**
    * Describes this sort as a step of the printed query plan.
    * @see simpledb.plan.Plan#describe(java.util.List)
    */
   public String describe(List<String> inputs) {
      List<String> fields = new ArrayList<>();
      for (SortField sf : sortFields)
         fields.add(sf.getFieldName() + (sf.isAscending() ? " ascending" : " descending"));
      return "Sort: sort " + inputs.get(0) + " by " + PlanPrinter.list(fields);
   }

   /**
    * Returns the input plans of this sort, for printing the query plan.
    * @see simpledb.plan.Plan#children()
    */
   public List<Plan> children() {
      return Arrays.asList(p);
   }
}
