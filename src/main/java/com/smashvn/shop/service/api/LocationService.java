package com.smashvn.shop.service.api;

import java.net.URI;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.net.ssl.SSLException;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class LocationService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate reverseGeocodeRestTemplate;
    private volatile HttpClient httpClient;
    private final Object nominatimRequestLock = new Object();
    private volatile long lastNominatimRequestAt;

    @Value("${location.nominatim.base-url:https://nominatim.openstreetmap.org}")
    private String nominatimBaseUrl;

    @Value("${location.nominatim.user-agent:SMASH-VN/1.0}")
    private String nominatimUserAgent;

    @Value("${location.maptiler.base-url:https://api.maptiler.com}")
    private String mapTilerBaseUrl;

    @Value("${location.maptiler.api-key:}")
    private String mapTilerApiKey;

    @Value("${location.maptiler.user-agent:SMASH-VN/1.0}")
    private String mapTilerUserAgent;

    public LocationService(@Qualifier("restTemplate") RestTemplate reverseGeocodeRestTemplate) {
        this.reverseGeocodeRestTemplate = reverseGeocodeRestTemplate;
    }

    @Cacheable(value = "ipLocationCache", key = "#ip")
    public Map<String, Object> getIpLocation(String ip) {
        System.out.println("[LocationService] Fetching location from external provider for IP: " + ip);
        Map<String, Object> result = new HashMap<>();
        result.put("ip", ip);

        // Try ipapi.co first
        try {
            HttpRequest ipapiReq = HttpRequest.newBuilder()
                    .uri(URI.create("https://ipapi.co/" + ip + "/json/"))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(Duration.ofSeconds(4))
                    .build();
            HttpResponse<String> response = httpClient().send(ipapiReq, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                if (root.has("latitude") && root.has("longitude")) {
                    double lat = root.get("latitude").asDouble();
                    double lon = root.get("longitude").asDouble();
                    
                    if (isValidCoordinate(lat, lon)) {
                        result.put("latitude", lat);
                        result.put("longitude", lon);
                        result.put("city", root.has("city") ? root.get("city").asText() : "");
                        result.put("region", root.has("region") ? root.get("region").asText() : "");
                        result.put("country", root.has("country_name") ? root.get("country_name").asText() : "Vietnam");
                        result.put("source", "ipapi.co");
                        return result;
                    } else {
                        System.err.println("[LocationService] ipapi.co returned invalid coordinates: lat=" + lat + ", lon=" + lon);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[LocationService] Failed to fetch from ipapi.co: " + e.getMessage());
        }

        // Try ipinfo.io as fallback
        try {
            HttpRequest ipinfoReq = HttpRequest.newBuilder()
                    .uri(URI.create("https://ipinfo.io/" + ip + "/json"))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(Duration.ofSeconds(4))
                    .build();
            HttpResponse<String> response = httpClient().send(ipinfoReq, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                if (root.has("loc")) {
                    String loc = root.get("loc").asText();
                    String[] latLon = loc.split(",");
                    if (latLon.length == 2) {
                        double lat = Double.parseDouble(latLon[0].trim());
                        double lon = Double.parseDouble(latLon[1].trim());
                        
                        if (isValidCoordinate(lat, lon)) {
                            result.put("latitude", lat);
                            result.put("longitude", lon);
                            result.put("city", root.has("city") ? root.get("city").asText() : "");
                            result.put("region", root.has("region") ? root.get("region").asText() : "");
                            result.put("country", root.has("country") ? root.get("country").asText() : "Vietnam");
                            result.put("source", "ipinfo.io");
                            return result;
                        } else {
                            System.err.println("[LocationService] ipinfo.io returned invalid coordinates: lat=" + lat + ", lon=" + lon);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[LocationService] Failed to fetch from ipinfo.io: " + e.getMessage());
        }

        throw new RuntimeException("Unable to determine location from IP: " + ip);
    }

    @Cacheable(value = "geocodeSearchCache", key = "#query")
    public Map<String, Object> searchAddress(String query) {
        System.out.println("[LocationService] Searching coordinates for address: " + query);
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Encode query string
            String encodedQuery = java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8);
            
            waitForNominatimRateLimit();
            // Search Nominatim with countrycodes=vn and limit=1
            HttpRequest searchReq = HttpRequest.newBuilder()
                    .uri(URI.create(nominatimBaseUrl + "/search?format=json&q=" + encodedQuery
                            + "&countrycodes=vn&limit=1"))
                    .header("User-Agent", nominatimUserAgent)
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(4))
                    .build();
            HttpResponse<String> response = httpClient().send(searchReq, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                if (root.isArray() && root.size() > 0) {
                    JsonNode firstResult = root.get(0);
                    if (firstResult.has("lat") && firstResult.has("lon")) {
                        double lat = Double.parseDouble(firstResult.get("lat").asText());
                        double lon = Double.parseDouble(firstResult.get("lon").asText());
                        
                        if (isValidCoordinate(lat, lon)) {
                            result.put("latitude", lat);
                            result.put("longitude", lon);
                            result.put("display_name", firstResult.has("display_name") ? firstResult.get("display_name").asText() : "");
                            return result;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[LocationService] Failed to search address coordinates: " + e.getMessage());
        }
        
        throw new RuntimeException("Unable to geocode address: " + query);
    }

    /**
     * Reverse geocoding được gọi từ backend để frontend không phụ thuộc trực
     * tiếp vào cấu trúc phản hồi của nhà cung cấp. Cache giúp tránh gửi lại
     * cùng một tọa độ khi người dùng bấm/kéo marker nhiều lần.
     */
    @Cacheable(value = "reverseGeocodeCache", key = "#latitude + ':' + #longitude")
    public ReverseGeocodedAddress reverseGeocode(double latitude, double longitude) {
        if (!isValidCoordinate(latitude, longitude)) {
            throw new IllegalArgumentException("Tọa độ không hợp lệ.");
        }

        final long startedAt = System.nanoTime();
        final boolean apiKeyPresent = mapTilerApiKey != null && !mapTilerApiKey.isBlank();
        final boolean userAgentPresent = mapTilerUserAgent != null && !mapTilerUserAgent.isBlank();
        String checkpoint = "provider configuration";
        int httpStatus = -1;
        String contentType = "<not received>";
        String bodyPreview = "<not received>";

        log.info("[ReverseGeocodeDiagnostic] start provider=MapTiler, lat={}, lng={}, apiKeyPresent={}, userAgentPresent={}, requestTimeoutMs={}",
                latitude, longitude, apiKeyPresent, userAgentPresent, 10000);

        try {
            if (!apiKeyPresent) {
                throw new IllegalStateException("MapTiler API key is not configured.");
            }

            checkpoint = "HTTP request";
            URI reverseUri = UriComponentsBuilder.fromUriString(mapTilerBaseUrl)
                    .pathSegment("geocoding", longitude + "," + latitude + ".json")
                    .queryParam("language", "vi")
                    .queryParam("key", mapTilerApiKey)
                    .build()
                    .encode()
                    .toUri();
            HttpHeaders requestHeaders = new HttpHeaders();
            if (userAgentPresent) requestHeaders.set("User-Agent", mapTilerUserAgent);
            requestHeaders.setAccept(List.of(MediaType.APPLICATION_JSON));
            HttpEntity<Void> requestEntity = new HttpEntity<>(requestHeaders);

            log.info("[ReverseGeocodeDiagnostic] HTTP request prepared provider=MapTiler, apiKeyPresent={}, userAgentPresent={}, uriHost={}",
                    apiKeyPresent, userAgentPresent && requestHeaders.containsHeader("User-Agent"),
                    reverseUri.getHost());

            ResponseEntity<String> response;
            try {
                response = reverseGeocodeRestTemplate.exchange(
                        reverseUri, HttpMethod.GET, requestEntity, String.class);
            } catch (HttpStatusCodeException exception) {
                checkpoint = "HTTP status";
                httpStatus = exception.getStatusCode().value();
                contentType = exception.getResponseHeaders() == null
                        ? "<missing>"
                        : exception.getResponseHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
                if (contentType == null) contentType = "<missing>";
                bodyPreview = safeResponseBodyPreview(exception.getResponseBodyAsString());
                log.info("[ReverseGeocodeDiagnostic] HTTP response elapsedMs={}, status={}, contentType={}, bodyPreview={}",
                        elapsedMillis(startedAt), httpStatus, contentType, bodyPreview);
                throw new IllegalStateException(
                        "Reverse geocoding provider returned status " + httpStatus, exception);
            }
            checkpoint = "HTTP status";
            httpStatus = response.getStatusCode().value();
            contentType = response.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
            if (contentType == null) contentType = "<missing>";
            String responseBody = response.getBody();
            bodyPreview = safeResponseBodyPreview(responseBody);
            log.info("[ReverseGeocodeDiagnostic] HTTP response elapsedMs={}, status={}, contentType={}, bodyPreview={}",
                    elapsedMillis(startedAt), httpStatus, contentType, bodyPreview);

            if (httpStatus != 200) {
                throw new IllegalStateException("Reverse geocoding provider returned status " + httpStatus);
            }

            checkpoint = "JSON parsing";
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode features = root == null ? null : root.path("features");
            log.info("[ReverseGeocodeDiagnostic] JSON parsed elapsedMs={}, rootType={}, hasFeaturesArray={}",
                    elapsedMillis(startedAt), root == null ? "<null>" : root.getNodeType(),
                    features != null && features.isArray());

            checkpoint = "address object";
            if (features == null || !features.isArray() || features.isEmpty()) {
                throw new IllegalStateException("Reverse geocoding response does not contain location features.");
            }
            log.info("[ReverseGeocodeDiagnostic] MapTiler features present count={}", features.size());

            Set<String> provinceValues = new LinkedHashSet<>();
            Set<String> districtValues = new LinkedHashSet<>();
            Set<String> wardValues = new LinkedHashSet<>();
            for (JsonNode feature : features) {
                collectMapTilerAdministrativeCandidate(
                        feature, provinceValues, districtValues, wardValues);
                JsonNode context = feature.path("context");
                if (context.isArray()) {
                    context.forEach(item -> collectMapTilerAdministrativeCandidate(
                            item, provinceValues, districtValues, wardValues));
                }
            }

            List<String> provinceCandidates = new ArrayList<>(provinceValues);
            List<String> districtCandidates = new ArrayList<>(districtValues);
            List<String> wardCandidates = new ArrayList<>(wardValues);
            JsonNode primaryFeature = features.get(0);

            ReverseGeocodedAddress resolved = new ReverseGeocodedAddress(
                    provinceCandidates,
                    districtCandidates,
                    wardCandidates,
                    buildMapTilerAddressDetail(features),
                    findMapTilerCountryCode(features),
                    textValue(primaryFeature, "place_name"));
            log.info("[ReverseGeocodeDiagnostic] completed elapsedMs={}, provinceCandidates={}, districtCandidates={}, wardCandidates={}, countryCode={}",
                    elapsedMillis(startedAt), provinceCandidates, districtCandidates, wardCandidates,
                    resolved.countryCode());
            return resolved;
        } catch (IllegalArgumentException exception) {
            logReverseGeocodeFailure(checkpoint, startedAt, httpStatus, contentType, bodyPreview, exception);
            throw new IllegalStateException("Không thể xác định địa chỉ vào lúc này.");
        } catch (Exception exception) {
            logReverseGeocodeFailure(checkpoint, startedAt, httpStatus, contentType, bodyPreview, exception);
            throw new IllegalStateException("Không thể xác định địa chỉ vào lúc này.");
        }
    }

    private void logReverseGeocodeFailure(String checkpoint, long startedAt, int httpStatus,
            String contentType, String bodyPreview, Throwable exception) {
        log.warn("[ReverseGeocodeDiagnostic] failed checkpoint={}, category={}, elapsedMs={}, status={}, contentType={}, bodyPreview={}, causeChain={}",
                checkpoint, classifyReverseGeocodeFailure(checkpoint, exception), elapsedMillis(startedAt),
                httpStatus < 0 ? "<not received>" : httpStatus, contentType, bodyPreview,
                safeCauseChain(exception));
    }

    private String classifyReverseGeocodeFailure(String checkpoint, Throwable exception) {
        if ("HTTP status".equals(checkpoint)) return "HTTP error";
        if ("JSON parsing".equals(checkpoint)) return "malformed JSON";
        if ("address object".equals(checkpoint)) return "missing address";
        if (hasCause(exception, HttpTimeoutException.class)
                || hasCause(exception, SocketTimeoutException.class)) return "timeout";
        if (hasCause(exception, UnknownHostException.class)
                || hasCause(exception, ConnectException.class)
                || hasCause(exception, SocketException.class)
                || hasCause(exception, SSLException.class)) return "DNS-connect-SSL";
        return "lỗi khác";
    }

    private boolean hasCause(Throwable exception, Class<? extends Throwable> expectedType) {
        Throwable current = exception;
        for (int depth = 0; current != null && depth < 8; depth++) {
            if (expectedType.isInstance(current)) return true;
            current = current.getCause();
        }
        return false;
    }

    private String safeCauseChain(Throwable exception) {
        List<String> causes = new ArrayList<>();
        Throwable current = exception;
        for (int depth = 0; current != null && depth < 8; depth++) {
            String message = current.getMessage();
            String safeMessage = message == null ? ""
                    : redactProviderSecrets(message.replaceAll("[\\r\\n\\t\\p{Cntrl}]", " ").trim());
            if (safeMessage.length() > 240) safeMessage = safeMessage.substring(0, 240) + "…";
            causes.add(current.getClass().getSimpleName() + (safeMessage.isEmpty() ? "" : ": " + safeMessage));
            current = current.getCause();
        }
        return String.join(" -> ", causes);
    }

    private String safeResponseBodyPreview(String body) {
        if (body == null) return "<null>";
        String sanitized = redactProviderSecrets(
                body.replaceAll("[\\r\\n\\t\\p{Cntrl}]", " ").trim());
        int limit = 600;
        return sanitized.length() <= limit ? sanitized : sanitized.substring(0, limit) + "…[truncated]";
    }

    private String redactProviderSecrets(String value) {
        return value.replaceAll("(?i)([?&]key=)[^&\\s\"']+", "$1<redacted>");
    }

    private long elapsedMillis(long startedAt) {
        return Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
    }

    private void waitForNominatimRateLimit() throws InterruptedException {
        synchronized (nominatimRequestLock) {
            long elapsed = System.currentTimeMillis() - lastNominatimRequestAt;
            long waitMillis = 1_000L - elapsed;
            if (waitMillis > 0) {
                Thread.sleep(waitMillis);
            }
            lastNominatimRequestAt = System.currentTimeMillis();
        }
    }

    private HttpClient httpClient() {
        HttpClient current = httpClient;
        if (current != null) return current;
        synchronized (this) {
            if (httpClient == null) {
                httpClient = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(5))
                        .build();
            }
            return httpClient;
        }
    }

    private void collectMapTilerAdministrativeCandidate(JsonNode item,
            Set<String> provinceValues, Set<String> districtValues, Set<String> wardValues) {
        String name = textValue(item, "text");
        String type = mapTilerFeatureType(item);
        if (name == null || name.isBlank() || type == null) return;

        switch (type) {
            case "region" -> provinceValues.add(name);
            case "subregion", "county", "joint_municipality", "municipal_district" ->
                districtValues.add(name);
            case "joint_submunicipality", "locality", "neighbourhood", "place" ->
                wardValues.add(name);
            case "municipality" -> {
                if (isWardAdministrativeName(name)) {
                    wardValues.add(name);
                } else if (isDistrictAdministrativeName(name)) {
                    districtValues.add(name);
                } else {
                    // MapTiler maps municipality differently between countries.
                    // Exact, province-scoped GHN matching decides whether this
                    // neutral candidate belongs to district or ward level.
                    districtValues.add(name);
                    wardValues.add(name);
                }
            }
            default -> {
                // Non-administrative features are used only for address detail.
            }
        }
    }

    private String mapTilerFeatureType(JsonNode item) {
        JsonNode placeTypes = item.path("place_type");
        if (placeTypes.isArray() && !placeTypes.isEmpty()) {
            String placeType = placeTypes.get(0).asText("").trim();
            if (!placeType.isEmpty()) return placeType;
        }

        String id = textValue(item, "id");
        if (id == null) return null;
        int separator = id.indexOf('.');
        return separator > 0 ? id.substring(0, separator) : id;
    }

    private boolean isWardAdministrativeName(String name) {
        String normalized = name.toLowerCase(Locale.ROOT).trim();
        return normalized.matches("^(phường|xã|thị trấn)\\b.*");
    }

    private boolean isDistrictAdministrativeName(String name) {
        String normalized = name.toLowerCase(Locale.ROOT).trim();
        return normalized.matches("^(quận|huyện|thị xã|thành phố|tp\\.?)\\b.*");
    }

    private String buildMapTilerAddressDetail(JsonNode features) {
        for (JsonNode feature : features) {
            String type = mapTilerFeatureType(feature);
            if (!"address".equals(type) && !"road".equals(type) && !"poi".equals(type)) continue;

            String detail = joinWithSpace(
                    textValue(feature, "address"),
                    textValue(feature, "text"));
            if (!detail.isBlank()) return detail;
        }
        return "";
    }

    private String findMapTilerCountryCode(JsonNode features) {
        for (JsonNode feature : features) {
            String countryCode = mapTilerCountryCode(feature);
            if (countryCode != null) return countryCode;

            JsonNode context = feature.path("context");
            if (!context.isArray()) continue;
            for (JsonNode item : context) {
                countryCode = mapTilerCountryCode(item);
                if (countryCode != null) return countryCode;
            }
        }
        return null;
    }

    private String mapTilerCountryCode(JsonNode item) {
        String countryCode = textValue(item.path("properties"), "country_code");
        if (countryCode == null) countryCode = textValue(item, "short_code");
        return countryCode == null ? null : countryCode.toLowerCase(Locale.ROOT);
    }

    private String textValue(JsonNode node, String key) {
        JsonNode value = node.path(key);
        if (value.isMissingNode() || value.isNull()) return null;
        String text = value.asText(null);
        return text == null ? null : text.trim();
    }

    private String joinWithSpace(String left, String right) {
        String safeLeft = left == null ? "" : left.trim();
        String safeRight = right == null ? "" : right.trim();
        return (safeLeft + " " + safeRight).trim();
    }

    public record ReverseGeocodedAddress(
            List<String> provinceCandidates,
            List<String> districtCandidates,
            List<String> wardCandidates,
            String addressDetail,
            String countryCode,
            String displayName) {
    }

    private boolean isValidCoordinate(double latitude, double longitude) {
        return Double.isFinite(latitude) && Double.isFinite(longitude)
                && latitude >= -90.0 && latitude <= 90.0
                && longitude >= -180.0 && longitude <= 180.0;
    }
}
