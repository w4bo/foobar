package it.unibo.conversational.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import it.unibo.conversational.Utils;
import it.unibo.conversational.algorithms.Parser.Type;
import it.unibo.conversational.datatypes.Entity;
import it.unibo.conversational.datatypes.Ngram;

/**
 * Interacting with the database SQL query.
 */
public final class QueryGeneratorChecker extends DBmanager {

  private QueryGeneratorChecker() {
  }

  /**
   * @return get the fact
   */
  public static Pair<Integer, String> getFactTable() {
    final String query = "SELECT * FROM `" + tabTABLE + "` WHERE `" + type(tabTABLE) + "` = \"" + TableTypes.FT + "\"";
    try (
        Statement stmt = getConnection().createStatement();
        ResultSet res = stmt.executeQuery(query)
    ) {
      res.first();
      return Pair.of(res.getInt(id(tabTABLE)), res.getString(name(tabTABLE)));
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * @param m member
   * @return get the level of the given member
   */
  public static Ngram getLevelOfMember(final Ngram m) {
    final String query = "SELECT * FROM `" + tabLEVEL + "` L, `" + tabCOLUMN + "` C WHERE C." + id(tabCOLUMN) + " = L." + id(tabCOLUMN) + " AND " + id(tabLEVEL) + " = " + m.mde().refToOtherTable();
    try (
        Statement stmt = getConnection().createStatement();
        ResultSet res = stmt.executeQuery(query)
    ) {
      res.first();
      final String name = res.getString(DBmanager.name(tabLEVEL));
      return new Ngram(name, Type.ATTR, new Entity(m.mde().refToOtherTable(), name, res.getInt(id(tabTABLE)), Utils.getDataType(res.getString(type(tabLEVEL)))), null);
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * @param id id of the language predicate
   * @return the <type, name> of the language predicate
   */
  public static Pair<String, String> getPredicate(final int id) {
    final String query = "SELECT " + type(tabLANGUAGEPREDICATE) + ", " + name(tabLANGUAGEPREDICATE) + " FROM `" + tabLANGUAGEPREDICATE + "` WHERE " + id(tabLANGUAGEPREDICATE) + " = " + id;
    try (
        Statement stmt = getConnection().createStatement();
        ResultSet res = stmt.executeQuery(query)
    ) {
      while (res.next()) {
        return Pair.of(res.getString(type(tabLANGUAGEPREDICATE)), res.getString(name(tabLANGUAGEPREDICATE)));
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * @return a map <measure, operators> (i.e., the operators that are appliable to the given measure)
   */
  public static Map<String, Set<Entity>> getOperatorOfMeasure() {
    final Map<String, Set<Entity>> map = Maps.newLinkedHashMap();
    final String query = "select gm." + id(tabGROUPBYOPERATOR) + ", gm." + id(tabMEASURE) + ", " + name(tabMEASURE) + ", " + name(tabGROUPBYOPERATOR) + " " 
        + "from " + tabGRBYOPMEASURE + " gm, " + tabMEASURE + " m, " + tabGROUPBYOPERATOR + " g "
        + "where g." + id(tabGROUPBYOPERATOR) + " = gm." + id(tabGROUPBYOPERATOR) + " and gm." + id(tabMEASURE) + " = m." + id(tabMEASURE) + "";
    try (
        Statement stmt = getConnection().createStatement();
        ResultSet res = stmt.executeQuery(query)
    ) {
      while (res.next()) { // for each group by operator
        final String mea = res.getString(name(tabMEASURE));
        final int id = res.getInt(id(tabGROUPBYOPERATOR));
        final String op = res.getString(name(tabGROUPBYOPERATOR));
        final Set<Entity> val = map.getOrDefault(mea, Sets.newLinkedHashSet());
        val.add(new Entity(id, op));
        map.put(mea, val);
      }
    } catch (final SQLException e) {
      e.printStackTrace();
    }
    return map;
  }

  /**
   * Save a query in the query dataset.
   * @param query nl query
   * @param gbset correct group by set
   * @param predicate correct selection clause
   * @param selclause correct measure clause
   */
  public static void saveQuery(final String query, final String gbset, final String predicate, final String selclause) {
      final String sql = "INSERT INTO `" + tabQuery + "` (`" + colQueryText + "`, `" + colQueryGBset + "`, `" + colQueryMeasClause + "`, `" + colQuerySelClause + "`) "
                            + "VALUES (\"" + query + "\", \"" + gbset + "\", \"" + selclause + "\", \"" + predicate + "\")";
      executeQuery(sql);
  }

  public static Pair<String, String> getTabDetails(int idFT, int idTable) {
    final String query = "SELECT * FROM `" + tabTABLE + "` WHERE `" + id(tabTABLE) + "` = " + idTable;
    final String query1 = "SELECT * FROM `" + tabCOLUMN + "` C INNER JOIN `" + tabRELATIONSHIP + "` R ON C." + id(tabRELATIONSHIP) + " = R." + id(tabRELATIONSHIP) + " WHERE `" + colRELTAB1 + "` = " + idFT + " AND `" + colRELTAB2 + "` = " + idTable;
    try (
        Statement stmt = getConnection().createStatement();
        ResultSet resDet = stmt.executeQuery(query);
        Statement stmt1 = getConnection().createStatement();
        ResultSet resCol = stmt1.executeQuery(query1)
    ) {
      resDet.first();
      resCol.first();
      return Pair.of(resDet.getString(name(tabTABLE)), resCol.getString(name(tabCOLUMN)));
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * Returns the member of each level.
   * @return set members (i.e., entities) for each levels
   */
  public static Map<String, Set<Entity>> getMembersofLevels() {
    final String query = "select m." + id(tabMEMBER) + ", " + name(tabMEMBER) + ", m." + id(tabLEVEL) + ", " + type(tabLEVEL) + ", " + name(tabLEVEL) + " from level l, member m where l." + id(tabLEVEL) + " = m." + id(tabLEVEL) + " limit 5";
    try (
        Statement stmt = getConnection().createStatement();
        ResultSet res = stmt.executeQuery(query);
    ) {
      final Map<String, Set<Entity>> members = Maps.newLinkedHashMap();
      while (res.next()) {
        final Set<Entity> tmp = members.getOrDefault(res.getString(name(tabLEVEL)), Sets.newLinkedHashSet());
        tmp.add(new Entity(res.getInt(id(tabMEMBER)), res.getString(name(tabMEMBER)), res.getInt(id(tabLEVEL)), Utils.getDataType(type(tabLEVEL))));
        members.put(res.getString(name(tabLEVEL)), tmp);
      }
      return members;
    } catch (final SQLException e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * Returns the member of each level.
   * @return set members (i.e., entities) for each levels
   */
  public static Map<String, Set<Entity>> getLevelsOfMembers() {
    final String query = "select * from member m, `level` a, `column` c, `table` t where c.table_id = t.table_id and a.column_id = c.column_id and m.level_id = a.level_id";
    try (
        Statement stmt = getConnection().createStatement();
        ResultSet res = stmt.executeQuery(query);
    ) {
      final Map<String, Set<Entity>> attributes = Maps.newLinkedHashMap();
      while (res.next()) {
        final Set<Entity> tmp = attributes.getOrDefault(res.getString(name(tabMEMBER)), Sets.newLinkedHashSet());
        tmp.add(new Entity(res.getInt(id(tabLEVEL)), res.getString(name(tabLEVEL)), res.getInt(id(tabTABLE)), Utils.getDataType(type(tabLEVEL))));
        attributes.put(res.getString(name(tabMEMBER)), tmp);
      }
      return attributes;
    } catch (final SQLException e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * Get levels with type "year". TODO: in this version only the level 'the_year' is retrieved. This is because it not easy to find "year" levels.
   * @return levels with type "year"
   */
  public static Set<Entity> getYearLevels() {
    final String query = "select * from `level` a, `column` c, `table` t where c.table_id = t.table_id and a.column_id = c.column_id and level_name = 'the_year'";
    try (
        Statement stmt = getConnection().createStatement();
        ResultSet res = stmt.executeQuery(query);
    ) {
      final Set<Entity> attributes = Sets.newLinkedHashSet();
      while (res.next()) {
        attributes.add(new Entity(res.getInt(id(tabLEVEL)), res.getString(name(tabLEVEL)), res.getInt(id(tabTABLE)), Utils.getDataType(type(tabLEVEL))));
      }
      return attributes;
    } catch (final SQLException e) {
      e.printStackTrace();
    }
    return null;
  }
}
