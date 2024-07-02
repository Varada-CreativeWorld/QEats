package com.crio.qeats.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@AllArgsConstructor
@Slf4j
public class GeoUtils {

  public static double findDistanceInKm(double srcLatitude, double srcLongitude,
      double dstLatitude, double dstLongitude) {
    return distance(srcLatitude, dstLatitude, srcLongitude, dstLongitude, 0, 0);
  }

  /**
   * THIS IS BORROWED CODE. Calculate distance between two points in latitude and longitude taking
   * into account height difference. If you are not interested in height difference pass 0.0. Uses
   * Haversine method as its base.
   *
   * <p>lat1, lon1 Start point lat2, lon2 End point el1 Start altitude in meters el2 End altitude
   * in meters
   *
   * @returns Distance in Kilo Meters
   */
  private static double distance(
      double lat1, double lat2, double lon1, double lon2, double el1, double el2) {

    final int R = 6371; // Radius of the earth in kilometers

    log.info("Calculating distance between points: ({}, {}) and ({}, {})", lat1, lon1, lat2, lon2);

    double latDistance = Math.toRadians(lat2 - lat1);
    double lonDistance = Math.toRadians(lon2 - lon1);
    log.info("Latitude distance (radians): {}", latDistance);
    log.info("Longitude distance (radians): {}", lonDistance);

    double a =
        Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
            + Math.cos(Math.toRadians(lat1))
            * Math.cos(Math.toRadians(lat2))
            * Math.sin(lonDistance / 2)
            * Math.sin(lonDistance / 2);
    log.info("Intermediate value 'a': {}", a);

    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    log.info("Intermediate value 'c': {}", c);

    double distance = R * c;
    log.info("Horizontal distance (km): {}", distance);

    double height = el1 - el2;

    // Convert height from meters to kilometers and combine with horizontal distance
    double totalDistance = Math.sqrt(Math.pow(distance, 2) + Math.pow(height / 1000.0, 2));
    log.info("Total distance (km) including height: {}", totalDistance);

    return totalDistance;
  }
}
