package com.crio.qeats.services;

import com.crio.qeats.dto.Restaurant;
import com.crio.qeats.exchanges.GetRestaurantsRequest;
import com.crio.qeats.exchanges.GetRestaurantsResponse;
import com.crio.qeats.repositoryservices.RestaurantRepositoryService;
import java.time.LocalTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
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
}
