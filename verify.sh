#!/bin/bash

# Exit immediately if a command exits with a non-zero status
set -e

# Configuration
BASE_URL=${1:-"http://localhost:8080"}
TIMESTAMP=$(date +%s)
TEST_USER="testuser_${TIMESTAMP}"
TEST_EMAIL="testuser_${TIMESTAMP}@example.com"
TEST_PASS="password123"

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color
BLUE='\033[0;34m'

echo -e "${BLUE}=== Starting API Verification Script ===${NC}"
echo -e "Target URL: ${BASE_URL}\n"

# Global variables for cleanup tracking
USER_ID=""
USER_TOKEN=""
ADMIN_TOKEN=""
CATEGORY_ID=""
POST_ID=""
COMMENT_ID=""
REPLY_ID=""

# Helper function to query with HTTP status code validation
make_request() {
  local method="$1"
  local url="$2"
  local headers="$3"
  local body="$4"
  local response_file=$(mktemp)
  local http_code

  if [ -n "$body" ]; then
    http_code=$(curl -s -o "$response_file" -w "%{http_code}" -X "$method" "$url" \
      -H "Content-Type: application/json" \
      $headers \
      -d "$body")
  else
    http_code=$(curl -s -o "$response_file" -w "%{http_code}" -X "$method" "$url" \
      $headers)
  fi

  local response_body=$(cat "$response_file")
  rm -f "$response_file"
  echo "${http_code}|${response_body}"
}

check_http_code() {
  local code="$1"
  local expected="$2"
  local context="$3"
  if [ "$code" -ne "$expected" ]; then
    echo -e "${RED}Error during ${context}: Expected HTTP ${expected}, got ${code}${NC}"
    exit 1
  fi
}

# Helper function to extract JSON fields
get_json_field() {
  local json="$1"
  local field="$2"
  # Try jq first, then python3, then sed
  if command -v jq >/dev/null 2>&1; then
    echo "$json" | jq -r ".${field} // empty"
  elif command -v python3 >/dev/null 2>&1; then
    echo "$json" | python3 -c "import sys, json; print(json.load(sys.stdin).get('$field', ''))"
  else
    # Fallback to sed for simple extraction (non-space values only)
    echo "$json" | sed -n 's/.*"'"$field"'":\([^,}]*\).*/\1/p' | tr -d '"' | tr -d ' '
  fi
}

# Helper function to format JSON
format_json() {
  if command -v jq >/dev/null 2>&1; then
    echo "$1" | jq .
  elif command -v python3 >/dev/null 2>&1; then
    echo "$1" | python3 -m json.tool
  else
    echo "$1"
  fi
}

# Setup Cleanup Hook
cleanup() {
  echo -e "\n${BLUE}=== Cleaning Up Test Data ===${NC}"
  if [ -n "$REPLY_ID" ]; then
    echo "Deleting nested reply ID: ${REPLY_ID}..."
    curl -s -X DELETE "${BASE_URL}/api/v1/comments/${REPLY_ID}" -H "Authorization: Bearer ${USER_TOKEN}" > /dev/null || true
  fi
  if [ -n "$COMMENT_ID" ]; then
    echo "Deleting comment ID: ${COMMENT_ID}..."
    curl -s -X DELETE "${BASE_URL}/api/v1/comments/${COMMENT_ID}" -H "Authorization: Bearer ${USER_TOKEN}" > /dev/null || true
  fi
  if [ -n "$POST_ID" ]; then
    echo "Deleting post ID: ${POST_ID}..."
    curl -s -X DELETE "${BASE_URL}/api/v1/posts/${POST_ID}" -H "Authorization: Bearer ${USER_TOKEN}" > /dev/null || true
  fi
  if [ -n "$CATEGORY_ID" ]; then
    echo "Deleting category ID: ${CATEGORY_ID}..."
    curl -s -X DELETE "${BASE_URL}/api/v1/categories/${CATEGORY_ID}" -H "Authorization: Bearer ${ADMIN_TOKEN}" > /dev/null || true
  fi
  if [ -n "$USER_ID" ]; then
    echo "Deleting test user ID: ${USER_ID}..."
    curl -s -X DELETE "${BASE_URL}/api/v1/users/${USER_ID}" -H "Authorization: Bearer ${ADMIN_TOKEN}" > /dev/null || true
  fi
  echo -e "${GREEN}Cleanup completed successfully.${NC}"
}
trap cleanup EXIT

