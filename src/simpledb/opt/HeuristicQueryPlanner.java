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
 * Lab 7: Every left-deep join order is tried. For each order, every
 * join is the cheapest algorithm for that step (see TablePlanner).
 * The order with the lowest total cost (blocks accessed) wins.
 * Trying every order is n!, which is fine for the few tables
 * a query usually has.
 * @author Edward Sciore
 */
public class HeuristicQueryPlanner implements QueryPlanner {
   private MetadataMgr mdm;
   private Plan bestplan;
   private String bestorder;
   private String bestjoins;
   private String joins; // joins used by the plan makeLeftDeepPlan just built

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

      // Step 2:  Try every join order and keep the cheapest
      bestplan = null;
      bestorder = null;
      bestjoins = null;
      tryAllOrders(tableplanners, new ArrayList<>());
      System.out.println("Best join order: " + bestorder + ", cost = " + TablePlanner.cost(bestplan));
      System.out.println("Joins used: " + bestjoins);
      Plan currentplan = bestplan;

      // Step 3: Group By
      if (!data.groupFields().isEmpty() || !data.aggFns().isEmpty()) {
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

      return p;
   }

   /**
    * Recursively builds every permutation of the tables.
    * Each complete permutation is turned into a plan and
    * compared against the best plan so far.
    */
   private void tryAllOrders(List<TablePlanner> remaining, List<TablePlanner> order) {
      if (remaining.isEmpty()) {
         Plan p = makeLeftDeepPlan(order);
         String orderstr = orderToString(order);
         if (bestplan == null || TablePlanner.cost(p) < TablePlanner.cost(bestplan)) {
            bestplan = p;
            bestorder = orderstr;
            bestjoins = joins;
         }
         return;
      }
      for (int i = 0; i < remaining.size(); i++) {
         TablePlanner tp = remaining.remove(i);
         order.add(tp);
         tryAllOrders(remaining, order);
         order.remove(order.size() - 1);
         remaining.add(i, tp);
      }
   }

   /**
    * Joins the tables from left to right in the given order.
    * If a table has no join predicate with the tables so far,
    * a product is used instead.
    */
   private Plan makeLeftDeepPlan(List<TablePlanner> order) {
      Plan current = order.get(0).makeSelectPlan();
      List<String> used = new ArrayList<>();
      int prevcost = TablePlanner.cost(current);
      used.add("scan " + order.get(0).tableName() + " (cost = " + prevcost + ")");
      for (int i = 1; i < order.size(); i++) {
         TablePlanner tp = order.get(i);
         Plan p = tp.makeJoinPlan(current);
         String name = tp.lastJoin();
         if (p == null) {
            p = tp.makeProductPlan(current);
            name = "product";
         }
         current = p;
         // cost(p) includes everything below it, so subtract to get this join's own cost
         int stepcost = TablePlanner.cost(p) - prevcost;
         prevcost = TablePlanner.cost(p);
         used.add(name + " with " + tp.tableName() + " (cost = " + stepcost + ")");
      }
      joins = String.join(", ", used);
      return current;
   }

   private String orderToString(List<TablePlanner> order) {
      List<String> names = new ArrayList<>();
      for (TablePlanner tp : order)
         names.add(tp.tableName());
      return names.toString();
   }

   public void setPlanner(Planner p) {
      // for use in planning views, which
      // for simplicity this code doesn't do.
   }
}
