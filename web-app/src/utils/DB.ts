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
IDBTransaction.prototype['asPromise'] = function <T>() {
    return new Promise<T>((resolve, reject) => {
        this.onsuccess = e => resolve(this)
        this.onerror = reject
    });
};
IDBTransaction.prototype['then'] = function <T>(resolve: (T) => void, reject) {
    this.onsuccess = e => resolve(this)
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

interface PromisedTransaction extends IDBTransaction {
    onabort: ((this: PromisedTransaction, ev: Event) => any) | null;
    oncomplete: ((this: PromisedTransaction, ev: Event) => any) | null;
    onerror: ((this: PromisedTransaction, ev: Event) => any) | null;

    objectStore<T, K extends IDBValidKey>(name: string): TypedObjectStore<T, K>;

    addEventListener<K extends keyof IDBTransactionEventMap>(type: K, listener: (this: PromisedTransaction, ev: IDBTransactionEventMap[K]) => any, options?: boolean | AddEventListenerOptions): void;

    addEventListener(type: string, listener: EventListenerOrEventListenerObject, options?: boolean | AddEventListenerOptions): void;

    removeEventListener<K extends keyof IDBTransactionEventMap>(type: K, listener: (this: PromisedTransaction, ev: IDBTransactionEventMap[K]) => any, options?: boolean | EventListenerOptions): void;

    removeEventListener(type: string, listener: EventListenerOrEventListenerObject, options?: boolean | EventListenerOptions): void;

    asPromise(): Promise<PromisedTransaction>,
}


interface Migration {
    execute(tr: PromisedTransaction, db: IDBDatabase, wrapper: DB): Promise<any> | void,

    version(): number;

    name(): string;
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
            const migrations = this.migrations;
            migrations.sort(Comparators.of((it => it.version()), Comparators.SORT_ASC, migrations))
            const version = migrations[migrations.length - 1]?.version() || 0

            const openDBRequest = window.indexedDB.open(this.name, version);
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
                const db: IDBDatabase = event.target['result'];
                this.db = db
                upgrading = true
                const filtered = migrations.filter(it => it.version() > event.oldVersion);
                const tr = event.target['transaction']
                for (let i = 0; i < filtered.length; i++) {
                    const it = filtered[i];
                    console.log('executing migration', it.version(), it.name())
                    await it.execute(tr, db, this);
                }
                await tr
                upgrading = false
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
        return tr as PromisedTransaction
    }
}

export default DB;