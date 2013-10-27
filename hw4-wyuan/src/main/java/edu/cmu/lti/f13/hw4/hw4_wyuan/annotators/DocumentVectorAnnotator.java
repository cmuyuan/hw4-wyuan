package edu.cmu.lti.f13.hw4.hw4_wyuan.annotators;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.cas.IntegerArray;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.uimafit.util.FSCollectionFactory;

import edu.cmu.lti.f13.hw4.hw4_wyuan.typesystems.Document;
import edu.cmu.lti.f13.hw4.hw4_wyuan.typesystems.Token;

public class DocumentVectorAnnotator extends JCasAnnotator_ImplBase {

  ArrayList<String> stopwordList;
  private boolean isStopword(String word){
    return stopwordList.contains(word);
  }
  /**load stop words to arrayList
   * */
  public void initialize(UimaContext aContext) throws ResourceInitializationException{
    // TODO Auto-generated method stub  
    super.initialize(aContext);
    File stopwordFile = new File("src/main/resources/stopwords.txt");
    Scanner stopwordFileScanner;
    stopwordList = new ArrayList<String>();    
    try {
      stopwordFileScanner = new Scanner(stopwordFile);
      if(stopwordFile.exists()){
        while(stopwordFileScanner.hasNext()){
          String s = stopwordFileScanner.nextLine();
          if(s.contains("#set"))
            continue;
          stopwordList.add(s);
        }
      }
    } catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
  
  @Override
  public void process(JCas jcas) throws AnalysisEngineProcessException {

    FSIterator<Annotation> iter = jcas.getAnnotationIndex().iterator();
    if (iter.isValid()) {
      iter.moveToNext();
      Document doc = (Document) iter.get();
      createTermFreqVector(jcas, doc);
    }

  }

  /**
   * update the tokenList of Document and update the CAS. DocumentVectorAnnotator analysis engine
   * uses the CAS provided by DocumentReader. It need to extract the bag of word feature vectors
   * from the text sentences. Specifically, the term and term frequency are filled with the word and
   * word occurrence for that specific sentence.
   * 
   * @param jcas
   * @param doc
   */

  private void createTermFreqVector(JCas jcas, Document doc) {

     String docText = doc.getText();

    // TO DO: construct a vector of tokens and update the tokenList in CAS

     String[] terms = docText.split(" ");
     HashMap<String, Token> termTokenMap = new HashMap<String, Token>();
     
     for(String term: terms){
       term = term.toLowerCase();
       term = term.replaceAll(",", "");
       term = term.replaceAll(";", "");
       term = term.replaceAll("\\.", "");
       if(isStopword(term))
         continue;
       if(termTokenMap.containsKey(term)){
         Token t = termTokenMap.get(term);
         t.setFrequency(t.getFrequency()+1);
         termTokenMap.put(term, t);
       }else{
         Token t = new Token(jcas);
         t.setText(term);
         t.setFrequency(1);
         termTokenMap.put(term, t);
       }       
     }
     FSList fSList = FSCollectionFactory.createFSList(jcas, termTokenMap.values());
     doc.setTokenList(fSList);
  }

}
