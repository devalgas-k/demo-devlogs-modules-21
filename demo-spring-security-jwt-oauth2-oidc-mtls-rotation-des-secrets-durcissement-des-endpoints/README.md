# Demo — Spring Security (mode A vs mode B)

Cette démo illustre un compromis classique :

- **Mode A (non optimisé)** : validation “type introspection” (opaque token) + latence simulée + aucun cache.
- **Mode B (optimisé)** : validation **JWT signée localement** (RS256), support de **rotation de clés** et checks de base (issuer, audience, scopes).

## Prérequis

- Java 21
- Maven 3.9+

## Build & tests

```bash
mvn -q test
```

## Lancer l’appli

### Mode A — introspection lente

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=mode-a
```

### Mode A (cached) — introspection + cache local (anti-latence)

Même principe que le mode A, mais avec cache in-memory (TTL configurable).

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=mode-a-cached
```

Obtenir un token opaque :

```bash
curl "http://localhost:8080/auth/opaque?sub=alice&scope=read"
```

Appeler l’endpoint protégé :

```bash
curl -H "Authorization: Bearer OPAQUE_TOKEN" "http://localhost:8080/api/secure"
```

### Mode B — JWT (RS256) + rotation

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=mode-b
```

Obtenir un JWT :

```bash
curl "http://localhost:8080/auth/jwt?sub=alice&scope=read"
```

Appeler l’endpoint protégé :

```bash
curl -H "Authorization: Bearer JWT_TOKEN" "http://localhost:8080/api/secure"
```

Scopes multiples (séparés par des espaces) :

```bash
curl "http://localhost:8080/auth/jwt?sub=alice&scope=read%20admin"
```

JWT sans `kid` (le resource server doit essayer plusieurs clés) :

```bash
curl "http://localhost:8080/auth/jwt?sub=alice&scope=read&noKid=true"
```

Not-before dans le futur (attendu: 401 tant que `nbf` n’est pas atteint) :

```bash
JWT="$(curl -s "http://localhost:8080/auth/jwt?sub=alice&scope=read&nbfSecondsInFuture=3600")"
curl -i -H "Authorization: Bearer $JWT" "http://localhost:8080/api/secure"
```

Clock skew (tolérance de dérive d’horloge) :

- Modifier `demo.security.clockSkewSeconds` dans `src/main/resources/application.yml`
- Relancer l’app et rejouer un cas `nbf` court (ex: 5–10s) pour voir quand ça bascule 401 → 200

Endpoint admin (requiert `SCOPE_admin`) :

```bash
curl -H "Authorization: Bearer JWT_TOKEN" "http://localhost:8080/api/admin"
```

Rotation de clé (les anciens tokens restent valides tant qu’ils ne sont pas expirés) :

```bash
curl -X POST "http://localhost:8080/auth/rotate"
```

## Scénarios “qui cassent” (testables)

Audience invalide (attendu: 401) :

```bash
JWT="$(curl -s "http://localhost:8080/auth/jwt?sub=alice&scope=read&aud=api://wrong")"
curl -i -H "Authorization: Bearer $JWT" "http://localhost:8080/api/secure"
```

Issuer invalide (attendu: 401) :

```bash
JWT="$(curl -s "http://localhost:8080/auth/jwt?sub=alice&scope=read&iss=https://issuer.wrong.example")"
curl -i -H "Authorization: Bearer $JWT" "http://localhost:8080/api/secure"
```

Token expiré (attendu: 401) :

```bash
JWT="$(curl -s "http://localhost:8080/auth/jwt?sub=alice&scope=read&ttlSeconds=10&iatSecondsAgo=3600")"
curl -i -H "Authorization: Bearer $JWT" "http://localhost:8080/api/secure"
```

Scope manquant (attendu: 403) :

```bash
JWT="$(curl -s "http://localhost:8080/auth/jwt?sub=alice&scope=admin")"
curl -i -H "Authorization: Bearer $JWT" "http://localhost:8080/api/secure"
```

## Observabilité minimale

- Stats applicatives : `GET /auth/stats`
- Actuator : `GET /actuator/health`

## Quand ne pas conclure “JWT > introspection”

- Si vous devez **révoquer immédiatement** un token (revocation), l’introspection (ou un store côté serveur) redevient souvent nécessaire.
- Si votre modèle d’autorisation dépend de données très dynamiques (ABAC), la validation locale ne suffit pas.
