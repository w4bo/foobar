package it.unibo.conversational.datatypes;

import java.util.Objects;
import java.util.Optional;

import it.unibo.conversational.Utils.DataType;

/** A reference to an element in the DW. */
public class Entity {
  /** Primary key of the element in its table. */
  private final Optional<Integer> pkInTable;
  /** Name of the element. */
  private final String nameInTable;
  /** Reference to other table. A member refers to the corresponding level. A level refers to the corresponding table. */
  private final Optional<Integer> refToOtherTable;
  /** Name of the reference in the other table. */
  private final Optional<String> nameOtherTable;
  /** Type of the entity in the database. */
  private final Optional<DataType> typeInDB;
  /** Table of the element. */
  private final Optional<String> tableName;

  public Entity(final String nameInTable) {
    this(Optional.empty(), nameInTable, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
  }

  public Entity(final String nameInTable, final DataType typeInDB) {
    this(Optional.empty(), nameInTable, Optional.empty(), Optional.empty(), Optional.of(typeInDB), Optional.empty());
  }

  public Entity(final int pkInTable, final String nameInTable, final DataType typeInDB) {
    this(Optional.of(pkInTable), nameInTable, Optional.empty(), Optional.empty(), Optional.of(typeInDB), Optional.empty());
  }

  public Entity(final int pkInTable, final String nameInTable, final int refToOtherTable, final DataType typeInDB) {
    this(Optional.of(pkInTable), nameInTable, Optional.of(refToOtherTable), Optional.empty(), Optional.of(typeInDB), Optional.empty());
  }

  public Entity(final int pkInTable, final String nameInTable, final Optional<Integer> idref,  final Optional<String> nameref, final String table_name, final DataType type) {
    this(Optional.of(pkInTable), nameInTable, idref, nameref, Optional.of(type), Optional.of(table_name));
  }

  public Entity(final Integer pkInTable, final String nameInTable) {
    this(Optional.of(pkInTable), nameInTable, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
  }

  public Entity(final Integer pkInTable, final String nameInTable, final String tableName) {
    this(Optional.of(pkInTable), nameInTable, Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(tableName));
  }
  
  public Entity(final Integer pkInTable, final String nameInTable, final Integer refToOtherTable) {
    this(Optional.of(pkInTable), nameInTable, Optional.of(refToOtherTable), Optional.empty(), Optional.empty(), Optional.empty());
  }

  public Entity(final Optional<Integer> pkInTable, final String nameInTable, final Optional<Integer> refToOtherTable,
      final Optional<String> nameInOtherTable, final Optional<DataType> typeInDB, final Optional<String> tableName) {
    this.pkInTable = pkInTable;
    this.nameInTable = nameInTable;
    this.refToOtherTable = refToOtherTable;
    this.nameOtherTable = nameInOtherTable;
    this.typeInDB = typeInDB;
    this.tableName = tableName;
  }

  public Entity(int pkInTable, String nameInTable, String tableName, int refToOtherTable, final String otherTable,  DataType dataType) {
    this(Optional.of(pkInTable), nameInTable, Optional.of(refToOtherTable), Optional.of(otherTable), Optional.of(dataType), Optional.of(tableName));
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj instanceof Entity) {
      final Entity o = (Entity) obj;
      return pkInTable.equals(o.pkInTable)
          && nameInTable.equals(o.nameInTable)
          && refToOtherTable.equals(o.refToOtherTable)
          && nameOtherTable.equals(o.nameOtherTable)
          && typeInDB.equals(o.typeInDB)
          && tableName.equals(o.tableName);
    }
    return false;
  }

  public DataType getTypeInDB() {
    return typeInDB.orElse(DataType.OTHER);
  }

  @Override
  public int hashCode() {
    return Objects.hash(pkInTable, nameInTable, refToOtherTable, nameOtherTable, typeInDB, tableName);
  }

  public String nameInTable() {
    return nameInTable;
  }

  public int pkInTable() {
    return pkInTable.get();
  }

  public int refToOtherTable() {
    return refToOtherTable.get();
  }


  public String table() {
    return tableName.get();
  }

  @Override
  public String toString() {
    return "\"" + nameInTable + "\"";
  }
}
