package videogamedb;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class ConstantGameUpdateSimulation extends Simulation{
    private HttpProtocolBuilder httpProtocol = http
            .baseUrl("https://videogamedb.uk/api/")
            .acceptHeader("application/json")
            .contentTypeHeader("application/json");

    private static final int USER_COUNT = Integer.parseInt(System.getProperty("USERS", "100"));
    private static final int DURATION = Integer.parseInt(System.getProperty("DURATION", "10"));

    private static FeederBuilder.FileBased<Object> jsonFeeder = jsonFile("data/gameJsonFile.json").random();

    @Override
    public void before() {
        System.out.printf("Running test with constant %d users during %d seconds in random intervals", USER_COUNT, DURATION);
    }
    private static ChainBuilder authenticate = exec(http("Authenticate")
            .post("/authenticate")
            .body(StringBody("{\n" +
                    "  \"password\": \"admin\",\n" +
                    "  \"username\": \"admin\"\n" +
                    "}"))
            .check(jmesPath("token").saveAs("authToken")));

    private static ChainBuilder updateGame =
            feed(jsonFeeder)
                    .exec(http("Update Game - #{name}")
                            .put("/videogame/#{id}")
                            .header("Authorization", "Bearer #{authToken}")
                            .body(ElFileBody("bodies/updateGameTemplate.json")).asJson()
                            .check(jmesPath("category").isEL("Modified"))); //Comprobamos que la categoria del juego sea "Modified" que es el valor que actualice

    private ScenarioBuilder scn = scenario("Constant games updates")
            .exec(authenticate)
            .pause(2) //Pausa de 2 segundos para que no se ejecute la siguiente peticion justo despues de la autenticacion
            .exec(updateGame);

    {
        setUp(
                scn.injectOpen(constantUsersPerSec(USER_COUNT).during(DURATION).randomized()) //Inyectamos usuarios constantemente durante x segundos en intervalos aleatorios
        ).protocols(httpProtocol);
    }
}
