package edu.yu.cs.com1320.project.stage5.impl;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import edu.yu.cs.com1320.project.stage5.Document;
import edu.yu.cs.com1320.project.stage5.PersistenceManager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.util.Scanner;

public class DocumentPersistenceManager implements PersistenceManager<URI, Document> {
    private File fileDir;
    public DocumentPersistenceManager(File baseDir){
        if (baseDir == null) {
            this.fileDir = new File(System.getProperty("user.dir"));
        } else {
            this.fileDir = baseDir;
        }
    }

    @Override
    public void serialize(URI uri, Document val) throws IOException {
        if (uri == null) {
            throw new IllegalArgumentException();
        }
        if (val == null) {
            throw new IllegalArgumentException();
        }
        String stringPath = manipulateURI(uri);
        File files = new File(fileDir, stringPath);
        files.getParentFile().mkdirs();
        GsonBuilder parse = new GsonBuilder();
        Gson gson = parse.create();
        String stringJson = gson.toJson(val);
        FileWriter writer = new FileWriter(files);
        writer.write(stringJson);
        writer.close();
    }
    @Override
    public Document deserialize(URI uri) throws IOException {
        String stringPath = manipulateURI(uri);
        Scanner content = new Scanner(new File(fileDir, stringPath));
        if (content == null) {
            return null;
        } else {
            Gson gson = new Gson();
            if (content.hasNextLine() == false) {
                return null;
            }else {
                Document doc = gson.fromJson(content.nextLine(), DocumentImpl.class);
                delete(uri);
                return doc;
            }
        }
    }

    @Override
    public boolean delete(URI uri) throws IOException {
        String stringPath = manipulateURI(uri);
        File file = new File(fileDir, stringPath);
        if(file.delete()){
            return true;
        } else {
            return false;
        }
    }

    private String manipulateURI(URI u){
        String stringPath = u.toString();
        stringPath = stringPath.replaceFirst("^(http[s]?://)", "");
        stringPath = stringPath.replaceFirst("/+$", "") + ".json";;
        return stringPath;
    }
}
