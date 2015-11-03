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

        LOGDEV.debug(" -> starting generating the search engines queries for counter examples");

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

        SearchEngine engine = new AzureBingSearchEngine();
        // download the search results in parallel
        long startCrawl = System.currentTimeMillis();
        EvidenceCrawler crawler = new EvidenceCrawler(model, queries);
        Evidence evidence = crawler.crawlEvidence(engine);
        LOGDEV.debug(" -> crawling evidence took " + TimeUtil.formatTime(System.currentTimeMillis() - startCrawl));

        //returned proofs
        //Set<ComplexProof> proofs = evidence.getComplexProofs();

        String _uri;
        URI uri = null;
        String domain = null;

        List<WebSite> websites = evidence.getAllWebSites();
        List<ComplexProof> proofs;
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
            System.out.println(":: query: " + w.getQuery().toString());
            System.out.println("--------------------------------------------------------------------------");

            writer_overview.println(_uri + separator + domain + separator + proofs.size() + separator + w.getQuery().getSubjectLabel() + separator + w.getQuery().getPropertyLabel() + separator + w.getQuery().getObjectLabel() + separator + w.getLanguage());

            for (ComplexProof proof: proofs){

                writer.println(_uri + separator + domain + separator + proofs.size() + separator + proof.getWebSite().getLanguage() + separator + proof.getSmallContext());

                //System.out.println("Proof pattern: " + proof.getPattern().naturalLanguageRepresentation);

                System.out.println("Proof language: " + proof.getWebSite().getLanguage());
                System.out.println("Proof website url: " + proof.getWebSite().getUrl());
                //System.out.println("Proof tiny context: " + proof.getTinyContext());
                System.out.println("Proof small context: " + proof.getSmallContext());
                //System.out.println("Proof medium context: " + proof.getMediumContext());
                //System.out.println("Proof large context: " + proof.getLargeContext());
                System.out.println("");
            }
            System.out.println("--------------------------------------------------------------------------");
        }


    }

    public static void main(String[] args) throws Exception {

        writer = new PrintWriter("proofs.csv", "UTF-8");
        writer_overview = new PrintWriter("proofs-overview.csv", "UTF-8");


        startProcess(getOneExample(), Defacto.TIME_DISTRIBUTION_ONLY.NO);

        writer.close();
        writer_overview.close();

    }

    private static DefactoModel getOneExample(){
        Model model = ModelFactory.createDefaultModel();
        model.read(DefactoModel.class.getClassLoader().getResourceAsStream("Nobel1909.ttl"), null, "TURTLE");
        return new DefactoModel(model, "Nobel Model", true, Arrays.asList("en", "fr", "de"));
    }


}
