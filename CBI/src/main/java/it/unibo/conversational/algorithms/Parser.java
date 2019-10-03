package it.unibo.conversational.algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import it.unibo.conversational.Utils.DataType;
import it.unibo.conversational.database.QueryGeneratorChecker;
import it.unibo.conversational.datatypes.Entity;
import it.unibo.conversational.datatypes.Mapping;
import it.unibo.conversational.datatypes.Ngram;
import it.unibo.conversational.datatypes.Ngram.ErrorType;

/**
 * Handling grammar and paring.
 */
public final class Parser {

  private static Map<String, Set<Entity>> operatorOfMeasure = QueryGeneratorChecker.getOperatorOfMeasure();
  private static Map<String, Set<Entity>> membersofLevels = QueryGeneratorChecker.getMembersofLevels();
  private static Map<String, Set<Entity>> levelsOfMembers = QueryGeneratorChecker.getLevelsOfMembers();
  private static Set<Entity> yearLevels = QueryGeneratorChecker.getYearLevels();

  private Parser() {
  }

  /**
   * Rules type.
   */
  public enum Type {
    /** Measure clause. */
    MC,
    /** Measure. */
    MEA,
    /** Fact name. */
    FACT,
    /** Hierarchy. */
    H,
    /** Measure aggregation. */
    OP,
    /** Group by `by`. */
    GB,
    /** Group by `where`. */
    WHR,
    /** Group by clause. */
    GC,
    /** Level. */
    ATTR,
    /** Comparison operator. */
    COP,
    /** Selection clause. */
    SC,
    /** Value. */
    VAL,
    /** Between operator. */
    BETWEEN,
    /** Logical and. */
    AND,
    /** Logical or. */
    OR,
    /** Logical not. */
    NOT,
    /** `Select`. */
    SELECT,
    /** Container for not mapped tokens. */
    BIN,
    /** Query. */
    Q,
    /** Count, count distinct. */
    COUNT,
    /** Dummy container for Servlet purpose. */
    FOO;
  }

  /**
   * Type of rules used for the parsing.
   */
  public enum Rule {
    /** MC ::= OP MEA. */
    M1(Type.MC, Type.OP, Type.MEA), // M1 is more generic than M3
    /** MC ::= MEA OP. */
    M2(Type.MC, Type.MEA, Type.OP), // M2 is more generic than M3
    /** MC ::= Count fact. */
    M5(Type.MC, Type.COUNT, Type.FACT),
    /** MC ::= MC MC. */
    M4(Type.MC, Type.MC, Type.MC),
    /** MEA ::= OP MEA. */
    M3(Type.MC, Type.MEA),
    // M5(Type.MC, false, Type.COUNT, Type.FACT),//

    /** GBC ::= BY LEV. */
    G1(Type.GC, Type.GB, Type.ATTR), //
    /** GBC ::= GBC LEV. */
    G2(Type.GC, Type.GC, Type.ATTR), //
    // /** GBC ::= LEV. */
    // G3(Type.GC, false, Type.ATTR), // do not apply G3 before S*, otherwise it won't recognize levels for SC

    /** SC ::= LEV COP VAL. */
    S1(Type.SC, Type.ATTR, Type.COP, Type.VAL),
    /** SC ::= VAL COP LEV. */
    S2(Type.SC, Type.VAL, Type.COP, Type.ATTR),
    /** SC ::= LEV VAL. */
    S3(Type.SC, Type.ATTR, Type.VAL),
    /** SC ::= VAL LEV. */
    S4(Type.SC, Type.VAL, Type.ATTR),
    /** SC ::= SC AND SC. */
    S5(Type.SC, Type.SC, Type.AND, Type.SC),
    /** SC ::= SC OR SC. */
    S6(Type.SC, Type.SC, Type.OR, Type.SC),
    /** SC ::= VAL. */
    S7(Type.SC, Type.VAL),

    /** Q ::= GBC SC MC. */
    Q1(Type.Q, Type.GC, Type.SC, Type.MC),
    /** Q ::= GBC SC MC. */
    Q2(Type.Q, Type.GC, Type.MC, Type.SC),
    /** Q ::= GBC SC MC. */
    Q3(Type.Q, Type.SC, Type.GC, Type.MC),
    /** Q ::= GBC SC MC. */
    Q4(Type.Q, Type.SC, Type.MC, Type.GC),
    /** Q ::= GBC SC MC. */
    Q5(Type.Q, Type.MC, Type.GC, Type.SC),
    /** Q ::= GBC SC MC. */
    Q6(Type.Q, Type.MC, Type.SC, Type.GC),
    /** Q ::= GBC MC. */
    Q7(Type.Q, Type.MC, Type.GC),
    /** Q ::= GBC MC. */
    Q8(Type.Q, Type.GC, Type.MC),
    /** Q ::= SC MC. */
    Q9(Type.Q, Type.MC, Type.SC),
    /** Q ::= SC MC. */
    Q10(Type.Q, Type.SC, Type.MC),
    /** Q ::= MC. */
    Q11(Type.Q, Type.MC);

