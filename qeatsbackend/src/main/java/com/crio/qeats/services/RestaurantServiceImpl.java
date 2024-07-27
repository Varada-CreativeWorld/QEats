
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
import java.time.LocalTime;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class RestaurantServiceImpl implements RestaurantService {

  private final Double peakHoursServingRadiusInKms = 3.0;
  private final Double normalHoursServingRadiusInKms = 5.0;

  @Autowired
  private RestaurantRepositoryService restaurantRepositoryService;

  @Override
  public GetRestaurantsResponse findAllRestaurantsCloseBy(
      GetRestaurantsRequest getRestaurantsRequest, LocalTime currentTime) {

    log.info("findAllRestaurantsCloseBy called with request: {} at time: {}", getRestaurantsRequest, currentTime);

    boolean isPeakHours = isPeakHours(currentTime);
    log.info("isPeakHours result: {}", isPeakHours);

    double serviceRadius = isPeakHours ? peakHoursServingRadiusInKms : normalHoursServingRadiusInKms;

    try {
      log.info("Calling restaurantRepositoryService with latitude: {}, longitude: {}, currentTime: {}, serviceRadius: {}",
               getRestaurantsRequest.getLatitude(), getRestaurantsRequest.getLongitude(), currentTime, serviceRadius);

      List<Restaurant> restaurants = restaurantRepositoryService.findAllRestaurantsCloseBy(
          getRestaurantsRequest.getLatitude(), getRestaurantsRequest.getLongitude(), currentTime, serviceRadius);

      log.info("restaurantRepositoryService returned: {}", restaurants);

      GetRestaurantsResponse response = new GetRestaurantsResponse();
      response.setRestaurants(restaurants);
      log.info("findAllRestaurantsCloseBy returning response: {}", response);

      return response;
    } catch (Exception e) {
      log.error("Exception occurred while fetching restaurants", e);
      throw e;
    }
  }

  private boolean isPeakHours(LocalTime currentTime) {
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



  // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
  // Implement findRestaurantsBySearchQuery. The request object has the search string.
  // We have to combine results from multiple sources:
  // 1. Restaurants by name (exact and inexact)
  // 2. Restaurants by cuisines (also called attributes)
  // 3. Restaurants by food items it serves
  // 4. Restaurants by food item attributes (spicy, sweet, etc)
  // Remember, a restaurant must be present only once in the resulting list.
  // Check RestaurantService.java file for the interface contract.
  @Override
  public GetRestaurantsResponse findRestaurantsBySearchQuery(
      GetRestaurantsRequest getRestaurantsRequest, LocalTime currentTime) {

     return null;
  }

}

