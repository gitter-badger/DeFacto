package org.aksw.defacto.search.query;

import java.util.HashMap;
import java.util.Map;

import org.aksw.defacto.Constants;
import org.aksw.defacto.boa.BoaPatternSearcher;
import org.aksw.defacto.boa.Pattern;
import org.aksw.defacto.model.DefactoModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.rdf.model.Statement;

/**
 * 
 * @author Daniel Gerber <dgerber@informatik.uni-leipzig.de>
 *
 */
public class QueryGenerator {

    public static final BoaPatternSearcher patternSearcher = new BoaPatternSearcher();
    private static final Logger LOGGER = LoggerFactory.getLogger(QueryGenerator.class);
    public static org.apache.log4j.Logger LOGDEV    = org.apache.log4j.Logger.getLogger("developer");
    private DefactoModel model;
    
    /**
     * 
     * @param model
     */
    public QueryGenerator(DefactoModel model) {
        
        this.model = model;
    }

    public Map<Pattern,MetaQuery> getCounterExampleSearchEngineQueries(String language){
        return this.generateCounterExampleSearchQueries(model.getFact(), language);
    }

    private Map<Pattern,MetaQuery> generateCounterExampleSearchQueries(Statement fact, String language){

        Map<Pattern,MetaQuery> counterqueryStrings =  new HashMap<>();
        String subjectLabel = model.getSubjectLabelNoFallBack(language);
        String objectLabel  = model.getObjectLabelNoFallBack(language);

        // we dont have labels in the given language so we generate a foreign query with english labels
        if ( subjectLabel.equals(Constants.NO_LABEL) || objectLabel.equals(Constants.NO_LABEL) ) {
            subjectLabel = model.getSubjectLabel("en");
            objectLabel = model.getObjectLabel("en");
        }
        for (Pattern pattern : patternSearcher.getInverseNaturalLanguageRepresentations(fact.getPredicate().getURI(), language)) {

            if ( !pattern.getNormalized().trim().isEmpty() ) {

                MetaQuery metaQuery = new MetaQuery(subjectLabel, pattern.getNormalized(), objectLabel, language, null);
                System.out.println(metaQuery);
                counterqueryStrings.put(pattern, metaQuery);
            }
        }

        // add one query without any predicate
        counterqueryStrings.put(new Pattern("??? NONE ???", language), new MetaQuery(subjectLabel, "??? NONE ???", objectLabel, language, null));
        LOGGER.debug(String.format("Generated %s negated queries for fact ('%s'): %s", counterqueryStrings.size(), language, fact.asTriple()));

        return counterqueryStrings;

    }
    
    /**
     * 
     * @return
     */
    public Map<Pattern,MetaQuery> getSearchEngineQueries(String language){
        return this.generateSearchQueries(model.getFact(), language);
    }


    /**
     * 
     * @param uriToLabels
     * @param fact
     * @return
     */
    private Map<Pattern,MetaQuery> generateSearchQueries(Statement fact, String language){

        Map<Pattern,MetaQuery> queryStrings =  new HashMap<>();
        String subjectLabel = model.getSubjectLabelNoFallBack(language);//.replaceAll("\\(.+?\\)", "").trim(); 
        String objectLabel  = model.getObjectLabelNoFallBack(language);//.replaceAll("\\(.+?\\)", "").trim();
        
        // we dont have labels in the given language so we generate a foreign query with english labels
        if ( subjectLabel.equals(Constants.NO_LABEL) || objectLabel.equals(Constants.NO_LABEL) ) {
        	subjectLabel = model.getSubjectLabel("en");
        	objectLabel = model.getObjectLabel("en");
        }
        
        // TODO
        // query boa index and generate the meta queries
        int i =0;
        for (Pattern pattern : patternSearcher.getNaturalLanguageRepresentations(fact.getPredicate().getURI(), language)) {
        	
        	if ( !pattern.getNormalized().trim().isEmpty() ) {
        		
        		MetaQuery metaQuery = new MetaQuery(subjectLabel, pattern.getNormalized(), objectLabel, language, null);
        		System.out.println(metaQuery);
        		queryStrings.put(pattern, metaQuery);

                LOGDEV.debug(" " + i + " pattern norm = [" + pattern.getNormalized() + "] metaquery: " +
                        " s = [" + metaQuery.getSubjectLabel() +
                        "] p = [" + metaQuery.getPropertyLabel() +
                        "] o = [" + metaQuery.getObjectLabel() + "]");

        	}
            i++;
        }
        
        // add one query without any predicate
        queryStrings.put(new Pattern("??? NONE ???", language), new MetaQuery(subjectLabel, "??? NONE ???", objectLabel, language, null));
        LOGDEV.debug(String.format(" -> Generated %s queries for fact ('%s'): %s", queryStrings.size(), language, fact.asTriple()));
        
        return queryStrings;
    }

}
