package server;

import com.google.gson.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Main {
    private static final int PORT = 23456;
    private static final String ADDRESS = "127.0.0.1";
    //private static final String path = System.getProperty("user.dir") + "/src/server/data/db.json";
    /*private static final String dbFilePath = System.getProperty("user.dir") + File.separator +
            "src" + File.separator +
            "server" + File.separator +
            "data"; */
    private static final String path = System.getProperty("user.dir")
            + File.separator +
            "src" + File.separator +
            "server" + File.separator +
            "data";

    //private static final String fileName = dbFilePath + File.separator + "db.json";
    private static final String fileName = path + File.separator + "db.json";
    private static final File file = new File(fileName);

    private static final Map<String, JsonElement> dataBase = new HashMap<>();

    public static void main(String[] args) {
        boolean shouldContinue = true;

        //System.out.println(fileName);
        try {
            file.getParentFile().mkdirs();
            file.createNewFile();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        ReadWriteLock lock = new ReentrantReadWriteLock();
        ExecutorService executor = Executors.newFixedThreadPool(4);

        try (ServerSocket server = new ServerSocket(PORT)) {
            System.out.println("Server started!");
            do {
                Socket socket = server.accept();
                DataInputStream input = new DataInputStream(socket.getInputStream());
                DataOutputStream output = new DataOutputStream(socket.getOutputStream());

                Gson gson = new Gson();

                String inputString = input.readUTF();

                JsonObject request = gson.fromJson(inputString, JsonObject.class);

                String type = request.get("type").getAsString();

                shouldContinue = !type.equals("exit");
                executor.submit(() -> {
                    try {
                        Map<String, JsonElement> response = getResponse(request, lock);
                        System.out.printf("Received: %s \n", request.toString());
                        String jsonResponse = gson.toJson(response);
                        output.writeUTF(jsonResponse);
                        System.out.printf("Sent: %s \n", jsonResponse);
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            } while (shouldContinue);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }
    }

    private static Map<String, JsonElement> getResponse(JsonObject request, ReadWriteLock rwLock) throws IOException {

        Map<String, JsonElement> response = new HashMap<>();
        Lock readLock = rwLock.readLock();
        Lock writeLock = rwLock.writeLock();
        Gson gson = new Gson();

        if (request.get("type").getAsString().equals("exit")) {
            response.put("response", JsonParser.parseString("OK"));
            return response;
        } else {
            switch (request.get("type").getAsString()) {
                case "set" -> {
                    writeLock.lock();
                        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(fileName))) {
                            JsonObject dataBaseParsed = new JsonObject();
                            if (Files.size(Path.of(fileName)) != 0) {
                                dataBaseParsed = gson.fromJson(bufferedReader, JsonObject.class);
                            }
                            try (FileWriter writer = new FileWriter(fileName)) {
                                JsonElement requestKey = (request.get("key"));
                                JsonElement requestValue = (request.get("value"));
                                JsonElement updatedDb = putDbValue(dataBaseParsed, requestKey, requestValue);
                                //dataBase.put(String.valueOf(requestKey), requestValue);
                                gson.toJson(updatedDb, writer);
                                response.put("response", JsonParser.parseString("OK"));
                            } catch (IOException e) {
                                e.printStackTrace();
                            } finally {
                                writeLock.unlock();
                            }
                        }
                }
                case "get" -> {
                    readLock.lock();
                    try (BufferedReader bufferedReader = new BufferedReader(new FileReader(fileName))) {
                        JsonObject dataBaseParsed = gson.fromJson(bufferedReader, JsonObject.class);
                        JsonElement requestKey = (request.get("key"));
                        JsonElement dbValue = getDbValue(dataBaseParsed, requestKey);
                        response.put("response", new JsonPrimitive("OK"));
                        response.put("value", dbValue);
                    } catch (RuntimeException e) {
                        response.put("response", new JsonPrimitive("ERROR"));
                        response.put("reason", new JsonPrimitive(e.getMessage()));
                    } finally {
                        readLock.unlock();
                    }
                }
                case "delete" -> {
                    writeLock.lock();
                    try (BufferedReader bufferedReader = new BufferedReader(new FileReader(fileName))) {
                        JsonObject dataBaseParsed = gson.fromJson(bufferedReader, JsonObject.class);
                        try (FileWriter writer = new FileWriter(fileName)) {
                            JsonElement requestKey = (request.get("key"));
                            JsonElement requestValue = (request.get("value"));
                            JsonElement updatedDb = deleteDbValue(dataBaseParsed, requestKey);
                            //dataBase.put(String.valueOf(requestKey), requestValue);
                            gson.toJson(updatedDb, writer);
                            response.put("response", JsonParser.parseString("OK"));
                        } catch (IOException e) {
                            e.printStackTrace();
                            response.put("response", new JsonPrimitive("ERROR"));
                            response.put("reason", new JsonPrimitive(e.getMessage()));
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        response.put("response", new JsonPrimitive("ERROR"));
                        response.put("reason", new JsonPrimitive(e.getMessage()));
                    } finally {
                        writeLock.unlock();
                    }
                }
                default -> throw new IllegalStateException("Unexpected value: " + request.get("type"));
            }
        }
        return response;
    }

    private static JsonElement getDbValue(JsonObject dataBaseParsed, JsonElement requestKey) {
        JsonElement dbValue = dataBaseParsed;
        if (requestKey.isJsonPrimitive()) {
            return dataBaseParsed.getAsJsonObject().get(requestKey.getAsString());
        } else {
            JsonElement endKey = getEndKey(requestKey);
            JsonArray complexKey = requestKey.getAsJsonArray();

            for (JsonElement arrayElem : complexKey) {
                if (dbValue.getAsJsonObject().has(arrayElem.getAsString())) { //if key exists in proper lvl of db, go lvl down
                    dbValue = dbValue.getAsJsonObject().get(arrayElem.getAsString());
                    // dbValue must be jsonobject if this one is not last key
                    if(endKey != arrayElem && dbValue.isJsonPrimitive()) {
                        throw new RuntimeException("No such key");
                    }
                } else {
                    throw new RuntimeException("No such key");
                }
            }
        }
        return dbValue;
    }

    private static JsonElement putDbValue( JsonObject dataBaseParsed, JsonElement requestKey, JsonElement requestValue) {
        JsonObject nextValue = dataBaseParsed;
        if (requestKey.isJsonPrimitive()) {
            dataBaseParsed.add(requestKey.getAsString(), requestValue);
            return dataBaseParsed;

        } else {
            JsonElement endKey = getEndKey(requestKey);
            JsonArray complexKey = requestKey.getAsJsonArray();

            for (JsonElement arrayElem : complexKey) {
                if (endKey != arrayElem) {
                    if (!nextValue.has(arrayElem.getAsString())) {
                        nextValue.add(arrayElem.getAsString(), new JsonObject());
                    }
                    nextValue = nextValue.getAsJsonObject(arrayElem.getAsString());
                } else {
                    nextValue.add(arrayElem.getAsString(), requestValue);
                }
            }
        }
        return dataBaseParsed;
    }
    private static JsonElement deleteDbValue( JsonObject dataBaseParsed, JsonElement requestKey) {
        JsonElement dbValue = dataBaseParsed;
        if (requestKey.isJsonPrimitive()) {
            dataBaseParsed.remove(requestKey.getAsString());
            return dataBaseParsed;

        } else {
            JsonElement endKey = getEndKey(requestKey);
            JsonArray complexKey = requestKey.getAsJsonArray();

            for (JsonElement arrayElem : complexKey) {
                if (dbValue.getAsJsonObject().has(arrayElem.getAsString())) { //if key exists in proper db lvl
                    if (endKey == arrayElem) {
                        dbValue.getAsJsonObject().remove(arrayElem.getAsString());
                    } else {
                        dbValue = dbValue.getAsJsonObject().get(arrayElem.getAsString());
                    }
                } else {
                    throw new RuntimeException("No such key");
                }
            }
        }
        return dataBaseParsed;
    }
    private static JsonElement getEndKey(JsonElement requestKey) {
        JsonArray complexKey = requestKey.getAsJsonArray();
        JsonElement endKey = complexKey.get(complexKey.size() - 1);
        try {
            for (JsonElement arrayElem : complexKey) {
                if (!arrayElem.isJsonNull()) {
                    endKey = arrayElem;
                }
            }
        } catch (RuntimeException e) {
            e.printStackTrace(System.out);
        }
        return endKey;
    }
}



