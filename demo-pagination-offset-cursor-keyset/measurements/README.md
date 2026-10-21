# Mesure PostgreSQL, 21 octobre 2026

Source brute : [postgresql-17.9.txt](postgresql-17.9.txt), protocole : [benchmark.sql](../sql/benchmark.sql).

PostgreSQL 17.9 Debian aarch64, Docker Desktop 28.3.2 sur hôte macOS ARM. `shared_buffers=128MB`, `work_mem=4MB`. Conteneur jetable sans limite CPU/mémoire propre ; VM Docker partagée avec d'autres activités, 7 CPU virtuels et 2791366656 octets de RAM exposés par Docker (environ 2,60 GiB), stockage Docker local. Ce contexte n'est pas un laboratoire isolé. Un million de lignes, payload 100 caractères, timestamps groupés par 10, tenant 42, index composite couvrant les clés de sélection mais pas payload. VACUUM ANALYZE préalable.

| Scénario | Lignes émises par Index Scan | Buffers exécution hit + read | Temps exécution ms |
|---|---:|---:|---:|
| Offset 0, première page | 50 | 5 + 0 | 0.029 |
| Offset 900000, passage 1 | 900050 | 10179 + 15314 | 581.868 |
| Keyset frontière équivalente, passage 1 | 50 | 4 + 1 | 0.068 |
| Offset 900000, passage 2 | 900050 | 3809 + 21684 | 711.732 |
| Keyset, passage 2 | 50 | 4 + 1 | 0.041 |
| Offset 900000, passage 3 | 900050 | 1 + 25492 | 702.799 |
| Keyset, passage 3 | 50 | 3 + 2 | 0.046 |

Les nombres de buffers sont des accès, pas un décompte de pages uniques. Ne pas additionner les compteurs parents/enfants du plan. Les écritures visibles sur certains passages sont conservées dans la trace brute.

Deux lectures de préparation sélectionnent les identifiants avant les plans ; elles ne garantissent pas un cache chaud pour les requêtes complètes avec payload. Les lectures et écritures des plans montrent explicitement un état de cache mixte. Les trois passages alternent offset puis keyset : ils ne constituent ni distribution robuste ni comparaison randomisée. Aucun multiplicateur de vitesse universel ne doit être déduit de ces durées. Le résultat solide dans ce plan est le travail effectué : 900050 lignes parcourues pour en rendre 50, contre 50 après positionnement dans l'index. PostgreSQL peut choisir d'autres plans selon données, filtres, statistiques et projection.

Les deux requêtes renvoient exactement les mêmes 50 identifiants (`identical_pages=t`). La frontière est connue à l'avance par construction des données ; le coût d'un saut arbitraire pour découvrir cette frontière n'est pas inclus. Le keyset correspond à la continuation depuis une page précédente, pas à un accès gratuit à la page 18001. Les requêtes mesurées font LIMIT 50 ; le service Java ajoute une ligne de lookahead (LIMIT 51) pour hasNext. Aucun coût HTTP, token HMAC, JDBC, COUNT ou transaction longue n'est mesuré.
