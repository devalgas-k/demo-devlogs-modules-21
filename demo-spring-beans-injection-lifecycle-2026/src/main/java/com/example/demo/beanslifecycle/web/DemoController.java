package com.example.demo.beanslifecycle.web;

import com.example.demo.beanslifecycle.config.DemoModeProperties;
import com.example.demo.beanslifecycle.config.InitTimingBeanPostProcessor;
import com.example.demo.beanslifecycle.domain.ExpensiveBean;
import com.example.demo.beanslifecycle.domain.ExpensiveUseCase;
import com.example.demo.beanslifecycle.domain.FixedClockReportService;
import com.example.demo.beanslifecycle.domain.ImportedIdService;
import com.example.demo.beanslifecycle.domain.BadPrototypeConsumer;
import com.example.demo.beanslifecycle.domain.GoodPrototypeConsumer;
import com.example.demo.beanslifecycle.domain.Token;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DemoController {

    private final DemoModeProperties properties;
    private final ExpensiveUseCase useCase;
    private final InitTimingBeanPostProcessor initTimingBeanPostProcessor;
    private final FixedClockReportService fixedClockReportService;
    private final ImportedIdService importedIdService;
    private final GoodPrototypeConsumer goodPrototypeConsumer;
    private final BadPrototypeConsumer badPrototypeConsumer;
    private final ApplicationContext applicationContext;

    public DemoController(
            DemoModeProperties properties,
            ExpensiveUseCase useCase,
            InitTimingBeanPostProcessor initTimingBeanPostProcessor,
            FixedClockReportService fixedClockReportService,
            ImportedIdService importedIdService,
            GoodPrototypeConsumer goodPrototypeConsumer,
            BadPrototypeConsumer badPrototypeConsumer,
            ApplicationContext applicationContext
    ) {
        this.properties = properties;
        this.useCase = useCase;
        this.initTimingBeanPostProcessor = initTimingBeanPostProcessor;
        this.fixedClockReportService = fixedClockReportService;
        this.importedIdService = importedIdService;
        this.goodPrototypeConsumer = goodPrototypeConsumer;
        this.badPrototypeConsumer = badPrototypeConsumer;
        this.applicationContext = applicationContext;
    }

    @GetMapping("/mode")
    public ModeResponse mode() {
        return new ModeResponse(properties.mode(), ExpensiveBean.constructionCount());
    }

    @GetMapping("/expensive/run")
    public ExpensiveRunResponse run(@RequestParam(defaultValue = "42") long input) {
        int before = ExpensiveBean.constructionCount();
        ExpensiveUseCase.RunResult result = useCase.run(input);
        int after = ExpensiveBean.constructionCount();
        return new ExpensiveRunResponse(result, before, after);
    }

    @GetMapping("/beans/init-timing")
    public List<InitTimingBeanPostProcessor.BeanInitTiming> initTiming(@RequestParam(defaultValue = "20") int top) {
        return initTimingBeanPostProcessor.top(top);
    }

    @GetMapping("/clock/fixed")
    public Instant fixedClockNow() {
        return fixedClockReportService.now();
    }

    @GetMapping("/imported/new-id")
    public String importedNewId() {
        return importedIdService.newId();
    }

    @GetMapping("/prototype/good")
    public UUID prototypeGood() {
        return goodPrototypeConsumer.newWidgetId();
    }

    @GetMapping("/prototype/bad")
    public UUID prototypeBad() {
        return badPrototypeConsumer.widgetId();
    }

    @GetMapping("/factory-bean/token")
    public TokenResponse tokenFromFactoryBean() {
        Token token = applicationContext.getBean("demoToken", Token.class);
        Object factoryBean = applicationContext.getBean("&demoToken");
        return new TokenResponse(token.value(), token.getClass().getName(), factoryBean.getClass().getName());
    }

    public record ModeResponse(String mode, int expensiveBeanConstructionCount) {
    }

    public record ExpensiveRunResponse(ExpensiveUseCase.RunResult result, int constructionCountBefore, int constructionCountAfter) {
    }

    public record TokenResponse(String tokenValue, String tokenBeanClass, String factoryBeanClass) {
    }
}
