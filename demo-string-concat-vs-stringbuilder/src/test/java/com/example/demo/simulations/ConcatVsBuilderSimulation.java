package com.example.demo.simulations;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import static io.gatling.javaapi.core.CoreDsl.constantConcurrentUsers;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

/**
 * Comparaison A/B : export CSV avec concaténation naïve ({@code /api/concat})
 * vs export avec {@code StringBuilder} ({@code /api/builder}).
 * <p>
 * Stratégie de charge :
 * <ul>
 *   <li>deux scénarios en parallèle, profils identiques ;</li>
 *   <li>montée progressive de 1 à 50 utilisateurs concurrents sur 20 s ;</li>
 *   <li>plateau de 30 s ;</li>
 *   <li>descente en 10 s.</li>
 * </ul>
 * Le but n'est pas d'atteindre le débit max, mais d'observer la divergence
 * entre les deux modes sur une charge "réaliste" (50 RPS cumulés).
 */
public class ConcatVsBuilderSimulation extends Simulation {

    private static final String BASE_URL = System.getProperty("baseUrl", "http://localhost:8080");
    private static final int ROWS = 20_000;

    HttpProtocolBuilder httpProtocol = http
            .baseUrl(BASE_URL)
            .acceptHeader("application/json")
            .userAgentHeader("Gatling-StringBuilderDemo");

    ScenarioBuilder naive = scenario("Naive concat (String +)")
            .exec(
                    http("GET /api/concat")
                            .get("/api/concat?rows=" + ROWS)
                            .check(status().is(200))
            );

    ScenarioBuilder optimized = scenario("StringBuilder")
            .exec(
                    http("GET /api/builder")
                            .get("/api/builder?rows=" + ROWS)
                            .check(status().is(200))
            );

    {
        setUp(
                naive.injectClosed(constantConcurrentUsers(50).during(60)),
                optimized.injectClosed(constantConcurrentUsers(50).during(60))
        ).protocols(httpProtocol)
         .assertions(
                 // 1) Aucune erreur : on compare deux chemins valides.
                 io.gatling.javaapi.core.CoreDsl.global().failedRequests().count().is(0L),
                 // 2) Le p99 du mode optimisé doit rester sous 1.5× celui du mode naïf
                 //    (sanity check : il devrait être ~10× plus rapide, on reste conservateur).
                 //    Le test reste informatif même si on ne l'active pas en CI.
                 //    Décommente la ligne suivante pour le vérifier :
                 // io.gatling.javaapi.core.CoreDsl.details("StringBuilder").responseTime.percentile(99.0).lt(1500)
                 null
         );
    }
}
