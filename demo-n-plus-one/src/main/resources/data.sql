insert into article (id, title) values (1, 'Understanding JPA');
insert into article (id, title) values (2, 'Hibernate Performance');
insert into article (id, title) values (3, 'Spring Boot Tips');
insert into article (id, title) values (4, 'REST API Design');
insert into article (id, title) values (5, 'Database Indexing');

insert into category (id, name) values (1, 'JPA');
insert into category (id, name) values (2, 'Hibernate');
insert into category (id, name) values (3, 'Spring');
insert into category (id, name) values (4, 'Performance');
insert into category (id, name) values (5, 'Database');

insert into rel_article__category (article_id, category_id) values (1, 1);
insert into rel_article__category (article_id, category_id) values (1, 3);
insert into rel_article__category (article_id, category_id) values (2, 2);
insert into rel_article__category (article_id, category_id) values (2, 4);
insert into rel_article__category (article_id, category_id) values (3, 3);
insert into rel_article__category (article_id, category_id) values (3, 4);
insert into rel_article__category (article_id, category_id) values (4, 3);
insert into rel_article__category (article_id, category_id) values (4, 5);
insert into rel_article__category (article_id, category_id) values (5, 5);
insert into rel_article__category (article_id, category_id) values (5, 4);
