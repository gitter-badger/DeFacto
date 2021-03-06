package org.aksw.defacto.search.crawl.crawler4j;

import org.aksw.defacto.Defacto;
import org.apache.poi.ss.formula.functions.T;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by esteves on 18.08.15.
 */
public class DBPediaExternalPageController {

    private static ArrayList<String> _seeds;


    public DBPediaExternalPageController(ArrayList seeds){
        _seeds = seeds;
    }
    public static void main(String[] args) throws Exception {

        String crawlStorageFolder = Defacto.DEFACTO_CONFIG.getStringSetting("crawl", "CRAWL_STORAGE_FOLDER");
        int numberOfCrawlers = Defacto.DEFACTO_CONFIG.getIntegerSetting("crawl", "NUMBER_OF_CRAWLERS");

        CrawlConfig config = new CrawlConfig();
        config.setCrawlStorageFolder(crawlStorageFolder);


        /*
         * Instantiate the controller for this crawl.
         */
        PageFetcher pageFetcher = new PageFetcher(config);
        RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
        RobotstxtServer robotstxtServer = new RobotstxtServer(robotstxtConfig, pageFetcher);
        CrawlController controller = new CrawlController(config, pageFetcher, robotstxtServer);

        /*
         * For each crawl, you need to add some seed urls. These are the first
         * URLs that are fetched and then the crawler starts following links
         * which are found in these pages
         */
        Iterator<String> s = _seeds.iterator();
        while (s.hasNext()) {
            controller.addSeed(s.next());
        }

        /*
         * Start the crawl. This is a blocking operation, meaning that your code
         * will reach the line after this only when crawling is finished.
         */
        controller.start(DBPediaExternalPageCrawler.class, numberOfCrawlers);
    }

}
