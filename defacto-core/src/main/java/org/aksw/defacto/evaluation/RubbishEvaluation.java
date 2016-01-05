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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

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
        List<DefactoModel> modelsRandom = new ArrayList<>();
        //the number of models to be computed
        int nrModelsToCompare = 4;
        //the number of models for each property (folder)
        int sizePropertyFolder = 0;

        String testDirectory = Defacto.DEFACTO_CONFIG.getStringSetting("eval", "data-directory")
                + Defacto.DEFACTO_CONFIG.getStringSetting("eval", "test-directory");

        File files = new File(testDirectory + "correct/");
        //getting all folders (properties)
        ArrayList<String> propertyFolders = new ArrayList<>();
        for(String propertyFolder : files.list()){
            File temp = new File(propertyFolder);
            if (temp.isDirectory()){
                propertyFolders.add(propertyFolder);}
        }

        //start the process for each property (folder)
        for(String propertyFolder : propertyFolders){
            File currentFolder = new File(propertyFolder);

                sizePropertyFolder = currentFolder.listFiles().length;
                LOGGER.info("Folder: " + propertyFolder + " contains " + sizePropertyFolder + " models (files)");

                //add all the models which presents functional property then we can deal with exclusions
                models.addAll(DefactoModelReader.readModels(currentFolder.getAbsolutePath(), true, languages));

                //starting computation for each model
                for (int i=0; i<models.size(); i++){

                    //this folder will be used to collect new (random) resources
                    String randomFolder = getRandomProperty(propertyFolders, currentFolder.getName());

                    //get inside the selected folder, N models
                    ArrayList<File> selectedFiles = getNRandomFiles(nrModelsToCompare, randomFolder);

                    //add all random selected models
                    for (int j=0; j<selectedFiles.size(); j++){
                        modelsRandom.add(DefactoModelReader.readModel(selectedFiles.get(j).getAbsolutePath()));
                    }

                    //compute the score for main model
                    LOGGER.info(
                            Defacto.checkFact(models.get(i), Defacto.TIME_DISTRIBUTION_ONLY.NO).getDeFactoScore().toString() +
                            ";" + models.get(i).getName() +
                            ";" + models.get(i).getSubjectLabel("en") +
                            ";" + models.get(i).getPredicate().getLocalName() +
                            ";" + models.get(i).getObjectLabel("en"));

                    //compute the scores for aux models
                    for ( int m = 0; m < modelsRandom.size() ; m++ ) {

                        DefactoModel tempModel = models.get(i);
                        tempModel.setName("temp" + m);
                        tempModel.setSubject(modelsRandom.get(m).getSubject());
                        //tempModel.setProperty(modelsRandom.get(m).getPredicate());
                        //tempModel.setObject(modelsRandom.get(m).getObject());

                        //compute the score for main model
                        LOGGER.info(
                                Defacto.checkFact(tempModel, Defacto.TIME_DISTRIBUTION_ONLY.NO).getDeFactoScore().toString() +
                                        ";" + tempModel.getName() +
                                        ";" + tempModel.getSubjectLabel("en") +
                                        ";" + tempModel.getPredicate().getLocalName() +
                                        ";" + tempModel.getObjectLabel("en"));
                    }

                    //clear the selected files
                    modelsRandom.clear();
                    selectedFiles.clear();
            }


        }
        
        //Collections.shuffle(models, new Random(100));

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
    private static String getRandomProperty(ArrayList<String> folders, String current){
        String x = current;
        int randomM = 0;
        while(x.equals(current)){
            Random rand = new Random();
            randomM = rand.nextInt(folders.size()-1);
            x = folders.get(randomM);
        }
        return x;
    }

    /***
     * get randomly n files inside a folder
     * @param n
     * @param folder
     * @return the selected files
     */
    private static ArrayList<File> getNRandomFiles(int n, String folder){

        Random rand = new Random();
        int size, aux, randomM;
        ArrayList<File> selected = new ArrayList(n);
        File[] files;

        try{
            aux = 0;
            File selectedFolder = new File(folder);

            files = selectedFolder.listFiles();
            size = files.length;

            while (aux<n){
                randomM = rand.nextInt(size);
                if (!selected.contains(files[randomM])){
                    aux++;
                    selected.add(files[randomM]);
                }
            }

        }catch (Exception e){
            LOGGER.error(e.toString());
        }
        return selected;
    }


}
