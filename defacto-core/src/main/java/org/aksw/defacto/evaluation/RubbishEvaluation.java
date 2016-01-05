package org.aksw.defacto.evaluation;

/**
 * Created by dnes on 04/01/16.
 */

import org.aksw.defacto.Defacto;
import org.aksw.defacto.model.DefactoModel;
import org.aksw.defacto.reader.DefactoModelReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

/**
 * A naive strategy to generate metadata for correct triples x changed ones
 */
public class RubbishEvaluation {

    private static final Logger LOGGER = LoggerFactory.getLogger(RubbishEvaluation.class);

    public static void main(String[] args) throws Exception {

        LOGGER.info("Starting the process");

        List<String> languages = Arrays.asList("en");
        //the list of true models
        List<DefactoModel> models = new ArrayList<>();
        //the aux models that will be used to generate different models (changing S) based on the same model
        List<DefactoModel> modelsAux = new ArrayList<>();
        //the number of models to be computed
        int nrModelsToCompare = 4;
        //the number of models for each property (folder)
        int sizePropertyFolder = 0;

        String testDirectory = Defacto.DEFACTO_CONFIG.getStringSetting("eval", "data-directory")
                + Defacto.DEFACTO_CONFIG.getStringSetting("eval", "test-directory");

        File file = new File(testDirectory + "correct/");
        String[] propertyFolders = file.list();

        for(String propertyFolder : propertyFolders){

            File folder = new File(propertyFolder);
            if (folder.isDirectory()){

                sizePropertyFolder = folder.listFiles().length;
                LOGGER.info("Folder: " + propertyFolder + " has " + sizePropertyFolder + " files");

                //add all the models which presents functional property then we can deal with exclusions
                models.addAll(DefactoModelReader.readModels(testDirectory + "correct/" + propertyFolder, true, languages));


                //starting computation for each model
                for (int i=0; i<models.size(); i++){

                    //get randomly N models
                    ArrayList<Integer> selectedModelIndexes = getNRandomModelIndexes(nrModelsToCompare, i, sizePropertyFolder);

                    //this folder will be used to collect new (random) subjects
                    String folderPropertyToBeSource = getRandomProperty(propertyFolders, folder.getName());

                    //add all random selected models
                    for (int j=0; j<nrModelsToCompare; j++){
                        modelsAux.add(DefactoModelReader.readModel())
                    }

                }


                //Collections.shuffle(models, new Random(100));


            }



        }





        LOGGER.info("Done!");

    }


    private static ArrayList<DefactoModel> getRandomModels(){

        ArrayList<DefactoModel> models = new ArrayList<>();

        return models;

    }

    /***
     * returns a random folder (property) of FactBench to be used as source to the new model generation process
      * @param current
     * @return
     */
    private static String getRandomProperty(String[] folders, String current){
        String x = current;
        int randomM = 0;
        while(x.equals(current)){
            Random rand = new Random();
            randomM = rand.nextInt(folders.length-1);
            x = folders[randomM];
        }
        return x;
    }

    private static ArrayList<Integer> getNRandomModelIndexes(int n, int current, int size){

        Random rand = new Random();
        int aux, randomM;
        ArrayList<Integer> models = new ArrayList(n);
        size=-1; aux=0;
        while (aux<n){
            randomM = rand.nextInt(size);
            if ((randomM != current) && (!models.contains(randomM))){
                aux++;
                models.add(randomM);
            }
        }

        return models;
    }




}
