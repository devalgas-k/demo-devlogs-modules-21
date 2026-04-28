module com.example.demo.transaction {
    requires spring.boot;
    requires spring.boot.autoconfigure;
    requires spring.context;
    requires spring.web;
    requires spring.core;
    requires spring.beans;
    requires spring.tx;
    requires spring.data.jpa;
    requires spring.data.commons;
    requires jakarta.persistence;
    requires jakarta.validation;
    requires org.hibernate.orm.core;
    requires static lombok;
    requires org.slf4j;

    opens com.example.demo to spring.core, spring.beans, spring.context;
    opens com.example.demo.domain to org.hibernate.orm.core, spring.core, spring.beans, spring.data.commons;
    opens com.example.demo.repository to spring.core, spring.beans, spring.context, spring.data.commons;
    opens com.example.demo.service to spring.core, spring.beans, spring.context, spring.tx;
    opens com.example.demo.web to spring.core, spring.beans, spring.context, spring.web;

    exports com.example.demo;
    exports com.example.demo.domain;
    exports com.example.demo.repository;
    exports com.example.demo.service;
}
