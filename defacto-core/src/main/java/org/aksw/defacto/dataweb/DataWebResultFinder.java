package org.aksw.defacto.dataweb;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.*;
import java.util.*;
import java.util.Map.Entry;

import com.hp.hpl.jena.rdf.model.*;
import org.aksw.defacto.util.LabeledTriple;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
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
 * TODO make  this a feature and move it to the correct package
 * TODO delete InformationFinder package
 */
public class DataWebResultFinder {

	private static Logger logger = Logger.getLogger(DataWebResultFinder.class);
	private int numberOfTriples = 0;
	private int numberOfRelevantTriples = 0;
	Set<String> relevantTriples = new TreeSet<String>();
	Set<String> distinctResources = new TreeSet<String>();
	private int labelCalls = 0;
	private SameAsService service;
	private static ArrayList<String> blackList = new ArrayList<>();
	private int maximumEquivalentURIs = 10;

	private static Set<String> notAllowedProperties = new HashSet<String>();
	static {
		notAllowedProperties.add("http://www.w3.org/2002/07/owl#sameAs");
		notAllowedProperties.add("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
	}
	private Map<String, String> labelCache = new TreeMap<String, String>();

	public static void main(String args[]) throws SameAsServiceException {

		blackList.add("http://www.econbiz.de/");

		try {

			//saveDocument();
			//queryDocument();



			//Model read = ModelFactory.createDefaultModel().read("http://www.globo.com");
			//StmtIterator si;
			//si = read.listStatements();
			//while (si.hasNext()) {
			//	Statement s=si.nextStatement();
			//	Resource r=s.getSubject();
			//	Property p=s.getPredicate();
			//	RDFNode o=s.getObject();
			//	System.out.println(r.getURI());
			//	System.out.println(p.getURI());
			//	System.out.println(o.asResource().getURI());
			//}

		}
		catch(Exception e) {
			System.out.print(e.toString());
		}





		DataWebResultFinder finder = new DataWebResultFinder();

		// test triple
		LabeledTriple ltriple = new LabeledTriple(
				"http://dbpedia.org/resource/Berlin", "Berlin",
				"http://dbpedia.org/ontology/leader", "leader",
				"http://dbpedia.org/resource/Klaus_Wowereit", "Klaus Wowereit");

		Map<LabeledTriple, Double> similarTriples = finder.findSimilarTriples(ltriple, 0.0);

		// Ordering<LabeledTriple> valueComparator =
		// Ordering.natural().onResultOf(Functions.forMap(similarTriples));
		// Map<LabeledTriple,Double> sortedMap =
		// ImmutableSortedMap.copyOf(similarTriples, valueComparator);

		for (Entry<LabeledTriple, Double> entry : similarTriples.entrySet()) {
			if (entry.getValue() > 0.0) {
				System.out.println(entry.getValue() + " " + entry.getKey());
			}
		}

	}

	public Map<LabeledTriple, Double> findSimilarTriples(LabeledTriple inputTriple, double threshold) throws SameAsServiceException {

		service = DefaultSameAsServiceFactory.getSingletonInstance();
		service.setCache(new InMemoryCache());
		String _uri = inputTriple.getSubjectURI();

		Equivalence equivalence = service.getDuplicates(URI.create(_uri));
		System.out.println( "Number of equivalent URIs for [" + _uri + "] : " + equivalence.getAmount());

		long startTime = System.currentTimeMillis();
		// retrieve Linked Data from all sources
		// List<LabeledTriple> ltriples = new LinkedList<LabeledTriple>();
		// switched to set, because for some reason we need to filter duplicates
		// for hash URIs
		Set<LabeledTriple> ltriples = new TreeSet<>();

		//System.out.println("total: " + equivalence.getAmount());
		int i = 0;
		outerloop:
		for (URI uri : equivalence) {
			if (i==maximumEquivalentURIs){
				System.out.println("Breaking");
				break outerloop;
			}

			if (!blackList.contains(uri.toString())){
				ltriples.addAll(readLinkedDataLabeled(uri.toString()));
				i++;
			}
			//if (!uri.toString().startsWith("http://www.econbiz.de/")) // not
																		// able
																		// to
																		// provide
																		// proper
																		// rdf
				//ltriples.addAll(readLinkedDataLabeled(uri.toString()));
				//i++;
		}

		long duration = System.currentTimeMillis() - startTime;

		System.out.println("number of all triples: " + numberOfTriples);
		System.out.println("number of relevant triples: " + numberOfRelevantTriples);
		System.out.println("number of distinct relevant triples: " + relevantTriples.size());
		System.out.println("number of distinct resources: " + distinctResources.size());
		System.out.println("Linked Data label calls: " + labelCalls);
		System.out.println("time spend for retrieving resources: " + duration + " ms");

		// score all triples, which we have obtained
		System.out.println("starting scoring " + ltriples.size() + " triples...");
		Map<LabeledTriple, Double> simTriples = new HashMap<>();
		for (LabeledTriple t : ltriples) {
			simTriples.put(t, score(inputTriple, t));
		}
		return simTriples;
	}

	public List<LabeledTriple> readLinkedDataLabeled(String uri) {
		logger.info("Reading all statements from " + uri + ".");
		String subjectLabel = getURILabel(uri);

		System.out.println("subject label = " + subjectLabel);



		List<LabeledTriple> ltriples = new LinkedList<>();
		// retrieve all statements from the URI
		HolderURI<String> ret = new HolderURI<>("");
		Model m = readLinkedData(uri, ret);
		if (m == null) {
			return ltriples;
		}
		numberOfTriples += m.size();
		System.out.println("returned triples: " + m.size());
		StmtIterator it = m.listStatements();
		int i = 0;
		while (it.hasNext()) {
			Statement st = it.next();
			// // currently, we are only looking at object properties, so we
			// // assume only object properties can be similar;
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
			// !st.getObject().asResource().getURI().startsWith("http://www.econbiz.de/")
			// &&
			// !st.getSubject().getURI().startsWith("http://www.econbiz.de/"))
			) {
				i++;
				System.out.println(st.getSubject().getNameSpace() + "+++"
						+ st.getPredicate().toString() + "+++"
						+ st.getObject().toString());
				// quick hack
				boolean test = relevantTriples.add(st.getSubject()
						.getNameSpace()
						+ "+++"
						+ st.getPredicate().toString()
						+ "+++" + st.getObject().toString());
				if (test) {
					distinctResources.add(st.getSubject().getURI().toString());
					distinctResources.add(st.getObject().asNode().getURI().toString());
					distinctResources.add(st.getPredicate().getURI().toString());
				}
				numberOfRelevantTriples++;
				// System.out.println(st);
				String predicateLabel = getURILabel(st.getPredicate().getURI());
				System.out.println("predicate label = " + predicateLabel);

				String objectLabel = getURILabel(st.getObject().asResource().getURI());
				System.out.println("object label = " + objectLabel);

				LabeledTriple lt = new LabeledTriple(
						st.getSubject().getURI(), subjectLabel,
						st.getPredicate().getURI(), predicateLabel,
						st.getObject().asResource().getURI(), objectLabel);
				ltriples.add(lt);
			}
		}
		System.out.println("possibly relevant triples: " + i);
		return ltriples;
	}

