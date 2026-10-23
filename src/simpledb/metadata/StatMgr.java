package simpledb.metadata;

import java.util.*;
import simpledb.tx.Transaction;
import simpledb.record.*;
import simpledb.query.Constant;

/**
 * The statistics manager is responsible for
 * keeping statistical information about each table.
 * The manager does not store this information in the database.
 * Instead, it calculates this information on system startup,
 * and periodically refreshes it.
 * @author Edward Sciore
 */
class StatMgr {
   private TableMgr tblMgr;
   private Map<String,StatInfo> tablestats;
   private Map<String,Integer> filesizes; // file size (blocks) when the stats were taken
   private int numcalls;
   
   /**
    * Create the statistics manager.
    * The initial statistics are calculated by
    * traversing the entire database.
    * @param tx the startup transaction
    */
   public StatMgr(TableMgr tblMgr, Transaction tx) {
      this.tblMgr = tblMgr;
      refreshStatistics(tx);
   }
   
   /**
    * Return the statistical information about the specified table.
    * @param tblname the name of the table
    * @param layout the table's layout
    * @param tx the calling transaction
    * @return the statistical information about the table
    */
   public synchronized StatInfo getStatInfo(String tblname, 
                              Layout layout, Transaction tx) {
      numcalls++;
      if (numcalls > 100)
         refreshStatistics(tx);
      StatInfo si = tablestats.get(tblname);
      // recount if there are no stats yet, or the table grew or shrank
      // since they were taken (so they are stale)
      Integer filesize = filesizes.get(tblname);
      if (si == null || filesize == null || filesize != tx.size(tblname + ".tbl")) {
         si = calcTableStats(tblname, layout, tx);
         tablestats.put(tblname, si);
      }
      return si;
   }
   
   /**
    * Recalculates the statistics of every table in the catalog,
    * and resets the call counter.
    * @param tx the calling transaction
    */
   private synchronized void refreshStatistics(Transaction tx) {
      tablestats = new HashMap<String,StatInfo>();
      filesizes = new HashMap<String,Integer>();
      numcalls = 0;
      Layout tcatlayout = tblMgr.getLayout("tblcat", tx);
      TableScan tcat = new TableScan(tx, "tblcat", tcatlayout);
      while(tcat.next()) {
         String tblname = tcat.getString("tblname");
         Layout layout = tblMgr.getLayout(tblname, tx);
         StatInfo si = calcTableStats(tblname, layout, tx);
         tablestats.put(tblname, si);
      }
      tcat.close();
   }
   
   /**
    * Scans the whole table to count its blocks and records, and,
    * for each field, its number of distinct values and its lowest / highest value.
    * Also records the table's file size, so stale stats can be detected.
    * @param tblname the name of the table
    * @param layout the table's layout
    * @param tx the calling transaction
    * @return the statistics of the table
    */
   private synchronized StatInfo calcTableStats(String tblname,
                              Layout layout, Transaction tx) {
      int numRecs = 0;
      int numblocks = 0;
      List<String> fields = layout.schema().fields();
      Map<String,Set<Constant>> values = new HashMap<>();
      Map<String,Constant> low = new HashMap<>();
      Map<String,Constant> high = new HashMap<>();
      for (String fldname : fields)
         values.put(fldname, new HashSet<>());
      TableScan ts = new TableScan(tx, tblname, layout);
      while (ts.next()) {
         numRecs++;
         numblocks = ts.getRid().blockNumber() + 1;
         for (String fldname : fields) {
            Constant val = ts.getVal(fldname);
            values.get(fldname).add(val);
            if (!low.containsKey(fldname) || val.compareTo(low.get(fldname)) < 0)
               low.put(fldname, val);
            if (!high.containsKey(fldname) || val.compareTo(high.get(fldname)) > 0)
               high.put(fldname, val);
         }
      }
      ts.close();
      Map<String,Integer> distinct = new HashMap<>();
      for (String fldname : fields)
         distinct.put(fldname, values.get(fldname).size());
      filesizes.put(tblname, tx.size(tblname + ".tbl"));
      return new StatInfo(numblocks, numRecs, distinct, low, high);
   }
}
