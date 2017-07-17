package webui.Controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tarantool.TarantoolClient;
import spark.Route;
import webui.util.Helper;

import static spark.Spark.halt;

public class UserController {

    private static final Logger LOG = LoggerFactory.getLogger(UserController.class.getSimpleName());

    public static Route addUser(TarantoolClient tarantoolClient) {
        return (request, response) -> {
            String phone = request.queryParams("phone");

            if (phone.isEmpty()) {
                halt(404, "User not found");
            }

            tarantoolClient.syncOps().call("");

            return "";
        };
    }

    public static Route getProfile(TarantoolClient tarantoolClient) {
        return (request, response) -> {
            String phone = request.queryParams("phone");

            //TODO Get data from tarantool DB

            return Helper.readJson("userInfoSample.json");
        };
    }
}
