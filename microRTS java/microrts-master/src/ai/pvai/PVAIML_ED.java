package ai.pvai;

import ai.RandomBiasedAI;
import ai.abstraction.LightRush;
import ai.abstraction.RangedRush;
import ai.abstraction.WorkerRush;
import ai.abstraction.partialobservability.POHeavyRush;
import ai.abstraction.partialobservability.POLightRush;
import ai.abstraction.partialobservability.PORangedRush;
import ai.abstraction.partialobservability.POWorkerRush;
import ai.core.AI;
import ai.core.AIWithComputationBudget;
import ai.core.ParameterSpecification;
import ai.evaluation.SimpleSqrtEvaluationFunction3;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import rts.GameState;
import rts.PhysicalGameState;
import rts.Player;
import rts.PlayerAction;
import rts.UnitAction;
import rts.units.Unit;
import rts.units.UnitTypeTable;
import util.Pair;
//weka itens
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Attribute;
import weka.classifiers.lazy.IBk;
import weka.core.DenseInstance;
import weka.core.SerializationHelper;
import weka.core.converters.ConverterUtils;


public class PVAIML_ED extends AIWithComputationBudget {

    AI strategies[] = null;
    int playerForThisComputation;
    GameState gs_to_start_from = null;
    IBk rf = null;
    UnitTypeTable localUtt = null;
    Instances dataSet = null;
    long tempoInicial = 0;
    
    // This is the default constructor that microRTS will call
    public PVAIML_ED(UnitTypeTable utt) {

        this(new AI[]{new WorkerRush(utt),
            new LightRush(utt),
            new RangedRush(utt),
            new RandomBiasedAI()}, 100, -1, utt);
    }

    public PVAIML_ED(AI s[], int time, int max_playouts, UnitTypeTable utt) {
        super(time, max_playouts);
        strategies = s;
        localUtt = utt;
        loadModel();
    }

    @Override
    public void reset() {
    }

    @Override
    public PlayerAction getAction(int player, GameState gs) throws Exception {
        tempoInicial = System.currentTimeMillis();
        tryClassify(player, gs);
        if (gs.canExecuteAnyAction(player)) {
            startNewComputation(player, gs);

            return getBestActionSoFar();
        } else {
            return new PlayerAction();
        }

    }

    public void startNewComputation(int a_player, GameState gs) {
        playerForThisComputation = a_player;
        gs_to_start_from = gs;
    }

    public PlayerAction getBestActionSoFar() throws Exception {
        int slength = strategies.length;
        AI ai[] = new AI[slength];
        PlayerAction pa[] = new PlayerAction[slength];
        ArrayList<TreeMap<Long, UnitAction>> s = new ArrayList<>();

        for (int i = 0; i < slength; i++) {
            ai[i] = strategies[i].clone();            
            pa[i] = strategies[i].getAction(playerForThisComputation, gs_to_start_from);

        }

        PlayerAction pAux = pa[0];

        for (PlayerAction p : pa) {
            TreeMap<Long, UnitAction> sAux = new TreeMap<>();
            p.getActions().forEach((u) -> {
                sAux.put(u.m_a.getID(), u.m_b);
            });
            s.add(sAux);
        }

        PlayerAction resultado = new PlayerAction();
        ArrayList<UnitAction> vote = new ArrayList<>();
        TreeMap<UnitAction, Integer> contagem = new TreeMap<>(new Comparator<UnitAction>() {
            @Override
            public int compare(UnitAction u1, UnitAction u2) {
                if (u1.equals(u2)) {
                    return 0;
                } else {
                    return 1;
                }
            }
        });

        while (!s.get(0).isEmpty()) {
            s.forEach((ua) -> {
                vote.add(ua.get(ua.firstKey()));
            });
            Unit uAux = null;
            for (Pair<Unit, UnitAction> u : pAux.getActions()) {
                if (u.m_a.getID() == s.get(0).firstKey()) {
                    uAux = u.m_a;
                }
            }
            s.forEach((ua) -> {
                ua.remove(ua.firstKey());
            });

            vote.stream().map((valor) -> {
                if (!contagem.containsKey(valor)) {
                    contagem.put(valor, 0);
                }
                return valor;
            }).forEachOrdered((valor) -> {
                contagem.put(valor, contagem.get(valor) + 1);
            });
            vote.clear();

            Iterator<Map.Entry<UnitAction, Integer>> iterator = contagem.entrySet().iterator();
            Map.Entry<UnitAction, Integer> entry = iterator.next();
            Integer maior = entry.getValue();
            UnitAction action = entry.getKey();
            iterator.remove();
            while (iterator.hasNext()) {
                entry = iterator.next();
                Integer aux = entry.getValue();
                if (aux > maior) {
                    action = entry.getKey();
                    maior = aux;
                }
                iterator.remove();
            }

            resultado.addUnitAction(uAux, action);

        }

        long total = (System.currentTimeMillis() - tempoInicial);
        if(total >=100){
            System.out.println("Maior = " + total);
        }else if(total > 3){
            System.out.println(total);
        }     
        
        return resultado;
    }

