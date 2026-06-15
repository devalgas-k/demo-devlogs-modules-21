package com.example.demo.web;

import com.example.demo.domain.CsvRow;
import com.example.demo.service.CsvExportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Expose les deux variantes d'export CSV pour comparaison Gatling.
 * <p>
 * Les deux endpoints reçoivent le même paramètre {@code rows} (par défaut 20 000),
 * produisent le même contenu, mais l'un coûte ~10× plus cher à JVM + GC que l'autre.
 */
@RestController
@RequestMapping("/api")
public class CsvExportController {

    /** Valeur par défaut : 20 000 lignes, taille typique d'un export "page" en B2B. */
    private static final int DEFAULT_ROWS = 20_000;

    private final CsvExportService service;

    public CsvExportController(CsvExportService service) {
        this.service = service;
    }

    /**
     * Variante "naïve" : concaténation dans une boucle.
     * <p>
     * Anti-pattern pédagogique. En production, JAMAIS sur des volumes > 100 lignes.
     */
    @GetMapping("/concat")
    public Map<String, Object> naive(@RequestParam(defaultValue = "" + DEFAULT_ROWS) int rows) {
        List<CsvRow> data = service.buildRows(rows);
        String csv = service.naiveConcat(data);
        return Map.of(
                "mode", "naive_concat",
                "rows", rows,
                "bytes", csv.getBytes().length
        );
    }

    /**
     * Variante "optimisée" : StringBuilder réutilisé.
     * <p>
     * La bonne pratique, équivalente fonctionnellement à {@code /concat}.
     */
    @GetMapping("/builder")
    public Map<String, Object> optimized(@RequestParam(defaultValue = "" + DEFAULT_ROWS) int rows) {
        List<CsvRow> data = service.buildRows(rows);
        String csv = service.optimizedBuilder(data);
        return Map.of(
                "mode", "string_builder",
                "rows", rows,
                "bytes", csv.getBytes().length
        );
    }
}
