import React, {useEffect, useRef, useState} from "react";
import {useStore} from "react-ui-basics/store/Store";
import * as DownloadQueueStore from "../stores/DownloadQueueStore";
import {SongLocalCacheDB, useLocalCache} from "../services/LocalCacheService";

const load = (url, setAudio, localCache: SongLocalCacheDB, artist: string, album: string, name: string, isRetrying?: boolean) => {
    var request = new XMLHttpRequest();
    request.open('GET', url, true);
    request.responseType = 'arraybuffer';

    request.onload = () => {
        let audioData = request.response;
        const responsetext = (!request.responseType || request.responseType ==='text') && request.responseText;
        if (request.status != 200 || Number(request.getResponseHeader('Content-Length')) === 0) {
            console.error(url, request.status, responsetext)
            if (!isRetrying || request.status === 503) {
                load(url, setAudio, localCache, artist, album, name, true)
            }
            return
        }

        const contentType = request.getResponseHeader('Content-Type');
        const data = audioData.slice();

        let song = {
            url,
            name,
            album,
            artist,
            type: contentType,
            size: data.byteLength,
            dataId: 0,
            timesPlayed: 0,
            dateAdded: new Date().getTime(),
        };
        localCache.add(song, data)
        setAudio(song, data)
    }
    request.send();
};

const MAX_PARALLEL_DOWNLOADS = 4;

const DownloadQueue = ({}) => {
    const localCache = useLocalCache();
    const {queue} = useStore(DownloadQueueStore.store)
    const [downloading, setDownloading] = useState(0)
    const downloadingRef = useRef<number>()
    downloadingRef.current = downloading

    useEffect(() => {
        if (!localCache)
            return
        if (downloading == MAX_PARALLEL_DOWNLOADS)
            return;
        if (queue.length <= downloading)
            return;

        const task = queue[downloading];

        setDownloading(downloading + 1)
        load(task.url, (song, data) => {
            setDownloading(downloadingRef.current - 1)
            DownloadQueueStore.remove(task)
            task.onDownloaded?.(song, data)
        }, localCache, task.artist, task.album, task.song)

    }, [localCache, queue, downloading])


    return null
}

export default DownloadQueue