package simpledb.test;

import simpledb.plan.Planner;
import simpledb.server.SimpleDB;
import simpledb.tx.Transaction;

/**
 * Builds the demo database "demodb_index" (at most 100 rows per table):
 *   DEPT     5 rows   (DId, DName, Budget float)
 *   STUDENT 50 rows   (SId, SName, MajorId, GradYear, GPA float)
 *   COURSE  20 rows   (CId, Title, DeptId, Credits float)
 *   SECTION 40 rows   (SectId, CourseId, Prof, YearOffered)
 *   ENROLL 100 rows   (EId, StudentId, SectionId, Grade, Score float)
 *
 * Indexes:
 *   STUDENT(MajorId) hash, STUDENT(SId) btree, STUDENT(GPA) btree (float key),
 *   COURSE(DeptId) hash, SECTION(SectId) hash, ENROLL(StudentId) btree.
 *
 * The data is fixed (no randomness), so every run produces the same database.
 * Names repeat (20 names for 50 students) and every student has 2 enrollments,
 * so joins return duplicate rows for the DISTINCT demo.
 * Run once. To rebuild, delete the database folder first.
 *
 * CreateDemoDBNoIndex builds "demodb_no_index": the SAME rows, but no indexes
 * at all, to compare plans, costs and running times with and without indexes.
 */
public class CreateDemoDBIndex {
   static final String[] NAMES = {"joe", "amy", "max", "sue", "bob", "kim", "art", "pat", "lee", "tom",
                                  "ann", "ben", "eve", "ian", "zoe", "ray", "liz", "dan", "meg", "sam"};
   static final String[] PROFS = {"turing", "newton", "einstein", "brando", "curie", "darwin", "hopper", "lovelace"};
   static final String[] GRADES = {"A", "A", "B+", "B", "B", "C", "C", "D", "F", "A"};

   public static void main(String[] args) {
      build("demodb_index", true);
   }