# 1. Register User
echo -e "${BLUE}[1/8] Registering new user: ${TEST_USER}...${NC}"
REG_BODY=$(cat <<EOF
{
  "username": "${TEST_USER}",
  "email": "${TEST_EMAIL}",
  "password": "${TEST_PASS}",
  "displayName": "Test User ${TIMESTAMP}",
  "bio": "Automated verification test user"
}
EOF
)

RESPONSE=$(make_request "POST" "${BASE_URL}/api/v1/auth/register" "" "$REG_BODY")
HTTP_CODE=$(echo "$RESPONSE" | cut -d'|' -f1)
REG_RESPONSE=$(echo "$RESPONSE" | cut -d'|' -f2-)
check_http_code "$HTTP_CODE" 200 "User Registration"
echo "Registration Response:"
format_json "$REG_RESPONSE"
echo ""

# 2. Login User
echo -e "${BLUE}[2/8] Logging in with new user to get USER_TOKEN...${NC}"
LOGIN_BODY=$(cat <<EOF
{
  "usernameOrEmail": "${TEST_USER}",
  "password": "${TEST_PASS}"
}
EOF
)

RESPONSE=$(make_request "POST" "${BASE_URL}/api/v1/auth/login" "" "$LOGIN_BODY")
HTTP_CODE=$(echo "$RESPONSE" | cut -d'|' -f1)
USER_LOGIN_RESPONSE=$(echo "$RESPONSE" | cut -d'|' -f2-)
check_http_code "$HTTP_CODE" 200 "User Login"
USER_TOKEN=$(get_json_field "$USER_LOGIN_RESPONSE" "token")

if [ -z "$USER_TOKEN" ]; then
  echo -e "${RED}Failed to capture USER_TOKEN${NC}"
  exit 1
fi
echo -e "${GREEN}USER_TOKEN captured successfully.${NC}\n"

# Get User ID for Cleanup
ME_RESPONSE=$(make_request "GET" "${BASE_URL}/api/v1/auth/me" "-H Authorization: Bearer ${USER_TOKEN}" "")
ME_HTTP_CODE=$(echo "$ME_RESPONSE" | cut -d'|' -f1)
ME_BODY=$(echo "$ME_RESPONSE" | cut -d'|' -f2-)
check_http_code "$ME_HTTP_CODE" 200 "Fetching User Profile"
USER_ID=$(get_json_field "$ME_BODY" "id")

# 3. Login Admin (Bootstrapped via DataInitializer)
echo -e "${BLUE}[3/8] Logging in as admin to get ADMIN_TOKEN...${NC}"
ADMIN_LOGIN_BODY=$(cat <<EOF
{
  "usernameOrEmail": "admin",
  "password": "admin123"
}
EOF
)

RESPONSE=$(make_request "POST" "${BASE_URL}/api/v1/auth/login" "" "$ADMIN_LOGIN_BODY")
HTTP_CODE=$(echo "$RESPONSE" | cut -d'|' -f1)
ADMIN_LOGIN_RESPONSE=$(echo "$RESPONSE" | cut -d'|' -f2-)
check_http_code "$HTTP_CODE" 200 "Admin Login"
ADMIN_TOKEN=$(get_json_field "$ADMIN_LOGIN_RESPONSE" "token")

if [ -z "$ADMIN_TOKEN" ]; then
  echo -e "${RED}Failed to capture ADMIN_TOKEN. Make sure the server was bootstrapped correctly.${NC}"
  exit 1
fi
echo -e "${GREEN}ADMIN_TOKEN captured successfully.${NC}\n"

# 4. Create Category (using ADMIN_TOKEN)
CATEGORY_NAME="Category_${TIMESTAMP}"
echo -e "${BLUE}[4/8] Creating new category: ${CATEGORY_NAME}...${NC}"
CAT_BODY=$(cat <<EOF
{
  "name": "${CATEGORY_NAME}",
  "description": "Verification testing category"
}
EOF
)

RESPONSE=$(make_request "POST" "${BASE_URL}/api/v1/categories" "-H Authorization: Bearer ${ADMIN_TOKEN}" "$CAT_BODY")
HTTP_CODE=$(echo "$RESPONSE" | cut -d'|' -f1)
CAT_RESPONSE=$(echo "$RESPONSE" | cut -d'|' -f2-)
check_http_code "$HTTP_CODE" 200 "Category Creation"
CATEGORY_ID=$(get_json_field "$CAT_RESPONSE" "id")
echo "Category Created:"
format_json "$CAT_RESPONSE"
echo ""

