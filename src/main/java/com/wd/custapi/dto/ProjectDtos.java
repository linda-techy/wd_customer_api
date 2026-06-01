package com.wd.custapi.dto;

import java.time.LocalDate;

public class ProjectDtos {

    // Utility holder for nested DTO types — not meant to be instantiated.
    private ProjectDtos() {}

    public static class ProjectCard {
        public Long id;
        public String name;
        public String code;
        public String location;
        public LocalDate startDate;
        public LocalDate endDate;
    }

}


