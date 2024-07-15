package client;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.gson.Gson;


public class Main {

    @Parameter(names = "-t", description = "Type of the request")
    private String type;
    @Parameter(names = "-k", description = "Key")
    private String key;
    @Parameter(names = "-v", description = "Value")
    private String value;
    @Parameter(names = "-in", description = "Test file name")
    private String testFileName;

    private static final String testFilePath = System.getProperty("user.dir")
            + File.separator +
            "src" + File.separator +
            "client";

    //private static final String fileName = testFilePath + File.separator + "datatestSettestSet.json";
    //private static final File file = new File(fileName);

    public static void main(String ... argv) {

        Main main = new Main();
        JCommander.newBuilder()
                .addObject(main)
                .build()
                .parse(argv);
        main.run();

    }
    public void run () {

        String ADDRESS = "127.0.0.1";
        int PORT = 23456;
        Gson gson = new Gson();
        Map<String, String> request = new HashMap<>();
    /*
        try {
            file.getParentFile().mkdirs();
            file.createNewFile();
        } catch ( IOException ioe ) {
            ioe.printStackTrace();
        }

     */

       // createTests();


        if (testFileName != null){
            request = readCommands(testFileName);
        } else {
            request.put("type", type);
            request.put("key", key);
            request.put("value", value);
        }
        String jsonRequest = gson.toJson(request);

        try (Socket socket = new Socket(InetAddress.getByName(ADDRESS), PORT)) {
            DataInputStream input = new DataInputStream(socket.getInputStream());
            DataOutputStream output = new DataOutputStream(socket.getOutputStream());
            System.out.println("Client started!");

            //send request
            System.out.printf("Sent: %s \n", jsonRequest);
            output.writeUTF("%s".formatted(jsonRequest));

            String inputString = input.readUTF();
            System.out.println("Received: " + inputString);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void createTests () {

        ReadWriteLock lock = new ReentrantReadWriteLock();
        Lock writeLock = lock.writeLock();

        writeLock.lock();
        String setFileName = getFullFilePath("testSet.json", false);
        File file = new File(setFileName);
        file.getParentFile().mkdirs();
        String jsonTestSet = "{\n" +
                "   \"type\":\"set\",\n" +
                "   \"key\":\"person\",\n" +
                "   \"value\":{\n" +
                "      \"name\":\"Elon Musk\",\n" +
                "      \"car\":{\n" +
                "         \"model\":\"Tesla Roadster\",\n" +
                "         \"year\":\"2018\"\n" +
                "      },\n" +
                "      \"rocket\":{\n" +
                "         \"name\":\"Falcon 9\",\n" +
                "         \"launches\":\"87\"\n" +
                "      }\n" +
                "   }\n" +
                "}";
        //System.out.println(jsonTestSet);
        try (FileWriter writer = new FileWriter(setFileName)) {
            writer.write(jsonTestSet);
            //gson.toJson(jsonTestSet, writer);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            writeLock.unlock();
        }
/*
        writeLock.lock();
        String getFileName = getFullFilePath("testGet.json", false);
        File fileGet = new File(getFileName);
        fileGet.getParentFile().mkdirs();
        String jsonTestGet = "{\"type\":\"get\",\"key\":\"person\"}";
        System.out.println(jsonTestGet);
        try (FileWriter writer = new FileWriter(getFileName)) {
            writer.write(jsonTestGet);
            //gson.toJson(jsonTestGet, writer);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            writeLock.unlock();
        }
*/
        /*
        //get name
        writeLock.lock();
        String getFileName = getFullFilePath("testGet.json", false);
        File fileGet = new File(getFileName);
        fileGet.getParentFile().mkdirs();
        String jsonTestGet = "{\"type\":\"get\",\"key\":[\"person\",\"name\"]}";
        System.out.println(jsonTestGet);
        try (FileWriter writer = new FileWriter(getFileName)) {
            writer.write(jsonTestGet);
            //gson.toJson(jsonTestGet, writer);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            writeLock.unlock();
        }
         */
        //get wrong key
        /*
        writeLock.lock();
        String getFileName = getFullFilePath("testGet.json", false);
        File fileGet = new File(getFileName);
        fileGet.getParentFile().mkdirs();
        String jsonTestGet = "{\"type\":\"get\",\"key\":[\"person\",\"nonexistent key\"]}";
        System.out.println(jsonTestGet);
        try (FileWriter writer = new FileWriter(getFileName)) {
            writer.write(jsonTestGet);
            //gson.toJson(jsonTestGet, writer);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            writeLock.unlock();
        }
*/
        writeLock.lock();
        String setNewFileName = getFullFilePath("testSet1.json", false);
        File fileSet = new File(setNewFileName);
        fileSet.getParentFile().mkdirs();
        String jsonTestSetNew = "{\"type\":\"set\",\"key\":[\"person\",\"rocket\",\"launches\"], \"value\":88}";
        System.out.println(jsonTestSetNew);
        try (FileWriter writer = new FileWriter(setNewFileName)) {
            writer.write(jsonTestSetNew);
            //gson.toJson(jsonTestGet, writer);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            writeLock.unlock();
        }

        /*
        writeLock.lock();
        String deleteFileName = getFullFilePath("testDelete.json", false);
        File fileDelete = new File(deleteFileName);
        fileDelete.getParentFile().mkdirs();
        String jsonTestDelete = "{\"type\":\"delete\",\"key\":[\"person\",\"car\", \"year\"]}";
        //System.out.println(jsonTestDelete);
        try (FileWriter writer = new FileWriter(deleteFileName)) {
            writer.write(jsonTestDelete);
            //gson.toJson(jsonTestGet, writer);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            writeLock.unlock();
        }
        */

    }

    private static String getFullFilePath(String filename, boolean myTest) {
        if(myTest) {
            return "src" + File.separator + filename;
        }
        return System.getProperty("user.dir")
                + File.separator +
                "src" + File.separator +   "client"
                + File.separator + "data"
                + File.separator + filename;
    }
    public Map<String,String> readCommands(String testFileName) {
        String testFileFullName = getFullFilePath(testFileName, false);

        Gson gson = new Gson();
        Map<String, String> request = new HashMap<>();
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(testFileFullName))) {
            request = gson.fromJson(bufferedReader, HashMap.class);
        } catch (RuntimeException | IOException e) {
            e.printStackTrace(System.out);
        }
        return request;
    }
}
