package com.crio.qeats.controller;

import com.crio.qeats.dto.Restaurant;
import com.crio.qeats.exchanges.GetRestaurantsRequest;
import com.crio.qeats.exchanges.GetRestaurantsResponse;
import com.crio.qeats.services.RestaurantService;
import java.text.Normalizer;
import java.time.LocalTime;
import java.util.List;
import java.util.regex.Pattern;
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
  public static final String MENU_API = "/menu";
  public static final String CART_API = "/cart";
  public static final String CART_ITEM_API = "/cart/item";
  public static final String CART_CLEAR_API = "/cart/clear";
  public static final String POST_ORDER_API = "/order";
  public static final String GET_ORDERS_API = "/orders";

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
      
      // Sanitize restaurant names
      getRestaurantsResponse.getRestaurants().forEach(restaurant -> {
        String sanitized = sanitizeRestaurantName(restaurant.getName());
        restaurant.setName(sanitized);
      });

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

  private String sanitizeRestaurantName(String name) {
    // Normalize the string to decompose special characters
    String normalized = Normalizer.normalize(name, Normalizer.Form.NFD);
    log.info("Normalized: {}", normalized); // Debug log for normalized string

    // Remove all combining diacritical marks
    Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    String withoutDiacritics = pattern.matcher(normalized).replaceAll("");
    log.info("Without Diacritics: {}", withoutDiacritics); // Debug log for string without diacritics

    // Remove all non-alphabetic characters
    String sanitized = withoutDiacritics.replaceAll("[^a-zA-Z ]", " ");
    log.info("After Removing Non-Alphabetic Characters: {}", sanitized); // Debug log for sanitized string

    // Trim extra spaces
    sanitized = sanitized.replaceAll("\\s+", " ").trim();
    log.info("Final Sanitized: {}", sanitized); // Debug log for final sanitized string

    return sanitized;
}

}
