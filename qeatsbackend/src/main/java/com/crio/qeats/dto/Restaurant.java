
/*
 *
 *  * Copyright (c) Crio.Do 2019. All rights reserved
 *
 */

package com.crio.qeats.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// TODO: CRIO_TASK_MODULE_SERIALIZATION
//  Implement Restaurant class.
// Complete the class such that it produces the following JSON during serialization.
// {
//  "restaurantId": "10",
//  "name": "A2B",
//  "city": "Hsr Layout",
//  "imageUrl": "www.google.com",
//  "latitude": 20.027,
//  "longitude": 30.0,
//  "opensAt": "18:00",
//  "closesAt": "23:00",
//  "attributes": [
//    "Tamil",
//    "South Indian"
//  ]
// }


@Data
@AllArgsConstructor
@NoArgsConstructor
public class Restaurant {

    @JsonProperty("restaurantId")
    private String restaurantId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("city")
    private String city;

    @JsonProperty("imageUrl")
    private String imageUrl;

    @JsonProperty("latitude")
    private Double latitude;

    @JsonProperty("longitude")
    private Double longitude;
  
    @JsonProperty("opensAt")
    private String opensAt;

    @JsonProperty("closesAt")
    private String closesAt;

    @JsonProperty("attributes")
    private List<String> attributes;
  
    // public Restaurant() {
    // }
  
    // public Restaurant(String restaurantId, String name, String city, String imageUrl, Double latitude, Double longitude, String opensAt, String closesAt, List<String> attributes) {
    //   this.restaurantId = restaurantId;
    //   this.name = name;
    //   this.city = city;
    //   this.imageUrl = imageUrl;
    //   this.latitude = latitude;
    //   this.longitude = longitude;
    //   this.opensAt = opensAt;
    //   this.closesAt = closesAt;
    //   this.attributes = attributes;
    // }
    
    
}

