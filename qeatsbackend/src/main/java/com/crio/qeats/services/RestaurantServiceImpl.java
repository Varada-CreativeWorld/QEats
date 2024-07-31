
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
import java.util.concurrent.CompletableFuture;
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

        Double servingRadiusInKms = isPeakHour(currentTime) ? peakHoursServingRadiusInKms : normalHoursServingRadiusInKms;
        String searchFor = getRestaurantsRequest.getSearchFor();
        List<List<Restaurant>> listOfRestaurantLists = new ArrayList<>();
        if (!searchFor.isEmpty()) {
          listOfRestaurantLists.add(restaurantRepositoryService.findRestaurantsByName(getRestaurantsRequest.getLatitude(),getRestaurantsRequest.getLongitude(), searchFor, currentTime,servingRadiusInKms));

          listOfRestaurantLists.add(restaurantRepositoryService.findRestaurantsByAttributes(getRestaurantsRequest.getLatitude(),getRestaurantsRequest.getLongitude(), searchFor,currentTime, servingRadiusInKms));
          listOfRestaurantLists.add(restaurantRepositoryService.findRestaurantsByItemName(getRestaurantsRequest.getLatitude(),getRestaurantsRequest.getLongitude(), searchFor,currentTime, servingRadiusInKms));

          listOfRestaurantLists.add(restaurantRepositoryService.findRestaurantsByItemName(getRestaurantsRequest.getLatitude(),getRestaurantsRequest.getLongitude(), searchFor,currentTime, servingRadiusInKms));
          listOfRestaurantLists.add(restaurantRepositoryService.findRestaurantsByItemAttributes(getRestaurantsRequest.getLatitude(),getRestaurantsRequest.getLongitude(), searchFor,currentTime, servingRadiusInKms));
          Set<String> restaurantSet = new HashSet<>();
          List<Restaurant> restaurantList = new ArrayList<>();
          for (List<Restaurant> restoList : listOfRestaurantLists) {
            for (Restaurant restaurant : restoList) {
              if (!restaurantSet.contains(restaurant.getRestaurantId())) {
                restaurantList.add(restaurant);
                restaurantSet.add(restaurant.getRestaurantId());
              }
            }
          }
          return new GetRestaurantsResponse(restaurantList);
      } else {
        return new GetRestaurantsResponse(new ArrayList<>());
      }

  }

  private boolean isTimeWithInRange(LocalTime timeNow, LocalTime startTime, LocalTime endTime) {
    return timeNow.isAfter(startTime) && timeNow.isBefore(endTime);
  }
  
  public boolean isPeakHour(LocalTime timeNow) {
    return isTimeWithInRange(timeNow, LocalTime.of(7, 59, 59), LocalTime.of(10, 00, 01)) || isTimeWithInRange(timeNow, LocalTime.of(12, 59, 59), LocalTime.of(14, 00, 01)) || isTimeWithInRange(timeNow, LocalTime.of(18, 59, 59),LocalTime.of(21, 00, 01));
  }


  // TODO: CRIO_TASK_MODULE_MULTITHREADING
  // Implement multi-threaded version of RestaurantSearch.
  // Implement variant of findRestaurantsBySearchQuery which is at least 1.5x time faster than
  // findRestaurantsBySearchQuery.
  @Override
  public GetRestaurantsResponse findRestaurantsBySearchQueryMt(GetRestaurantsRequest getRestaurantsRequest, LocalTime currentTime) {

    Double servingRadiusInKms = isPeakHour(currentTime) ? peakHoursServingRadiusInKms : normalHoursServingRadiusInKms;
    String searchFor = getRestaurantsRequest.getSearchFor();

    if (searchFor.isEmpty()) {
      return new GetRestaurantsResponse(new ArrayList<>());
    }

    // Execute queries asynchronously
    CompletableFuture<List<Restaurant>> byNameFuture = CompletableFuture.supplyAsync(() ->
      restaurantRepositoryService.findRestaurantsByName(getRestaurantsRequest.getLatitude(), getRestaurantsRequest.getLongitude(), searchFor, currentTime, servingRadiusInKms)
    );

    CompletableFuture<List<Restaurant>> byAttributesFuture = CompletableFuture.supplyAsync(() ->
      restaurantRepositoryService.findRestaurantsByAttributes(getRestaurantsRequest.getLatitude(), getRestaurantsRequest.getLongitude(), searchFor, currentTime, servingRadiusInKms)
    );

    CompletableFuture<List<Restaurant>> byItemNameFuture = CompletableFuture.supplyAsync(() ->
      restaurantRepositoryService.findRestaurantsByItemName(getRestaurantsRequest.getLatitude(), getRestaurantsRequest.getLongitude(), searchFor, currentTime, servingRadiusInKms)
    );

    CompletableFuture<List<Restaurant>> byItemAttributesFuture = CompletableFuture.supplyAsync(() ->
      restaurantRepositoryService.findRestaurantsByItemAttributes(getRestaurantsRequest.getLatitude(), getRestaurantsRequest.getLongitude(), searchFor, currentTime, servingRadiusInKms)
    );

    // Combine all futures
    CompletableFuture<Void> allFutures = CompletableFuture.allOf(
      byNameFuture, byAttributesFuture, byItemNameFuture, byItemAttributesFuture
    );

    // Gather results
    List<Restaurant> combinedResults = new ArrayList<>();
    Set<String> restaurantSet = new HashSet<>();

    try {
      allFutures.get();

      addToRestaurantList(byNameFuture.get(), combinedResults, restaurantSet);
      addToRestaurantList(byAttributesFuture.get(), combinedResults, restaurantSet);
      addToRestaurantList(byItemNameFuture.get(), combinedResults, restaurantSet);
      addToRestaurantList(byItemAttributesFuture.get(), combinedResults, restaurantSet);

    } catch (InterruptedException | ExecutionException e) {
      log.error("Exception occurred while fetching restaurants asynchronously", e);
    }

    return new GetRestaurantsResponse(combinedResults);
  }

  private void addToRestaurantList(List<Restaurant> restaurants, List<Restaurant> combinedResults, Set<String> restaurantSet) {
    for (Restaurant restaurant : restaurants) {
      if (!restaurantSet.contains(restaurant.getRestaurantId())) {
        combinedResults.add(restaurant);
        restaurantSet.add(restaurant.getRestaurantId());
      }
    }
  }
}

