package org.aksw.defacto.model;

/**
 * Created by dnes on 05/01/16.
 */
public class PropertyConfiguration {

    private String subjectClass;
    private String predicateUri;
    private String objectClass;
    private boolean isFunctional;

    public PropertyConfiguration(String predicate, String sClass, String oClass, boolean functional){
        this.subjectClass = sClass;
        this.predicateUri = predicate;
        this.objectClass = oClass;
        this.isFunctional = functional;
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

    public boolean isFunctionalProperty() {
        return this.isFunctional;
    }
}
