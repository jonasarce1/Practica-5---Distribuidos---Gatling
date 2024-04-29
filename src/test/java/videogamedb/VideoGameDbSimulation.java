package videogamedb;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class VideoGameDbSimulation extends Simulation {

    //Configuracion http
    //La configuracion http es para definir la base url y el header de la peticion
    private HttpProtocolBuilder httpProtocol = http
            .baseUrl("https://videogamedb.uk/api/")
            .acceptHeader("application/json");

    //LLamadas a metodos (http)

    //GET ALL GAMES
    private static ChainBuilder getAllVideoGames = exec(http("Get All Video Games")
            .get("/videogame"));

    //Definicion del escenario
    //La definicion del escenario es para definir el flujo de la prueba
    private ScenarioBuilder scn = scenario("Video Game DB Stress Test")
            .exec(http("Get All Video Games")
                    .get("/videogame"));

    //Cargar simulacion
    //Hago un setUp para cargar la simulacion, inyectando un usuario al inicio
    {
        setUp(
        scn.injectOpen(atOnceUsers(1))
        ).protocols(httpProtocol);
    }
}
