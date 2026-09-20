# Vektrlabs

Vektrlabs is a Spring Boot application with PostgreSQL.

## Prerequisites

- Docker and Docker Compose
- Java 25 (if running locally)

## Getting Started

### 1. Configuration

The application requires an `application.properties` file in `src/main/resources/`. A template is provided:

```bash
cp src/main/resources/application.properties.example src/main/resources/application.properties
```

Edit `application.properties` with your local database credentials if not using Docker.

### 2. Running with Docker Compose

To start the database (PostgreSQL) and the application:

```bash
docker-compose up --build
```

The database will be available on port `5432` and the application on port `8080`.

### 3. Running Locally

First, start only the database:

```bash
docker-compose up db
```

Then run the Spring Boot application:

```bash
./gradlew bootRun
```

## Database Features

- **PostgreSQL 17**: Main relational database.

## Authentication and profiles

Configure the Cognito issuer, confidential app client and logout settings from
`application.properties.example`. Authentication uses the Authorization Code
flow and a server-side session; the browser does not store Cognito tokens.
Register the application's login callback and logout return URL with Cognito.
Session cookies require HTTPS by default; set `SESSION_COOKIE_SECURE=false` only
for local HTTP development.

Catalogue browsing is public. Store creation, cart actions, checkout and other
business operations require a signed-in user with a complete profile: non-blank
`firstName` and `lastName`; `middleName` is optional. Locations are not required
yet. Missing or incomplete profiles are directed to `/complete-profile`.

`POST /api/user-profiles` creates the signed-in user's profile;
`GET /api/user-profiles/me` reads it and `PUT /api/user-profiles/me` completes or
updates it. These APIs require login but not an already-complete profile.
The identity and email are taken from authenticated claims, never the form.
Session-authenticated mutations must send the CSRF token provided by the app.

A store references its `UserProfile` directly, and one profile can create many
stores. `StoreController` still calls `StoreService.create`; the service sets the
profile in `setEntityDependencies`. There are no membership, ownership-role or
tax-profile services. Store-scoped catalogue changes are limited to the
associated profile.

Orders reference their buyer's profile. `POST /api/v1/order` accepts
`{"cartId":"<uuid>"}` and uses the caller's cart, just like
`POST /api/v1/shopping/cart/{cartId}/checkout`. Do not send order items or buyer
identity separately; edit the cart first. REST and AI paths use the same access
checks, and another buyer's cart/order cannot be accessed by supplying its ID.

**Payment completion is blocked:** the consumed Jericho callback currently trusts
a browser-supplied success flag. `/api/v1/payments/callback` is denied until
Jericho verifies payment-provider state. Checkout can create an order/payment
session, but this callback cannot mark it paid.

### Existing databases

The new required `stores.user_profile_id` and `orders.buyer_id` columns reference
`user_profiles.id`; `user_profiles.cognito_sub` is the unique external identity
lookup key. Existing records require an explicitly chosen profile backfill before
deploying these required foreign keys. Do not assume Hibernate `ddl-auto=update`
can infer identities or migrate former membership/tax data. No database reset or
destructive migration is included. Existing display-name profiles also need an
explicit first/last-name backfill; names are not automatically split.

## Development

- Built with Spring Boot 4.1.0
- Java 25
- Gradle

## Production deployment (EC2 + GitHub Actions)

This repository now includes production deployment assets:

- `scripts/provision-ec2.sh` provisions an EC2 host with configurable top-of-file variables (instance type, VPC/subnet, SG, IAM profile, ports, storage).
- `docker-compose.prod.yml` runs the app container in production mode.
- `scripts/render-env-from-ssm.sh` materializes `.env.production` from AWS SSM Parameter Store.
- `scripts/deploy-on-ec2.sh` pulls the target image, validates required env/Stripe live keys, and deploys with Docker Compose.
- `.github/workflows/ci.yml` runs Gradle tests.
- `.github/workflows/cd-prod.yml` builds/pushes image to ECR and deploys to EC2 via SSM on `main`.

### GitHub repository setup

Set these repository variables:

- `AWS_REGION`
- `ECR_REPOSITORY`
- `EC2_INSTANCE_ID`
- `DEPLOY_ASSET_BUCKET`
- `SSM_PARAMETER_PATH_PREFIX` (example: `/vektrlabs/prod/app/`)
- `APP_PORT` (optional, defaults to `8080`)

Set this repository secret:

- `AWS_ROLE_TO_ASSUME` (OIDC-assumable IAM role ARN for GitHub Actions)

### Required SSM parameters

Under `SSM_PARAMETER_PATH_PREFIX`, provide at least:

- `APP_BASE_URL`
- `APP_POST_LOGOUT_REDIRECT_URI`
- `COGNITO_ISSUER_URI`
- `COGNITO_CLIENT_ID`
- `COGNITO_CLIENT_SECRET`
- `COGNITO_LOGOUT_URI`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `SESSION_COOKIE_SECURE`
- `PAYMENT_STRATEGY`
- `STRIPE_PUBLISHABLE_KEY` (`pk_live_...` in production)
- `STRIPE_SECRET_KEY` (`sk_live_...` in production)

### IAM permissions

The GitHub OIDC role needs ECR push/pull, S3 object read/write for deploy assets, and SSM send-command permissions.
The EC2 instance profile needs ECR pull, SSM parameter read/decrypt, S3 read for deploy assets, and SSM managed instance permissions.

## Architecture and Revision Planning

See [Revisions - v1](revisions/v1.md) for the architecture assessment, Jericho
integration analysis, prioritized change plan, and decisions needed before
implementation.
