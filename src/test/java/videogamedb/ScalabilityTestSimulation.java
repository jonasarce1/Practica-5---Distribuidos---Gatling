package videogamedb;


import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;
public class ScalabilityTestSimulation extends Simulation{
    private HttpProtocolBuilder httpProtocol = http
            .baseUrl("https://videogamedb.uk/api/")
            .acceptHeader("application/json")
            .contentTypeHeader("application/json");

    private static final int USER_COUNT = Integer.parseInt(System.getProperty("USER_COUNT", "500"));
    private static final int RAMP_DURATION = Integer.parseInt(System.getProperty("RAMP_DURATION", "20"));

    @Override
    public void before() {
        System.out.printf("Running scalability test with %d users and a %d seconds ramp up", USER_COUNT, RAMP_DURATION);
    }


    private static ChainBuilder getAllVideoGames = exec(http("Get All Video Games")
            .get("/videogame"));

    private ScenarioBuilder scn = scenario("Scalability Test Scenario ")
            .exec(getAllVideoGames);
    {
        setUp(
                scn.injectOpen(
                        rampUsers(USER_COUNT).during(RAMP_DURATION) //Aumentamos gradualmente la carga de usuarios
                )
        ).protocols(httpProtocol);
    }
}

