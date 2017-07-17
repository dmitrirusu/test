package webui;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.tasks.TaskCompletionSource;
import com.google.firebase.tasks.Tasks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tarantool.TarantoolClient;
import spark.Filter;
import spark.Request;
import spark.Response;
import spark.Service;
import webui.Controllers.TokenController;
import webui.Controllers.UserController;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static spark.Spark.halt;


public class APIService {

    private static final Logger LOG = LoggerFactory.getLogger(APIService.class.getSimpleName());
    public Service spark;
    private Properties config;
    private TarantoolClient tarantoolClient;

    public APIService(Properties config, TarantoolClient tarantoolClient) {
        this.config = config;
        this.tarantoolClient = tarantoolClient;
    }

    public APIService start() throws ExecutionException, InterruptedException, SQLException {
        LOG.info("Starting.");

        this.spark = Service.ignite();
        new Thread(this::initSpark).start();
        spark.awaitInitialization();
        return this;
    }

    public void stop() {
        LOG.info("Stopping.");
        if (spark != null) {
            spark.stop();
        }
    }

    private void initSpark() {
        AuthFilter authFilter = new AuthFilter(tarantoolClient);
        spark.port(Integer.parseInt(config.getProperty("port", "12448")));
        spark.staticFiles.externalLocation(config.getProperty("staticFilesDir"));

        spark.post("/token/firebase", TokenController.getToken(tarantoolClient));

//        spark.before("/add_user", authFilter);
        spark.post("/add_user", UserController.addUser(tarantoolClient));

//        spark.before("/profile", authFilter);
        spark.get("/profile", UserController.getProfile(tarantoolClient));

        spark.get("/*", (request, response) -> new String(
                Files.readAllBytes(
                        Paths.get(config.getProperty("staticFilesDir"), "index.html"))));
    }


    public static class AuthFilter implements Filter {

        private TarantoolClient tarantoolClient;

        public AuthFilter(TarantoolClient tarantoolClient) {
            this.tarantoolClient = tarantoolClient;
        }

        @Override
        public void handle(Request request, Response response) throws Exception {
            LOG.debug("before: " + request.pathInfo());
            String fireToken = request.headers("Authorization");

            if (!verify(request, fireToken)) {
                halt(400, "Wrong fireId");
            }
        }

        private boolean verify(Request request, String fireToken) throws ExecutionException, InterruptedException, java.util.concurrent.TimeoutException {
            TaskCompletionSource<Boolean> verifyCS = new TaskCompletionSource<>();

            FirebaseAuth.getInstance().getUser(fireToken)
                    .addOnSuccessListener(userRecord -> {
                        com.google.firebase.auth.UserInfo[] providerData = userRecord.getProviderData();
                        for (com.google.firebase.auth.UserInfo info : providerData) {
                            if (info.getProviderId().equals("phone")) {
                                String substring = info.getUid().substring(1);
                                request.session().attribute("phone", substring);
                                List<Boolean> isAdmin = ((ArrayList<Boolean>) tarantoolClient.syncOps().call("isAdmin", substring).get(0));
                                verifyCS.setResult(isAdmin.get(0));
                                return;
                            }
                        }
                        verifyCS.setResult(false);
                        LOG.debug(String.format("Successfully fetched user data: %s", userRecord.getUid()));
                    })
                    .addOnFailureListener(e -> {
                        LOG.error(String.format("Error fetching user data:  %s", e.getMessage()));
                        verifyCS.setResult(false);
                    });
            return Tasks.await(verifyCS.getTask(), 10, TimeUnit.SECONDS);
        }
    }

}
