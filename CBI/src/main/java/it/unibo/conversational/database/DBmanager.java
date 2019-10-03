package it.unibo.conversational.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;

import com.google.common.collect.Sets;

import it.unibo.conversational.Utils;

/** Handling connection to database. */
public class DBmanager {

  protected DBmanager() {
  }

  protected static String id(String s) {
    return s + "_id";
  }

  protected static String name(String s) {
    return s + "_name";
  }

  protected static String type(String s) {
    return s + "_type";
  }

  protected static String synonyms(String s) {
    return s + "_synonyms";
  }

  /** Table types. */
  public enum TableTypes {
    /** Fact table. */
    FT,
    /** Dimension table. */
    DT
  }

  /****************************/
  public static final String tabTABLE = "table";
  public static final String tabRELATIONSHIP = "relationship";
  public static final String tabCOLUMN = "column";
  public static final String tabQueryBI = "queries_bi";
  public static final String tabQuery = "queries";
  public static final String tabDATABASE = "database";
  public static final String tabFACT = "fact";
  public static final String tabHiF = "hierarchy_in_fact";
  public static final String tabHIERARCHY = "hierarchy";
  public static final String tabLEVEL = "level";
  public static final String tabMEMBER = "member";
  public static final String tabMEASURE = "measure";
  public static final String tabGROUPBYOPERATOR = "groupbyoperator";
  public static final String tabSYNONYM = "synonym";
  public static final String tabGRBYOPMEASURE = "groupbyoperator_of_measure";
  public static final String tabLANGUAGEPREDICATE = "language_predicate";
  public static final String tabLEVELROLLUP = "level_rollup";
  public static final Set<String> tabsWithSyns = Sets.newHashSet(tabFACT, tabLEVEL, tabMEASURE, tabMEMBER, tabLANGUAGEPREDICATE);

  public static final String colRELTAB1 = "table1";
  public static final String colRELTAB2 = "table2";
  public static final String colCOLISKEY = "isKey";
  public static final String colDBIP = "IPaddress";
  public static final String colDBPORT = "port";
  public static final String colLEVELCARD = "cardinality";
  public static final String colLEVELMIN = "min";
  public static final String colLEVELMAX = "max";
  public static final String colLEVELAVG = "avg";
  public static final String colLEVELMINDATE = "mindate";
  public static final String colLEVELMAXDATE = "maxdate";
  public static final String colLEVELRUSTART = "start";
  public static final String colLEVELRUTO = "level_to";
  public static final String colGROUPBYOPNAME = "operator";
  public static final String colSYNTERM = "term";
  public static final String colQueryID = "id";
  public static final String colQueryText = "query";
  public static final String colQueryGBset = "gc";
  public static final String colQuerySelClause = "sc";
  public static final String colQueryMeasClause = "mc";
  public static final String colQueryGPSJ = "gpsj";
  /****************************/

  private static Connection connSchemaDB;
  private static String schemadb;
  private static String password;
  private static String username;
  private static String port;
  private static String ip;

  public static final Connection getConnection() {
    if (connSchemaDB == null) {
      ip = Utils.credentialsFromFile()[0];
      port = Utils.credentialsFromFile()[1];
      username = Utils.credentialsFromFile()[2];
      password = Utils.credentialsFromFile()[3];
      schemadb = Utils.credentialsFromFile()[6];
      final String host = "jdbc:mysql://" + ip + ":" + port;
      final String schemaDBstringConnection = host + "/" + schemadb;
      try {
        Class.forName("com.mysql.cj.jdbc.Driver");
        connSchemaDB = DriverManager.getConnection(schemaDBstringConnection, username, password);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return connSchemaDB;
  }

  /**
   * Execute the query and return a result.
   * @param query query to execute
   */
  public static final void executeQuery(final String query) {
    try (PreparedStatement pstmt = connSchemaDB.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
      pstmt.executeUpdate();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  /**
   * Execute the query and return an integer.
   * @param query query to execute
   * @return integer
   */
  public static final int executeQueryReturnID(final String query) {
    try (PreparedStatement pstmt = connSchemaDB.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
      pstmt.executeUpdate();
      final ResultSet generatedKeys = pstmt.getGeneratedKeys();
      generatedKeys.next();
      return generatedKeys.getInt(1);
    } catch (final SQLException e) {
      e.printStackTrace();
      return -1;
    }
  }
}
