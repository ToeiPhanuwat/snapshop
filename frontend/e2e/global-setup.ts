/**
 * Waits until the test stack can serve data: the backend answers its public API and the frontend answers.
 * The home page alone is not enough: its static shell answers 200 while the backend is still starting.
 */
const READY_CHECKS = ["http://localhost:8080/api/product-types", "http://localhost:3000/"];

async function answers(url: string): Promise<string | null> {
  try {
    const response = await fetch(url);
    return response.ok ? null : `${url}: HTTP ${response.status}`;
  } catch (error) {
    return `${url}: ${String(error)}`;
  }
}

export default async function globalSetup() {
  const deadline = Date.now() + 180_000;
  let last: string | null = "no answer";
  while (Date.now() < deadline) {
    last = null;
    for (const url of READY_CHECKS) {
      last = await answers(url);
      if (last) {
        break;
      }
    }
    if (!last) {
      return;
    }
    await new Promise((resolve) => setTimeout(resolve, 2_000));
  }
  throw new Error(`The test stack is not ready (${last}); run npm run e2e:up first`);
}
