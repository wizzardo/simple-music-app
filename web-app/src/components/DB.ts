import {Comparators} from "react-ui-basics/Tools";

export const asPromise = <T>(request: IDBRequest<T>) => new Promise<T>((resolve, reject) => {
    request.onsuccess = e => resolve(request.result)
    request.onerror = reject
});

IDBRequest.prototype['asPromise'] = function <T>() {
    return new Promise<T>((resolve, reject) => {
        this.onsuccess = e => resolve(this.result)
        this.onerror = reject
    });
};
IDBRequest.prototype['then'] = function <T>(resolve: (T) => void, reject) {
    this.onsuccess = e => resolve(this.result)
    this.onerror = reject
};


interface PromisedRequest<T> extends IDBRequest<T> {
    asPromise(): Promise<T>,
}

interface TypedCursorWithValue<T> extends IDBCursorWithValue {
    readonly value: T;
}

interface TypedObjectStore<T, K extends IDBValidKey> extends IDBObjectStore {
    add(value: T, key?: K): PromisedRequest<K>;

    clear(): PromisedRequest<undefined>;

    count(query?: Query<K>): PromisedRequest<number>;

    createIndex<I extends IDBValidKey>(name: string, keyPath: string | string[], options?: IDBIndexParameters): TypedIndex<T, I, K>;

    delete(query: Query<K>): PromisedRequest<undefined>;

    deleteIndex(name: string): void;

    get(query: Query<K>): PromisedRequest<T>;

    getAll(query?: Query<K> | null, count?: number): PromisedRequest<T[]>;

    getAllKeys(query?: Query<K> | null, count?: number): PromisedRequest<K[]>;

    getKey(query: Query<K>): PromisedRequest<K | undefined>;

    index<I extends IDBValidKey>(name: string): TypedIndex<T, I, K>;

    openCursor(query?: Query<K> | null, direction?: IDBCursorDirection): PromisedRequest<TypedCursorWithValue<T> | null>;

    openKeyCursor(query?: Query<K> | null, direction?: IDBCursorDirection): PromisedRequest<IDBCursor | null>;

    put(value: T, key?: K): PromisedRequest<K>;
}

interface TypedKeyRange<K extends IDBValidKey> extends IDBKeyRange {
    readonly lower: K;
    readonly upper: K;

    includes(key: K): boolean;
}

type Query<K extends IDBValidKey> = K | TypedKeyRange<K>

interface TypedIndex<T, K extends IDBValidKey, M extends IDBValidKey> extends IDBIndex {
    readonly objectStore: TypedObjectStore<T, M>;

    count(query?: Query<K>): PromisedRequest<number>;

    get(query: Query<K>): PromisedRequest<T>;

    getAll(query?: Query<K> | null, count?: number): PromisedRequest<T[]>;

    getAllKeys(query?: Query<K> | null, count?: number): PromisedRequest<M[]>;

    getKey(query: Query<K>): PromisedRequest<M | undefined>;

    openCursor(query?: Query<K> | null, direction?: IDBCursorDirection): PromisedRequest<TypedCursorWithValue<T> | null>;

    openKeyCursor(query?: Query<K> | null, direction?: IDBCursorDirection): PromisedRequest<IDBCursor | null>;
}

interface Migration {
    execute(db: IDBDatabase): Promise<any>,

    version(): number;
}

class DB {

    db: IDBDatabase;
    name: string;
    migrations: Migration[]

    constructor(name: string, migrations: Migration[]) {
        this.name = name
        this.migrations = migrations
    }

    open() {
        let upgraded = false
        let upgrading = false
        return new Promise((resolve, reject) => {
            let migrations = this.migrations;
            migrations.sort(Comparators.of((it => it.version()), Comparators.SORT_ASC, migrations))
            let version = migrations[migrations.length - 1]?.version() || 0

            let openDBRequest = window.indexedDB.open(this.name, version);
            openDBRequest.onerror = event => {
                console.error(event)
                reject(event)
            };
            openDBRequest.onsuccess = event => {
                console.log('onsuccess', event)
                this.db = event.target['result'] as IDBDatabase;
                if (!upgraded && !upgrading) {
                    resolve(this.db)
                }
            };
            openDBRequest.onupgradeneeded = async event => {
                console.log('onupgradeneeded', event)
                let db: IDBDatabase = event.target['result'];
                upgrading = true
                let filtered = migrations.filter(it => it.version() > db.version);
                for (let i = 0; i < filtered.length; i++) {
                    const it = filtered[i];
                    await it.execute(db);
                }
                upgrading = false
                this.db = db
                resolve(this.db)
                upgraded = true
            };
        })
    }


    trRO(...stores: string[]) {
        return this.tr('readonly', ...stores)
    }

    trRW(...stores: string[]) {
        return this.tr('readwrite', ...stores)
    }

    tr(mode: 'readwrite' | 'readonly', ...stores: string[]) {
        // debugger
        let tr = this.db.transaction(stores, mode);
        tr.oncomplete = event => {
            console.log("transaction.oncomplete");
        };

        tr.onerror = event => {
            console.error(event)
        };
        return tr
    }
}

export default DB;