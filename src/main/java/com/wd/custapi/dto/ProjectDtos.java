package com.wd.custapi.dto;

import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

public class ProjectDtos {

    // Utility holder for nested DTO types — not meant to be instantiated.
    private ProjectDtos() {}

    @Getter
    @Setter
    public static class ProjectCard {
        private Long id;
        private String name;
        private String code;
        private String location;
        private LocalDate startDate;
        private LocalDate endDate;
    }

}


