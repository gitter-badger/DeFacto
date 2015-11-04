package org.aksw.defacto.search.query;

import java.util.ArrayList;

/**
 * 
 * @author Daniel Gerber <dgerber@informatik.uni-leipzig.de>
 */
public class BingQuery implements Query {

    public static org.apache.log4j.Logger LOGDEV    = org.apache.log4j.Logger.getLogger("developer");

    //just for testing...later on put it somewhere else...
    static ArrayList<String> negatedProperty = new ArrayList<String>() {{
        add("rejected");
        add("losers");
        add("renounce");
        add("did not win");
        add("loser");
        add("did not receive");
    }};

    @Override
    public String generateQuery(MetaQuery query) {

        String subject  = query.getSubjectLabel().replace("&", "and");
        String property = normalizePredicate(query.getPropertyLabel().trim());
        String object   = query.getObjectLabel().replace("&", "and");
        String queryString = "";

        LOGDEV.debug(" -> starting query generation process...");
        LOGDEV.debug("METAQUERY: " +query );
        
        // standard search engine query
        if ( query.getTopicTerms().isEmpty() ) {
            
            // since we don't get any results if we use the near operator we skip it 
            // if ( query.getPropertyLabel().equals("??? NONE ???") ) queryString = String.format("\"%s\" near:%s \"%s\"", subject, 15, object);
            if ( query.getPropertyLabel().equals("??? NONE ???") ) queryString = String.format("\"%s\" AND \"%s\"", subject, object);
            else {
                if (property.equals("NONE")) {
                    queryString = String.format("\"%s\" AND \"%s\"", subject, object);
                }else {
                    if (negatedProperty.contains(property)){
                        queryString = String.format("\"%s\" AND \"%s\" AND norelax:\"%s\"", subject, object, property);
                    }else{
                        queryString = String.format("\"%s\" AND \"%s\" AND \"%s\"", subject, property, object);
                    }

                }
            }
        }
        else {
            
            // this query is going to be exectued from the topic majority web feature
            queryString = String.format("\"%s\" AND \"%s\" AND \"%s\"", subject, property, object);
            
            // add the first 3 topic terms to the query
            for ( int i = 0 ; i < 3 && i < query.getTopicTerms().size() ; i++)
                // use the norelax option here because bing only includes first 4 terms as must contain
                queryString += " AND norelax:\"" + query.getTopicTerms().get(i).getWord() + "\"";
        }

        LOGDEV.debug("QUERYSTRING: " + queryString);

        return queryString;
    }

    @Override
    public String normalizePredicate(String propertyLabel) {
        System.out.println(propertyLabel);
        return propertyLabel.replaceAll(",", "").replace("`", "").replace(" 's", "'s").replace("?R?", "").replace("?D?", "").replaceAll(" +", " ").replaceAll("'[^s]", "").replaceAll("&", "and").trim();
    }

    //AQUI: ALTERAR PARA CONSIDERAR NEGATION QUERIES
    public static void main(String[] args) {

        MetaQuery query1 = new MetaQuery("Franck Ribery|-| politician |-|Galatasaray|-|en");
        MetaQuery query2 = new MetaQuery("Mount Eccles National Park|-|?D? is a stupid ?R?|-|Texas|-|en");
        MetaQuery query3 = new MetaQuery("Mount Eccles National Park|-|?D? 's is a , ,, , '' stupid ?R?|-|Texas|-|fr");
        
        BingQuery bq = new BingQuery();
        System.out.println(bq.generateQuery(query1));
        System.out.println("");
        System.out.println(bq.generateQuery(query2));
        System.out.println("");
        System.out.println(bq.generateQuery(query3));
    }
}
