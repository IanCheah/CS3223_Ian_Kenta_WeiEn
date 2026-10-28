package simpledb.test;

/**
 * Builds the demo database "demodb_no_index": exactly the same tables and rows
 * as "demodb_index" (see CreateDemoDBIndex), but without any indexes.
 * Running the same query on both databases shows the effect of the indexes
 * on the query plan, its estimated cost and its running time.
 *
 * Run once. To rebuild, delete the demodb_no_index folder first.
 */
public class CreateDemoDBNoIndex {
   public static void main(String[] args) {
      CreateDemoDBIndex.build("demodb_no_index", false);
   }
}
