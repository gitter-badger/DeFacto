package org.aksw.defacto.search.engine.bing;

import net.billylieurance.azuresearch.AbstractAzureSearchQuery.AZURESEARCH_QUERYTYPE;
import net.billylieurance.azuresearch.AbstractAzureSearchResult;
import net.billylieurance.azuresearch.AzureSearchCompositeQuery;
import net.billylieurance.azuresearch.AzureSearchResultSet;
import net.billylieurance.azuresearch.AzureSearchWebResult;
import org.aksw.defacto.Defacto;
import org.aksw.defacto.boa.Pattern;
import org.aksw.defacto.evidence.WebSite;
import org.aksw.defacto.search.engine.DefaultSearchEngine;
import org.aksw.defacto.search.query.BingQuery;
import org.aksw.defacto.search.query.MetaQuery;
import org.aksw.defacto.search.result.DefaultSearchResult;
import org.aksw.defacto.search.result.SearchResult;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Date: 2/6/12
 * Time: 7:11 PM
 * Class BingSearchEngine contains the facilities required to contact Bing search engine to get the search results for a
 * set of keywords
 * 
 * @author Daniel Gerber <dgerber@informatik.uni-leipzig.de>
 * @author Mohamed Morsey <morsey@informatik.uni-leipzig.de>
 */
public class AzureBingSearchEngine extends DefaultSearchEngine {

    private String NUMBER_OF_SEARCH_RESULTS;
    private static String BING_API_KEY;
    private static Logger logger =  Logger.getLogger(AzureBingSearchEngine.class);
    public static org.apache.log4j.Logger LOGDEV    = org.apache.log4j.Logger.getLogger("developer");
    
    public AzureBingSearchEngine() {
        
        if ( Defacto.DEFACTO_CONFIG != null ) {

            BING_API_KEY = Defacto.DEFACTO_CONFIG.getStringSetting("crawl", "BING_API_KEY");
            NUMBER_OF_SEARCH_RESULTS = Defacto.DEFACTO_CONFIG.getStringSetting("crawl", "NUMBER_OF_SEARCH_RESULTS");
        }
    }
    
    @Override
    public Long getNumberOfResults(MetaQuery query) {
        
        return 0L;
    }
    
    public static void main(String[] args) {
        
//        MetaQuery query0 = new MetaQuery(String.format("%s|-|%s|-|%s|-|%s", "Obama", "?D? is president of ?R?", "United States", "en"));
//        MetaQuery query  = new MetaQuery(String.format("%s|-|%s|-|%s|-|%s", "Montebelluna", "?R? Wii version of `` ?D?", "Procter & Gamble", "en"));
//        MetaQuery query1 = new MetaQuery(String.format("%s|-|%s|-|%s|-|%s", "Gloria Estefan", "??? NONE ???", "Remember Me with Love", "en"));
//        MetaQuery query2 = new MetaQuery(String.format("%s|-|%s|-|%s|-|%s", "Avram Hershko", "?D? is a component of ?R?", "United States Marine Corps", "en"));
        
        Defacto.init();

        Pattern p = new Pattern();
        p.naturalLanguageRepresentation = "?D? was not prizewinning ?R?";

        MetaQuery q = new MetaQuery("Guglielmo Marconi|-| not prizewinning |-|Nobel Prize in Physics|-|en", p);

        //MetaQuery q = new MetaQuery("Ghostbusters II|-|?D? NONE ?R?|-|Bill Murray|-|fr");
        AzureBingSearchEngine engine = new AzureBingSearchEngine();
        System.out.println(BING_API_KEY);
        System.out.println(engine.query(q, null).getTotalHitCount());
//        System.out.println(engine.query(query, null).getWebSites().size());
        
//        URI uri;
//        try {
//            String query = "'Obama' AND 'is president of' AND 'United States'";
//                uri = new URI("https", "api.datamarket.azure.com", "/Data.ashx/Bing/SearchWeb/v1/Web",
//                        "Query='"+query+"'", null );
//                //Bing and java URI disagree about how to represent + in query parameters.  This is what we have to do instead...
//                uri = new URI(uri.getScheme() + "://" + uri.getAuthority()  + uri.getPath() + "?" + uri.getRawQuery().replace("+", "%2b"));
//                System.out.println(uri);
//                
//         //log.log(Level.WARNING, uri.toString());
//        } catch (URISyntaxException e1) {
//                e1.printStackTrace();
//                return;
//        }
        
        
        
//        System.out.println(engine.query(query1, null).getWebSites().size());
//        System.out.println(engine.query(query2, null).getWebSites().size());
    }
    
    
    
    @Override
    public SearchResult query(MetaQuery query, Pattern pattern) {

        try {

            AzureSearchCompositeQuery aq = new AzureSearchCompositeQuery();
            aq.setAppid(BING_API_KEY);
            aq.setLatitude("47.603450");
            aq.setLongitude("-122.329696");
            if ( query.getLanguage().equals("en") ) aq.setMarket("en-US");
            else if ( query.getLanguage().equals("de") )aq.setMarket("de-DE");
            else aq.setMarket("fr-FR");
            
            aq.setSources(new AZURESEARCH_QUERYTYPE[] { AZURESEARCH_QUERYTYPE.WEB });

            String strQuery = this.generateQuery(query);
            LOGDEV.debug("BING Query: " + strQuery);
            aq.setQuery(strQuery);
            System.out.println(this.generateQuery(query));
            aq.doQuery();
            
            AzureSearchResultSet<AbstractAzureSearchResult> ars = aq.getQueryResult();
            // query bing and get only the urls and the total hit count back
            List<WebSite> results = new ArrayList<WebSite>();
            
            int i = 1;
            for (AbstractAzureSearchResult result : ars){

                if ( i > Integer.valueOf(NUMBER_OF_SEARCH_RESULTS) ) break;
                
                if ( ((AzureSearchWebResult) result).getUrl().startsWith("http://images.webgiftr.com/") 
                		|| ((AzureSearchWebResult) result).getUrl().startsWith("http://www.calza.com/")) continue;
                
                WebSite website = new WebSite(query, ((AzureSearchWebResult) result).getUrl());
                website.setTitle(result.getTitle());
                website.setRank(i++);
                website.setLanguage(query.getLanguage());
                results.add(website);
            }
            
            return new DefaultSearchResult(results, ars.getWebTotal(), query, pattern, false);
        }
        catch (Exception e) {
            
        	e.printStackTrace();
            return new DefaultSearchResult(new ArrayList<WebSite>(), 0L, query, pattern, false);
        }
    }

    @Override
    public String generateQuery(MetaQuery query) {

        LOGDEV.debug(" -> generateQuery(" + query.toString() + ")");
        return new BingQuery().generateQuery(query);
    }

}
