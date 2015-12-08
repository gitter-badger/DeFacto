package org.aksw.defacto.nlp.ner;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;

/**
 * Created by dnes on 08/12/15.
 */
public class FOXNamedEntityRecognition {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(StanfordNLPNamedEntityRecognition.class);


    public static void main(String[] args) {

        String test = "This is a year 1872 this is not a year 345 this is also not a year 3323 but his 1293 2013";
        FOXNamedEntityRecognition t  = new FOXNamedEntityRecognition();
        System.out.println(t.getAnnotatedSenteces(test));
    }


    public FOXNamedEntityRecognition(){

    }



    public String getAnnotatedSenteces(String s){


        try{


            /*
            input : text or an url (e.g.: `G. W. Leibniz was born in Leipzig`, `http://en.wikipedia.org/wiki/Leipzig_University`)
            type : { text | url }
            task : { NER }
            output : { JSON-LD | N-Triples | RDF/{ JSON | XML } | Turtle | TriG | N-Quads}
             */

            //String url = "http://" + server.host + ":" + server.port;

            String url = "http://fox-demo.aksw.org/api";
            final Charset UTF_8 = Charset.forName("UTF-8");
            JSONObject jo = new JSONObject()
                    .put("type", "text")
                    .put("task", "NER")
                    .put("output", "Turtle")
                    .put("input", s);


            LOG.debug("Parameter: " + jo);


            Response response = Request
                    .Post(url.concat("/call/ner/entities"))
                    .addHeader("Content-type", "application/json;charset=".concat(UTF_8.toString()))
                    .addHeader("Accept-Charset", UTF_8.toString())
                    .body(
                            new StringEntity(
                                    jo.toString(), ContentType.APPLICATION_JSON
                            )
                    )
                    .execute();

            HttpResponse httpResponse = response.returnResponse();
            LOG.debug(httpResponse.getStatusLine().toString());

            HttpEntity entry = httpResponse.getEntity();
            String r = IOUtils.toString(entry.getContent(), UTF_8);
            EntityUtils.consume(entry);

            LOG.debug(r);

            return r;

        }catch (Exception e){
            return "";
        }


    }

}