    private final Type[] elements;
    private final Type ret;

    /**
     * Create a rule.
     * @param ret return type
     * @param acceptPermutation whether the rule accept permutation
     * @param elements list of elements
     */
    Rule(final Type ret, final Type... elements) {
      this.ret = ret;
      this.elements = elements;
    }

    /**
     * Check whether a list of ngrams match this rule.
     * @param ngrams list of ngrams
     * @return true if the list of ngrams match this rule
     */
    public boolean match(final List<Ngram> ngrams) {
      boolean found = ngrams.size() == elements.length;
      for (int i = 0; i < ngrams.size() && found; i++) {
        found = found && ngrams.get(i).type.equals(elements[i]);
      }
      return found;
    }
  }


  /**
   * Parse a mapping.
   * @param mapping mapping to be translated
   * @return parsing interpretations sorted by number of matched entities
   */
  public static Optional<Mapping> parse(final Mapping mapping) {
    final Set<Mapping> res = parse(Lists.newArrayList(//
        new Rule[] { Rule.M1, Rule.M2, Rule.M5, Rule.M3, Rule.M4 }, //
        new Rule[] { Rule.G1, Rule.G2 }, //
        new Rule[] { Rule.S1, Rule.S2, Rule.S3, Rule.S4, Rule.S5, Rule.S6, Rule.S7 }, //
        new Rule[] { Rule.Q1, Rule.Q2, Rule.Q3, Rule.Q4, Rule.Q5, Rule.Q6, Rule.Q7, Rule.Q8, Rule.Q9, Rule.Q10, Rule.Q11 }//
    ), mapping);
    return res.isEmpty() ? Optional.empty()
        : res.stream() //
            .filter(s -> s.ngrams.stream().anyMatch(n -> n.type.equals(Type.Q))) //
            .max(Mapping::compareMappings);
  }

  public static void typeCheck(final Mapping m) {
    for (Ngram n : m.ngrams.stream().filter(n -> !n.children.isEmpty()).collect(Collectors.toList())) {
      typeCheck(n, operatorOfMeasure, membersofLevels, levelsOfMembers);
    }
  }

  /**
   * Type check the generated parsing tree and annotate the ngrams that do not satisfy the type checking.
   * @param ngram parse tree
   * @param mea2op operator/measure constraints
   * @param attr2val attribute/value constraints
   * @param val2attr value/attribute constraints
   * @return the annotated parse tree
   */
  public static Ngram typeCheck(final Ngram ngram,
      final Map<String, Set<Entity>> mea2op,
      final Map<String, Set<Entity>> attr2val,
      final Map<String, Set<Entity>> val2attr) {
    final Set<Ngram> clauses = Ngram.simpleClauses(ngram);
    for (final Ngram c : clauses) {
      switch (c.type) {
      case MC:
        if (c.children.size() == 2) { // the clause contains (operator + measure | count fact )
          final Optional<Ngram> op = c.children.stream().filter(cc -> cc.type.equals(Type.OP)).findAny();
          if (op.isPresent()) { // (operator + measure)
            final Ngram mea = c.children.stream().filter(cc -> cc.type.equals(Type.MEA)).findAny().get();
            if (!mea2op.isEmpty() && !mea2op.get(mea.mde().nameInTable()).contains(op.get().mde())) {
              c.annotate(ErrorType.MDMV, mea2op.get(mea.mde().nameInTable()));
            }
          } else {
            // TODO do nothing, count can only be applied to fact (for now)
          }
        }
        break;
      case GC: // TODO MDGV is not implemented
        break;
      case SC:
        if (c.children.size() >= 2) { // the clause contains both a level and a value
          final Ngram lev = c.children.stream().filter(cc -> cc.type.equals(Type.ATTR)).findAny().get();
          final Ngram val = c.children.stream().filter(cc -> cc.type.equals(Type.VAL)).findAny().get();
          if (!lev.typeInDB().equals(val.typeInDB()) // if the types differ or the member is not in the domain of the level
              || !lev.typeInDB().equals(DataType.NUMERIC) && val.mde().refToOtherTable() != lev.mde().pkInTable()) {
            c.annotate(ErrorType.AVM, attr2val.get(lev.mde().nameInTable()));
          }
        }
        break;
      default: 
        throw new IllegalArgumentException("Type checking cannot be applied to " + c);
      }
    }
    return ngram;
  }

