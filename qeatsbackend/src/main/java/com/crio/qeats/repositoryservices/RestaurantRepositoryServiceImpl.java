package com.crio.qeats.repositoryservices;

import ch.hsr.geohash.GeoHash;
import com.crio.qeats.dto.Restaurant;
import com.crio.qeats.globals.GlobalConstants;
import com.crio.qeats.models.RestaurantEntity;
import com.crio.qeats.repositories.RestaurantRepository;
import com.crio.qeats.utils.GeoLocation;
import com.crio.qeats.utils.GeoUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.inject.Provider;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

@Service
@Primary
@Slf4j
public class RestaurantRepositoryServiceImpl implements RestaurantRepositoryService {

  @Autowired
  private MongoTemplate mongoTemplate;

  @Autowired
  private Provider<ModelMapper> modelMapperProvider;

  @Autowired
  RestaurantRepository restaurantRepository;

  private boolean isOpenNow(LocalTime time, RestaurantEntity res) {
    LocalTime openingTime = LocalTime.parse(res.getOpensAt());
    LocalTime closingTime = LocalTime.parse(res.getClosesAt());

    return time.isAfter(openingTime) && time.isBefore(closingTime);
  }

  private String sanitizeRestaurantName(String name) {
    // Use regex to replace non-alphabetic characters with a space
    String sanitized = name.replaceAll("[^a-zA-Z ]", " ");

    // Replace multiple spaces with a single space and trim the result
    sanitized = sanitized.replaceAll("\\s+", " ").trim();

    return sanitized;
}

  @Override
  public List<Restaurant> findAllRestaurantsCloseBy(Double latitude,
      Double longitude, LocalTime currentTime, Double servingRadiusInKms) {

    log.info("findAllRestaurantsCloseBy called with latitude: {}, longitude: {}, currentTime: {}, servingRadiusInKms: {}",
        latitude, longitude, currentTime, servingRadiusInKms);

    List<Restaurant> restaurants = new ArrayList<>();
    List<RestaurantEntity> results = restaurantRepository.findAll();

    log.info("Total restaurants fetched from repository: {}", results.size());

    ModelMapper modelMapper = modelMapperProvider.get();
    for (RestaurantEntity res : results) {
      log.info("Checking if restaurant {} is close by and open", res.getRestaurantId());

      String sanitizedName = sanitizeRestaurantName(res.getName());
      res.setName(sanitizedName); // Update the name in entity if sanitized

      if (isRestaurantCloseByAndOpen(res, currentTime, latitude, longitude, servingRadiusInKms)) {
        Restaurant temp = modelMapper.map(res, Restaurant.class);
        restaurants.add(temp);
        log.info("Restaurant {} is added to the list", res.getRestaurantId());
      } else {
        log.info("Restaurant {} is not close by or not open", res.getRestaurantId());
      }
    }

    log.info("Total restaurants found close by and open: {}", restaurants.size());

    List<Restaurant> topRestaurants = restaurants.size() > 100 ? restaurants.subList(0, 100) : restaurants;
    return topRestaurants;
  }

  private boolean isRestaurantCloseByAndOpen(RestaurantEntity restaurantEntity,
      LocalTime currentTime, Double latitude, Double longitude, Double servingRadiusInKms) {
    if (isOpenNow(currentTime, restaurantEntity)) {
      double distance = GeoUtils.findDistanceInKm(latitude, longitude,
          restaurantEntity.getLatitude(), restaurantEntity.getLongitude());
      boolean isCloseBy = distance < servingRadiusInKms;

      log.info("Restaurant {} is open now. Distance from the user: {}. Is within serving radius: {}",
          restaurantEntity.getRestaurantId(), distance, isCloseBy);

      return isCloseBy;
    } else {
      log.info("Restaurant {} is not open now", restaurantEntity.getRestaurantId());
      return false;
    }
  }
}
