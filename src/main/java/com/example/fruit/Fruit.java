package com.example.fruit;

import java.util.List;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "fruits")
public class Fruit extends PanacheEntity {

    @Column(nullable = false, unique = true)
    public String name;

    public static Uni<List<Fruit>> findAllSorted() {
        return listAll(Sort.by("name"));
    }
}
