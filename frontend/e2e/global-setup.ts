/** Waits until the test stack answers on the home page (the backend and database are up behind it). */
export default async function globalSetup() {
  const deadline = Date.now() + 180_000;
  let last = "no answer";
  while (Date.now() < deadline) {
    try {
      const response = await fetch("http://localhost:3000/");
      if (response.ok) {
        return;
      }
      last = `HTTP ${response.status}`;
    } catch (error) {
      last = String(error);
    }
    await new Promise((resolve) => setTimeout(resolve, 2_000));
  }
  throw new Error(`The test stack is not up on http://localhost:3000 (${last}); run npm run e2e:up first`);
}
