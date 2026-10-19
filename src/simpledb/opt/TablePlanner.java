package simpledb.opt;

import java.util.LinkedHashMap;
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
   private String lastjoin; // name of the join picked by the last makeJoinPlan call
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
    * Constructs a select plan for the table.
    * The plan will use an indexselect, if possible.
    * @return a select plan for the table.
    */
   public Plan makeSelectPlan() {
      Plan p = makeIndexSelect();
      if (p == null)
         p = myplan;
      return addSelectPred(p);
   }
   
   public String tableName() {
      return tblname;
   }
   
   public String lastJoin() {
      return lastjoin;
   }
   
   /**
    * Constructs a join plan of the specified plan
    * and the table.  Every join algorithm that applies
    * is built, and the one with the lowest estimated
    * cost (blocks accessed) is returned.
    * The method returns null if no join is possible.
    * @param current the specified plan
    * @return a join plan of the plan and this table
    */
   public Plan makeJoinPlan(Plan current) {
      Schema currsch = current.schema();
      Predicate joinpred = mypred.joinSubPred(myschema, currsch);
      if (joinpred == null)
         return null;
      
      // Lab 7: build all candidate joins (null if not applicable)
      Map<String,Plan> candidates = new LinkedHashMap<>();
      candidates.put("index join", makeIndexJoin(current, currsch));
      candidates.put("hash join", makeHashJoin(current, currsch));
      candidates.put("sort-merge join", makeMergeSortJoin(current, currsch));
      candidates.put("nested loops join", makeNestedLoopsJoin(current, currsch));
      
      // Pick the cheapest one
      String bestname = null;
      Plan bestplan = null;
      for (String name : candidates.keySet()) {
         Plan p = candidates.get(name);
         if (p == null)
            continue;
         if (bestplan == null || cost(p) < cost(bestplan)) {
            bestname = name;
            bestplan = p;
         }
      }
      lastjoin = bestname;
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
      Plan p = addSelectPred(myplan);
      return new MultibufferProductPlan(tx, current, p);
   }
   
   private Plan makeIndexSelect() {
      for (String fldname : indexes.keySet()) {
         Constant val = mypred.equatesWithConstant(fldname);
         if (val != null) {
            IndexInfo ii = indexes.get(fldname);
            System.out.println("index on " + fldname + " used");
            return new IndexSelectPlan(myplan, ii, val);
         }
      }
      return null;
   }
   
   // New method added for lab4 parta. Copied makeIndexJoin
   private Plan makeMergeSortJoin(Plan current, Schema currsch) {
	   for (String fldname : this.myschema.fields()) {
		   String outerfield = this.mypred.equatesWithField(fldname);
		   if (outerfield != null && currsch.hasField(outerfield)) {
			   Plan p = new MergeJoinPlan(tx, current, myplan, outerfield, fldname);
			   p = addSelectPred(p);
			   return addJoinPred(p, currsch);
		   }
	   }
	   return null;
   }
   
   // New method added for lab5
   private Plan makeHashJoin(Plan current, Schema currsch) {
	   for (String fldname : this.myschema.fields()) {
		   String outerfield = this.mypred.equatesWithField(fldname);
		   if (outerfield != null && currsch.hasField(outerfield)) {
			   Plan p = new HashJoinPlan(tx, current, myplan, outerfield, fldname);
			   p = addSelectPred(p);
			   return addJoinPred(p, currsch);
		   }
	   }
	   return null;
   }
   
   // New method added for lab4 partc.
   private Plan makeNestedLoopsJoin(Plan current, Schema currsch) {
	   Predicate joinpred = this.mypred.joinSubPred(myschema, currsch);
	   if (joinpred == null)
		   return null;
	   Plan p = addSelectPred(this.myplan);
	   return new NestedLoopsJoinPlan(current, p, joinpred);
   }
   
   private Plan makeIndexJoin(Plan current, Schema currsch) {
      for (String fldname : indexes.keySet()) {
         String outerfield = mypred.equatesWithField(fldname);
         if (outerfield != null && currsch.hasField(outerfield)) {
            IndexInfo ii = indexes.get(fldname);
            Plan p = new IndexJoinPlan(current, myplan, ii, outerfield);
            p = addSelectPred(p);
            return addJoinPred(p, currsch);
         }
      }
      return null;
   }
   
   private Plan makeProductJoin(Plan current, Schema currsch) {
	  System.out.println("PRODUCT JOIN");
	  System.out.println("current: " + current);
	  System.out.println("currsch: " + currsch);
      Plan p = makeProductPlan(current);
      return addJoinPred(p, currsch);
   }
   
   private Plan addSelectPred(Plan p) {
      Predicate selectpred = mypred.selectSubPred(myschema);
      if (selectpred != null)
         return new SelectPlan(p, selectpred);
      else
         return p;
   }
   
   private Plan addJoinPred(Plan p, Schema currsch) {
      Predicate joinpred = mypred.joinSubPred(currsch, myschema);
      if (joinpred != null)
         return new SelectPlan(p, joinpred);
      else
         return p;
   }
}
