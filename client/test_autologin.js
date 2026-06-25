const axios = require('axios');

const axiosClient = axios.create({
  baseURL: 'http://localhost:8080',
  headers: {
    'Content-Type': 'application/json',
  },
});

axiosClient.interceptors.request.use((config) => {
  // no local storage in node, but we'll mock it empty
  return config;
});

async function runTest() {
  const email = `test_${Date.now()}@example.com`;
  const password = "password123";
  
  console.log("Registering...", email);
  try {
    const res = await axiosClient.post('/api/auth/register', {
      name: "Auto Login Test",
      email: email,
      password: password,
      role: "CUSTOMER"
    });
    console.log("Register response:", res.status);
  } catch (err) {
    console.error("Register failed:", err.response?.status, err.response?.data);
    return;
  }

  console.log("Logging in immediately...");
  try {
    const loginRes = await axiosClient.post('/api/auth/authenticate', {
      email: email,
      password: password
    });
    console.log("Login response:", loginRes.status, loginRes.data);
  } catch (err) {
    console.error("Login failed:", err.response?.status, err.response?.data);
  }
}

runTest();
