package org.aksw.defacto.model;

/**
 * Created by dnes on 05/01/16.
 */
public class PropertyConfiguration {

    private String subjectClass;
    private String predicateUri;
    private String objectClass;

    public PropertyConfiguration(String predicate, String sClass, String oClass){
        this.subjectClass = sClass;
        this.predicateUri = predicate;
        this.objectClass = oClass;
    }

    public String getSubjectClass(){
        return this.subjectClass;
    }

    public String getPredicateUri(){
        return this.predicateUri;
    }

    public String getObjectClass(){
        return this.objectClass;
    }

}
