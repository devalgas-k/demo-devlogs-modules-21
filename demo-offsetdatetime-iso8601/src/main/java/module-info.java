module com.example.demo.offsetdatetime {
    requires spring.boot;
    requires spring.boot.autoconfigure;
    requires spring.context;
    requires spring.beans;
    requires spring.core;
    requires spring.web;
    requires jakarta.persistence;
    requires static lombok;
    requires java.sql;
    requires spring.data.jpa;
    requires spring.data.commons;
    requires org.hibernate.orm.core;

    opens com.example.demo to spring.core, spring.beans, spring.context;
    opens com.example.demo.domain to org.hibernate.orm.core, spring.core;
    opens com.example.demo.web to spring.beans, spring.web, spring.context;

    exports com.example.demo;
    exports com.example.demo.domain;
    exports com.example.demo.web;
}
