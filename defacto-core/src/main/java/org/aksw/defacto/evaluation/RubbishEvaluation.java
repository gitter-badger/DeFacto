package org.aksw.defacto.evaluation;

/**
 * Created by dnes on 04/01/16.
 */

import org.aksw.defacto.Defacto;
import org.aksw.defacto.model.DefactoModel;
import org.aksw.defacto.model.PropertyConfiguration;
import org.aksw.defacto.model.PropertyConfigurationSingleton;
import org.aksw.defacto.reader.DefactoModelReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * A naive strategy to generate metadata for correct triples x changed ones
 */
public class RubbishEvaluation {

    private static final Logger LOGGER = LoggerFactory.getLogger(RubbishEvaluation.class);
    private static String getWhichPart = "";
    private static PrintWriter writer;

    public static void main(String[] args) throws Exception {

        Defacto.init();

        writer = new PrintWriter("rubbish.txt", "UTF-8");
        writer.println("score; model; subject; predicate; object; type");

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
        ArrayList<File> folders = new ArrayList<>();
        for(File f : files.listFiles()){
            if (f.isDirectory()){
                folders.add(f);}
        }

        //start the process for each property (folder)
        for(File currentFolder : folders){

                sizePropertyFolder = currentFolder.listFiles().length;
                LOGGER.info("Folder: " + currentFolder.getName() + " contains " + sizePropertyFolder + " models (files)");

                //add all the models which presents functional property then we can deal with exclusions
                models.addAll(DefactoModelReader.readModels(currentFolder.getAbsolutePath(), true, languages));

                //starting computation for each model
                for (int i=0; i<models.size(); i++){

                    //this folder will be used to collect new (random) resources
                    File randomFolder = getRandomProperty(folders, currentFolder);

                    //get inside the selected folder, N models
                    ArrayList<File> selectedFiles = getNRandomFiles(nrModelsToCompare, randomFolder);

                    //add all random selected models
                    for (int j=0; j<selectedFiles.size(); j++){
                        modelsRandom.add(DefactoModelReader.readModel(selectedFiles.get(j).getAbsolutePath()));
                    }

                    LOGGER.info("original model proccessed: " + models.get(i).getName());
                    //compute the score for main model
                    writer.println(Defacto.checkFact(models.get(i), Defacto.TIME_DISTRIBUTION_ONLY.NO).getDeFactoScore().toString() +
                            ";" + models.get(i).getName() +
                            ";" + models.get(i).getSubjectLabel("en") +
                            ";" + models.get(i).getPredicate().getLocalName() +
                            ";" + models.get(i).getObjectLabel("en") +
                            ";" + "original");

                    //compute the scores for aux models
                    for ( int m = 0; m < modelsRandom.size() ; m++ ) {

                        DefactoModel tempModel = models.get(i);
                        tempModel.setName("temp" + m);

                        if (getWhichPart.equals("S")){
                            tempModel.setSubject(modelsRandom.get(m).getSubject());
                        }else if (getWhichPart.equals("O")){
                            tempModel.setSubject(modelsRandom.get(m).getObject());
                        }else{
                            throw new Exception("It should not happens :/ getWhichPart = " + getWhichPart);
                        }

                        LOGGER.info("changed model proccessed: " + tempModel.getName());
                        //compute the score for main model
                        writer.println(
                                Defacto.checkFact(tempModel, Defacto.TIME_DISTRIBUTION_ONLY.NO).getDeFactoScore().toString() +
                                        ";" + tempModel.getName() +
                                        ";" + tempModel.getSubjectLabel("en") +
                                        ";" + tempModel.getPredicate().getLocalName() +
                                        ";" + tempModel.getObjectLabel("en") +
                                        ";" + "changed");

                    }

                    writer.flush();

                    //clear the selected files
                    modelsRandom.clear();
                    selectedFiles.clear();
            }


        }

        //Collections.shuffle(models, new Random(100));

        writer.close();
        LOGGER.info("Done!");

    }


    private static ArrayList<DefactoModel> getRandomModels(){

        ArrayList<DefactoModel> models = new ArrayList<>();

        return models;

    }

    /***
     * returns a random folder (property) of FactBench to be used as source to the new model generation process based on the structure of the provided model
      * @param current
     * @return
     */
    private static File getRandomProperty(ArrayList<File> folders, File current) throws Exception{
        File x = current;
        int randomM;
        boolean allowed = false;
        getWhichPart = "";
        int controller = 1;

        if (folders.size() > 1) {
            while ((x.equals(current)) && controller < folders.size()) {

                controller++;
                Random rand = new Random();
                randomM = rand.nextInt(folders.size() - 1);
                x = folders.get(randomM);
                //check whether current and chosen share the same configuration
                if (!PropertyConfigurationSingleton.getInstance().getConfigurations().containsKey(x.getName()))
                    throw new Exception("Configuration has not been found -> " + x.getName());

                if (!PropertyConfigurationSingleton.getInstance().getConfigurations().containsKey(current.getName()))
                    throw new Exception("Configuration has not been found -> " + current.getName());

                if (!current.getName().equals(x.getName())) {

                    PropertyConfiguration cCurrent =
                            PropertyConfigurationSingleton.getInstance().getConfigurations().get(current.getName());
                    PropertyConfiguration cRandom =
                            PropertyConfigurationSingleton.getInstance().getConfigurations().get(x.getName());

                    //checking constraints
                    if (cCurrent.getSubjectClass().equals(cRandom.getSubjectClass())) {
                        allowed = true;
                        getWhichPart = "S";
                    } else if (cCurrent.getSubjectClass().equals(cRandom.getObjectClass())) {
                        allowed = true;
                        getWhichPart = "O";
                    }
                    if (!allowed) {
                        x = current;
                    }
                }
            }
        }

        return x;
    }

    /***
     * get randomly n files inside a folder
     * @param n
     * @param selectedFolder
     * @return the selected files
     */
    private static ArrayList<File> getNRandomFiles(int n, File selectedFolder){

        Random rand = new Random();
        int size, aux, randomM;
        ArrayList<File> selected = new ArrayList(n);
        File[] files;

        try{
            aux = 0;

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