  public static void infer(final Mapping m) {
    for (Ngram n : m.ngrams) {
      infer(n, operatorOfMeasure, membersofLevels, levelsOfMembers, yearLevels);
    }
  }

  /**
   * Infer missing information and add it to the parsing tree.
   * @param ngram parse tree
   * @param mea2op operator/measure constraints
   * @return the annotated and expanded parse tree
   */
  public static Ngram infer(final Ngram ngram, final Map<String, Set<Entity>> mea2op,
      final Map<String, Set<Entity>> attr2val,
      final Map<String, Set<Entity>> val2attr,
      final Set<Entity> dateAttributes) {
    final List<Ngram> clauses = Ngram.simpleClauses(ngram).stream().collect(Collectors.toList());
    for (int i = 0; i < clauses.size(); i++) {
      final Ngram c = clauses.get(i);
      switch (c.type) {
      case MC:
        if (c.children.size() == 1) { // the clause contains only a measure, add the operator
          final Ngram mea = c.children.stream().filter(cc -> cc.type.equals(Type.MEA)).findAny().get();
          final Set<Entity> appliableOperators = mea2op.get(mea.mde().nameInTable());
          if (appliableOperators.size() != 1) {
            c.annotate(ErrorType.MA, appliableOperators);
          } else {
            final Entity operator = appliableOperators.stream().findAny().get();
            final Ngram op = new Ngram(operator.nameInTable(), Type.OP, operator, mea.pos());
            c.setChildren(ImmutableList.of(op, mea));
          }
        }
        break;
      case GC: // do nothing
        break;
      case SC:
        if (c.children.size() == 2) { // the clause contains both a level and a value
          final Ngram lev = c.children.stream().filter(cc -> cc.type.equals(Type.ATTR)).findAny().get();
          final Ngram val = c.children.stream().filter(cc -> cc.type.equals(Type.VAL)).findAny().get();
          final Ngram eq = new Ngram("=", Type.COP, new Entity("="), lev.pos());
          c.setChildren(ImmutableList.of(lev, eq, val));
        } else if (c.children.size() == 1) { // the clause contains only a value
          // If you are here, you are either:
          // - a member NUMERIC/DATE member
          // - a member from a categorical attribute with the reference to the corresponding level
          final Ngram val = c.children.stream().filter(cc -> cc.type.equals(Type.VAL)).findAny().get();
          final Ngram eq = new Ngram("=", Type.COP, new Entity("="), val.pos());
          switch (val.typeInDB()) {
          case NUMERIC:
            final double value = Double.parseDouble(val.tokens);
            if (value >= 1900 && value <= 2155) { // you are a date (boundaries are compliant with year type in MySQL)
              final Entity levEntity = dateAttributes.stream().findAny().get(); // TODO an entity is picked randomly
              final Ngram lev = new Ngram(levEntity.nameInTable(), Type.ATTR, levEntity, val.pos());
              c.setChildren(ImmutableList.of(lev, eq, val));
            } else {
              throw new IllegalArgumentException("What should I do with dangling value " + val + " ?");
            }
            break;
          case DATE:
            throw new IllegalArgumentException("What should I do with dangling date " + val + " ?");
          case STRING:
            final Set<Entity> candidates = val2attr.get(val.mde().nameInTable());
            if (candidates != null && !candidates.isEmpty()) {
              if (candidates.size() > 1) {
                c.annotate(ErrorType.AA, candidates);
              } else {
                final Entity levEntity = candidates.stream().findAny().get();
                final Ngram lev = new Ngram(levEntity.nameInTable(), Type.ATTR, levEntity, val.pos());
                c.setChildren(ImmutableList.of(lev, eq, val));
              }
            }
            break;
          default:
            throw new NotImplementedException("This case is not handled: " + val);
          }
        }
        break;
      default:
        throw new IllegalArgumentException("Inference cannot be applied to " + c);
      }
    }
    return ngram;
  }

