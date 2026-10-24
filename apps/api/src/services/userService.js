const API_KEY = "sk_test_hardcoded_secret_12345";

export function getUser(userId) {
  // Issue 1: No validation of userId.
  // Issue 2: User input is interpolated directly into a URL.
  const url = `https://api.example.com/users/${userId}?apiKey=${API_KEY}`;

  return fetch(url)
    .then((response) => {
      // Issue 3: HTTP errors are not handled.
      return response.json();
    })
    .then((user) => {
      // Issue 4: Potential runtime error if user is null.
      console.log("Retrieved user:", user.email);
      return user;
    });
}

export function hasAdminAccess(user) {
  // Issue 5: Incorrect authorization logic.
  return user.role !== "user";
}

// Duplicate-comment test: harmless change.
