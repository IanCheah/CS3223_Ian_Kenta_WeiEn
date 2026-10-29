package simpledb.test;

import java.util.List;
import java.util.Scanner;

import simpledb.plan.Plan;
import simpledb.query.Scan;
import simpledb.record.Schema;
import simpledb.server.SimpleDB;
import simpledb.tx.Transaction;

/**
 * A minimal interactive SQL prompt for SimpleDB.
 * Type a statement and press Enter; a blank line quits.
 * Each statement runs in its own transaction, so a bad
 * statement rolls back without ending the session.
 */
public class QueryRunner {
   public static void main(String[] args) {
      SimpleDB db = new SimpleDB("demodb_no_index");
      Scanner sc = new Scanner(System.in);
      System.out.println("SimpleDB query runner. Enter SQL, or a blank line to quit.");
      System.out.print("SQL> ");
      while (sc.hasNextLine()) {
         String cmd = sc.nextLine().trim();
         if (cmd.isEmpty())
            break;
         Transaction tx = db.newTx();
         try {
            if (cmd.toLowerCase().startsWith("select"))
               doQuery(db, tx, cmd);
            else
               System.out.println(db.planner().executeUpdate(cmd, tx) + " records processed.");
            tx.commit();
         }
         catch (Exception e) {
            System.out.println("Error: " + e);
            e.printStackTrace();
            tx.rollback();
         }
         System.out.print("SQL> ");
      }
      sc.close();
      System.out.println("Bye.");
   }

   private static void doQuery(SimpleDB db, Transaction tx, String qry) {
      long start = System.currentTimeMillis();
      Plan p = db.planner().createQueryPlan(qry, tx);
      Schema sch = p.schema();
      List<String> flds = sch.fields();

      for (String fldname : flds)
         System.out.print(fldname + "\t");
      System.out.println();
      for (int i = 0; i < flds.size(); i++)
         System.out.print("--------");
      System.out.println();

      Scan s = p.open();
      int count = 0;
      while (s.next()) {
         for (String fldname : flds)
            System.out.print(s.getVal(fldname) + "\t");
         System.out.println();
         count++;
      }
      s.close();
      long end = System.currentTimeMillis();
      System.out.println("(" + count + " rows)");
      System.out.println("Time taken: " + (end - start) + " ms");
   }
}
