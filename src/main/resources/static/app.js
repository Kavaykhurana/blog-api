/**
 * ==========================================================================
 * NexusBlog Application Logic & SPA Router
 * ==========================================================================
 */

const API_BASE = '/api/v1';

// Global Application State
const state = {
  currentUser: null,
  posts: [],
  categories: [],
  tags: [],
  filters: {
    search: '',
    category: '',
    tag: ''
  },
  pagination: {
    page: 0,
    size: 6,
    totalPages: 0,
    isLast: true
  }
};

// ==========================================================================
// Toast Notification Helper
// ==========================================================================
function showToast(message, type = 'success') {
  const container = document.getElementById('toast-container');
  if (!container) return;

  const toast = document.createElement('div');
  toast.className = `toast ${type}`;
  
  const icon = type === 'success' ? 
    `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="color: var(--accent);"><polyline points="20 6 9 17 4 12"></polyline></svg>` : 
    `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="color: var(--danger);"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="8" x2="12" y2="12"></line><line x1="12" y1="16" x2="12.01" y2="16"></line></svg>`;

  toast.innerHTML = `
    ${icon}
    <div style="font-size: 0.9rem; font-weight: 500;">${message}</div>
    <button class="toast-close" onclick="this.parentElement.remove()">&times;</button>
  `;

  container.appendChild(toast);
  
  // Auto-remove after 4 seconds
  setTimeout(() => {
    toast.style.animation = 'fadeOut 0.3s forwards';
    toast.addEventListener('animationend', () => toast.remove());
  }, 4000);
}

