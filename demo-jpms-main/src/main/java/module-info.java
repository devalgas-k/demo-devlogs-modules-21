module com.example.main {
    requires com.example.demo.jdto;
    requires spring.boot;
    requires spring.boot.autoconfigure;
    requires spring.context;
    requires spring.beans;
    requires spring.core;

    opens com.example.main to spring.core, spring.beans, spring.context;
}
