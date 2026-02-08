package com.wd.custapi.util;

/**
 * Utility class for geographic calculations.
 * Uses the Haversine formula to calculate distances between GPS coordinates.
 */
public class GeoUtils {

    private static final double EARTH_RADIUS_KM = 6371.0;

    /**
     * Maximum allowed distance (in km) between user's GPS coordinates
     * and the project site for check-in/check-out.
     */
    public static final double MAX_CHECKIN_DISTANCE_KM = 2.0;

    private GeoUtils() {
        // Utility class
    }

    /**
     * Calculate the distance between two GPS coordinates using the Haversine formula.
     *
     * @param lat1 Latitude of point 1 (in degrees)
     * @param lon1 Longitude of point 1 (in degrees)
     * @param lat2 Latitude of point 2 (in degrees)
     * @param lon2 Longitude of point 2 (in degrees)
     * @return Distance in kilometers
     */
    public static double calculateDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

    /**
     * Check if a given GPS coordinate is within the allowed radius of the project site.
     *
     * @param userLat      User's latitude
     * @param userLon      User's longitude
     * @param projectLat   Project site latitude
     * @param projectLon   Project site longitude
     * @param maxDistanceKm Maximum allowed distance in km
     * @return true if within range, false otherwise
     */
    public static boolean isWithinRadius(double userLat, double userLon,
                                         double projectLat, double projectLon,
                                         double maxDistanceKm) {
        double distance = calculateDistanceKm(userLat, userLon, projectLat, projectLon);
        return distance <= maxDistanceKm;
    }

    /**
     * Format distance for display.
     *
     * @param distanceKm Distance in kilometers
     * @return Formatted string like "1.5 km" or "350 m"
     */
    public static String formatDistance(double distanceKm) {
        if (distanceKm < 1.0) {
            return String.format("%.0f m", distanceKm * 1000);
        }
        return String.format("%.1f km", distanceKm);
    }
}
