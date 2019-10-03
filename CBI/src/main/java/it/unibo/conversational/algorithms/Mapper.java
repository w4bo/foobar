package it.unibo.conversational.algorithms;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.JDBCType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import edu.stanford.nlp.util.StringUtils;
import it.unibo.conversational.Utils;
import it.unibo.conversational.Utils.PredicateType;
import it.unibo.conversational.algorithms.Parser.Type;
import it.unibo.conversational.database.DBmanager;
import it.unibo.conversational.database.DBsynonyms;
import it.unibo.conversational.database.QueryGeneratorChecker;
import it.unibo.conversational.datatypes.Entity;
import it.unibo.conversational.datatypes.Mapping;
import it.unibo.conversational.datatypes.Ngram;

/** Prepare the mappings for the parsing. */
public final class Mapper {
  private Mapper() {
  }

  private static final Logger L = LoggerFactory.getLogger(Mapper.class);

  /**
   * Read the list of stopwords from file.
   * @return the list of stopwords
   * @throws Exception in case of error
   */
  private static Set<String> readStopWord() throws Exception {
    final Set<String> stopWord = Sets.newLinkedHashSet();
    final BufferedReader br = new BufferedReader(new InputStreamReader(new Mapper().getClass().getClassLoader().getResourceAsStream("stopwords.txt")));
    String st;
    while ((st = br.readLine()) != null) {
      stopWord.add(st);
    }
    return stopWord;
  }

  /**
   * Return a clean set of tokens.
   * @param nl natural language sentence
   * @return a clean set of tokens
   * @throws Exception in case of error
   */
  public static List<String> cleanSentence(final String nl, final boolean skipCleaning) throws Exception {
    final Set<String> stopWord = readStopWord();
    final List<String> tokens = 
        Arrays.asList(nl.replace("_", " ").split(" ")).stream()
          .filter(t -> !t.isEmpty())
          .filter(t -> skipCleaning || !stopWord.contains(t))
          .collect(Collectors.toList());
    return tokens;
  }

  private static Type getNgramType(final Entity m) {
    Type t = null;
    if (m.table().equals(DBmanager.tabMEMBER) || m.pkInTable() == -1) {
      t = Type.VAL;
    } else if (m.table().equals(DBmanager.tabMEASURE)) {
      t = Type.MEA;
    } else if (m.table().equals(DBmanager.tabLEVEL)) {
      t = Type.ATTR;
    } else if (m.table().equals(DBmanager.tabHIERARCHY)) {
      t = Type.H;
    } else if (m.table().equals(DBmanager.tabFACT)) {
      t = Type.FACT;
    } else if (m.table().equals(DBmanager.tabLANGUAGEPREDICATE)) {
      PredicateType pType = PredicateType.valueOf(QueryGeneratorChecker.getPredicate(m.pkInTable()).getLeft());
      switch (pType) {
      case COUNTOPERATOR:
        t = Type.COUNT;
        break;
      case SELECT:
        t = Type.SELECT;
        break;
      case GROUPBYOPERATOR:
        t = Type.OP;
        break;
      case GROUPBYTERM:
        t = Type.GB;
        break;
      case SELECTIONTERM:
        t = Type.WHR;
        break;
      case PREDICATE:
        if (QueryGeneratorChecker.getPredicate(m.pkInTable()).getRight().equals("between")) {
          t = Type.BETWEEN;
        } else {
          t = Type.COP;
        }
        break;
      case BOOLEANOPEAROR:
        String term = QueryGeneratorChecker.getPredicate(m.pkInTable()).getValue();
        switch (term.toLowerCase()) {
        case "and":
          t = Type.AND;
          break;
        case "not":
          t = Type.NOT;
          break;
        case "or":
          t = Type.OR;
          break;
        default:
          throw new IllegalArgumentException("Unknown boolean operator: " + m);
        }
        break;
      default:
        throw new IllegalArgumentException("Unknown language predicate: " + m);
      }
    } else {
      throw new IllegalArgumentException("Unknown type: " + m);
    }
    return t;
  }

  /**
   * Get all the possibile interpretations out of a list of ngrams.
   * @param data list of remaining ngrams
   * @param totalLenght total length of the sentence
   * @param threshold coverage threshold
   * @param maxDist maximum distance between two ngrams
   * @return interpretations
   */
  public static List<Mapping> createMappings(final List<Ngram> data, final int totalLenght, final double threshold, final int maxDist) {
    final List<Mapping> res = Lists.newArrayList();
    createMappings(Lists.newArrayList(), 0, data, totalLenght, threshold, maxDist, res);
    return res;
  }

