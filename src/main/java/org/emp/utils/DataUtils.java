package org.emp.utils;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.channels.FileChannel;
import java.util.stream.Stream;

public class DataUtils {
    private static final int BUFFER_MAX_BYTES = 4 * 1024 * 1024;

    public static float[] readPointCloudFromFile(String filePath) throws IOException {
        RandomAccessFile file = new RandomAccessFile(filePath, "r");
        FileChannel channel = file.getChannel();
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_MAX_BYTES);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.clear();

        channel.read(buffer);

        float[] points = new float[buffer.position() / Float.BYTES];
        buffer.rewind();
        buffer.asFloatBuffer().get(points);

        channel.close();
        return points;
    }

    public static void writePointCloudToFile(float[] pointCloud, String filePath) throws IOException {
        RandomAccessFile file = new RandomAccessFile(filePath, "rw");
        FileChannel channel = file.getChannel();
        ByteBuffer byteBuffer = ByteBuffer.allocate(4 * pointCloud.length);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();
        floatBuffer.put(pointCloud);
        floatBuffer.flip();

        channel.write(byteBuffer);

        file.close();
        channel.close();
    }

    public static float readVehicleHeight(String fileDir) throws IOException {
        float height = 0.0f;
        String fileName = Stream.of(new File(fileDir).listFiles()).map(File::getName).findFirst().get();
        File file = new File(fileDir + fileName);
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String tempString;
        while ((tempString = reader.readLine()) != null) {
            String[] egoObjectStrings = tempString.split(" ");
            height = Float.parseFloat(egoObjectStrings[8]);
        }
        reader.close();

        return height;
    }
}