	// retrieves the label from a resource via Linked Data (TODO: that should be
	// cached,
	// otherwise we end up doing very many Linked Data calls)
	public String getURILabel(String uri) {

		logger.info("Getting label of " + uri + ".");

		String retorno = "";

		// check whether we already have the label in our cache
		String lc = labelCache.get(uri);
		if (lc != null) {
			logger.info("Cache hit.");
			return lc;
		}
		labelCalls++;

		HolderURI<String> ret = new HolderURI<>("");
		Model m = readLinkedData(uri, ret);
		if (m == null) {
			// use local name if URI cannot be dereferenced
			retorno = ModelFactory.createDefaultModel().createResource(uri).getLocalName();
		}
		Property prefLabel = m.createProperty("http://www.w3.org/2004/02/skos/core#prefLabel");
		Statement st = m.getResource(uri).getProperty(prefLabel);
		if (st != null) {
			retorno =  st.getObject().toString();
		}
		st = m.getResource(uri).getProperty(RDFS.label);
		// System.out.println(st);
		if (st != null) {
			retorno =  st.getObject().toString();
		} else {
			// the fallback is to create the label from the URI name
			retorno =  m.createResource(uri).getLocalName();
		}

		labelCache.put(uri, retorno);
		return retorno;
	}

	// TODO: because we get all kinds of errors in Linked Data, this method
	// needs to be extended
	// quite a bit - maybe even an own project or using any23 as heavy-weight
	// dependency
	public Model readLinkedData(String uri, HolderURI<String> ret) {

		boolean fail = false;
		Model model = ModelFactory.createDefaultModel();
		URL url;
		InputStream in = null;

		try{
			SolrDocument d = getDocumentByID(getHash(uri));
			if (d != null) {
				ret.set(uri);
				model.read(d.toString());
			}
			else{

				/*
				try {
					URL url = new URL(uri);
					HttpURLConnection connection = (HttpURLConnection) url.openConnection();
					connection.setRequestMethod("HEAD");
					connection.connect();
					String contentType = connection.getContentType();
					System.out.println("the content type = " + contentType);
				}
				catch (Exception e){

				}
				*/

				// replaced Jena by own implementation to have timeouts

				try {
					url = new URL(uri);
					URLConnection conn = url.openConnection();
					conn.setRequestProperty("Accept", "application/rdf+xml");
					conn.setConnectTimeout(3000);
					conn.setReadTimeout(5000);
					in = conn.getInputStream();

					try {
						model.read(in, "RDF/XML");
					} catch (Exception e) {
						e.printStackTrace();
						try {
							URL url2 = new URL(uri);
							URLConnection conn2 = url2.openConnection();
							conn2.setRequestProperty("Accept", "text/n3");
							conn2.setConnectTimeout(3000);
							conn2.setReadTimeout(5000);
							in = conn2.getInputStream();
							try {
								model.read(in, "N3");
							} catch (Exception e3) {
								e3.printStackTrace();
								fail=true;
							}
						} catch (Exception e2) {
							fail = true;
							e2.printStackTrace();
						}

					}

					//if (!fail) {
					//	saveDocument(getHash(uri), in.toString());
					//}

				} catch (Exception e) {
					e.printStackTrace();
				}

			}

		}catch (Exception e){
			e.printStackTrace();
		}


		// JenaReader jr = new JenaReader();
		// jr.read(model, uri);
		// } catch(JenaException e) {
		// model = ModelFactory.createDefaultModel();

		// }
		return model;


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
	
    private String inputStreamToString(InputStream in) {
	    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in));
	    StringBuilder stringBuilder = new StringBuilder();
	    String line = null;
	
