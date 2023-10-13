import {useEffect, useMemo, useState} from "react";
import {WINDOW} from "react-ui-basics/Tools";
import {useLocalCache, WebCacheEntry} from "../services/LocalCacheService";
import * as BlobStore from "../stores/BlobStore";
import {useStore} from "react-ui-basics/store/Store";

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
            if (!url)
                return;

            let cacheEntry = await localCache.getWebCacheEntry(url);
            if (cacheEntry)
                setValue(cacheEntry)

            try {
                const response = await fetch(url, {
                    headers: {
                        'If-None-Match': cacheEntry?.etag || ''
                    }
                });

                if (response.status == 200) {
                    const etag = response.headers.get("ETag");
                    const blob = await response.blob();
                    const data = await blob.arrayBuffer();
                    cacheEntry = {
                        url,
                        etag,
                        data
                    }
                    if (etag)
                        localCache.addWebCacheEntry(cacheEntry)
                    setValue(cacheEntry)
                } else if (response.status == 304) {
                    return
                } else {
                    console.log("unexpected response", url, response.status, response.text())
                }
            } catch (e) {
                console.error(`Failed to load ${url}`, e)
            }
        })()
    }, [url, localCache])

    return value?.data
}

export const useIsShownOnScreen = (element: Element) => {
    const [isShown, setIsShown] = useState(false);

    const observer = useMemo(() => new IntersectionObserver(
        ([entry]) => {
            setIsShown(entry.isIntersecting);
        },
    ), []);

    useEffect(() => {
        if (!element)
            return;

        observer.observe(element);

        return () => {
            observer.disconnect();
        };
    }, [element, observer]);

    return isShown;
};

export const useBlobUrl = (buffer: ArrayBuffer) => {
    const [url, setUrl] = useState<string>()

    useEffect(() => {
        if (!buffer)
            return;

        const blob = new Blob([buffer]);
        const blobUrl = URL.createObjectURL(blob);
        setUrl(blobUrl);
        return () => URL.revokeObjectURL(blobUrl);
    }, [buffer])

    return url
}

export const useImageBlobUrl = (src: string, doLoad: boolean = true): string => {
    const blobUrl = useStore(BlobStore.store)[src]
    const buffer = useWebCache(!blobUrl && doLoad ? src : null);
    useEffect(() => {
        if (!buffer || blobUrl || !src) return

        const blob = new Blob([buffer]);
        BlobStore.add(src, URL.createObjectURL(blob))
    }, [buffer, blobUrl, src])
    return blobUrl
}