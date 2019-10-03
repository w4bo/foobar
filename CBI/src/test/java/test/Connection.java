package test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.ResultSet;
import java.sql.Statement;

import org.junit.Test;

import it.unibo.conversational.database.DBmanager;

/**
 * Test database connection.
 */
public class Connection {

  private void count(final String table) throws Exception {
    try (
        Statement stmt = DBmanager.getConnection().createStatement();
        ResultSet res = stmt.executeQuery("select * from `" + table + "`")
    ) {
      assertTrue(table + " is empty", res.first());
    } catch (final Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  /**
   * Test non empty tables.
   * @throws Exception in case of error
   */
  @Test
  public void testNonEmptyTables() throws Exception {
    count(DBmanager.tabTABLE);
    count(DBmanager.tabRELATIONSHIP);
    count(DBmanager.tabCOLUMN);
    count(DBmanager.tabDATABASE);
    count(DBmanager.tabFACT);
    count(DBmanager.tabHiF);
    count(DBmanager.tabHIERARCHY);
    count(DBmanager.tabLEVEL);
    count(DBmanager.tabMEMBER);
    count(DBmanager.tabMEASURE);
    count(DBmanager.tabGROUPBYOPERATOR);
    count(DBmanager.tabSYNONYM);
    count(DBmanager.tabLANGUAGEPREDICATE);
    count(DBmanager.tabGRBYOPMEASURE); // MUST BY POPULATED MANUALLY
    // count(DBmanager.tabLEVELROLLUP); MUST BY POPULATED MANUALLY
  }
}
