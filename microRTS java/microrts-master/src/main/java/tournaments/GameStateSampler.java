package tournaments;

import ai.PassiveAI;
import ai.RandomAI;
import ai.RandomBiasedAI;
import ai.abstraction.HeavyRush;
import ai.abstraction.LightRush;
import ai.abstraction.RangedRush;
import ai.abstraction.WorkerRush;
import ai.abstraction.cRush.CRush_V1;
import ai.abstraction.partialobservability.POHeavyRush;
import ai.abstraction.partialobservability.POLightRush;
import ai.abstraction.partialobservability.PORangedRush;
import ai.abstraction.partialobservability.POWorkerRush;
import ai.core.AI;
import ai.minimax.RTMiniMax.IDRTMinimax;
import ai.montecarlo.MonteCarlo;
import ai.montecarlo.lsi.LSI;
import ai.portfolio.PortfolioAI;
import ai.portfolio.portfoliogreedysearch.PGSAI;
import ai.pvai.PVAIML_ED;
import gui.frontend.FEStatePane;
import rts.GameState;
import rts.PhysicalGameState;
import rts.units.UnitTypeTable;

import java.io.*;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class GameStateSampler {
    private static int frequency = 15;
    private static String rootPath = "D:\\Datasets\\AI-GT\\mixed_opponents";
    private static String path = Paths.get(rootPath, "manual_data").toString();
    private int counter;
    private int sample_id;

    public GameStateSampler() {
        Reset();
    }

    private void makedir(String dirname) {
        String fullPath = Paths.get(path, dirname).toString();
        File dir = new File(fullPath);

        if (!dir.exists()) {
            boolean result = dir.mkdir();
            if(result) System.out.println(String.format("%s created", fullPath));
            else System.out.println(String.format("failed to create dir %s", fullPath));
        }
    }

    public void Sample(String p1, String p2, String mapPath, int iteration, GameState gs) {
        counter++;
        if (counter == frequency) {
            makedir(p1);
            String mapName = mapPath.substring(mapPath.lastIndexOf('\\') + 1);
            String filename = String.format("%s_iter%d_%s_%d.json", mapName, iteration, p2, sample_id);
            String samplePath = Paths.get(path, p1, filename).toString();
            try (Writer writer = new BufferedWriter(
                    new OutputStreamWriter(
                            new FileOutputStream(samplePath), "utf-8"))) {
                gs.getPhysicalGameState().toJSON(writer);
                sample_id++;
            } catch(Exception e) {
                e.printStackTrace();
            }
            this.counter = 0;
        }
    }

    public void Reset() {
        this.counter = 0;
        this.sample_id = 0;
    }

    private static String getBooleanRepresentation(boolean b) {
        return b ? "true" : "false";
    }

    public static void main(String[] args) {
        try {
            // region constants
            String mapsDir = "D:\\_Guy\\Studies\\AI-GT\\Micro_RTS_WITH_CNN\\microrts-master\\maps\\24x24";
            int iterations = 1;
            int iterationsRandom = 5;
            int maxGameLength = 10000;
            int timeBudget = 100;
            int iterationsBudget = -1;
            int preAnalysisBudget = 1000;

            boolean fullObservability = true;
            boolean selfMatches = false;
            boolean timeOutCheck = true;
            boolean gcCheck = false;
            boolean preGameAnalysis = false;
            // endregion

            // region config
            UnitTypeTable utt = new UnitTypeTable(UnitTypeTable.VERSION_ORIGINAL);
            Class[] ais = {
                    WorkerRush.class,
                    LightRush.class,
                    HeavyRush.class,
                    RangedRush.class,
                    CRush_V1.class,
//                    POWorkerRush.class,
//                    POLightRush.class,
//                    POHeavyRush.class,
//                    PORangedRush.class,
                    PGSAI.class,
                    IDRTMinimax.class,
                    MonteCarlo.class,
                    PortfolioAI.class,
                    LSI.class,
                    PVAIML_ED.class
            };
            Class[] randoms = {
                    RandomAI.class,
                    RandomBiasedAI.class
            };
            File folder = new File(mapsDir);
            File[] listOfFiles = folder.listFiles();
            String[] mapsConfiguration = new String[listOfFiles.length];
            for (int i = 0; i < listOfFiles.length; i++) {
                mapsConfiguration[i] = listOfFiles[i].getPath();
                System.out.println(mapsConfiguration[i]);
            }
            // endregion

            // region write config file
            String formatString =
                    "frequency (sample every:) = %d\n" +
                    "mapsDir = %s\n" +
                    "iterations = %d\n" +
                    "iterationsRandom = %d\n" +
                    "maxGameLength = %d\n" +
                    "timeBudget = %d\n" +
                    "iterationsBudget = %d\n" +
                    "preAnalysisBudget = %d\n" +
                    "fullObservability = %s\n" +
                    "selfMatches = %s\n" +
                    "timeOutCheck = %s\n" +
                    "gcCheck = %s\n" +
                    "preGameAnalysis = %s\n";

            File confFile = Paths.get(rootPath, "conf.txt").toFile();
            confFile.delete();
            try (Writer writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(confFile.toString()), "utf-8"))) {
                writer.write(String.format(formatString,
                        frequency,
                        mapsDir,
                        iterations,
                        iterationsRandom,
                        maxGameLength,
                        timeBudget,
                        iterationsBudget,
                        preAnalysisBudget,
                        getBooleanRepresentation(fullObservability),
                        getBooleanRepresentation(selfMatches),
                        getBooleanRepresentation(timeOutCheck),
                        getBooleanRepresentation(gcCheck),
                        getBooleanRepresentation(preGameAnalysis)
                ));
                writer.write("Maps:\n");
                for (String map : mapsConfiguration)
                    writer.write(map + "\n");

                writer.write("AIs:\n");
                for (Class ai : ais)
                    writer.write(ai.getSimpleName() + "\n");

                writer.write("Random AIs:\n");
                for (Class ai : randoms)
                    writer.write(ai.getSimpleName() + "\n");

            } catch(Exception e) {
                e.printStackTrace();
            }
            // endregion

            List<AI> selectedAIs = new ArrayList<>();
            List<AI> opponentAIs = new ArrayList<>();
            List<AI> opponentAIsRandom = new ArrayList<>();
            List<String> maps = Arrays.asList(mapsConfiguration);

            for (Class c : ais) {
                Constructor cons = c.getConstructor(UnitTypeTable.class);
                selectedAIs.add((AI) cons.newInstance(utt));
                opponentAIs.add((AI) cons.newInstance(utt));
            }
            for (Class c : randoms) {
                Constructor cons = c.getConstructor(UnitTypeTable.class);
                opponentAIsRandom.add((AI) cons.newInstance(utt));
            }
            Writer writer = new PrintWriter(System.out);
            System.out.println("Creating (structured) dataset...");
            path = Paths.get(rootPath, "structured").toString();
//            FixedOpponentsTournament.runTournament(selectedAIs, opponentAIs, maps,
//                    iterations, maxGameLength, timeBudget, iterationsBudget, preAnalysisBudget,
//                    fullObservability, timeOutCheck, gcCheck, preGameAnalysis,
//                    utt, null, writer, writer);

            System.out.println("Creating (random) dataset...");
            path = Paths.get(rootPath, "random").toString();
            FixedOpponentsTournament.runTournament(selectedAIs, opponentAIsRandom, maps,
                    iterationsRandom, maxGameLength, timeBudget, iterationsBudget, preAnalysisBudget,
                    fullObservability, timeOutCheck, gcCheck, preGameAnalysis,
                    utt, null, writer, writer);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