// ==========================================================================
// API Request Helpers (With JWT attachment)
// ==========================================================================
async function apiRequest(endpoint, options = {}) {
  const url = `${API_BASE}${endpoint}`;
  
  // Set headers
  const headers = new Headers(options.headers || {});
  if (state.currentUser && state.currentUser.token) {
    headers.set('Authorization', `Bearer ${state.currentUser.token}`);
  }
  
  if (options.body && !(options.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json');
  }

  const config = {
    ...options,
    headers
  };

  try {
    const response = await fetch(url, config);
    
    // Handle 204 No Content
    if (response.status === 204) {
      return null;
    }

    const data = await response.json();
    
    if (!response.ok) {
      // Handle unauthorized session expiration
      if (response.status === 401 && state.currentUser) {
        showToast('Session expired. Please log in again.', 'error');
        logout();
      }
      throw new Error(data.message || data.error || 'API Request failed');
    }
    
    return data;
  } catch (err) {
    console.error(`API Error on ${endpoint}:`, err);
    throw err;
  }
}

// REST wrapper methods
const api = {
  get: (endpoint) => apiRequest(endpoint, { method: 'GET' }),
  post: (endpoint, body) => apiRequest(endpoint, { method: 'POST', body: JSON.stringify(body) }),
  put: (endpoint, body) => apiRequest(endpoint, { method: 'PUT', body: JSON.stringify(body) }),
  delete: (endpoint) => apiRequest(endpoint, { method: 'DELETE' })
};

// ==========================================================================
// Authentication System
// ==========================================================================
function loadAuthFromStorage() {
  const token = localStorage.getItem('token');
  const username = localStorage.getItem('username');
  const role = localStorage.getItem('role');

  if (token && username) {
    state.currentUser = { token, username, role };
    updateNavForAuth();
  }
}

function saveAuthToStorage(authData) {
  localStorage.setItem('token', authData.token);
  localStorage.setItem('username', authData.username);
  localStorage.setItem('role', authData.role);
  state.currentUser = authData;
  updateNavForAuth();
}

function logout() {
  localStorage.removeItem('token');
  localStorage.removeItem('username');
  localStorage.removeItem('role');
  state.currentUser = null;
  updateNavForAuth();
  showToast('Logged out successfully.');
  window.location.hash = '#/';
}

function updateNavForAuth() {
  const authContainer = document.getElementById('nav-auth-container');
  const authOnlyLinks = document.querySelectorAll('.auth-only');
  const adminOnlyLinks = document.querySelectorAll('.admin-only');

  if (state.currentUser) {
    // Logged In navbar view
    authOnlyLinks.forEach(link => link.style.display = 'block');
    
    if (state.currentUser.role === 'ROLE_ADMIN' || state.currentUser.role === 'ADMIN') {
      adminOnlyLinks.forEach(link => link.style.display = 'block');
    } else {
      adminOnlyLinks.forEach(link => link.style.display = 'none');
    }

    authContainer.innerHTML = `
      <div style="display: flex; align-items: center; gap: 1rem;">
        <span style="font-size: 0.85rem; color: var(--text-secondary);">
          Hi, <strong style="color: var(--text-primary); font-family: var(--font-display);">${state.currentUser.username}</strong>
        </span>
        <button onclick="logout()" class="btn btn-secondary" style="padding: 0.4rem 0.9rem; font-size: 0.85rem;">Sign Out</button>
      </div>
    `;
  } else {
    // Logged Out navbar view
    authOnlyLinks.forEach(link => link.style.display = 'none');
    adminOnlyLinks.forEach(link => link.style.display = 'none');
    authContainer.innerHTML = `
      <a href="#/auth" class="nav-link btn-primary" id="nav-login">Sign In</a>
    `;
  }
}

// Switch Login/Register Tabs
function switchAuthTab(tab) {
  const loginForm = document.getElementById('login-form');
  const registerForm = document.getElementById('register-form');
  const tabLogin = document.getElementById('tab-login');
  const tabRegister = document.getElementById('tab-register');

  if (tab === 'login') {
    loginForm.style.display = 'block';
    registerForm.style.display = 'none';
    tabLogin.classList.add('active');
    tabRegister.classList.remove('active');
  } else {
    loginForm.style.display = 'none';
    registerForm.style.display = 'block';
    tabLogin.classList.remove('active');
    tabRegister.classList.add('active');
  }
}

// ==========================================================================
// SPA Router & Navigation
// ==========================================================================
function routeHandler() {
  const hash = window.location.hash || '#/';
  
  // Hide all panels
  document.querySelectorAll('.view-panel').forEach(panel => {
    panel.classList.remove('active');
  });

  // Reset active navbar link
  document.querySelectorAll('.nav-link').forEach(link => {
    link.classList.remove('active');
  });

  // Dynamic Route Matching
  if (hash === '#/' || hash === '') {
    document.getElementById('feed-view').classList.add('active');
    document.getElementById('nav-feed').classList.add('active');
    loadFeed();
    loadCategoriesAndTags();
  } else if (hash.startsWith('#/post/')) {
    const postId = hash.split('#/post/')[1];
    document.getElementById('post-view').classList.add('active');
    loadPostDetail(postId);
  } else if (hash.startsWith('#/editor')) {
    if (!state.currentUser) {
      showToast('You must be logged in to create or edit posts.', 'error');
      window.location.hash = '#/auth';
      return;
    }
    document.getElementById('editor-view').classList.add('active');
    document.getElementById('nav-write').classList.add('active');
    
    const parts = hash.split('#/editor/');
    const postId = parts.length > 1 ? parts[1] : null;
    openEditor(postId);
  } else if (hash === '#/auth') {
    if (state.currentUser) {
      window.location.hash = '#/';
      return;
    }
    document.getElementById('auth-view').classList.add('active');
  } else if (hash === '#/profile') {
    if (!state.currentUser) {
      window.location.hash = '#/auth';
      return;
    }
    document.getElementById('profile-view').classList.add('active');
    document.getElementById('nav-dashboard').classList.add('active');
    loadProfile();
  } else if (hash === '#/admin') {
    if (!state.currentUser || !(state.currentUser.role === 'ROLE_ADMIN' || state.currentUser.role === 'ADMIN')) {
      showToast('Access denied.', 'error');
      window.location.hash = '#/';
      return;
    }
    document.getElementById('admin-view').classList.add('active');
    document.getElementById('nav-admin').classList.add('active');
    loadAdminPanel();
  } else {
    // Fallback to home
    window.location.hash = '#/';
  }
}

// ==========================================================================
// 1. HOME FEED VIEW LOGIC
// ==========================================================================
async function loadFeed() {
  const loading = document.getElementById('posts-loading');
  const empty = document.getElementById('posts-empty');
  const grid = document.getElementById('posts-grid');
  
  grid.innerHTML = '';
  loading.style.display = 'block';
  empty.style.display = 'none';

  // Construct Query Params
  let query = `?page=${state.pagination.page}&size=${state.pagination.size}&sortBy=createdAt&sortDir=desc`;
  if (state.filters.search) query += `&search=${encodeURIComponent(state.filters.search)}`;
  if (state.filters.category) query += `&category=${encodeURIComponent(state.filters.category)}`;
  if (state.filters.tag) query += `&tag=${encodeURIComponent(state.filters.tag)}`;

  try {
    const res = await api.get(`/posts${query}`);
    loading.style.display = 'none';

    if (!res.content || res.content.length === 0) {
      empty.style.display = 'block';
      renderPagination(0, true);
      return;
    }

    state.pagination.totalPages = res.totalPages;
    state.pagination.isLast = res.isLast;

    res.content.forEach(post => {
      const card = document.createElement('div');
      card.className = 'glass-card post-card';
      card.onclick = () => window.location.hash = `#/post/${post.id}`;

      const initial = post.author.displayName ? post.author.displayName.charAt(0).toUpperCase() : 'U';
      const formattedDate = new Date(post.createdAt).toLocaleDateString(undefined, {
        month: 'short',
        day: 'numeric',
        year: 'numeric'
      });

      const categoriesStr = post.categories && post.categories.length > 0 ? 
        post.categories.map(c => `<span style="font-weight:700; color:var(--secondary);">${c.name}</span>`).join(', ') : 'Uncategorized';

      card.innerHTML = `
        <div class="post-card-meta">
          <div class="post-category">${categoriesStr}</div>
          <div>${formattedDate}</div>
        </div>
        <h3>${escapeHtml(post.title)}</h3>
        <p>${escapeHtml(post.excerpt || post.content.substring(0, 150) + '...')}</p>
        <div class="post-card-footer">
          <div class="post-author">
            <div class="author-avatar">${initial}</div>
            <span class="author-name">${escapeHtml(post.author.displayName)}</span>
          </div>
          <div class="post-stats">
            <div class="stat-item" title="Likes">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor" style="color: var(--danger);"><path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"></path></svg>
              <span>${post.likesCount}</span>
            </div>
          </div>
        </div>
      `;
      grid.appendChild(card);
    });

    renderPagination(res.totalPages, res.isLast);

  } catch (err) {
    loading.style.display = 'none';
    showToast('Failed to load blog posts.', 'error');
  }
}

async function loadCategoriesAndTags() {
  try {
    const categories = await api.get('/categories');
    const tags = await api.get('/tags');

    state.categories = categories;
    state.tags = tags;

    // Render Category Chips
    const catContainer = document.getElementById('category-chips');
    catContainer.innerHTML = `<span class="chip ${!state.filters.category ? 'active' : ''}" onclick="filterByCategory('')">All</span>`;
    categories.forEach(cat => {
      const activeClass = state.filters.category === cat.name ? 'active' : '';
      catContainer.innerHTML += `<span class="chip ${activeClass}" onclick="filterByCategory('${cat.name}')">${escapeHtml(cat.name)}</span>`;
    });

    // Render Tag Chips
    const tagContainer = document.getElementById('tag-chips');
    tagContainer.innerHTML = `<span class="chip ${!state.filters.tag ? 'active' : ''}" onclick="filterByTag('')">All</span>`;
    tags.forEach(tag => {
      const activeClass = state.filters.tag === tag.name ? 'active' : '';
      tagContainer.innerHTML += `<span class="chip ${activeClass}" onclick="filterByTag('${tag.name}')">#${escapeHtml(tag.name)}</span>`;
    });
  } catch (err) {
    console.error('Failed to load categories/tags chips', err);
  }
}

function filterByCategory(categoryName) {
  state.filters.category = categoryName;
  state.pagination.page = 0;
  loadFeed();
  loadCategoriesAndTags();
}

function filterByTag(tagName) {
  state.filters.tag = tagName;
  state.pagination.page = 0;
  loadFeed();
  loadCategoriesAndTags();
}

function renderPagination(totalPages, isLast) {
  const container = document.getElementById('pagination-container');
  container.innerHTML = '';
  
  if (totalPages <= 1) return;

  const prevBtn = document.createElement('button');
  prevBtn.className = 'btn btn-secondary';
  prevBtn.disabled = state.pagination.page === 0;
  prevBtn.innerHTML = '&larr; Previous';
  prevBtn.onclick = () => {
    state.pagination.page--;
    loadFeed();
  };
  container.appendChild(prevBtn);

  const indicator = document.createElement('span');
  indicator.style.color = 'var(--text-secondary)';
  indicator.style.fontSize = '0.9rem';
  indicator.innerText = `Page ${state.pagination.page + 1} of ${totalPages}`;
  container.appendChild(indicator);

  const nextBtn = document.createElement('button');
  nextBtn.className = 'btn btn-secondary';
  nextBtn.disabled = isLast;
  nextBtn.innerHTML = 'Next &rarr;';
  nextBtn.onclick = () => {
    state.pagination.page++;
    loadFeed();
  };
  container.appendChild(nextBtn);
}

// ==========================================================================
// 2. POST DETAIL SCREEN
// ==========================================================================
let currentDetailPostId = null;

async function loadPostDetail(id) {
  currentDetailPostId = id;
  const topFormWrapper = document.getElementById('comment-form-wrapper');
  const loginPrompt = document.getElementById('comment-form-login-prompt');

  if (state.currentUser) {
    topFormWrapper.style.display = 'block';
    loginPrompt.style.display = 'none';
  } else {
    topFormWrapper.style.display = 'none';
    loginPrompt.style.display = 'block';
  }

  try {
    const post = await api.get(`/posts/${id}`);
    
    document.getElementById('post-detail-title').innerText = post.title;
    
    const initial = post.author.displayName ? post.author.displayName.charAt(0).toUpperCase() : 'U';
    document.getElementById('post-detail-avatar').innerText = initial;
    document.getElementById('post-detail-author-name').innerText = post.author.displayName;
    document.getElementById('post-detail-date').innerText = new Date(post.createdAt).toLocaleDateString(undefined, {
      month: 'long',
      day: 'numeric',
      year: 'numeric'
    });

    // Content body (HTML rendering or text)
    document.getElementById('post-detail-content').innerHTML = post.content.replace(/\n/g, '<br>');

    // Categories
    const catContainer = document.getElementById('post-detail-categories');
    catContainer.innerHTML = '';
    if (post.categories) {
      post.categories.forEach(cat => {
        catContainer.innerHTML += `<span class="chip active">${escapeHtml(cat.name)}</span>`;
      });
    }

    // Tags
    const tagsContainer = document.getElementById('post-detail-tags');
    tagsContainer.innerHTML = '';
    if (post.tags) {
      post.tags.forEach(tag => {
        tagsContainer.innerHTML += `<span class="tag-badge">#${escapeHtml(tag.name)}</span>`;
      });
    }

    // Liked Button Status
    const likeBtn = document.getElementById('post-like-btn');
    const likeText = document.getElementById('post-like-text');
    const likesCountEl = document.getElementById('post-likes-count');

    likesCountEl.innerText = post.likesCount;
    if (post.liked) {
      likeBtn.classList.add('liked');
      likeText.innerText = 'Liked';
    } else {
      likeBtn.classList.remove('liked');
      likeText.innerText = 'Like';
    }

    likeBtn.onclick = () => togglePostLike(post.id);

    // Edit/Delete Owner actions
    const ownerActions = document.querySelector('.owner-actions');
    const editBtn = document.getElementById('post-edit-btn');
    const deleteBtn = document.getElementById('post-delete-btn');

    const canModify = state.currentUser && 
      (state.currentUser.username === post.author.username || 
       state.currentUser.role === 'ROLE_ADMIN' || 
       state.currentUser.role === 'ADMIN');

    if (canModify) {
      ownerActions.style.display = 'flex';
      editBtn.href = `#/editor/${post.id}`;
      deleteBtn.onclick = () => deletePost(post.id);
    } else {
      ownerActions.style.display = 'none';
    }

    // Load Comments list
    loadComments(post.id);

  } catch (err) {
    showToast('Failed to load post details.', 'error');
    window.location.hash = '#/';
  }
}

async function togglePostLike(postId) {
  if (!state.currentUser) {
    showToast('Please sign in to like this post.', 'warning');
    window.location.hash = '#/auth';
    return;
  }

  try {
    const res = await api.post(`/posts/${postId}/like`);
    // Re-load post metadata to update likes counts
    loadPostDetail(postId);
  } catch (err) {
    showToast('Failed to like post.', 'error');
  }
}

async function deletePost(postId) {
  if (!confirm('Are you sure you want to delete this post? This action cannot be undone.')) return;

  try {
    await api.delete(`/posts/${postId}`);
    showToast('Post deleted successfully.');
    window.location.hash = '#/';
  } catch (err) {
    showToast('Failed to delete post.', 'error');
  }
}

// ==========================================================================
// COMMENT SYSTEMS & RECURSIVE THREAD RENDERING
// ==========================================================================
async function loadComments(postId) {
  const treeContainer = document.getElementById('comments-tree');
  treeContainer.innerHTML = '<li style="text-align:center; color:var(--text-muted);">Loading comments...</li>';

  try {
    const res = await api.get(`/posts/${postId}/comments?page=0&size=50`);
    treeContainer.innerHTML = '';
    
    const commentsList = res.content || [];
    
    // Count total comments recursively
    const countTotal = (nodes) => {
      let count = 0;
      nodes.forEach(n => {
        count += 1;
        if (n.replies) count += countTotal(n.replies);
      });
      return count;
    };
    
    document.getElementById('comments-count').innerText = countTotal(commentsList);

    if (commentsList.length === 0) {
      treeContainer.innerHTML = '<li style="text-align:center; color:var(--text-muted); padding: 1.5rem;">No discussions yet. Start the conversation below!</li>';
      return;
    }

    // Loop top-level nodes
    commentsList.forEach(comment => {
      const nodeHtml = buildCommentNode(comment);
      treeContainer.appendChild(nodeHtml);
    });

  } catch (err) {
    treeContainer.innerHTML = '<li style="text-align:center; color:var(--danger);">Failed to load discussion comments.</li>';
  }
}

// HTML Generator for recursive comments
function buildCommentNode(comment) {
  const li = document.createElement('li');
  li.className = 'comment-node';
  li.id = `comment-node-${comment.id}`;

  const initial = comment.author.displayName ? comment.author.displayName.charAt(0).toUpperCase() : 'U';
  const formattedDate = new Date(comment.createdAt).toLocaleDateString(undefined, {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  });

  const isOwner = state.currentUser && 
    (state.currentUser.username === comment.author.username || 
     state.currentUser.role === 'ROLE_ADMIN' || 
     state.currentUser.role === 'ADMIN');

  let actionButtons = '';
  if (state.currentUser) {
    actionButtons += `
      <button class="comment-action-btn" onclick="showReplyForm(${comment.id})">
        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="15 3 21 3 21 9"></polygon><path d="M9 18v-6H3v6h6z"></path><path d="M21 3l-6 6"></path><path d="M21 9v10a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V9a2 2 0 0 1 2-2h8"></path></svg>
        Reply
      </button>
    `;
    if (isOwner) {
      actionButtons += `
        <button class="comment-action-btn" onclick="showEditCommentForm(${comment.id})">Edit</button>
        <button class="comment-action-btn danger-btn" onclick="deleteComment(${comment.id})">Delete</button>
      `;
    }
  }

  li.innerHTML = `
    <div class="comment-card" id="comment-card-${comment.id}">
      <div class="comment-header">
        <div class="comment-author-info">
          <div class="author-avatar" style="width:24px; height:24px; font-size:0.7rem;">${initial}</div>
          <span class="author-name" style="font-size:0.85rem;">${escapeHtml(comment.author.displayName)}</span>
        </div>
        <div class="comment-time">${formattedDate}</div>
      </div>
      <div class="comment-body" id="comment-body-text-${comment.id}">
        ${escapeHtml(comment.content)}
      </div>
      <div class="comment-actions">
        ${actionButtons}
      </div>
      <!-- Inline Containers -->
      <div id="reply-container-${comment.id}"></div>
    </div>
  `;

  // Render children replies recursively if any
  if (comment.replies && comment.replies.length > 0) {
    const ul = document.createElement('ul');
    ul.className = 'comment-replies';
    
    comment.replies.forEach(reply => {
      const replyNode = buildCommentNode(reply);
      ul.appendChild(replyNode);
    });
    
    li.appendChild(ul);
  }

  return li;
}

// Inline Reply Textbox creation
function showReplyForm(commentId) {
  // Remove existing reply forms to prevent duplicate textareas
  const existing = document.querySelector('.reply-form-container');
  if (existing) existing.remove();

  const container = document.getElementById(`reply-container-${commentId}`);
  const formDiv = document.createElement('div');
  formDiv.className = 'reply-form-container';
  formDiv.innerHTML = `
    <textarea id="reply-text-${commentId}" class="textarea-input" style="min-height:70px; font-size:0.9rem;" placeholder="Write a public reply..." required></textarea>
    <div style="display:flex; gap:0.5rem; justify-content:flex-end;">
      <button class="btn btn-secondary" style="padding:0.35rem 0.8rem; font-size:0.8rem;" onclick="this.parentElement.parentElement.remove()">Cancel</button>
      <button class="btn btn-primary" style="padding:0.35rem 0.8rem; font-size:0.8rem;" onclick="submitReply(${commentId})">Post Reply</button>
    </div>
  `;
  container.appendChild(formDiv);
  document.getElementById(`reply-text-${commentId}`).focus();
}

async function submitReply(parentId) {
  const textEl = document.getElementById(`reply-text-${parentId}`);
  const content = textEl.value.trim();
  if (!content) return;

  try {
    await api.post(`/posts/${currentDetailPostId}/comments`, {
      content,
      parentId
    });
    showToast('Reply posted successfully.');
    loadComments(currentDetailPostId);
  } catch (err) {
    showToast('Failed to post reply.', 'error');
  }
}

// Inline Edit Textbox creation
function showEditCommentForm(commentId) {
  const card = document.getElementById(`comment-card-${commentId}`);
  const bodyTextEl = document.getElementById(`comment-body-text-${commentId}`);
  const actionsEl = card.querySelector('.comment-actions');
  const currentContent = bodyTextEl.innerText.trim();

  // Hide content and buttons
  bodyTextEl.style.display = 'none';
  actionsEl.style.display = 'none';

  const editDiv = document.createElement('div');
  editDiv.className = 'reply-form-container';
  editDiv.id = `edit-form-container-${commentId}`;
  editDiv.style.borderLeftColor = 'var(--secondary)';
  editDiv.innerHTML = `
    <textarea id="edit-comment-text-${commentId}" class="textarea-input" style="min-height:70px; font-size:0.9rem;" required>${currentContent}</textarea>
    <div style="display:flex; gap:0.5rem; justify-content:flex-end;">
      <button class="btn btn-secondary" style="padding:0.35rem 0.8rem; font-size:0.8rem;" onclick="cancelCommentEdit(${commentId})">Cancel</button>
      <button class="btn btn-primary" style="padding:0.35rem 0.8rem; font-size:0.8rem; background:linear-gradient(135deg, var(--secondary) 0%, var(--primary) 100%);" onclick="submitCommentEdit(${commentId})">Update</button>
    </div>
  `;
  card.appendChild(editDiv);
  document.getElementById(`edit-comment-text-${commentId}`).focus();
}

function cancelCommentEdit(commentId) {
  document.getElementById(`edit-form-container-${commentId}`).remove();
  document.getElementById(`comment-body-text-${commentId}`).style.display = 'block';
  document.getElementById(`comment-card-${commentId}`).querySelector('.comment-actions').style.display = 'flex';
}

async function submitCommentEdit(commentId) {
  const content = document.getElementById(`edit-comment-text-${commentId}`).value.trim();
  if (!content) return;

  try {
    await api.put(`/comments/${commentId}`, { content });
    showToast('Comment updated.');
    loadComments(currentDetailPostId);
  } catch (err) {
    showToast('Failed to update comment.', 'error');
  }
}

async function deleteComment(commentId) {
  if (!confirm('Are you sure you want to delete this comment? Nested replies will also be deleted.')) return;

  try {
    await api.delete(`/comments/${commentId}`);
    showToast('Comment deleted successfully.');
    loadComments(currentDetailPostId);
  } catch (err) {
    showToast('Failed to delete comment.', 'error');
  }
}

// ==========================================================================
// 3. POST WRITER / EDITOR SCREEN
// ==========================================================================
async function openEditor(postId = null) {
  const form = document.getElementById('post-editor-form');
  form.reset();

  document.getElementById('edit-post-id').value = postId || '';
  const titleEl = document.getElementById('editor-title');
  const submitBtnEl = document.getElementById('editor-submit-btn');

  if (postId) {
    titleEl.innerText = 'Edit Post';
    submitBtnEl.innerText = 'Update Post';
    try {
      const post = await api.get(`/posts/${postId}`);
      
      document.getElementById('post-title').value = post.title;
      document.getElementById('post-excerpt').value = post.excerpt || '';
      document.getElementById('post-content').value = post.content;
      document.getElementById('post-status').value = post.status;
      
      const catsStr = post.categories ? post.categories.map(c => c.name).join(', ') : '';
      const tagsStr = post.tags ? post.tags.map(t => t.name).join(', ') : '';
      
      document.getElementById('post-categories').value = catsStr;
      document.getElementById('post-tags').value = tagsStr;
    } catch (err) {
      showToast('Failed to load post data for editing.', 'error');
      window.location.hash = '#/';
    }
  } else {
    titleEl.innerText = 'Create New Post';
    submitBtnEl.innerText = 'Publish Post';
  }
}

// ==========================================================================
// 5. USER PROFILE SCREEN
// ==========================================================================
async function loadProfile() {
  const myPostsGrid = document.getElementById('my-posts-grid');
  myPostsGrid.innerHTML = '<div style="text-align:center; grid-column: 1/-1;">Loading your posts...</div>';

  try {
    // 1. Load active user profile metadata
    const user = await api.get('/auth/me');
    
    document.getElementById('profile-display-name').innerText = user.displayName;
    document.getElementById('profile-username').innerText = `@${user.username}`;
    document.getElementById('profile-bio').innerText = user.bio || 'No bio written yet. Edit profile to write something!';
    
    const initial = user.displayName ? user.displayName.charAt(0).toUpperCase() : 'U';
    document.getElementById('profile-avatar').innerText = initial;
    
    // Clean user role text (e.g. ROLE_ADMIN -> ADMIN)
    const cleanRole = user.role ? user.role.replace('ROLE_', '') : 'USER';
    document.getElementById('profile-role').innerText = cleanRole;

    // 2. Load current user's posts
    const myPosts = await api.get('/posts/my?page=0&size=50');
    myPostsGrid.innerHTML = '';

    document.getElementById('profile-posts-count').innerText = myPosts.totalElements;

    if (!myPosts.content || myPosts.content.length === 0) {
      myPostsGrid.innerHTML = '<div style="text-align:center; grid-column: 1/-1; color: var(--text-muted);" class="glass-card">You have not created any posts yet. Click "Write Post" above!</div>';
      return;
    }

    myPosts.content.forEach(post => {
      const card = document.createElement('div');
      card.className = 'glass-card post-card';
      
      const formattedDate = new Date(post.createdAt).toLocaleDateString(undefined, {
        month: 'short',
        day: 'numeric',
        year: 'numeric'
      });

      const catsStr = post.categories && post.categories.length > 0 ? 
        post.categories.map(c => c.name).join(', ') : 'Uncategorized';

      const isPublished = post.status === 'PUBLISHED';
      const statusBadge = isPublished ? 
        `<span style="background:var(--accent-glow); color:#A7F3D0; font-size:0.75rem;" class="chip">Published</span>` :
        `<span style="background:rgba(245,158,11,0.15); color:#FDE68A; font-size:0.75rem;" class="chip">Draft</span>`;

      card.innerHTML = `
        <div class="post-card-meta">
          <div>Category: <strong>${escapeHtml(catsStr)}</strong></div>
          <div>${statusBadge}</div>
        </div>
        <h3 onclick="window.location.hash='#/post/${post.id}'">${escapeHtml(post.title)}</h3>
        <p onclick="window.location.hash='#/post/${post.id}'">${escapeHtml(post.excerpt || post.content.substring(0, 100) + '...')}</p>
        <div class="post-card-footer">
          <span style="font-size:0.8rem; color:var(--text-muted);">${formattedDate}</span>
          <div style="display:flex; gap:0.5rem;">
            <a href="#/editor/${post.id}" class="btn btn-secondary" style="padding:0.35rem 0.75rem; font-size:0.8rem;">Edit</a>
            <button onclick="deletePost(${post.id})" class="btn btn-danger" style="padding:0.35rem 0.75rem; font-size:0.8rem;">Delete</button>
          </div>
        </div>
      `;
      myPostsGrid.appendChild(card);
    });

  } catch (err) {
    myPostsGrid.innerHTML = '<div style="text-align:center; grid-column: 1/-1; color: var(--danger);">Failed to load profile dashboard details.</div>';
  }
}

// ==========================================================================
// 6. ADMIN CONTROL CENTER LOGIC
// ==========================================================================
async function loadAdminPanel() {
  const catBody = document.getElementById('admin-categories-body');
  const tagBody = document.getElementById('admin-tags-body');

  catBody.innerHTML = '<tr><td colspan="3">Loading...</td></tr>';
  tagBody.innerHTML = '<tr><td colspan="3">Loading...</td></tr>';

  try {
    const categories = await api.get('/categories');
    const tags = await api.get('/tags');

    // Load categories
    catBody.innerHTML = '';
    categories.forEach(cat => {
      catBody.innerHTML += `
        <tr>
          <td>${cat.id}</td>
          <td><strong>${escapeHtml(cat.name)}</strong></td>
          <td>
            <button onclick="openCategoryDialog(${cat.id}, '${escapeHtml(cat.name)}', '${escapeHtml(cat.description || '')}')" class="btn btn-secondary" style="padding:0.3rem 0.6rem; font-size:0.75rem;">Edit</button>
            <button onclick="deleteCategory(${cat.id})" class="btn btn-danger" style="padding:0.3rem 0.6rem; font-size:0.75rem;">Delete</button>
          </td>
        </tr>
      `;
    });

    // Load tags
    tagBody.innerHTML = '';
    tags.forEach(tag => {
      tagBody.innerHTML += `
        <tr>
          <td>${tag.id}</td>
          <td><span class="tag-badge">#${escapeHtml(tag.name)}</span></td>
          <td>
            <button onclick="deleteTag(${tag.id})" class="btn btn-danger" style="padding:0.3rem 0.6rem; font-size:0.75rem;">Delete</button>
          </td>
        </tr>
      `;
    });
  } catch (err) {
    showToast('Failed to load admin management tables.', 'error');
  }
}

// Dialog Modal Actions (Category)
function openCategoryDialog(id = null, name = '', desc = '') {
  document.getElementById('cat-id').value = id || '';
  document.getElementById('cat-name').value = name;
  document.getElementById('cat-desc').value = desc;
  document.getElementById('category-dialog').showModal();
}

function closeCategoryDialog() {
  document.getElementById('category-dialog').close();
}

async function saveCategory(event) {
  event.preventDefault();
  const id = document.getElementById('cat-id').value;
  const name = document.getElementById('cat-name').value.trim();
  const description = document.getElementById('cat-desc').value.trim();

  try {
    if (id) {
      // Edit
      await api.put(`/categories/${id}`, { name, description });
      showToast('Category updated successfully.');
    } else {
      // Add
      await api.post('/categories', { name, description });
      showToast('Category created successfully.');
    }
    closeCategoryDialog();
    loadAdminPanel();
  } catch (err) {
    showToast(err.message || 'Failed to save category.', 'error');
  }
}

async function deleteCategory(id) {
  if (!confirm('Are you sure you want to delete this category?')) return;
  try {
    await api.delete(`/categories/${id}`);
    showToast('Category deleted successfully.');
    loadAdminPanel();
  } catch (err) {
    showToast('Failed to delete category.', 'error');
  }
}

// Dialog Modal Actions (Tag)
function openTagDialog() {
  document.getElementById('tag-name').value = '';
  document.getElementById('tag-dialog').showModal();
}

function closeTagDialog() {
  document.getElementById('tag-dialog').close();
}

async function saveTag(event) {
  event.preventDefault();
  const name = document.getElementById('tag-name').value.trim();

  try {
    await api.post('/tags', { name });
    showToast('Tag created successfully.');
    closeTagDialog();
    loadAdminPanel();
  } catch (err) {
    showToast(err.message || 'Failed to save tag.', 'error');
  }
}

async function deleteTag(id) {
  if (!confirm('Are you sure you want to delete this tag?')) return;
  try {
    await api.delete(`/tags/${id}`);
    showToast('Tag deleted successfully.');
    loadAdminPanel();
  } catch (err) {
    showToast('Failed to delete tag.', 'error');
  }
}

// ==========================================================================
// Dialog Backdrop Light-Dismiss Click Listeners (WebKit Fallback)
// ==========================================================================
function setupDialogLightDismiss() {
  const dialogs = document.querySelectorAll('dialog');
  dialogs.forEach(dialog => {
    // If browser supports 'closedBy', standard closedby="any" handles it.
    // If Safari/Legacy, use coordinate clicks checking
    if (!('closedBy' in HTMLDialogElement.prototype)) {
      dialog.addEventListener('click', (event) => {
        if (event.target !== dialog) return;
        const rect = dialog.getBoundingClientRect();
        const isDialogContent = (
          rect.top <= event.clientY &&
          event.clientY <= rect.top + rect.height &&
          rect.left <= event.clientX &&
          event.clientX <= rect.left + rect.width
        );
        if (!isDialogContent) {
          dialog.close();
        }
      });
    }
  });
}

// ==========================================================================
// Form Action Submissions
// ==========================================================================
document.addEventListener('DOMContentLoaded', () => {
  // Load local auth
  loadAuthFromStorage();

  // Setup dialog coordinate click backdrops
  setupDialogLightDismiss();

  // Route Listener
  window.addEventListener('hashchange', routeHandler);
  
  // Trigger initial route
  routeHandler();

  // 1. Search Bar keyboard typing filter
  let searchTimeout;
  const searchBar = document.getElementById('search-bar');
  if (searchBar) {
    searchBar.addEventListener('input', (e) => {
      clearTimeout(searchTimeout);
      searchTimeout = setTimeout(() => {
        state.filters.search = e.target.value.trim();
        state.pagination.page = 0;
        loadFeed();
      }, 500); // 500ms debounce
    });
  }

  // 2. Login Form Submit
  const loginForm = document.getElementById('login-form');
  if (loginForm) {
    loginForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const usernameOrEmail = document.getElementById('login-username').value.trim();
      const password = document.getElementById('login-password').value;

      try {
        const res = await api.post('/auth/login', { usernameOrEmail, password });
        saveAuthToStorage(res);
        showToast('Successfully signed in. Welcome back!');
        window.location.hash = '#/';
      } catch (err) {
        showToast('Login failed. Please check credentials.', 'error');
      }
    });
  }

  // 3. Register Form Submit
  const regForm = document.getElementById('register-form');
  if (regForm) {
    regForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const username = document.getElementById('reg-username').value.trim();
      const email = document.getElementById('reg-email').value.trim();
      const displayName = document.getElementById('reg-displayname').value.trim();
      const bio = document.getElementById('reg-bio').value.trim();
      const password = document.getElementById('reg-password').value;

      try {
        await api.post('/auth/register', { username, email, displayName, bio, password });
        showToast('Registration successful! Please login with your details.');
        switchAuthTab('login');
      } catch (err) {
        showToast(err.message || 'Registration failed. Username or email may be taken.', 'error');
      }
    });
  }

  // 4. Post Form Editor Submit
  const editorForm = document.getElementById('post-editor-form');
  if (editorForm) {
    editorForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const id = document.getElementById('edit-post-id').value;
      const title = document.getElementById('post-title').value.trim();
      const excerpt = document.getElementById('post-excerpt').value.trim();
      const content = document.getElementById('post-content').value;
      const status = document.getElementById('post-status').value;
      
      const catsInput = document.getElementById('post-categories').value;
      const tagsInput = document.getElementById('post-tags').value;

      // Clean sets
      const categories = catsInput.split(',')
        .map(c => c.trim())
        .filter(c => c.length > 0);
      
      const tags = tagsInput.split(',')
        .map(t => t.trim())
        .filter(t => t.length > 0);

      const payload = { title, excerpt, content, status, categories, tags };

      try {
        if (id) {
          await api.put(`/posts/${id}`, payload);
          showToast('Blog post updated successfully!');
          window.location.hash = `#/post/${id}`;
        } else {
          const newPost = await api.post('/posts', payload);
          showToast('Blog post created successfully!');
          window.location.hash = `#/post/${newPost.id}`;
        }
      } catch (err) {
        showToast(err.message || 'Failed to save blog post.', 'error');
      }
    });
  }

  // 5. Main Top-Level Comment Form Submit
  const mainCommentForm = document.getElementById('comment-form');
  if (mainCommentForm) {
    mainCommentForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const textEl = document.getElementById('comment-text-input');
      const content = textEl.value.trim();
      if (!content) return;

      try {
        await api.post(`/posts/${currentDetailPostId}/comments`, {
          content,
          parentId: null
        });
        showToast('Comment posted successfully.');
        textEl.value = '';
        loadComments(currentDetailPostId);
      } catch (err) {
        showToast('Failed to post comment.', 'error');
      }
    });
  }
});

// Helper: Escape HTML strings to prevent XSS injection
function escapeHtml(str) {
  if (!str) return '';
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}
