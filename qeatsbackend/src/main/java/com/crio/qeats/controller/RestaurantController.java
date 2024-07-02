package com.crio.qeats.controller;

import com.crio.qeats.exchanges.GetRestaurantsRequest;
import com.crio.qeats.exchanges.GetRestaurantsResponse;
import com.crio.qeats.services.RestaurantService;
import java.time.LocalTime;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;

@RestController
@Slf4j
public class RestaurantController {

  public static final String RESTAURANT_API_ENDPOINT = "/qeats/v1";
  public static final String RESTAURANTS_API = "/restaurants";

  @Autowired
  private RestaurantService restaurantService;

  @GetMapping(RESTAURANT_API_ENDPOINT + RESTAURANTS_API)
  public ResponseEntity<GetRestaurantsResponse> getRestaurants(@Valid GetRestaurantsRequest rq) {
    log.info("getRestaurants called with {}", rq);

    if (rq.getLatitude() == null || rq.getLongitude() == null) {
      log.error("Invalid request parameters: Latitude or Longitude is null");
      return ResponseEntity.badRequest().build();
    }

    if (!isValidLatitude(rq.getLatitude()) || !isValidLongitude(rq.getLongitude())) {
      log.error("Invalid request parameters: Latitude or Longitude out of range");
      return ResponseEntity.badRequest().build();
    }

    try {
      GetRestaurantsResponse getRestaurantsResponse = restaurantService.findAllRestaurantsCloseBy(rq, LocalTime.now());
      log.info("getRestaurants returned {}", getRestaurantsResponse);
      return ResponseEntity.ok().body(getRestaurantsResponse);
    } catch (Exception e) {
      log.error("Exception occurred while getting restaurants", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  private boolean isValidLatitude(double latitude) {
    return latitude >= -90 && latitude <= 90;
  }

  private boolean isValidLongitude(double longitude) {
    return longitude >= -180 && longitude <= 180;
  }
}
