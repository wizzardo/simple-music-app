import DB from "../utils/DB";
import Store, {useStore} from "react-ui-basics/store/Store";

const SONGS_STORE_NAME = "songs";
const SONG_DATA_STORE_NAME = "song_data";
const WEB_CACHE_STORE_NAME = "web_cache";

export interface Song {
    url: string
    name: string,
    artist: string,
    album: string,
    type: string,
    size: number,
    dataId: number,
    timesPlayed: number,
    dateAdded: number,
}

export interface SongData {
    data: ArrayBuffer,
}

export interface WebCacheEntry {
    url: string
    etag: string,
    data: ArrayBuffer,
}

export class SongLocalCacheDB extends DB {
    async songByUrl(url: string) {
        return this.trRO(SONGS_STORE_NAME)
            .objectStore<Song, number>(SONGS_STORE_NAME)
            .index<string>('url')
            .get(url)
            .asPromise()
    }

    async songs() {
        return this.trRO(SONGS_STORE_NAME)
            .objectStore<Song, number>(SONGS_STORE_NAME)
            .getAll()
            .asPromise()
    }

    async deleteSong(song: Song) {
        let tr = this.trRW(SONGS_STORE_NAME, SONG_DATA_STORE_NAME);
        const songId = await tr.objectStore<Song, number>(SONGS_STORE_NAME)
            .index<string>('url')
            .getKey(song.url)
            .asPromise()

        await tr.objectStore<Song, number>(SONGS_STORE_NAME)
            .delete(songId)
            .asPromise()
        await tr.objectStore<SongData, number>(SONG_DATA_STORE_NAME)
            .delete(song.dataId)
            .asPromise()
    }

    async deleteUnusedSongData() {
        let tr = this.trRW(SONGS_STORE_NAME, SONG_DATA_STORE_NAME);
        const songs = await tr.objectStore<Song, number>(SONGS_STORE_NAME)
            .getAll()
            .asPromise()
        const songDataKeys = await tr.objectStore<SongData, number>(SONG_DATA_STORE_NAME)
            .getAllKeys()
            .asPromise()

        var usedDataKeys = songs.reduce((set, song) => {
            set.add(song.dataId)
            return set
        }, new Set<number>());

        for (const id of songDataKeys) {
            if (!usedDataKeys.has(id)) {
                await tr.objectStore<SongData, number>(SONG_DATA_STORE_NAME)
                    .delete(id)
                    .asPromise()
            }
        }
    }

    async songData(id: number) {
        return this.trRO(SONG_DATA_STORE_NAME)
            .objectStore<SongData, number>(SONG_DATA_STORE_NAME)
            .get(id)
            .asPromise()
    }

    async add(song: Song, data: ArrayBuffer) {
        const tr = this.trRW(SONGS_STORE_NAME, SONG_DATA_STORE_NAME);
        song.dataId = await tr.objectStore<SongData, number>(SONG_DATA_STORE_NAME)
            .add({data})
            .asPromise();
        return tr.objectStore<Song, number>(SONGS_STORE_NAME)
            .add(song)
            .asPromise()
    }

    async addWebCacheEntry(entry: WebCacheEntry) {
        const tr = this.trRW(WEB_CACHE_STORE_NAME);
        return await tr.objectStore<WebCacheEntry, number>(WEB_CACHE_STORE_NAME)
            .add(entry)
            .asPromise();
    }

    async getWebCacheEntry(url: string) {
        return this.trRO(WEB_CACHE_STORE_NAME)
            .objectStore<WebCacheEntry, number>(WEB_CACHE_STORE_NAME)
            .index<string>('url')
            .get(url)
            .asPromise()
    }
}

const db = new SongLocalCacheDB("localCache", [
    {
        version: () => 1,
        name: () => 'init',
        execute: (tr, db) => {
            db.createObjectStore(SONGS_STORE_NAME, {autoIncrement: true});
        }
    },
    {
        version: () => 2,
        name: () => 'creating indexes',
        execute: async (tr, db, wrapper) => {
            const store = tr.objectStore<Song, number>(SONGS_STORE_NAME);
            store.createIndex("url", "url", {unique: true});
            store.createIndex("artist", "artist", {unique: false});
            store.createIndex("album", "album", {unique: false});
            store.createIndex("size", "size", {unique: false});
        }
    },
    {
        version: () => 3,
        name: () => 'adding song_data',
        execute: async (tr, db, wrapper) => {
            db.createObjectStore(SONG_DATA_STORE_NAME, {autoIncrement: true});
        }
    },
    {
        version: () => 4,
        name: () => 'adding web_cache',
        execute: async (tr, db, wrapper) => {
            db.createObjectStore(WEB_CACHE_STORE_NAME, {autoIncrement: true});
            const store = tr.objectStore<WebCacheEntry, number>(WEB_CACHE_STORE_NAME);
            store.createIndex("url", "url", {unique: true});
        }
    },
]) as SongLocalCacheDB

interface DBStore {
    ready: boolean
}

const store = new Store<DBStore>({ready: false});
(async () => {
    try {
        await db.open()
        store.set(state => {
            state.ready = true
        })
    } catch (e) {
        console.error(e)
    }
})()

export const useLocalCache = () => {
    const isReady = useStore(store).ready;
    return isReady ? db : null;
}
