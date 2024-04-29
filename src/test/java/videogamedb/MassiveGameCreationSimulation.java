package videogamedb;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;


public class MassiveGameCreationSimulation extends Simulation{
    private HttpProtocolBuilder httpProtocol = http
            .baseUrl("https://videogamedb.uk/api/")
            .acceptHeader("application/json")
            .contentTypeHeader("application/json");

    private static final int USER_COUNT = Integer.parseInt(System.getProperty("USERS", "1000"));
    private static final int DURATION = Integer.parseInt(System.getProperty("DURATION", "10"));

    private static FeederBuilder.FileBased<Object> jsonFeeder = jsonFile("data/gameJsonFile.json").random(); //Elegimos una entrada random de nuestro archivo json

    private static ChainBuilder authenticate = exec(http("Authenticate")
            .post("/authenticate")
            .body(StringBody("{\n" +
                    "  \"password\": \"admin\",\n" +
                    "  \"username\": \"admin\"\n" +
                    "}"))
            .check(jmesPath("token").saveAs("authToken"))); //Guardamos el token en una variable para usarlo en la prueba

    @Override
    public void before() {
        System.out.printf("Running test with peak of %d users during %d seconds", USER_COUNT, DURATION);
    }

    private static ChainBuilder postNewGame =
            feed(jsonFeeder)
                    .exec(http("Post New Game - #{name}")
                            .post("/videogame")
                            .header("Authorization", "Bearer #{authToken}")
                            .body(ElFileBody("bodies/newGameTemplate.json")).asJson());

    private ScenarioBuilder scn = scenario("Creación Masiva de Juegos")
            .exec(authenticate)
            .exec(postNewGame)
            .exec(postNewGame)
            .exec(postNewGame)
            .exec(postNewGame)
            .exec(postNewGame);

    {
        setUp(
                scn.injectOpen(
                        nothingFor(5), //Nada durante 5 segundos
                        stressPeakUsers(USER_COUNT).during(DURATION) //Inyectamos un pico repentino de usuarios durante el tiempo que hemos definido
                )
        ).protocols(httpProtocol);
    }

}
