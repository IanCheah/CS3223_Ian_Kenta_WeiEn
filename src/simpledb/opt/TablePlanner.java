package simpledb.opt;

import java.util.Arrays;
import java.util.List;
import java.util.Map;


import simpledb.tx.Transaction;
import simpledb.record.*;
import simpledb.query.*;
import simpledb.metadata.*;
import simpledb.index.planner.*;
import simpledb.multibuffer.MultibufferProductPlan;
import simpledb.plan.*;
import simpledb.materialize.MergeJoinPlan;
import simpledb.materialize.HashJoinPlan;


/**
 * This class contains methods for planning a single table.
 * @author Edward Sciore
 */
class TablePlanner {
   private String tblname;
   private TablePlan myplan;
   private Predicate mypred;
   private Schema myschema;
   private Map<String,IndexInfo> indexes;
   private Transaction tx;
   
   /**
    * Creates a new table planner.
    * The specified predicate applies to the entire query.
    * The table planner is responsible for determining
    * which portion of the predicate is useful to the table,
    * and when indexes are useful.
    * @param tblname the name of the table
    * @param mypred the query predicate
    * @param tx the calling transaction
    */
   public TablePlanner(String tblname, Predicate mypred, Transaction tx, MetadataMgr mdm) {
      this.mypred  = mypred;
      this.tx  = tx;
      this.tblname = tblname;
      myplan   = new TablePlan(tx, tblname, mdm);
      myschema = myplan.schema();
      indexes  = mdm.getIndexInfo(tblname, tx);
   }
   
   /**
    * Chooses the cheapest selection plan using a table scan or an index.
    * @return a select plan for the table.
    */
   public Plan makeSelectPlan() {
      Plan bestplan = addSelectPred(myplan);
      for (String fldname : indexes.keySet()) {
         Constant val = mypred.equatesWithConstant(fldname);
         if (val == null)
            continue;
         Plan p = new IndexSelectPlan(myplan, indexes.get(fldname), val);
         // the index already applies "fldname = val", so only the other terms are left
         Predicate selectpred = mypred.selectSubPred(myschema).withoutConstant(fldname, val);
         if (selectpred != null)
            p = new SelectPlan(p, selectpred);
         if (cost(p) < cost(bestplan))
            bestplan = p;
      }
      return bestplan;
   }
   
   /**
    * Returns the name of the table this planner is for.
    * @return the table name
    */
   public String tableName() {
      return tblname;
   }
   
   /**
    * Chooses the cheapest join allowed by the configured join mode.
    * @param current the specified plan
    * @return a join plan of the plan and this table
    */
   public Plan makeJoinPlan(Plan current) {
      Schema currsch = current.schema();
      Predicate joinpred = mypred.joinSubPred(myschema, currsch);
      if (joinpred == null)
         return null;
      
      // Lab 7: build all candidate joins (null if not applicable)
      String mode = System.getProperty("simpledb.joinMode", "all");
      List<Plan> candidates = new java.util.ArrayList<>();
      switch (mode) {
         case "nested":
            candidates.add(makeNestedLoopsJoin(current, currsch));
            break;
         case "merge":
            candidates.add(makeMergeSortJoin(current, currsch));
            break;
         case "hash":
            candidates.add(makeHashJoin(current, currsch));
            break;
         case "index":
            candidates.add(makeIndexJoin(current, currsch));
            break;
         case "index_nested":
            candidates.add(makeIndexJoin(current, currsch));
            candidates.add(makeNestedLoopsJoin(current, currsch));
            break;
         case "all":
            candidates.add(makeIndexJoin(current, currsch));
            candidates.add(makeHashJoin(current, currsch));
            candidates.add(makeMergeSortJoin(current, currsch));
            candidates.add(makeNestedLoopsJoin(current, currsch));
            break;
         default:
            throw new IllegalArgumentException("Unknown join mode: " + mode);
      }

      // Pick the cheapest one
      Plan bestplan = null;
      for (Plan p : candidates) {
         if (p == null)
            continue;
         if (bestplan == null || cost(p) < cost(bestplan))
            bestplan = p;
      }
      return bestplan;
   }
   
