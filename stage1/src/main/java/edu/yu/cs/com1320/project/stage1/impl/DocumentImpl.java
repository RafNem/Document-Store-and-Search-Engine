package edu.yu.cs.com1320.project.stage1.impl;
import edu.yu.cs.com1320.project.stage1.Document;
import java.net.URI;
import java.util.Arrays;

public class DocumentImpl implements Document {
    private String documentTxt;
    private byte[] documentBinaryData;
    private URI uriKey;

    //should throw anIllegalArgumentException if either argument is null or empty/blank:
    public DocumentImpl(URI uri, String txt){
       if(uri == null || uri.toString().isBlank() || txt == null || txt.isBlank()){
           throw new IllegalArgumentException();
       }
       this.uriKey = uri;
       this.documentTxt = txt;
    }

    //should throw ancIllegalArgumentException if either argument is null or empty/blank:
    public DocumentImpl(URI uri, byte[] binaryData){
        if(uri == null || uri.toString().isBlank() || binaryData == null || binaryData.length == 0){
            throw new IllegalArgumentException();
        }
        this.uriKey = uri;
        this.documentBinaryData = binaryData;
    }

    /**
     * @return content of text document
     */
    @Override
    public String getDocumentTxt() {

        return this.documentTxt;
    }

    /**
     * @return content of binary data document
     */
    @Override
    public byte[] getDocumentBinaryData() {

        return this.documentBinaryData;
    }

    /**
     * @return URI which uniquely identifies this document
     */
    @Override
    public URI getKey() {

        return this.uriKey;
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
}
