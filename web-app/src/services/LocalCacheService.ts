import DB from "../utils/DB";
import Store, {useStore} from "react-ui-basics/store/Store";

const SONGS_STORE_NAME = "songs";
const SONG_DATA_STORE_NAME = "song_data";

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
]) as SongLocalCacheDB

interface DBStore {
    ready: boolean
}

const store = new Store<DBStore>({ready: false});
(async () => {
    await db.open()
    store.set(state => {
        state.ready = true
    })
})()

export const useLocalCache = () => {
    const isReady = useStore(store).ready;
    return isReady ? db : null;
}
