module com.example.demo.nplusone {
    requires spring.boot;
    requires spring.boot.autoconfigure;
    requires spring.context;
    requires spring.beans;
    requires spring.core;
    requires spring.web;
    requires spring.tx;
    requires spring.aop;
    requires jakarta.persistence;
    requires static lombok;
    requires java.sql;
    requires java.naming;
    requires spring.data.jpa;
    requires spring.data.commons;
    requires org.hibernate.orm.core;

    opens com.example.demo to spring.core, spring.beans, spring.context;
    opens com.example.demo.domain to org.hibernate.orm.core, spring.core;
    opens com.example.demo.web to spring.beans, spring.web, spring.context;
    opens com.example.demo.service to spring.beans, spring.context, spring.aop, spring.tx, spring.core;

    exports com.example.demo;
    exports com.example.demo.domain;
    exports com.example.demo.web;
    exports com.example.demo.service;
} 
