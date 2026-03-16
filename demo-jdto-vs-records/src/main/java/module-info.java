module com.example.demo.jdto {
    requires spring.boot;
    requires spring.boot.autoconfigure;
    requires spring.context;
    requires spring.beans;
    requires spring.core;

    exports com.example.demo.dto;
    opens com.example.demo to spring.core, spring.beans, spring.context;
}