   /**
    * The estimated cost of a plan, in blocks accessed.
    * Very bad plans can overflow an int and turn negative,
    * so treat those as the most expensive.
    */
   static int cost(Plan p) {
      int c = p.blocksAccessed();
      return c < 0 ? Integer.MAX_VALUE : c;
   }
   
   /**
    * Constructs a product plan of the specified plan and
    * this table.
    * @param current the specified plan
    * @return a product plan of the specified plan and this table
    */
   public Plan makeProductPlan(Plan current) {
      Plan p = makeSelectPlan();
      return new MultibufferProductPlan(tx, current, p);
   }
   
   
   /**
    * Builds a sort-merge join using an equality that connects this table to the current plan.
    * Pushes this table's selections below the join and applies any remaining join conditions afterward.
    *
    * @param current the plan for the tables already joined
    * @param currsch the schema of the current plan
    * @return the join plan, or null if no connecting equality is found
    */
   private Plan makeMergeSortJoin(Plan current, Schema currsch) {
	   for (String fldname : this.myschema.fields()) {
		   String outerfield = this.mypred.equatesWithField(fldname);
		   if (outerfield != null && currsch.hasField(outerfield)) {
			   Plan p = new MergeJoinPlan(tx, current, makeSelectPlan(), outerfield, fldname);
			   return addRemainingJoinPred(p, currsch, fldname, outerfield);
		   }
	   }
	   return null;
   }

   /**
    * Builds a partition-based hash join using a connecting equality.
    * Pushes this table's selections below the join and applies any remaining join conditions afterward.
    *
    * @param current the plan for the tables already joined
    * @param currsch the schema of the current plan
    * @return the join plan, or null if no connecting equality is found
    */
   private Plan makeHashJoin(Plan current, Schema currsch) {
	   for (String fldname : this.myschema.fields()) {
		   String outerfield = this.mypred.equatesWithField(fldname);
		   if (outerfield != null && currsch.hasField(outerfield)) {
			   Plan p = new HashJoinPlan(tx, current, makeSelectPlan(), outerfield, fldname);
			   return addRemainingJoinPred(p, currsch, fldname, outerfield);
		   }
	   }
	   return null;
   }

   /**
    * Builds a nested-loops join using the predicate connecting this table to the current plan.
    * Applies this table's local selections before joining.
    *
    * @param current the plan for the tables already joined
    * @param currsch the schema of the current plan
    * @return the join plan, or null if there is no connecting predicate
    */
   private Plan makeNestedLoopsJoin(Plan current, Schema currsch) {
	   Predicate joinpred = this.mypred.joinSubPred(myschema, currsch);
	   if (joinpred == null)
		   return null;
	   Plan p = makeSelectPlan();
	   return new NestedLoopsJoinPlan(current, p, joinpred);
   }

   // The index join probes the table itself, so the selection
   // on this table can only be applied after the join.
   private Plan makeIndexJoin(Plan current, Schema currsch) {
      for (String fldname : indexes.keySet()) {
         String outerfield = mypred.equatesWithField(fldname);
         if (outerfield != null && currsch.hasField(outerfield)) {
            IndexInfo ii = indexes.get(fldname);
            Plan p = new IndexJoinPlan(current, myplan, ii, outerfield);
            p = addSelectPred(p);
            return addRemainingJoinPred(p, currsch, fldname, outerfield);
         }
      }
      return null;
   }
   
   private Plan addSelectPred(Plan p) {
      Predicate selectpred = mypred.selectSubPred(myschema);
      if (selectpred != null)
         return new SelectPlan(p, selectpred);
      else
         return p;
   }

   /**
    * Adds the join terms other than "fldname=outerfield", which the
    * join itself already enforces. Re-applying that term would also
    * count its selectivity twice in recordsOutput().
    */
   private Plan addRemainingJoinPred(Plan p, Schema currsch, String fldname, String outerfield) {
      Predicate joinpred = mypred.joinSubPred(currsch, myschema);
      if (joinpred == null)
         return p;
      joinpred = joinpred.withoutEquality(fldname, outerfield);
      if (joinpred != null)
         return new SelectPlan(p, joinpred);
      else
         return p;
   }
}
