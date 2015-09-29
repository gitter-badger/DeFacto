package org.aksw.defacto.dataweb;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

import com.hp.hpl.jena.rdf.model.*;
import org.aksw.defacto.util.LabeledTriple;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.nnsoft.sameas4j.DefaultSameAsServiceFactory;
import org.nnsoft.sameas4j.Equivalence;
import org.nnsoft.sameas4j.SameAsService;
import org.nnsoft.sameas4j.SameAsServiceException;
import org.nnsoft.sameas4j.cache.InMemoryCache;

import uk.ac.shef.wit.simmetrics.similaritymetrics.QGramsDistance;

import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

/**
 * 
 * Searches the data web for triples, which are similar to an input triple.
 * Currently, this is backed by sameAs.org
 * 
 * @author Jens Lehmann
 * @author Diego Esteves
 * TODO make  this a feature and move it to the correct package
 * TODO delete InformationFinder package
 */
public class DataWebResultFinder {

	private static Logger logger = Logger.getLogger(DataWebResultFinder.class);
	private static HttpSolrServer server_triples = new HttpSolrServer("http://localhost:8123/solr/ld_triples");
    private static HttpSolrServer server_uris = new HttpSolrServer("http://localhost:8123/solr/ld_uris");
    private static ArrayList<String> blackList = new ArrayList<>();
    private static Set<String> notAllowedProperties = new HashSet<>();
    private int numberOfTriples = 0;
	private int numberOfRelevantTriples = 0;
	private int labelCalls = 0;
    Set<String> relevantTriples = new TreeSet<String>();
	private SameAsService service;
	static {
		notAllowedProperties.add("http://www.w3.org/2002/07/owl#sameAs");
		notAllowedProperties.add("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
	}

	public static void main(String args[]) throws SameAsServiceException {

		blackList.add("http://www.econbiz.de/");

		DataWebResultFinder finder = new DataWebResultFinder();

        LabeledTriple ltriple = new LabeledTriple("http://dbpedia.org/resource/Berlin", "Berlin",
				"http://dbpedia.org/ontology/leader", "leader",
				"http://dbpedia.org/resource/Klaus_Wowereit", "Klaus Wowereit");

		Map<LabeledTriple, Double> similarTriples = finder.findSimilarTriples(ltriple, 0.0, 10);

		for (Entry<LabeledTriple, Double> entry : similarTriples.entrySet()) {
			if (entry.getValue() > 0.0) {
				System.out.println(entry.getValue() + " " + entry.getKey());}
		}
	}

    public Map<LabeledTriple, Double> findSimilarTriples(LabeledTriple inputTriple, double threshold, int maximumEquivalentURIs) throws SameAsServiceException {

		service = DefaultSameAsServiceFactory.getSingletonInstance();
		service.setCache(new InMemoryCache());
		String _uri = inputTriple.getSubjectURI();
        //cache here the querying to the sameas service, and check again only after X days....
		Equivalence equivalence = service.getDuplicates(URI.create(_uri));
		System.out.println( ":: Number of equivalent URIs for [" + _uri + "] : " + equivalence.getAmount());

		long startTime = System.currentTimeMillis();

		Set<LabeledTriple> ltriples = new TreeSet<>();
		int i = 0;
		outerloop:
		for (URI uri : equivalence) {
			if (i==maximumEquivalentURIs){
				System.out.println(" -> the limit has been reached, stopping the equivalent URIs searching");
				break outerloop;}
			if (!blackList.contains(uri.toString())){
				ltriples.addAll(getExistingTriplesByURI(uri.toString()));
				i++;
			}else{
                System.out.println(" -> the URI has been denied");
            }
		}
		long duration = System.currentTimeMillis() - startTime;

        System.out.println(":: Number of all SameAs URIs (excluding denied): " + i);
		System.out.println(":: Number of all triples: " + numberOfTriples);
		System.out.println(":: Number of relevant triples: " + numberOfRelevantTriples);
		System.out.println(":: Number of distinct relevant triples: " + relevantTriples.size());
		//System.out.println(":: Number of distinct resources: " + distinctResources.size());
		System.out.println(":: Linked Data label calls: " + labelCalls);
		System.out.println(":: Time spend for retrieving resources: " + duration + " ms");

		// score all triples, which we have obtained
		System.out.println(" -> starting scoring " + ltriples.size() + " triples...");
		Map<LabeledTriple, Double> simTriples = new HashMap<>();
		for (LabeledTriple t : ltriples) {
			simTriples.put(t, score(inputTriple, t));
		}
		return simTriples;
	}

    /**
     * isRelevant -> decides whether the triple is relevant given an URI.
     */
    private boolean isRelevant(Statement st, String uri){

		if (st.getSubject().isURIResource()
				&& st.getObject().isURIResource()
				&& st.getSubject().getURI().toString().equals(uri)
				&& !st.getPredicate().getURI().startsWith(RDF.getURI())
				&& !st.getPredicate().getURI().startsWith(RDFS.getURI())
				&& !st.getPredicate().getURI().startsWith(OWL.getURI())
				&& !st.getPredicate().getURI().startsWith("http://www.w3.org/2004/02/skos/core")
				&& !st.getPredicate().getURI().startsWith("http://www.w3.org/2008/05/skos-xl")
				&& !st.getPredicate().getURI().startsWith("http://purl.org/dc/terms/subject")
				&& !st.getPredicate().getURI().startsWith("http://www.geonames.org/ontology#wikipediaArticle")
				&& !st.getPredicate().getURI().startsWith("http://dbpedia.org/ontology/wikiPageExternalLink")
				) {
					return true;
				}
		else{
			return false;
		}
	}

    /**
     * addTripleToCache -> add triples into the cache.
     */
    private static String addTripleToCache(long shash, long phash, long ohash) throws Exception{

        SolrInputDocument doc = new SolrInputDocument();
        UpdateResponse response;
        doc.addField("s", shash);
        doc.addField("p", phash);
        doc.addField("o", ohash);

        //check whether the response returns the ID somewhere...
        response = server_triples.add(doc);
        server_triples.commit();

        //otherwise I would have to stupidly query it :-(
        SolrQuery query = new SolrQuery();
        query.setQuery("s:" + shash + " AND p:" + phash + " AND o:" + ohash);
        query.setFields("id");
        query.setStart(0);
        query.set("defType", "edismax");

        QueryResponse response2 = server_triples.query(query);
        SolrDocumentList results = response2.getResults();

        if (results != null && results.size() > 0){
            return results.get(0).getFieldValue("id").toString();
        } else {
            throw new Exception("error on getting the ID for the operation");
        }

	}

    /**
     * addTriplesToCache -> extract triples from given model and add them into cache.
     */
    private Set<LabeledTriple> addTriplesToCache(Model m, String uri) throws Exception{

        Set<LabeledTriple> addedTriples = new TreeSet<>();
        String sURI,pURI,oURI,sl,pl,ol;
        int i = 0;
        String tripleOK;
        int sURIOK, pURIOK, oURIOK;

        ArrayList<String> relatedTripleURIs = new ArrayList<>();

        //adding master URI
        String uril = getURILabel(m, uri);

        if (addURIToCache(uri.hashCode(), uri, uril, null) == 0){
            throw new Exception("error on adding URI to cache");
        }

        //reading related triples model
        StmtIterator it = m.listStatements();
        while (it.hasNext()) {
            Statement st = it.next();
            // // currently, we are only looking at object properties, so we
            // // assume only object properties can be similar;
            if (isRelevant(st, uri)) {
                numberOfRelevantTriples++;
                //System.out.println(st.getSubject().getNameSpace() + "+++" + st.getPredicate().toString() + "+++" + st.getObject().toString());

                sURI = st.getSubject().getNameSpace();
                pURI = st.getPredicate().toString();
                oURI = st.getObject().toString();
                sl = getURILabel(m, sURI);
                pl = getURILabel(m, pURI);
                ol = getURILabel(m, oURI);

                //adding the triples

                tripleOK = getTripleIDBySPO(sURI, pURI, oURI);
                if (StringUtils.isEmpty(tripleOK) || StringUtils.isBlank(tripleOK)){
                    addTripleToCache(sURI.hashCode(), pURI.hashCode(), oURI.hashCode());
                }

                //adding the URIs
                sURIOK = addURIToCache(sURI.hashCode(), sURI, sl, null);
                pURIOK = addURIToCache(pURI.hashCode(), pURI, pl, null);
                oURIOK = addURIToCache(oURI.hashCode(), oURI, ol, null);

                relatedTripleURIs.add(tripleOK.toString());

                //0 = error , 1 = existing URI, 2 = success
                if (StringUtils.isEmpty(tripleOK) || sURIOK == 0 || pURIOK == 0  || oURIOK == 0){
                    throw new Exception("failed to add URI to cache");
                }else{
                    addedTriples.add(new LabeledTriple(sURI,sl,pURI,pl,oURI,ol));
                }

            }
        }

        //updating the cache including related triples, then next time just query the cache instead of relies on linked data extraction from given URI
        updateCacheAddingRelatedURIs(uri.hashCode(), relatedTripleURIs);

        return addedTriples;
	}

    /**
     * getCachedRelatedTriplesByURI -> get the list of cached triples related to given URI.
     */
    private Set<LabeledTriple> getCachedRelatedTriplesByURI(String uri){

        //returns the related URIs cached on the server

        Set<LabeledTriple> ltriples = new TreeSet<>();
        Iterator<Object> iterator = null;
        String s,p,o,sl,pl,ol;

        try{
            SolrDocument doc_cached = getURIbyID(uri.hashCode());
            if (doc_cached != null) {

                Collection<Object> relatedURIs = doc_cached.getFieldValues("relatedTriplesIDs");

                iterator = relatedURIs.iterator();
                while (iterator.hasNext()) {
                    Object e = iterator.next();

                    SolrDocument triple = getTripleByID((long) e);

                    Object so = triple.getFieldValue("s");
                    Object po = triple.getFieldValue("p");
                    Object oo = triple.getFieldValue("o");

                    SolrDocument sdoc = getURIbyID(so.hashCode());
                    SolrDocument pdoc = getURIbyID(po.hashCode());
                    SolrDocument odoc = getURIbyID(oo.hashCode());

                    s =  sdoc.getFieldValue("uri").toString();
                    sl =  sdoc.getFieldValue("label").toString();

                    p =  pdoc.getFieldValue("uri").toString();
                    pl =  pdoc.getFieldValue("label").toString();

                    o =  odoc.getFieldValue("uri").toString();
                    ol =  odoc.getFieldValue("label").toString();

                    ltriples.add(new LabeledTriple(s,sl,p,pl,o,ol));

                }

            }

        }catch (Exception e){
            return null;
        }

        return ltriples;

	}

    /**
     * getLinkedDataURIs -> get the list of triples related to given URI.
     */
    public Set<LabeledTriple> getExistingTriplesByURI(String uri) {

		logger.info("Reading all statements from " + uri + ".");
		Set<LabeledTriple> ltriples = new TreeSet<>();

        try{
            //if uri exists in cache, then get from cache
            ltriples = getCachedRelatedTriplesByURI(uri);
            if (ltriples != null && ltriples.size() > 0){
                return ltriples;
            } else {
                //else, extract uris and save into cache
                Model m = getModelFromURI(uri);
                if (m == null) {
                    return ltriples;
                }
                else{
                    numberOfTriples += m.size();
                    System.out.println("returned triples: " + m.size());
                    ltriples = addTriplesToCache(m, uri);
                    return ltriples;
                }
            }

        }catch (Exception e){
            logger.error("Error: " + e.toString());
            return null;
        }

	}

    /**
     * getURILabel -> get the label of given URI based on the jena model.
     */
    private String getURILabel(Model m, String uri) {

        String label;

        try{

            if (m == null) {
                // use local name if URI cannot be dereferenced
                label = ModelFactory.createDefaultModel().createResource(uri).getLocalName();
            } else {
                Property prefLabel = m.createProperty("http://www.w3.org/2004/02/skos/core#prefLabel");
                Statement st = m.getResource(uri).getProperty(prefLabel);
                if (st != null) {
                    label = st.getObject().toString();
                } else {
                    st = m.getResource(uri).getProperty(RDFS.label);
                    if (st != null) {
                        label = st.getObject().toString();
                    } else {
                        // the fallback is to create the label from the URI name
                        label = m.createResource(uri).getLocalName();
                    }
                }
            }

            return label;

        }catch (Exception e){
            return null;
        }

    }


    /**
     * getModelFromURI -> extract linked data (as Jena Model) from given URI
     */
	public Model getModelFromURI(String uri) {

		Model model = ModelFactory.createDefaultModel();
		URL url;
		InputStream in = null;

        try {
            url = new URL(uri);
            URLConnection conn = url.openConnection();
            conn.setRequestProperty("Accept", "application/rdf+xml");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(5000);
            in = conn.getInputStream();
            try {
                model.read(in, "RDF/XML");
                return model;
            } catch (Exception e) {
                try {
                    URL url2 = new URL(uri);
                    URLConnection conn2 = url2.openConnection();
                    conn2.setRequestProperty("Accept", "text/n3");
                    conn2.setConnectTimeout(3000);
                    conn2.setReadTimeout(5000);
                    in = conn2.getInputStream();
                    try {
                        model.read(in, "N3");
                        return model;
                    } catch (Exception e3) {
                        return null;
                    }
                } catch (Exception e2) {
                    return null;
                }
            }
        } catch (Exception e) {
            return null;
        }

	}

    public double score(LabeledTriple triple1, LabeledTriple triple2) {
		// take q-grams over subject, predicate and object
		QGramsDistance distance = new QGramsDistance();
		double sVal = distance.getSimilarity(triple1.getSubjectLabel(),
                triple2.getSubjectLabel());
		double pVal = distance.getSimilarity(triple1.getPredicateLabel(),
				triple2.getPredicateLabel());
		double oVal = distance.getSimilarity(triple1.getObjectLabel(),
				triple2.getObjectLabel());
		return (sVal + pVal + oVal) / 3;
	}

    private int getHash(String value){
		int hash = 7;
		for (int i = 0; i < value.length(); i++) {
			hash = hash*31 + value.charAt(i);
		}
		return hash;
		//return value.hashCode();
	}

    private static boolean updateCacheAddingRelatedURIs(long uriHash, ArrayList<String> relatedTripleURIs)  {

		if (relatedTripleURIs == null || relatedTripleURIs.isEmpty()){
			return false;
		}
		try{
			SolrInputDocument doc = new SolrInputDocument();
			Map<String,ArrayList<String>> relatedURIUpdate = new HashMap<>();
			relatedURIUpdate.put("add", relatedTripleURIs);
			doc.addField("hash", uriHash);
			doc.addField("relatedTriplesIDs", relatedURIUpdate);
			server_uris.add(doc);
            server_uris.commit();
			return true;
		}catch (Exception e){
			logger.error("Error: " + e.toString());
			return false;
		}

	}

    /**
     * addURIToCache -> add given URI to the cache
     * 0 = error , 1 = existing URI, 2 = success
     */
    private static int addURIToCache(long hash, String uri, String urilabel, ArrayList<String> relatedTripleURIs) throws IOException, SolrServerException {

        try{

            if (!containsURI(hash)){

                SolrInputDocument doc = new SolrInputDocument();
                doc.addField("uri", uri);
                doc.addField("hash", hash);
                doc.addField("label", urilabel);
                doc.addField("relatedTriplesIDs", relatedTripleURIs);

                server_uris.add(doc);
                server_uris.commit();
                return 2;
            }else{
                return 1;
            }

        }catch (Exception e){
            return 0;
        }

	}

    private static SolrDocument getURIbyID(long hash) throws MalformedURLException, SolrServerException {
		SolrQuery query = new SolrQuery();
		query.setQuery("hash:" + hash);
		query.setFields("hash","label");
		query.setStart(0);
		query.set("defType", "edismax");

		QueryResponse response = server_uris.query(query);
		SolrDocumentList results = response.getResults();

		if (results != null && results.size() > 0){
			return results.get(0);
		} else {
			return null;
		}
	}

    private static SolrDocument getTripleByID(long id) throws MalformedURLException, SolrServerException {
        SolrQuery query = new SolrQuery();
        query.setQuery("id:" + id);
        query.setFields("s","p", "o");
        query.setStart(0);
        query.set("defType", "edismax");

        QueryResponse response = server_triples.query(query);
        SolrDocumentList results = response.getResults();

        if (results != null && results.size() > 0){
            return results.get(0);
        } else {
            return null;
        }
    }

    private static String getTripleIDBySPO(String s, String p, String o) throws MalformedURLException, SolrServerException {
        SolrQuery query = new SolrQuery();
        query.setQuery("s:" + s + " p:" + p + " o:" +o);
        query.setFields("id");
        query.setStart(0);
        query.set("defType", "edismax");

        QueryResponse response = server_triples.query(query);
        SolrDocumentList results = response.getResults();

        if (results != null && results.size() > 0){
            return results.get(0).getFieldValue("id").toString();
        } else {
            return null;
        }
    }

    private static boolean containsURI(long hash) throws MalformedURLException, SolrServerException {

		SolrQuery query = new SolrQuery();
		query.setQuery("hash:" + hash);
		query.setFields("uri","hash");
		query.setStart(0);
		query.set("defType", "edismax");

		QueryResponse response = server_uris.query(query);
		SolrDocumentList results = response.getResults();

		return results == null ? false : results.size() > 0 ? true : false;

	}

}
