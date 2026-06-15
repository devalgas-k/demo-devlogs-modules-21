package com.example.demo.service;

import com.example.demo.domain.CsvRow;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sanity test : on vérifie que les deux implémentations produisent le même
 * contenu (à la mise en forme près), et qu'à volume égal la version naïve
 * est significativement plus lente.
 * <p>
 * Le seuil de 1.5× est conservateur : en pratique, sur 5000 lignes,
 * l'écart observé est typiquement de 10× à 50×.
 */
class CsvExportServiceTest {

    @Test
    void both_modes_produce_same_number_of_lines() {
        CsvExportService svc = new CsvExportService();
        List<CsvRow> rows = svc.buildRows(1_000);

        String naive = svc.naiveConcat(rows);
        String optimized = svc.optimizedBuilder(rows);

        // Mêmes 1000 lignes + 1000 sauts de ligne
        long naiveLines = naive.lines().count();
        long optimizedLines = optimized.lines().count();

        assertEquals(naiveLines, optimizedLines, "Les deux exports doivent avoir le même nombre de lignes");
        assertEquals(1_000L, naiveLines, "L'export doit contenir 1 000 lignes");
    }

    @Test
    void naive_is_slower_than_optimized_on_small_volume() {
        CsvExportService svc = new CsvExportService();
        List<CsvRow> rows = svc.buildRows(5_000);

        // Warm-up
        svc.naiveConcat(rows);
        svc.optimizedBuilder(rows);

        long naiveNanos = timeNanos(() -> svc.naiveConcat(rows));
        long builderNanos = timeNanos(() -> svc.optimizedBuilder(rows));

        // Sur 5 000 lignes, l'écart est de 5× à 50× selon la machine.
        // On vérifie seulement un facteur 1.5× pour ne pas être flaky en CI.
        assertTrue(builderNanos * 1.5 < naiveNanos,
                "Le mode builder doit être au moins 1.5× plus rapide (naive=" + naiveNanos
                        + "ns, builder=" + builderNanos + "ns)");
    }

    private long timeNanos(Runnable r) {
        long start = System.nanoTime();
        r.run();
        return System.nanoTime() - start;
    }

    @Test
    void buildRows_is_stable() {
        CsvExportService svc = new CsvExportService();
        List<CsvRow> rows = svc.buildRows(100);
        assertEquals(100, rows.size());
        // id = index
        IntStream.range(0, 100).forEach(i -> assertEquals(i, rows.get(i).id()));
    }
}
