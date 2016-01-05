package org.aksw.defacto.model;

import java.util.HashMap;

/**
 * Created by dnes on 05/01/16.
 * TODO: refine the categories, e.x.: starring has an ACTOR (specialization of PERSON)
 */
public class PropertyConfigurationSingleton {

    private static HashMap<String,PropertyConfiguration> configuration;
    private static PropertyConfigurationSingleton instance = null;

    protected PropertyConfigurationSingleton() {
        configuration = new HashMap<>();
        configuration.put("award", new PropertyConfiguration("http://dbpedia.org/ontology/award", "PERSON", "PRIZE"));
        configuration.put("birth", new PropertyConfiguration("http://dbpedia.org/ontology/birthDate", "PERSON", "LOCATION"));
        configuration.put("death", new PropertyConfiguration("http://dbpedia.org/ontology/deathPlace", "PERSON", "LOCATION"));
        configuration.put("foundationPlace", new PropertyConfiguration("http://dbpedia.org/ontology/foundationPlace", "COMPANY", "LOCATION"));
        configuration.put("leader", new PropertyConfiguration("http://dbpedia.org/ontology/leaderName", "PERSON", "LOCATION"));
        configuration.put("nbateam", new PropertyConfiguration("http://dbpedia.org/ontology/nflTeam", "PERSON", "ORGANIZATION"));
        configuration.put("publicationDate", new PropertyConfiguration("http://dbpedia.org/ontology/publicationDate", "PERSON", "PUBLICATION")); //title instead of date
        configuration.put("spouse", new PropertyConfiguration("http://dbpedia.org/ontology/spouse", "PERSON", "PERSON"));
        configuration.put("subsidiary", new PropertyConfiguration("http://dbpedia.org/ontology/subsidiary", "COMPANY", "COMPANY"));
        configuration.put("starring", new PropertyConfiguration("http://dbpedia.org/ontology/starring", "MOVIE", "PERSON"));
    }
    public static PropertyConfigurationSingleton getInstance() {
        if(instance == null) {
            instance = new PropertyConfigurationSingleton();
        }
        return instance;
    }

    public HashMap<String, PropertyConfiguration> getConfigurations(){
        return configuration;
    }

}