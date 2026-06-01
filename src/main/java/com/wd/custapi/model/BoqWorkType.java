package com.wd.custapi.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "boq_work_types")
@Getter
@Setter
public class BoqWorkType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    // Constructors
    public BoqWorkType() {}

    public BoqWorkType(String name, String description, Integer displayOrder) {
        this.name = name;
        this.description = description;
        this.displayOrder = displayOrder;
    }
}
