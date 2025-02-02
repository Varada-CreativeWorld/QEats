/*
 *
 *  * Copyright (c) Crio.Do 2019. All rights reserved
 *
 */

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
  ItemRepository itemRepository;

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

        ModelMapper modelMapper = modelMapperProvider.get();
        Set<String> restaurantSet = new HashSet<>();
        List<Restaurant> restaurantList = new ArrayList<>();
        Optional<List<RestaurantEntity>> optionalExactRestaurantEntityList = restaurantRepository.findRestaurantsByNameExact(searchString);
        if (optionalExactRestaurantEntityList.isPresent()) {
          List<RestaurantEntity> restaurantEntityList = optionalExactRestaurantEntityList.get();
          for (RestaurantEntity restaurantEntity : restaurantEntityList) {
              if (isRestaurantCloseByAndOpen(restaurantEntity, currentTime,latitude, longitude, servingRadiusInKms) &&!restaurantSet.contains(restaurantEntity.getRestaurantId())) {
                  restaurantList.add(modelMapper.map(restaurantEntity,
  Restaurant.class));
                  restaurantSet.add(restaurantEntity.getRestaurantId());
              }
          }
        }
        Optional<List<RestaurantEntity>> optionalInexactRestaurantEntityList = restaurantRepository.findRestaurantsByName(searchString);
        if (optionalInexactRestaurantEntityList.isPresent()) {
          List<RestaurantEntity> restaurantEntityList = optionalInexactRestaurantEntityList.get();
          for (RestaurantEntity restaurantEntity : restaurantEntityList) {
              if (isRestaurantCloseByAndOpen(restaurantEntity, currentTime,latitude, longitude, servingRadiusInKms) && !restaurantSet.contains(restaurantEntity.getRestaurantId())) {
                  restaurantList.add(modelMapper.map(restaurantEntity,
  Restaurant.class));
                  restaurantSet.add(restaurantEntity.getRestaurantId());
              }
          }
        }
        return restaurantList;
  }
  


  // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
  // Objective:
  // Find restaurants whose attributes (cuisines) intersect with the search query.
  @Override
  public List<Restaurant> findRestaurantsByAttributes(
      Double latitude, Double longitude,
      String searchString, LocalTime currentTime, Double servingRadiusInKms) {

        List<Pattern> patterns = Arrays.stream(searchString.split(" ")).map(attr -> Pattern.compile(attr, Pattern.CASE_INSENSITIVE)).collect(Collectors.toList());
        Query query = new Query();
        for (Pattern pattern : patterns) {
          query.addCriteria(Criteria.where("attributes").regex(pattern));
        }
        List<RestaurantEntity> restaurantEntityList = mongoTemplate.find(query, RestaurantEntity.class);
        List<Restaurant> restaurantList = new ArrayList<>();
        ModelMapper modelMapper = modelMapperProvider.get();
        for (RestaurantEntity restaurantEntity : restaurantEntityList) {
          if (isRestaurantCloseByAndOpen(restaurantEntity, currentTime,
            latitude, longitude, servingRadiusInKms)) {
            restaurantList.add(modelMapper.map(restaurantEntity,
            Restaurant.class));
          }
        }
        return restaurantList;
  }




  @Override
  public List<Restaurant> findRestaurantsByItemName(
      Double latitude, Double longitude,
      String searchString, LocalTime currentTime, Double servingRadiusInKms) {

        String regex = String.join("|", Arrays.asList(searchString.split("")));
        Optional<List<ItemEntity>> optionalExactItems = itemRepository.findItemsByNameExact(searchString);
        Optional<List<ItemEntity>> optionalInexactItems = itemRepository.findItemsByNameInexact(regex);
        List<ItemEntity> itemEntityList = optionalExactItems.orElseGet(ArrayList::new);
        List<ItemEntity> inexactItemEntityList = optionalInexactItems.orElseGet(ArrayList::new);
        itemEntityList.addAll(inexactItemEntityList);
        return getRestaurantListServingItems(latitude, longitude, currentTime, servingRadiusInKms, itemEntityList);
  }

  // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
  // Objective:
  // Find restaurants which serve food items whose attributes intersect with the search query.
  @Override
  public List<Restaurant> findRestaurantsByItemAttributes(Double latitude, Double longitude,
      String searchString, LocalTime currentTime, Double servingRadiusInKms) {
        List<Pattern> patterns = Arrays.stream(searchString.split(" ")).map(attr -> Pattern.compile(attr, Pattern.CASE_INSENSITIVE)).collect(Collectors.toList());
        Query query = new Query();
        for (Pattern pattern : patterns) {
          query.addCriteria(Criteria.where("attributes").regex(pattern));
        }
        List<ItemEntity> itemEntityList = mongoTemplate.find(query,ItemEntity.class);
        return getRestaurantListServingItems(latitude, longitude,currentTime, servingRadiusInKms,itemEntityList);
  }

  private List<Restaurant> getRestaurantListServingItems(Double latitude, Double longitude, LocalTime currentTime, Double servingRadiusInKms, List<ItemEntity>itemEntityList) {
    List<String> itemIdList = itemEntityList.stream().map(ItemEntity::getItemId).collect(Collectors.toList());
    Optional<List<MenuEntity>> optionalMenuEntityList = menuRepository.findMenusByItemsItemIdIn(itemIdList);
    Optional<List<RestaurantEntity>> optionalRestaurantEntityList = Optional.empty();
    if (optionalMenuEntityList.isPresent()) {
      List<MenuEntity> menuEntityList = optionalMenuEntityList.get();
      List<String> restaurantIdList = menuEntityList.stream().map(MenuEntity::getRestaurantId).collect(Collectors.toList());
      optionalRestaurantEntityList = restaurantRepository.findRestaurantsByRestaurantIdIn(restaurantIdList);
    }
    List<Restaurant> restaurantList = new ArrayList<>();
    ModelMapper modelMapper = modelMapperProvider.get();
    if (optionalRestaurantEntityList.isPresent()) {
      List<RestaurantEntity> restaurantEntityList = optionalRestaurantEntityList.get();
      List<RestaurantEntity> restaurantEntitiesFiltered = new ArrayList<>();
      for (RestaurantEntity restaurantEntity : restaurantEntityList) {
          if (isRestaurantCloseByAndOpen(restaurantEntity, currentTime, latitude, longitude,servingRadiusInKms)) {
              restaurantEntitiesFiltered.add(restaurantEntity);
          }
      }
      restaurantList = restaurantEntitiesFiltered.stream().map(restaurantEntity -> modelMapper.map(restaurantEntity,Restaurant.class)).collect(Collectors.toList());
    }
    return restaurantList;
  }
}