# 5. Create Post (using USER_TOKEN, linking the new category)
echo -e "${BLUE}[5/8] Creating a new post as user...${NC}"
POST_BODY=$(cat <<EOF
{
  "title": "Automated Test Post ${TIMESTAMP}",
  "content": "This is the content of the post created by the verification script.",
  "excerpt": "A short excerpt for the automated post.",
  "status": "PUBLISHED",
  "categories": ["${CATEGORY_NAME}"],
  "tags": ["java", "springboot", "verification"]
}
EOF
)

RESPONSE=$(make_request "POST" "${BASE_URL}/api/v1/posts" "-H Authorization: Bearer ${USER_TOKEN}" "$POST_BODY")
HTTP_CODE=$(echo "$RESPONSE" | cut -d'|' -f1)
POST_RESPONSE=$(echo "$RESPONSE" | cut -d'|' -f2-)
check_http_code "$HTTP_CODE" 201 "Post Creation"
echo "Post Created:"
format_json "$POST_RESPONSE"

POST_ID=$(get_json_field "$POST_RESPONSE" "id")
if [ -z "$POST_ID" ]; then
  echo -e "${RED}Failed to parse POST_ID from post creation response${NC}"
  exit 1
fi
echo -e "${GREEN}POST_ID: ${POST_ID} captured successfully.${NC}\n"

# 6. Comment on Post (using USER_TOKEN)
echo -e "${BLUE}[6/8] Posting a parent comment...${NC}"
COMMENT_BODY=$(cat <<EOF
{
  "content": "This is a verification parent comment."
}
EOF
)

RESPONSE=$(make_request "POST" "${BASE_URL}/api/v1/posts/${POST_ID}/comments" "-H Authorization: Bearer ${USER_TOKEN}" "$COMMENT_BODY")
HTTP_CODE=$(echo "$RESPONSE" | cut -d'|' -f1)
COMMENT_RESPONSE=$(echo "$RESPONSE" | cut -d'|' -f2-)
check_http_code "$HTTP_CODE" 200 "Comment Creation"
echo "Comment Posted:"
format_json "$COMMENT_RESPONSE"

COMMENT_ID=$(get_json_field "$COMMENT_RESPONSE" "id")
if [ -z "$COMMENT_ID" ]; then
  echo -e "${RED}Failed to parse COMMENT_ID from comment response${NC}"
  exit 1
fi
echo -e "${GREEN}COMMENT_ID: ${COMMENT_ID} captured successfully.${NC}\n"

# 7. Nested Reply Comment (using USER_TOKEN)
echo -e "${BLUE}[7/8] Posting a nested reply comment...${NC}"
REPLY_BODY=$(cat <<EOF
{
  "content": "This is a nested reply comment to comment ${COMMENT_ID}.",
  "parentId": ${COMMENT_ID}
}
EOF
)

RESPONSE=$(make_request "POST" "${BASE_URL}/api/v1/posts/${POST_ID}/comments" "-H Authorization: Bearer ${USER_TOKEN}" "$REPLY_BODY")
HTTP_CODE=$(echo "$RESPONSE" | cut -d'|' -f1)
REPLY_RESPONSE=$(echo "$RESPONSE" | cut -d'|' -f2-)
check_http_code "$HTTP_CODE" 200 "Reply Creation"
REPLY_ID=$(get_json_field "$REPLY_RESPONSE" "id")
echo "Nested Reply Response:"
format_json "$REPLY_RESPONSE"
echo ""

# 8. Like and Get Posts Page
echo -e "${BLUE}[8/8] Toggling like on the post and fetching the posts page...${NC}"
RESPONSE=$(make_request "POST" "${BASE_URL}/api/v1/posts/${POST_ID}/like" "-H Authorization: Bearer ${USER_TOKEN}" "")
HTTP_CODE=$(echo "$RESPONSE" | cut -d'|' -f1)
LIKE_RESPONSE=$(echo "$RESPONSE" | cut -d'|' -f2-)
check_http_code "$HTTP_CODE" 200 "Toggling Like"
echo "Like Response:"
format_json "$LIKE_RESPONSE"
echo ""

RESPONSE=$(make_request "GET" "${BASE_URL}/api/v1/posts?page=0&size=10" "" "")
HTTP_CODE=$(echo "$RESPONSE" | cut -d'|' -f1)
GET_POSTS_RESPONSE=$(echo "$RESPONSE" | cut -d'|' -f2-)
check_http_code "$HTTP_CODE" 200 "Getting Posts Page"
echo "Get Posts Response (Page 0):"
format_json "$GET_POSTS_RESPONSE"
echo ""

echo -e "${GREEN}=== API Verification Completed Successfully! ===${NC}"
