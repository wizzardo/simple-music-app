const RUNTIME = 'runtime';

self.addEventListener('install', event => {
    // console.log('sw.install')

    event.waitUntil((async () => {
        const cache = await caches.open(RUNTIME);
        await cache.addAll([
            '/'
        ]);
    })());
    self.skipWaiting();
});

async function fetchAndCache(request) {
    const response = await fetch(request)
    let contentType = response.headers.get('Content-Type');
    console.log('fetchAndCache', request.url, contentType, response.status)

    if (response.status === 200 && contentType && (!contentType.startsWith("audio") || request.url.endsWith('.mp3'))) {
        const cache = await caches.open(RUNTIME);
        await cache.put(request, response.clone())
    }
    // console.log('cached', request.url)
    return response
}

async function cachedOrFetch(request) {
    const cachedResponse = await caches.match(request);
    if (cachedResponse)
        return cachedResponse;
    return await fetchAndCache(request);
}

async function fetchOrCachedOnTimeout(request, timeout, clientId) {
    const cachedResponse = await caches.match(request);
    if (!cachedResponse)
        return fetchAndCache(request)

    return new Promise((resolve, reject) => {
        let resolved = false;
        fetchAndCache(request).then(response => {
            if (!resolved) {
                console.log('resolve fetched', request.url)
                resolve(response)
                resolved = true
            } else if (!!clientId) {
                const contentType = response.headers.get('Content-Type');
                if ('application/json' === contentType) {
                    response.json().then(json => {
                        self.clients.get(clientId).then(client => {
                            client.postMessage({type: 'FETCH', url: request.url, data: json})
                        })
                    })
                }
            }
        }).catch(reason => {
            if (!resolved) {
                console.log('resolve cached on error', request.url, reason)
                resolve(cachedResponse)
                resolved = true
            }
        })
        setTimeout(() => {
            if (!resolved) {
                console.log('resolve cached', request.url)
                resolve(cachedResponse)
                resolved = true
            }
        }, timeout)
    })
}

self.addEventListener('fetch', event => {
    const request = event.request;
    if (request.method !== 'GET')
        return;

    let url = request.url;
    const clientId = event.clientId;

    // if(url.endsWith('/')){
    //     debugger
    // }


    // console.log('sw.fetch', url)
    if (url.startsWith('http://localhost'))
        return

    if (url.startsWith('https://fonts.gstatic.com')) {
        event.respondWith(cachedOrFetch(request));
    } else if (url.startsWith('https://fonts.googleapis.com')) {
        event.respondWith(cachedOrFetch(request));
    } else if (url.startsWith('http') && url.endsWith('.css')) {
        event.respondWith(cachedOrFetch(request));
    } else if (url.startsWith('http') && url.endsWith('.js')) {
        event.respondWith(cachedOrFetch(request));
    } else if (url.startsWith(self.location.origin)) {
        if (request.headers.get('Accept').indexOf('text/html') !== -1) {
            fetchOrCachedOnTimeout(request, 30, clientId).then(response => {
                if (response.status === 404) {
                    event.respondWith(fetchOrCachedOnTimeout(
                        new Request(url.substring(0, url.indexOf('/', 10) + 1), {...request}), 300, clientId
                    ))
                } else {
                    event.respondWith(response)
                }
            })
        } else
            event.respondWith(fetchOrCachedOnTimeout(request, 30, clientId));
    }
});


// self.addEventListener('activate', event => {
//     // console.log('sw.activate')
//     event.waitUntil((async () => {
//         const cache = await caches.open(RUNTIME);
//         const keys = await cache.keys()
//         for (let i = 0; i < keys.length; i++) {
//             // console.log('key', keys[i])
//         }
//     })())
// });