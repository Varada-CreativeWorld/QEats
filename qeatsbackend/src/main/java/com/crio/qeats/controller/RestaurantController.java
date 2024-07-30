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
import io.micrometer.core.instrument.util.StringUtils;
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
  public ResponseEntity<GetRestaurantsResponse> getRestaurants(
      @Valid GetRestaurantsRequest getRestaurantsRequest) {
    log.info("getRestaurants called with {}", getRestaurantsRequest);
    GetRestaurantsResponse getRestaurantsResponse;
    if (getRestaurantsRequest.getLatitude() != null && getRestaurantsRequest.getLongitude() != null
        && getRestaurantsRequest.getLatitude() >= -90 && getRestaurantsRequest.getLatitude() <= 90
        && getRestaurantsRequest.getLongitude() >= -180
        && getRestaurantsRequest.getLongitude() <= 180) {
      List<Restaurant> restaurants;
      // if (!StringUtils.isEmpty(getRestaurantsRequest.getSearchFor())) {
      if (getRestaurantsRequest.getSearchFor() != null
          && !getRestaurantsRequest.getSearchFor().isEmpty()) {
        getRestaurantsResponse =
            restaurantService.findRestaurantsBySearchQuery(getRestaurantsRequest, LocalTime.now());
        log.info("getRestaurants returned {}", getRestaurantsResponse);
        // restaurants = getRestaurantsResponse.getRestaurants();
      } else {
        getRestaurantsResponse =
            restaurantService.findAllRestaurantsCloseBy(getRestaurantsRequest, LocalTime.now());
        // restaurants = getRestaurantsResponse.getRestaurants();
      }
      if (getRestaurantsResponse != null && !getRestaurantsResponse.getRestaurants().isEmpty()) {
        restaurants = getRestaurantsResponse.getRestaurants();

        for (int i = 0; i < restaurants.size(); i++) {
          restaurants.get(i).setName(restaurants.get(i).getName().replace("é", "?"));
        }
        log.info("getRestaurants returned {}", getRestaurantsResponse);
        return ResponseEntity.ok().body(getRestaurantsResponse);
      } else {
        return new ResponseEntity<>(HttpStatus.OK);
      }

    }

    else
    {
      return ResponseEntity.badRequest().body(null);
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

    // Remove all combining diacritical marks
    Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    String withoutDiacritics = pattern.matcher(normalized).replaceAll("");

    // Remove all non-alphabetic characters
    String sanitized = withoutDiacritics.replaceAll("[^a-zA-Z ]", "");

    // Trim extra spaces
    sanitized = sanitized.replaceAll("\\s+", " ").trim();

    return sanitized;
  }

}
