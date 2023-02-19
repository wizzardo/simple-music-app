import Store from "react-ui-basics/store/Store";
import {Song} from "../services/LocalCacheService";


export type DownloadQueueState = {
    queue: DownloadTask[],
}

export type DownloadTask = {
    url: string,
    artist: string,
    album: string,
    song: string,
    format: string,
    bitrate: number,
    onDownloaded?: (song: Song, data: ArrayBuffer, source?: MediaSource) => void,
}

export const store = new Store({
    queue: []
} as DownloadQueueState)

export default store;

export const clear = () => {
    store.set(state => {
        state.queue = []
    });
}
export const pop = () => {
    store.set(state => {
        state.queue.splice(0, 1)
    });
}
export const remove = (task: DownloadTask) => {
    store.set(state => {
        const i = state.queue.findIndex(it => it.url === task.url)
        if (i >= 0)
            state.queue.splice(i, 1)
    });
}

export const downloadAll = (tasks: DownloadTask[]) => {
    store.set(state => {
        state.queue.push(...tasks)
    });
};

export const download = (
    url: string,
    artist: string,
    album: string,
    song: string,
    format: string,
    bitrate: number,
    onDownloaded?: (song: Song, data: ArrayBuffer, source?: MediaSource) => void
) => {
    store.set(state => {
        if (!state.queue.some(it => it.url === url))
            state.queue.push({url, song, artist, album, format, bitrate, onDownloaded})
    });
}