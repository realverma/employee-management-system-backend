package com.harsh.employee.cache;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ActiveWorkerCache implements Serializable {
    private Long workerId;
    private String workerName;
    private Long siteId;
    private String siteName;
    private String clockInTime;

    // Getters, Setters, Constructors
}