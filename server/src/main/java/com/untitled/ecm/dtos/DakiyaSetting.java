package com.untitled.ecm.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DakiyaSetting implements java.io.Serializable {
    String key;
    String value;
}
