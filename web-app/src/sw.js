const RUNTIME = 'runtime';

self.addEventListener('install', event => {
    console.log('sw.install')

    e.waitUntil((async () => {
        const cache = await caches.open(RUNTIME);
        console.log('[Service Worker] Caching all: app shell and content');
        await cache.addAll([
            '/'
        ]);
    })());
    // self.skipWaiting();
});

async function fetchAndCache(event) {
    const cache = await caches.open(RUNTIME);
    const response = await fetch(event.request)
    await cache.put(event.request, response.clone())
    console.log('cached', event.request.url)
    return response
}

async function cachedOrFetch(event) {
    const cachedResponse = await caches.match(event.request);
    if (cachedResponse)
        return cachedResponse;
    return await fetchAndCache(event);
}

async function fetchOrCachedOnTimeout(event, timeout) {
    const cachedResponse = await caches.match(event.request);
    if (!cachedResponse)
        return fetchAndCache(event)

    return new Promise((resolve, reject) => {
        let resolved = false;
        fetchAndCache(event).then(response => {
            if (!resolved) {
                console.log('resolve fetched', event.request.url)
                resolve(response)
                resolved = true
            }
        }).catch(reason => {
            if (!resolved) {
                console.log('resolve cached on error', event.request.url, reason)
                resolve(cachedResponse)
                resolved = true
            }
        })
        setTimeout(() => {
            if (!resolved) {
                console.log('resolve cached', event.request.url)
                resolve(cachedResponse)
                resolved = true
            }
        }, timeout)
    })
}

self.addEventListener('fetch', event => {
    let url = event.request.url;
    console.log('sw.fetch', url)
    // if (url.startsWith('http://localhost'))
    //     return

    if (url.startsWith('https://fonts.gstatic.com')) {
        event.respondWith(cachedOrFetch(event));
    } else if (url.startsWith('https://fonts.googleapis.com')) {
        event.respondWith(cachedOrFetch(event));
    } else if (url.startsWith('http') && url.endsWith('.css')) {
        event.respondWith(cachedOrFetch(event));
    } else if (url.startsWith('http') && url.endsWith('.js')) {
        event.respondWith(cachedOrFetch(event));
    } else if (url.startsWith(self.location.origin)) {
        event.respondWith(fetchOrCachedOnTimeout(event, 100));
    }
});


self.addEventListener('activate', event => {
    console.log('sw.activate')
    event.waitUntil((async () => {
        const cache = await caches.open(RUNTIME);
        const keys = await cache.keys()
        for (let i = 0; i < keys.length; i++) {
            console.log('key', keys[i])
        }
    })())
});