// Service worker mock file to prevent NoResourceFoundException
self.addEventListener('install', (event) => {
    // Skip installation phase
    self.skipWaiting();
});

self.addEventListener('activate', (event) => {
    // Claim clients immediately
    event.waitUntil(self.clients.claim());
});
