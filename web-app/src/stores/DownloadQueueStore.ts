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
    onDownloaded?: (song: Song, data: ArrayBuffer) => void,
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
    onDownloaded?: (song: Song, data: ArrayBuffer) => void
) => {
    store.set(state => {
        state.queue.push({url, song, artist, album, format, bitrate, onDownloaded})
    });
}