  /**
   * Get all the possibile interpretations out of a list of ngrams.
   * @param acc accumulator
   * @param coveredTokens sentence coverage so far
   * @param data list of remaining ngrams
   * @param totalLenght total length of the sentence
   * @param threshold coverage threshold
   * @param maxDist maximum distance between two ngrams
   * @return interpretations
   * @return
   */
  public static List<Mapping> createMappings(final List<Ngram> acc, final int coveredTokens, final List<Ngram> data, final int totalLenght, final double threshold, final int maxDist) {
    final List<Mapping> res = Lists.newArrayList();
    createMappings(acc, coveredTokens, data, totalLenght, threshold, maxDist, res);
    return res;
  }

  private static List<Mapping> createMappings(final List<Ngram> acc, final int coveredTokens, final List<Ngram> data, final int totalLenght, final double threshold, final int maxDist, final List<Mapping> res) {
    final List<Ngram> newData = new ArrayList<>(data);
    for (final Ngram r : data) {
      final int diff = r.pos().getLeft() - (acc.isEmpty() ? 0 : acc.get(acc.size() - 1).pos().getRight()); // distance between begin and end of the two consecutive ngrams
      final int newCoveredTokens = coveredTokens + r.pos().getRight() - r.pos().getLeft() + 1; // if the words begins in 0 end ends in 1, the length is 2
      // If the two ngrams are closer than maxDist and the amount of covered sentence is above threshold
      if (acc.isEmpty() || diff >= 1 && diff <= maxDist) {
        final List<Ngram> newInt = new ArrayList<>(acc);
        newInt.add(r);
        newData.remove(r);
        if (newCoveredTokens >= threshold) {
          res.add(new Mapping(newInt));
        }
        if (!newData.isEmpty() && newCoveredTokens + totalLenght - r.pos().getRight() >= threshold) {
          createMappings(newInt, newCoveredTokens, newData, totalLenght, threshold, maxDist, res);
        }
      }
    }
    return res;
  }

