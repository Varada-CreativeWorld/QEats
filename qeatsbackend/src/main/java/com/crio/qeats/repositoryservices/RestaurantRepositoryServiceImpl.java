package com.crio.qeats.repositoryservices;

import ch.hsr.geohash.GeoHash;
import com.crio.qeats.configs.RedisConfiguration;
import com.crio.qeats.dto.Item;
import com.crio.qeats.dto.Restaurant;
import com.crio.qeats.globals.GlobalConstants;
import com.crio.qeats.models.ItemEntity;
import com.crio.qeats.models.MenuEntity;
import com.crio.qeats.models.RestaurantEntity;
import com.crio.qeats.repositories.ItemRepository;
import com.crio.qeats.repositories.MenuRepository;
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
import redis.clients.jedis.Jedis;

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

  @Autowired
  MenuRepository menuRepository;

  @Autowired
  private RedisConfiguration redisConfiguration;

  @Autowired
  private ObjectMapper objectMapper;

  private boolean isOpenNow(LocalTime time, RestaurantEntity res) {
    LocalTime openingTime = LocalTime.parse(res.getOpensAt());
    LocalTime closingTime = LocalTime.parse(res.getClosesAt());

    return time.isAfter(openingTime) && time.isBefore(closingTime);
  }

  @Override
  public List<Restaurant> findAllRestaurantsCloseBy(Double latitude,
      Double longitude, LocalTime currentTime, Double servingRadiusInKms) {

    log.info("findAllRestaurantsCloseBy called with latitude: {}, longitude: {}, currentTime: {}, servingRadiusInKms: {}",
        latitude, longitude, currentTime, servingRadiusInKms);

    GeoHash geoHash = GeoHash.withCharacterPrecision(latitude, longitude, 7);
    String cacheKey = geoHash.toBase32();
    List<Restaurant> restaurants = new ArrayList<>();

    try (Jedis jedis = redisConfiguration.getJedisPool().getResource()) {
      // Check cache first
      String cachedData = jedis.get(cacheKey);
      if (cachedData != null) {
        log.info("Cache hit for key: {}", cacheKey);
        restaurants = objectMapper.readValue(cachedData, new TypeReference<List<Restaurant>>() {});
        return restaurants;
      } else {
        log.info("Cache miss for key: {}", cacheKey);
      }

      // Fetch from repository if not in cache
      List<RestaurantEntity> results = restaurantRepository.findAll();
      log.info("Total restaurants fetched from repository: {}", results.size());

      ModelMapper modelMapper = modelMapperProvider.get();
      for (RestaurantEntity res : results) {
        log.info("Checking if restaurant {} is close by and open", res.getRestaurantId());

        if (isRestaurantCloseByAndOpen(res, currentTime, latitude, longitude, servingRadiusInKms)) {
          Restaurant temp = modelMapper.map(res, Restaurant.class);
          restaurants.add(temp);
          log.info("Restaurant {} is added to the list", res.getRestaurantId());
        } else {
          log.info("Restaurant {} is not close by or not open", res.getRestaurantId());
        }
      }

      log.info("Total restaurants found close by and open: {}", restaurants.size());

      // Cache the result
      jedis.setex(cacheKey, RedisConfiguration.REDIS_ENTRY_EXPIRY_IN_SECONDS, objectMapper.writeValueAsString(restaurants));
    } catch (IOException e) {
      log.error("Error processing JSON for cache", e);
    }

    return restaurants;
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

  // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
  // Objective:
  // Find restaurants whose names have an exact or partial match with the search query.
  @Override
  public List<Restaurant> findRestaurantsByName(Double latitude, Double longitude,
      String searchString, LocalTime currentTime, Double servingRadiusInKms) {

        log.info("findRestaurantsByName called with latitude: {}, longitude: {}, searchString: {}, currentTime: {}, servingRadiusInKms: {}",
        latitude, longitude, searchString, currentTime, servingRadiusInKms);
  
        List<RestaurantEntity> results = restaurantRepository.findAll();
        List<Restaurant> matchingRestaurants = new ArrayList<>();
        ModelMapper modelMapper = modelMapperProvider.get();
      
        for (RestaurantEntity res : results) {
          if (res.getName().toLowerCase().contains(searchString.toLowerCase()) &&
              isRestaurantCloseByAndOpen(res, currentTime, latitude, longitude, servingRadiusInKms)) {
            
            Restaurant temp = modelMapper.map(res, Restaurant.class);
            matchingRestaurants.add(temp);
            log.info("Restaurant {} matches search and is added to the list", res.getRestaurantId());
          }
        }
      
        log.info("Total restaurants found matching search criteria: {}", matchingRestaurants.size());
      
        return matchingRestaurants;

  }


  // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
  // Objective:
  // Find restaurants whose attributes (cuisines) intersect with the search query.
  @Override
  public List<Restaurant> findRestaurantsByAttributes(
      Double latitude, Double longitude,
      String searchString, LocalTime currentTime, Double servingRadiusInKms) {

        log.info("findRestaurantsByAttributes called with latitude: {}, longitude: {}, searchString: {}, currentTime: {}, servingRadiusInKms: {}",
        latitude, longitude, searchString, currentTime, servingRadiusInKms);
  
        List<RestaurantEntity> results = restaurantRepository.findAll();
        List<Restaurant> matchingRestaurants = new ArrayList<>();
        ModelMapper modelMapper = modelMapperProvider.get();
        
        for (RestaurantEntity res : results) {
          boolean matches = res.getAttributes() != null && res.getAttributes().stream()
              .anyMatch(attribute -> attribute.toLowerCase().contains(searchString.toLowerCase()));
      
          if (matches && isRestaurantCloseByAndOpen(res, currentTime, latitude, longitude, servingRadiusInKms)) {
            Restaurant temp = modelMapper.map(res, Restaurant.class);
            matchingRestaurants.add(temp);
            log.info("Restaurant {} matches search by attributes and is added to the list", res.getRestaurantId());
          }
        }
      
        log.info("Total restaurants found matching search by attributes: {}", matchingRestaurants.size());
      
        return matchingRestaurants;
  }



  // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
  // Objective:
  // Find restaurants which serve food items whose names form a complete or partial match
  // with the search query.

  @Override
  public List<Restaurant> findRestaurantsByItemName(
      Double latitude, Double longitude,
      String searchString, LocalTime currentTime, Double servingRadiusInKms) {

        log.info("findRestaurantsByItemName called with latitude: {}, longitude: {}, searchString: {}, currentTime: {}, servingRadiusInKms: {}",
        latitude, longitude, searchString, currentTime, servingRadiusInKms);
  
        List<Restaurant> matchingRestaurants = new ArrayList<>();
        List<RestaurantEntity> allRestaurants = restaurantRepository.findAll();
        ModelMapper modelMapper = modelMapperProvider.get();
      
        for (RestaurantEntity restaurant : allRestaurants) {
          if (isRestaurantCloseByAndOpen(restaurant, currentTime, latitude, longitude, servingRadiusInKms)) {
            Optional<MenuEntity> menuEntityOpt = menuRepository.findMenuByRestaurantId(restaurant.getRestaurantId());
            if (menuEntityOpt.isPresent()) {
              MenuEntity menuEntity = menuEntityOpt.get();
              for (Item item : menuEntity.getItems()) {
                if (item.getName().toLowerCase().contains(searchString.toLowerCase())) {
                  Restaurant temp = modelMapper.map(restaurant, Restaurant.class);
                  matchingRestaurants.add(temp);
                  log.info("Restaurant {} has item {} matching search and is added to the list", restaurant.getRestaurantId(), item.getName());
                  break; // Found a matching item, no need to check further items for this restaurant
                }
              }
            }
          }
        }
      
        log.info("Total restaurants found matching item name criteria: {}", matchingRestaurants.size());
        return matchingRestaurants;
  }

  // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
  // Objective:
  // Find restaurants which serve food items whose attributes intersect with the search query.
  @Override
  public List<Restaurant> findRestaurantsByItemAttributes(Double latitude, Double longitude,
      String searchString, LocalTime currentTime, Double servingRadiusInKms) {
        log.info("findRestaurantsByItemAttributes called with latitude: {}, longitude: {}, searchString: {}, currentTime: {}, servingRadiusInKms: {}",
        latitude, longitude, searchString, currentTime, servingRadiusInKms);
  
        List<MenuEntity> menus = menuRepository.findAll();
        List<Restaurant> matchingRestaurants = new ArrayList<>();
        ModelMapper modelMapper = modelMapperProvider.get();
        Set<String> restaurantIds = new HashSet<>();
      
        for (MenuEntity menu : menus) {
          boolean matches = menu.getItems() != null && menu.getItems().stream()
              .anyMatch(item -> item.getAttributes() != null &&
                  item.getAttributes().stream()
                      .anyMatch(attribute -> attribute.toLowerCase().contains(searchString.toLowerCase())));
      
          if (matches) {
            RestaurantEntity res = restaurantRepository.findById(menu.getRestaurantId()).orElse(null);
            if (res != null && !restaurantIds.contains(res.getRestaurantId()) &&
                isRestaurantCloseByAndOpen(res, currentTime, latitude, longitude, servingRadiusInKms)) {
      
              Restaurant temp = modelMapper.map(res, Restaurant.class);
              matchingRestaurants.add(temp);
              restaurantIds.add(res.getRestaurantId());
              log.info("Restaurant {} matches search by item attributes and is added to the list", res.getRestaurantId());
            }
          }
        }
      
        log.info("Total restaurants found matching search by item attributes: {}", matchingRestaurants.size());
      
        return matchingRestaurants;
  }

}











