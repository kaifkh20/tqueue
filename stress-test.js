// stress-test.js

const URL = 'http://localhost:8080/api/task/add'; // <-- Change this to your Spring Boot endpoint
const TOTAL_REQUESTS = 10;

// Helper to generate a random number between a min and max
const getRandomDuration = (min, max) => Math.floor(Math.random() * (max - min + 1)) + min;

// Helper to randomly pick true or false
const getRandomBool = () => Math.random() < 0.5;

async function sendRequest(id) {
  const payload = {
    taskName: `Task ${id}`,
    taskDescription: `Task ${id} description`,
 //   taskDuration: getRandomDuration(1, 15), // Random duration between 1 and 15
   	taskDuration : 60,
		shouldFail: getRandomBool()              // Randomly true or false
  };

  try {
    const response = await fetch(URL, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(payload),
    });

    if (response.ok) {
      return { id, status: 'SUCCESS', code: response.status };
    } else {
      return { id, status: 'FAILED_REPLY', code: response.status };
    }
  } catch (error) {
    // This catches network drops, connection refused, or timeouts
    return { id, status: 'DROPPED/ERROR', error: error.message };
  }
}

async function runTest() {
  console.log(`🚀 Starting stress test: Sending ${TOTAL_REQUESTS} requests to ${URL}...`);
  const startTime = Date.now();

  // Create an array of 1,000 promises executing simultaneously
  const promises = [];
  for (let i = 1; i <= TOTAL_REQUESTS; i++) {
    promises.push(sendRequest(i));
  }

  // Wait for all of them to finish
  const results = await Promise.all(promises);
  const duration = ((Date.now() - startTime) / 1000).toFixed(2);

  // Analyze the results
  const summary = results.reduce((acc, result) => {
    acc[result.status] = (acc[result.status] || 0) + 1;
    return acc;
  }, {});

  console.log('\n--- 📊 Test Results Summary ---');
  console.log(`Total Time Taken: ${duration} seconds`);
  console.log(`Successful Responses: ${summary['SUCCESS'] || 0}`);
  console.log(`Server Errors (e.g., 500/400): ${summary['FAILED_REPLY'] || 0}`);
  console.log(`Dropped/Network Refused: ${summary['DROPPED/ERROR'] || 0}`);
  
  if (summary['DROPPED/ERROR'] > 0) {
    console.log('\n⚠️ Some requests were dropped! Check if your Spring Boot app maxed out its queues.');
  }
}

runTest();
