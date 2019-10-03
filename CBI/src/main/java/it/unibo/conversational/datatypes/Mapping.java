package it.unibo.conversational.datatypes;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import it.unibo.conversational.Utils;
import it.unibo.conversational.algorithms.Mapper;
import it.unibo.conversational.algorithms.Parser.Type;

/** A Mapping is a forest of ngrams. */
public final class Mapping {
  /** List of ngrams in a mapping. */
  public final ImmutableList<Ngram> ngrams;
  private final Ngram bestNgram;
//  private final List<Ngram> tokens;

  /**
   * Return the ngram with the highest number of children as a string tree parsable by zhsh.
   * @return string tree parsable by zhsh.
   */
  public String toStringTree() {
    return ngrams.stream().max((Ngram n1, Ngram n2) -> Integer.compare(n1.countNode(), n2.countNode())).get().toStringTree();
  }

  /**
   * Create a mapping.
   * @param ngrams a list of ngrams
   */
  public Mapping(final List<Ngram> ngrams) {
    this.ngrams = ImmutableList.copyOf(ngrams);
    this.bestNgram = ngrams.stream().max((n1, n2) -> Integer.compare(n1.countNode(), n2.countNode())).get();
  }

  /**
   * Count the number of nodes in a sentence.
   * @return number of nested node (plus self)
   */
  public int countNode() {
    return ngrams.stream().mapToInt(n -> n.countNode()).max().getAsInt();
  }

  /**
   * Create a sentence as a list of ngrams.
   * @param ngrams list of ngrams
   */
  public Mapping(final Ngram... ngrams) {
    this(Arrays.asList(ngrams));
  }

  /**
   * @return the number of matched ngrams in a sentence
   */
  public int getNMatched() {
    return (int) Ngram.leaves(bestNgram).size();
  }

  /**
   * @return the average similarity of the ngrams contained in the parsing tree (i.e., the ngram with the highest number of leaves).
   */
  public double getAvgSimilarity() {
    return bestNgram.similarity();
  }

  /**
   * This method is used to compare the mappings.
   * @return sum of ngrams similarities for the ngrams contained in the parsing tree.
   */
  public double getScore() {
    return Ngram.leaves(bestNgram).stream().mapToDouble(n -> n.similarity()).sum();
  }

  /**
   * This method is used in the pruning phase.
   * @return sum of ngrams similarities.
   */
  public double getPotentialScore() {
    return ngrams.stream().mapToDouble(n -> n.similarity()).sum();
  }

  @Override
  public String toString() {
    return ngrams.toString();
  }

  @Override
  public int hashCode() {
    return Objects.hash(ngrams);
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj instanceof Mapping) {
      return this.ngrams.equals(((Mapping) obj).ngrams);
    }
    return false;
  }

  /**
   * Create a CSV representing the mapping.
   * @param m translated sentence
   * @param nlp natural language query
   * @return csv string
   * @throws Exception in case of error
   */
  public static String toCsv(final Mapping m, final String nlp) throws Exception {
    final List<String> tokens = Mapper.cleanSentence(nlp, false);
    final List<Ngram> s = fillSentence(m, String.join(" ", tokens));
    final Ngram r = s.size() == 1 ? s.get(0) : new Ngram(Type.FOO, s);
    final Set<String> ret = Sets.newLinkedHashSet();
    final Map<Type, Integer> counter = Maps.newLinkedHashMap();
    recursiveFlatten(ret, Lists.newLinkedList(), r, counter);
    return ret.stream().reduce((c1, c2) -> c1 + "\n" + c2).get();
  }

  private static void recursiveFlatten(final Set<String> res, final List<String> names, final Ngram n, final Map<Type, Integer> counter) {
    final List<String> newNames = Lists.newArrayList(names);
    if (n.children.isEmpty()) {
      newNames.add(n.type.toString() + ":" + n.mde().nameInTable() + " [(" + n.pos().getLeft() + ";" + n.pos().getRight() + ");" + Utils.DF.format(n.similarity()) + "]");
      res.add(newNames.stream().reduce((c1, c2) -> c1 + "\\" + c2).get() + "," + 1);
      newNames.add(n.tokens);
      res.add(newNames.stream().reduce((c1, c2) -> c1 + "\\" + c2).get() + "," + 1);
    } else {
      final int id = counter.compute(n.type, (k, v) -> counter.getOrDefault(k, 0) + 1);
      newNames.add("" + n.type + id + (n.type.equals(Type.Q) ? "[" + Ngram.leaves(n).size() + "; " + Utils.DF.format(n.similarity()) + "]" : ""));
      res.add(newNames.stream().reduce((c1, c2) -> c1 + "\\" + c2).get() + "," + 1);
      n.children.stream().forEach((Ngram c) -> {
        res.add(newNames.stream().reduce((c1, c2) -> c1 + "\\" + c2).get() + "," + 1);
        recursiveFlatten(res, newNames, c, counter);
      });
    }
  }

  /**
   * Add tokens to bin if they are not used.
   * @param s mapping
   * @param nlp nl sentence 
   * @return extend the mappings with bin
   */
  private static List<Ngram> fillSentence(final Mapping s, final String nlp) {
    final List<Ngram> ngrams = Lists.newArrayList(s.ngrams);
    final AtomicInteger atomicInteger = new AtomicInteger(0);
    final Map<Integer, String> tokens = Maps.newLinkedHashMap();
    Arrays.asList(nlp.split(" ")).stream().forEach(t -> tokens.put(atomicInteger.getAndIncrement(), t));
    ngrams.stream()
      .flatMap(n -> Ngram.leaves(n).stream())
      .map(t -> ((Ngram) t).pos())//
      .forEach(p -> {
        for (int i = p.getLeft(); i <= p.getRight(); i++) {
          tokens.remove(i);
        }
      });
    final List<Ngram> bin = 
        tokens.entrySet().stream()
          .map(e -> new Ngram(e.getValue(), Type.BIN, new Entity(e.getValue()), Pair.of(e.getKey(), e.getKey())))
          .sorted(Ngram::compareNgrams)
          .collect(Collectors.toList());
    ngrams.addAll(bin);
    return ngrams;
  }

  /**
   * Compare mappings by number of matched entities and average similarity (reversed).
   * @param s1 a mapping
   * @param s2 another mapping
   * @return mapping comparison
   */
  public static int compareMappings(final Mapping s1, final Mapping s2) {
    final int c = Double.compare(s1.getScore(), s2.getScore());
    return c == 0 ? s1.toString().compareTo(s2.toString()) : c;
  }
}
