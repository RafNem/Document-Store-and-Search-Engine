package edu.yu.cs.com1320.project.stage5.impl;
import edu.yu.cs.com1320.project.stage5.Document;
import java.net.URI;
import java.util.*;

public class DocumentImpl implements Document {
    private String documentTxt;
    private byte[] documentBinaryData;
    private URI uriKey;
    private HashMap<String, Integer> wordCount;
    private HashSet<String> setOfWords;
    private Long timeUsed;
    private Map<String, Integer> wordMap;

    //should throw anIllegalArgumentException if either argument is null or empty/blank:
//    public DocumentImpl(URI uri, String text, Map<String, Integer> wordMap)
    public DocumentImpl(URI uri, String txt){
        if(uri == null || uri.toString().isBlank() || txt == null || txt.isBlank()){
            throw new IllegalArgumentException();
        }
        this.uriKey = uri;
        this.documentTxt = txt;
        this.wordCount = new HashMap<>();
        mapWordCount();
    }
    //should throw an IllegalArgumentException if either argument is null or empty/blank:
    public DocumentImpl(URI uri, byte[] binaryData){
        if(uri == null || uri.toString().isBlank() || binaryData == null || binaryData.length == 0){
            throw new IllegalArgumentException();
        }
        this.uriKey = uri;
        this.documentBinaryData = binaryData;
        this.wordCount = new HashMap<>();
    }

    @Override
    public String getDocumentTxt() {
        return this.documentTxt;
    }

    @Override
    public byte[] getDocumentBinaryData() {
        return this.documentBinaryData;
    }

    @Override
    public URI getKey() {
        return this.uriKey;
    }

    @Override
    public int wordCount(String word) {
        String tester = word.toUpperCase().replaceAll("[^A-Za-z0-9\\s]", "");
        return this.wordCount.getOrDefault(tester,0);
    }

    @Override
    public Set<String> getWords() {
        this.setOfWords = new HashSet<>(this.wordCount.keySet());
        return this.setOfWords;
    }

    @Override
    public long getLastUseTime() {
        return this.timeUsed;
    }

    @Override
    public void setLastUseTime(long timeInNanoseconds) {
        this.timeUsed = timeInNanoseconds;
    }

    @Override
    public Map<String, Integer> getWordMap() {
        return this.wordMap;
    }


    @Override
    public void setWordMap(Map<String, Integer> wordMap) {
        this.wordMap = new HashMap<>(wordMap);

    }

    // hashcode formula for our program
    @Override
    public int hashCode() {
        int result = uriKey.hashCode();
        result = 31 * result + (documentTxt != null ? documentTxt.hashCode() : 0);
        result = 31 * result + Arrays.hashCode(documentBinaryData);
        return result;
    }
    // Two documents are considered equal if they have the same hashCode.
    @Override
    public boolean equals(Object obj){
        return(this.hashCode() == obj.hashCode());
    }

    private void mapWordCount(){
        String text = this.documentTxt.toUpperCase().replaceAll("[^a-zA-Z0-9\\s]", "");
        for (String words : text.split("\\s+")) {
            if (!(words.isBlank())) {
                this.wordCount.put(words, this.wordCount.getOrDefault(words,0) + 1);
            }
        }
    }

    @Override
    public int compareTo(Document o) {
        if(this.getLastUseTime() > o.getLastUseTime()){
            return 1;
        }else if(this.getLastUseTime() < o.getLastUseTime()){
            return -1;
        }
        return 0;
    }
}
