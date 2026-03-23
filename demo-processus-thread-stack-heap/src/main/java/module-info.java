module com.example.demo.processus.thread.stack.heap {
    requires spring.boot;
    requires spring.boot.autoconfigure;
    requires spring.context;
    requires spring.beans;
    requires spring.core;

    opens com.example.demo to spring.core, spring.beans, spring.context;
    exports com.example.demo;
    exports com.example.demo.service;
    exports com.example.demo.domain;
}