    @Override
    public AI clone() {
        return new PVAIML_ED(strategies, TIME_BUDGET, ITERATIONS_BUDGET, localUtt);
    }

    @Override
    public List<ParameterSpecification> getParameters() {
        List<ParameterSpecification> parameters = new ArrayList<>();
        parameters.add(new ParameterSpecification("TimeBudget", int.class, 100));
        parameters.add(new ParameterSpecification("IterationsBudget", int.class, -1));
        return parameters;
    }

    protected void loadModel() {
        dataSet = null;
        try {
            rf = (IBk) SerializationHelper.read(getClass().getResourceAsStream("IBK_ED.model"));
            ConverterUtils.DataSource source = new ConverterUtils.DataSource(getClass().getResourceAsStream("dadosEnemyDistModelTemplate.arff"));
            dataSet = source.getDataSet();
            dataSet.setClassIndex(dataSet.numAttributes() - 1);

            Instance avai = new DenseInstance(10);
            avai.setDataset(dataSet);
            avai.setValue(0, 0);
            avai.setValue(1, 0);
            avai.setValue(2, 0);
            avai.setValue(3, 0);
            avai.setValue(4, 0);
            avai.setValue(5, 0);
            avai.setValue(6, 0);
            avai.setValue   (7, 8);
            avai.setValue(8, -1);
                double enemy = rf.classifyInstance(avai);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(PVAIML_ED.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Erro "+ex);
        } catch (Exception ex) {
            Logger.getLogger(PVAIML_ED.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Erro "+ex);
        }
    }

    protected void tryClassify(int player, GameState gs) {
        int playerEnemy = 0;
        if (player == 0) {
            playerEnemy = 1;
        }
        if (gs.getTime() % 100 == 0 && gs.getTime() != 0) {
            this.recordInfo(playerEnemy, player, gs, gs.getTime());
        } else if (gs.getTime() == 0) {
            PhysicalGameState pgs = gs.getPhysicalGameState();
            if (pgs.getHeight() == 8) {
                this.strategies = new AI[]{new WorkerRushPlusPlus(localUtt), 
                    new WorkerDefense(localUtt)};
            } else if (pgs.getHeight() == 16) {
                this.strategies = new AI[]{new WorkerRushPlusPlus(localUtt)};
            } else if (pgs.getHeight() == 24) {
                this.strategies = new AI[]{new WorkerRushPlusPlus(localUtt),
                    new WorkerDefense(localUtt),
                    new LightDefense(localUtt)};
            } else if (pgs.getHeight() == 32) {
                this.strategies = new AI[]{new WorkerRush(localUtt),
                    new LightRush(localUtt),
                    new WorkerDefense(localUtt),
                    new EconomyRush(localUtt)
                };
            } else if (pgs.getHeight() == 64) {
                this.strategies = new AI[]{//new POWorkerRush(localUtt),
                    new POLightRush(localUtt),
                    new EconomyRush(localUtt),
                    new WorkerDefense(localUtt)};
            }
        }
    }

    private void recordInfo(int playerEnemy, int player, GameState gs, int time) {

        PhysicalGameState pgs = gs.getPhysicalGameState();
        Player pEn = gs.getPlayer(playerEnemy);
        Player pA = gs.getPlayer(player);
        SimpleSqrtEvaluationFunction3 ef = new SimpleSqrtEvaluationFunction3();
        Unit base = null;
        int nWorkers = 0;
        int nBases = 0;
        int nBarracks = 0;
        int nRanged = 0;
        int nLight = 0;
        int nHeavy = 0;
        for (Unit u : pgs.getUnits()) {
            if (u.getType().name.equals("Base") && u.getPlayer() == player) {
                if (base == null) {
                    base = u;
                }
            }
            if (u.getType().name.equals("Base") && u.getPlayer() == playerEnemy) {
                ++nBases;
            }
            if (u.getType().name.equals("Barracks") && u.getPlayer() == playerEnemy) {
                ++nBarracks;
            }
            if (u.getType().name.equals("Worker") && u.getPlayer() == playerEnemy) {
                ++nWorkers;
            }
            if (u.getType().name.equals("Ranged") && u.getPlayer() == playerEnemy) {
                ++nRanged;
            }
            if (u.getType().name.equals("Light") && u.getPlayer() == playerEnemy) {
                ++nLight;
            }
            if (u.getType().name.equals("Heavy") && u.getPlayer() == playerEnemy) {
                ++nHeavy;
            }

        }
        Instance avai = new DenseInstance(10);
        avai.setDataset(dataSet);
        avai.setValue(0, nBases);
        avai.setValue(1, nBarracks);
        avai.setValue(2, nWorkers);
        avai.setValue(3, nLight);
        avai.setValue(4, nHeavy);
        avai.setValue(5, nRanged);
        avai.setValue(6, gs.getTime());
        avai.setValue(7, pgs.getWidth());
        if (base == null) {
            avai.setValue(8, -1);
        } else {
            avai.setValue(8, distUnitEneBase(base, pA, gs));
        }

        try {
            double enemy = rf.classifyInstance(avai);
            Attribute a = dataSet.attribute(dataSet.numAttributes() - 1);
            String nameEnemy = a.value((int) enemy);
            //System.out.println("Enemy name = " + nameEnemy);
            setNewStrategy(getStrategyByEnemy(nameEnemy, pgs.getHeight()));

        } catch (Exception ex) {
            Logger.getLogger(PVAIML_ED.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Erro na classificação="+ex);
        }
    }

    public int distUnitEneBase(Unit base, Player p, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        Unit closestEnemy = null;
        int closestDistance = 0;
        for (Unit u2 : pgs.getUnits()) {
            if (u2.getPlayer() >= 0 && u2.getPlayer() != p.getID()) {
                int d = Math.abs(u2.getX() - base.getX()) + Math.abs(u2.getY() - base.getY());
                if (closestEnemy == null || d < closestDistance) {
                    closestEnemy = u2;
                    closestDistance = d;
                }
            }
        }
        return closestDistance;
    }

    protected String getStrategyByEnemy(String enemy, int mapSnapshot) {
        String strategy = "";

        //mapa 8
        if (enemy.equals("PuppetSearchMCTS") && mapSnapshot == 8) {
            return "WorkerRushPlusPlusLightDefenseEconomyMilitaryRush";
        }
        if (enemy.equals("PORangedRush") && mapSnapshot == 8) {
            return "LightDefenseRangedDefenseEconomyMilitaryRush";
        }
        if (enemy.equals("POLightRush") && mapSnapshot == 8) {
            return "WorkerDefenseEconomyMilitaryRush";
        }
        if (enemy.equals("NaiveMCTS") && mapSnapshot == 8) {
            return "WorkerRushPlusPlusLightDefense";
        }
        if (enemy.equals("RandomBiasedAI") && mapSnapshot == 8) {
            return "WorkerRushPlusPlusEconomyRushLightDefense";
        }
        if (enemy.equals("POHeavyRush") && mapSnapshot == 8) {
            return "WorkerRushPlusPlusEconomyRushLightDefenseEconomyMilitaryRush";
        }
        if (enemy.equals("PortfolioAI") && mapSnapshot == 8) {
            return "WorkerRushPlusPlusEconomyMilitaryRush";
        }
        if (enemy.equals("EconomyRush") && mapSnapshot == 8) {
            return "WorkerRushPlusPlusPORangedRushLightDefense";
        }
        if (enemy.equals("POWorkerRush") && mapSnapshot == 8) {
            return "WorkerRushPlusPlusEconomyMilitaryRush";
        }
        //mapa 16
        if (enemy.equals("POLightRush") && mapSnapshot == 16) {
            return "WorkerRushPlusPlusEconomyRush";
        }
        if (enemy.equals("EconomyRush") && mapSnapshot == 16) {
            return "WorkerRushPlusPlusWorkerDefense";
        }
        if (enemy.equals("PORangedRush") && mapSnapshot == 16) {
            return "WorkerRushPlusPlusEconomyRushRangedDefense";
        }
        if (enemy.equals("PortfolioAI") && mapSnapshot == 16) {
            return "WorkerRushPlusPlusEconomyMilitaryRush";
        }
        if (enemy.equals("POWorkerRush") && mapSnapshot == 16) {
            return "WorkerRushPlusPlusPORangedRushWorkerDefense";
        }
        if (enemy.equals("NaiveMCTS") && mapSnapshot == 16) {
            return "WorkerRushPlusPlusPORangedRushLightDefense";
        }
        if (enemy.equals("POHeavyRush") && mapSnapshot == 16) {
            return "WorkerRushPlusPlusEconomyRushWorkerDefense";
        }
        if (enemy.equals("PuppetSearchMCTS") && mapSnapshot == 16) {
            return "WorkerRushPlusPlusLightDefenseWorkerDefense";
        }
        if (enemy.equals("RandomBiasedAI") && mapSnapshot == 16) {
            return "WorkerRushPlusPlusPOLightRushLightDefenseWorkerDefense";
        }
        //mapa 24
        if (enemy.equals("POLightRush") && mapSnapshot == 24) {
            return "WorkerRushPlusPlusEconomyRush";
        }
        if (enemy.equals("PuppetSearchMCTS") && mapSnapshot == 24) {
            return "WorkerRushPlusPlusEconomyRush";
        }
        if (enemy.equals("PortfolioAI") && mapSnapshot == 24) {
            return "WorkerRushPlusPlusPORangedRushWorkerDefense";
        }
        if (enemy.equals("PORangedRush") && mapSnapshot == 24) {
            return "PORangedRushWorkerDefenseEconomyMilitaryRush";
        }
        if (enemy.equals("POHeavyRush") && mapSnapshot == 24) {
            return "WorkerRushPlusPlusEconomyMilitaryRush";
        }
        if (enemy.equals("POWorkerRush") && mapSnapshot == 24) {
            return "RangedDefenseWorkerDefenseEconomyMilitaryRush";
        }
        if (enemy.equals("NaiveMCTS") && mapSnapshot == 24) {
            return "WorkerRushPlusPlusPORangedRushWorkerDefense";
        }
        if (enemy.equals("RandomBiasedAI") && mapSnapshot == 24) {
            return "WorkerRushPlusPlusLightDefenseRangedDefense";
        }
        if (enemy.equals("EconomyRush") && mapSnapshot == 24) {
            return "WorkerRushPlusPlusPOHeavyRush";
        }
        //mapas 32
        if (enemy.equals("RandomBiasedAI") && mapSnapshot == 32) {
            return "POLightRushPORangedRushWorkerDefenseEconomyMilitaryRush";
        }
        if (enemy.equals("PuppetSearchMCTS") && mapSnapshot == 32) {
            return "WorkerRushPlusPlusPOLightRushEconomyMilitaryRush";
        }
        if (enemy.equals("PortfolioAI") && mapSnapshot == 32) {
            return "EconomyRushPORangedRushRangedDefenseEconomyMilitaryRush";
        }
        if (enemy.equals("POHeavyRush") && mapSnapshot == 32) {
            return "PORangedRushWorkerDefenseEconomyMilitaryRush";
        }
        if (enemy.equals("POLightRush") && mapSnapshot == 32) {
            return "POLightRushEconomyRushEconomyMilitaryRush";
        }
        if (enemy.equals("POWorkerRush") && mapSnapshot == 32) {
            return "POLightRushEconomyMilitaryRush";
        }
        if (enemy.equals("EconomyRush") && mapSnapshot == 32) {
            return "POLightRushEconomyRushEconomyMilitaryRush";
        }
        if (enemy.equals("PORangedRush") && mapSnapshot == 32) {
            return "POHeavyRushPORangedRushLightDefenseEconomyMilitaryRush";
        }
        if (enemy.equals("NaiveMCTS") && mapSnapshot == 32) {
            return "EconomyRushPORangedRushRangedDefenseEconomyMilitaryRush";
        }
        //mapas 64
        if (enemy.equals("PORangedRush") && mapSnapshot == 64) {
            return "POHeavyRushRangedDefenseWorkerDefense";
        }
        if (enemy.equals("NaiveMCTS") && mapSnapshot == 64) {
            return "PORangedRushLightDefenseRangedDefenseEconomyMilitaryRush";
        }
        if (enemy.equals("POWorkerRush") && mapSnapshot == 64) {
            return "LightDefenseRangedDefenseWorkerDefense";
        }
        if (enemy.equals("POLightRush") && mapSnapshot == 64) {
            return "POLightRushLightDefenseRangedDefense";
        }
        if (enemy.equals("PortfolioAI") && mapSnapshot == 64) {
            return "PORangedRushLightDefenseRangedDefenseEconomyMilitaryRush";
        }
        if (enemy.equals("RandomBiasedAI") && mapSnapshot == 64) {
            return "WorkerRushPlusPlusPOLightRushEconomyRushWorkerDefense";
        }
        if (enemy.equals("PuppetSearchMCTS") && mapSnapshot == 64) {
            return "POLightRushEconomyRushWorkerDefense";
        }
        if (enemy.equals("EconomyRush") && mapSnapshot == 64) {
            return "EconomyRushPOHeavyRushRangedDefenseEconomyMilitaryRush";
        }
        if (enemy.equals("POHeavyRush") && mapSnapshot == 64) {
            return "LightDefenseWorkerDefenseEconomyMilitaryRush";
        }

        return strategy;
    }

    protected void setNewStrategy(String BagStrategy) {
        ArrayList<AI> newStrat = new ArrayList<>();

        if (BagStrategy.contains("POWorkerRush")) {
            newStrat.add(new POWorkerRush(localUtt));
        }
        if (BagStrategy.contains("WorkerRushPlusPlus")) {
            newStrat.add(new WorkerRushPlusPlus(localUtt));
        }
        if (BagStrategy.contains("POLightRush")) {
            newStrat.add(new POLightRush(localUtt));
        }
        if (BagStrategy.contains("EconomyRush")) {
            newStrat.add(new EconomyRush(localUtt));
        }
        if (BagStrategy.contains("RandomBiasedAI")) {
            newStrat.add(new RandomBiasedAI(localUtt));
        }
        if (BagStrategy.contains("POHeavyRush")) {
            newStrat.add(new POHeavyRush(localUtt));
        }
        if (BagStrategy.contains("PORangedRush")) {
            newStrat.add(new PORangedRush(localUtt));
        }
        if (BagStrategy.contains("LightDefense")) {
            newStrat.add(new LightDefense(localUtt));
        }
        if (BagStrategy.contains("RangedDefense")) {
            newStrat.add(new RangedDefense(localUtt));
        }
        if (BagStrategy.contains("WorkerDefense")) {
            newStrat.add(new WorkerDefense(localUtt));
        }
        if (BagStrategy.contains("EconomyMilitaryRush")) {
            newStrat.add(new EconomyMilitaryRush(localUtt));
        }

        this.strategies = new AI[newStrat.size()];
        for (int i = 0; i < newStrat.size(); i++) {
            this.strategies[i] = newStrat.get(i);
        }
        //System.out.println("Estratégias : " + newStrat);
    }
}
