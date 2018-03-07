//package tournaments;
//
//import ai.deeplearning.Convnet;
//import org.nd4j.linalg.api.ndarray.INDArray;
//import org.nd4j.linalg.factory.Nd4j;
//import org.nd4j.linalg.util.ArrayUtil;
//
//import java.io.BufferedReader;
//import java.io.FileReader;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Iterator;
//import java.util.List;
//
///**
// * Created by nitzanfa on 07/02/2018.
// */
//public class ParserDataSetIterator implements Iterator<Convnet.TripleINDArray > {
//    private static String[] readLines(String filename) throws IOException {
//        FileReader fileReader = new FileReader(filename);
//        BufferedReader bufferedReader = new BufferedReader(fileReader);
//        List<String> lines = new ArrayList<String>();
//        String line = null;
//        while ((line = bufferedReader.readLine()) != null) {
//            lines.add(line);
//        }
//        bufferedReader.close();
//        return lines.toArray(new String[lines.size()]);
//    }
//
//    public static void parser(String filename) throws IOException {
//        String arr[] = readLines(filename);
//        String[] pref = Arrays.copyOfRange(arr,0,4);
//        int sizeX = Integer.parseInt(pref[0].split("=")[1]);
//        int sizeY = Integer.parseInt(pref[1].split("=")[1]);
//        int depth = Integer.parseInt(pref[2].split("=")[1]);
//        int numOfSamples = Integer.parseInt(pref[3].split("=")[1]);
//
//        int start = 4;
//        int end = 4 + 2 +  32*32;
//        numOfSamples = 1;
//        for(int sample = 0 ; sample < numOfSamples ; sample++ )
//        {
//            String[] dataSample = Arrays.copyOfRange(arr,start,start+2);
//            String player1 = dataSample[0].split("=")[1];
//            String player2 = dataSample[1].split("=")[1];
//            System.out.println(player1);
//            System.out.println(player2);
//            String temp;
//            String[] dataSampleWithoutPlayers = Arrays.copyOfRange(arr,start+2,end);
//
//            double[][] myDoubleArray = new double[32][32];
//            for(int i = 0; i < 32 ; i ++)
//                for(int j = 0 ; j < 32 ; j ++) {
//                    temp = dataSampleWithoutPlayers[i * 32 + j];
//                    myDoubleArray[i][j]= string2Double(temp);
//                }
//            double[] flat = ArrayUtil.flattenDoubleArray(myDoubleArray);
//            int[] shape = {32,32,11};
//            INDArray myArr = Nd4j.create(flat,shape,'c');
//            Convnet.TripleINDArray res = new Convnet.TripleINDArray();
//            res.arr=myArr;
//            res.player1=player1;
//            res.player2=player2;
//            lst.add(res);
//            start = end;
//            end = end + 2 + 32*32;
//
//        }
//
//    }
//
//    private static double string2Double(String temp) {
//        return Integer.parseInt(temp);
//        //todo
//    }
//
//
//    public static void main(String[] args) throws IOException {
//        parser("DataSet.txt");
//    }
//
//
//    static ArrayList<Convnet.TripleINDArray> lst = new ArrayList<Convnet.TripleINDArray>();
//    static int current = 0;
//    @Override
//    public boolean hasNext() {
//        return current < lst.size();
//    }
//
//    @Override
//    public Convnet.TripleINDArray next() {
//        Convnet.TripleINDArray temp =  lst.get(current);
//        current++;
//        return temp;
//    }
//}
//
