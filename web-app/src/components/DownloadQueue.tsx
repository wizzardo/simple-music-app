import React, {useEffect, useRef, useState} from "react";
import {useStore} from "react-ui-basics/store/Store";
import * as DownloadQueueStore from "../stores/DownloadQueueStore";
import {Song, SongLocalCacheDB, useLocalCache} from "../services/LocalCacheService";
import {addEventListener} from "react-ui-basics/Tools";

const load = (url,
              localCache: SongLocalCacheDB,
              artist: string,
              album: string,
              name: string,
              setAudio?: (song: Song, data?: ArrayBuffer, source?: MediaSource) => void,
              onFinish?: () => void,
) => {
    function concat(arrays: Uint8Array[]) {
        let totalLength = arrays.reduce((acc, value) => acc + value.length, 0);

        if (!arrays.length) return null;

        let result = new Uint8Array(totalLength);

        let length = 0;
        for (let array of arrays) {
            result.set(array, length);
            length += array.length;
        }

        return result;
    }

    fetch(url + '/stream')
        .then((response) => {
            const reader = response.body.getReader();
            const contentType = response.headers.get('Content-Type');
            const arrays: Uint8Array[] = []

            let song: Song = {
                url,
                name,
                album,
                artist,
                type: contentType,
                size: 0,
                dataId: 0,
                timesPlayed: 0,
                dateAdded: new Date().getTime(),
            };


            let sourceBuffer: SourceBuffer;
            let bufferPosition = 0;
            let isWaiting = false

            if (setAudio) {
                const mediaSource = new MediaSource();
                addEventListener(mediaSource, 'sourceopen', () => {
                    sourceBuffer = mediaSource.addSourceBuffer(contentType);
                    isWaiting = true
                    addEventListener(sourceBuffer, 'updateend', () => {
                        if (bufferPosition < arrays.length) {
                            sourceBuffer.appendBuffer(arrays[bufferPosition++]);
                        } else {
                            isWaiting = true;
                        }
                    })
                })
                setAudio(song, null, mediaSource)
            }

            function pump() {
                reader.read().then(({done, value}) => {
                    // console.log('on chunk', value?.length, 'is last:', done, bufferPosition, arrays.length)
                    if (value) {
                        arrays.push(value)
                        if (isWaiting) {
                            bufferPosition++
                            isWaiting = false;
                            sourceBuffer.appendBuffer(value)
                        }
                    }

                    if (done) {
                        const data = concat(arrays);
                        // console.log('done', data)
                        song.size = data.byteLength
                        localCache.add(song, data)
                        onFinish && onFinish()
                        return;
                    }

                    pump();
                });
            }

            pump()
        })
    ;

    // var request = new XMLHttpRequest();
    // request.open('GET', url, true);
    // request.responseType = 'arraybuffer';
    //
    // request.onload = () => {
    //     let audioData = request.response;
    //     const responsetext = (!request.responseType || request.responseType ==='text') && request.responseText;
    //     if (request.status != 200 || Number(request.getResponseHeader('Content-Length')) === 0) {
    //         console.error(url, request.status, responsetext)
    //         if (!isRetrying || request.status === 503) {
    //             load(url, setAudio, localCache, artist, album, name, true)
    //         }
    //         return
    //     }
    //
    //     const contentType = request.getResponseHeader('Content-Type');
    //     const data: ArrayBuffer = audioData.slice();
    //
    //     let song: Song = {
    //         url,
    //         name,
    //         album,
    //         artist,
    //         type: contentType,
    //         size: data.byteLength,
    //         dataId: 0,
    //         timesPlayed: 0,
    //         dateAdded: new Date().getTime(),
    //     };
    //     localCache.add(song, data)
    //     setAudio(song, data)
    // }
    // request.send();
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
        load(task.url, localCache, task.artist, task.album, task.song, (song: Song, data: ArrayBuffer, source: MediaSource) => {
            task.onData && task.onData(song, data, source)
        }, () => {
            setDownloading(downloadingRef.current - 1)
            DownloadQueueStore.remove(task)
        })

    }, [localCache, queue, downloading])


    return null
}

export default DownloadQueue