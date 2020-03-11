package systems.reformcloud.reformcloud2.executor.api.common.database.basic.drivers.mongo;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import systems.reformcloud.reformcloud2.executor.api.common.base.Conditions;
import systems.reformcloud.reformcloud2.executor.api.common.database.Database;
import systems.reformcloud.reformcloud2.executor.api.common.database.DatabaseReader;
import systems.reformcloud.reformcloud2.executor.api.common.dependency.DefaultDependency;
import systems.reformcloud.reformcloud2.executor.api.common.dependency.repo.DefaultRepositories;
import systems.reformcloud.reformcloud2.executor.api.common.utility.StringUtil;
import systems.reformcloud.reformcloud2.executor.api.common.utility.maps.AbsentMap;

import javax.annotation.Nonnull;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Map;

public final class MongoDatabase extends Database<com.mongodb.client.MongoDatabase> {

    private final Map<String, DatabaseReader> perTableReader = new AbsentMap<>();

    public MongoDatabase() {
        URL dependency = DEPENDENCY_LOADER.loadDependency(new DefaultDependency(
                DefaultRepositories.MAVEN_CENTRAL,
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
        this.createDatabase(table);
        return perTableReader.putIfAbsent(table, new MongoDatabaseReader(table, this));
    }

    @Nonnull
    @Override
    public com.mongodb.client.MongoDatabase get() {
        return mongoDatabase;
    }
}