  /**
   * Parse a mapping.
   * @param rules list of rules to apply in the parsing
   * @param mapping mapping to be translated
   * @return parsing interpretations sorted by number of matched entities
   */
  public static Set<Mapping> parse(final List<Rule[]> rules, final Mapping mapping) {
    // stack of translated sentences
    final Set<Mapping> t = Sets.newLinkedHashSet();
    // init the sentence search space
    final Stack<Mapping> s = new Stack<Mapping>();
    s.push(mapping);
    while (!s.isEmpty()) {
      boolean isChanged = false;
      // pick a sentence
      final List<Ngram> sentence = s.pop().ngrams;
      // foreach rule...
      for (final Rule[] rul : rules) {
        for (int i = 0; i < sentence.size(); i++) {
          for (final Rule r : rul) {
            final int size = r.elements.length;
            if (i + size > sentence.size()) {
              continue;
            }
            final List<Ngram> subsentence = sentence.subList(i, i + size);
            if (r.match(subsentence)) { // check if the ngrams match the rule type
              final List<Ngram> tmp = Lists.newArrayList(sentence);
              tmp.add(i, new Ngram(r.ret, subsentence)); // add the generated (new) ngram
              tmp.removeAll(subsentence); // and remove the previous ngrams
              s.push(new Mapping(tmp)); // add the sentence to the search space
              isChanged = true;
              i += size - 1;
              break;
            }
          }
        }
      }
      if (!isChanged) { // if the sentence is unchanged (no transformation can be applied)...
        t.add(new Mapping(sentence)); // it can be added to the translated sentences
      }
    }
    return t;
  }

  private static String getMeasureAsString(final List<Ngram> ngrams, final String par1, final String par2) {
    String res = "";
    String op = null;
    String meas = null;
    for (final Ngram n : ngrams) {
      if (n.children.isEmpty()) {
        if (n.type.equals(Type.OP)) {
          op = n.mde().nameInTable();
        } else {
          meas = n.type.equals(Type.FACT) ? par1 + "*" + par2 : par1 + n.mde().nameInTable() + par2;
        }
      } else {
        if (!res.equals("")) {
          res += ", ";
        }
        res += sqlMeas(n.children);
      }
    }
    if (op != null && meas != null) {
      res += op + meas;
    } else if (op == null) {
      res += "sum" + par1 + meas + par2;
    }
    return res;
  }

  private static String recoursiveMeasure(final List<Ngram> ngrams) {
    return getMeasureAsString(ngrams, " ", "");
  }

  private static String sqlMeas(final List<Ngram> ngrams) {
    return getMeasureAsString(ngrams, "(", ")");
  }

  private static Pair<String, List<Ngram>> getWhere(final List<Ngram> scset) {
    String res = "\nWHERE ";
    List<Ngram> newatt = new ArrayList<>();
    String attr = null;
    String bin = null;
    String val1 = null;
    String not = null;
    for (Ngram n : scset) {
      if (n.type.equals(Type.AND) || n.type.equals(Type.OR)) {
        res += " " + n.mde().nameInTable() + " ";
      }
      if (n.type.equals(Type.NOT)) {
        not = n.mde().nameInTable();
      }
      if (n.type.equals(Type.ATTR)) {
        attr = n.mde().nameInTable();
      }
      if (n.type.equals(Type.BIN)) {
        bin = n.mde().nameInTable();
      }
      if (n.type.equals(Type.VAL)) {
        if (bin != null) {
          // ho l'operatore binario
          if (bin.equals("between")) {
            if (val1 == null) {
              val1 = n.mde().nameInTable();
            } else {
              res += attr + " " + bin + " " + val1 + " and " + n.mde().nameInTable();
              attr = null;
              not = null;
              val1 = null;
              bin = null;
            }
          } else {
            if (attr == null && n.mde().refToOtherTable() != -1) {
              // cerca il livello a cui appartiene il membro
              Ngram l = QueryGeneratorChecker.getLevelOfMember(n);
              attr = l.tokens;
              newatt.add(l);
            }
            if (attr != null) {
              res += attr + " " + bin + " " + n.mde().nameInTable();
              attr = null;
              not = null;
              val1 = null;
              bin = null;
            }
          }
        } else if (not != null) {
          // Non ho l'operatore bianrio ma ho il not
          if (attr == null && n.mde().refToOtherTable() != -1) {
            // cerca il livello a cui appartiene il membro
            Ngram l = QueryGeneratorChecker.getLevelOfMember(n);
            attr = l.tokens;
            newatt.add(l);
          }
          if (attr != null) {
            res += not + " " + attr + " = " + n.mde().nameInTable();
            attr = null;
            not = null;
            val1 = null;
            bin = null;
          }
        } else {
          // Non ho né l'operatore bianario né il not => considero implicito =
          if (attr == null && n.mde().refToOtherTable() != -1) {
            // cerca il livello a cui appartiene il membro
            Ngram l = QueryGeneratorChecker.getLevelOfMember(n);
            attr = l.tokens;
            newatt.add(l);
          }
          if (attr != null) {
            res += attr + " = " + n.mde().nameInTable();
            attr = null;
            not = null;
            val1 = null;
            bin = null;
          }
        }
      }
    }
    return Pair.of(res, newatt);
  }

