package com.wd.custapi.dto;

import java.time.LocalDate;

public class ProjectDtos {

    // Utility holder for nested DTO types — not meant to be instantiated.
    private ProjectDtos() {}

    public static class ProjectCard {
        private Long id;
        private String name;
        private String code;
        private String location;
        private LocalDate startDate;
        private LocalDate endDate;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }

        public LocalDate getStartDate() {
            return startDate;
        }

        public void setStartDate(LocalDate startDate) {
            this.startDate = startDate;
        }

        public LocalDate getEndDate() {
            return endDate;
        }

        public void setEndDate(LocalDate endDate) {
            this.endDate = endDate;
        }
    }

}


