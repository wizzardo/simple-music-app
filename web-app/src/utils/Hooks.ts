import {useEffect, useState} from "react";
import {WINDOW} from "react-ui-basics/Tools";
import {useLocalCache, WebCacheEntry} from "../services/LocalCacheService";

export const useWindowSize = () => {
    const [windowsSize, setWindowSize] = useState({width: WINDOW.innerWidth, height: WINDOW.innerHeight})

    useEffect(() => {
        const listener = () => {
            setWindowSize({width: WINDOW.innerWidth, height: WINDOW.innerHeight})
        }
        WINDOW.addEventListener('resize', listener)
        return () => WINDOW.removeEventListener('resize', listener)
    }, [])
    return windowsSize
}

export const useIsSafari = () => {
    const [isSafari] = useState(() => /^((?!chrome|android).)*safari/i.test(navigator.userAgent))
    return isSafari
}

export const useAsync = <R>(f: () => Promise<R>): R => {
    const [value, setValue] = useState<R>();
    useEffect(() => {
        f().then(setValue).catch(console.error)
    }, [f])
    return value
}

export const useWebCache = (url): ArrayBuffer => {
    const localCache = useLocalCache();
    const [value, setValue] = useState<WebCacheEntry>();

    useEffect(() => {
        (async () => {
            if (!localCache)
                return;

            let cacheEntry = await localCache.getWebCacheEntry(url);
            if (cacheEntry)
                setValue(cacheEntry)

            const response = await fetch(url, {
                headers: {
                    'If-None-Match': cacheEntry?.etag || ''
                }
            });

            if (response.status == 200) {
                const etag = response.headers.get("ETag");
                debugger
                const blob = await response.blob();
                const data = await blob.arrayBuffer();
                cacheEntry = {
                    url,
                    etag,
                    data
                }
                localCache.addWebCacheEntry(cacheEntry)
                setValue(cacheEntry)
            } else if (response.status == 304) {
                return
            } else {
                console.log("unexpected response", url, response.status, response.text())
            }
        })()
    }, [url, localCache])

    return value?.data
}