  private static String createQuery(final List<Ngram> ngrams) throws Exception {
    Set<Ngram> attributes = Sets.newLinkedHashSet();
    String select = "";
    String from = "";
    String where = "";
    String groupby = "";

    for (final Ngram ngs : ngrams) {
      if (ngs.type.equals(Type.GC)) {
        Set<Ngram> ga = Ngram.leaves(ngs).stream().map(n -> (Ngram) n).filter(n -> !n.type.equals(Type.GB)).collect(Collectors.toSet());
        groupby = "\nGROUP BY " + ga.stream().map(n -> ((Ngram) n).mde().nameInTable()).reduce((s1, s2) -> s1 + ", " + s2).orElse("");
        attributes.addAll(ga);
        // gbset = s.flatten(ngs.children.stream()).map(n -> (Ngram) n).filter(n -> !n.type.equals(Type.GB)).collect(Collectors.toList());
      } else if (ngs.type.equals(Type.SC)) {
        List<Ngram> scset = Ngram.leaves(ngs).stream().map(n -> (Ngram) n).collect(Collectors.toList());
        // List<Ngram> scset = fillSCset(ngs, checker);
        Pair<String, List<Ngram>> sa = getWhere(scset);
        where = sa.getLeft();
        attributes.addAll(scset.stream().filter(n -> n.type.equals(Type.ATTR)).collect(Collectors.toSet()));
        attributes.addAll(sa.getRight());
      } else if (ngs.type.equals(Type.MC)) {
        select = "SELECT " + sqlMeas(ngs.children);
      }
    }

    Map<Integer, String> tabIns = Maps.newLinkedHashMap();
    if (!select.equals("")) {
      Pair<Integer, String> ftdet = QueryGeneratorChecker.getFactTable();
      from = "\nFROM " + ftdet.getRight() + " FT ";
      int nt = 1;
      for (Ngram gba : attributes) {
        int idT = gba.mde().refToOtherTable();
        String alias;
        if (!tabIns.containsKey(idT)) {
          Pair<String, String> detTab = QueryGeneratorChecker.getTabDetails(ftdet.getLeft(), idT);
          alias = "t" + nt++;
          from += "\nINNER JOIN " + detTab.getLeft() + " " + alias + " ON " + alias + "." + detTab.getRight() + " = FT." + detTab.getRight();
          tabIns.put(idT, alias);
        }
      }
    }

    String query = where + groupby;
    for (Ngram a : attributes) {
      String alias = tabIns.get(a.mde().refToOtherTable());
      query = query.replace(a.mde().nameInTable(), alias + "." + a.mde().nameInTable());
    }

    return select + from + query;
  }

  /**
   * Get the SQL version of the query.
   * @param s mapping
   * @return SQL version of the query
   * @throws Exception in case of error
   */
  public static String getSQLQuery(final Mapping s) throws Exception {
    for (Ngram ngs : s.ngrams) {
      if (ngs.type.equals(Type.Q)) {
        return createQuery(ngs.children);
      }
    }
    return "IT IS NOT POSSIBLE TO CLOSE THE QUERY";
  }

  /**
   * Get a string that separate SC, GBC and MC out of a list of ngrams.
   * @param ngrams list of ngrams
   * @return string in the format MC\GBC\SC
   */
  public static String getPartOfResult(final List<Ngram> ngrams) {
    String select = "";
    String gb = "";
    String where = "";
    for (Ngram nch : ngrams) {
      if (nch.type == Type.Q) {
        return getPartOfResult(nch.children);
      } else {
        if (nch.type == Type.SC) {
          where = Ngram.leaves(nch).stream().map(n -> ((Ngram) n).mde().nameInTable()).reduce((s1, s2) -> s1 + " " + s2).get();
        } else if (nch.type == Type.GC) {
          gb = Ngram.leaves(nch).stream().filter(n -> ((Ngram) n).type != Type.GB).map(n -> ((Ngram) n).mde().nameInTable()).reduce((s1, s2) -> s1 + ", " + s2).get();
        } else if (nch.type == Type.MC) {
          select = recoursiveMeasure(nch.children);
        }
      }
    }
    return select + "\\" + gb + "\\" + where;
  }
}
