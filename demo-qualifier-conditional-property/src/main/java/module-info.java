module com.example.demo.qualifierconditional {
    requires spring.boot;
    requires spring.boot.autoconfigure;
    requires spring.context;
    requires spring.beans;
    requires spring.core;
    requires spring.web;
    requires jakarta.persistence;

    opens com.example.demo to spring.core, spring.beans, spring.context;
    opens com.example.demo.naive to spring.beans, spring.context;
    opens com.example.demo.optimized to spring.beans, spring.context;
    opens com.example.demo.web to spring.beans, spring.web, spring.context;

    exports com.example.demo;
    exports com.example.demo.web;
}
