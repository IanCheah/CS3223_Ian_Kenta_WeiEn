package simpledb.opt;

import java.util.*;

import simpledb.tx.Transaction;
import simpledb.materialize.SortPlan;
import simpledb.materialize.GroupByPlan;
import simpledb.materialize.AggregationFn;

import simpledb.metadata.MetadataMgr;
import simpledb.parse.QueryData;
import simpledb.plan.*;

/**
 * A query planner that optimizes using a heuristic-based algorithm.
 * Lab 7: Left-deep join orders are found with System R style
 * dynamic programming. The best plan for every subset of the tables
 * is built from the best plans of its smaller subsets, by joining
 * one more table with the cheapest algorithm for that step
 * (see TablePlanner). This is O(n * 2^n) instead of the n! of
 * trying every order.
 * Cross products are only used for a subset when none of its
 * tables can be joined to the rest.
 * @author Edward Sciore
 */
public class HeuristicQueryPlanner implements QueryPlanner {
   private MetadataMgr mdm;

   /** The best plan found for one subset of the tables. */
   private static class Best {
      Plan plan;
      String order;  // tables in join order, for display

      Best(Plan plan, String order) {
         this.plan = plan;
         this.order = order;
      }
   }

   public HeuristicQueryPlanner(MetadataMgr mdm) {
      this.mdm = mdm;
   }

   public Plan createPlan(QueryData data, Transaction tx) {

      // Step 1:  Create a TablePlanner object for each mentioned table
      List<TablePlanner> tableplanners = new ArrayList<>();
      for (String tblname : data.tables()) {
         TablePlanner tp = new TablePlanner(tblname, data.pred(), tx, mdm);
         tableplanners.add(tp);
      }

      // Step 2:  Find the cheapest left-deep join order with DP
      Best best = findBestPlan(tableplanners);
      System.out.println("Best join order: " + best.order + ", cost = " + TablePlanner.cost(best.plan));
      Plan currentplan = best.plan;

      // Step 3: Group By
      if (!data.groupFields().isEmpty() || !data.aggFns().isEmpty()) {
    	  Set<String> allowedFields = new HashSet<>(data.groupFields());

    	    for (AggregationFn fn : data.aggFns()) {
    	        allowedFields.add(fn.fieldName());
    	    }

    	    for (String selected : data.fields()) {
    	        if (!allowedFields.contains(selected)) {
    	            throw new IllegalArgumentException(
    	                "Selected field must appear in GROUP BY "
    	                + "or be aggregated: " + selected
    	            );
    	        }
    	    }

    	    currentplan = new GroupByPlan(tx, currentplan, data.groupFields(), data.aggFns());
      }

      // Step 4:  Project
      Plan p = new ProjectPlan(currentplan, data.fields());

      // Step 5: Distinct
      if (data.isDistinct()) {
    	  p = new GroupByPlan(tx, p, data.fields(), new ArrayList<AggregationFn>());
      }

      // Step 6: Order By
      if (!data.sortFields().isEmpty()) {
    	  p = new SortPlan(tx, p, data.sortFields());
      }

      System.out.print("Query plan:\n" + PlanPrinter.toSteps(p));
      return p;
   }

   /**
    * System R style dynamic programming over subsets of the tables.
    * A subset is a bitmask: bit i is set if table i is in it.
    * best[mask] is the cheapest left-deep plan joining exactly those tables.
    * Removing a table from a mask gives a smaller number, so going
    * through the masks in increasing order always builds the
    * smaller subsets first.
    */
   private Best findBestPlan(List<TablePlanner> tps) {
      int n = tps.size();
      Best[] best = new Best[1 << n];

      // subsets of one table: the cheapest way to read it (scan or index select)
      for (int i = 0; i < n; i++) {
         TablePlanner tp = tps.get(i);
         Plan p = tp.makeSelectPlan();
         best[1 << i] = new Best(p, tp.tableName());
      }

      for (int mask = 1; mask < (1 << n); mask++) {
         if (Integer.bitCount(mask) < 2)
            continue;
         // join one more table onto the best plan of the rest
         best[mask] = extend(tps, best, mask, false);
         // none of the tables can be joined to the rest, so use a product
         if (best[mask] == null)
            best[mask] = extend(tps, best, mask, true);
      }

      Best result = best[(1 << n) - 1];
      result.order = "[" + result.order + "]";
      return result;
   }

   /**
    * Finds the cheapest plan for the subset mask that puts one of its
    * tables on top of the best plan for the other tables.
    * Uses joins only, or products only if product is true.
    * Returns null if no such plan exists.
    */
   private Best extend(List<TablePlanner> tps, Best[] best, int mask, boolean product) {
      Best result = null;
      // last table first, so that ties keep the tables in the order
      // the query lists them (same as the old exhaustive search)
      for (int i = tps.size() - 1; i >= 0; i--) {
         if ((mask & (1 << i)) == 0)
            continue;
         Best prev = best[mask & ~(1 << i)];
         if (prev == null)
            continue;
         TablePlanner tp = tps.get(i);
         Plan p = product ? tp.makeProductPlan(prev.plan) : tp.makeJoinPlan(prev.plan);
         if (p == null)
            continue;
         if (result == null || TablePlanner.cost(p) < TablePlanner.cost(result.plan))
            result = new Best(p, prev.order + ", " + tp.tableName());
      }
      return result;
   }

   public void setPlanner(Planner p) {
      // for use in planning views, which
      // for simplicity this code doesn't do.
   }
}
