package org.aksw.defacto.helper;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import org.aksw.defacto.Defacto;
import org.aksw.defacto.boa.Pattern;
import org.aksw.defacto.evidence.ComplexProof;
import org.aksw.defacto.evidence.Evidence;
import org.aksw.defacto.evidence.WebSite;
import org.aksw.defacto.model.DefactoModel;
import org.aksw.defacto.search.crawl.EvidenceCrawler;
import org.aksw.defacto.search.engine.SearchEngine;
import org.aksw.defacto.search.engine.bing.AzureBingSearchEngine;
import org.aksw.defacto.search.query.MetaQuery;
import org.aksw.defacto.search.query.QueryGenerator;
import org.aksw.defacto.util.TimeUtil;

import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by dnes on 30/10/15.
 */
public class proofExtractor {

    public static org.apache.log4j.Logger LOGDEV    = org.apache.log4j.Logger.getLogger("developer");
    public static PrintWriter writer;
    public static PrintWriter writer_overview;
    public static String separator = ";";

    private static void startProcess(DefactoModel model, Defacto.TIME_DISTRIBUTION_ONLY onlyTimes){

        Defacto.init();

        LOGDEV.info("Extracting Proofs for: " + model);
        Defacto.onlyTimes = onlyTimes;

        LOGDEV.debug(" [1] starting generating the search engines queries for counter examples");

        long start = System.currentTimeMillis();
        QueryGenerator queryGenerator = new QueryGenerator(model);
        Map<Pattern,MetaQuery> queries = new HashMap<>();
        for ( String language : model.languages ) {
            Map<Pattern,MetaQuery> q = queryGenerator.getCounterExampleSearchEngineQueries(language);
            queries.putAll(q);
        }
        if ( queries.size() <= 0 ) {
            LOGDEV.debug(" -> none query has been generated for the model: " + model);
        }
        LOGDEV.debug(" -> Preparing queries took " + TimeUtil.formatTime(System.currentTimeMillis() - start));

        int i;

        //for (Map.Entry<Pattern, MetaQuery> entry : queries.entrySet())
       // {
       //     LOGDEV.debug(" -> query: " + entry.getKey() + "/" + entry.getValue());
        //}


        SearchEngine engine = new AzureBingSearchEngine();
        // download the search results in parallel
        long startCrawl = System.currentTimeMillis();
        EvidenceCrawler crawler = new EvidenceCrawler(model, queries);
        Evidence evidence = crawler.crawlCounterEvidence(engine);
        LOGDEV.debug(" -> crawling counter evidence took " + TimeUtil.formatTime(System.currentTimeMillis() - startCrawl));

        String _uri;
        URI uri = null;
        String domain = null;

        List<Pattern> patterns = evidence.getBoaPatterns();
        for (Pattern p: patterns){
            LOGDEV.debug(" -> Pattern: " + p.toString());
        }
        if (patterns.size() == 0){
            LOGDEV.debug(" -> No pattern has been found for the evidence!");
        }

        for ( Map.Entry<Pattern, List<WebSite>> patternToWebSites : evidence.getWebSites().entrySet()) {
            LOGDEV.debug("Pattern: " + patternToWebSites.getKey().toString() );
            for (WebSite website : patternToWebSites.getValue()) {
                LOGDEV.debug("website: " + website.getUrl());
            }
            LOGDEV.debug("");
        }

        List<WebSite> websites = evidence.getAllWebSites();
        List<ComplexProof> proofs;

        //HEADERS
        writer.println("uri" + separator + "domain"+ separator + "total-proofs" + separator + "subject label" + separator + "property label" + separator + "object label" + separator + "language");
        writer_overview.println("uri" + separator + "domain" + separator + "total-proofs" + separator + "subject label" + separator + "property label" + separator + "object label" + separator + "language");


        System.out.println(":: nr. websites: " + evidence.getAllWebSites().size());
        List<WebSite> websitesWithproofsInBetween = evidence.getAllWebSitesWithComplexProofAndAtLeastOneBOAPatternInBetween();
        System.out.println(":: nr. websites with proofs with pattern in between S and O: " + websitesWithproofsInBetween.size());

        for (WebSite w: websites) {

            try {
                uri = new URI(w.getUrl().toString());
                domain = uri.getHost();
            } catch (URISyntaxException e) {

            }
            _uri = w.getUrl();


            System.out.println(":: website: " + _uri);
            System.out.println(":: website domain: " + domain);

            proofs = evidence.getComplexProofs(w);
            System.out.println(":: nr. proofs: " + proofs.size());

            List<ComplexProof> proofsInBetween = evidence.getComplexProofsAtLeastOneBOAPatternInBetween(w);
            System.out.println(":: nr. proofs with pattern in between: " + proofsInBetween.size());

            System.out.println(":: query: " + w.getQuery().toString());
            System.out.println("--------------------------------------------------------------------------");


            writer_overview.println(_uri + separator + domain + separator + proofs.size() + separator + w.getQuery().getSubjectLabel() + separator + w.getQuery().getPropertyLabel() + separator + w.getQuery().getObjectLabel() + separator + w.getLanguage());

            for (ComplexProof proof: proofs){

                writer.println(_uri + separator + domain + separator + proofs.size() + separator + proof.getWebSite().getLanguage() + separator + proof.getSmallContext());

                //System.out.println("Proof pattern: " + proof.getPattern().naturalLanguageRepresentation);

                System.out.println("Proof language: " + proof.getWebSite().getLanguage());
                System.out.println("Proof website url: " + proof.getWebSite().getUrl());
                //System.out.println("Proof tiny context: " + proof.getTinyContext());
                //System.out.println("Proof small context: " + proof.getSmallContext());
                //System.out.println("Proof medium context: " + proof.getMediumContext());
                System.out.println("Proof large context: " + proof.getLargeContext());
                System.out.println("website text:" + proof.getWebSite().getText());
                System.out.println("");
            }
            System.out.println("--------------------------------------------------------------------------");
        }

        LOGDEV.debug(" -> DONE!");


    }

    public static void main(String[] args) throws Exception {

        writer = new PrintWriter("proofs_neg.csv", "UTF-8");
        writer_overview = new PrintWriter("proofs_neg_stats.csv", "UTF-8");


        startProcess(getOneExample(), Defacto.TIME_DISTRIBUTION_ONLY.NO);

        writer.close();
        writer_overview.close();

    }

    private static DefactoModel getOneExample(){
        Model model = ModelFactory.createDefaultModel();
        model.read(DefactoModel.class.getClassLoader().getResourceAsStream("Nobel1909.ttl"), null, "TURTLE");
        return new DefactoModel(model, "Nobel Model", true, Arrays.asList("en"));
    }


}
