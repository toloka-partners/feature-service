# Feature Analytics and Usage Tracking REST API Endpoints

## Description

Implement REST API endpoints to programmatically post feature usage events and retrieve analytics data. This enables external systems and clients to track feature usage and query aggregated statistics for features and products.

## Objective

Expose REST API endpoints for posting usage events and retrieving analytics data programmatically.

## Requirements

### 1. POST /api/usage - Create Usage Event

- Accept authenticated POST requests to create new usage events
- **Required field**: `actionType` (ActionType enum)
- **Optional fields**: `featureCode` (String), `productCode` (String), `context` (Map<String, Object>)
- Auto-capture: `userId` from SecurityContext, `timestamp` (server time), `ipAddress`, `userAgent` from request
- **Responses**: 
  - `201 Created` with FeatureUsageDto body and Location header
  - `4xx Client Error` for validation errors (missing actionType, invalid enum, malformed JSON)
  - `401 Unauthorized` for unauthenticated requests

### 2. GET /api/usage/feature/{featureCode}/stats - Feature Statistics

- Return FeatureStatsDto with: `featureCode`, `totalUsageCount`, `uniqueUserCount`, `usageByActionType`, `topUsers`, `usageByProduct`
- **Query parameters** (optional): `actionType`, `startDate`, `endDate` (ISO 8601 format)
- Return zero counts for non-existent features (not 404)
- **Responses**: `2xx Success`, `4xx Client Error` for invalid dates, `401 Unauthorized` for unauthenticated requests

### 3. GET /api/usage/product/{productCode}/stats - Product Statistics

- Return ProductStatsDto with: `productCode`, `totalUsageCount`, `uniqueUserCount`, `uniqueFeatureCount`, `usageByActionType`, `topFeatures`, `topUsers`
- **Query parameters** (optional): `actionType`, `startDate`, `endDate` (ISO 8601 format)
- Return zero counts for non-existent products (not 404)
- **Responses**: `2xx Success`, `4xx Client Error` for invalid dates, `401 Unauthorized` for unauthenticated requests

### 4. GET /api/usage/feature/{featureCode}/events - Feature Events List

- Return List<FeatureUsageDto> filtered by featureCode
- **Query parameters** (optional): `actionType`, `startDate`, `endDate`
- Return empty list for no matches
- **Responses**: `2xx Success`, `401 Unauthorized` for unauthenticated requests

### 5. GET /api/usage/product/{productCode}/events - Product Events List

- Return List<FeatureUsageDto> filtered by productCode
- **Query parameters** (optional): `actionType`, `startDate`, `endDate`
- Return empty list for no matches
- **Responses**: `2xx Success`, `401 Unauthorized` for unauthenticated requests

### 6. GET /api/usage/events - All Usage Events List

- Return List<FeatureUsageDto> with all usage events
- **Query parameters** (optional): `actionType`, `userId`, `featureCode`, `productCode`, `startDate`, `endDate` (ISO 8601 format)
- Return empty list for no matches
- **Responses**: `2xx Success`, `4xx Client Error` for invalid dates, `401 Unauthorized` for unauthenticated requests

### 7. GET /api/usage/stats - Overall Usage Statistics

- Return UsageStatsDto with: `totalUsageCount`, `uniqueUserCount`, `uniqueFeatureCount`, `uniqueProductCount`, `usageByActionType`, `topFeatures`, `topProducts`, `topUsers`
- **Query parameters** (optional): `actionType`, `startDate`, `endDate` (ISO 8601 format)
- **Responses**: `2xx Success`, `4xx Client Error` for invalid dates, `401 Unauthorized` for unauthenticated requests

### 8. Security & Validation

- All endpoints require OAuth2 authentication
- Validation errors return `4xx Client Error` (not `5xx Server Error`)
- Invalid date formats return `4xx Client Error`

## Test Coverage

### Positive Cases
- Create usage event with all fields and with minimal data (actionType only)
- Retrieve feature/product stats with and without filters (actionType, date range)
- Retrieve feature/product events lists with and without filters

### Edge Cases
- Non-existent features/products return successful response with empty stats (zero counts) or empty lists
- Invalid dates, malformed JSON, invalid enum values return 4xx client errors
- Unauthenticated requests return 401

### Data Integrity
- Verify auto-captured fields: userId, timestamp, ipAddress, userAgent
- Verify Location header in POST response

## Acceptance Criteria

* POST /api/usage creates events and returns 201 with Location header

* GET /api/usage/feature/{featureCode}/stats returns 2xx success with accurate statistics and supports optional filtering

* GET /api/usage/product/{productCode}/stats returns 2xx success with accurate statistics and supports optional filtering

* GET /api/usage/feature/{featureCode}/events and /product/{productCode}/events return 2xx success with filtered event lists

* All endpoints require authentication (401 for unauthenticated requests)

* Validation errors return 4xx client errors, not 5xx server errors

* Non-existent resources return 2xx success with empty data, not 404

## FAIL_TO_PASS

```
com.sivalabs.ft.features.api.controllers.FeatureUsageControllerTests
```

## PASS_TO_PASS

```
com.sivalabs.ft.features.api.controllers.FeatureControllerTests
com.sivalabs.ft.features.api.controllers.ProductControllerTests
com.sivalabs.ft.features.api.controllers.ReleaseControllerTests
com.sivalabs.ft.features.api.controllers.CommentControllerTests
com.sivalabs.ft.features.api.controllers.FavoriteFeatureControllerTests
com.sivalabs.ft.features.api.controllers.FeatureUsageControllerIntegrationTest
com.sivalabs.ft.features.domain.FeatureUsageRepositoryTest
com.sivalabs.ft.features.domain.FeatureUsageServiceTest
com.sivalabs.ft.features.domain.ProductRepositoryTest
com.sivalabs.ft.features.domain.ProductServiceTest
```