  /**
   * @param nlQuery NL sentence
   * @param thrSimilarityMember soglia per la simialrità tra i membri
   * @param thrSimilarityMetadata soglia per la similarità tra i metadati
   * @param synMember numero massimo di sinonimi che voglio prendere
   * @param synMeta numero massimo di sinonimi che voglio prendere
   * @param percPhrase la percentuale di frase massima che posso non considerare nella mia interpretazione
   * @param maxDist la distanza massima tra due ngrammi in termini di posizione delle parole nella frase
   * @param skipCleaning 
   * @return list of interpreted mappings
   * @throws Exception in case of error
   */
  public static List<Mapping> createMappings(final String nlQuery, final double thrSimilarityMember, final double thrSimilarityMetadata, final int synMember, final int synMeta, final double percPhrase, final int maxDist, final int ngramSize, final double nGramSimThr, final Map<String, Object> stats, boolean skipCleaning) throws Exception {
    Long startTime = System.currentTimeMillis();

    // lemmatizzazione e tagging della frase usando coreNLP
    // final List<String> interestingNER = Arrays.asList("NUMERIC", "DATE"); // Mi interessano solo i token taggati come date e numeri
    // final List<String> wordsLemma = new ArrayList<String>();
    // final Map<String, String> wordsNer = new HashMap<String, String>();
    // final Properties props = new Properties();
    // props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner");
    // final StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
    // final Annotation document = new Annotation(phrase);
    // pipeline.annotate(document);
    // // Salvo i lemmi delle parole NON stopwords e i token con un tag NER che mi interessa
    // final List<CoreMap> sentences = document.get(SentencesAnnotation.class);
    // for (final CoreMap sentence : sentences) {
    //   for (final CoreLabel token : sentence.get(TokensAnnotation.class)) {
    //     final String word = token.get(TextAnnotation.class); // token com'è nella frase
    //     if (!stopWord.contains(word)) {
    //       wordsLemma.add(token.get(LemmaAnnotation.class)); // lemma
    //     }
    //     if (token.get(NamedEntityTagAnnotation.class) != null && interestingNER.contains(token.get(NamedEntityTagAnnotation.class))) {
    //       wordsNer.put(word, token.get(NamedEntityTagAnnotation.class));
    //     }
    //   } 
    // }

    final List<String> tokens = cleanSentence(nlQuery, skipCleaning);
    final Map<String, String> wordsNer = Maps.newLinkedHashMap();
    tokens.forEach(t -> {
      try {
        Double.parseDouble(t.replace(",", ".")); // TODO what to do if v is a year??? DATE AND YEAR???
        wordsNer.put(t, JDBCType.NUMERIC.toString());
      } catch (final Exception e) {
      }
    });
    stats.put("lemmatization_time", System.currentTimeMillis() - startTime);
    stats.put("lemmatization_sentence", StringUtils.join(tokens));
    L.debug("--- lemmatized sentence: " + StringUtils.join(tokens));

    // Cerco i match tra i token e i termini nel DB
    startTime = System.currentTimeMillis();
    final List<Ngram> validMatch = Lists.newArrayList();
    for (int i = 0; i < tokens.size(); i++) {
      for (int j = ngramSize; j >= 1; j--) { // Creo tutti gli ngrammi possibili da ngramSize a 1
        int idxend = i + j;
        if (idxend <= tokens.size()) {
          final List<String> ngrams = tokens.subList(i, idxend);
          if (wordsNer.containsKey(StringUtils.join(ngrams))) { // Se l'ngramma è taggato lo aggiungo ai match
            final Ngram n = new Ngram(StringUtils.join(ngrams), Type.VAL, new Entity(StringUtils.join(ngrams), Utils.getDataType(wordsNer.get(StringUtils.join(ngrams)))), Pair.of(i, (i + j - 1)));
            validMatch.add(n);
          } else { // Altrimenti cerco i migliori sinonimi tra i membri e i migliori tra i metadati
            final List<Triple<Entity, Double, String>> syns = DBsynonyms.getEntities(ngrams, thrSimilarityMember, thrSimilarityMetadata, synMember, synMeta);
            for (final Triple<Entity, Double, String> dbm : syns) {
              final Ngram ngram = new Ngram(StringUtils.join(ngrams), getNgramType(dbm.getLeft()), dbm.getLeft(), dbm.getMiddle(), dbm.getRight(), Pair.of(i, (i + j - 1)));
              validMatch.add(ngram);
            }
          }
        }
      }
    }
    stats.put("match_count", validMatch.size());
    final List<Ngram> confidentMatch = validMatch.stream().filter(n -> n.similarity() >= nGramSimThr).collect(Collectors.toList());
    confidentMatch.stream() //
        .forEach(m -> { //
          final Set<Integer> range = IntStream.rangeClosed(m.pos().getLeft(), m.pos().getRight()).boxed().collect(Collectors.toSet());
          final List<Ngram> toRemove = validMatch //
              .stream() // get the non-confident ngrams that intersect with the current ngram
              .filter(n -> {
                final Set<Integer> currange = IntStream.rangeClosed(n.pos().getLeft(), n.pos().getRight()).boxed().collect(Collectors.toSet());
                return !m.equals(n) // do not remove myself
                        && (currange.size() < range.size() || currange.size() == range.size() && n.similarity() < nGramSimThr)
                        && range.containsAll(currange);
              })
              .collect(Collectors.toList());
          validMatch.removeAll(toRemove);
        });
    validMatch.sort(Ngram::compareNgrams);
    stats.put("match_time", System.currentTimeMillis() - startTime);
    stats.put("match_confident_count", validMatch.size());
    L.debug("--- n matches: " + validMatch.size());
    Utils.writeMappings("result_interpretation", "M_" + nlQuery + "_" + thrSimilarityMember + "_" + thrSimilarityMetadata + "_" + synMember + "_" + synMeta + "_" + percPhrase + "_" + maxDist + "_" + ngramSize, validMatch);

    // create all possible interpretations from ngrams
    startTime = System.currentTimeMillis();
    final double threshold = tokens.size() * percPhrase;
    final List<Mapping> interpretationsSentence = createMappings(new ArrayList<>(validMatch), tokens.size(), threshold, maxDist);
    stats.put("sentence_time", System.currentTimeMillis() - startTime);
    stats.put("sentence_count", interpretationsSentence.size());
    L.warn("--- n sentences: " + interpretationsSentence.size());
    Utils.writeParsing("result_interpretation", "I_" + nlQuery + "_" + thrSimilarityMember + "_" + thrSimilarityMetadata + "_" + synMember + "_" + synMeta + "_" + percPhrase + "_" + maxDist + "_" + ngramSize, interpretationsSentence);
    return interpretationsSentence;
  }
}