	    try {
			while ((line = bufferedReader.readLine()) != null) {
			stringBuilder.append(line + "\n");
			}
	    bufferedReader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	    
	    return stringBuilder.toString();
    }

	private static boolean saveDocument(int id, String content) throws IOException, SolrServerException {
		HttpSolrServer server = new HttpSolrServer("http://localhost:8123/solr/ld_sources");
		//for(int i=0;i<1000;++i) {
			SolrInputDocument doc = new SolrInputDocument();
			//doc.addField("id", "book-" + i);
		doc.addField("id", id);
			//doc.addField("text", "sakjdhaskjdhjkasdas " + i);
		doc.addField("text", content);
			server.add(doc);
			//if(i%100==0)
			//	server.commit();  // periodically flush
		//}
		server.commit();
		return true;
	}
	private static SolrDocument getDocumentByID(int id) throws MalformedURLException, SolrServerException {
		HttpSolrServer solr = new HttpSolrServer("http://localhost:8123/solr/ld_sources");

		SolrQuery query = new SolrQuery();
		query.setQuery("id:" + id);
		query.setFields("id","text");
		query.setStart(0);
		query.set("defType", "edismax");

		QueryResponse response = solr.query(query);
		SolrDocumentList results = response.getResults();

		if (results != null && results.size() > 0){
			return results.get(0);
		}else
		{
			return null;
		}

	}
	private static boolean contains(String id) throws MalformedURLException, SolrServerException {

		HttpSolrServer solr = new HttpSolrServer("http://localhost:8123/solr/ld_sources");

		SolrQuery query = new SolrQuery();
		query.setQuery("id:" + id);
		query.setFields("id","text");
		query.setStart(0);
		query.set("defType", "edismax");

		QueryResponse response = solr.query(query);
		SolrDocumentList results = response.getResults();

		return results == null ? false : results.size() > 0 ? true : false;



		//for (int i = 0; i < results.size(); ++i) {
		//	System.out.println(results.get(i));
		//}


	}

}
