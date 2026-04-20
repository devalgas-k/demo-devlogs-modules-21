module com.example.demo.crash {
    requires spring.boot;
    requires spring.boot.autoconfigure;
    requires spring.context;
    requires spring.web;
    requires spring.core;
    requires spring.beans;
    requires jakarta.validation;
    requires org.slf4j;

    opens com.example.demo.crash to spring.core, spring.beans, spring.context;
    opens com.example.demo.crash.config to spring.core, spring.beans, spring.context, spring.boot;
    opens com.example.demo.crash.runtime to spring.core, spring.beans, spring.context, spring.web;
    opens com.example.demo.crash.startup to spring.core, spring.beans, spring.context, spring.boot;
}
