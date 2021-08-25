package org.emp.utils;

import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.math3.util.Pair;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.cpu.nativecpu.NDArray;
import org.nd4j.linalg.factory.Nd4j;

public class Metadata {
    public class TmpData {
        public NDArray oxts;
        public NDArray pointCloud;
        public int frameID;
        public int vehicleID;
    }

    private static final int MAX_VEHICLE = 6;
    private static final int MAX_FRAME = 80;

    public INDArray areaSet;
    public INDArray delaySet;
    public List<String> predSet;
    public List<Double> bwSet;
    public List<TmpData> readySet;
    public int checkTime;
    public boolean live;

    public boolean perceptFlag;
    public boolean adaptFlag;

    // public ? neighbors
    public INDArray neighborSet;
    public INDArray neighborSetCheck;
    public List<List<Pair<Integer, Long>>> toBeCheck;
    public List<Integer> online;
    public INDArray onlineCheck;
    public INDArray finishAreaNum;

    public INDArray frameStartT;
    public INDArray frameEndT;
    public INDArray recvStartT;
    public INDArray recvEndT;
    public INDArray areaTransTime;
    
    public List<List<Integer>> frameSenderNum;
    public int currentFrame;
    public int latestFrame;

    private HashMap<Integer, Integer> totalSizes;
    private HashMap<Integer, Integer> amountRecv;
    private HashMap<Integer, Long> timestamps;
    private HashMap<Integer, Long> timeTaken;

    public Metadata() {
        this.areaSet = Nd4j.zeros(MAX_FRAME, MAX_VEHICLE);
        this.delaySet = Nd4j.zeros(MAX_VEHICLE);
        this.bwSet = new ArrayList<>(Collections.nCopies(MAX_VEHICLE, null));
        this.readySet = new ArrayList<>();
        this.checkTime = 0;
        this.live = true;

        this.perceptFlag = true;
        this.adaptFlag = true;

        this.neighborSet = Nd4j.zeros(MAX_VEHICLE, MAX_FRAME);
        this.neighborSetCheck = Nd4j.ones(MAX_FRAME, MAX_VEHICLE, MAX_VEHICLE).mul(-1);
        this.toBeCheck = Stream.generate(ArrayList<Pair<Integer, Long>>::new).limit(MAX_FRAME + 1).collect(Collectors.toList());
        this.online = new ArrayList<>();
        this.onlineCheck = Nd4j.ones(MAX_FRAME, MAX_VEHICLE).mul(-1);
        this.finishAreaNum = Nd4j.zeros(MAX_FRAME, MAX_VEHICLE);

        this.frameStartT = Nd4j.zeros(MAX_FRAME);
        this.frameEndT = Nd4j.zeros(MAX_FRAME);
        this.recvStartT = Nd4j.zeros(MAX_VEHICLE, MAX_FRAME);
        this.recvEndT = Nd4j.zeros(MAX_VEHICLE, MAX_FRAME);
        this.areaTransTime = Nd4j.zeros(MAX_VEHICLE, MAX_FRAME, 5);  // what does 5 mean here?

        this.frameSenderNum = generate2DLists(MAX_FRAME);
        this.currentFrame = 0;
        this.latestFrame = 0;

        this.totalSizes = new HashMap<>();
        this.amountRecv = new HashMap<>();
        this.timestamps = new HashMap<>();
    }

    private List<List<Integer>> generate2DLists(int rows) {
        return Stream.generate(ArrayList<Integer>::new).limit(rows).collect(Collectors.toList());
    }

    private int byteSubarrayToInt(byte[] arr, int start, int end) {
        return ByteBuffer.wrap(Arrays.copyOfRange(arr, start, end)).getInt();
    }

    public boolean parse(byte[] message, int index) {
        if (message[0] == 80 || message[0] == 79) {
            // ASCII 80 --> P, ASCII 79 --> O
            if (message.length < 6) {
                return false;
            }
            int size = byteSubarrayToInt(message, 0, 4);
            int frameID = byteSubarrayToInt(message, 4, 6);
            int pieceID = byteSubarrayToInt(message, 6, 8);

            if (totalSizes.containsKey(index)) {
                throw new RuntimeException(String.format("Error: received a new message from index %d before previous finishes", index));
            }
            int messageActualLength = message.length - 6;
            totalSizes.put(index, size);
            if (messageActualLength < size) {
                if (!timestamps.containsKey(index)) {
                    timestamps.put(index, System.currentTimeMillis());
                    amountRecv.put(index, message.length);
                }
                return false;
            } else {
                timeTaken.put(index, System.currentTimeMillis() - timestamps.get(index));
                double estBW = (message.length - amountRecv.get(index)) / timeTaken.get(index) / 125000;
                if (estBW != 0) {
                    bwSet.set(index, estBW);
                }
            }

            areaSet.putScalar(frameID, index, pieceID);
        }
        return true;
    }

    public static byte[] toBytes(byte[] content, int index, char fileType) {
        byte[] result = new byte[content.length + 1 + 4];
        byte[] size = ByteBuffer.allocate(4).putInt(content.length).array();
        System.arraycopy(content, 0, result, 1 + 4, content.length);
        System.arraycopy(size, 0, result, 0, 4);
        result[4] = (byte) fileType;
        return result;
    }
}