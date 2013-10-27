package edu.cmu.lti.f13.hw4.hw4_wyuan.casconsumers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.util.ProcessTrace;

import edu.cmu.lti.f13.hw4.hw4_wyuan.typesystems.Document;
import edu.cmu.lti.f13.hw4.hw4_wyuan.typesystems.Token;
import edu.cmu.lti.f13.hw4.hw4_wyuan.utils.Utils;

public class RetrievalEvaluator extends CasConsumer_ImplBase {

//  /** query id number **/
//  public ArrayList<Integer> qIdList;
//
//  /** query and text relevant values **/
//  public ArrayList<Integer> relList;
  
  /** (queryAnswers,qid) list*/
  public LinkedList<queryAnswers> QAList;
  
  private class queryAnswers{
    int qid;
    
    /**(term,freq) pairs of the query*/
    HashMap<String, Integer> queryTermFreqMap = new HashMap<String, Integer>();
    
    /**each HashMap stores (term,freq) pairs of an answer*/
    LinkedList<HashMap<String, Integer>> answersTermfreqMapList = new LinkedList<HashMap<String, Integer>>();
    
    LinkedList<Integer> AnswerRelList = new LinkedList<Integer>();
    
    /**cosine similarity of answers*/
    LinkedList<Double> answersScoreList = new LinkedList<Double>();
    
    /**the rank of the only correct answer among all 3 answers*/
    int correctAnswerRank;    
    
    queryAnswers(int qid){
      this.qid = qid;
    }
  }
  
  public void initialize() throws ResourceInitializationException {
//    qIdList = new ArrayList<Integer>();
//    relList = new ArrayList<Integer>();
    QAList  = new LinkedList<queryAnswers>();
  }

  /**
   * TODO :: 1. construct the global word dictionary 2. keep the word frequency for each sentence
   */
  @Override
  public void processCas(CAS aCas) throws ResourceProcessException {

    JCas jcas;
    try {
      jcas = aCas.getJCas();
    } catch (CASException e) {
      throw new ResourceProcessException(e);
    }

    FSIterator it = jcas.getAnnotationIndex(Document.type).iterator();

    if (it.hasNext()) {
      Document doc = (Document) it.next();

      // Make sure that your previous annotators have populated this in CAS
      FSList fsTokenList = doc.getTokenList();
      ArrayList<Token> tokenList=Utils.fromFSListToCollection(fsTokenList, Token.class);
      int qid = doc.getQueryID();
      int rel = doc.getRelevanceValue();
      
      for(queryAnswers qa: QAList){
        if(qa.qid == qid){//found in QAList
          addEntryToQAList(qa, rel, tokenList);
          return;
        }
      }
      
      /*if qid is not in QAList*/
      queryAnswers newqa = new queryAnswers(qid);
      addEntryToQAList(newqa, rel, tokenList);
      QAList.add(newqa);
      
//      qIdList.add(doc.getQueryID());
//      relList.add(doc.getRelevanceValue());
    }

  }
  
  /** add new queryAnswers object to QAList*/
  private void addEntryToQAList(queryAnswers qa, int rel, ArrayList<Token> tokenList){
    if(rel == 99){
      for(Token token: tokenList)
        qa.queryTermFreqMap.put(token.getText(), token.getFrequency());
    }
    else{
      HashMap<String, Integer> answerTermFreqPair = new HashMap<String, Integer>();
      for(Token token: tokenList)
        answerTermFreqPair.put(token.getText(), token.getFrequency());
      qa.answersTermfreqMapList.add(answerTermFreqPair);
      //qa.answersScoreList.add(0D);
      qa.AnswerRelList.add(rel);
    }
  }
  /**
   * TODO 1. Compute Cosine Similarity and rank the retrieved sentences 2. Compute the MRR metric
   */
  @Override
  public void collectionProcessComplete(ProcessTrace arg0) throws ResourceProcessException,
          IOException {

    super.collectionProcessComplete(arg0);
    
    // TODO :: compute the cosine similarity measure
    for(queryAnswers qa: QAList){
      HashMap<String, Integer> queryVector = qa.queryTermFreqMap;
      for(HashMap<String, Integer> docVector: qa.answersTermfreqMapList){
        double sim = computeCosineSimilarity(queryVector, docVector);
        qa.answersScoreList.add(sim);
      }
      
    }
    
    // TODO :: compute the rank of retrieved sentences
    for(queryAnswers qa: QAList){
      int correctAnswerRank=1;
      
      /*find index of correct answer in AnswerLists*/
      int correctAnswerIndex=0;
      for(Integer rel: qa.AnswerRelList){
        if(rel != 1)
          correctAnswerIndex++;
        else
          break;
      }

      /*compute rank*/
      Double correctAnswerSim = qa.answersScoreList.get(correctAnswerIndex);
      for(int i=0; i<qa.answersScoreList.size(); i++){
        if(i != correctAnswerIndex){
          if(correctAnswerSim < qa.answersScoreList.get(i))
            correctAnswerRank++;
        }
      }
      
      /*save rank to qa.rankList*/
      qa.correctAnswerRank = correctAnswerRank;
      
      System.out.println("Score: "+correctAnswerSim+" rank="+correctAnswerRank+" rel=1 qid="+qa.qid+" sent"+(correctAnswerIndex+1));
    }    

    // TODO :: compute the metric:: mean reciprocal rank
    double metric_mrr = compute_mrr();
    System.out.println(" (MRR) Mean Reciprocal Rank ::" + metric_mrr);
  }

  /**
   * write code for finding cosine similarity between query sentence and each of the subsequent
   * document sentence. *
   * 
   * @return cosine_similarity
   */
  private double computeCosineSimilarity(HashMap<String, Integer> queryVector,
          HashMap<String, Integer> docVector) {
    double cosine_similarity = 0.0;
    double qaDist = 0D;
    double querySqrt = 0D;
    double answerSqrt = 0D;
    
    Iterator<Entry<String, Integer>> queryVectorIt = queryVector.entrySet().iterator();
    Iterator<Entry<String, Integer>> docVectorIt = docVector.entrySet().iterator();
    
    // TODO :: compute cosine similarity between two sentences    
    /* iterate over query terms*/
    while(queryVectorIt.hasNext()){
      Entry<String, Integer> entry = queryVectorIt.next();
      String qTerm = entry.getKey();
      int qFreq = entry.getValue();
      if(docVector.containsKey(qTerm)){
        int aFreq = docVector.get(qTerm);
        qaDist += (double)qFreq*aFreq;
      }
      querySqrt += (double)qFreq*qFreq;      
    }
    
    while(docVectorIt.hasNext()){
      Entry<String, Integer> entry =docVectorIt.next();
      int aFreq = entry.getValue();
      answerSqrt += (double)aFreq*aFreq;
    }
    
    querySqrt = Math.sqrt(querySqrt);
    answerSqrt = Math.sqrt(answerSqrt);    
    cosine_similarity = qaDist/(querySqrt*answerSqrt);
    
    return cosine_similarity;
  }

  /**
   * computes the Mean Reciprocal Rank (MRR) metric for the retrieval system
   * The MRR is averaged with respect to all sentences in the collection.
   * @return mrr
   */
  private double compute_mrr() {
    double metric_mrr = 0.0;

    // TODO :: compute Mean Reciprocal Rank (MRR) of the text collection
    for(queryAnswers qa:QAList){
      metric_mrr += 1.0/(double)qa.correctAnswerRank;
    }
    metric_mrr /= (double)QAList.size();
    
    return metric_mrr;
  }

}
