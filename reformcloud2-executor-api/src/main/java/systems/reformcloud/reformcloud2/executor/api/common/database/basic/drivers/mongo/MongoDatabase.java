package systems.reformcloud.reformcloud2.executor.api.common.database.basic.drivers.mongo;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import systems.reformcloud.reformcloud2.executor.api.common.base.Conditions;
import systems.reformcloud.reformcloud2.executor.api.common.configuration.JsonConfiguration;
import systems.reformcloud.reformcloud2.executor.api.common.database.Database;
import systems.reformcloud.reformcloud2.executor.api.common.database.DatabaseReader;
import systems.reformcloud.reformcloud2.executor.api.common.dependency.util.MavenCentralDependency;
import systems.reformcloud.reformcloud2.executor.api.common.utility.StringUtil;
import systems.reformcloud.reformcloud2.executor.api.common.utility.maps.AbsentMap;
import systems.reformcloud.reformcloud2.executor.api.common.utility.task.Task;
import systems.reformcloud.reformcloud2.executor.api.common.utility.task.defaults.DefaultTask;

import javax.annotation.Nonnull;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public final class MongoDatabase extends Database<com.mongodb.client.MongoDatabase> {

    private static final String KEY_NAME = "_key";

    private static final String ID_NAME = "_identifier";

    private final Map<String, DatabaseReader> perTableReader = new AbsentMap<>();

    public MongoDatabase() {
        URL dependency = DEPENDENCY_LOADER.loadDependency(new MavenCentralDependency(
                "org.mongodb",
                "mongo-java-driver",
                "3.12.2"
        ));
        Conditions.nonNull(dependency, StringUtil.formatError("dependency load for mongo database"));
        DEPENDENCY_LOADER.addDependency(dependency);
    }

    private MongoClient mongoClient;

    private com.mongodb.client.MongoDatabase mongoDatabase;

    private String host;

    private int port;

    private String userName;

    private String password;

    private String table;

    @Override
    public void connect(@Nonnull String host, int port, @Nonnull String userName, @Nonnull String password, @Nonnull String table) {
        if (!isConnected()) {
            this.host = host;
            this.port = port;
            this.userName = userName;
            this.password = password;
            this.table = table;

            try {
                this.mongoClient = MongoClients.create(
                        MessageFormat.format(
                                "mongodb://{0}:{1}@{2}:{3}/{4}",
                                userName,
                                URLEncoder.encode(password, StandardCharsets.UTF_8.name()),
                                host,
                                Integer.toString(port),
                                table
                        )
                );
                this.mongoDatabase = mongoClient.getDatabase(table);
            } catch (final UnsupportedEncodingException ex) {
                ex.printStackTrace(); //Should never happen
            }
        }
    }

    @Override
    public boolean isConnected() {
        return mongoClient != null;
    }

    @Override
    public void reconnect() {
        disconnect();
        connect(host, port, userName, password, table);
    }

    @Override
    public void disconnect() {
        if (isConnected()) {
            this.mongoClient.close();
            this.mongoClient = null;
        }
    }

    @Override
    public boolean createDatabase(String name) {
        mongoDatabase.getCollection(name);
        return true;
    }

    @Override
    public boolean deleteDatabase(String name) {
        mongoDatabase.getCollection(name).drop();
        return true;
    }

    @Override
    public DatabaseReader createForTable(String table) {
        return perTableReader.putIfAbsent(table, new DatabaseReader() {
            @Nonnull
            @Override
            public Task<JsonConfiguration> find(@Nonnull String key) {
                Task<JsonConfiguration> task = new DefaultTask<>();
                Task.EXECUTOR.execute(() -> {
                    Document document = mongoDatabase.getCollection(table).find(Filters.eq(KEY_NAME, key)).first();
                    if (document == null) {
                        task.complete(null);
                    } else {
                        JsonConfiguration configuration = new JsonConfiguration(document.toJson());
                        configuration.remove(KEY_NAME).remove(ID_NAME);
                        task.complete(configuration);
                    }
                });
                return task;
            }

            @Nonnull
            @Override
            public Task<JsonConfiguration> findIfAbsent(@Nonnull String identifier) {
                Task<JsonConfiguration> task = new DefaultTask<>();
                Task.EXECUTOR.execute(() -> {
                    Document document = mongoDatabase.getCollection(table).find(Filters.eq(ID_NAME, identifier)).first();
                    if (document == null) {
                        task.complete(null);
                    } else {
                        JsonConfiguration configuration = new JsonConfiguration(document.toJson());
                        configuration.remove(KEY_NAME).remove(ID_NAME);
                        task.complete(configuration);
                    }
                });
                return task;
            }

            @Nonnull
            @Override
            public Task<JsonConfiguration> insert(@Nonnull String key, String identifier, @Nonnull JsonConfiguration data) {
                Task<JsonConfiguration> task = new DefaultTask<>();
                Task.EXECUTOR.execute(() -> {
                    Document document = mongoDatabase.getCollection(table).find(Filters.eq(ID_NAME, identifier)).first();
                    if (document == null) {
                        data.add(KEY_NAME, key).add(ID_NAME, identifier != null ? identifier : UUID.randomUUID().toString());
                        mongoDatabase.getCollection(table).insertOne(JsonConfiguration.GSON.get().fromJson(data.toPrettyString(), Document.class));
                        task.complete(data);
                    } else {
                        task.complete(new JsonConfiguration(document.toJson()));
                    }
                });
                return task;
            }

            @Nonnull
            @Override
            public Task<Boolean> update(@Nonnull String key, @Nonnull JsonConfiguration newData) {
                Task<Boolean> task = new DefaultTask<>();
                Task.EXECUTOR.execute(() -> {
                    Document document = mongoDatabase.getCollection(table).find(Filters.eq(KEY_NAME, key)).first();
                    if (document == null) {
                        task.complete(false);
                    } else {
                        JsonConfiguration configuration = new JsonConfiguration(document.toJson());
                        remove(key).awaitUninterruptedly();
                        insert(key, configuration.getString(ID_NAME), newData).awaitUninterruptedly();
                        task.complete(true);
                    }
                });
                return task;
            }

            @Nonnull
            @Override
            public Task<Boolean> updateIfAbsent(@Nonnull String identifier, @Nonnull JsonConfiguration newData) {
                Task<Boolean> task = new DefaultTask<>();
                Task.EXECUTOR.execute(() -> {
                    Document document = mongoDatabase.getCollection(table).find(Filters.eq(ID_NAME, identifier)).first();
                    if (document == null) {
                        task.complete(false);
                    } else {
                        JsonConfiguration configuration = new JsonConfiguration(document.toJson());
                        remove(configuration.getString(KEY_NAME)).awaitUninterruptedly();
                        insert(configuration.getString(KEY_NAME), identifier, newData).awaitUninterruptedly();
                        task.complete(true);
                    }
                });
                return task;
            }

            @Nonnull
            @Override
            public Task<Void> remove(@Nonnull String key) {
                Task<Void> task = new DefaultTask<>();
                Task.EXECUTOR.execute(() -> {
                    mongoDatabase.getCollection(table).deleteOne(Filters.eq(KEY_NAME, key));
                    task.complete(null);
                });
                return task;
            }

            @Nonnull
            @Override
            public Task<Void> removeIfAbsent(@Nonnull String identifier) {
                Task<Void> task = new DefaultTask<>();
                Task.EXECUTOR.execute(() -> {
                    mongoDatabase.getCollection(table).deleteOne(Filters.eq(ID_NAME, identifier));
                    task.complete(null);
                });
                return task;
            }

            @Nonnull
            @Override
            public Task<Boolean> contains(@Nonnull String key) {
                Task<Boolean> task = new DefaultTask<>();
                Task.EXECUTOR.execute(() -> {
                    Document document = mongoDatabase.getCollection(table).find(Filters.eq(KEY_NAME, key)).first();
                    task.complete(document != null);
                });
                return task;
            }

            @Nonnull
            @Override
            public Task<Integer> size() {
                Task<Integer> task = new DefaultTask<>();
                Task.EXECUTOR.execute(() -> {
                    AtomicInteger atomicInteger = new AtomicInteger(0);
                    mongoDatabase.getCollection(table).find().forEach((Consumer<Document>) document -> atomicInteger.addAndGet(1));
                    task.complete(atomicInteger.get());
                });
                return task;
            }

            @Nonnull
            @Override
            public String getName() {
                return table;
            }

            @Override
            @Nonnull
            public Iterator<JsonConfiguration> iterator() {
                List<JsonConfiguration> list = new ArrayList<>();
                mongoDatabase.getCollection(table).find().forEach((Consumer<Document>) document -> list.add(new JsonConfiguration(document.toJson())));
                return list.iterator();
            }
        });
    }

    @Nonnull
    @Override
    public com.mongodb.client.MongoDatabase get() {
        return mongoDatabase;
    }
}
