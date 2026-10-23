package simpledb.plan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Prints a plan as numbered steps, in the order they are executed:
 * <pre>
 * 1. Select: keep rows of student where gradyear = 2020 (estimated cost = 154, estimated rows = 200)
 * 2. Hash Join: join result and enroll on sid = studentid (estimated cost = 851, estimated rows = 598)
 * 3. Project: output sname and grade (estimated cost = 0, estimated rows = 598)
 * Total estimated cost: 1005 block accesses
 * </pre>
 * Every operator is a step, except base tables, which are named
 * directly. A step's input is "result" if it is the previous step,
 * or "result of step N" otherwise.
 * cost is the estimated blocks accessed by that step alone
 * (including reading any base tables it takes as input),
 * so the steps add up to the total.
 * rows is the estimated number of records the step outputs.
 */
public class PlanPrinter {

	/**
	 * Formats a plan as numbered operator steps followed by its total estimated block-access cost.
	 * Base tables are named as inputs rather than emitted as separate steps.
	 *
	 * @param p the root plan to describe
	 * @return the formatted plan text
	 */
   public static String toSteps(Plan p) {
      List<String> steps = new ArrayList<>();
      addSteps(p, steps);
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < steps.size(); i++)
         sb.append(i + 1).append(". ").append(steps.get(i)).append("\n");
      sb.append("Total estimated cost: ").append(p.blocksAccessed()).append(" block accesses\n\n");
      return sb.toString();
   }

   /**
    * Adds the steps for p (children first) and returns
    * the step number of p, or 0 if p is a base table.
    */
   private static int addSteps(Plan p, List<String> steps) {
      List<Plan> kids = p.children();
      if (kids.isEmpty() && p instanceof TablePlan)
         return 0;

      List<Integer> kidsteps = new ArrayList<>();
      for (Plan kid : kids)
         kidsteps.add(addSteps(kid, steps));

      // "result" is only unambiguous for a single input that was the previous step
      int numsteps = 0;
      for (int s : kidsteps)
         if (s > 0)
            numsteps++;
      List<String> inputs = new ArrayList<>();
      // a plan's cost includes its inputs' costs, so take away the
      // inputs that are steps of their own to get this step's own cost
      // (base tables are not steps, so reading them is counted here)
      long owncost = p.blocksAccessed();
      for (int i = 0; i < kids.size(); i++) {
         int s = kidsteps.get(i);
         if (s == 0)
            inputs.add(kids.get(i).describe(null));
         else {
            owncost -= kids.get(i).blocksAccessed();
            if (numsteps == 1 && s == steps.size())
               inputs.add("result");
            else
               inputs.add("result of step " + s);
         }
      }
      // e.g. a nested loops join whose outer is estimated empty never runs its inner
      owncost = Math.max(0, owncost);
      steps.add(p.describe(inputs) + " (estimated cost = " + owncost + ", estimated rows = " + p.recordsOutput() + ")");
      return steps.size();
   }

   /**
    * Joins items as "a", "a and b", or "a, b and c".
    */
   public static String list(Collection<?> items) {
      StringBuilder sb = new StringBuilder();
      Iterator<?> it = items.iterator();
      int i = 0;
      while (it.hasNext()) {
         Object item = it.next();
         if (i > 0)
            sb.append(it.hasNext() ? ", " : " and ");
         sb.append(item);
         i++;
      }
      return sb.toString();
   }
}
