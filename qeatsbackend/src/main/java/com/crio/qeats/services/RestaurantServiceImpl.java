
/*
 *
 *  * Copyright (c) Crio.Do 2019. All rights reserved
 *
 */

package com.crio.qeats.services;

import com.crio.qeats.dto.Restaurant;
import com.crio.qeats.exchanges.GetRestaurantsRequest;
import com.crio.qeats.exchanges.GetRestaurantsResponse;
import com.crio.qeats.repositoryservices.RestaurantRepositoryService;
import com.crio.qeats.repositoryservices.RestaurantRepositoryServiceDummyImpl;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Log4j2
public class RestaurantServiceImpl implements RestaurantService {

  private final Double peakHoursServingRadiusInKms = 3.0;
  private final Double normalHoursServingRadiusInKms = 5.0;
  @Autowired
  private RestaurantRepositoryService restaurantRepositoryService = new RestaurantRepositoryServiceDummyImpl();


  // TODO: CRIO_TASK_MODULE_RESTAURANTSAPI - Implement findAllRestaurantsCloseby.
  // Check RestaurantService.java file for the interface contract.
  @Override
  public GetRestaurantsResponse findAllRestaurantsCloseBy(
      GetRestaurantsRequest getRestaurantsRequest, LocalTime currentTime) {

      boolean isPeakHours = isPeakHours(currentTime);
      double serviceRadius = isPeakHours ? peakHoursServingRadiusInKms : normalHoursServingRadiusInKms;
      
      List<Restaurant> restaurants = restaurantRepositoryService.findAllRestaurantsCloseBy(getRestaurantsRequest.getLatitude(), getRestaurantsRequest.getLongitude(), currentTime, serviceRadius);
      GetRestaurantsResponse response = new GetRestaurantsResponse();
      response.setRestaurants(restaurants);
      return response;
  }


  private boolean isPeakHours(LocalTime currentTime) {
    // Define peak hours: 8AM - 10AM, 1PM-2PM, 7PM-9PM
    LocalTime peakStartMorning = LocalTime.of(8, 0);
    LocalTime peakEndMorning = LocalTime.of(10, 0);
    LocalTime peakStartAfternoon = LocalTime.of(13, 0);
    LocalTime peakEndAfternoon = LocalTime.of(14, 0);
    LocalTime peakStartEvening = LocalTime.of(19, 0);
    LocalTime peakEndEvening = LocalTime.of(21, 0);

    return (currentTime.compareTo(peakStartMorning) >= 0 && currentTime.compareTo(peakEndMorning) <= 0) ||
           (currentTime.compareTo(peakStartAfternoon) >= 0 && currentTime.compareTo(peakEndAfternoon) <= 0) ||
           (currentTime.compareTo(peakStartEvening) >= 0 && currentTime.compareTo(peakEndEvening) <= 0);
}


}

