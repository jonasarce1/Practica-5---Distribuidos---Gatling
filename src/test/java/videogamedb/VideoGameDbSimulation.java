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
            .acceptHeader("application/json") //Asignamos headers por defecto tanto para aceptar como para mandar contenido
            .contentTypeHeader("application/json");

    //Parametros runtime
    //Numero de usuarios que vamos a simular, System.getProperty nos da la opcion de pasar parametros en tiempo de ejecucion y si no habra 5 usuarios
    private static final int USER_COUNT = Integer.parseInt(System.getProperty("USERS", "5"));
    //Duracion de la rampa, esto es el tiempo gradual que se tardara en aumentar la carga de usuarios
    private static final int RAMP_DURATION = Integer.parseInt(System.getProperty("RAMP_DURATION", "10"));


    //Feeder (inyector de datos)
    //Creo un json con los datos de todos los juegos de la API en formato json
    private static FeederBuilder.FileBased<Object> jsonFeeder = jsonFile("data/gameJsonFile.json").random(); //Elegimos una entrada random de nuestro archivo json


    //Bloque Before
    @Override
    public void before() {
        System.out.printf("Running test with %d users and a %d seconds ramp up", USER_COUNT, RAMP_DURATION);
    }


    //LLamadas a metodos (http)

    //GET ALL GAMES
    private static ChainBuilder getAllVideoGames = exec(http("Get All Video Games")
            .get("/videogame"));

    //AUTENTICACION
    private static ChainBuilder authenticate = exec(http("Authenticate")
            .post("/authenticate")
            .body(StringBody("{\n" +
                    "  \"password\": \"admin\",\n" +
                    "  \"username\": \"admin\"\n" +
                    "}"))
            .check(jmesPath("token").saveAs("authToken"))); //Guardamos el token en una variable para usarlo en la prueba

    //POST GAME
    private static ChainBuilder postNewGame =
            feed(jsonFeeder) //Usamos el feeder para inyectar datos
                    .exec(http("Post New Game - #{name}") //#{name} sera el nombre de un juego aleatorio de nuestro json
                            .post("/videogame")
                            .header("Authorization", "Bearer #{authToken}") //Indicamos con #{} que vamos a usar ese token en la prueba/newGameTemplate.json
                            .body(ElFileBody("bodies/newGameTemplate.json")).asJson()); //Usamos un archivo json con el template del videojuego (ElFileBody = FileBody)

    //GET LAST POSTED GAME
    private static ChainBuilder getLastPostedGame = exec(http("Get Last Posted Game - #{name}")
            .get("/videogame/#{id}") //Id del juego que acabamos de postear
            .check(jmesPath("name").isEL("#{name}"))); //Comprobamos que el nombre del juego sea el mismo que el que acabamos de postear (isEL = is equal)

    //DELETE LAST POSTED GAME
    private static ChainBuilder deleteLastPostedGame =
            exec(http("Delete Last Posted Game - #{name}")
                    .delete("/videogame/#{id}")
                    .header("Authorization", "Bearer #{authToken}")
                    .check(bodyString().is("Video game deleted"))); //Comprobamos que el mensaje de respuesta al borrar sea "Video game deleted"


    //Definicion del escenario
    //La definicion del escenario es para definir el flujo de la prueba
    private ScenarioBuilder scn = scenario("Video Game DB Stress Test")
            .exec(getAllVideoGames)
            .pause(2) //Pausa de 2 segundos
            .exec(authenticate)
            .pause(2)
            .exec(postNewGame)
            .pause(2)
            .exec(getLastPostedGame)
            .pause(2)
            .exec(deleteLastPostedGame);


    //Cargar simulacion
    //Hago un setUp para cargar la simulacion, inyectando un usuario al inicio
    {
        setUp(
                scn.injectOpen(
                        nothingFor(5), //Nada durante 5 segundos
                        rampUsers(USER_COUNT).during(RAMP_DURATION) //Rampa de usuarios, eso es
                )
        ).protocols(httpProtocol);
    }
}