   /**
    * Creates the tables and inserts the rows into the database dbname.
    * Also used by CreateDemoDBNoIndex, so both databases hold exactly the same rows.
    * @param dbname the database folder to create
    * @param withIndexes whether to create the indexes
    */
   static void build(String dbname, boolean withIndexes) {
      SimpleDB db = new SimpleDB(dbname);
      Planner planner = db.planner();

      // ---------- DEPT: 5 rows ----------
      Transaction tx = db.newTx();
      planner.executeUpdate("create table DEPT(DId int, DName varchar(8), Budget float)", tx);
      String[][] depts = {{"10", "compsci", "1250.5"}, {"20", "math", "980.25"}, {"30", "drama", "450.75"},
                          {"40", "physics", "1100.0"}, {"50", "history", "620.5"}};
      for (String[] d : depts)
         planner.executeUpdate("insert into DEPT(DId, DName, Budget) values ("
               + d[0] + ", '" + d[1] + "', " + d[2] + ")", tx);
      tx.commit();
      System.out.println("DEPT: 5 rows");

      // ---------- STUDENT: 50 rows ----------
      tx = db.newTx();
      planner.executeUpdate("create table STUDENT(SId int, SName varchar(10), MajorId int, GradYear int, GPA float)", tx);
      if (withIndexes) {
         planner.executeUpdate("create index idx_major on STUDENT (MajorId) using hash", tx);
         planner.executeUpdate("create index idx_sid on STUDENT (SId) using btree", tx);
         planner.executeUpdate("create index idx_gpa on STUDENT (GPA) using btree", tx);
      }
      tx.commit();
      for (int i = 1; i <= 50; i++) {
         tx = db.newTx();
         String name = NAMES[(i + i / 20) % NAMES.length];   // each name used 2-3 times
         int major = 10 * (1 + (i * 7) % 5);                  // 10..50, 10 students each
         int grad = 2019 + (i * 5) % 6;                       // 2019..2024
         float gpa = 2.0f + ((i * 7) % 9) * 0.25f;            // 2.0, 2.25, ..., 4.0 (exact in float)
         planner.executeUpdate("insert into STUDENT(SId, SName, MajorId, GradYear, GPA) values ("
               + i + ", '" + name + "', " + major + ", " + grad + ", " + gpa + ")", tx);
         tx.commit();
      }
      System.out.println("STUDENT: 50 rows");

      // ---------- COURSE: 20 rows ----------
      tx = db.newTx();
      planner.executeUpdate("create table COURSE(CId int, Title varchar(20), DeptId int, Credits float)", tx);
      if (withIndexes) planner.executeUpdate("create index idx_deptid on COURSE (DeptId) using hash", tx);
      tx.commit();
      String[] titles = {"db systems", "compilers", "networks", "algorithms",
                         "calculus", "algebra", "statistics", "geometry",
                         "acting", "elocution", "playwriting", "stagecraft",
                         "mechanics", "optics", "quantum", "relativity",
                         "ancient rome", "world war", "asia", "renaissance"};
      float[] credits = {4.0f, 4.0f, 3.5f, 4.0f, 2.5f, 3.0f, 3.5f, 2.5f, 2.0f, 1.5f,
                         2.0f, 1.5f, 4.0f, 3.0f, 4.5f, 3.5f, 2.0f, 2.5f, 3.0f, 2.0f};
      for (int i = 0; i < titles.length; i++) {
         tx = db.newTx();
         int cid = 12 + 10 * i;                               // 12, 22, ..., 202
         int dept = 10 * (1 + i / 4);                         // 4 courses per dept
         planner.executeUpdate("insert into COURSE(CId, Title, DeptId, Credits) values ("
               + cid + ", '" + titles[i] + "', " + dept + ", " + credits[i] + ")", tx);
         tx.commit();
      }
      System.out.println("COURSE: 20 rows");

      // ---------- SECTION: 40 rows ----------
      tx = db.newTx();
      planner.executeUpdate("create table SECTION(SectId int, CourseId int, Prof varchar(8), YearOffered int)", tx);
      if (withIndexes) planner.executeUpdate("create index idx_sectid on SECTION (SectId) using hash", tx);
      tx.commit();
      for (int i = 0; i < 40; i++) {
         tx = db.newTx();
         int sectid = 13 + 10 * i;                            // 13, 23, ..., 403
         int courseid = 12 + 10 * (i % 20);                   // every course has 2 sections
         String prof = PROFS[i % PROFS.length];
         int year = 2016 + (i * 5) % 8;                       // 2016..2023
         planner.executeUpdate("insert into SECTION(SectId, CourseId, Prof, YearOffered) values ("
               + sectid + ", " + courseid + ", '" + prof + "', " + year + ")", tx);
         tx.commit();
      }
      System.out.println("SECTION: 40 rows");

      // ---------- ENROLL: 100 rows ----------
      tx = db.newTx();
      planner.executeUpdate("create table ENROLL(EId int, StudentId int, SectionId int, Grade varchar(2), Score float)", tx);
      if (withIndexes) planner.executeUpdate("create index idx_studentId on ENROLL (StudentId) using btree", tx);
      tx.commit();
      for (int i = 0; i < 100; i++) {
         tx = db.newTx();
         int eid = 14 + 10 * i;
         int studentid = 1 + (i * 13) % 50;                   // every student enrolled exactly twice
         int sectionid = 13 + 10 * ((i * 7) % 40);
         String grade = GRADES[i % GRADES.length];
         float score = 40.0f + ((i * 17) % 61) + ((i % 2 == 0) ? 0.5f : 0.0f);   // 40.0 .. 100.5
         planner.executeUpdate("insert into ENROLL(EId, StudentId, SectionId, Grade, Score) values ("
               + eid + ", " + studentid + ", " + sectionid + ", '" + grade + "', " + score + ")", tx);
         tx.commit();
      }
      System.out.println("ENROLL: 100 rows");
      System.out.println(dbname + " created" + (withIndexes ? " (with indexes)." : " (no indexes)."));
   }
}
