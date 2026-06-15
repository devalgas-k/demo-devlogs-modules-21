package com.example.demo.service;

import com.example.demo.domain.CsvRow;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

/**
 * Service qui produit un export CSV en mémoire.
 * <p>
 * Deux implémentations équivalentes côté résultat, mais radicalement
 * différentes côté coût CPU + GC :
 * <ul>
 *   <li>{@link #naiveConcat(List)} — utilise {@code String +=} dans une boucle.
 *       En Java, l'opérateur {@code +=} sur un {@code String} compile
 *       en {@code StringBuilder.append(...).toString()}, mais il crée un
 *       <strong>nouveau</strong> builder à chaque tour de boucle, et un
 *       nouveau {@code String} à chaque {@code toString()}. Sur N itérations
 *       on a donc N²/2 caractères copiés en moyenne et O(N) objets créés.</li>
 *   <li>{@link #optimizedBuilder(List)} — utilise un seul {@code StringBuilder}
 *       réutilisé et {@code setLength(0)} entre les passes. O(N) caractères
 *       copiés et un seul objet alloué par l'export.</li>
 * </ul>
 */
@Service
public class CsvExportService {

    /** Construit un jeu de données de démonstration. */
    public List<CsvRow> buildRows(int n) {
        List<CsvRow> rows = new ArrayList<>(n);
        RandomGenerator rng = RandomGenerator.of("L64X128MixRandom");
        for (int i = 0; i < n; i++) {
            rows.add(new CsvRow(i, rng.nextDouble(1_000_000.0)));
        }
        return rows;
    }

    /**
     * Implémentation naïve : concaténation dans une boucle.
     * <p>
     * Ce qu'il se passe réellement (décompilation) :
     * <pre>{@code
     * for (CsvRow row : rows) {
     *     String tmp = sb.toString();        // copie 1
     *     sb = new StringBuilder(tmp + row); // copie 2
     *     // + une 3ᵉ copie implicite à cause de String + String
     * }
     * }</pre>
     * Soit, pour N lignes, O(N²) copies de caractères.
     */
    public String naiveConcat(List<CsvRow> rows) {
        String csv = "";
        for (CsvRow row : rows) {
            csv += row.toCsvCell() + "\n";
        }
        return csv;
    }

    /**
     * Implémentation optimisée : un seul {@code StringBuilder}, capacité pré-dimensionnée.
     * <p>
     * On évite la double peine (création du builder + copie à {@code toString()})
     * en ne touchant jamais à {@code toString()} pendant la boucle.
     * Coût total : O(N) copies, et le {@code String} final est créé une seule fois
     * (à l'appel de {@code toString()} hors boucle).
     */
    public String optimizedBuilder(List<CsvRow> rows) {
        // Heuristique grossière : 32 caractères par ligne.
        // StringBuilder(int) n'accepte qu'un int : on caste après avoir borné.
        int initialCapacity = (int) Math.min((long) Integer.MAX_VALUE, (long) rows.size() * 32L);
        StringBuilder sb = new StringBuilder(initialCapacity);
        for (CsvRow row : rows) {
            sb.append(row.toCsvCell()).append('\n');
        }
        return sb.toString();
    }
}
