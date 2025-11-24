package com.wd.custapi.dto;

import java.time.LocalDate;

public class ProjectDtos {

    public static class ProjectCard {
        public Long id;
        public String name;
        public String code;
        public String location;
        public LocalDate startDate;
        public LocalDate endDate;
    }

}


