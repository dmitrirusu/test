package webui.Controllers;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.tasks.TaskCompletionSource;
import com.google.firebase.tasks.Tasks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tarantool.TarantoolClient;
import spark.Route;

import java.util.concurrent.TimeUnit;

import static spark.Spark.halt;

public class TokenController {
    private static final Logger LOG = LoggerFactory.getLogger(TokenController.class.getSimpleName());

    public static Route getToken(TarantoolClient tarantoolClient) {
        return (request, response) -> {
            String fireToken = request.queryParams("token");
            final String[] phone = new String[1];

            TaskCompletionSource<Boolean> verifyCS = new TaskCompletionSource<>();
            FirebaseAuth.getInstance().getUser(fireToken)
                    .addOnSuccessListener(userRecord -> {
                        com.google.firebase.auth.UserInfo[] providerData = userRecord.getProviderData();
                        for (com.google.firebase.auth.UserInfo info : providerData) {
                            if (info.getProviderId().equals("phone")) {
                                phone[0] = info.getUid().substring(1);
                            }
                        }

                        verifyCS.setResult(true);
                        LOG.debug(String.format("Successfully fetched user data: %s", userRecord.getUid()));
                    })
                    .addOnFailureListener(e -> {
                        LOG.error(String.format("Error fetching user data:  %s", e.getMessage()));
                        verifyCS.setResult(false);
                        halt(400, "Wrong fireId");
                    });
            Tasks.await(verifyCS.getTask(), 10, TimeUnit.SECONDS);


            LOG.debug("Token verified");
            return response;
        };
    }
}
