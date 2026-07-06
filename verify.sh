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

# Helper function to extract JSON fields
get_json_field() {
  local json="$1"
  local field="$2"
  if command -v python3 >/dev/null 2>&1; then
    echo "$json" | python3 -c "import sys, json; print(json.load(sys.stdin).get('$field', ''))"
  else
    # Fallback to sed for simple extraction
    echo "$json" | sed -n 's/.*"'"$field"'":\([^,}]*\).*/\1/p' | tr -d '"' | tr -d ' '
  fi
}

# Helper function to format JSON
format_json() {
  if command -v python3 >/dev/null 2>&1; then
    echo "$1" | python3 -m json.tool
  else
    echo "$1"
  fi
}

# Helper to check response status
check_status() {
  local response="$1"
  if [ -z "$response" ]; then
    echo -e "${RED}Error: Empty response${NC}"
    exit 1
  fi
}

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

REG_RESPONSE=$(curl -s -X POST "${BASE_URL}/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  -d "$REG_BODY")

check_status "$REG_RESPONSE"
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

USER_LOGIN_RESPONSE=$(curl -s -X POST "${BASE_URL}/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "$LOGIN_BODY")

check_status "$USER_LOGIN_RESPONSE"
USER_TOKEN=$(get_json_field "$USER_LOGIN_RESPONSE" "token")

if [ -z "$USER_TOKEN" ]; then
  echo -e "${RED}Failed to capture USER_TOKEN${NC}"
  exit 1
fi
echo -e "${GREEN}USER_TOKEN captured successfully.${NC}\n"

# 3. Login Admin (Bootstrapped via DataInitializer)
echo -e "${BLUE}[3/8] Logging in as admin to get ADMIN_TOKEN...${NC}"
ADMIN_LOGIN_BODY=$(cat <<EOF
{
  "usernameOrEmail": "admin",
  "password": "admin123"
}
EOF
)

ADMIN_LOGIN_RESPONSE=$(curl -s -X POST "${BASE_URL}/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "$ADMIN_LOGIN_BODY")

check_status "$ADMIN_LOGIN_RESPONSE"
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

CAT_RESPONSE=$(curl -s -X POST "${BASE_URL}/api/v1/categories" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  -d "$CAT_BODY")

check_status "$CAT_RESPONSE"
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

POST_RESPONSE=$(curl -s -X POST "${BASE_URL}/api/v1/posts" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${USER_TOKEN}" \
  -d "$POST_BODY")

check_status "$POST_RESPONSE"
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

COMMENT_RESPONSE=$(curl -s -X POST "${BASE_URL}/api/v1/posts/${POST_ID}/comments" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${USER_TOKEN}" \
  -d "$COMMENT_BODY")

check_status "$COMMENT_RESPONSE"
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

REPLY_RESPONSE=$(curl -s -X POST "${BASE_URL}/api/v1/posts/${POST_ID}/comments" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${USER_TOKEN}" \
  -d "$REPLY_BODY")

check_status "$REPLY_RESPONSE"
echo "Nested Reply Response:"
format_json "$REPLY_RESPONSE"
echo ""

# 8. Like and Get Posts Page
echo -e "${BLUE}[8/8] Toggling like on the post and fetching the posts page...${NC}"
LIKE_RESPONSE=$(curl -s -X POST "${BASE_URL}/api/v1/posts/${POST_ID}/like" \
  -H "Authorization: Bearer ${USER_TOKEN}" \
  -d "")

check_status "$LIKE_RESPONSE"
echo "Like Response:"
format_json "$LIKE_RESPONSE"
echo ""

GET_POSTS_RESPONSE=$(curl -s -X GET "${BASE_URL}/api/v1/posts?page=0&size=10")
check_status "$GET_POSTS_RESPONSE"
echo "Get Posts Response (Page 0):"
format_json "$GET_POSTS_RESPONSE"
echo ""

echo -e "${GREEN}=== API Verification Completed Successfully! ===${NC}"
