import DB from "../utils/DB";
import Store, {useStore} from "react-ui-basics/store/Store";

export const SONGS_STORE_NAME = "songs";

interface Song {
    url: string
    artist: string,
    album: string,
    size: number,
    data: ArrayBuffer,
}

export class SongLocalCacheDB extends DB {
    async songByUrl(url: string) {
        return await (this.trRO(SONGS_STORE_NAME)
                .objectStore<Song, number>(SONGS_STORE_NAME)
                .index<string>('url')
                .get(url)
                .asPromise()
        )
    }

    async add(song: Song) {
        return await (this.trRW(SONGS_STORE_NAME)
            .objectStore<Song, number>(SONGS_STORE_NAME)
            .add(song))
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
            await tr
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

export const useLocalSongCacheRW = () => {
    const isReady = useStore(store).ready;
    if (!isReady)
        return null;

    return db.trRW(SONGS_STORE_NAME).objectStore<Song, number>(SONGS_STORE_NAME)
}